package com.prieto.colab_movil.data.remote

import com.prieto.colab_movil.data.model.PlanificacionResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PlanificacionApiService {
    @GET("api/proyectos/{proyectoId}/planificacion")
    suspend fun verPlanificacion(@Path("proyectoId") proyectoId: Long): Response<PlanificacionResponseDto>

    @POST("api/proyectos/{proyectoId}/planificacion")
    suspend fun generarPlanificacion(@Path("proyectoId") proyectoId: Long): Response<PlanificacionResponseDto>
}
