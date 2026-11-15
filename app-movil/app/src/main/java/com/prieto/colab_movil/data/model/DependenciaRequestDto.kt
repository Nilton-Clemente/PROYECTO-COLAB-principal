package com.prieto.colab_movil.data.model

import com.google.gson.annotations.SerializedName

/** Identificador de la tarea predecesora de una dependencia. */
data class DependenciaRequestDto(
    @SerializedName("tareaPredecesoraId") val tareaPredecesoraId: Long
)