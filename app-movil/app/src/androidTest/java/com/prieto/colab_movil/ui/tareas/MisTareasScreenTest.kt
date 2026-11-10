package com.prieto.colab_movil.ui.tareas

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import com.prieto.colab_movil.data.model.ConfirmacionResponseDto
import com.prieto.colab_movil.data.model.PerfilResponseDto
import com.prieto.colab_movil.data.model.PlanificacionResponseDto
import com.prieto.colab_movil.data.model.ProyectoResponseDto
import com.prieto.colab_movil.data.model.TareaDetalleResponseDto
import com.prieto.colab_movil.data.repository.MisTareasDatos
import com.prieto.colab_movil.data.repository.MisTareasRepository
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MisTareasScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun mostrar(repository: RepositorioPrueba = RepositorioPrueba()): MisTareasViewModel {
        val vm = MisTareasViewModel(repository)
        compose.setContent {
            MaterialTheme {
                MisTareasScreen(proyectoId = 7, onBack = {}, onPerfil = {}, viewModel = vm)
            }
        }
        compose.waitForIdle()
        return vm
    }

    @Test
    fun muestraDatosRealesYFiltraSinInventarConfirmaciones() {
        mostrar()
        compose.onNodeWithText("Proyecto de prueba").assertIsDisplayed()
        compose.onNodeWithText("Hola, Alexis").assertIsDisplayed()
        compose.onNodeWithText("API de matrícula").assertIsDisplayed()
        compose.onNodeWithText("Confirmadas (0)").performClick()
        compose.onNodeWithText("No hay tareas en esta categoría.").assertIsDisplayed()
        compose.onNodeWithText("Por confirmar (1)").performClick()
        compose.onNodeWithText("API de matrícula").assertIsDisplayed()
    }

    @Test
    fun confirmaTodasSoloTrasAceptarDialogoYRecarga() {
        val repository = RepositorioPrueba()
        mostrar(repository)
        compose.onNodeWithText("Confirmar todas").performClick()
        compose.onNodeWithText("Confirmar tus tareas").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, repository.confirmaciones) }
        compose.onAllNodesWithText("Confirmar todas").onLast().performClick()
        compose.onNodeWithText("Esperando confirmaciones del equipo").assertIsDisplayed()
        compose.onNodeWithText("Confirmar todas").assertIsNotEnabled()
        compose.onNodeWithText("ALTA").assertExists()
        compose.runOnIdle {
            assertEquals(1, repository.confirmaciones)
            assertTrue(repository.cargas >= 2)
        }
    }

    @Test
    fun solicitudVaciaNoSeEnviaYElBorradorSePuedeDescartarSinReabrir() {
        val repository = RepositorioPrueba()
        mostrar(repository)
        compose.onNodeWithText("Solicitar ajuste").performScrollTo().performClick()
        compose.onNodeWithText("Enviar solicitud").assertIsNotEnabled()
        compose.onNodeWithText("Motivos del ajuste").performTextInput("Necesito apoyo")
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("¿Descartar el borrador?").assertIsDisplayed()
        compose.onNodeWithText("Descartar").performClick()
        compose.onNodeWithText("Solicitar ajuste · TK-21").assertDoesNotExist()
        compose.runOnIdle { assertNull(repository.comentario) }
    }

    @Test
    fun solicitudIncluyeContextoDeLaTareaYSeRecargaTrasGuardar() {
        val repository = RepositorioPrueba()
        mostrar(repository)
        compose.onNodeWithText("Solicitar ajuste").performScrollTo().performClick()
        compose.onNodeWithText("Motivos del ajuste").performTextInput("Necesito apoyo")
        compose.onNodeWithText("Enviar solicitud").performClick()
        compose.onNodeWithText("Solicitar ajuste · TK-21").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals("TK-21 · API de matrícula\nNecesito apoyo", repository.comentario)
            assertTrue(repository.cargas >= 2)
        }
    }

    @Test
    fun errorDeRecargaBloqueaAccionesHastaActualizar() {
        val repository = RepositorioPrueba()
        mostrar(repository)
        compose.runOnIdle { repository.fallarCarga = true }
        compose.onNodeWithContentDescription("Actualizar tareas").performClick()
        compose.onNodeWithText("Confirmar todas").assertIsNotEnabled()
        compose.runOnIdle { repository.fallarCarga = false }
        compose.onNodeWithContentDescription("Actualizar tareas").performClick()
        compose.onNodeWithText("Confirmar todas").assertIsEnabled()
    }

    @Test
    fun noDuplicaConfirmacionMientrasLaPeticionEstaEnCurso() {
        val repository = RepositorioPrueba()
        val pendiente = CompletableDeferred<Result<ConfirmacionResponseDto>>()
        repository.respuestaPendiente = pendiente
        val vm = mostrar(repository)
        compose.runOnIdle {
            vm.confirmar(7)
            vm.confirmar(7)
            assertEquals(1, repository.confirmaciones)
        }
        compose.onNodeWithText("Confirmar todas").assertIsNotEnabled()
        compose.runOnIdle { pendiente.complete(Result.failure(IllegalStateException("Error de red"))) }
        compose.onNodeWithText("Confirmar todas").assertIsEnabled()
        compose.runOnIdle { assertTrue(repository.cargas >= 2) }
    }

    @Test
    fun planificacionActivaSustituyeConfirmacionYAbreLaTareaSeleccionada() {
        val repository = RepositorioPrueba()
        repository.datos = repository.datos.copy(planificacion = repository.datos.planificacion!!.copy(estado = "ACTIVA"))
        val vm = MisTareasViewModel(repository)
        var seleccionada: Long? = null
        compose.setContent {
            MaterialTheme {
                MisTareasScreen(7, {}, {}, vm, onRegistrarAvance = { seleccionada = it })
            }
        }
        compose.onNodeWithText("Confirmar todas").assertDoesNotExist()
        compose.onNodeWithText("Registrar avance").assertIsEnabled().performClick()
        compose.onNodeWithText("Selecciona una tarea").assertIsDisplayed()
        compose.onNodeWithText("TK-21 · API de matrícula").performClick()
        compose.runOnIdle { assertEquals(21L, seleccionada); assertEquals(0, repository.confirmaciones) }
    }

    @Test
    fun recargarDetectaActivacionDelEquipoYCambiaElBoton() {
        val repository = RepositorioPrueba()
        mostrar(repository)
        compose.onNodeWithText("Confirmar todas").assertExists()
        compose.runOnIdle {
            repository.datos = repository.datos.copy(planificacion = repository.datos.planificacion!!.copy(estado = "ACTIVA"))
        }
        compose.onNodeWithContentDescription("Actualizar tareas").performClick()
        compose.onNodeWithText("Confirmar todas").assertDoesNotExist()
        compose.onNodeWithText("Registrar avance").assertIsEnabled()
        compose.onNodeWithText("Por confirmar (1)").assertDoesNotExist()
    }

    private class RepositorioPrueba : MisTareasRepository {
        var cargas = 0
        var confirmaciones = 0
        var comentario: String? = null
        var fallarCarga = false
        var respuestaPendiente: CompletableDeferred<Result<ConfirmacionResponseDto>>? = null
        private val gson = Gson()
        var datos = MisTareasDatos(
            proyecto = gson.fromJson("""{"id":7,"nombre":"Proyecto de prueba"}""", ProyectoResponseDto::class.java),
            perfil = PerfilResponseDto("alexis@ejemplo.com", "Alexis", emptyList()),
            planificacion = PlanificacionResponseDto(3, 7, "PROPUESTA", emptyList()),
            tareas = listOf(gson.fromJson("""{"id":21,"nombre":"API de matrícula","descripcion":"Validar cupos","etapaId":2,"etapaNombre":"Backend","responsableId":12,"responsableEmail":"alexis@ejemplo.com","prioridad":"ALTA","estado":"PENDIENTE","duracionDias":5,"dependencias":[]}""", TareaDetalleResponseDto::class.java)),
            confirmacion = null
        )

        override suspend fun cargar(proyectoId: Long): Result<MisTareasDatos> {
            cargas++
            return if (fallarCarga) Result.failure(IllegalStateException("Error de red")) else Result.success(datos)
        }

        override suspend fun confirmar(proyectoId: Long): Result<ConfirmacionResponseDto> {
            confirmaciones++
            respuestaPendiente?.let { return it.await() }
            val respuesta = respuesta("ACEPTADA")
            datos = datos.copy(confirmacion = respuesta)
            return Result.success(respuesta)
        }

        override suspend fun solicitarCambios(proyectoId: Long, comentario: String): Result<ConfirmacionResponseDto> {
            this.comentario = comentario
            val respuesta = respuesta("CAMBIOS_SOLICITADOS").copy(comentario = comentario)
            datos = datos.copy(confirmacion = respuesta)
            return Result.success(respuesta)
        }

        private fun respuesta(estado: String) = ConfirmacionResponseDto(
            1, 12, "alexis@ejemplo.com", "Alexis", estado, null, "2026-11-09T10:20:30"
        )
    }
}
