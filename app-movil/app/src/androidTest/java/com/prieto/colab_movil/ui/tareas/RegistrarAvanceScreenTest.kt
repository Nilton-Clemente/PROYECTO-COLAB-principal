package com.prieto.colab_movil.ui.tareas

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import com.prieto.colab_movil.data.model.*
import com.prieto.colab_movil.data.repository.*
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class RegistrarAvanceScreenTest {
    @get:Rule val compose = createComposeRule()
    private val nota = "Implementación y pruebas de seguridad"

    private fun mostrar(repository: RepositorioPrueba = RepositorioPrueba(), onBack: () -> Unit = {}): RegistrarAvanceViewModel {
        val vm = RegistrarAvanceViewModel(repository)
        compose.setContent { MaterialTheme { RegistrarAvanceScreen(7, 21, onBack, {}, vm) } }
        compose.waitForIdle()
        return vm
    }

    private fun escribirNota() {
        desplazarA("Descripción del avance")
        compose.onNodeWithText("Descripción del avance").performTextInput(nota)
        compose.waitForIdle()
    }

    private fun desplazarA(texto: String, substring: Boolean = false) {
        compose.onNodeWithTag("avance-contenido").performScrollToNode(hasText(texto, substring = substring))
    }

    @Test
    fun muestraContextoRealYGuardarRequiereDescripcion() {
        mostrar()
        compose.onNodeWithText("API real").assertIsDisplayed()
        compose.onNodeWithText("Proyecto real").assertIsDisplayed()
        compose.onNodeWithText("5 días").assertIsDisplayed()
        compose.onNodeWithText("Guardar avance").assertIsNotEnabled()
        escribirNota()
        compose.onNodeWithText("Guardar avance").assertIsEnabled()
    }

    @Test
    fun guardarActualizaHistorialYLimpiaFormularioSinCambiarEstado() {
        val repo = RepositorioPrueba()
        mostrar(repo)
        escribirNota()
        compose.onNodeWithText("Guardar avance").performClick()
        compose.onNodeWithText("Guardar avance").assertIsNotEnabled()
        desplazarA("Historial de sesiones (1)")
        compose.onNodeWithText("Historial de sesiones (1)").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(1, repo.registros)
            assertEquals(30, repo.ultimoRegistro!!.minutos)
            assertEquals(nota, repo.ultimoRegistro!!.nota)
            assertEquals(0, repo.cambiosEstado)
            assertTrue(repo.cargas >= 2)
        }
    }

    @Test
    fun cancelarPideDescartarYNoRegistraTiempo() {
        val repo = RepositorioPrueba()
        var salidas = 0
        mostrar(repo) { salidas++ }
        escribirNota()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("¿Descartar el avance sin guardar?").assertIsDisplayed()
        compose.onNodeWithText("Descartar").performClick()
        compose.runOnIdle { assertEquals(1, salidas); assertEquals(0, repo.registros) }
    }

    @Test
    fun errorDeRecargaBloqueaGuardadoHastaActualizarSinPerderBorrador() {
        val repo = RepositorioPrueba()
        mostrar(repo)
        escribirNota()
        compose.runOnIdle { repo.fallarCarga = true }
        compose.onNodeWithContentDescription("Actualizar avance").performClick()
        compose.onNodeWithText("Guardar avance").assertIsNotEnabled()
        compose.runOnIdle { repo.fallarCarga = false }
        compose.onNodeWithContentDescription("Actualizar avance").performClick()
        compose.onNodeWithText("Guardar avance").assertIsEnabled()
    }

    @Test
    fun doblePulsacionNoDuplicaPeticion() {
        val repo = RepositorioPrueba()
        val pendiente = CompletableDeferred<Result<RegistroAvanceResponseDto>>()
        repo.pendiente = pendiente
        val vm = mostrar(repo)
        compose.runOnIdle {
            vm.registrar(7, 21, 30, nota) {}
            vm.registrar(7, 21, 30, nota) {}
            assertEquals(1, repo.registros)
        }
        compose.onNodeWithText("Procesando…").assertIsNotEnabled()
        compose.runOnIdle { pendiente.complete(Result.failure(IllegalStateException("Registro rechazado"))) }
    }

    @Test
    fun resultadoInciertoExigeRevisarHistorialAntesDeReintentar() {
        val repo = RepositorioPrueba()
        repo.errorRegistro = RegistroAvanceInciertoException(IOException("Respuesta perdida"))
        mostrar(repo)
        escribirNota()
        compose.onNodeWithText("Guardar avance").performClick()
        compose.onNodeWithText("Guardar avance").assertIsNotEnabled()
        desplazarA("Ya revisé el historial")
        compose.onNodeWithText("Ya revisé el historial").performClick()
        compose.onNodeWithText("Guardar avance").assertIsEnabled()
        compose.runOnIdle { assertEquals(1, repo.registros) }
    }

    @Test
    fun estadoSeActualizaSoloTrasConfirmarYSinRegistrarHoras() {
        val repo = RepositorioPrueba()
        mostrar(repo)
        desplazarA("Completada")
        compose.onNodeWithText("Completada").performClick()
        compose.runOnIdle { assertEquals(0, repo.cambiosEstado) }
        compose.onNodeWithText("Actualizar estado").performClick()
        compose.runOnIdle { assertEquals(1, repo.cambiosEstado); assertEquals(0, repo.registros) }
        desplazarA("Actual: Completada")
        compose.onNodeWithText("Actual: Completada").assertIsDisplayed()
    }

    @Test
    fun tareaBloqueadaNoPermiteCambiarEstadoManualmente() {
        val repo = RepositorioPrueba()
        repo.datos = repo.datos.copy(tarea = repo.datos.tarea.copy(estado = "BLOQUEADA"))
        val vm = mostrar(repo)
        compose.runOnIdle {
            EstadoTarea.entries.forEach { vm.cambiarEstado(7, 21, it) }
            assertEquals(0, repo.cambiosEstado)
        }
        desplazarA("El bloqueo se calcula", substring = true)
        compose.onNodeWithText("El bloqueo se calcula", substring = true).assertIsDisplayed()
    }

    @Test
    fun fueraDePlanificacionActivaNoSePermiteRegistrar() {
        val repo = RepositorioPrueba()
        repo.datos = repo.datos.copy(planificacion = repo.datos.planificacion.copy(estado = "PROPUESTA"))
        val vm = mostrar(repo)
        compose.onNodeWithText("Guardar avance").assertIsNotEnabled()
        compose.runOnIdle { vm.registrar(7, 21, 30, nota) {}; assertEquals(0, repo.registros) }
    }

    @Test
    fun rechazoDelServidorConservaBorradorYNoCambiaEstado() {
        val repo = RepositorioPrueba()
        repo.errorRegistro = IllegalStateException("No tienes permisos")
        mostrar(repo)
        escribirNota()
        compose.onNodeWithText("Guardar avance").performClick()
        compose.onNodeWithText("Guardar avance").assertIsEnabled()
        desplazarA("Descripción del avance")
        compose.onNode(hasSetTextAction()).assertTextContains(nota)
        compose.runOnIdle { assertEquals(1, repo.registros); assertEquals(0, repo.cambiosEstado) }
    }

    @Test
    fun guardadoExitosoConRecargaFallidaNoReenviaLaSesion() {
        val repo = RepositorioPrueba()
        repo.fallarDespuesDeGuardar = true
        mostrar(repo)
        escribirNota()
        compose.onNodeWithText("Guardar avance").performClick()
        compose.onNodeWithText("Guardar avance").assertIsNotEnabled()
        compose.runOnIdle { repo.fallarCarga = false }
        compose.onNodeWithContentDescription("Actualizar avance").performClick()
        compose.onNodeWithText("Guardar avance").assertIsNotEnabled()
        desplazarA("Historial de sesiones (1)")
        compose.onNodeWithText("Historial de sesiones (1)").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, repo.registros) }
    }

    @Test
    fun pendienteNoPuedeCompletarseDirectamenteNiRegistrarEnOtraTarea() {
        val repo = RepositorioPrueba()
        repo.datos = repo.datos.copy(tarea = repo.datos.tarea.copy(estado = "PENDIENTE"))
        val vm = mostrar(repo)
        compose.runOnIdle {
            vm.cambiarEstado(7, 21, EstadoTarea.COMPLETADA)
            vm.cambiarEstado(7, 22, EstadoTarea.EN_PROGRESO)
            vm.registrar(7, 22, 30, nota) {}
            assertEquals(0, repo.cambiosEstado)
            assertEquals(0, repo.registros)
            vm.cambiarEstado(7, 21, EstadoTarea.EN_PROGRESO)
            assertEquals(1, repo.cambiosEstado)
        }
    }

    private class RepositorioPrueba : AvanceRepository {
        var cargas = 0
        var registros = 0
        var cambiosEstado = 0
        var fallarCarga = false
        var fallarDespuesDeGuardar = false
        var errorRegistro: Exception? = null
        var ultimoRegistro: RegistroAvanceRequestDto? = null
        var pendiente: CompletableDeferred<Result<RegistroAvanceResponseDto>>? = null
        var datos = AvanceDatos(
            Gson().fromJson("""{"id":7,"nombre":"Proyecto real"}""", ProyectoResponseDto::class.java),
            PerfilResponseDto("alexis@ejemplo.com", "Alexis", emptyList()),
            PlanificacionResponseDto(3, 7, "ACTIVA", emptyList()),
            Gson().fromJson("""{"id":21,"nombre":"API real","etapaId":2,"etapaNombre":"Backend","estado":"EN_PROGRESO","duracionDias":5,"dependencias":[]}""", TareaDetalleResponseDto::class.java),
            HistorialAvanceResponseDto(90, emptyList())
        )

        override suspend fun cargar(proyectoId: Long, tareaId: Long): Result<AvanceDatos> {
            cargas++
            return if (fallarCarga) Result.failure(IOException("Error de red")) else Result.success(datos)
        }

        override suspend fun registrar(proyectoId: Long, tareaId: Long, request: RegistroAvanceRequestDto): Result<RegistroAvanceResponseDto> {
            registros++
            ultimoRegistro = request
            pendiente?.let { return it.await() }
            errorRegistro?.let { return Result.failure(it) }
            val registro = RegistroAvanceResponseDto(91, tareaId, 8, "Alexis", request.minutos, "2026-11-09T10:30:00", request.nota)
            datos = datos.copy(historial = HistorialAvanceResponseDto(datos.historial.totalMinutos + request.minutos, listOf(registro)))
            if (fallarDespuesDeGuardar) fallarCarga = true
            return Result.success(registro)
        }

        override suspend fun cambiarEstado(proyectoId: Long, tareaId: Long, nuevoEstado: String): Result<TareaDetalleResponseDto> {
            cambiosEstado++
            datos = datos.copy(tarea = datos.tarea.copy(estado = nuevoEstado))
            return Result.success(datos.tarea)
        }
    }
}
