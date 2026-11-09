package com.prieto.colab_movil.data.model

import com.google.gson.annotations.SerializedName

data class RegistroRequestDto(
    @SerializedName("email")
    val email: String,
    @SerializedName("contrasena")
    val contrasena: String,
    @SerializedName("nombre")
    val nombre: String
)
