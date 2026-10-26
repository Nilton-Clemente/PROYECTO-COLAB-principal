package com.prieto.colab_movil.data.repository

import com.prieto.colab_movil.data.model.AuthResponseDto
import com.prieto.colab_movil.data.model.LoginRequestDto
import com.prieto.colab_movil.data.remote.AuthApiService
import com.prieto.colab_movil.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AuthRepository {
    suspend fun login(email: String, contrasena: String): Result<AuthResponseDto>
}

class AuthRepositoryImpl(
    private val apiService: AuthApiService = RetrofitClient.authApiService
) : AuthRepository {

    override suspend fun login(email: String, contrasena: String): Result<AuthResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.login(LoginRequestDto(email, contrasena))
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val errorMsg = when (response.code()) {
                        401 -> "Credenciales incorrectas. Verifica tu correo o contraseña e inténtalo nuevamente."
                        400 -> "Solicitud inválida. Verifica los datos ingresados."
                        else -> "Error en el servidor (${response.code()}). Inténtalo más tarde."
                    }
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Result.failure(Exception("No se pudo conectar con el servidor backend (http://10.0.2.2:8081). Asegúrate de que esté en ejecución."))
            }
        }
    }
}
