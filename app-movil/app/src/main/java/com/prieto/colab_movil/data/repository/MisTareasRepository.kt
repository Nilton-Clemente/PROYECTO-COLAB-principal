package com.prieto.colab_movil.data.repository

import com.google.gson.JsonParser
import com.prieto.colab_movil.data.model.ConfirmacionResponseDto
import com.prieto.colab_movil.data.model.PerfilResponseDto
import com.prieto.colab_movil.data.model.PlanificacionResponseDto
import com.prieto.colab_movil.data.model.ProyectoResponseDto
import com.prieto.colab_movil.data.model.SolicitarCambiosRequestDto
import com.prieto.colab_movil.data.model.TareaDetalleResponseDto
import com.prieto.colab_movil.data.remote.MisTareasApiService
import com.prieto.colab_movil.data.remote.PerfilApiService
import com.prieto.colab_movil.data.remote.PlanificacionApiService
import com.prieto.colab_movil.data.remote.ProyectoApiService
import com.prieto.colab_movil.data.remote.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

data class MisTareasDatos(
    val proyecto: ProyectoResponseDto,
    val perfil: PerfilResponseDto,
    val planificacion: PlanificacionResponseDto?,
    val tareas: List<TareaDetalleResponseDto>,
    val confirmacion: ConfirmacionResponseDto?
)

interface MisTareasRepository {
    suspend fun cargar(proyectoId: Long): Result<MisTareasDatos>
    suspend fun confirmar(proyectoId: Long): Result<ConfirmacionResponseDto>
    suspend fun solicitarCambios(proyectoId: Long, comentario: String): Result<ConfirmacionResponseDto>
}

class MisTareasRepositoryImpl(
    private val api: MisTareasApiService = RetrofitClient.misTareasApiService,
    private val proyectoApi: ProyectoApiService = RetrofitClient.proyectoApiService,
    private val perfilApi: PerfilApiService = RetrofitClient.perfilApiService,
    private val planificacionApi: PlanificacionApiService = RetrofitClient.planificacionApiService
) : MisTareasRepository {
    override suspend fun cargar(proyectoId: Long): Result<MisTareasDatos> = ejecutar {
        val proyecto = proyectoApi.verProyecto(proyectoId).cuerpo()
        val perfil = perfilApi.obtenerPerfil().cuerpo()
        val respuesta = planificacionApi.verPlanificacion(proyectoId)
        // Consultar no debe generar una planificación ni esconder errores de permisos/red.
        val planificacion = if (respuesta.code() == 404) null else respuesta.cuerpo()
        if (planificacion == null) {
            MisTareasDatos(proyecto, perfil, null, emptyList(), null)
        } else {
            val tareas = api.misTareas(proyectoId).cuerpo()
            val confirmaciones = api.confirmaciones(proyectoId).cuerpo()
            MisTareasDatos(
                proyecto, perfil, planificacion, tareas,
                confirmaciones.firstOrNull {
                    it.integranteEmail.trim().equals(perfil.email.trim(), ignoreCase = true)
                }
            )
        }
    }

    override suspend fun confirmar(proyectoId: Long): Result<ConfirmacionResponseDto> = ejecutar {
        api.confirmar(proyectoId).cuerpo()
    }

    override suspend fun solicitarCambios(proyectoId: Long, comentario: String): Result<ConfirmacionResponseDto> = ejecutar {
        require(comentario.isNotBlank()) { "Describe el ajuste que necesitas." }
        api.solicitarCambios(proyectoId, SolicitarCambiosRequestDto(comentario.trim())).cuerpo()
    }
}

private suspend fun <T> ejecutar(accion: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
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

private fun <T> Response<T>.cuerpo(): T {
    if (isSuccessful) return body() ?: throw IllegalStateException("El servidor devolvió una respuesta vacía.")
    val detalle = try {
        val json = JsonParser.parseString(errorBody()?.string().orEmpty()).asJsonObject
        listOf("detail", "message").firstNotNullOfOrNull { campo ->
            json.get(campo)?.takeUnless { it.isJsonNull }?.asString?.takeIf { it.isNotBlank() }
        }
    } catch (_: Exception) {
        null
    }
    throw IllegalStateException(detalle ?: when (code()) {
        401 -> "Tu sesión expiró. Vuelve a iniciar sesión."
        403 -> "No tienes permisos para realizar esta acción en el proyecto."
        404 -> "No se encontró el recurso solicitado."
        409 -> "La planificación cambió o ya no admite confirmaciones. Actualiza los datos."
        else -> "No se pudo completar la operación (HTTP ${code()})."
    })
}
