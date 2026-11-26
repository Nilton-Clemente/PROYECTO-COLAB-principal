package com.prieto.colab_movil.ui.tareas

import com.prieto.colab_movil.data.model.TareaDetalleResponseDto
import java.text.Normalizer
import java.util.Locale

internal enum class FiltroEstadoTareas(val titulo: String, val estado: String? = null) {
    TODAS("Todas"),
    PENDIENTES("Pendientes", "PENDIENTE"),
    EN_PROGRESO("En progreso", "EN_PROGRESO"),
    BLOQUEADAS("Bloqueadas", "BLOQUEADA"),
    COMPLETADAS("Completadas", "COMPLETADA")
}

internal fun filtrarMisTareas(
    tareas: List<TareaDetalleResponseDto>,
    busqueda: String,
    filtro: FiltroEstadoTareas = FiltroEstadoTareas.TODAS
): List<TareaDetalleResponseDto> {
    val termino = normalizarBusqueda(busqueda)
    return tareas.filter { tarea ->
        (filtro.estado == null || tarea.estado == filtro.estado) &&
            (termino.isEmpty() || normalizarBusqueda(
                "TK-${tarea.id} ${tarea.nombre} ${tarea.descripcion.orEmpty()} ${tarea.etapaNombre}"
            ).contains(termino))
    }
}

private fun normalizarBusqueda(valor: String): String = Normalizer.normalize(valor, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).trim()