package com.prieto.colab_movil.data.model

enum class EstadoProyecto(val titulo: String) {
    SIN_PLANIFICACION("Sin planificación"),
    PLANIFICACION_PROPUESTA("Planificación propuesta"),
    EN_DESARROLLO("En desarrollo"),
    FINALIZADO("Finalizado"),
    CANCELADO("Cancelado"),
    DESCONOCIDO("Estado no disponible");

    val cerrado: Boolean get() = this == FINALIZADO || this == CANCELADO

    companion object {
        fun desdeApi(valor: String?): EstadoProyecto = entries.firstOrNull { it.name == valor } ?: DESCONOCIDO
    }
}

val ProyectoResponseDto.estadoProyecto: EstadoProyecto get() = EstadoProyecto.desdeApi(estado)