package com.prieto.colab_movil.data.model

import com.google.gson.annotations.SerializedName

/**
 * Unidad de trabajo dentro de una etapa.
 * Debe coincidir con el record `TareaResponse` del backend Spring Boot.
 */
data class TareaResponseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("responsableEmail") val responsableEmail: String?,
    @SerializedName("prioridad") val prioridad: String?,
    @SerializedName("estado") val estado: String?,
    @SerializedName("duracionDias") val duracionDias: Int?,
    @SerializedName("fechaInicio") val fechaInicio: String?,
    @SerializedName("fechaFin") val fechaFin: String?,
    @SerializedName("dependencias") val dependencias: List<Long>?
)
