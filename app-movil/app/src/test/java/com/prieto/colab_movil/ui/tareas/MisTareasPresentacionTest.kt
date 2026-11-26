package com.prieto.colab_movil.ui.tareas

import com.google.gson.Gson
import com.prieto.colab_movil.data.model.TareaDetalleResponseDto
import org.junit.Assert.*
import org.junit.Test

class MisTareasPresentacionTest {
    private val tareas = listOf(
        tarea(21, "API de matrícula", "EN_PROGRESO", "Validar cupos", "Backend"),
        tarea(22, "Pantalla de acceso", "PENDIENTE", "Formulario", "Frontend"),
        tarea(23, "Pruebas", "COMPLETADA", "Verificar respuestas", "Calidad"),
        tarea(24, "Integración", "BLOQUEADA", "Conectar servicios", "Backend"),
        tarea(25, "Tarea histórica", null, null, "General")
    )

    @Test
    fun buscaSinDistinguirTildesMayusculasNiEspaciosExteriores() {
        assertEquals(listOf(21L), filtrarMisTareas(tareas, " MATRÍCULA ").map { it.id })
        assertEquals(listOf(24L), filtrarMisTareas(tareas, "integracion").map { it.id })
    }

    @Test
    fun buscaPorCodigoDescripcionYEtapa() {
        assertEquals(listOf(22L), filtrarMisTareas(tareas, "tk-22").map { it.id })
        assertEquals(listOf(21L), filtrarMisTareas(tareas, "cupos").map { it.id })
        assertEquals(listOf(21L, 24L), filtrarMisTareas(tareas, "backend").map { it.id })
    }

    @Test
    fun combinaBusquedaConEstadoRealDelBackend() {
        assertEquals(listOf(24L), filtrarMisTareas(tareas, "backend", FiltroEstadoTareas.BLOQUEADAS).map { it.id })
        assertTrue(filtrarMisTareas(tareas, "frontend", FiltroEstadoTareas.COMPLETADAS).isEmpty())
        assertEquals(listOf(23L), filtrarMisTareas(tareas, "", FiltroEstadoTareas.COMPLETADAS).map { it.id })
    }

    @Test
    fun noOcultaEstadosDesconocidosNiLosConvierteEnPendientes() {
        assertEquals(tareas, filtrarMisTareas(tareas, ""))
        assertEquals(listOf(22L), filtrarMisTareas(tareas, "", FiltroEstadoTareas.PENDIENTES).map { it.id })
        assertEquals(listOf(25L), filtrarMisTareas(tareas, "historica").map { it.id })
    }

    @Test
    fun noModificaElOrdenNiLaListaOriginalYToleraListasVacias() {
        val antes = tareas.toList()
        filtrarMisTareas(tareas, "backend")
        assertEquals(antes, tareas)
        assertTrue(filtrarMisTareas(emptyList(), "api").isEmpty())
    }

    private fun tarea(id: Long, nombre: String, estado: String?, descripcion: String?, etapa: String) =
        Gson().fromJson("""{"id":$id,"nombre":"$nombre","estado":${estado?.let { "\"$it\"" } ?: "null"},"descripcion":${descripcion?.let { "\"$it\"" } ?: "null"},"etapaNombre":"$etapa"}""", TareaDetalleResponseDto::class.java)
}