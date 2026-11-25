package com.prieto.colab_movil.data.repository

import com.prieto.colab_movil.data.model.IntegranteResponseDto
import com.prieto.colab_movil.data.model.MetricasProyectoDto
import com.prieto.colab_movil.data.model.ProyectoResponseDto
import com.prieto.colab_movil.data.remote.ProyectoApiService
import com.prieto.colab_movil.data.remote.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

data class MetricasProyectoDatos(
    val proyecto: ProyectoResponseDto,
    val metricas: MetricasProyectoDto,
    val equipo: List<IntegranteResponseDto> = emptyList()
)

interface MetricasProyectoRepository {
    suspend fun cargar(proyectoId: Long): Result<MetricasProyectoDatos>
}

class MetricasProyectoRepositoryImpl(
    private val api: ProyectoApiService = RetrofitClient.proyectoApiService
) : MetricasProyectoRepository {
    override suspend fun cargar(proyectoId: Long): Result<MetricasProyectoDatos> = try {
        Result.success(withContext(Dispatchers.IO) {
            val proyecto = async { api.verProyecto(proyectoId).cuerpo() }
            val metricas = api.verMetricas(proyectoId).cuerpo()
            val equipo = if (metricas.totalTareas > 0 && !metricas.porResponsable.isNullOrEmpty()) {
                // El correo de las métricas permite mostrar responsables aunque falle el directorio del equipo.
                try {
                    api.verEquipo(proyectoId).cuerpo()
                } catch (e: HttpException) {
                    if (e.code() in listOf(401, 403, 404)) throw e
                    emptyList()
                } catch (_: IOException) {
                    emptyList()
                }
            } else emptyList()
            MetricasProyectoDatos(proyecto.await(), metricas, equipo)
        })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private fun <T> Response<T>.cuerpo(): T {
    if (!isSuccessful) throw HttpException(this)
    return checkNotNull(body()) { "El servidor devolvió una respuesta vacía." }
}