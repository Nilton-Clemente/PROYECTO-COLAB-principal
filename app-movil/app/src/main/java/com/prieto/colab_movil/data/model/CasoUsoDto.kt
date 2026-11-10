package com.prieto.colab_movil.data.model

import com.google.gson.annotations.SerializedName

data class CasoUsoDto(
    @SerializedName("nombre")
    val nombre: String,
    @SerializedName("descripcion")
    val descripcion: String? = null,
    @SerializedName("actores")
    val actores: String? = null,
    @SerializedName("precondiciones")
    val precondiciones: String? = null,
    @SerializedName("disparador")
    val disparador: String? = null,
    @SerializedName("flujoPrincipal")
    val flujoPrincipal: String? = null,
    @SerializedName("flujosAlternativos")
    val flujosAlternativos: String? = null,
    @SerializedName("excepciones")
    val excepciones: String? = null,
    @SerializedName("postcondiciones")
    val postcondiciones: String? = null,
    @SerializedName("reglasDeNegocio")
    val reglasDeNegocio: String? = null
)
