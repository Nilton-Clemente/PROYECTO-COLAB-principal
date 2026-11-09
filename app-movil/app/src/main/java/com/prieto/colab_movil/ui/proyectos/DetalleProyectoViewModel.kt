package com.prieto.colab_movil.ui.proyectos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prieto.colab_movil.data.repository.ProyectoRepository
import com.prieto.colab_movil.data.repository.ProyectoRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetalleProyectoViewModel(
    private val repository: ProyectoRepository = ProyectoRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetalleProyectoUiState>(DetalleProyectoUiState.Loading)
    val uiState: StateFlow<DetalleProyectoUiState> = _uiState.asStateFlow()

    fun cargarDetalle(proyectoId: Long) {
        _uiState.value = DetalleProyectoUiState.Loading
        viewModelScope.launch {
            val proyectoRes = repository.verProyecto(proyectoId)
            val equipoRes = repository.verEquipo(proyectoId)

            if (proyectoRes.isSuccess && proyectoRes.getOrNull() != null) {
                _uiState.value = DetalleProyectoUiState.Success(
                    proyecto = proyectoRes.getOrNull()!!,
                    equipo = equipoRes.getOrNull() ?: emptyList()
                )
            } else {
                val error = proyectoRes.exceptionOrNull()?.message ?: "No se pudo cargar la información del proyecto."
                _uiState.value = DetalleProyectoUiState.Error(error)
            }
        }
    }
}
