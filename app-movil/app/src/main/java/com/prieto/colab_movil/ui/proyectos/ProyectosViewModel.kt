package com.prieto.colab_movil.ui.proyectos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prieto.colab_movil.data.repository.ProyectoRepository
import com.prieto.colab_movil.data.repository.ProyectoRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProyectosViewModel(
    private val repository: ProyectoRepository = ProyectoRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProyectosUiState>(ProyectosUiState.Loading)
    val uiState: StateFlow<ProyectosUiState> = _uiState.asStateFlow()

    fun cargarDatos() {
        _uiState.value = ProyectosUiState.Loading
        viewModelScope.launch {
            val perfilRes = repository.obtenerPerfil()
            val proyectosRes = repository.listarProyectos()
            val invitacionesRes = repository.listarInvitaciones()

            val nombre = perfilRes.getOrNull()?.nombre ?: "Usuario"
            val proyectos = proyectosRes.getOrNull() ?: emptyList()
            val invitaciones = invitacionesRes.getOrNull() ?: emptyList()

            if (perfilRes.isSuccess || proyectosRes.isSuccess) {
                _uiState.value = ProyectosUiState.Success(
                    nombre = nombre,
                    proyectos = proyectos,
                    invitaciones = invitaciones
                )
            } else {
                val error = proyectosRes.exceptionOrNull() ?: perfilRes.exceptionOrNull()
                _uiState.value = ProyectosUiState.Error(error?.message ?: "Error al cargar datos")
            }
        }
    }
}
