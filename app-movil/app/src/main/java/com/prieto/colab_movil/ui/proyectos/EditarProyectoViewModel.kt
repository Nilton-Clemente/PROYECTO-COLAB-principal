package com.prieto.colab_movil.ui.proyectos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prieto.colab_movil.data.repository.ProyectoRepository
import com.prieto.colab_movil.data.repository.ProyectoRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditarProyectoViewModel(
    private val repository: ProyectoRepository = ProyectoRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditarProyectoUiState>(EditarProyectoUiState.Loading)
    val uiState: StateFlow<EditarProyectoUiState> = _uiState.asStateFlow()

    fun cargarProyecto(proyectoId: Long) {
        _uiState.value = EditarProyectoUiState.Loading
        viewModelScope.launch {
            repository.verProyecto(proyectoId)
                .onSuccess { proyecto ->
                    _uiState.value = EditarProyectoUiState.Success(proyecto)
                }
                .onFailure { e ->
                    _uiState.value = EditarProyectoUiState.Error(e.message ?: "No se pudo cargar el proyecto.")
                }
        }
    }
}