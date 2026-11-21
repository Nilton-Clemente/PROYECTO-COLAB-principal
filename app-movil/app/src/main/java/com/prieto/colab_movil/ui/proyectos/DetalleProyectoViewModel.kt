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
import retrofit2.HttpException

class DetalleProyectoViewModel(
    private val repository: ProyectoRepository = ProyectoRepositoryImpl(),
    private val planificacionRepository: PlanificacionRepository = PlanificacionRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetalleProyectoUiState>(DetalleProyectoUiState.Loading)
    val uiState: StateFlow<DetalleProyectoUiState> = _uiState.asStateFlow()

    private var operacion: Job? = null

    fun cargarDetalle(proyectoId: Long) {
        if (operacion?.isActive == true || _uiState.value is DetalleProyectoUiState.Eliminado) return
        val anterior = (_uiState.value as? DetalleProyectoUiState.Success)?.takeIf { it.proyecto.id == proyectoId }
        _uiState.value = anterior?.copy(actualizando = true, aviso = null) ?: DetalleProyectoUiState.Loading
        operacion = viewModelScope.launch { consultar(proyectoId, anterior) }
    }

    fun cambiarCiclo(proyectoId: Long, accion: AccionProyecto, confirmacion: String = "") {
        val anterior = _uiState.value as? DetalleProyectoUiState.Success ?: return
        if (anterior.proyecto.id != proyectoId || !anterior.puedeModificar || accion !in anterior.acciones || operacion?.isActive == true) return
        if (accion == AccionProyecto.EDITAR || (accion == AccionProyecto.ELIMINAR && !confirmaNombreProyecto(anterior.proyecto.nombre, confirmacion))) return
        _uiState.value = anterior.copy(accionEnCurso = accion, aviso = null)
        operacion = viewModelScope.launch {
            val resultado = when (accion) {
                AccionProyecto.CANCELAR -> repository.cancelarProyecto(proyectoId).map { actualizado ->
                    anterior.copy(proyecto = actualizado, aviso = "Proyecto cancelado. El historial sigue disponible.")
                }
                AccionProyecto.ELIMINAR -> repository.eliminarProyecto(proyectoId).map { DetalleProyectoUiState.Eliminado(proyectoId) }
                AccionProyecto.EDITAR -> return@launch
            }
            resultado.fold(
                onSuccess = { _uiState.value = it },
                onFailure = { error ->
                    val aviso = when ((error as? HttpException)?.code()) {
                        403 -> "Ya no tienes permiso para realizar esta acción."
                        404 -> "El proyecto ya no está disponible."
                        409 -> "El estado del proyecto cambió. Revisa las acciones disponibles."
                        else -> "No se pudo confirmar la operación. Revisa el estado antes de intentarlo de nuevo."
                    }
                    _uiState.value = anterior.copy(actualizando = true, datosDesactualizados = true)
                    consultar(proyectoId, anterior, aviso)
                }
            )
        }
    }

    private suspend fun consultar(proyectoId: Long, anterior: DetalleProyectoUiState.Success?, aviso: String? = null) {
            val proyectoRes = repository.verProyecto(proyectoId)
            if ((proyectoRes.exceptionOrNull() as? HttpException)?.code() in listOf(403, 404)) {
                _uiState.value = DetalleProyectoUiState.Error("El proyecto ya no está disponible o no tienes acceso.")
                return
            }
            val equipoRes = repository.verEquipo(proyectoId)
            val planRes = planificacionRepository.verPlanificacion(proyectoId)
            val email = repository.obtenerPerfil().getOrNull()?.email
            val fallaronDatos = proyectoRes.isFailure || equipoRes.isFailure || planRes.isFailure

            if (anterior != null || (proyectoRes.getOrNull() != null && equipoRes.isSuccess && planRes.isSuccess)) {
                _uiState.value = DetalleProyectoUiState.Success(
                    proyecto = proyectoRes.getOrNull() ?: anterior!!.proyecto,
                    equipo = equipoRes.getOrNull() ?: anterior!!.equipo,
                    tienePlanificacion = if (planRes.isSuccess) planRes.getOrNull() != null else anterior!!.tienePlanificacion,
                    datosDesactualizados = fallaronDatos,
                    soyCreador = email != null && equipoRes.getOrNull().orEmpty().any { it.email == email && it.rol == "CREADOR" },
                    aviso = listOfNotNull(aviso, if (fallaronDatos) "No se pudieron actualizar todos los datos. Inténtalo de nuevo." else null)
                        .joinToString("\n").ifBlank { null }
                )
            } else {
                val error = (proyectoRes.exceptionOrNull() ?: equipoRes.exceptionOrNull() ?: planRes.exceptionOrNull())?.message ?: "No se pudo cargar la información del proyecto."
                _uiState.value = DetalleProyectoUiState.Error(error)
            }
    }
}
