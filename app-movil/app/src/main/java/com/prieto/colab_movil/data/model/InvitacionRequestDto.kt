package com.prieto.colab_movil.data.model

import com.google.gson.annotations.SerializedName

data class InvitacionRequestDto(
    @SerializedName("email") val email: String
)