package com.prieto.colab_movil.data.remote

import com.prieto.colab_movil.data.model.AuthResponseDto
import com.prieto.colab_movil.data.model.LoginRequestDto
import com.prieto.colab_movil.data.model.RegistroRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<AuthResponseDto>

    @POST("api/auth/registro")
    suspend fun registro(@Body request: RegistroRequestDto): Response<AuthResponseDto>
}
