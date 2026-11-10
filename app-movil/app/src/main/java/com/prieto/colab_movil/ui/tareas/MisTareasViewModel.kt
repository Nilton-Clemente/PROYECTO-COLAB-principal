package com.prieto.colab_movil.ui.tareas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prieto.colab_movil.data.model.ConfirmacionResponseDto
import com.prieto.colab_movil.data.repository.MisTareasDatos
import com.prieto.colab_movil.data.repository.MisTareasRepository
import com.prieto.colab_movil.data.repository.MisTareasRepositoryImpl
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class FiltroTareas(val titulo: String) {
    TODAS("Todas"), POR_CONFIRMAR("Por confirmar"), CONFIRMADAS("Confirmadas")
}

sealed interface MisTareasUiState {
    data object Loading : MisTareasUiState
    data class Error(val mensaje: String) : MisTareasUiState
    data class Success(
        val datos: MisTareasDatos,
        val isSaving: Boolean = false,
        val isRefreshing: Boolean = false,
        val datosDesactualizados: Boolean = false,
        val mensaje: String? = null
    ) : MisTareasUiState {
        val activa: Boolean get() = datos.planificacion?.estado == "ACTIVA"
        val confirmadas: Boolean get() = activa || datos.confirmacion?.estado == "ACEPTADA"
        val ocupado: Boolean get() = isSaving || isRefreshing
        val puedeRegistrarAvance: Boolean get() = activa && !ocupado && !datosDesactualizados && datos.tareas.isNotEmpty()
        val puedeConfirmar: Boolean get() = puedeSolicitarCambios && !confirmadas
        val puedeSolicitarCambios: Boolean get() = !ocupado && !datosDesactualizados &&
            datos.planificacion?.estado == "PROPUESTA" && datos.tareas.isNotEmpty()

        fun tareasVisibles(filtro: FiltroTareas) = when (filtro) {
            FiltroTareas.TODAS -> datos.tareas
            FiltroTareas.POR_CONFIRMAR -> if (confirmadas) emptyList() else datos.tareas
            FiltroTareas.CONFIRMADAS -> if (confirmadas) datos.tareas else emptyList()
        }
    }
}

class MisTareasViewModel(
    private val repository: MisTareasRepository = MisTareasRepositoryImpl()
) : ViewModel() {
    private val _uiState = MutableStateFlow<MisTareasUiState>(MisTareasUiState.Loading)
    val uiState = _uiState.asStateFlow()
    private var operacion: Job? = null

    fun cargar(proyectoId: Long) {
        if (operacion?.isActive == true) return
        operacion = viewModelScope.launch { actualizar(proyectoId) }
    }

    fun confirmar(proyectoId: Long) {
        val estado = _uiState.value as? MisTareasUiState.Success ?: return
        if (!estado.puedeConfirmar) return
        guardar(proyectoId, "Tus tareas fueron confirmadas.") { repository.confirmar(proyectoId) }
    }

    fun solicitarCambios(proyectoId: Long, tareaId: Long, comentario: String, onEnviado: () -> Unit) {
        val estado = _uiState.value as? MisTareasUiState.Success ?: return
        if (!estado.puedeSolicitarCambios) return
        val tarea = estado.datos.tareas.firstOrNull { it.id == tareaId } ?: return
        if (comentario.isBlank()) {
            _uiState.value = estado.copy(mensaje = "Describe los motivos del ajuste antes de enviarlo.")
            return
        }
        guardar(proyectoId, "Solicitud enviada al creador del proyecto.", onEnviado) {
            repository.solicitarCambios(proyectoId, "TK-${tarea.id} · ${tarea.nombre}\n${comentario.trim()}")
        }
    }

    fun limpiarMensaje() {
        val estado = _uiState.value as? MisTareasUiState.Success ?: return
        _uiState.value = estado.copy(mensaje = null)
    }

    private fun guardar(
        proyectoId: Long,
        mensajeExitoso: String,
        onExito: () -> Unit = {},
        accion: suspend () -> Result<ConfirmacionResponseDto>
    ) {
        if (operacion?.isActive == true) return
        val estado = _uiState.value as? MisTareasUiState.Success ?: return
        _uiState.value = estado.copy(isSaving = true, mensaje = null)
        operacion = viewModelScope.launch {
            val resultado = accion()
            val confirmacion = resultado.getOrNull()
            _uiState.value = estado.copy(
                datos = estado.datos.copy(confirmacion = confirmacion ?: estado.datos.confirmacion),
                isRefreshing = true,
                datosDesactualizados = true
            )
            if (resultado.isSuccess) onExito()
            // Recargar incluso tras errores: una respuesta perdida no significa que no se guardó.
            actualizar(
                proyectoId,
                if (resultado.isSuccess) mensajeExitoso else resultado.exceptionOrNull()?.message
                    ?: "No se pudo completar la operación."
            )
        }
    }

    private suspend fun actualizar(proyectoId: Long, mensaje: String? = null) {
        val anterior = _uiState.value as? MisTareasUiState.Success
        _uiState.value = anterior?.copy(isRefreshing = true, mensaje = null) ?: MisTareasUiState.Loading
        repository.cargar(proyectoId).fold(
            onSuccess = { _uiState.value = MisTareasUiState.Success(it, mensaje = mensaje) },
            onFailure = {
                val error = it.message ?: "No se pudieron cargar tus tareas."
                _uiState.value = anterior?.copy(
                    isSaving = false,
                    isRefreshing = false,
                    datosDesactualizados = true,
                    mensaje = listOfNotNull(mensaje, "$error Actualiza para continuar.").joinToString("\n")
                ) ?: MisTareasUiState.Error(error)
            }
        )
    }
}
