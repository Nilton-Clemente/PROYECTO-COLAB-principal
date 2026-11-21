package com.prieto.colab_movil.data.repository

import com.prieto.colab_movil.data.model.IntegranteResponseDto
import com.prieto.colab_movil.data.model.InvitacionResponseDto
import com.prieto.colab_movil.data.model.PerfilResponseDto
import com.prieto.colab_movil.data.model.ProyectoRequest
import com.prieto.colab_movil.data.model.ProyectoResponseDto
import com.prieto.colab_movil.data.model.TecnologiaDto
import com.prieto.colab_movil.data.remote.PerfilApiService
import com.prieto.colab_movil.data.remote.ProyectoApiService
import com.prieto.colab_movil.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import retrofit2.HttpException

interface ProyectoRepository {
    suspend fun obtenerPerfil(): Result<PerfilResponseDto>
    suspend fun listarProyectos(): Result<List<ProyectoResponseDto>>
    suspend fun verProyecto(id: Long): Result<ProyectoResponseDto>
    suspend fun verEquipo(id: Long): Result<List<IntegranteResponseDto>>
    suspend fun listarInvitaciones(): Result<List<InvitacionResponseDto>>
    suspend fun aceptarInvitacion(id: Long): Result<Unit>
    suspend fun rechazarInvitacion(id: Long): Result<Unit>
    suspend fun listarTecnologias(): Result<List<TecnologiaDto>>
    suspend fun crearProyecto(request: ProyectoRequest): Result<ProyectoResponseDto>
    suspend fun editarProyecto(id: Long, request: ProyectoRequest): Result<ProyectoResponseDto>
    suspend fun cancelarProyecto(id: Long): Result<ProyectoResponseDto>
    suspend fun eliminarProyecto(id: Long): Result<Unit>
}

class ProyectoRepositoryImpl(
    private val proyectoApiService: ProyectoApiService = RetrofitClient.proyectoApiService,
    private val perfilApiService: PerfilApiService = RetrofitClient.perfilApiService,
    private val cicloApiService: ProyectoApiService = RetrofitClient.cicloProyectoApiService
) : ProyectoRepository {

    override suspend fun obtenerPerfil(): Result<PerfilResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = perfilApiService.obtenerPerfil()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al cargar perfil (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listarProyectos(): Result<List<ProyectoResponseDto>> = withContext(Dispatchers.IO) {
        try {
            val response = proyectoApiService.listarProyectos()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al cargar proyectos (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verProyecto(id: Long): Result<ProyectoResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = proyectoApiService.verProyecto(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(HttpException(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verEquipo(id: Long): Result<List<IntegranteResponseDto>> = withContext(Dispatchers.IO) {
        try {
            val response = proyectoApiService.verEquipo(id)
            if (!response.isSuccessful) throw HttpException(response)
            Result.success(checkNotNull(response.body()) { "El servidor no devolvió el equipo." })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listarInvitaciones(): Result<List<InvitacionResponseDto>> = withContext(Dispatchers.IO) {
        try {
            val response = proyectoApiService.listarInvitaciones()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("No se pudieron cargar las invitaciones (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo conectar con el servidor backend. Verifica tu conexión."))
        }
    }

    override suspend fun listarTecnologias(): Result<List<TecnologiaDto>> = withContext(Dispatchers.IO) {
        try {
            val response = proyectoApiService.listarTecnologias()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al cargar tecnologías (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun crearProyecto(request: ProyectoRequest): Result<ProyectoResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = proyectoApiService.crearProyecto(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al guardar el proyecto (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun editarProyecto(id: Long, request: ProyectoRequest): Result<ProyectoResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = proyectoApiService.editarProyecto(id, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al guardar los cambios (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun aceptarInvitacion(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = proyectoApiService.aceptarInvitacion(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("No se pudo aceptar la invitación (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo conectar con el servidor backend. Verifica tu conexión."))
        }
    }

    override suspend fun cancelarProyecto(id: Long): Result<ProyectoResponseDto> = ejecutarCambio {
        val respuesta = cicloApiService.cancelarProyecto(id)
        if (!respuesta.isSuccessful) throw HttpException(respuesta)
        checkNotNull(respuesta.body()) { "La cancelación no devolvió el proyecto actualizado." }
    }

    override suspend fun eliminarProyecto(id: Long): Result<Unit> = ejecutarCambio {
        val respuesta = cicloApiService.eliminarProyecto(id)
        if (!respuesta.isSuccessful) throw HttpException(respuesta)
    }

    override suspend fun rechazarInvitacion(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = proyectoApiService.rechazarInvitacion(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("No se pudo declinar la invitación (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo conectar con el servidor backend. Verifica tu conexión."))
        }
    }
}

private suspend fun <T> ejecutarCambio(operacion: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
    try {
        Result.success(operacion())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
