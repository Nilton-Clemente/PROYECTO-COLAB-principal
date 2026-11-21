package com.prieto.colab_movil.ui.proyectos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prieto.colab_movil.data.repository.ProyectoRepository
import com.prieto.colab_movil.data.repository.ProyectoRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class ProyectosViewModel(
    private val repository: ProyectoRepository = ProyectoRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProyectosUiState>(ProyectosUiState.Loading)
    val uiState: StateFlow<ProyectosUiState> = _uiState.asStateFlow()

    private var operacion: Job? = null
    private val eliminados = mutableSetOf<Long>()

    fun retirarEliminado(id: Long) {
        eliminados.add(id)
        (_uiState.value as? ProyectosUiState.Success)?.let { anterior ->
            _uiState.value = anterior.copy(proyectos = anterior.proyectos.filterNot { it.id == id })
        }
        cargarDatos()
    }

    fun cargarDatos() {
        if (operacion?.isActive == true) return
        val anterior = _uiState.value as? ProyectosUiState.Success
        _uiState.value = anterior?.copy(actualizando = true, aviso = null) ?: ProyectosUiState.Loading
        operacion = viewModelScope.launch {
            val perfilRes = repository.obtenerPerfil()
            val proyectosRes = repository.listarProyectos()
            val invitacionesRes = repository.listarInvitaciones()

            if (anterior != null || (perfilRes.isSuccess && proyectosRes.isSuccess && invitacionesRes.isSuccess)) {
                _uiState.value = ProyectosUiState.Success(
                    nombre = perfilRes.getOrNull()?.nombre ?: anterior!!.nombre,
                    proyectos = (proyectosRes.getOrNull() ?: anterior!!.proyectos).filterNot { it.id in eliminados },
                    invitaciones = invitacionesRes.getOrNull() ?: anterior!!.invitaciones,
                    aviso = if (perfilRes.isFailure || proyectosRes.isFailure || invitacionesRes.isFailure)
                        "No se pudieron actualizar todos los datos. Inténtalo de nuevo." else null
                )
            } else {
                val error = proyectosRes.exceptionOrNull() ?: perfilRes.exceptionOrNull() ?: invitacionesRes.exceptionOrNull()
                _uiState.value = ProyectosUiState.Error(error?.message ?: "Error al cargar datos")
            }
        }
    }
}
