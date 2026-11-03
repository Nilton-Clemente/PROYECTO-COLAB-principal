package com.prieto.colab_movil.data.model

import com.google.gson.annotations.SerializedName

/**
 * Fase dentro de una planificación.
 * Debe coincidir con el record `EtapaResponse` del backend Spring Boot.
 */
data class EtapaResponseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("orden") val orden: Int?,
    @SerializedName("tareas") val tareas: List<TareaResponseDto>?
)
