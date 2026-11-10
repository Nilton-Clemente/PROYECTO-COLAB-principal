package com.prieto.colab_movil.data.remote

import com.prieto.colab_movil.data.model.HistorialAvanceResponseDto
import com.prieto.colab_movil.data.model.RegistroAvanceRequestDto
import com.prieto.colab_movil.data.model.RegistroAvanceResponseDto
import com.prieto.colab_movil.data.model.TareaDetalleResponseDto
import com.prieto.colab_movil.data.model.TareaEstadoRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AvanceApiService {
    @GET("api/proyectos/{proyectoId}/tareas/{tareaId}/avances")
    suspend fun historial(
        @Path("proyectoId") proyectoId: Long,
        @Path("tareaId") tareaId: Long
    ): Response<HistorialAvanceResponseDto>

    @POST("api/proyectos/{proyectoId}/tareas/{tareaId}/avances")
    suspend fun registrar(
        @Path("proyectoId") proyectoId: Long,
        @Path("tareaId") tareaId: Long,
        @Body request: RegistroAvanceRequestDto
    ): Response<RegistroAvanceResponseDto>

    @PUT("api/proyectos/{proyectoId}/tareas/{tareaId}/estado")
    suspend fun cambiarEstado(
        @Path("proyectoId") proyectoId: Long,
        @Path("tareaId") tareaId: Long,
        @Body request: TareaEstadoRequestDto
    ): Response<TareaDetalleResponseDto>
}
