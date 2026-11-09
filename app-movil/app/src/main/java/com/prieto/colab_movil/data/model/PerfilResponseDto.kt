package com.prieto.colab_movil.data.model

import com.google.gson.annotations.SerializedName

data class PerfilResponseDto(
    @SerializedName("email")
    val email: String,
    @SerializedName("nombre")
    val nombre: String,
    @SerializedName("habilidades")
    val habilidades: List<HabilidadDto>
)
