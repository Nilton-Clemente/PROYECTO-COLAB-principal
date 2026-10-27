package com.prieto.colab_movil.data.remote

import com.prieto.colab_movil.data.model.InvitacionResponseDto
import com.prieto.colab_movil.data.model.ProyectoResponseDto
import retrofit2.Response
import retrofit2.http.GET

interface ProyectoApiService {
    @GET("api/proyectos")
    suspend fun listarProyectos(): Response<List<ProyectoResponseDto>>

    @GET("api/invitaciones")
    suspend fun listarInvitaciones(): Response<List<InvitacionResponseDto>>
}
