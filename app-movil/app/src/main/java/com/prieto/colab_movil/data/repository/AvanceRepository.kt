package com.prieto.colab_movil.data.repository

import com.google.gson.JsonParser
import com.prieto.colab_movil.data.model.*
import com.prieto.colab_movil.data.remote.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

data class AvanceDatos(
    val proyecto: ProyectoResponseDto,
    val perfil: PerfilResponseDto,
    val planificacion: PlanificacionResponseDto,
    val tarea: TareaDetalleResponseDto,
    val historial: HistorialAvanceResponseDto
)

/** El POST pudo persistirse aunque la respuesta no llegara: nunca reintentarlo automáticamente. */
class RegistroAvanceInciertoException(cause: Throwable) : Exception(
    "No se pudo verificar si el avance quedó guardado. Actualiza y revisa el historial antes de volver a registrar la sesión.", cause
)

interface AvanceRepository {
    suspend fun cargar(proyectoId: Long, tareaId: Long): Result<AvanceDatos>
    suspend fun registrar(proyectoId: Long, tareaId: Long, request: RegistroAvanceRequestDto): Result<RegistroAvanceResponseDto>
    suspend fun cambiarEstado(proyectoId: Long, tareaId: Long, nuevoEstado: String): Result<TareaDetalleResponseDto>
}

class AvanceRepositoryImpl(
    private val api: AvanceApiService = RetrofitClient.avanceApiService,
    private val tareasApi: MisTareasApiService = RetrofitClient.misTareasApiService,
    private val proyectoApi: ProyectoApiService = RetrofitClient.proyectoApiService,
    private val perfilApi: PerfilApiService = RetrofitClient.perfilApiService,
    private val planificacionApi: PlanificacionApiService = RetrofitClient.planificacionApiService
) : AvanceRepository {
    override suspend fun cargar(proyectoId: Long, tareaId: Long): Result<AvanceDatos> = ejecutarAvance {
        val proyecto = proyectoApi.verProyecto(proyectoId).cuerpoAvance()
        val perfil = perfilApi.obtenerPerfil().cuerpoAvance()
        val planificacion = planificacionApi.verPlanificacion(proyectoId).cuerpoAvance()
        val tarea = tareasApi.misTareas(proyectoId).cuerpoAvance().firstOrNull { it.id == tareaId }
            ?: throw IllegalStateException("Esta tarea ya no está asignada a tu usuario en este proyecto.")
        val historial = api.historial(proyectoId, tareaId).cuerpoAvance()
        AvanceDatos(proyecto, perfil, planificacion, tarea, historial)
    }

    override suspend fun registrar(
        proyectoId: Long,
        tareaId: Long,
        request: RegistroAvanceRequestDto
    ): Result<RegistroAvanceResponseDto> = ejecutarAvance {
        require(request.minutos > 0) { "El tiempo invertido debe ser mayor que cero." }
        require(request.nota.trim().length in 20..1000) { "La descripción debe tener entre 20 y 1000 caracteres." }
        val response = try {
            api.registrar(proyectoId, tareaId, request.copy(nota = request.nota.trim()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Incluye respuestas JSON inválidas tras una posible escritura en el servidor.
            throw RegistroAvanceInciertoException(e)
        }
        if (response.code() >= 500 || (response.isSuccessful && response.body() == null)) {
            throw RegistroAvanceInciertoException(IllegalStateException("Respuesta HTTP ${response.code()}"))
        }
        val registro = response.cuerpoAvance()
        if (registro.id <= 0 || registro.tareaId != tareaId || registro.minutos != request.minutos) {
            throw RegistroAvanceInciertoException(IllegalStateException("Respuesta de registro inconsistente."))
        }
        registro
    }

    override suspend fun cambiarEstado(proyectoId: Long, tareaId: Long, nuevoEstado: String): Result<TareaDetalleResponseDto> = ejecutarAvance {
        api.cambiarEstado(proyectoId, tareaId, TareaEstadoRequestDto(nuevoEstado)).cuerpoAvance()
    }
}

private suspend fun <T> ejecutarAvance(accion: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
    try {
        Result.success(accion())
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Result.failure(IOException("No se pudo conectar con el servidor. Verifica tu conexión.", e))
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private fun <T> Response<T>.cuerpoAvance(): T {
    if (isSuccessful) return body() ?: throw IllegalStateException("El servidor devolvió una respuesta vacía.")
    val detalle = try {
        val json = JsonParser.parseString(errorBody()?.string().orEmpty()).asJsonObject
        listOf("detail", "message").firstNotNullOfOrNull {
            json.get(it)?.takeUnless { campo -> campo.isJsonNull }?.asString?.takeIf(String::isNotBlank)
        }
    } catch (_: Exception) { null }
    throw IllegalStateException(detalle ?: when (code()) {
        401 -> "Tu sesión expiró. Vuelve a iniciar sesión."
        403 -> "No tienes permisos para gestionar esta tarea."
        404 -> "No se encontró la tarea o la planificación del proyecto."
        409 -> "La tarea cambió o la transición no está permitida. Actualiza los datos."
        else -> "No se pudo completar la operación (HTTP ${code()})."
    })
}
