package com.prieto.colab_movil.data.model

import com.google.gson.annotations.SerializedName

/**
 * Respuesta de la planificación ya persistida (CU-08).
 * Debe coincidir con el record `PlanificacionResponse` del backend Spring Boot.
 */
data class PlanificacionResponseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("proyectoId") val proyectoId: Long,
    @SerializedName("estado") val estado: String?,
    @SerializedName("etapas") val etapas: List<EtapaResponseDto>?
)
