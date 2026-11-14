package com.prieto.colab_movil.data.model

import com.google.gson.annotations.SerializedName

data class RegistroAvanceRequestDto(
    @SerializedName("minutos") val minutos: Int,
    @SerializedName("nota") val nota: String
)

data class RegistroAvanceResponseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("tareaId") val tareaId: Long,
    @SerializedName("usuarioId") val usuarioId: Long,
    @SerializedName("usuarioNombre") val usuarioNombre: String,
    @SerializedName("minutos") val minutos: Int,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("nota") val nota: String?
)

data class HistorialAvanceResponseDto(
    @SerializedName("totalMinutos") val totalMinutos: Long,
    @SerializedName("registros") val registros: List<RegistroAvanceResponseDto>
)

data class TareaEstadoRequestDto(
    @SerializedName("nuevoEstado") val nuevoEstado: String
)
