package com.prieto.colab_movil.ui.invitaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prieto.colab_movil.data.repository.ProyectoRepository
import com.prieto.colab_movil.data.repository.ProyectoRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InvitacionesViewModel(
    private val repository: ProyectoRepository = ProyectoRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<InvitacionesUiState>(InvitacionesUiState.Loading)
    val uiState: StateFlow<InvitacionesUiState> = _uiState.asStateFlow()

    // Mensaje de una sola lectura (para el Snackbar).
    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _uiState.value = InvitacionesUiState.Loading
        viewModelScope.launch {
            repository.listarInvitaciones()
                .onSuccess { invitaciones ->
                    _uiState.value = InvitacionesUiState.Success(invitaciones)
                }
                .onFailure { e ->
                    _uiState.value = InvitacionesUiState.Error(
                        e.message ?: "No se pudieron cargar las invitaciones."
                    )
                }
        }
    }

    fun aceptar(id: Long) {
        viewModelScope.launch {
            repository.aceptarInvitacion(id)
                .onSuccess {
                    _mensaje.value = "Te uniste al proyecto"
                    cargar()
                }
                .onFailure { e ->
                    _mensaje.value = e.message ?: "No se pudo aceptar la invitación"
                }
        }
    }

    fun rechazar(id: Long) {
        viewModelScope.launch {
            repository.rechazarInvitacion(id)
                .onSuccess {
                    _mensaje.value = "Invitación declinada"
                    cargar()
                }
                .onFailure { e ->
                    _mensaje.value = e.message ?: "No se pudo declinar la invitación"
                }
        }
    }

    fun consumirMensaje() {
        _mensaje.value = null
    }
}
