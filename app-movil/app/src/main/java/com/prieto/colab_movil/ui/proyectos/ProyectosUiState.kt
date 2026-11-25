package com.prieto.colab_movil.ui.proyectos

import com.prieto.colab_movil.data.model.InvitacionResponseDto
import com.prieto.colab_movil.data.model.IntegranteResponseDto
import com.prieto.colab_movil.data.model.ProyectoResponseDto

sealed interface ProyectosUiState {
    object Loading : ProyectosUiState
    data class Success(
        val nombre: String,
        val proyectos: List<ProyectoResponseDto>,
        val invitaciones: List<InvitacionResponseDto>,
        val actualizando: Boolean = false,
        val aviso: String? = null,
        val equipos: Map<Long, List<IntegranteResponseDto>> = emptyMap()
    ) : ProyectosUiState
    data class Error(val message: String) : ProyectosUiState
}
