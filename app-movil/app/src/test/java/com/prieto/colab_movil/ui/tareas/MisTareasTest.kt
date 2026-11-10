package com.prieto.colab_movil.ui.tareas

import com.google.gson.JsonParser
import com.prieto.colab_movil.data.remote.MisTareasApiService
import com.prieto.colab_movil.data.remote.PerfilApiService
import com.prieto.colab_movil.data.remote.PlanificacionApiService
import com.prieto.colab_movil.data.remote.ProyectoApiService
import com.prieto.colab_movil.data.repository.MisTareasDatos
import com.prieto.colab_movil.data.repository.MisTareasRepositoryImpl
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetSocketAddress
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/** Pruebas con HTTP local real y las interfaces Retrofit de producción, sin backend externo. */
class MisTareasTest {
    private lateinit var server: HttpServer
    private lateinit var repository: MisTareasRepositoryImpl
    private val respuestas = ConcurrentHashMap<String, Pair<Int, String>>()
    private val peticiones = Collections.synchronizedList(mutableListOf<Pair<String, String>>())
    private val base = "/api/proyectos/7"
    private val confirmacion = """{"id":1,"integranteId":12,"integranteEmail":" ALEXIS@ejemplo.com ","integranteNombre":"Alexis","estado":"ACEPTADA","comentario":null,"fecha":"2026-11-09T10:20:30"}"""

    @Before
    fun preparar() {
        respuestas["GET $base"] = 200 to """{"id":7,"nombre":"Proyecto real"}"""
        respuestas["GET /api/usuario/perfil"] = 200 to """{"email":"alexis@ejemplo.com","nombre":"Alexis","habilidades":[]}"""
        respuestas["GET $base/planificacion"] = 200 to """{"id":3,"proyectoId":7,"estado":"PROPUESTA","etapas":[]}"""
        respuestas["GET $base/tareas/mis-tareas"] = 200 to """[{"id":21,"nombre":"API de matrícula","descripcion":"Validar cupos","etapaId":2,"etapaNombre":"Backend","responsableId":12,"responsableEmail":"alexis@ejemplo.com","prioridad":"ALTA","estado":"PENDIENTE","duracionDias":5,"fechaInicio":"2026-11-10","fechaFin":"2026-11-14","orden":1,"dependencias":[20]}]"""
        respuestas["GET $base/planificacion/confirmaciones"] = 200 to "[]"
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val ruta = "${exchange.requestMethod} ${exchange.requestURI.path}"
            peticiones.add(ruta to exchange.requestBody.bufferedReader().use { it.readText() })
            val (codigo, texto) = respuestas[ruta] ?: (500 to """{"detail":"Ruta inesperada: $ruta"}""")
            val bytes = texto.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
            exchange.sendResponseHeaders(codigo, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://127.0.0.1:${server.address.port}/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        repository = MisTareasRepositoryImpl(
            retrofit.create(MisTareasApiService::class.java),
            retrofit.create(ProyectoApiService::class.java),
            retrofit.create(PerfilApiService::class.java),
            retrofit.create(PlanificacionApiService::class.java)
        )
    }

    @After
    fun cerrar() {
        server.stop(0)
    }

    private fun cargar(): MisTareasDatos = runBlocking { repository.cargar(7).getOrThrow() }

    @Test
    fun cargaTareasDelEndpointAutenticadoYConservaSusCampos() {
        val datos = cargar()
        assertEquals("Proyecto real", datos.proyecto.nombre)
        assertEquals("Alexis", datos.perfil.nombre)
        val tarea = datos.tareas.single()
        assertEquals(21L, tarea.id)
        assertEquals("Backend", tarea.etapaNombre)
        assertEquals(12L, tarea.responsableId)
        assertEquals("2026-11-10", tarea.fechaInicio)
        assertEquals(listOf(20L), tarea.dependencias)
        assertNull(datos.confirmacion)
        assertTrue(peticiones.any { it.first == "GET $base/tareas/mis-tareas" })
    }

    @Test
    fun soloSeleccionaLaConfirmacionDelUsuarioActualNormalizandoEmail() {
        respuestas["GET $base/planificacion/confirmaciones"] = 200 to """[{"id":2,"integranteId":13,"integranteEmail":"otro@ejemplo.com","integranteNombre":"Otro","estado":"ACEPTADA"},$confirmacion]"""
        assertEquals(12L, cargar().confirmacion?.integranteId)
    }

    @Test
    fun sinPlanificacionNoGeneraNiConsultaConfirmacionesInexistentes() {
        respuestas["GET $base/planificacion"] = 404 to "{}"
        val datos = cargar()
        assertNull(datos.planificacion)
        assertTrue(datos.tareas.isEmpty())
        assertFalse(peticiones.any { it.first.startsWith("POST") || it.first.endsWith("/mis-tareas") || it.first.endsWith("/confirmaciones") })
    }

