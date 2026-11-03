package com.prieto.colab_movil.data.repository

import com.prieto.colab_movil.data.model.HabilidadDto
import com.prieto.colab_movil.data.model.PerfilResponseDto
import com.prieto.colab_movil.data.remote.PerfilApiService
import com.prieto.colab_movil.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface PerfilRepository {
    suspend fun obtenerPerfil(): Result<PerfilResponseDto>
    suspend fun listarCatalogo(): Result<List<HabilidadDto>>
    suspend fun agregarHabilidad(habilidadId: Long): Result<Unit>
    suspend fun eliminarHabilidad(habilidadId: Long): Result<Unit>
}

class PerfilRepositoryImpl(
    private val apiService: PerfilApiService = RetrofitClient.perfilApiService
) : PerfilRepository {

    override suspend fun obtenerPerfil(): Result<PerfilResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.obtenerPerfil()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("No se pudo cargar el perfil (${response.code()})."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo conectar con el servidor backend. Verifica tu conexión."))
        }
    }

    override suspend fun listarCatalogo(): Result<List<HabilidadDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.listarCatalogo()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("No se pudo cargar el catálogo (${response.code()})."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo conectar con el servidor backend. Verifica tu conexión."))
        }
    }

    override suspend fun agregarHabilidad(habilidadId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.agregarHabilidad(habilidadId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("No se pudo agregar la habilidad (${response.code()})."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo conectar con el servidor backend. Verifica tu conexión."))
        }
    }

    override suspend fun eliminarHabilidad(habilidadId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.eliminarHabilidad(habilidadId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("No se pudo eliminar la habilidad (${response.code()})."))
            }
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo conectar con el servidor backend. Verifica tu conexión."))
        }
    }
}
