package com.prieto.colab_movil.ui.metricas

import com.google.gson.Gson
import com.prieto.colab_movil.data.model.IntegranteResponseDto
import com.prieto.colab_movil.data.model.MetricasIntegranteDto
import com.prieto.colab_movil.data.model.MetricasProyectoDto
import com.prieto.colab_movil.data.model.ProyectoResponseDto
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class MetricasPresentacionTest {
    @Test fun leeElContratoDeMetricas() {
        val metricas = Gson().fromJson("""{"totalTareas":4,"pendientes":1,"enProgreso":1,"completadas":1,"bloqueadas":1,"porcentajeCompletadas":25.0,"porResponsable":[{"integranteId":7,"email":"ana@example.test","totalTareas":2,"completadas":1}]}""", MetricasProyectoDto::class.java)
        assertEquals(4L, metricas.totalTareas)
        assertEquals(listOf(1L, 1L, 1L, 1L), listOf(metricas.pendientes, metricas.enProgreso, metricas.completadas, metricas.bloqueadas))
        assertEquals(25.0, metricas.porcentajeCompletadas, 0.0)
        assertEquals(50.0, porcentajeTareas(metricas.porResponsable!!.single().completadas, 2)!!, 0.0)
    }

    @Test fun distingueSinTareasDeCeroPorCientoYEvitaRedondearACompleto() {
        assertNull(porcentajeTareas(0, 0))
        assertEquals(0.0, porcentajeTareas(0, 2)!!, 0.0)
        assertTrue(textoPorcentaje(99.99).startsWith("< 100"))
        assertFalse(textoPorcentaje(100.0).startsWith("<"))
    }

    @Test fun priorizaNombreDelEquipoYUsaCorreoComoAlternativa() {
        val avance = MetricasIntegranteDto(7, "ana@example.test", 2, 1)
        assertEquals("Ana Torres", nombreResponsable(avance, IntegranteResponseDto(7, "ana@example.test", "Ana Torres", "CREADOR", "ACTIVO")))
        assertEquals("ana@example.test", nombreResponsable(avance, null))
    }

    @Test fun soloMarcaVencidaUnaEntregaAnteriorDeUnProyectoEnDesarrollo() {
        val proyecto = Gson().fromJson("""{"id":1,"nombre":"Colab","fechaEntrega":"2026-11-20","estado":"EN_DESARROLLO"}""", ProyectoResponseDto::class.java)
        val hoy = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 21, 23, 59) }.time
        assertTrue(entregaVencida(proyecto, hoy))
        assertFalse(entregaVencida(proyecto.copy(fechaEntrega = "2026-11-21"), hoy))
        assertFalse(entregaVencida(proyecto.copy(fechaEntrega = null), hoy))
        assertFalse(entregaVencida(proyecto.copy(estado = "FINALIZADO"), hoy))
        assertFalse(entregaVencida(proyecto.copy(estado = "CANCELADO"), hoy))
    }
}