package com.prieto.colab_movil.ui.proyectos

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
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
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class ActualizarProyectosTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun proyectosConservaListaSiFallaActualizacionParcial() {
        val repo = Repositorio()
        val vm = ProyectosViewModel(repo)
        compose.runOnIdle { vm.cargarDatos() }
        compose.waitUntil(5_000) { (vm.uiState.value as? ProyectosUiState.Success)?.actualizando == false }
        compose.runOnIdle { repo.fallarProyectos = true; vm.cargarDatos() }
        compose.waitUntil(5_000) { (vm.uiState.value as? ProyectosUiState.Success)?.let { !it.actualizando && it.aviso != null } == true }
        val estado = vm.uiState.value as ProyectosUiState.Success
        assertEquals("Proyecto", estado.proyectos.single().nombre)
        assertFalse(estado.actualizando)
    }

    @Test
    fun tarjetasMuestranIconosDeTecnologiasConDescripcionAccesible() {
        val proyecto = Proyecto(
            id = 7, codigo = "TEC-7", titulo = "Proyecto", estado = EstadoProyecto.SIN_PLANIFICACION,
            descripcion = null, tecnologias = listOf(TecnologiaDto(1, "Kotlin", "kotlin"), TecnologiaDto(2, "Personalizada")),
            fechaEntrega = null
        )
        var abierto: Proyecto? = null
        compose.setContent { ProyectosContent(proyectos = listOf(proyecto), onProyectoClick = { abierto = it }) }
        compose.onNodeWithContentDescription("Tecnología: Kotlin", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("Tecnología: Personalizada", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Kotlin", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("Proyecto").performClick()
        compose.runOnIdle { assertEquals(proyecto, abierto) }
    }

    @Test
    fun tarjetaMuestraAvataresActivosJuntoALaEntregaYLosConservaSiFallaElEquipo() {
        val repo = Repositorio().apply {
            equipo = listOf(
                IntegranteResponseDto(1, "a@tecsup.edu.pe", "Alex Torres", "CREADOR", "ACTIVO"),
                IntegranteResponseDto(2, "b@tecsup.edu.pe", "Beatriz Vega", "INTEGRANTE", "ACTIVO"),
                IntegranteResponseDto(3, "c@tecsup.edu.pe", "Camila Luna", "INTEGRANTE", "PENDIENTE")
            )
        }
        val vm = ProyectosViewModel(repo)
        compose.setContent { ProyectosScreen(viewModel = vm) }
        compose.waitUntil(5_000) { (vm.uiState.value as? ProyectosUiState.Success)?.actualizando == false }
        compose.onNodeWithText("Entrega: Por definir").assertIsDisplayed()
        compose.onNodeWithContentDescription("Creador: Alex Torres").assertIsDisplayed()
        compose.onNodeWithContentDescription("Integrante: Beatriz Vega").assertIsDisplayed()
        compose.onNodeWithContentDescription("Integrante: Camila Luna").assertDoesNotExist()
        compose.runOnIdle { repo.fallarEquipo = true; vm.cargarDatos() }
        compose.waitUntil(5_000) { (vm.uiState.value as? ProyectosUiState.Success)?.aviso?.contains("actualizar todos los equipos") == true }
        compose.onNodeWithContentDescription("Integrante: Beatriz Vega").assertIsDisplayed()
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
    fun detalleAbreMetricasInclusoSinPlanificacion() {
        val repo = Repositorio().apply { proyecto = proyecto.copy(estado = "SIN_PLANIFICACION") }
        val vm = DetalleProyectoViewModel(repo, PlanRepository().apply { sinPlan = true })
        var abierto = false
        compose.setContent { DetalleProyectoScreen(proyectoId = 7, viewModel = vm, onMetricas = { abierto = true }) }
        compose.waitUntil(5_000) { vm.uiState.value is DetalleProyectoUiState.Success }
        compose.onNodeWithText("Ver métricas").performClick()
        compose.runOnIdle { assertTrue(abierto) }
    }

    @Test
    fun cancelarMantieneElDetalleYEliminarRequiereElNombre() {
        val repo = Repositorio()
        val vm = DetalleProyectoViewModel(repo, PlanRepository())
        var eliminado = 0L
        compose.setContent { DetalleProyectoScreen(proyectoId = 7, viewModel = vm, onEliminado = { eliminado = it }) }
        compose.waitUntil(5_000) { vm.uiState.value is DetalleProyectoUiState.Success }
        compose.onNodeWithContentDescription("Más acciones del proyecto").performClick()
        compose.onNodeWithText("Cancelar proyecto").performClick()
        compose.onNodeWithText("Confirmar cancelación").performClick()
        compose.onNodeWithText("Cancelado").assertIsDisplayed()
        compose.onNodeWithText("Mis tareas e historial").assertExists()
        compose.onNodeWithContentDescription("Más acciones del proyecto").performClick()
        compose.onNodeWithText("Eliminar proyecto").performClick()
        compose.onNodeWithText("Eliminar definitivamente").assertIsNotEnabled()
        compose.onNodeWithText("Escribe el nombre del proyecto").performTextInput("Proyecto")
        compose.onNodeWithText("Eliminar definitivamente").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(7L, eliminado) }
    }

    @Test
    fun conflictoRefrescaElEstadoSinRepetirLaCancelacionNiMostrarEditar() {
        val repo = Repositorio().apply { conflicto = true }
        val vm = DetalleProyectoViewModel(repo, PlanRepository())
        compose.runOnIdle { vm.cargarDetalle(7) }
        compose.waitUntil(5_000) { vm.uiState.value is DetalleProyectoUiState.Success }
        compose.runOnIdle {
            vm.cambiarCiclo(7, AccionProyecto.CANCELAR)
            vm.cambiarCiclo(7, AccionProyecto.CANCELAR)
        }
        compose.waitUntil(5_000) { (vm.uiState.value as? DetalleProyectoUiState.Success)?.aviso?.contains("estado del proyecto cambió") == true }
        val estado = vm.uiState.value as DetalleProyectoUiState.Success
        assertEquals("FINALIZADO", estado.proyecto.estado)
        assertEquals(listOf(AccionProyecto.ELIMINAR), estado.acciones)
        assertEquals(1, repo.cancelaciones)
    }

    @Test
    fun eliminarNoReapareceSiFallaLaRecargaDelListado() {
        val repo = Repositorio()
        val vm = ProyectosViewModel(repo)
        compose.runOnIdle { vm.cargarDatos() }
        compose.waitUntil(5_000) { (vm.uiState.value as? ProyectosUiState.Success)?.actualizando == false }
        compose.runOnIdle { repo.fallarProyectos = true; vm.retirarEliminado(7) }
        compose.waitUntil(5_000) { (vm.uiState.value as? ProyectosUiState.Success)?.aviso != null }
        assertTrue((vm.uiState.value as ProyectosUiState.Success).proyectos.isEmpty())
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

    @Test
    fun invitarDesdeEquipoEnviaElCorreoYActualizaElPendiente() {
        val repo = Repositorio().apply { proyecto = proyecto.copy(estado = "SIN_PLANIFICACION") }
        val vm = DetalleProyectoViewModel(repo, PlanRepository().apply { sinPlan = true })
        compose.setContent { DetalleProyectoScreen(proyectoId = 7, viewModel = vm) }
        compose.waitUntil(5_000) { vm.uiState.value is DetalleProyectoUiState.Success }
        compose.onNodeWithText("Equipo (1)").performScrollTo().performClick()
        compose.onNodeWithText("＋  Invitar integrante").performScrollTo().performClick()
        compose.onNodeWithText("Correo institucional").performTextInput(" b@tecsup.edu.pe ")
        compose.onNodeWithText("Invitar", substring = false).performClick()
        compose.waitUntil(5_000) { (vm.uiState.value as? DetalleProyectoUiState.Success)?.equipo?.size == 2 }
        compose.runOnIdle {
            assertEquals(listOf(7L to "b@tecsup.edu.pe"), repo.envios)
            val estado = vm.uiState.value as DetalleProyectoUiState.Success
            assertEquals("PENDIENTE", estado.equipo.last().estado)
            assertTrue(estado.aviso!!.contains("Invitación enviada"))
            assertFalse(estado.enviandoInvitacion)
        }
    }

    @Test
    fun invitarNoDuplicaElEnvioMientrasEstaPendiente() {
        val repo = Repositorio().apply { proyecto = proyecto.copy(estado = "SIN_PLANIFICACION") }
        val vm = DetalleProyectoViewModel(repo, PlanRepository().apply { sinPlan = true })
        compose.runOnIdle { vm.cargarDetalle(7) }
        compose.waitUntil(5_000) { vm.uiState.value is DetalleProyectoUiState.Success }
        val espera = CompletableDeferred<Result<Unit>>()
        compose.runOnIdle {
            repo.envio = espera
            vm.invitarIntegrante(7, "b@tecsup.edu.pe")
            vm.invitarIntegrante(7, "b@tecsup.edu.pe")
        }
        compose.runOnIdle {
            assertEquals(1, repo.envios.size)
            assertTrue((vm.uiState.value as DetalleProyectoUiState.Success).enviandoInvitacion)
            espera.complete(Result.success(Unit))
        }
        compose.waitUntil(5_000) { (vm.uiState.value as? DetalleProyectoUiState.Success)?.aviso != null }
    }

    @Test
    fun invitarMuestraUsuarioNoRegistradoSinSimularExito() {
        val repo = Repositorio().apply {
            proyecto = proyecto.copy(estado = "SIN_PLANIFICACION")
            envio = CompletableDeferred(Result.failure(HttpException(Response.error<Unit>(404, "".toResponseBody()))))
        }
        val vm = DetalleProyectoViewModel(repo, PlanRepository().apply { sinPlan = true })
        compose.runOnIdle { vm.cargarDetalle(7) }
        compose.waitUntil(5_000) { vm.uiState.value is DetalleProyectoUiState.Success }
        compose.runOnIdle { vm.invitarIntegrante(7, "b@tecsup.edu.pe") }
        compose.waitUntil(5_000) { (vm.uiState.value as? DetalleProyectoUiState.Success)?.aviso != null }
        val estado = vm.uiState.value as DetalleProyectoUiState.Success
        assertTrue(estado.aviso!!.contains("no está registrado"))
        assertEquals(1, estado.equipo.size)
        assertTrue(estado.puedeModificar)
    }

    private class Repositorio : ProyectoRepository {
        var proyecto = Gson().fromJson("""{"id":7,"nombre":"Proyecto","estado":"EN_DESARROLLO","puedeCancelar":true}""", ProyectoResponseDto::class.java)
        var conflicto = false
        var cancelaciones = 0
        var fallarProyectos = false
        var fallarInvitaciones = false
        var fallarEquipo = false
        var equipo = listOf(IntegranteResponseDto(1, "a@tecsup.edu.pe", "Alex", "CREADOR", "ACTIVO"))
        var aceptaciones = 0
        var aceptacion: CompletableDeferred<Result<Unit>>? = null
        val envios = mutableListOf<Pair<Long, String>>()
        var envio: CompletableDeferred<Result<Unit>>? = null
        override suspend fun invitarIntegrante(proyectoId: Long, email: String): Result<Unit> {
            envios.add(proyectoId to email)
            val resultado = envio?.await() ?: Result.success(Unit)
            if (resultado.isSuccess) equipo = equipo + IntegranteResponseDto(2, email, "Beatriz", "INTEGRANTE", "PENDIENTE")
            return resultado
        }
        override suspend fun obtenerPerfil() = Result.success(PerfilResponseDto("a@tecsup.edu.pe", "Alex", emptyList()))
        override suspend fun listarProyectos(): Result<List<ProyectoResponseDto>> =
            if (fallarProyectos) Result.failure(Exception("Error")) else Result.success(listOf(Gson().fromJson("""{"id":7,"nombre":"Proyecto"}""", ProyectoResponseDto::class.java)))
        override suspend fun listarInvitaciones(): Result<List<InvitacionResponseDto>> = if (fallarInvitaciones) Result.failure(Exception("Error")) else Result.success(emptyList())
        override suspend fun aceptarInvitacion(id: Long): Result<Unit> { aceptaciones++; return aceptacion?.await() ?: Result.success(Unit) }
        override suspend fun rechazarInvitacion(id: Long): Result<Unit> = error("No se usa")
        override suspend fun verProyecto(id: Long): Result<ProyectoResponseDto> = Result.success(proyecto)
        override suspend fun verEquipo(id: Long): Result<List<IntegranteResponseDto>> =
            if (fallarEquipo) Result.failure(Exception("Error"))
            else Result.success(equipo)
        override suspend fun listarTecnologias(): Result<List<TecnologiaDto>> = error("No se usa")
        override suspend fun crearProyecto(request: ProyectoRequest): Result<ProyectoResponseDto> = error("No se usa")
        override suspend fun editarProyecto(id: Long, request: ProyectoRequest): Result<ProyectoResponseDto> = error("No se usa")
        override suspend fun cancelarProyecto(id: Long): Result<ProyectoResponseDto> {
            cancelaciones++
            proyecto = proyecto.copy(estado = if (conflicto) "FINALIZADO" else "CANCELADO", puedeCancelar = false, puedeEliminar = true)
            return if (conflicto) Result.failure(HttpException(Response.error<ProyectoResponseDto>(409, "".toResponseBody()))) else Result.success(proyecto)
        }
        override suspend fun eliminarProyecto(id: Long): Result<Unit> = Result.success(Unit)
    }

    private class PlanRepository : PlanificacionRepository {
        var fallar = false
        var sinPlan = false
        override suspend fun verPlanificacion(proyectoId: Long): Result<PlanificacionResponseDto?> =
            if (fallar) Result.failure(Exception("Error")) else Result.success(
                if (sinPlan) null else PlanificacionResponseDto(3, proyectoId, "PROPUESTA", emptyList())
            )
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