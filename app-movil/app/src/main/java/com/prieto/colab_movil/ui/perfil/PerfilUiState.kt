package com.prieto.colab_movil.ui.perfil

import com.prieto.colab_movil.data.model.HabilidadDto

sealed interface PerfilUiState {
    object Loading : PerfilUiState
    data class Success(
        val nombre: String,
        val email: String,
        val habilidades: List<HabilidadDto>,
        val catalogo: List<HabilidadDto>
    ) : PerfilUiState
    data class Error(val message: String) : PerfilUiState
}
