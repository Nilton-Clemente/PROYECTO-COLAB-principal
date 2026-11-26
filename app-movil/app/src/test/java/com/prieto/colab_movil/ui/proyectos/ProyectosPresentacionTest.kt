package com.prieto.colab_movil.ui.proyectos

import com.google.gson.Gson
import com.prieto.colab_movil.data.model.EstadoProyecto
import com.prieto.colab_movil.data.model.IntegranteResponseDto
import com.prieto.colab_movil.data.model.ProyectoResponseDto
import com.prieto.colab_movil.data.model.TecnologiaDto
import org.junit.Assert.*
import org.junit.Test

class ProyectosPresentacionTest {
    private val email = "alex@tecsup.edu.pe"

    @Test
    fun identificaCreadorPorSuCorreoYRolReal() {
        assertEquals(ParticipacionProyecto.CREADOR, determinarParticipacion(email, listOf(integrante("CREADOR"))))
    }

    @Test
    fun identificaParticipanteAunqueElEquipoIncluyaOtroCreador() {
        val equipo = listOf(integrante("CREADOR", correo = "otra@tecsup.edu.pe"), integrante("INTEGRANTE"))
        assertEquals(ParticipacionProyecto.PARTICIPANTE, determinarParticipacion(email, equipo))
    }

    @Test
    fun comparaCorreosSinMayusculasNiEspaciosExteriores() {
        assertEquals(ParticipacionProyecto.CREADOR, determinarParticipacion(" ALEX@TECSUP.EDU.PE ", listOf(integrante("CREADOR", correo = " $email "))))
    }

    @Test
    fun sinIdentidadNoInventaUnaParticipacion() {
        for (correo in listOf(null, "", " ")) {
            assertEquals(ParticipacionProyecto.SIN_VERIFICAR, determinarParticipacion(correo, listOf(integrante("CREADOR"))))
        }
    }

    @Test
    fun equipoVacioOUsuarioAusenteNoImplicaSerParticipante() {
        assertEquals(ParticipacionProyecto.SIN_VERIFICAR, determinarParticipacion(email, emptyList()))
        assertEquals(ParticipacionProyecto.SIN_VERIFICAR, determinarParticipacion("otra@tecsup.edu.pe", listOf(integrante("CREADOR"))))
    }

    @Test
    fun noClasificaRolesDesconocidosONulosComoParticipante() {
        for (rol in listOf(null, "", "ADMINISTRADOR")) {
            assertEquals(ParticipacionProyecto.SIN_VERIFICAR, determinarParticipacion(email, listOf(integrante(rol))))
        }
    }

    @Test
    fun invitacionesPendientesEIntegrantesInactivosNoDefinenElRol() {
        for (estado in listOf(null, "PENDIENTE", "INACTIVO")) {
            assertEquals(ParticipacionProyecto.SIN_VERIFICAR, determinarParticipacion(email, listOf(integrante("INTEGRANTE", estado))))
        }
    }

    @Test
    fun mapeaParticipacionYConservaSoloAvataresActivosOrdenados() {
        val dto = Gson().fromJson("""{"id":7,"nombre":"Proyecto","estado":"FINALIZADO","puedeCancelar":false}""", ProyectoResponseDto::class.java)
        val creador = integrante("CREADOR", correo = "otra@tecsup.edu.pe").copy(integranteId = 2)
        val participante = integrante("INTEGRANTE")
        val resultado = mapToProyecto(dto, listOf(participante, integrante("INTEGRANTE", "PENDIENTE"), creador), email)
        assertEquals(ParticipacionProyecto.PARTICIPANTE, resultado.participacion)
        assertEquals(listOf(creador, participante), resultado.integrantes)
        assertEquals(EstadoProyecto.FINALIZADO, resultado.estado)
        assertEquals(ParticipacionProyecto.CREADOR, mapToProyecto(dto, listOf(integrante("CREADOR")), email).participacion)
    }

    @Test
    fun sinBusquedaIncluyeTodosLosRolesYLosSinVerificar() {
        val proyectos = proyectos()
        assertEquals(proyectos, filtrarProyectos(proyectos))
        assertEquals(proyectos, filtrarProyectos(proyectos, busqueda = "   "))
    }

    @Test
    fun combinaBusquedaConEstadoSinFiltrarPorRol() {
        assertEquals(listOf(3L), filtrarProyectos(proyectos(), Filtro.FINALIZADOS, "proyecto").map { it.id })
        assertTrue(filtrarProyectos(proyectos(), Filtro.FINALIZADOS, "TEC-2").isEmpty())
        assertEquals(listOf(1L, 2L, 4L), filtrarProyectos(proyectos(), Filtro.EN_DESARROLLO).map { it.id })
    }

    @Test
    fun buscaNombreSinDistinguirTildesMayusculasNiEspaciosExteriores() {
        val proyectos = listOf(proyecto(1, ParticipacionProyecto.CREADOR).copy(titulo = "Gestión de matrícula"))
        assertEquals(proyectos, filtrarProyectos(proyectos, busqueda = " MATRICULA "))
        assertEquals(proyectos, filtrarProyectos(proyectos, busqueda = "gestion"))
    }

    @Test
    fun buscaPorCodigoDescripcionYTecnologia() {
        val proyectos = proyectos().map {
            if (it.id == 2L) it.copy(descripcion = "Validar cupos", tecnologias = listOf(TecnologiaDto(1, "Kotlin"))) else it
        }
        for (busqueda in listOf("tec-2", "CUPOS", "kotlin")) {
            assertEquals(listOf(2L), filtrarProyectos(proyectos, busqueda = busqueda).map { it.id })
        }
    }

    @Test
    fun noUsaRolComoFiltroDeBusqueda() {
        assertTrue(filtrarProyectos(proyectos(), busqueda = "Participante").isEmpty())
        assertTrue(filtrarProyectos(proyectos(), busqueda = "Creado por mí").isEmpty())
        assertEquals(listOf(1L, 2L, 3L, 4L), filtrarProyectos(proyectos(), busqueda = "Proyecto").map { it.id })
    }

    @Test
    fun conservaOrdenYListaOriginalYToleraListasVacias() {
        val proyectos = proyectos()
        val antes = proyectos.toList()
        filtrarProyectos(proyectos, busqueda = "TEC-3")
        assertEquals(antes, proyectos)
        assertTrue(filtrarProyectos(emptyList(), Filtro.FINALIZADOS, "proyecto").isEmpty())
    }

    private fun integrante(rol: String?, estado: String? = "ACTIVO", correo: String = email) =
        IntegranteResponseDto(1, correo, "Alex", rol, estado)

    private fun proyectos() = listOf(
        proyecto(1, ParticipacionProyecto.CREADOR),
        proyecto(2, ParticipacionProyecto.PARTICIPANTE),
        proyecto(3, ParticipacionProyecto.CREADOR).copy(estado = EstadoProyecto.FINALIZADO),
        proyecto(4, ParticipacionProyecto.SIN_VERIFICAR)
    )

    private fun proyecto(id: Long, participacion: ParticipacionProyecto) = Proyecto(
        id = id, codigo = "TEC-$id", titulo = "Proyecto $id", estado = EstadoProyecto.EN_DESARROLLO,
        descripcion = null, tecnologias = emptyList(), fechaEntrega = null, participacion = participacion
    )
}