    @Test
    fun errorDePermisosNoSeInterpretaComoAusenciaDePlanificacion() = runBlocking {
        respuestas["GET $base/planificacion"] = 403 to """{"detail":"No perteneces al proyecto"}"""
        val resultado = repository.cargar(7)
        assertTrue(resultado.isFailure)
        assertEquals("No perteneces al proyecto", resultado.exceptionOrNull()?.message)
    }

    @Test
    fun falloEnConfirmacionesNoMuestraFalsamenteTareasPendientes() = runBlocking {
        respuestas["GET $base/planificacion/confirmaciones"] = 500 to "{}"
        assertTrue(repository.cargar(7).isFailure)
    }

    @Test
    fun confirmarUsaEndpointDePlanificacionYNoCambiaLaPrioridad() = runBlocking {
        respuestas["POST $base/planificacion/confirmar"] = 200 to confirmacion
        assertEquals("ACEPTADA", repository.confirmar(7).getOrThrow().estado)
        assertEquals("POST $base/planificacion/confirmar", peticiones.single().first)
    }

    @Test
    fun solicitudPersisteComentarioSinEspaciosExteriores() = runBlocking {
        respuestas["POST $base/planificacion/solicitar-cambios"] = 200 to confirmacion.replace("ACEPTADA", "CAMBIOS_SOLICITADOS")
        val resultado = repository.solicitarCambios(7, "  TK-21: necesito apoyo  ").getOrThrow()
        assertEquals("CAMBIOS_SOLICITADOS", resultado.estado)
        assertEquals("POST $base/planificacion/solicitar-cambios", peticiones.single().first)
        assertEquals("TK-21: necesito apoyo", JsonParser.parseString(peticiones.single().second).asJsonObject["comentario"].asString)
    }

    @Test
    fun comentarioVacioNoHacePeticiones() = runBlocking {
        assertTrue(repository.solicitarCambios(7, " \n ").isFailure)
        assertTrue(peticiones.isEmpty())
    }

    @Test
    fun conflictoDelServidorConservaElMotivoReal() = runBlocking {
        respuestas["POST $base/planificacion/confirmar"] = 409 to """{"detail":"Solo se puede confirmar una planificación en estado PROPUESTA"}"""
        val resultado = repository.confirmar(7)
        assertTrue(resultado.isFailure)
        assertTrue(resultado.exceptionOrNull()!!.message!!.contains("PROPUESTA"))
    }

    @Test
    fun filtrosPendientesYAccionesAntesDeConfirmar() {
        val estado = MisTareasUiState.Success(cargar())
        assertEquals(1, estado.tareasVisibles(FiltroTareas.TODAS).size)
        assertEquals(1, estado.tareasVisibles(FiltroTareas.POR_CONFIRMAR).size)
        assertTrue(estado.tareasVisibles(FiltroTareas.CONFIRMADAS).isEmpty())
        assertTrue(estado.puedeConfirmar)
        assertTrue(estado.puedeSolicitarCambios)
    }

    @Test
    fun confirmacionAbarcaTodasLasTareasPeroConservaEstadoYPrioridad() {
        respuestas["GET $base/planificacion/confirmaciones"] = 200 to "[$confirmacion]"
        val estado = MisTareasUiState.Success(cargar())
        assertTrue(estado.confirmadas)
        assertTrue(estado.tareasVisibles(FiltroTareas.POR_CONFIRMAR).isEmpty())
        assertEquals(1, estado.tareasVisibles(FiltroTareas.CONFIRMADAS).size)
        assertEquals("ALTA", estado.datos.tareas.single().prioridad)
        assertEquals("PENDIENTE", estado.datos.tareas.single().estado)
        assertFalse(estado.puedeConfirmar)
        assertTrue(estado.puedeSolicitarCambios)
    }

    @Test
    fun noPermiteAccionesDuranteGuardadoRecargaODatosDesactualizados() {
        val estado = MisTareasUiState.Success(cargar())
        listOf(
            estado.copy(isSaving = true),
            estado.copy(isRefreshing = true),
            estado.copy(datosDesactualizados = true),
            estado.copy(datos = estado.datos.copy(tareas = emptyList())),
            estado.copy(datos = estado.datos.copy(planificacion = null)),
            estado.copy(datos = estado.datos.copy(planificacion = estado.datos.planificacion!!.copy(estado = "ACTIVA")))
        ).forEach {
            assertFalse(it.puedeConfirmar)
            assertFalse(it.puedeSolicitarCambios)
        }
    }
}
