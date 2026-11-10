package com.prieto.colab_movil.ui.tareas

import com.google.gson.JsonParser
import com.prieto.colab_movil.data.model.RegistroAvanceRequestDto
import com.prieto.colab_movil.data.remote.*
import com.prieto.colab_movil.data.repository.AvanceRepositoryImpl
import com.prieto.colab_movil.data.repository.RegistroAvanceInciertoException
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

class RegistrarAvanceTest {
    private lateinit var server: HttpServer
    private lateinit var repository: AvanceRepositoryImpl
    private val respuestas = ConcurrentHashMap<String, Pair<Int, String>>()
    private val peticiones = Collections.synchronizedList(mutableListOf<Pair<String, String>>())
    private val base = "/api/proyectos/7"
    private val tarea = """{"id":21,"nombre":"API real","etapaId":2,"etapaNombre":"Backend","responsableId":12,"responsableEmail":"alexis@ejemplo.com","estado":"EN_PROGRESO","duracionDias":5,"dependencias":[]}"""
    private val registro = """{"id":91,"tareaId":21,"usuarioId":8,"usuarioNombre":"Alexis","minutos":90,"fecha":"2026-11-09T10:30:00","nota":"Implementación y pruebas de seguridad"}"""

    @Before
    fun preparar() {
        respuestas["GET $base"] = 200 to """{"id":7,"nombre":"Proyecto real"}"""
        respuestas["GET /api/usuario/perfil"] = 200 to """{"email":"alexis@ejemplo.com","nombre":"Alexis","habilidades":[]}"""
        respuestas["GET $base/planificacion"] = 200 to """{"id":3,"proyectoId":7,"estado":"ACTIVA","etapas":[]}"""
        respuestas["GET $base/tareas/mis-tareas"] = 200 to "[$tarea]"
        respuestas["GET $base/tareas/21/avances"] = 200 to """{"totalMinutos":90,"registros":[$registro]}"""
        respuestas["POST $base/tareas/21/avances"] = 201 to registro
        respuestas["PUT $base/tareas/21/estado"] = 200 to tarea.replace("EN_PROGRESO", "COMPLETADA")
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val ruta = "${exchange.requestMethod} ${exchange.requestURI.path}"
            peticiones.add(ruta to exchange.requestBody.bufferedReader().use { it.readText() })
            val (codigo, texto) = respuestas[ruta] ?: (500 to """{"detail":"Ruta inesperada"}""")
            val bytes = texto.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
            exchange.sendResponseHeaders(codigo, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        val retrofit = Retrofit.Builder().baseUrl("http://127.0.0.1:${server.address.port}/")
            .addConverterFactory(GsonConverterFactory.create()).build()
        repository = AvanceRepositoryImpl(
            retrofit.create(AvanceApiService::class.java),
            retrofit.create(MisTareasApiService::class.java),
            retrofit.create(ProyectoApiService::class.java),
            retrofit.create(PerfilApiService::class.java),
            retrofit.create(PlanificacionApiService::class.java)
        )
    }

    @After
    fun cerrar() { server.stop(0) }

    @Test
    fun cargaContextoYAcumuladoRealesSinConvertirDiasEnHoras() = runBlocking {
        val datos = repository.cargar(7, 21).getOrThrow()
        assertEquals("Proyecto real", datos.proyecto.nombre)
        assertEquals("Alexis", datos.perfil.nombre)
        assertEquals(5, datos.tarea.duracionDias)
        assertEquals(90L, datos.historial.totalMinutos)
        assertEquals(91L, datos.historial.registros.single().id)
        assertEquals("1,5 h", horasAvance(datos.historial.totalMinutos))
        assertTrue(peticiones.all { it.first.startsWith("GET") })
    }

    @Test
    fun tareaNoAsignadaNoCargaSuHistorial() = runBlocking {
        assertTrue(repository.cargar(7, 22).isFailure)
        assertFalse(peticiones.any { it.first.endsWith("/avances") })
    }

    @Test
    fun guardaMinutosYNotaSinCambiarEstado() = runBlocking {
        assertEquals(91L, repository.registrar(7, 21, RegistroAvanceRequestDto(90, "  Implementación y pruebas de seguridad  ")).getOrThrow().id)
        val (ruta, body) = peticiones.single()
        assertEquals("POST $base/tareas/21/avances", ruta)
        val json = JsonParser.parseString(body).asJsonObject
        assertEquals(90, json["minutos"].asInt)
        assertEquals("Implementación y pruebas de seguridad", json["nota"].asString)
        assertEquals(setOf("minutos", "nota"), json.keySet())
    }

    @Test
    fun validaDescripcionYTiempoAntesDeHacerPost() = runBlocking {
        listOf(
            RegistroAvanceRequestDto(0, "Descripción válida de una sesión"),
            RegistroAvanceRequestDto(-30, "Descripción válida de una sesión"),
            RegistroAvanceRequestDto(30, "Corta"),
            RegistroAvanceRequestDto(30, "x".repeat(1001))
        ).forEach { assertTrue(repository.registrar(7, 21, it).isFailure) }
        assertTrue(peticiones.isEmpty())
    }

    @Test
    fun conservaMotivoDelServidorYNoReintentaPost() = runBlocking {
        respuestas["POST $base/tareas/21/avances"] = 403 to """{"detail":"Solo el responsable o el creador pueden registrar avances"}"""
        val error = repository.registrar(7, 21, RegistroAvanceRequestDto(30, "Descripción válida de una sesión")).exceptionOrNull()
        assertTrue(error!!.message!!.contains("responsable"))
        assertFalse(error is RegistroAvanceInciertoException)
        assertEquals(1, peticiones.size)
    }

    @Test
    fun errorDeServidorTrasPostEsInciertoYNoSeReenvia() = runBlocking {
        respuestas["POST $base/tareas/21/avances"] = 500 to "{}"
        val error = repository.registrar(7, 21, RegistroAvanceRequestDto(30, "Descripción válida de una sesión")).exceptionOrNull()
        assertTrue(error is RegistroAvanceInciertoException)
        assertEquals(1, peticiones.size)
    }

    @Test
    fun respuestaExitosaInconsistenteNoPermiteAsumirQueNoSeGuardo() = runBlocking {
        respuestas["POST $base/tareas/21/avances"] = 201 to "{}"
        assertTrue(repository.registrar(7, 21, RegistroAvanceRequestDto(90, "Descripción válida de una sesión")).exceptionOrNull() is RegistroAvanceInciertoException)
        assertEquals(1, peticiones.size)
    }

    @Test
    fun respuestaJsonInvalidaTrasPostEsIncierta() = runBlocking {
        respuestas["POST $base/tareas/21/avances"] = 201 to "no-es-json"
        assertTrue(repository.registrar(7, 21, RegistroAvanceRequestDto(90, "Descripción válida de una sesión")).exceptionOrNull() is RegistroAvanceInciertoException)
        assertEquals(1, peticiones.size)
    }

    @Test
    fun planificacionActivaHabilitaAvancesYDeshabilitaAjustes() = runBlocking {
        val datos = repository.cargar(7, 21).getOrThrow()
        val estado = MisTareasUiState.Success(
            com.prieto.colab_movil.data.repository.MisTareasDatos(datos.proyecto, datos.perfil, datos.planificacion, listOf(datos.tarea), null)
        )
        assertTrue(estado.confirmadas)
        assertTrue(estado.puedeRegistrarAvance)
        assertFalse(estado.puedeConfirmar)
        assertFalse(estado.puedeSolicitarCambios)
        assertFalse(estado.copy(datosDesactualizados = true).puedeRegistrarAvance)
        assertFalse(estado.copy(isRefreshing = true).puedeRegistrarAvance)
        assertFalse(estado.copy(isSaving = true).puedeRegistrarAvance)
    }

    @Test
    fun actualizaEstadoConContratoRealSinRegistrarHoras() = runBlocking {
        assertEquals("COMPLETADA", repository.cambiarEstado(7, 21, "COMPLETADA").getOrThrow().estado)
        val (ruta, body) = peticiones.single()
        assertEquals("PUT $base/tareas/21/estado", ruta)
        assertEquals("COMPLETADA", JsonParser.parseString(body).asJsonObject["nuevoEstado"].asString)
    }

    @Test
    fun conflictoDeTransicionConservaMensaje() = runBlocking {
        respuestas["PUT $base/tareas/21/estado"] = 409 to """{"detail":"Transición no permitida"}"""
        assertEquals("Transición no permitida", repository.cambiarEstado(7, 21, "PENDIENTE").exceptionOrNull()!!.message)
    }

    @Test
    fun soloPermiteTransicionesSoportadasPorElBackend() {
        assertEquals(listOf(EstadoTarea.EN_PROGRESO), EstadoTarea.transiciones("PENDIENTE"))
        assertEquals(listOf(EstadoTarea.PENDIENTE, EstadoTarea.COMPLETADA), EstadoTarea.transiciones("EN_PROGRESO"))
        listOf("BLOQUEADA", "COMPLETADA", "DESCONOCIDA", null).forEach { assertTrue(EstadoTarea.transiciones(it).isEmpty()) }
    }

    @Test
    fun accionesBloqueadasFueraDeActivaDuranteCargaOConDatosViejos() = runBlocking {
        val datos = repository.cargar(7, 21).getOrThrow()
        val estado = RegistrarAvanceUiState.Success(datos)
        assertTrue(estado.puedeRegistrar)
        listOf(
            estado.copy(ocupado = true), estado.copy(desactualizado = true),
            estado.copy(datos = datos.copy(planificacion = datos.planificacion.copy(estado = "PROPUESTA")))
        ).forEach { assertFalse(it.puedeRegistrar); assertFalse(it.puedeGestionar) }
        assertFalse(estado.copy(registroIncierto = true).puedeRegistrar)
        assertTrue(estado.copy(registroIncierto = true).puedeGestionar)
    }
}
