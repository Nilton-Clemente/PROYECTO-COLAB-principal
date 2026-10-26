package com.prieto.colab_movil.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prieto.colab_movil.data.repository.PerfilRepository
import com.prieto.colab_movil.data.repository.PerfilRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PerfilViewModel(
    private val repository: PerfilRepository = PerfilRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<PerfilUiState>(PerfilUiState.Loading)
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _uiState.value = PerfilUiState.Loading
        refrescar()
    }

    private fun refrescar() {
        viewModelScope.launch {
            val perfil = repository.obtenerPerfil()
            val catalogo = repository.listarCatalogo()

            val p = perfil.getOrNull()
            val c = catalogo.getOrNull()
            if (p != null && c != null) {
                _uiState.value = PerfilUiState.Success(
                    nombre = p.nombre,
                    email = p.email,
                    habilidades = p.habilidades,
                    catalogo = c
                )
            } else {
                val error = perfil.exceptionOrNull() ?: catalogo.exceptionOrNull()
                _uiState.value = PerfilUiState.Error(
                    error?.message ?: "No se pudo cargar el perfil."
                )
            }
        }
    }

    fun agregarHabilidad(habilidadId: Long) {
        viewModelScope.launch {
            repository.agregarHabilidad(habilidadId)
            refrescar()
        }
    }

    fun eliminarHabilidad(habilidadId: Long) {
        viewModelScope.launch {
            repository.eliminarHabilidad(habilidadId)
            refrescar()
        }
    }
}
