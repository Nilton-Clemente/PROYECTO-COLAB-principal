package com.prieto.colab_movil.ui.metricas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prieto.colab_movil.data.repository.MetricasProyectoDatos
import com.prieto.colab_movil.data.repository.MetricasProyectoRepository
import com.prieto.colab_movil.data.repository.MetricasProyectoRepositoryImpl
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface MetricasProyectoUiState {
    data object Cargando : MetricasProyectoUiState
    data class Error(val mensaje: String, val permiteReintentar: Boolean) : MetricasProyectoUiState
    data class Listo(
        val datos: MetricasProyectoDatos,
        val actualizando: Boolean = false,
        val aviso: String? = null
    ) : MetricasProyectoUiState
}

class MetricasProyectoViewModel(
    private val repository: MetricasProyectoRepository = MetricasProyectoRepositoryImpl()
) : ViewModel() {
    private val _uiState = MutableStateFlow<MetricasProyectoUiState>(MetricasProyectoUiState.Cargando)
    val uiState = _uiState.asStateFlow()
    private var operacion: Job? = null

    fun cargar(proyectoId: Long) {
        if (operacion?.isActive == true) return
        val anterior = (_uiState.value as? MetricasProyectoUiState.Listo)?.takeIf { it.datos.proyecto.id == proyectoId }
        _uiState.value = anterior?.copy(actualizando = true, aviso = null) ?: MetricasProyectoUiState.Cargando
        operacion = viewModelScope.launch {
            repository.cargar(proyectoId).fold(
                onSuccess = { _uiState.value = MetricasProyectoUiState.Listo(it) },
                onFailure = { error ->
                    val codigo = (error as? HttpException)?.code()
                    val reintentable = codigo !in listOf(401, 403, 404)
                    val mensaje = when (codigo) {
                        401 -> "Tu sesión expiró. Vuelve a iniciar sesión."
                        403 -> "Solo los integrantes activos pueden consultar las métricas del proyecto."
                        404 -> "El proyecto o sus métricas ya no están disponibles."
                        else -> if (error is IOException) "No se pudo conectar con el servidor. Revisa tu conexión."
                            else "No se pudieron cargar las métricas del proyecto."
                    }
                    _uiState.value = if (reintentable && anterior != null) {
                        anterior.copy(aviso = "$mensaje Se muestran los últimos datos disponibles.")
                    } else MetricasProyectoUiState.Error(mensaje, reintentable)
                }
            )
        }
    }
}