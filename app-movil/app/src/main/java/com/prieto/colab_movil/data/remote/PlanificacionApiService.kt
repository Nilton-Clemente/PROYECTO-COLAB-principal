package com.prieto.colab_movil.data.remote

import com.prieto.colab_movil.data.model.DependenciaRequestDto
import com.prieto.colab_movil.data.model.EtapaRequestDto
import com.prieto.colab_movil.data.model.EtapaResponseDto
import com.prieto.colab_movil.data.model.PlanificacionResponseDto
import com.prieto.colab_movil.data.model.TareaRequestDto
import com.prieto.colab_movil.data.model.TareaResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PUT

interface PlanificacionApiService {
    @GET("api/proyectos/{proyectoId}/planificacion")
    suspend fun verPlanificacion(@Path("proyectoId") proyectoId: Long): Response<PlanificacionResponseDto>

    @POST("api/proyectos/{proyectoId}/planificacion")
    suspend fun generarPlanificacion(@Path("proyectoId") proyectoId: Long): Response<PlanificacionResponseDto>

    @POST("api/proyectos/{proyectoId}/etapas")
    suspend fun crearEtapa(
        @Path("proyectoId") proyectoId: Long,
        @Body request: EtapaRequestDto
    ): Response<EtapaResponseDto>

    @PUT("api/proyectos/{proyectoId}/etapas/{etapaId}")
    suspend fun editarEtapa(
        @Path("proyectoId") proyectoId: Long,
        @Path("etapaId") etapaId: Long,
        @Body request: EtapaRequestDto
    ): Response<EtapaResponseDto>

    @DELETE("api/proyectos/{proyectoId}/etapas/{etapaId}")
    suspend fun eliminarEtapa(
        @Path("proyectoId") proyectoId: Long,
        @Path("etapaId") etapaId: Long
    ): Response<Unit>

    @POST("api/proyectos/{proyectoId}/tareas")
    suspend fun crearTarea(
        @Path("proyectoId") proyectoId: Long,
        @Body request: TareaRequestDto
    ): Response<TareaResponseDto>

    @PUT("api/proyectos/{proyectoId}/tareas/{tareaId}")
    suspend fun editarTarea(
        @Path("proyectoId") proyectoId: Long,
        @Path("tareaId") tareaId: Long,
        @Body request: TareaRequestDto
    ): Response<TareaResponseDto>

    @DELETE("api/proyectos/{proyectoId}/tareas/{tareaId}")
    suspend fun eliminarTarea(
        @Path("proyectoId") proyectoId: Long,
        @Path("tareaId") tareaId: Long
    ): Response<Unit>

    @POST("api/proyectos/{proyectoId}/tareas/{tareaId}/dependencias")
    suspend fun agregarDependencia(
        @Path("proyectoId") proyectoId: Long,
        @Path("tareaId") tareaId: Long,
        @Body request: DependenciaRequestDto
    ): Response<TareaResponseDto>

    @DELETE("api/proyectos/{proyectoId}/tareas/{tareaId}/dependencias/{predecesoraId}")
    suspend fun quitarDependencia(
        @Path("proyectoId") proyectoId: Long,
        @Path("tareaId") tareaId: Long,
        @Path("predecesoraId") predecesoraId: Long
    ): Response<TareaResponseDto>
}
