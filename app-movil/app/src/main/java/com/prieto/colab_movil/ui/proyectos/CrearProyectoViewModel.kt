package com.prieto.colab_movil.ui.proyectos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prieto.colab_movil.data.model.ProyectoRequest
import com.prieto.colab_movil.data.model.TecnologiaDto
import com.prieto.colab_movil.data.repository.ProyectoRepository
import com.prieto.colab_movil.data.repository.ProyectoRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CrearProyectoViewModel(
    private val repository: ProyectoRepository = ProyectoRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<CrearProyectoUiState>(CrearProyectoUiState.Idle)
    val uiState: StateFlow<CrearProyectoUiState> = _uiState.asStateFlow()

    private val _tecnologias = MutableStateFlow<List<TecnologiaDto>>(emptyList())
    val tecnologias: StateFlow<List<TecnologiaDto>> = _tecnologias.asStateFlow()

    init {
        cargarTecnologias()
    }

    fun cargarTecnologias() {
        viewModelScope.launch {
            _tecnologias.value = repository.listarTecnologias().getOrNull() ?: emptyList()
        }
    }

    fun crearProyecto(request: ProyectoRequest) {
        if (_uiState.value is CrearProyectoUiState.Cargando) return
        _uiState.value = CrearProyectoUiState.Cargando
        viewModelScope.launch {
            repository.crearProyecto(request)
                .onSuccess { proyecto ->
                    _uiState.value = CrearProyectoUiState.Exito(proyecto.id)
                }
                .onFailure { e ->
                    _uiState.value = CrearProyectoUiState.Error(e.message ?: "Error al guardar el proyecto")
                }
        }
    }

    fun resetState() {
        _uiState.value = CrearProyectoUiState.Idle
    }
}
