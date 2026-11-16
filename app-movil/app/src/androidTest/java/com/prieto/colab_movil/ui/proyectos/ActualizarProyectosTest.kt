package com.prieto.colab_movil.ui.proyectos

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import com.prieto.colab_movil.data.model.*
import com.prieto.colab_movil.data.repository.ProyectoRepository
import com.prieto.colab_movil.data.repository.PlanificacionRepository
import com.prieto.colab_movil.ui.invitaciones.InvitacionesUiState
import com.prieto.colab_movil.ui.invitaciones.InvitacionesViewModel
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActualizarProyectosTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun proyectosConservaListaSiFallaActualizacionParcial() {
        val repo = Repositorio()
        val vm = ProyectosViewModel(repo)
        compose.runOnIdle { vm.cargarDatos() }
        compose.waitUntil(5_000) { vm.uiState.value is ProyectosUiState.Success }
        compose.runOnIdle { repo.fallarProyectos = true; vm.cargarDatos() }
        compose.waitUntil(5_000) { (vm.uiState.value as? ProyectosUiState.Success)?.aviso != null }
        val estado = vm.uiState.value as ProyectosUiState.Success
        assertEquals("Proyecto", estado.proyectos.single().nombre)
        assertFalse(estado.actualizando)
    }

    @Test
    fun detalleConservaEquipoYPlanificacionSiFallaLaConsulta() {
        val repo = Repositorio()
        val plan = PlanRepository()
        val vm = DetalleProyectoViewModel(repo, plan)
        compose.runOnIdle { vm.cargarDetalle(7) }
        compose.waitUntil(5_000) { vm.uiState.value is DetalleProyectoUiState.Success }
        compose.runOnIdle { repo.fallarEquipo = true; plan.fallar = true; vm.cargarDetalle(7) }
        compose.waitUntil(5_000) { (vm.uiState.value as? DetalleProyectoUiState.Success)?.datosDesactualizados == true }
        val estado = vm.uiState.value as DetalleProyectoUiState.Success
        assertTrue(estado.tienePlanificacion)
        assertEquals(1, estado.equipo.size)
        assertNotNull(estado.aviso)
    }

    @Test
    fun invitacionesNoDuplicaOperacionesYRecuperaDespuesDeFallo() {
        val repo = Repositorio()
        val vm = InvitacionesViewModel(repo)
        compose.waitUntil(5_000) { vm.uiState.value is InvitacionesUiState.Success }
        val espera = CompletableDeferred<Result<Unit>>()
        compose.runOnIdle {
            repo.aceptacion = espera
            vm.aceptar(1)
            vm.aceptar(1)
            vm.cargar()
        }
        compose.runOnIdle { assertEquals(1, repo.aceptaciones); assertTrue((vm.uiState.value as InvitacionesUiState.Success).ocupado) }
        compose.runOnIdle { repo.fallarInvitaciones = true; espera.complete(Result.success(Unit)) }
        compose.waitUntil(5_000) { (vm.uiState.value as? InvitacionesUiState.Success)?.desactualizado == true }
        compose.runOnIdle { repo.fallarInvitaciones = false; vm.cargar() }
        compose.waitUntil(5_000) { (vm.uiState.value as? InvitacionesUiState.Success)?.let { !it.desactualizado && !it.actualizando } == true }
    }

    private class Repositorio : ProyectoRepository {
        var fallarProyectos = false
        var fallarInvitaciones = false
        var fallarEquipo = false
        var aceptaciones = 0
        var aceptacion: CompletableDeferred<Result<Unit>>? = null
        override suspend fun obtenerPerfil() = Result.success(PerfilResponseDto("a@tecsup.edu.pe", "Alex", emptyList()))
        override suspend fun listarProyectos(): Result<List<ProyectoResponseDto>> =
            if (fallarProyectos) Result.failure(Exception("Error")) else Result.success(listOf(Gson().fromJson("""{"id":7,"nombre":"Proyecto"}""", ProyectoResponseDto::class.java)))
        override suspend fun listarInvitaciones(): Result<List<InvitacionResponseDto>> = if (fallarInvitaciones) Result.failure(Exception("Error")) else Result.success(emptyList())
        override suspend fun aceptarInvitacion(id: Long): Result<Unit> { aceptaciones++; return aceptacion?.await() ?: Result.success(Unit) }
        override suspend fun rechazarInvitacion(id: Long): Result<Unit> = error("No se usa")
        override suspend fun verProyecto(id: Long): Result<ProyectoResponseDto> = Result.success(Gson().fromJson("""{"id":7,"nombre":"Proyecto"}""", ProyectoResponseDto::class.java))
        override suspend fun verEquipo(id: Long): Result<List<IntegranteResponseDto>> = if (fallarEquipo) Result.failure(Exception("Error")) else Result.success(listOf(Gson().fromJson("""{"id":1}""", IntegranteResponseDto::class.java)))
        override suspend fun listarTecnologias(): Result<List<TecnologiaDto>> = error("No se usa")
        override suspend fun crearProyecto(request: ProyectoRequest): Result<ProyectoResponseDto> = error("No se usa")
        override suspend fun editarProyecto(id: Long, request: ProyectoRequest): Result<ProyectoResponseDto> = error("No se usa")
    }

    private class PlanRepository : PlanificacionRepository {
        var fallar = false
        override suspend fun verPlanificacion(proyectoId: Long): Result<PlanificacionResponseDto?> =
            if (fallar) Result.failure(Exception("Error")) else Result.success(PlanificacionResponseDto(3, proyectoId, "PROPUESTA", emptyList()))
        override suspend fun generarPlanificacion(proyectoId: Long): Result<PlanificacionResponseDto> = error("No se usa")
        override suspend fun guardarPlanificacion(proyectoId: Long, request: PlanificacionUpdateRequestDto): Result<PlanificacionResponseDto> = error("No se usa")
        override suspend fun crearEtapa(proyectoId: Long, request: EtapaRequestDto): Result<EtapaResponseDto> = error("No se usa")
        override suspend fun editarEtapa(proyectoId: Long, etapaId: Long, request: EtapaRequestDto): Result<EtapaResponseDto> = error("No se usa")
        override suspend fun eliminarEtapa(proyectoId: Long, etapaId: Long): Result<Unit> = error("No se usa")
        override suspend fun crearTarea(proyectoId: Long, request: TareaRequestDto): Result<TareaResponseDto> = error("No se usa")
        override suspend fun editarTarea(proyectoId: Long, tareaId: Long, request: TareaRequestDto): Result<TareaResponseDto> = error("No se usa")
        override suspend fun eliminarTarea(proyectoId: Long, tareaId: Long): Result<Unit> = error("No se usa")
        override suspend fun agregarDependencia(proyectoId: Long, tareaId: Long, predecesoraId: Long): Result<Unit> = error("No se usa")
        override suspend fun quitarDependencia(proyectoId: Long, tareaId: Long, predecesoraId: Long): Result<Unit> = error("No se usa")
    }
}