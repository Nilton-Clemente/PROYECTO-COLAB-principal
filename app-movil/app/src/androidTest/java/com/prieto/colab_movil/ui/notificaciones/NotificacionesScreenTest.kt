package com.prieto.colab_movil.ui.notificaciones

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prieto.colab_movil.data.model.NotificacionResponseDto
import com.prieto.colab_movil.data.repository.NotificacionesRepository
import com.prieto.colab_movil.ui.proyectos.ProyectosContent
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificacionesScreenTest {
    @get:Rule val compose = createComposeRule()
    private val mensaje = "Ana solicitó cambios en la planificación del proyecto \"Matrícula\" (TEC-7): TK-21 · API de matrícula\nNecesito revisar los cupos."

    private fun mostrar(repo: Repositorio = Repositorio()): NotificacionesViewModel {
        val vm = NotificacionesViewModel(repo)
        compose.setContent { MaterialTheme { NotificacionesScreen(onBack = {}, viewModel = vm) } }
        return vm
    }

    @Test
    fun muestraMensajeCompletoSinMarcarloAutomaticamenteYConfirmaLectura() {
        val repo = Repositorio()
        val vm = mostrar(repo)
        compose.onNodeWithText(mensaje).assertIsDisplayed()
        compose.onNodeWithText("1 sin leer").assertIsDisplayed()
        compose.runOnIdle { assertTrue(repo.lecturas.isEmpty()) }
        compose.onNodeWithTag("leer-notificacion-7").performClick()
        compose.onNodeWithText("Leída").assertIsDisplayed()
        compose.onNodeWithText("0 sin leer").assertIsDisplayed()
        compose.onNodeWithText(mensaje).assertExists()
        compose.runOnIdle {
            assertEquals(listOf(7L), repo.lecturas)
            assertTrue((vm.uiState.value as NotificacionesUiState.Success).notificaciones.single().leida)
        }
    }

    @Test
    fun primerErrorOfreceReintentarEnLugarDeBandejaVacia() {
        val repo = Repositorio().apply { fallarCarga = true }
        mostrar(repo)
        compose.onNodeWithText("Sin conexión").assertIsDisplayed()
        compose.onNodeWithText("Todavía no tienes notificaciones.").assertDoesNotExist()
        compose.runOnIdle { repo.fallarCarga = false }
        compose.onNodeWithText("Reintentar").performClick()
        compose.onNodeWithText(mensaje).assertIsDisplayed()
    }

    @Test
    fun diferenciaAvisosDelMismoTipoPorNombreYCodigoDelProyecto() {
        val repo = Repositorio().apply {
            avisos = listOf(
                aviso().copy(mensaje = "Planificación activada", proyectoId = 7,
                    proyectoNombre = "Matrícula", proyectoCodigo = "TEC-7"),
                aviso().copy(id = 8, mensaje = "Planificación activada", proyectoId = 8,
                    proyectoNombre = "Biblioteca", proyectoCodigo = "TEC-8")
            )
        }
        mostrar(repo)
        compose.onNodeWithTag("notificacion-proyecto-7").assertTextEquals("Matrícula · TEC-7").assertIsDisplayed()
        compose.onNodeWithTag("notificacion-proyecto-8").assertTextEquals("Biblioteca · TEC-8").assertIsDisplayed()
    }

    @Test
    fun avisoAntiguoNoDeduceProyectoDesdeElMensaje() {
        mostrar()
        compose.onNodeWithTag("notificacion-proyecto-7").assertTextEquals("Proyecto no identificado")
        compose.onNodeWithText(mensaje).assertIsDisplayed()
    }

    @Test
    fun contextoParcialMuestraCodigoDelIdSinNombreInventado() {
        val repo = Repositorio().apply {
            avisos = listOf(aviso().copy(proyectoId = 7, proyectoNombre = " ", proyectoCodigo = ""))
        }
        mostrar(repo)
        compose.onNodeWithTag("notificacion-proyecto-7").assertTextEquals("TEC-7")
    }

    @Test
    fun actualizarConErrorConservaMensajesYPermiteRecuperar() {
        val repo = Repositorio()
        val vm = mostrar(repo)
        compose.onNodeWithText(mensaje).assertExists()
        compose.runOnIdle { repo.fallarCarga = true }
        compose.runOnIdle { vm.cargar() }
        compose.onNodeWithText(mensaje).assertExists()
        compose.onNodeWithText("Sin conexión Desliza para reintentar.").assertIsDisplayed()
        compose.onNodeWithTag("leer-notificacion-7").assertIsNotEnabled()
        compose.runOnIdle { repo.fallarCarga = false }
        compose.runOnIdle { vm.cargar() }
        compose.onNodeWithTag("leer-notificacion-7").assertIsEnabled()
    }

    @Test
    fun errorAlMarcarNoInventaUnaLecturaYConsultaAntesDeReintentar() {
        val repo = Repositorio().apply { fallarLectura = true }
        mostrar(repo)
        compose.onNodeWithTag("leer-notificacion-7").performClick()
        compose.onNodeWithText("1 sin leer").assertIsDisplayed()
        compose.onNodeWithText("No se pudo guardar la lectura").assertIsDisplayed()
        compose.onNodeWithText("Leída").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(2, repo.cargas)
            assertEquals(listOf(7L), repo.lecturas)
        }
    }

    @Test
    fun respuestaPerdidaSeReconciliaConLecturaRealDelServidor() {
        val repo = Repositorio().apply { fallarLectura = true; guardarAntesDeFallar = true }
        mostrar(repo)
        compose.onNodeWithTag("leer-notificacion-7").performClick()
        compose.onNodeWithText("0 sin leer").assertIsDisplayed()
        compose.onNodeWithText("Leída").assertIsDisplayed()
        compose.onNodeWithTag("leer-notificacion-7").assertDoesNotExist()
    }

    @Test
    fun noDuplicaLecturasNiActualizacionesMientrasHayOperacion() {
        val repo = Repositorio()
        val vm = mostrar(repo)
        val espera = CompletableDeferred<Result<NotificacionResponseDto>>()
        compose.onNodeWithText(mensaje).assertExists()
        compose.runOnIdle {
            repo.esperaLectura = espera
            vm.marcarLeida(7)
            vm.marcarLeida(7)
            vm.cargar()
        }
        compose.onNodeWithTag("leer-notificacion-7").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(listOf(7L), repo.lecturas)
            assertEquals(1, repo.cargas)
            espera.complete(Result.success(repo.avisos.single().copy(leida = true)))
        }
        compose.onNodeWithText("0 sin leer").assertIsDisplayed()
    }

    @Test
    fun bandejaVaciaNoInventaMensajesYDeslizarConsultaOtraVez() {
        val repo = Repositorio().apply { avisos = emptyList() }
        mostrar(repo)
        compose.onNodeWithText("Todavía no tienes notificaciones.").assertIsDisplayed()
        compose.onNodeWithText("0 sin leer").assertIsDisplayed()
        compose.runOnIdle { repo.avisos = listOf(aviso()) }
        compose.onNodeWithTag("notificaciones-actualizar").performTouchInput { swipeDown() }
        compose.waitUntil(5_000) { repo.cargas > 1 }
        compose.onNodeWithText(mensaje).assertExists()
    }

    @Test
    fun campanaAbreBandejaYAlVolverReflejaLaLecturaEnElContador() {
        val repo = Repositorio()
        val vm = NotificacionesViewModel(repo)
        compose.setContent {
            val estado by vm.uiState.collectAsState()
            var bandeja by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { vm.cargar() }
            MaterialTheme {
                if (bandeja) NotificacionesScreen(onBack = { bandeja = false }, viewModel = vm)
                else ProyectosContent(
                    notificacionesSinLeer = (estado as? NotificacionesUiState.Success)?.noLeidas,
                    onNotificacionesClick = { bandeja = true }
                )
            }
        }
        compose.onNodeWithContentDescription("Notificaciones").assert(hasStateDescription("1 notificaciones sin leer")).performClick()
        compose.onNodeWithText(mensaje).assertIsDisplayed()
        compose.onNodeWithTag("leer-notificacion-7").performClick()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithContentDescription("Notificaciones").assert(hasStateDescription("0 notificaciones sin leer"))
    }

    @Test
    fun eliminarLeidasPideConfirmacionYConservaLasNoLeidas() {
        val repo = Repositorio().apply { avisos = listOf(aviso(), aviso().copy(id = 8, leida = true)) }
        val vm = mostrar(repo)
        compose.onNodeWithContentDescription("Actualizar notificaciones").assertDoesNotExist()
        compose.onNodeWithTag("eliminar-notificaciones-leidas").assertIsEnabled().performClick()
        compose.onNodeWithText("¿Eliminar notificaciones leídas?").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, repo.eliminaciones) }
        compose.onNodeWithText("Cancelar", substring = false).performClick()
        compose.onNodeWithTag("notificacion-8").assertExists()
        compose.runOnIdle { assertEquals(0, repo.eliminaciones) }
        compose.onNodeWithTag("eliminar-notificaciones-leidas").performClick()
        compose.onNodeWithText("Eliminar", substring = false).performClick()
        compose.onNodeWithTag("notificacion-8").assertDoesNotExist()
        compose.onNodeWithTag("notificacion-7").assertExists()
        compose.onNodeWithText("1 sin leer").assertIsDisplayed()
        compose.onNodeWithTag("eliminar-notificaciones-leidas").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(1, repo.eliminaciones)
            assertEquals(listOf(7L), (vm.uiState.value as NotificacionesUiState.Success).notificaciones.map { it.id })
        }
    }

    @Test
    fun eliminarEstaDeshabilitadoSinLeidasYSeHabilitaAlConfirmarUnaLectura() {
        val repo = Repositorio()
        val vm = mostrar(repo)
        compose.onNodeWithTag("eliminar-notificaciones-leidas").assertIsNotEnabled()
        compose.runOnIdle { vm.eliminarLeidas(); assertEquals(0, repo.eliminaciones) }
        compose.onNodeWithTag("leer-notificacion-7").performClick()
        compose.onNodeWithTag("eliminar-notificaciones-leidas").assertIsEnabled().performClick()
        compose.onNodeWithText("Eliminar", substring = false).performClick()
        compose.onNodeWithText("Todavía no tienes notificaciones.").assertIsDisplayed()
        compose.onNodeWithText("0 sin leer").assertIsDisplayed()
        compose.onNodeWithTag("eliminar-notificaciones-leidas").assertIsNotEnabled()
    }

    @Test
    fun borradoEsperaAlServidorYNoDuplicaOperaciones() {
        val repo = Repositorio().apply { avisos = listOf(aviso(), aviso().copy(id = 8, leida = true)) }
        val vm = mostrar(repo)
        val espera = CompletableDeferred<Result<Unit>>()
        compose.onNodeWithTag("notificacion-8").assertExists()
        compose.runOnIdle {
            repo.esperaEliminacion = espera
            vm.eliminarLeidas()
            vm.eliminarLeidas()
            vm.marcarLeida(7)
            vm.cargar()
        }
        compose.onNodeWithTag("notificacion-8").assertExists()
        compose.onNodeWithTag("eliminar-notificaciones-leidas").assertIsNotEnabled()
        compose.onNodeWithTag("leer-notificacion-7").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(1, repo.eliminaciones)
            assertEquals(1, repo.cargas)
            assertTrue(repo.lecturas.isEmpty())
            espera.complete(Result.success(Unit))
        }
        compose.onNodeWithTag("notificacion-8").assertDoesNotExist()
        compose.onNodeWithTag("leer-notificacion-7").assertIsEnabled()
    }

    @Test
    fun falloAlEliminarConservaAvisosYRespuestaPerdidaSeReconciliaSinRepetirDelete() {
        val repo = Repositorio().apply {
            avisos = listOf(aviso(), aviso().copy(id = 8, leida = true))
            fallarEliminacion = true
        }
        val vm = mostrar(repo)
        compose.onNodeWithTag("eliminar-notificaciones-leidas").performClick()
        compose.onNodeWithText("Eliminar", substring = false).performClick()
        compose.onNodeWithTag("notificacion-8").assertExists()
        compose.onNodeWithText("No se pudo confirmar la eliminación").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(1, repo.eliminaciones)
            assertEquals(2, repo.cargas)
            repo.borrarAntesDeFallar = true
            vm.eliminarLeidas()
        }
        compose.onNodeWithTag("notificacion-8").assertDoesNotExist()
        compose.onNodeWithText("1 sin leer").assertIsDisplayed()
        compose.runOnIdle { assertEquals(2, repo.eliminaciones); assertEquals(3, repo.cargas) }
    }

    @Test
    fun borradoConfirmadoNoRestauraLeidasSiFallaRecargaYPermiteRecuperarDeslizando() {
        val repo = Repositorio().apply { avisos = listOf(aviso(), aviso().copy(id = 8, leida = true)) }
        val vm = mostrar(repo)
        compose.onNodeWithTag("notificacion-8").assertExists()
        compose.runOnIdle { repo.fallarCarga = true; vm.eliminarLeidas() }
        compose.onNodeWithTag("notificacion-8").assertDoesNotExist()
        compose.onNodeWithTag("notificacion-7").assertExists()
        compose.onNodeWithTag("eliminar-notificaciones-leidas").assertIsNotEnabled()
        compose.runOnIdle {
            val estado = vm.uiState.value as NotificacionesUiState.Success
            assertTrue(estado.desactualizado)
            assertFalse(estado.ocupado)
            repo.fallarCarga = false
        }
        compose.onNodeWithTag("notificaciones-actualizar").performTouchInput { swipeDown() }
        compose.waitUntil(5_000) { (vm.uiState.value as? NotificacionesUiState.Success)?.desactualizado == false }
        compose.onNodeWithTag("leer-notificacion-7").assertIsEnabled()
    }

    @Test
    fun falloDeBorradoYRecargaBloqueaAccionesHastaRecuperarDatos() {
        val repo = Repositorio().apply {
            avisos = listOf(aviso(), aviso().copy(id = 8, leida = true))
            fallarEliminacion = true
        }
        val vm = mostrar(repo)
        compose.onNodeWithTag("notificacion-8").assertExists()
        compose.runOnIdle { repo.fallarCarga = true; vm.eliminarLeidas() }
        compose.onNodeWithTag("notificacion-8").assertExists()
        compose.onNodeWithTag("eliminar-notificaciones-leidas").assertIsNotEnabled()
        compose.runOnIdle {
            assertTrue((vm.uiState.value as NotificacionesUiState.Success).desactualizado)
            vm.eliminarLeidas()
            assertEquals(1, repo.eliminaciones)
            repo.fallarCarga = false
            vm.cargar()
        }
        compose.onNodeWithTag("eliminar-notificaciones-leidas").assertIsEnabled()
    }

    private fun aviso() = NotificacionResponseDto(7, "PLANIFICACION", mensaje, false, "2026-11-25T10:30:00")

    private inner class Repositorio : NotificacionesRepository {
        var avisos = listOf(aviso())
        var cargas = 0
        var fallarCarga = false
        var fallarLectura = false
        var guardarAntesDeFallar = false
        var esperaLectura: CompletableDeferred<Result<NotificacionResponseDto>>? = null
        val lecturas = mutableListOf<Long>()
        var eliminaciones = 0
        var fallarEliminacion = false
        var borrarAntesDeFallar = false
        var esperaEliminacion: CompletableDeferred<Result<Unit>>? = null

        override suspend fun eliminarLeidas(): Result<Unit> {
            eliminaciones++
            val resultado = esperaEliminacion?.await() ?: if (fallarEliminacion)
                Result.failure(Exception("No se pudo confirmar la eliminación")) else Result.success(Unit)
            if (resultado.isSuccess || borrarAntesDeFallar) avisos = avisos.filterNot { it.leida }
            return resultado
        }

        override suspend fun listar(): Result<List<NotificacionResponseDto>> {
            cargas++
            return if (fallarCarga) Result.failure(Exception("Sin conexión")) else Result.success(avisos)
        }

        override suspend fun marcarLeida(id: Long): Result<NotificacionResponseDto> {
            lecturas.add(id)
            esperaLectura?.let { return it.await() }
            val leida = avisos.first { it.id == id }.copy(leida = true)
            if (!fallarLectura || guardarAntesDeFallar) avisos = avisos.map { if (it.id == id) leida else it }
            return if (fallarLectura) Result.failure(Exception("No se pudo guardar la lectura")) else Result.success(leida)
        }
    }
}