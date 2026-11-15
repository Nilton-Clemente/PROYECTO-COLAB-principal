package com.prieto.colab_movil.ui.planificacion

import com.prieto.colab_movil.data.remote.PlanificacionApiService
import com.prieto.colab_movil.data.remote.clienteGeneracionPlanificacion
import com.prieto.colab_movil.data.repository.GeneracionPlanificacionInciertaException
import com.prieto.colab_movil.data.repository.PlanificacionHttpException
import com.prieto.colab_movil.data.repository.PlanificacionRepositoryImpl
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetSocketAddress
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PlanificacionGeneracionTest {
    private lateinit var server: HttpServer
    private val executor = Executors.newCachedThreadPool()
    private val peticiones = Collections.synchronizedList(mutableListOf<String>())
    private val plan = """{"id":3,"proyectoId":7,"estado":"PROPUESTA","etapas":[]}"""
    @Volatile private var codigoPost = 201
    @Volatile private var cuerpoPost = plan
    @Volatile private var demoraPost = 0L
    @Volatile private var guardado = false
    @Volatile private var codigoGet: Int? = null

    @Before
    fun preparar() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.executor = executor
        server.createContext("/") { exchange ->
            peticiones.add(exchange.requestMethod)
            val post = exchange.requestMethod == "POST"
            if (post) {
                Thread.sleep(demoraPost)
                if (codigoPost == 201) guardado = true
            }
            val codigo = if (post) codigoPost else codigoGet ?: if (guardado) 200 else 404
            val texto = if (post) cuerpoPost else if (guardado) plan else "{}"
            try {
                val bytes = texto.toByteArray(Charsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(codigo, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            } catch (_: java.io.IOException) {
                // La persistencia no se revierte cuando el cliente pierde la conexión.
            } finally { exchange.close() }
        }
        server.start()
    }

    @After
    fun cerrar() { server.stop(0); executor.shutdownNow() }

    private fun api(client: OkHttpClient): PlanificacionApiService = Retrofit.Builder()
        .baseUrl("http://127.0.0.1:${server.address.port}/")
        .client(client).addConverterFactory(GsonConverterFactory.create()).build()
        .create(PlanificacionApiService::class.java)

    private fun repository(timeoutMs: Long = 2_000): PlanificacionRepositoryImpl {
        val normal = api(OkHttpClient())
        val generacion = api(clienteGeneracionPlanificacion(OkHttpClient()).newBuilder()
            .readTimeout(timeoutMs, TimeUnit.MILLISECONDS).build())
        return PlanificacionRepositoryImpl(normal, generacion)
    }

    @Test
    fun soloGeneracionAmpliaLaEsperaYDesactivaReintentosDePost() {
        val normal = OkHttpClient.Builder().readTimeout(15, TimeUnit.SECONDS).build()
        val ia = clienteGeneracionPlanificacion(normal)
        assertEquals(15_000, normal.readTimeoutMillis)
        assertEquals(180_000, ia.readTimeoutMillis)
        assertEquals(210_000, ia.callTimeoutMillis)
        assertFalse(ia.retryOnConnectionFailure)
        assertEquals(normal.connectTimeoutMillis, ia.connectTimeoutMillis)
    }

    @Test
    fun respuestaQueTardaMasDeQuinceSegundosSeRecibeSinFalsoError() = runBlocking {
        demoraPost = 15_200
        val normal = api(OkHttpClient.Builder().readTimeout(15, TimeUnit.SECONDS).build())
        val ia = api(clienteGeneracionPlanificacion(OkHttpClient()))
        val resultado = PlanificacionRepositoryImpl(normal, ia).generarPlanificacion(7).getOrThrow()
        assertEquals(3L, resultado.id)
        assertEquals(listOf("POST"), peticiones.toList())
    }

    @Test
    fun timeoutNoSeConfundeConFalloDefinitivoYSePuedeConsultarLoPersistido() = runBlocking {
        demoraPost = 300
        val repo = repository(timeoutMs = 60)
        val error = repo.generarPlanificacion(7).exceptionOrNull()
        assertTrue(error is GeneracionPlanificacionInciertaException)
        delay(400)
        assertEquals(3L, repo.verPlanificacion(7).getOrThrow()!!.id)
        assertEquals(1, peticiones.count { it == "POST" })
    }

    @Test
    fun conflictoConsultaExistenteEnLugarDeTratarloComoNuevaGeneracion() = runBlocking {
        codigoPost = 409
        cuerpoPost = """{"detail":"La planificación ya fue generada"}"""
        val error = repository().generarPlanificacion(7).exceptionOrNull()
        assertTrue(error is GeneracionPlanificacionInciertaException)
        assertEquals(409, (error!!.cause as PlanificacionHttpException).codigo)
    }

    @Test
    fun erroresDeValidacionPermisosEIAConservanElMotivoReal() = runBlocking {
        for (codigo in listOf(400, 401, 403, 502)) {
            codigoPost = codigo
            cuerpoPost = """{"detail":"Motivo real $codigo"}"""
            val error = repository().generarPlanificacion(7).exceptionOrNull()
            assertTrue(error is PlanificacionHttpException)
            assertEquals(codigo, (error as PlanificacionHttpException).codigo)
            assertEquals("Motivo real $codigo", error.message)
        }
    }

    @Test
    fun erroresTransitoriosDeGatewaySeTratanComoResultadoIncierto() = runBlocking {
        for (codigo in listOf(408, 500, 503, 504)) {
            codigoPost = codigo
            cuerpoPost = "{}"
            assertTrue(repository().generarPlanificacion(7).exceptionOrNull() is GeneracionPlanificacionInciertaException)
        }
    }

    @Test
    fun jsonInvalidoTrasGuardarNoProvocaOtroPost() = runBlocking {
        cuerpoPost = "respuesta cortada"
        val repo = repository()
        assertTrue(repo.generarPlanificacion(7).exceptionOrNull() is GeneracionPlanificacionInciertaException)
        assertNotNull(repo.verPlanificacion(7).getOrThrow())
        assertEquals(1, peticiones.count { it == "POST" })
    }

    @Test
    fun cancelacionDeCoroutineNoSeConvierteEnErrorDeConexion() = runBlocking {
        demoraPost = 500
        val trabajo = async { repository().generarPlanificacion(7) }
        delay(50)
        trabajo.cancel()
        try {
            trabajo.await()
            fail("Debe propagar la cancelación")
        } catch (_: CancellationException) { }
    }

    @Test
    fun consulta404EsAusenciaY403EsErrorTipado() = runBlocking {
        assertNull(repository().verPlanificacion(7).getOrThrow())
        codigoGet = 403
        val error = repository().verPlanificacion(7).exceptionOrNull()
        assertTrue(error is PlanificacionHttpException)
        assertEquals(403, (error as PlanificacionHttpException).codigo)
        assertTrue(peticiones.all { it == "GET" })
    }
}