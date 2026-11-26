package com.prieto.colab_movil.ui.proyectos

import com.google.gson.Gson
import com.prieto.colab_movil.data.model.EstadoProyecto
import com.prieto.colab_movil.data.model.ProyectoResponseDto
import com.prieto.colab_movil.data.model.estadoProyecto
import org.junit.Assert.*
import org.junit.Test

class CicloProyectoTest {
    private val dto = Gson().fromJson("""{"id":7,"nombre":"Proyecto"}""", ProyectoResponseDto::class.java)

    @Test fun reconoceEstadosYDejaLosDesconocidosSinClasificar() {
        assertEquals(EstadoProyecto.DESCONOCIDO, dto.estadoProyecto)
        assertEquals(EstadoProyecto.DESCONOCIDO, EstadoProyecto.desdeApi("FUTURO"))
        val estados = EstadoProyecto.entries.filterNot { it == EstadoProyecto.DESCONOCIDO }
        assertEquals(5, estados.size)
        assertTrue(estados.all { EstadoProyecto.desdeApi(it.name) == it && Filtro.entries.count { filtro -> filtro.coincide(it) } == 2 })
        assertTrue(Filtro.TODOS.coincide(dto.estadoProyecto))
        assertEquals(listOf(EstadoProyecto.FINALIZADO, EstadoProyecto.CANCELADO), estados.filter { it.cerrado })
    }

    @Test fun reflejaPermisosDelServidorYAcotaLaEdicionAlCreadorSinPlanificacion() {
        val sinPlan = dto.copy(estado = "SIN_PLANIFICACION", puedeEliminar = true)
        assertEquals(listOf(AccionProyecto.EDITAR, AccionProyecto.ELIMINAR), DetalleProyectoUiState.Success(sinPlan, emptyList(), false, soyCreador = true).acciones)
        assertEquals(listOf(AccionProyecto.ELIMINAR), DetalleProyectoUiState.Success(sinPlan, emptyList(), true, soyCreador = true).acciones)
        val activo = dto.copy(estado = "EN_DESARROLLO", puedeCancelar = true)
        assertEquals(listOf(AccionProyecto.CANCELAR), DetalleProyectoUiState.Success(activo, emptyList(), true).acciones)
        assertTrue(DetalleProyectoUiState.Success(activo.copy(puedeCancelar = false), emptyList(), true).acciones.isEmpty())
    }

    @Test fun bloqueaCambiosDuranteOperacionesOSiLosDatosEstanDesactualizados() {
        val estado = DetalleProyectoUiState.Success(dto.copy(puedeEliminar = true), emptyList(), false)
        assertTrue(estado.puedeModificar)
        assertFalse(estado.copy(actualizando = true).puedeModificar)
        assertFalse(estado.copy(datosDesactualizados = true).puedeModificar)
        assertFalse(estado.copy(accionEnCurso = AccionProyecto.ELIMINAR).puedeModificar)
        assertFalse(estado.copy(enviandoInvitacion = true).puedeModificar)
    }
}