package com.prieto.colab_movil.ui.invitaciones

import com.prieto.colab_movil.data.model.InvitacionResponseDto

sealed interface InvitacionesUiState {
    object Loading : InvitacionesUiState
    data class Success(val invitaciones: List<InvitacionResponseDto>) : InvitacionesUiState
    data class Error(val message: String) : InvitacionesUiState
}
