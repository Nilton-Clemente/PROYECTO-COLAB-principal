package com.prieto.colab_movil.ui.proyectos

import com.prieto.colab_movil.data.model.InvitacionResponseDto
import com.prieto.colab_movil.data.model.ProyectoResponseDto

sealed interface ProyectosUiState {
    object Loading : ProyectosUiState
    data class Success(
        val nombre: String,
        val proyectos: List<ProyectoResponseDto>,
        val invitaciones: List<InvitacionResponseDto>
    ) : ProyectosUiState
    data class Error(val message: String) : ProyectosUiState
}
