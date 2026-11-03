package com.prieto.colab_movil.ui.proyectos

import com.prieto.colab_movil.data.model.ProyectoResponseDto

sealed interface EditarProyectoUiState {
    object Loading : EditarProyectoUiState
    data class Success(val proyecto: ProyectoResponseDto) : EditarProyectoUiState
    data class Error(val message: String) : EditarProyectoUiState
}