package com.prieto.colab_movil.data.model

import com.google.gson.annotations.SerializedName

/** Datos requeridos por el CRUD manual de etapas del backend. */
data class EtapaRequestDto(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("orden") val orden: Int
)