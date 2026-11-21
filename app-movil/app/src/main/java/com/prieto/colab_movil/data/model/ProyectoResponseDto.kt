package com.prieto.colab_movil.data.model

import com.google.gson.annotations.SerializedName

data class ProyectoResponseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("problematica") val problematica: String?,
    @SerializedName("objetivoGeneral") val objetivoGeneral: String?,
    @SerializedName("objetivosEspecificos") val objetivosEspecificos: String?,
    @SerializedName("alcance") val alcance: String?,
    @SerializedName("restricciones") val restricciones: String?,
    @SerializedName("fechaEntrega") val fechaEntrega: String?,
    @SerializedName("tipo") val tipo: String?,
    @SerializedName("tecnologias") val tecnologias: List<TecnologiaDto>?,
    @SerializedName("requerimientosFuncionales") val requerimientosFuncionales: List<RequerimientoFuncionalDto>?,
    @SerializedName("requerimientosNoFuncionales") val requerimientosNoFuncionales: List<RequerimientoNoFuncionalDto>?,
    @SerializedName("casosDeUso") val casosDeUso: List<CasoUsoDto>?,
    @SerializedName("estado") val estado: String? = null,
    @SerializedName("fechaFinalizacion") val fechaFinalizacion: String? = null,
    @SerializedName("fechaCancelacion") val fechaCancelacion: String? = null,
    @SerializedName("puedeCancelar") val puedeCancelar: Boolean = false,
    @SerializedName("puedeEliminar") val puedeEliminar: Boolean = false
)
