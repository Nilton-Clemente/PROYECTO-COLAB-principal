package com.prieto.colab_movil.data.model

import com.google.gson.annotations.SerializedName

/** Datos requeridos por el CRUD manual de tareas del backend. */
data class TareaRequestDto(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("etapaId") val etapaId: Long,
    @SerializedName("responsableId") val responsableId: Long?,
    @SerializedName("prioridad") val prioridad: String,
    @SerializedName("duracionDias") val duracionDias: Int,
    @SerializedName("orden") val orden: Int
)