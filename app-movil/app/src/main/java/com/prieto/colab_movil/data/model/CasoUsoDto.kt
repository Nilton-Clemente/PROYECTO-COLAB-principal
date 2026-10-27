package com.prieto.colab_movil.data.model

import com.google.gson.annotations.SerializedName

data class CasoUsoDto(
    @SerializedName("codigo")
    val codigo: String?,
    @SerializedName("nombre")
    val nombre: String,
    @SerializedName("descripcion")
    val descripcion: String?
)
