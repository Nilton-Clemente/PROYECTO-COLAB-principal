package com.prieto.colab_movil.data.repository

import com.google.gson.JsonParser
import com.prieto.colab_movil.data.model.PlanificacionResponseDto
import com.prieto.colab_movil.data.remote.PlanificacionApiService
import com.prieto.colab_movil.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

interface PlanificacionRepository {
    /** Devuelve la planificación o `null` cuando el proyecto aún no la tiene (HTTP 404). */
    suspend fun verPlanificacion(proyectoId: Long): Result<PlanificacionResponseDto?>
    suspend fun generarPlanificacion(proyectoId: Long): Result<PlanificacionResponseDto>
}

class PlanificacionRepositoryImpl(
    private val apiService: PlanificacionApiService = RetrofitClient.planificacionApiService
) : PlanificacionRepository {

    override suspend fun verPlanificacion(proyectoId: Long): Result<PlanificacionResponseDto?> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.verPlanificacion(proyectoId)
                when {
                    response.code() == 404 -> Result.success(null)
                    response.isSuccessful && response.body() != null -> Result.success(response.body())
                    else -> Result.failure(Exception(mensajeErrorDe(response)))
                }
            } catch (e: Exception) {
                Result.failure(Exception("No se pudo conectar con el servidor backend. Verifica tu conexión."))
            }
        }

    override suspend fun generarPlanificacion(proyectoId: Long): Result<PlanificacionResponseDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.generarPlanificacion(proyectoId)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception(mensajeErrorDe(response)))
                }
            } catch (e: Exception) {
                Result.failure(Exception("No se pudo conectar con el servidor backend. Verifica tu conexión."))
            }
        }
}

/**
 * Extrae el motivo real del error que devuelve el backend.
 * Spring Boot (versión 4) responde los `ResponseStatusException` como ProblemDetail
 * (campo `detail`) o, en el formato clásico, dentro de `message`.
 * Si el backend no incluye el mensaje, se mapea el código HTTP a una descripción útil.
 */
private fun mensajeErrorDe(response: Response<*>): String {
    val code = response.code()
    val raw = try {
        response.errorBody()?.string()
    } catch (e: Exception) {
        null
    }
    if (raw.isNullOrBlank()) return descripcionPorCodigo(code)

    return try {
        val obj = JsonParser.parseString(raw).asJsonObject
        val detail = obj.get("detail")?.takeUnless { it.isJsonNull }?.asString
        val message = obj.get("message")?.takeUnless { it.isJsonNull }?.asString
        when {
            !detail.isNullOrBlank() -> detail
            !message.isNullOrBlank() -> message
            else -> descripcionPorCodigo(code)
        }
    } catch (e: Exception) {
        descripcionPorCodigo(code)
    }
}

/**
 * Mensaje de respaldo por código HTTP para cuando el backend no devuelve
 * un motivo explícito en el cuerpo del error.
 */
private fun descripcionPorCodigo(code: Int): String = when (code) {
    400 -> "Solicitud inválida. Verifica que el proyecto tenga objetivo general, fecha de entrega, requerimientos funcionales, tecnologías y al menos dos integrantes con habilidades."
    401 -> "No autorizado. Vuelve a iniciar sesión."
    403 -> "No tienes permisos. Solo el creador del proyecto puede generar la planificación."
    404 -> "No se encontró el recurso solicitado."
    409 -> "La planificación ya fue generada para este proyecto."
    502 -> "No se pudo obtener una respuesta válida de la IA. Inténtalo de nuevo."
    500 -> "Error interno del servidor. Inténtalo más tarde."
    else -> "Error del servidor (HTTP $code)."
}
