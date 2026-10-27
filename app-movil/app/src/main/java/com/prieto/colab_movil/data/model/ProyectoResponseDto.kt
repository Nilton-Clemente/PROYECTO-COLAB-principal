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
    @SerializedName("tecnologias") val tecnologias: List<TecnologiaDto>?
)
