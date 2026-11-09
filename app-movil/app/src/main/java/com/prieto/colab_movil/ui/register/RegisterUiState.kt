package com.prieto.colab_movil.ui.register

sealed interface RegisterUiState {
    object Idle : RegisterUiState
    object Loading : RegisterUiState
    data class Success(val token: String) : RegisterUiState
    data class Error(val message: String, val isEmailRegistered: Boolean = false) : RegisterUiState
}
