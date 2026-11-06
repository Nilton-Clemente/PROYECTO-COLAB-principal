package com.prieto.colab_movil.ui.planificacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prieto.colab_movil.data.repository.PlanificacionRepository
import com.prieto.colab_movil.data.repository.PlanificacionRepositoryImpl
import com.prieto.colab_movil.data.repository.ProyectoRepository
import com.prieto.colab_movil.data.repository.ProyectoRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlanificacionViewModel(
    private val planificacionRepository: PlanificacionRepository = PlanificacionRepositoryImpl(),
    private val proyectoRepository: ProyectoRepository = ProyectoRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlanificacionUiState>(PlanificacionUiState.Loading)
    val uiState: StateFlow<PlanificacionUiState> = _uiState.asStateFlow()

    fun cargar(proyectoId: Long) {
        _uiState.value = PlanificacionUiState.Loading
        viewModelScope.launch {
            val planRes = planificacionRepository.verPlanificacion(proyectoId)
            val proyectoRes = proyectoRepository.verProyecto(proyectoId)
            val equipoRes = proyectoRepository.verEquipo(proyectoId)

            val plan = planRes.getOrNull()
            when {
                planRes.isSuccess && plan == null -> {
                    // No hay planificación todavía: se genera automáticamente.
                    generar(proyectoId)
                }
                planRes.isSuccess && plan != null -> {
                    _uiState.value = PlanificacionUiState.Success(
                        planificacion = plan,
                        proyecto = proyectoRes.getOrNull(),
                        equipo = equipoRes.getOrNull() ?: emptyList()
                    )
                }
                else -> {
                    _uiState.value = PlanificacionUiState.Error(
                        planRes.exceptionOrNull()?.message ?: "No se pudo cargar la planificación."
                    )
                }
            }
        }
    }

    fun generar(proyectoId: Long) {
        _uiState.value = PlanificacionUiState.Generating
        viewModelScope.launch {
            val genRes = planificacionRepository.generarPlanificacion(proyectoId)
            if (genRes.isSuccess) {
                cargar(proyectoId)
            } else {
                _uiState.value = PlanificacionUiState.Error(
                    message = genRes.exceptionOrNull()?.message ?: "No se pudo generar la planificación.",
                    fromGeneration = true
                )
            }
        }
    }
}
