package com.prieto.colab_movil.ui.invitaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prieto.colab_movil.data.repository.ProyectoRepository
import com.prieto.colab_movil.data.repository.ProyectoRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class InvitacionesViewModel(
    private val repository: ProyectoRepository = ProyectoRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<InvitacionesUiState>(InvitacionesUiState.Loading)
    val uiState: StateFlow<InvitacionesUiState> = _uiState.asStateFlow()

    // Mensaje de una sola lectura (para el Snackbar).
    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    private var operacion: Job? = null

    init {
        cargar()
    }

    fun cargar() {
        if (operacion?.isActive == true) return
        val anterior = _uiState.value as? InvitacionesUiState.Success
        _uiState.value = anterior?.copy(actualizando = true) ?: InvitacionesUiState.Loading
        operacion = viewModelScope.launch { consultar(anterior) }
    }

    private suspend fun consultar(anterior: InvitacionesUiState.Success?) {
        repository.listarInvitaciones().fold(
            onSuccess = { _uiState.value = InvitacionesUiState.Success(it) },
            onFailure = { error ->
                if (anterior == null) {
                    _uiState.value = InvitacionesUiState.Error(error.message ?: "No se pudieron cargar las invitaciones.")
                } else {
                    _uiState.value = anterior.copy(actualizando = false, ocupado = false, desactualizado = true)
                    _mensaje.value = "No se pudieron actualizar las invitaciones. Inténtalo de nuevo."
                }
            }
        )
    }

    fun aceptar(id: Long) = gestionar(id, true)
    fun rechazar(id: Long) = gestionar(id, false)

    private fun gestionar(id: Long, aceptar: Boolean) {
        val anterior = _uiState.value as? InvitacionesUiState.Success ?: return
        if (operacion?.isActive == true || anterior.ocupado || anterior.desactualizado) return
        _uiState.value = anterior.copy(ocupado = true)
        operacion = viewModelScope.launch {
            val resultado = if (aceptar) repository.aceptarInvitacion(id) else repository.rechazarInvitacion(id)
            _mensaje.value = if (resultado.isSuccess) {
                if (aceptar) "Te uniste al proyecto" else "Invitación declinada"
            } else resultado.exceptionOrNull()?.message ?: "No se pudo procesar la invitación"
            consultar(anterior.copy(desactualizado = true))
        }
    }

    fun consumirMensaje() {
        _mensaje.value = null
    }
}
