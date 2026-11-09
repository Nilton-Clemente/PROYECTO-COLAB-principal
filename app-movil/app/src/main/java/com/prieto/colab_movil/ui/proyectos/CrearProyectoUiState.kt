package com.prieto.colab_movil.ui.proyectos

sealed interface CrearProyectoUiState {
    object Idle : CrearProyectoUiState
    object Cargando : CrearProyectoUiState
    data class Exito(val proyectoId: Long) : CrearProyectoUiState
    data class Error(val message: String) : CrearProyectoUiState
}
