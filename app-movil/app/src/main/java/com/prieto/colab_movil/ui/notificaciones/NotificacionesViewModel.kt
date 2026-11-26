package com.prieto.colab_movil.ui.notificaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prieto.colab_movil.data.model.NotificacionResponseDto
import com.prieto.colab_movil.data.repository.NotificacionesRepository
import com.prieto.colab_movil.data.repository.NotificacionesRepositoryImpl
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface NotificacionesUiState {
    data object Loading : NotificacionesUiState
    data class Error(val mensaje: String) : NotificacionesUiState
    data class Success(
        val notificaciones: List<NotificacionResponseDto>,
        val actualizando: Boolean = false,
        val marcandoId: Long? = null,
        val eliminandoLeidas: Boolean = false,
        val desactualizado: Boolean = false,
        val aviso: String? = null
    ) : NotificacionesUiState {
        val noLeidas: Int get() = notificaciones.count { !it.leida }
        val ocupado: Boolean get() = actualizando || marcandoId != null || eliminandoLeidas
        val puedeEliminarLeidas: Boolean get() = !ocupado && !desactualizado && notificaciones.any { it.leida }
    }
}

class NotificacionesViewModel(
    private val repository: NotificacionesRepository = NotificacionesRepositoryImpl()
) : ViewModel() {
    private val _uiState = MutableStateFlow<NotificacionesUiState>(NotificacionesUiState.Loading)
    val uiState = _uiState.asStateFlow()
    private var operacion: Job? = null

    fun cargar() {
        if (operacion?.isActive == true) return
        val anterior = _uiState.value as? NotificacionesUiState.Success
        _uiState.value = anterior?.copy(actualizando = true, aviso = null) ?: NotificacionesUiState.Loading
        operacion = viewModelScope.launch { consultar(anterior) }
    }

    fun marcarLeida(id: Long) {
        val anterior = _uiState.value as? NotificacionesUiState.Success ?: return
        if (operacion?.isActive == true || anterior.ocupado || anterior.desactualizado ||
            anterior.notificaciones.none { it.id == id && !it.leida }) return
        _uiState.value = anterior.copy(marcandoId = id, aviso = null)
        operacion = viewModelScope.launch {
            repository.marcarLeida(id).fold(
                onSuccess = { leida ->
                    _uiState.value = anterior.copy(notificaciones = anterior.notificaciones.map { if (it.id == id) leida else it })
                },
                onFailure = {
                    // La lectura pudo guardarse aunque se perdiera la respuesta; volver a consultar.
                    val aviso = it.message ?: "No se pudo confirmar la lectura."
                    _uiState.value = anterior.copy(actualizando = true, desactualizado = true, aviso = aviso)
                    consultar(anterior, aviso)
                }
            )
        }
    }

    fun eliminarLeidas() {
        val anterior = _uiState.value as? NotificacionesUiState.Success ?: return
        if (operacion?.isActive == true || !anterior.puedeEliminarLeidas) return
        _uiState.value = anterior.copy(eliminandoLeidas = true, aviso = null)
        operacion = viewModelScope.launch {
            repository.eliminarLeidas().fold(
                onSuccess = {
                    // El servidor confirmó el borrado; no restaurar las leídas si falla la recarga.
                    val actualizado = anterior.copy(notificaciones = anterior.notificaciones.filterNot { it.leida }, aviso = null)
                    _uiState.value = actualizado.copy(actualizando = true)
                    consultar(actualizado)
                },
                onFailure = {
                    // El borrado pudo aplicarse aunque se perdiera la respuesta. No repetirlo automáticamente.
                    val aviso = it.message ?: "No se pudo confirmar la eliminación de las notificaciones leídas."
                    _uiState.value = anterior.copy(actualizando = true, desactualizado = true, aviso = aviso)
                    consultar(anterior, aviso)
                }
            )
        }
    }

    private suspend fun consultar(anterior: NotificacionesUiState.Success?, aviso: String? = null) {
        repository.listar().fold(
            onSuccess = { _uiState.value = NotificacionesUiState.Success(it, aviso = aviso) },
            onFailure = {
                val mensaje = listOfNotNull(aviso, it.message ?: "No se pudieron cargar las notificaciones.").joinToString("\n")
                _uiState.value = anterior?.copy(
                    actualizando = false, marcandoId = null, eliminandoLeidas = false, desactualizado = true,
                    aviso = "$mensaje Desliza para reintentar."
                ) ?: NotificacionesUiState.Error(mensaje)
            }
        )
    }
}