package com.prieto.colab_movil.data.model

import com.google.gson.annotations.SerializedName

/**
 * Cuerpo de la petición para crear/editar un proyecto.
 * Debe coincidir con el record `ProyectoRequest` del backend Spring Boot.
 */
data class ProyectoRequest(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("problematica") val problematica: String?,
    @SerializedName("objetivoGeneral") val objetivoGeneral: String?,
    @SerializedName("objetivosEspecificos") val objetivosEspecificos: String?,
    @SerializedName("alcance") val alcance: String?,
    @SerializedName("restricciones") val restricciones: String?,
    @SerializedName("fechaEntrega") val fechaEntrega: String?,
    @SerializedName("tecnologiasIds") val tecnologiasIds: List<Long>?,
    @SerializedName("requerimientosFuncionales") val requerimientosFuncionales: List<RequerimientoFuncionalDto>?,
    @SerializedName("requerimientosNoFuncionales") val requerimientosNoFuncionales: List<RequerimientoNoFuncionalDto>?,
    @SerializedName("casosDeUso") val casosDeUso: List<CasoUsoDto>?
)
