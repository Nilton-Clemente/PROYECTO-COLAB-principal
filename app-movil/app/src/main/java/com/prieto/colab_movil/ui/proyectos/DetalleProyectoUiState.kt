package com.prieto.colab_movil.ui.proyectos

import com.prieto.colab_movil.data.model.IntegranteResponseDto
import com.prieto.colab_movil.data.model.ProyectoResponseDto

sealed interface DetalleProyectoUiState {
    object Loading : DetalleProyectoUiState
    data class Success(
        val proyecto: ProyectoResponseDto,
        val equipo: List<IntegranteResponseDto>,
        val tienePlanificacion: Boolean,
        val actualizando: Boolean = false,
        val datosDesactualizados: Boolean = false,
        val aviso: String? = null
    ) : DetalleProyectoUiState
    data class Error(val message: String) : DetalleProyectoUiState
}
