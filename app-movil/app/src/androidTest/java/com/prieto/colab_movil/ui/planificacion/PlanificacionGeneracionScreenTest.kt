package com.prieto.colab_movil.ui.planificacion

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import com.prieto.colab_movil.data.model.*
import com.prieto.colab_movil.data.repository.*
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicLong

@RunWith(AndroidJUnit4::class)
class PlanificacionGeneracionScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun mostrar(repo: RepositorioPlan): PlanificacionViewModel {
        val vm = PlanificacionViewModel(repo, RepositorioProyecto(), esperarRecuperacion = {})
        compose.setContent { MaterialTheme { PlanificacionScreen(proyectoId = repo.id, viewModel = vm) } }
        compose.waitForIdle()
        return vm
    }

    @Test
    fun postExitosoMuestraResultadoSinVolverAConsultarNiRegenerar() {
        val repo = RepositorioPlan()
        val vm = mostrar(repo)
        compose.runOnIdle {
            assertTrue(vm.uiState.value is PlanificacionUiState.Success)
            assertEquals(1, repo.posts)
            assertEquals(1, repo.gets)
        }
        compose.onNodeWithContentDescription("Editar planificación").assertIsEnabled()
    }

    @Test
    fun timeoutRecuperaPlanGuardadoSinMostrarErrorNiRepetirPost() {
        val repo = RepositorioPlan().apply {
            error = GeneracionPlanificacionInciertaException(SocketTimeoutException())
            disponibleEnConsulta = 4
        }
        val vm = mostrar(repo)
        compose.runOnIdle {
            assertTrue(vm.uiState.value is PlanificacionUiState.Success)
            assertEquals(1, repo.posts)
            assertEquals(4, repo.gets)
        }
        compose.onNodeWithText("No se pudo completar la operación").assertDoesNotExist()
    }

    @Test
    fun siSiguePendienteConsultarResultadoNoGeneraOtraVez() {
        val repo = RepositorioPlan().apply {
            error = GeneracionPlanificacionInciertaException(SocketTimeoutException())
        }
        val vm = mostrar(repo)
        compose.onNodeWithText("Generación pendiente de confirmar").assertIsDisplayed()
        compose.onNodeWithContentDescription("Editar planificación").assertIsNotEnabled()
        compose.runOnIdle {
            assertTrue(vm.uiState.value is PlanificacionUiState.PendingGeneration)
            repo.disponibleEnConsulta = repo.gets + 1
        }
        compose.onNodeWithText("Consultar resultado").performClick()
        compose.runOnIdle {
            assertTrue(vm.uiState.value is PlanificacionUiState.Success)
            assertEquals(1, repo.posts)
        }
    }

    @Test
    fun pulsacionesRepetidasDurantePostSoloGeneranUnaVez() {
        val repo = RepositorioPlan().apply { pendiente = CompletableDeferred() }
        val vm = mostrar(repo)
        compose.onNodeWithText("Generando planificación con IA…").assertIsDisplayed()
        compose.onNodeWithContentDescription("Editar planificación").assertIsNotEnabled()
        compose.runOnIdle {
            vm.generar(repo.id)
            vm.cargar(repo.id)
            vm.consultarResultado(repo.id)
            assertEquals(1, repo.posts)
            repo.pendiente!!.complete(Result.success(repo.plan))
        }
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(vm.uiState.value is PlanificacionUiState.Success) }
    }

    @Test
    fun planExistenteNoLlamaALaIA() {
        val repo = RepositorioPlan().apply { disponibleEnConsulta = 1 }
        val vm = mostrar(repo)
        compose.runOnIdle {
            assertTrue(vm.uiState.value is PlanificacionUiState.Success)
            assertEquals(0, repo.posts)
        }
    }

    @Test
    fun errorDeValidacionMuestraMotivoYReintentoConsultaAntesDeGenerar() {
        val repo = RepositorioPlan().apply { error = PlanificacionHttpException(400, "Faltan dos integrantes con habilidades") }
        val vm = mostrar(repo)
        compose.onNodeWithText("Faltan dos integrantes con habilidades").assertIsDisplayed()
        compose.runOnIdle {
            assertTrue(vm.uiState.value is PlanificacionUiState.Error)
            assertEquals(1, repo.posts)
            repo.error = null
            vm.generar(repo.id)
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(vm.uiState.value is PlanificacionUiState.Success)
            assertEquals(2, repo.posts)
            assertEquals(2, repo.gets)
        }
    }

    @Test
    fun errorDeConsultaNoIniciaGeneracion() {
        val repo = RepositorioPlan().apply { errorConsulta = PlanificacionHttpException(403, "Sin permisos") }
        mostrar(repo)
        compose.onNodeWithText("Sin permisos").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, repo.posts) }
    }

    @Test
    fun volverAEntrarTrasCancelarLaPantallaConsultaSinDuplicarElPost() {
        val repo = RepositorioPlan().apply { pendiente = CompletableDeferred() }
        val primero = mostrar(repo)
        val store = ViewModelStore()
        compose.runOnIdle {
            store.put("plan", primero)
            store.clear()
        }
        val segundo = PlanificacionViewModel(repo, RepositorioProyecto(), esperarRecuperacion = {})
        compose.runOnIdle { segundo.cargar(repo.id) }
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(segundo.uiState.value is PlanificacionUiState.PendingGeneration)
            assertEquals(1, repo.posts)
            repo.disponibleEnConsulta = repo.gets + 1
            segundo.consultarResultado(repo.id)
        }
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(segundo.uiState.value is PlanificacionUiState.Success) }
    }

    private class RepositorioPlan : PlanificacionRepository {
        val id = ids.incrementAndGet()
        val plan = PlanificacionResponseDto(3, id, "PROPUESTA", emptyList())
        var gets = 0
        var posts = 0
        var disponibleEnConsulta = Int.MAX_VALUE
        var error: Exception? = null
        var errorConsulta: Exception? = null
        var pendiente: CompletableDeferred<Result<PlanificacionResponseDto>>? = null
        override suspend fun verPlanificacion(proyectoId: Long): Result<PlanificacionResponseDto?> {
            gets++
            errorConsulta?.let { return Result.failure(it) }
            return Result.success(if (gets >= disponibleEnConsulta) plan else null)
        }
        override suspend fun generarPlanificacion(proyectoId: Long): Result<PlanificacionResponseDto> {
            posts++
            pendiente?.let { return it.await() }
            error?.let { return Result.failure(it) }
            return Result.success(plan)
        }
        override suspend fun crearEtapa(proyectoId: Long, request: EtapaRequestDto): Result<EtapaResponseDto> = error("No se usa")
        override suspend fun editarEtapa(proyectoId: Long, etapaId: Long, request: EtapaRequestDto): Result<EtapaResponseDto> = error("No se usa")
        override suspend fun eliminarEtapa(proyectoId: Long, etapaId: Long): Result<Unit> = error("No se usa")
        override suspend fun crearTarea(proyectoId: Long, request: TareaRequestDto): Result<TareaResponseDto> = error("No se usa")
        override suspend fun editarTarea(proyectoId: Long, tareaId: Long, request: TareaRequestDto): Result<TareaResponseDto> = error("No se usa")
        override suspend fun eliminarTarea(proyectoId: Long, tareaId: Long): Result<Unit> = error("No se usa")
        override suspend fun agregarDependencia(proyectoId: Long, tareaId: Long, predecesoraId: Long): Result<Unit> = error("No se usa")
        override suspend fun quitarDependencia(proyectoId: Long, tareaId: Long, predecesoraId: Long): Result<Unit> = error("No se usa")
    }

    private class RepositorioProyecto : ProyectoRepository {
        override suspend fun verProyecto(id: Long) = Result.success(
            Gson().fromJson("""{"id":$id,"nombre":"Proyecto IA"}""", ProyectoResponseDto::class.java))
        override suspend fun verEquipo(id: Long): Result<List<IntegranteResponseDto>> = Result.success(emptyList())
        override suspend fun obtenerPerfil(): Result<PerfilResponseDto> = error("No se usa")
        override suspend fun listarProyectos(): Result<List<ProyectoResponseDto>> = error("No se usa")
        override suspend fun listarInvitaciones(): Result<List<InvitacionResponseDto>> = error("No se usa")
        override suspend fun aceptarInvitacion(id: Long): Result<Unit> = error("No se usa")
        override suspend fun rechazarInvitacion(id: Long): Result<Unit> = error("No se usa")
        override suspend fun listarTecnologias(): Result<List<TecnologiaDto>> = error("No se usa")
        override suspend fun crearProyecto(request: ProyectoRequest): Result<ProyectoResponseDto> = error("No se usa")
        override suspend fun editarProyecto(id: Long, request: ProyectoRequest): Result<ProyectoResponseDto> = error("No se usa")
    }

    companion object { private val ids = AtomicLong(100_000) }
}