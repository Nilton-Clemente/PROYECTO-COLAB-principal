package com.prieto.colab_movil.data.model

import com.google.gson.annotations.SerializedName

data class HabilidadDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("nombre")
    val nombre: String,
    @SerializedName("area")
    val area: String?
)
