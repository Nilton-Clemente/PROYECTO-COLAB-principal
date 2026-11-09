package com.prieto.colab_movil.ui.proyectos

import com.prieto.colab_movil.data.model.IntegranteResponseDto
import com.prieto.colab_movil.data.model.ProyectoResponseDto

sealed interface DetalleProyectoUiState {
    object Loading : DetalleProyectoUiState
    data class Success(
        val proyecto: ProyectoResponseDto,
        val equipo: List<IntegranteResponseDto>
    ) : DetalleProyectoUiState
    data class Error(val message: String) : DetalleProyectoUiState
}
