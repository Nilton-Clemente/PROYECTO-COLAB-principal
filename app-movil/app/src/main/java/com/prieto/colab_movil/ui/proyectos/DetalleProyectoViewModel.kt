package com.prieto.colab_movil.ui.proyectos

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
import kotlinx.coroutines.Job

class DetalleProyectoViewModel(
    private val repository: ProyectoRepository = ProyectoRepositoryImpl(),
    private val planificacionRepository: PlanificacionRepository = PlanificacionRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetalleProyectoUiState>(DetalleProyectoUiState.Loading)
    val uiState: StateFlow<DetalleProyectoUiState> = _uiState.asStateFlow()

    private var operacion: Job? = null

    fun cargarDetalle(proyectoId: Long) {
        if (operacion?.isActive == true) return
        val anterior = (_uiState.value as? DetalleProyectoUiState.Success)?.takeIf { it.proyecto.id == proyectoId }
        _uiState.value = anterior?.copy(actualizando = true, aviso = null) ?: DetalleProyectoUiState.Loading
        operacion = viewModelScope.launch {
            val proyectoRes = repository.verProyecto(proyectoId)
            val equipoRes = repository.verEquipo(proyectoId)
            val planRes = planificacionRepository.verPlanificacion(proyectoId)

            if (anterior != null || (proyectoRes.getOrNull() != null && equipoRes.isSuccess && planRes.isSuccess)) {
                _uiState.value = DetalleProyectoUiState.Success(
                    proyecto = proyectoRes.getOrNull() ?: anterior!!.proyecto,
                    equipo = equipoRes.getOrNull() ?: anterior!!.equipo,
                    tienePlanificacion = if (planRes.isSuccess) planRes.getOrNull() != null else anterior!!.tienePlanificacion,
                    datosDesactualizados = proyectoRes.isFailure || equipoRes.isFailure || planRes.isFailure,
                    aviso = if (proyectoRes.isFailure || equipoRes.isFailure || planRes.isFailure)
                        "No se pudieron actualizar todos los datos. Inténtalo de nuevo." else null
                )
            } else {
                val error = (proyectoRes.exceptionOrNull() ?: equipoRes.exceptionOrNull() ?: planRes.exceptionOrNull())?.message ?: "No se pudo cargar la información del proyecto."
                _uiState.value = DetalleProyectoUiState.Error(error)
            }
        }
    }
}
