package com.prieto.colab_movil.data.model

import com.google.gson.annotations.SerializedName

/**
 * Solicitud del guardado atómico de una planificación editada (CU-09).
 * Espeja el record `PlanificacionUpdateRequest` del backend Spring Boot.
 */
data class PlanificacionUpdateRequestDto(
    @SerializedName("etapas") val etapas: List<EtapaUpdateDto>
) {
    data class EtapaUpdateDto(
        @SerializedName("id") val id: Long?,
        @SerializedName("nombre") val nombre: String,
        @SerializedName("orden") val orden: Int?,
        @SerializedName("tareas") val tareas: List<TareaUpdateDto>
    )

    data class TareaUpdateDto(
        @SerializedName("id") val id: Long?,
        @SerializedName("key") val key: String,
        @SerializedName("nombre") val nombre: String,
        @SerializedName("descripcion") val descripcion: String?,
        @SerializedName("responsableId") val responsableId: Long?,
        @SerializedName("prioridad") val prioridad: String,
        @SerializedName("duracionDias") val duracionDias: Int,
        @SerializedName("dependencias") val dependencias: List<String>
    )
}
