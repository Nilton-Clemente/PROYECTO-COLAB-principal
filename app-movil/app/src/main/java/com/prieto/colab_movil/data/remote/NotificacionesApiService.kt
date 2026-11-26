package com.prieto.colab_movil.data.remote

import com.prieto.colab_movil.data.model.NotificacionResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.DELETE
import retrofit2.http.PUT
import retrofit2.http.Path

interface NotificacionesApiService {
    @GET("api/notificaciones")
    suspend fun listar(): Response<List<NotificacionResponseDto>>

    @PUT("api/notificaciones/{id}/leida")
    suspend fun marcarLeida(@Path("id") id: Long): Response<NotificacionResponseDto>

    @DELETE("api/notificaciones/leidas")
    suspend fun eliminarLeidas(): Response<Unit>
}