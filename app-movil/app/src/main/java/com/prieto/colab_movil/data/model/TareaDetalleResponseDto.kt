package com.prieto.colab_movil.data.model

import com.google.gson.annotations.SerializedName

/** Contrato de TareaDetalleResponse, utilizado por el endpoint mis-tareas. */
data class TareaDetalleResponseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("etapaId") val etapaId: Long,
    @SerializedName("etapaNombre") val etapaNombre: String,
    @SerializedName("responsableId") val responsableId: Long?,
    @SerializedName("responsableEmail") val responsableEmail: String?,
    @SerializedName("prioridad") val prioridad: String?,
    @SerializedName("estado") val estado: String?,
    @SerializedName("duracionDias") val duracionDias: Int?,
    @SerializedName("fechaInicio") val fechaInicio: String?,
    @SerializedName("fechaFin") val fechaFin: String?,
    @SerializedName("orden") val orden: Int?,
    @SerializedName("dependencias") val dependencias: List<Long>?
)
