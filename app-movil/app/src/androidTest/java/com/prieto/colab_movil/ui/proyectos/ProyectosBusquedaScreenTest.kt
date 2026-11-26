package com.prieto.colab_movil.ui.proyectos

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prieto.colab_movil.data.model.EstadoProyecto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProyectosBusquedaScreenTest {
    @get:Rule val compose = createComposeRule()

    private val proyectos = listOf(
        proyecto(1, "Gestión de matrícula", ParticipacionProyecto.CREADOR),
        proyecto(2, "Proyecto compartido", ParticipacionProyecto.PARTICIPANTE),
        proyecto(3, "Mi proyecto finalizado", ParticipacionProyecto.CREADOR).copy(estado = EstadoProyecto.FINALIZADO),
        proyecto(4, "Proyecto por verificar", ParticipacionProyecto.SIN_VERIFICAR)
    )

    @Test
    fun etiquetasIdentificanCadaTarjetaYConservanLaNavegacion() {
        var abierto: Proyecto? = null
        compose.setContent { MaterialTheme { ProyectosContent(proyectos = proyectos, onProyectoClick = { abierto = it }) } }
        verTarjeta(1).assert(hasText("Creado por mí"))
        verTarjeta(2).assert(hasText("Participante")).performClick()
        compose.runOnIdle { assertEquals(2L, abierto?.id) }
        verTarjeta(4).assert(hasText("Rol por verificar"))
    }

    @Test
    fun busquedaReemplazaFiltrosDeRolYCombinaConEstado() {
        mostrar()
        compose.onNodeWithTag("filtros-participacion").assertDoesNotExist()
        compose.onNodeWithText("Tu participación").assertDoesNotExist()
        buscar("proyecto")
        elegirEstado(Filtro.FINALIZADOS)
        verTarjeta(3).assertExists()
        compose.onNodeWithTag("proyecto-1").assertDoesNotExist()
        compose.onNodeWithTag("proyecto-2").assertDoesNotExist()
        compose.onNodeWithTag("proyectos-lista").performScrollToIndex(0)
        compose.onNodeWithTag("buscar-proyectos").assertTextEquals("Buscar proyectos", "proyecto")
        buscar("compartido")
        compose.onNodeWithText("No hay proyectos que coincidan con la búsqueda y este estado").assertIsDisplayed()
    }

    @Test
    fun buscaSinTildesYLimpiarRecuperaTodosLosRoles() {
        mostrar()
        buscar(" MATRICULA ")
        verTarjeta(1).assert(hasText("Creado por mí"))
        compose.onNodeWithTag("proyecto-2").assertDoesNotExist()
        compose.onNodeWithTag("proyectos-lista").performScrollToIndex(0)
        compose.onNodeWithContentDescription("Limpiar búsqueda").performClick()
        compose.onNodeWithContentDescription("Limpiar búsqueda").assertDoesNotExist()
        verTarjeta(2).assert(hasText("Participante"))
        verTarjeta(4).assert(hasText("Rol por verificar"))
    }

    @Test
    fun conservaBusquedaYEstadoAlRestaurarLaPantalla() {
        val restauracion = StateRestorationTester(compose)
        restauracion.setContent { MaterialTheme { ProyectosContent(proyectos = proyectos) } }
        buscar("proyecto")
        elegirEstado(Filtro.FINALIZADOS)
        restauracion.emulateSavedInstanceStateRestore()
        verTarjeta(3).assertExists()
        compose.onNodeWithTag("proyecto-1").assertDoesNotExist()
        compose.onNodeWithTag("proyecto-2").assertDoesNotExist()
        compose.onNodeWithTag("proyectos-lista").performScrollToIndex(0)
        compose.onNodeWithTag("buscar-proyectos").assertTextEquals("Buscar proyectos", "proyecto")
        buscar("matricula")
        compose.onNodeWithText("No hay proyectos que coincidan con la búsqueda y este estado").assertIsDisplayed()
    }

    @Test
    fun muestraEstadoVacioSinResultadosYActualizaContadores() {
        mostrar()
        buscar("no existe")
        compose.onNodeWithText("No se encontraron proyectos con esta búsqueda").assertIsDisplayed()
        compose.onNodeWithTag("estado-TODOS").assert(hasText("0"))
        buscar("matricula")
        compose.onNodeWithTag("estado-TODOS").assert(hasText("1"))
    }

    @Test
    fun rolYEstadoSeMuestranJuntosEnLaTarjeta() {
        mostrar()
        verTarjeta(1).assertExists()
        compose.onNodeWithTag("etiquetas-proyecto-1", useUnmergedTree = true)
            .assert(hasAnyDescendant(hasText("Creado por mí")))
            .assert(hasAnyDescendant(hasTestTag("estado-proyecto-1")))
        val estado = compose.onNodeWithTag("estado-proyecto-1", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val rol = compose.onNodeWithTag("rol-proyecto-1", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("El rol debe estar al lado del estado", rol.left >= estado.right && rol.top < estado.bottom && estado.top < rol.bottom)
    }

    @Test
    fun buscaPorCodigoYLimpiarMantieneElFiltroDeEstado() {
        mostrar()
        buscar("tec-2")
        verTarjeta(2).assert(hasText("Participante"))
        compose.onNodeWithTag("proyecto-1").assertDoesNotExist()
        elegirEstado(Filtro.FINALIZADOS)
        compose.onNodeWithTag("proyectos-lista").performScrollToIndex(0)
        compose.onNodeWithContentDescription("Limpiar búsqueda").performClick()
        verTarjeta(3).assertExists()
        compose.onNodeWithTag("proyecto-2").assertDoesNotExist()
    }

    private fun mostrar() {
        compose.setContent { MaterialTheme { ProyectosContent(proyectos = proyectos) } }
    }

    private fun verTarjeta(id: Long): SemanticsNodeInteraction {
        compose.onNodeWithTag("proyectos-lista").performScrollToNode(hasTestTag("proyecto-$id"))
        return compose.onNodeWithTag("proyecto-$id")
    }

    private fun buscar(texto: String) {
        compose.onNodeWithTag("proyectos-lista").performScrollToIndex(0)
        compose.onNodeWithTag("buscar-proyectos").performTextReplacement(texto)
        compose.onNodeWithTag("buscar-proyectos").performImeAction()
    }

    private fun elegirEstado(filtro: Filtro) {
        compose.onNodeWithTag("proyectos-lista").performScrollToNode(hasTestTag("filtros-estado"))
        compose.onNodeWithTag("filtros-estado").performScrollToNode(hasTestTag("estado-${filtro.name}"))
        compose.onNodeWithTag("estado-${filtro.name}").performClick()
    }

    private fun proyecto(id: Long, titulo: String, participacion: ParticipacionProyecto) = Proyecto(
        id = id, codigo = "TEC-$id", titulo = titulo, estado = EstadoProyecto.EN_DESARROLLO,
        descripcion = null, tecnologias = emptyList(), fechaEntrega = null, participacion = participacion
    )
}