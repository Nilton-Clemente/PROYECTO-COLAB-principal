package com.prieto.colab_movil.ui.notificaciones

import com.prieto.colab_movil.data.model.NotificacionResponseDto
import com.prieto.colab_movil.data.remote.NotificacionesApiService
import com.prieto.colab_movil.data.repository.NotificacionesRepositoryImpl
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetSocketAddress
import java.util.Collections

class NotificacionesRepositoryTest {
    private lateinit var server: HttpServer
    private lateinit var repository: NotificacionesRepositoryImpl
    private val peticiones = Collections.synchronizedList(mutableListOf<String>())
    @Volatile private var codigo = 200
    @Volatile private var cuerpo = "[]"
    private val aviso = """{"id":7,"tipoEvento":"PLANIFICACION","mensaje":"Ana solicitó cambios en Matrícula: TK-21 · revisar cupos","leida":false,"fechaCreacion":"2026-11-25T10:30:00"}"""

    @Before
    fun preparar() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            exchange.requestBody.close()
            peticiones.add("${exchange.requestMethod} ${exchange.requestURI.path}")
            val bytes = cuerpo.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
            exchange.sendResponseHeaders(codigo, if (codigo == 204) -1 else bytes.size.toLong())
            exchange.responseBody.use { if (codigo != 204) it.write(bytes) }
        }
        server.start()
        val api = Retrofit.Builder().baseUrl("http://127.0.0.1:${server.address.port}/")
            .addConverterFactory(GsonConverterFactory.create()).build().create(NotificacionesApiService::class.java)
        repository = NotificacionesRepositoryImpl(api)
    }

    @After
    fun cerrar() { server.stop(0) }

    @Test
    fun listaAvisosConMensajeCompletoFechaYEstadoReal() = runBlocking {
        cuerpo = "[$aviso]"
        val resultado = repository.listar().getOrThrow().single()
        assertEquals("GET /api/notificaciones", peticiones.single())
        assertEquals("Ana solicitó cambios en Matrícula: TK-21 · revisar cupos", resultado.mensaje)
        assertEquals("2026-11-25T10:30:00", resultado.fechaCreacion)
        assertFalse(resultado.leida)
    }

    @Test
    fun marcarLeidaUsaPutConIdYDevuelveLaConfirmacionDelServidor() = runBlocking {
        cuerpo = aviso.replace("false", "true")
        val resultado = repository.marcarLeida(7).getOrThrow()
        assertEquals("PUT /api/notificaciones/7/leida", peticiones.single())
        assertTrue(resultado.leida)
        assertEquals(7L, resultado.id)
    }

    @Test
    fun eliminarLeidasUsaDeleteYAcepta204SinCuerpo() = runBlocking {
        codigo = 204
        assertEquals(Unit, repository.eliminarLeidas().getOrThrow())
        assertEquals("DELETE /api/notificaciones/leidas", peticiones.single())
    }

    @Test
    fun eliminarLeidasNoSimulaExitoAnteErroresDelServidor() = runBlocking {
        for (estado in listOf(401, 403, 404, 500)) {
            codigo = estado
            assertTrue(repository.eliminarLeidas().isFailure)
        }
        assertEquals(4, peticiones.size)
        assertTrue(peticiones.all { it == "DELETE /api/notificaciones/leidas" })
    }

    @Test
    fun endpointDeBorradoAusenteNoSeConfundeConNotificacionInexistente() = runBlocking {
        for (estado in listOf(404, 405)) {
            codigo = estado
            val mensaje = repository.eliminarLeidas().exceptionOrNull()!!.message!!
            assertTrue(mensaje.contains("Actualiza o reinicia el backend"))
            assertFalse(mensaje.contains("La notificación ya no está disponible"))
        }
    }

    @Test
    fun cancelacionDelBorradoSePropaga() = runBlocking {
        val api = object : NotificacionesApiService {
            override suspend fun listar(): Response<List<NotificacionResponseDto>> = error("No se usa")
            override suspend fun marcarLeida(id: Long): Response<NotificacionResponseDto> = error("No se usa")
            override suspend fun eliminarLeidas(): Response<Unit> = throw CancellationException("Cancelado")
        }
        try {
            NotificacionesRepositoryImpl(api).eliminarLeidas()
            fail("La cancelación debe propagarse")
        } catch (_: CancellationException) {
            // Comportamiento esperado al abandonar la pantalla.
        }
    }

    @Test
    fun conservaProyectoEnLaConsultaYAlMarcarLeida() = runBlocking {
        val conProyecto = aviso.dropLast(1) + ""","proyectoId":7,"proyectoNombre":"Matrícula","proyectoCodigo":"TEC-7"}"""
        cuerpo = "[$conProyecto]"
        val resultado = repository.listar().getOrThrow().single()
        assertEquals(7L, resultado.proyectoId)
        assertEquals("Matrícula", resultado.proyectoNombre)
        assertEquals("TEC-7", resultado.proyectoCodigo)
        cuerpo = conProyecto.replace("false", "true")
        val leida = repository.marcarLeida(7).getOrThrow()
        assertEquals(resultado.copy(leida = true), leida)
    }

    @Test
    fun errorDeSesionNoSeConvierteEnListaVacia() = runBlocking {
        codigo = 401
        val resultado = repository.listar()
        assertTrue(resultado.isFailure)
        assertTrue(resultado.exceptionOrNull()!!.message!!.contains("sesión expiró"))
    }

    @Test
    fun permisoDenegadoNoSimulaLectura() = runBlocking {
        codigo = 403
        assertTrue(repository.marcarLeida(7).exceptionOrNull()!!.message!!.contains("permiso"))
    }

    @Test
    fun respuestaVaciaNoSimulaBandejaVacia() = runBlocking {
        cuerpo = ""
        assertTrue(repository.listar().isFailure)
    }

    @Test
    fun validaQueLaLecturaPertenezcaAlIdYEsteConfirmada() = runBlocking {
        cuerpo = aviso
        assertTrue(repository.marcarLeida(7).isFailure)
        cuerpo = aviso.replace("false", "true").replace("\"id\":7", "\"id\":99")
        assertTrue(repository.marcarLeida(7).isFailure)
    }

    @Test
    fun conservaCompatibilidadConNotificacionesAntiguasSinTipoNiFecha() = runBlocking {
        cuerpo = """[{"id":1,"mensaje":"Mensaje antiguo","leida":true}]"""
        val resultado = repository.listar().getOrThrow().single()
        assertNull(resultado.tipoEvento)
        assertNull(resultado.fechaCreacion)
        assertNull(resultado.proyectoId)
        assertNull(resultado.proyectoNombre)
        assertNull(resultado.proyectoCodigo)
        assertEquals("Mensaje antiguo", resultado.mensaje)
    }

    @Test
    fun cancelacionSePropagaSinConvertirseEnErrorDeConexion() = runBlocking {
        val api = object : NotificacionesApiService {
            override suspend fun listar(): Response<List<NotificacionResponseDto>> = throw CancellationException("Cancelado")
            override suspend fun marcarLeida(id: Long): Response<NotificacionResponseDto> = error("No se usa")
            override suspend fun eliminarLeidas(): Response<Unit> = error("No se usa")
        }
        try {
            NotificacionesRepositoryImpl(api).listar()
            fail("La cancelación debe propagarse")
        } catch (_: CancellationException) {
            // Comportamiento esperado al abandonar la pantalla.
        }
    }
}