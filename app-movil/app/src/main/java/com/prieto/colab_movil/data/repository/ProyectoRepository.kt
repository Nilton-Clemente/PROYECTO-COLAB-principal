package com.prieto.colab_movil.data.repository

import com.prieto.colab_movil.data.model.IntegranteResponseDto
import com.prieto.colab_movil.data.model.InvitacionResponseDto
import com.prieto.colab_movil.data.model.PerfilResponseDto
import com.prieto.colab_movil.data.model.ProyectoResponseDto
import com.prieto.colab_movil.data.remote.PerfilApiService
import com.prieto.colab_movil.data.remote.ProyectoApiService
import com.prieto.colab_movil.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ProyectoRepository {
    suspend fun obtenerPerfil(): Result<PerfilResponseDto>
    suspend fun listarProyectos(): Result<List<ProyectoResponseDto>>
    suspend fun verProyecto(id: Long): Result<ProyectoResponseDto>
    suspend fun verEquipo(id: Long): Result<List<IntegranteResponseDto>>
    suspend fun listarInvitaciones(): Result<List<InvitacionResponseDto>>
}

class ProyectoRepositoryImpl(
    private val proyectoApiService: ProyectoApiService = RetrofitClient.proyectoApiService,
    private val perfilApiService: PerfilApiService = RetrofitClient.perfilApiService
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
                Result.failure(Exception("Error al cargar detalle del proyecto (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verEquipo(id: Long): Result<List<IntegranteResponseDto>> = withContext(Dispatchers.IO) {
        try {
            val response = proyectoApiService.verEquipo(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    override suspend fun listarInvitaciones(): Result<List<InvitacionResponseDto>> = withContext(Dispatchers.IO) {
        try {
            val response = proyectoApiService.listarInvitaciones()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }
}
