package com.prieto.colab_movil.data.remote

import com.prieto.colab_movil.data.model.IntegranteResponseDto
import com.prieto.colab_movil.data.model.InvitacionResponseDto
import com.prieto.colab_movil.data.model.ProyectoRequest
import com.prieto.colab_movil.data.model.ProyectoResponseDto
import com.prieto.colab_movil.data.model.TecnologiaDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ProyectoApiService {
    @GET("api/proyectos")
    suspend fun listarProyectos(): Response<List<ProyectoResponseDto>>

    @GET("api/proyectos/{id}")
    suspend fun verProyecto(@Path("id") id: Long): Response<ProyectoResponseDto>

    @GET("api/proyectos/{id}/integrantes")
    suspend fun verEquipo(@Path("id") id: Long): Response<List<IntegranteResponseDto>>

    @GET("api/invitaciones")
    suspend fun listarInvitaciones(): Response<List<InvitacionResponseDto>>

    @GET("api/tecnologias")
    suspend fun listarTecnologias(): Response<List<TecnologiaDto>>

    @POST("api/proyectos")
    suspend fun crearProyecto(@Body request: ProyectoRequest): Response<ProyectoResponseDto>
}
