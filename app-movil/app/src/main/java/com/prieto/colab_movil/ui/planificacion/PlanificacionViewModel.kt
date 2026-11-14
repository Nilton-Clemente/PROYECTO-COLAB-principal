package com.prieto.colab_movil.ui.planificacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prieto.colab_movil.data.model.PlanificacionResponseDto
import com.prieto.colab_movil.data.repository.GeneracionPlanificacionInciertaException
import com.prieto.colab_movil.data.repository.PlanificacionHttpException
import com.prieto.colab_movil.data.repository.PlanificacionRepository
import com.prieto.colab_movil.data.repository.PlanificacionRepositoryImpl
import com.prieto.colab_movil.data.repository.ProyectoRepository
import com.prieto.colab_movil.data.repository.ProyectoRepositoryImpl
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

// Un GET 404 no prueba que un POST anterior haya terminado. Evita duplicarlo al volver a entrar.
private val generacionesPendientes = ConcurrentHashMap.newKeySet<Long>()

class PlanificacionViewModel(
    private val planificacionRepository: PlanificacionRepository = PlanificacionRepositoryImpl(),
    private val proyectoRepository: ProyectoRepository = ProyectoRepositoryImpl(),
    private val esperarRecuperacion: suspend () -> Unit = { delay(3_000) }
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlanificacionUiState>(PlanificacionUiState.Loading)
    val uiState: StateFlow<PlanificacionUiState> = _uiState.asStateFlow()

    private var operacion: Job? = null

    fun cargar(proyectoId: Long) = iniciar(proyectoId, soloConsultar = false)

    fun generar(proyectoId: Long) = cargar(proyectoId)

    fun consultarResultado(proyectoId: Long) = iniciar(proyectoId, soloConsultar = true)

    private fun iniciar(proyectoId: Long, soloConsultar: Boolean) {
        if (operacion?.isActive == true) return
        _uiState.value = PlanificacionUiState.Loading
        operacion = viewModelScope.launch {
            val resultado = planificacionRepository.verPlanificacion(proyectoId)
            if (resultado.isFailure) {
                _uiState.value = PlanificacionUiState.Error(
                    resultado.exceptionOrNull()?.message ?: "No se pudo cargar la planificación."
                )
                return@launch
            }
            val plan = resultado.getOrNull()
            if (plan != null) {
                mostrarPlan(proyectoId, plan)
            } else if (soloConsultar || !generacionesPendientes.add(proyectoId)) {
                recuperarResultado(proyectoId)
            } else {
                _uiState.value = PlanificacionUiState.Generating
                val generado = planificacionRepository.generarPlanificacion(proyectoId)
                val planGenerado = generado.getOrNull()
                if (planGenerado != null) {
                    // El POST devuelve el resultado persistido; no depender de otro GET.
                    mostrarPlan(proyectoId, planGenerado)
                } else {
                    val error = generado.exceptionOrNull()
                    if (error is GeneracionPlanificacionInciertaException) {
                        recuperarResultado(proyectoId)
                    } else {
                        generacionesPendientes.remove(proyectoId)
                        _uiState.value = PlanificacionUiState.Error(
                            message = error?.message ?: "No se pudo generar la planificación.",
                            fromGeneration = true
                        )
                    }
                }
            }
        }
    }

    private suspend fun recuperarResultado(proyectoId: Long) {
        _uiState.value = PlanificacionUiState.CheckingGeneration
        repeat(20) { intento ->
            if (intento > 0) esperarRecuperacion()
            val resultado = planificacionRepository.verPlanificacion(proyectoId)
            val plan = resultado.getOrNull()
            if (plan != null) {
                mostrarPlan(proyectoId, plan)
                return
            }
            val error = resultado.exceptionOrNull()
            if (error is PlanificacionHttpException && error.codigo in setOf(401, 403)) {
                _uiState.value = PlanificacionUiState.Error(error.message.orEmpty())
                return
            }
        }
        _uiState.value = PlanificacionUiState.PendingGeneration(
            "Todavía no se pudo confirmar el resultado. El servidor podría seguir generando la planificación. " +
                "Puedes consultar de nuevo sin iniciar otra generación."
        )
    }

    private suspend fun mostrarPlan(proyectoId: Long, plan: PlanificacionResponseDto) {
        generacionesPendientes.remove(proyectoId)
        val proyecto = proyectoRepository.verProyecto(proyectoId).getOrNull()
        val equipo = proyectoRepository.verEquipo(proyectoId).getOrDefault(emptyList())
        _uiState.value = PlanificacionUiState.Success(plan, proyecto, equipo)
    }
}
