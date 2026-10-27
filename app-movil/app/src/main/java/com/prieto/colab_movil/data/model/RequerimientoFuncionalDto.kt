package com.prieto.colab_movil.data.model

import com.google.gson.annotations.SerializedName

data class RequerimientoFuncionalDto(
    @SerializedName("codigo")
    val codigo: String?,
    @SerializedName("descripcion")
    val descripcion: String
)
