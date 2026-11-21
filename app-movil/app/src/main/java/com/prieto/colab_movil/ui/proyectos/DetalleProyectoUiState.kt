package com.prieto.colab_movil.ui.proyectos

import com.prieto.colab_movil.data.model.IntegranteResponseDto
import com.prieto.colab_movil.data.model.ProyectoResponseDto
import com.prieto.colab_movil.data.model.EstadoProyecto
import com.prieto.colab_movil.data.model.estadoProyecto

enum class AccionProyecto(val titulo: String) {
    EDITAR("Editar proyecto"), CANCELAR("Cancelar proyecto"), ELIMINAR("Eliminar proyecto")
}

internal fun confirmaNombreProyecto(nombre: String, confirmacion: String) =
    nombre.isNotBlank() && nombre.trim() == confirmacion.trim()

sealed interface DetalleProyectoUiState {
    object Loading : DetalleProyectoUiState
    data class Eliminado(val proyectoId: Long) : DetalleProyectoUiState
    data class Success(
        val proyecto: ProyectoResponseDto,
        val equipo: List<IntegranteResponseDto>,
        val tienePlanificacion: Boolean,
        val actualizando: Boolean = false,
        val datosDesactualizados: Boolean = false,
        val aviso: String? = null,
        val soyCreador: Boolean = false,
        val accionEnCurso: AccionProyecto? = null
    ) : DetalleProyectoUiState {
        val acciones: List<AccionProyecto> get() = buildList {
            if (soyCreador && !tienePlanificacion && proyecto.estadoProyecto == EstadoProyecto.SIN_PLANIFICACION) add(AccionProyecto.EDITAR)
            if (proyecto.puedeCancelar) add(AccionProyecto.CANCELAR)
            if (proyecto.puedeEliminar) add(AccionProyecto.ELIMINAR)
        }
        val puedeModificar: Boolean get() = !actualizando && !datosDesactualizados && accionEnCurso == null
    }
    data class Error(val message: String) : DetalleProyectoUiState
}
