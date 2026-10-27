package com.prieto.colab_movil.data.model

import com.google.gson.annotations.SerializedName

data class InvitacionResponseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("proyectoId") val proyectoId: Long,
    @SerializedName("proyectoNombre") val proyectoNombre: String
)
