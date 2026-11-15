package com.prieto.colab_movil.ui.tareas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prieto.colab_movil.data.model.RegistroAvanceRequestDto
import com.prieto.colab_movil.data.repository.AvanceDatos
import com.prieto.colab_movil.data.repository.AvanceRepository
import com.prieto.colab_movil.data.repository.AvanceRepositoryImpl
import com.prieto.colab_movil.data.repository.RegistroAvanceInciertoException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class EstadoTarea(val titulo: String, val subtitulo: String) {
    PENDIENTE("Pendiente", "Sin iniciar"),
    EN_PROGRESO("En progreso", "Trabajo activo"),
    BLOQUEADA("Bloqueada", "Depende de otras tareas"),
    COMPLETADA("Completada", "Trabajo finalizado");

    companion object {
        fun transiciones(estadoActual: String?): List<EstadoTarea> = when (estadoActual) {
            "PENDIENTE" -> listOf(EN_PROGRESO)
            "EN_PROGRESO" -> listOf(PENDIENTE, COMPLETADA)
            else -> emptyList()
        }
    }
}

sealed interface RegistrarAvanceUiState {
    data object Loading : RegistrarAvanceUiState
    data class Error(val mensaje: String) : RegistrarAvanceUiState
    data class Success(
        val datos: AvanceDatos,
        val ocupado: Boolean = false,
        val desactualizado: Boolean = false,
        val registroIncierto: Boolean = false,
        val mensaje: String? = null
    ) : RegistrarAvanceUiState {
        val puedeGestionar: Boolean get() = !ocupado && !desactualizado && datos.planificacion.estado == "ACTIVA"
        val puedeRegistrar: Boolean get() = puedeGestionar && !registroIncierto
        val transiciones: List<EstadoTarea> get() = EstadoTarea.transiciones(datos.tarea.estado)
    }
}

class RegistrarAvanceViewModel(
    private val repository: AvanceRepository = AvanceRepositoryImpl()
) : ViewModel() {
    private val _uiState = MutableStateFlow<RegistrarAvanceUiState>(RegistrarAvanceUiState.Loading)
    val uiState = _uiState.asStateFlow()
    private var operacion: Job? = null

    fun cargar(proyectoId: Long, tareaId: Long) {
        if (operacion?.isActive == true) return
        operacion = viewModelScope.launch { actualizar(proyectoId, tareaId) }
    }

    fun registrar(proyectoId: Long, tareaId: Long, minutos: Int, nota: String, onGuardado: () -> Unit) {
        val estado = _uiState.value as? RegistrarAvanceUiState.Success ?: return
        if (!estado.puedeRegistrar || operacion?.isActive == true ||
            estado.datos.proyecto.id != proyectoId || estado.datos.tarea.id != tareaId) return
        val error = when {
            minutos !in 30..960 || minutos % 30 != 0 -> "Selecciona entre 0,5 y 16 horas en pasos de media hora."
            nota.trim().length !in 20..1000 -> "La descripción debe tener entre 20 y 1000 caracteres."
            else -> null
        }
        if (error != null) {
            _uiState.value = estado.copy(mensaje = error)
            return
        }
        _uiState.value = estado.copy(ocupado = true, mensaje = null)
        operacion = viewModelScope.launch {
            val resultado = repository.registrar(proyectoId, tareaId, RegistroAvanceRequestDto(minutos, nota.trim()))
            if (resultado.isSuccess) onGuardado()
            _uiState.value = estado.copy(
                ocupado = true,
                desactualizado = true,
                registroIncierto = resultado.exceptionOrNull() is RegistroAvanceInciertoException
            )
            actualizar(
                proyectoId, tareaId,
                if (resultado.isSuccess) "Avance guardado. El estado de la tarea no se ha modificado."
                else resultado.exceptionOrNull()?.message ?: "No se pudo guardar el avance."
            )
        }
    }

    fun cambiarEstado(proyectoId: Long, tareaId: Long, nuevo: EstadoTarea) {
        val estado = _uiState.value as? RegistrarAvanceUiState.Success ?: return
        if (!estado.puedeGestionar || nuevo !in estado.transiciones || operacion?.isActive == true ||
            estado.datos.proyecto.id != proyectoId || estado.datos.tarea.id != tareaId) return
        _uiState.value = estado.copy(ocupado = true, mensaje = null)
        operacion = viewModelScope.launch {
            val resultado = repository.cambiarEstado(proyectoId, tareaId, nuevo.name)
            _uiState.value = estado.copy(
                datos = estado.datos.copy(tarea = resultado.getOrNull() ?: estado.datos.tarea),
                ocupado = true, desactualizado = true
            )
            actualizar(
                proyectoId, tareaId,
                if (resultado.isSuccess) "Estado actualizado. No se ha registrado tiempo adicional."
                else resultado.exceptionOrNull()?.message ?: "No se pudo actualizar el estado."
            )
        }
    }

    fun confirmarHistorialRevisado() {
        val estado = _uiState.value as? RegistrarAvanceUiState.Success ?: return
        if (!estado.ocupado && !estado.desactualizado) {
            _uiState.value = estado.copy(registroIncierto = false, mensaje = null)
        }
    }

    fun limpiarMensaje() {
        val estado = _uiState.value as? RegistrarAvanceUiState.Success ?: return
        _uiState.value = estado.copy(mensaje = null)
    }

    private suspend fun actualizar(proyectoId: Long, tareaId: Long, mensaje: String? = null) {
        val anterior = _uiState.value as? RegistrarAvanceUiState.Success
        _uiState.value = anterior?.copy(ocupado = true, mensaje = null) ?: RegistrarAvanceUiState.Loading
        repository.cargar(proyectoId, tareaId).fold(
            onSuccess = {
                _uiState.value = RegistrarAvanceUiState.Success(
                    datos = it, registroIncierto = anterior?.registroIncierto == true, mensaje = mensaje
                )
            },
            onFailure = {
                val error = it.message ?: "No se pudo cargar la tarea y su historial."
                _uiState.value = anterior?.copy(
                    ocupado = false, desactualizado = true,
                    mensaje = listOfNotNull(mensaje, "$error Actualiza los datos para continuar.").joinToString("\n")
                ) ?: RegistrarAvanceUiState.Error(error)
            }
        )
    }
}
