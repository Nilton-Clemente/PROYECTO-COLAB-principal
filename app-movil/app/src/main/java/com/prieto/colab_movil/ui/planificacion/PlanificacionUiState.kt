package com.prieto.colab_movil.ui.planificacion

import com.prieto.colab_movil.data.model.IntegranteResponseDto
import com.prieto.colab_movil.data.model.PlanificacionResponseDto
import com.prieto.colab_movil.data.model.ProyectoResponseDto

sealed interface PlanificacionUiState {
    object Loading : PlanificacionUiState
    object Generating : PlanificacionUiState
    object CheckingGeneration : PlanificacionUiState
    data class PendingGeneration(val message: String) : PlanificacionUiState

    data class Success(
        val planificacion: PlanificacionResponseDto,
        val proyecto: ProyectoResponseDto?,
        val equipo: List<IntegranteResponseDto>
    ) : PlanificacionUiState

    data class Error(val message: String, val fromGeneration: Boolean = false) : PlanificacionUiState
}
