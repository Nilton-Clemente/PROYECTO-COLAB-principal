package com.prieto.colab_movil.data.model

import com.google.gson.annotations.SerializedName

/** La confirmación pertenece al integrante dentro de una planificación, no a una tarea. */
data class ConfirmacionResponseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("integranteId") val integranteId: Long,
    @SerializedName("integranteEmail") val integranteEmail: String,
    @SerializedName("integranteNombre") val integranteNombre: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("comentario") val comentario: String?,
    @SerializedName("fecha") val fecha: String?
)

data class SolicitarCambiosRequestDto(
    @SerializedName("comentario") val comentario: String
)
