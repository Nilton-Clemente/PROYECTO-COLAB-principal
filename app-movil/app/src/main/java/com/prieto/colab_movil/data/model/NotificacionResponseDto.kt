package com.prieto.colab_movil.data.model

import com.google.gson.annotations.SerializedName

data class NotificacionResponseDto(
    @SerializedName("id") val id: Long,
    @SerializedName("tipoEvento") val tipoEvento: String?,
    @SerializedName("mensaje") val mensaje: String,
    @SerializedName("leida") val leida: Boolean,
    @SerializedName("fechaCreacion") val fechaCreacion: String?,
    @SerializedName("proyectoId") val proyectoId: Long? = null,
    @SerializedName("proyectoNombre") val proyectoNombre: String? = null,
    @SerializedName("proyectoCodigo") val proyectoCodigo: String? = null
)