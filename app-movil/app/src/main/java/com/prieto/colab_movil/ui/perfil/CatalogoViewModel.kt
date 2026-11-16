package com.prieto.colab_movil.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prieto.colab_movil.data.model.HabilidadDto
import com.prieto.colab_movil.data.repository.PerfilRepository
import com.prieto.colab_movil.data.repository.PerfilRepositoryImpl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class NivelHabilidad(val etiqueta: String) {
    BASICO("Básico"), INTERMEDIO("Intermedio"), AVANZADO("Avanzado")
}

fun nivelHabilidadTexto(nivel: String?): String = when (nivel) {
    "BASICO" -> "Básico"
    "INTERMEDIO" -> "Intermedio"
    "AVANZADO" -> "Avanzado"
    null, "" -> "Sin nivel especificado"
    else -> nivel
}

sealed interface CatalogoUiState {
    data object Loading : CatalogoUiState
    data class Error(val mensaje: String = "No se pudieron cargar los datos. Inténtalo de nuevo.") : CatalogoUiState
    data class Success(
        val habilidades: List<HabilidadDto>,
        val perfil: List<HabilidadDto>,
        val guardando: Boolean = false,
        val mensaje: String? = null
    ) : CatalogoUiState
}

class CatalogoViewModel(private val repository: PerfilRepository = PerfilRepositoryImpl()) : ViewModel() {
    private val _uiState = MutableStateFlow<CatalogoUiState>(CatalogoUiState.Loading)
    val uiState = _uiState.asStateFlow()
    private var operacion: Job? = null

    init { cargar() }

    fun cargar() {
        if (operacion?.isActive == true) return
        _uiState.value = CatalogoUiState.Loading
        operacion = viewModelScope.launch {
            val catalogo = repository.listarCatalogo()
            val perfil = repository.obtenerPerfil()
            _uiState.value = if (catalogo.isSuccess && perfil.isSuccess) {
                CatalogoUiState.Success(catalogo.getOrThrow(), perfil.getOrThrow().habilidades)
            } else CatalogoUiState.Error()
        }
    }

    fun agregar(habilidad: HabilidadDto, nivel: NivelHabilidad) {
        val estado = _uiState.value as? CatalogoUiState.Success ?: return
        if (estado.guardando || operacion?.isActive == true || estado.perfil.any { it.id == habilidad.id }) return
        _uiState.value = estado.copy(guardando = true, mensaje = null)
        operacion = viewModelScope.launch {
            try {
                val resultado = repository.agregarHabilidad(habilidad.id, nivel.name)
                val perfilActualizado = repository.obtenerPerfil()
                if (perfilActualizado.isSuccess) {
                    val perfil = perfilActualizado.getOrThrow().habilidades
                    _uiState.value = estado.copy(perfil = perfil, mensaje = when {
                        resultado.isSuccess -> "${habilidad.nombre} se añadió a tu perfil."
                        perfil.any { it.id == habilidad.id } -> "${habilidad.nombre} ya aparece en tu perfil; comprueba su nivel."
                        else -> "No se pudo añadir ${habilidad.nombre}. Inténtalo de nuevo."
                    })
                } else {
                    _uiState.value = CatalogoUiState.Error("No se pudo confirmar si la habilidad quedó guardada. Reintenta la consulta antes de volver a añadirla.")
                }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    fun consumirMensaje() {
        val estado = _uiState.value as? CatalogoUiState.Success ?: return
        _uiState.value = estado.copy(mensaje = null)
    }
}