package com.prieto.colab_movil.data.model

import com.google.gson.annotations.SerializedName

data class IntegranteResponseDto(
    @SerializedName("integranteId")
    val integranteId: Long,
    @SerializedName("email")
    val email: String,
    @SerializedName("nombre")
    val nombre: String,
    @SerializedName("rol")
    val rol: String?,
    @SerializedName("estado")
    val estado: String?
)
