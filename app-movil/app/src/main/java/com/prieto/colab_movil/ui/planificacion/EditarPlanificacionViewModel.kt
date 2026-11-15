package com.prieto.colab_movil.ui.planificacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prieto.colab_movil.data.model.IntegranteResponseDto
import com.prieto.colab_movil.data.model.PlanificacionResponseDto
import com.prieto.colab_movil.data.model.PlanificacionUpdateRequestDto
import com.prieto.colab_movil.data.repository.PlanificacionRepository
import com.prieto.colab_movil.data.repository.PlanificacionRepositoryImpl
import com.prieto.colab_movil.data.repository.ProyectoRepository
import com.prieto.colab_movil.data.repository.ProyectoRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EtapaEditableUi(
    val localId: String,
    val remoteId: Long?,
    val nombre: String,
    val tareas: List<TareaEditableUi>
)

data class TareaEditableUi(
    val localId: String,
    val remoteId: Long?,
    val nombre: String,
    val descripcion: String,
    val responsableId: Long?,
    val prioridad: String,
    val duracionDias: String,
    val dependencias: List<String>,
    val dependenciasOriginales: Set<Long> = emptySet()
)

sealed interface EditarPlanificacionUiState {
    data object Loading : EditarPlanificacionUiState
    data class Success(
        val etapas: List<EtapaEditableUi>,
        val equipo: List<IntegranteResponseDto>,
        val isSaving: Boolean = false,
        val errorMessage: String? = null
    ) : EditarPlanificacionUiState
    data class Error(val message: String) : EditarPlanificacionUiState
}

class EditarPlanificacionViewModel(
    private val planificacionRepository: PlanificacionRepository = PlanificacionRepositoryImpl(),
    private val proyectoRepository: ProyectoRepository = ProyectoRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditarPlanificacionUiState>(EditarPlanificacionUiState.Loading)
    val uiState: StateFlow<EditarPlanificacionUiState> = _uiState.asStateFlow()

    fun cargar(proyectoId: Long) {
        _uiState.value = EditarPlanificacionUiState.Loading
        viewModelScope.launch {
            val planificacion = planificacionRepository.verPlanificacion(proyectoId).getOrElse {
                _uiState.value = EditarPlanificacionUiState.Error(it.message ?: "No se pudo cargar la planificación.")
                return@launch
            }
            if (planificacion == null) {
                _uiState.value = EditarPlanificacionUiState.Error("El proyecto aún no tiene una planificación para editar.")
                return@launch
            }

            val equipo = proyectoRepository.verEquipo(proyectoId).getOrDefault(emptyList())
            _uiState.value = EditarPlanificacionUiState.Success(
                etapas = planificacion.aEtapasEditables(equipo),
                equipo = equipo
            )
        }
    }

    fun guardar(
        proyectoId: Long,
        etapasOriginales: List<EtapaEditableUi>,
        etapasActuales: List<EtapaEditableUi>,
        onGuardado: () -> Unit
    ) {
        val estadoExitoso = _uiState.value as? EditarPlanificacionUiState.Success ?: return
        _uiState.value = estadoExitoso.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            val resultado = runCatching {
                guardarPlan(proyectoId, etapasActuales)
            }
            resultado.onSuccess { onGuardado() }
                .onFailure { error ->
                    _uiState.value = estadoExitoso.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "No se pudieron guardar los cambios."
                    )
                }
        }
    }

    private suspend fun guardarPlan(proyectoId: Long, actuales: List<EtapaEditableUi>) {
        val request = PlanificacionUpdateRequestDto(
            etapas = actuales.mapIndexed { indice, etapa ->
                PlanificacionUpdateRequestDto.EtapaUpdateDto(
                    id = etapa.remoteId,
                    nombre = etapa.nombre.trim(),
                    orden = indice + 1,
                    tareas = etapa.tareas.map { tarea ->
                        val duracion = tarea.duracionDias.toIntOrNull()?.takeIf { it > 0 }
                            ?: throw IllegalArgumentException("La duración de «${tarea.nombre}» debe ser mayor que cero.")
                        PlanificacionUpdateRequestDto.TareaUpdateDto(
                            id = tarea.remoteId,
                            key = tarea.localId,
                            nombre = tarea.nombre.trim(),
                            descripcion = tarea.descripcion.trim().ifBlank { null },
                            responsableId = tarea.responsableId,
                            prioridad = tarea.prioridad,
                            duracionDias = duracion,
                            dependencias = tarea.dependencias
                        )
                    }
                )
            }
        )
        planificacionRepository.guardarPlanificacion(proyectoId, request).orThrow()
    }
}

private fun PlanificacionResponseDto.aEtapasEditables(
    equipo: List<IntegranteResponseDto>
): List<EtapaEditableUi> {
    val integranteIdPorEmail = equipo.associate { it.email.lowercase() to it.integranteId }
    return etapas.orEmpty().sortedBy { it.orden ?: Int.MAX_VALUE }.map { etapa ->
        EtapaEditableUi(
            localId = "etapa-${etapa.id}",
            remoteId = etapa.id,
            nombre = etapa.nombre,
            tareas = etapa.tareas.orEmpty().map { tarea ->
                TareaEditableUi(
                    localId = "tarea-${tarea.id}",
                    remoteId = tarea.id,
                    nombre = tarea.nombre,
                    descripcion = tarea.descripcion.orEmpty(),
                    responsableId = tarea.responsableEmail
                        ?.let { email -> integranteIdPorEmail[email.lowercase()] },
                    prioridad = tarea.prioridad.orEmpty().uppercase().ifBlank { "MEDIA" },
                    duracionDias = (tarea.duracionDias ?: 1).toString(),
                    dependencias = tarea.dependencias.orEmpty().map { "tarea-$it" },
                    dependenciasOriginales = tarea.dependencias.orEmpty().toSet()
                )
            }
        )
    }
}

private fun <T> Result<T>.orThrow(): T = getOrElse { throw it }