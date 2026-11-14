package com.prieto.colab_movil.data.remote

import com.prieto.colab_movil.data.model.ConfirmacionResponseDto
import com.prieto.colab_movil.data.model.SolicitarCambiosRequestDto
import com.prieto.colab_movil.data.model.TareaDetalleResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MisTareasApiService {
    @GET("api/proyectos/{proyectoId}/tareas/mis-tareas")
    suspend fun misTareas(@Path("proyectoId") proyectoId: Long): Response<List<TareaDetalleResponseDto>>

    @GET("api/proyectos/{proyectoId}/planificacion/confirmaciones")
    suspend fun confirmaciones(@Path("proyectoId") proyectoId: Long): Response<List<ConfirmacionResponseDto>>

    @POST("api/proyectos/{proyectoId}/planificacion/confirmar")
    suspend fun confirmar(@Path("proyectoId") proyectoId: Long): Response<ConfirmacionResponseDto>

    @POST("api/proyectos/{proyectoId}/planificacion/solicitar-cambios")
    suspend fun solicitarCambios(
        @Path("proyectoId") proyectoId: Long,
        @Body request: SolicitarCambiosRequestDto
    ): Response<ConfirmacionResponseDto>
}
