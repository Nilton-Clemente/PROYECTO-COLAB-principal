package com.prieto.colab_movil.data.model

import com.google.gson.annotations.SerializedName

data class RequerimientoNoFuncionalDto(
    @SerializedName("codigo")
    val codigo: String?,
    @SerializedName("descripcion")
    val descripcion: String,
    @SerializedName("categoria")
    val categoria: String?
)
