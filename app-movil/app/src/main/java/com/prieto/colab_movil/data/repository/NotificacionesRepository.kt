package com.prieto.colab_movil.data.repository

import com.prieto.colab_movil.data.model.NotificacionResponseDto
import com.prieto.colab_movil.data.remote.NotificacionesApiService
import com.prieto.colab_movil.data.remote.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

interface NotificacionesRepository {
    suspend fun listar(): Result<List<NotificacionResponseDto>>
    suspend fun marcarLeida(id: Long): Result<NotificacionResponseDto>
    suspend fun eliminarLeidas(): Result<Unit>
}

class NotificacionesRepositoryImpl(
    private val api: NotificacionesApiService = RetrofitClient.notificacionesApiService
) : NotificacionesRepository {
    override suspend fun listar(): Result<List<NotificacionResponseDto>> = ejecutar { api.listar().cuerpo() }

    override suspend fun marcarLeida(id: Long): Result<NotificacionResponseDto> = ejecutar {
        api.marcarLeida(id).cuerpo().also {
            check(it.id == id && it.leida) { "No se pudo confirmar la lectura. Actualiza las notificaciones." }
        }
    }

    override suspend fun eliminarLeidas(): Result<Unit> = ejecutar {
        api.eliminarLeidas().validar(
            noDisponible = "El servidor no tiene disponible la eliminación de notificaciones leídas. Actualiza o reinicia el backend e inténtalo de nuevo."
        )
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
    validar()
    return body() ?: throw IllegalStateException("El servidor devolvió una respuesta vacía.")
}

private fun Response<*>.validar(noDisponible: String = "La notificación ya no está disponible.") {
    if (isSuccessful) return
    throw IllegalStateException(when (code()) {
        401 -> "Tu sesión expiró. Vuelve a iniciar sesión."
        403 -> "No tienes permiso para consultar o modificar esta notificación."
        404, 405 -> noDisponible
        else -> "No se pudieron consultar o modificar las notificaciones (HTTP ${code()})."
    })
}