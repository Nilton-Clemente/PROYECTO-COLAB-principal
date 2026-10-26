package com.prieto.colab_movil.data.model

import com.google.gson.annotations.SerializedName

data class AuthResponseDto(
    @SerializedName("token")
    val token: String
)
