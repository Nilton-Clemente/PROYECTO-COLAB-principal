package com.prieto.colab_movil.ui.proyectos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prieto.colab_movil.data.repository.ProyectoRepository
import com.prieto.colab_movil.data.repository.ProyectoRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class ProyectosViewModel(
    private val repository: ProyectoRepository = ProyectoRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProyectosUiState>(ProyectosUiState.Loading)
    val uiState: StateFlow<ProyectosUiState> = _uiState.asStateFlow()

    private var operacion: Job? = null
    private val eliminados = mutableSetOf<Long>()

    fun retirarEliminado(id: Long) {
        eliminados.add(id)
        (_uiState.value as? ProyectosUiState.Success)?.let { anterior ->
            _uiState.value = anterior.copy(proyectos = anterior.proyectos.filterNot { it.id == id }, equipos = anterior.equipos - id)
        }
        cargarDatos()
    }

    fun cargarDatos() {
        if (operacion?.isActive == true) return
        val anterior = _uiState.value as? ProyectosUiState.Success
        _uiState.value = anterior?.copy(actualizando = true, aviso = null) ?: ProyectosUiState.Loading
        operacion = viewModelScope.launch {
            val perfilRes = repository.obtenerPerfil()
            val proyectosRes = repository.listarProyectos()
            val invitacionesRes = repository.listarInvitaciones()

            if (anterior != null || (perfilRes.isSuccess && proyectosRes.isSuccess && invitacionesRes.isSuccess)) {
                val proyectos = (proyectosRes.getOrNull() ?: anterior!!.proyectos).filterNot { it.id in eliminados }
                val invitaciones = invitacionesRes.getOrNull() ?: anterior!!.invitaciones
                val pendientes = invitaciones.map { it.proyectoId }.toSet()
                val ids = proyectos.map { it.id }.filterNot { it in pendientes }.distinct()
                val previos = anterior?.equipos.orEmpty().filterKeys { it in ids }
                val listado = ProyectosUiState.Success(
                    nombre = perfilRes.getOrNull()?.nombre ?: anterior!!.nombre,
                    emailUsuario = perfilRes.getOrNull()?.email ?: anterior?.emailUsuario,
                    proyectos = proyectos,
                    invitaciones = invitaciones,
                    actualizando = ids.isNotEmpty(),
                    equipos = previos,
                    aviso = if (perfilRes.isFailure || proyectosRes.isFailure || invitacionesRes.isFailure)
                        "No se pudieron actualizar todos los datos. Inténtalo de nuevo." else null
                )
                _uiState.value = listado
                val limite = Semaphore(4)
                val respuestas = coroutineScope {
                    ids.map { id -> async { id to limite.withPermit { repository.verEquipo(id) } } }.awaitAll()
                }
                val equipos = previos.toMutableMap()
                for ((id, resultado) in respuestas) {
                    resultado.getOrNull()?.let { equipos[id] = it }
                }
                _uiState.value = listado.copy(
                    proyectos = proyectos.filterNot { it.id in eliminados },
                    equipos = equipos.filterKeys { it !in eliminados },
                    actualizando = false,
                    aviso = listado.aviso ?: if (respuestas.any { it.second.isFailure })
                        "No se pudieron actualizar todos los equipos. Desliza para reintentar." else null
                )
            } else {
                val error = proyectosRes.exceptionOrNull() ?: perfilRes.exceptionOrNull() ?: invitacionesRes.exceptionOrNull()
                _uiState.value = ProyectosUiState.Error(error?.message ?: "Error al cargar datos")
            }
        }
    }
}
