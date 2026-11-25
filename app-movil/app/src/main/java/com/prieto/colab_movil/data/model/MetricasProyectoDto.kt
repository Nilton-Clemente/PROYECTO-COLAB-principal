package com.prieto.colab_movil.data.model

import com.google.gson.annotations.SerializedName

data class MetricasProyectoDto(
    @SerializedName("totalTareas") val totalTareas: Long,
    @SerializedName("pendientes") val pendientes: Long,
    @SerializedName("enProgreso") val enProgreso: Long,
    @SerializedName("completadas") val completadas: Long,
    @SerializedName("bloqueadas") val bloqueadas: Long,
    @SerializedName("porcentajeCompletadas") val porcentajeCompletadas: Double,
    @SerializedName("porResponsable") val porResponsable: List<MetricasIntegranteDto>?
)

data class MetricasIntegranteDto(
    @SerializedName("integranteId") val integranteId: Long,
    @SerializedName("email") val email: String?,
    @SerializedName("totalTareas") val totalTareas: Long,
    @SerializedName("completadas") val completadas: Long
)