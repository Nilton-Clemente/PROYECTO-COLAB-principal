package com.prieto.colab_movil.data.remote

import com.prieto.colab_movil.data.model.HabilidadDto
import com.prieto.colab_movil.data.model.PerfilResponseDto
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface PerfilApiService {

    @GET("api/usuario/perfil")
    suspend fun obtenerPerfil(): Response<PerfilResponseDto>

    @GET("api/habilidades")
    suspend fun listarCatalogo(): Response<List<HabilidadDto>>

    @PUT("api/usuario/habilidades/{habilidadId}")
    suspend fun agregarHabilidad(
        @Path("habilidadId") habilidadId: Long,
        @Query("nivel") nivel: String? = null
    ): Response<Unit>

    @DELETE("api/usuario/habilidades/{habilidadId}")
    suspend fun eliminarHabilidad(@Path("habilidadId") habilidadId: Long): Response<Unit>
}
