package com.prieto.colab_movil.ui.proyectos

import com.google.gson.JsonParser
import com.prieto.colab_movil.data.remote.PerfilApiService
import com.prieto.colab_movil.data.remote.ProyectoApiService
import com.prieto.colab_movil.data.repository.ProyectoRepositoryImpl
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetSocketAddress
import java.util.Collections

class InvitarIntegranteTest {
    private lateinit var servidor: HttpServer
    private lateinit var repositorio: ProyectoRepositoryImpl
    private val peticiones = Collections.synchronizedList(mutableListOf<Pair<String, String>>())
    @Volatile private var codigo = 201

    @Before
    fun preparar() {
        servidor = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        servidor.createContext("/") { intercambio ->
            val cuerpo = intercambio.requestBody.bufferedReader().use { it.readText() }
            peticiones.add("${intercambio.requestMethod} ${intercambio.requestURI.path}" to cuerpo)
            intercambio.sendResponseHeaders(codigo, -1)
            intercambio.close()
        }
        servidor.start()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://127.0.0.1:${servidor.address.port}/")
            .addConverterFactory(GsonConverterFactory.create()).build()
        val api = retrofit.create(ProyectoApiService::class.java)
        repositorio = ProyectoRepositoryImpl(api, retrofit.create(PerfilApiService::class.java), api)
    }

    @After
    fun cerrar() { servidor.stop(0) }

    @Test
    fun enviaPostAlProyectoConElCorreoSinEspacios() = runBlocking {
        repositorio.invitarIntegrante(7, " b@tecsup.edu.pe ").getOrThrow()
        assertEquals("POST /api/proyectos/7/invitaciones", peticiones.single().first)
        val cuerpo = JsonParser.parseString(peticiones.single().second).asJsonObject
        assertEquals(setOf("email"), cuerpo.keySet())
        assertEquals("b@tecsup.edu.pe", cuerpo["email"].asString)
    }

    @Test
    fun conservaLosErroresDelServidorSinReportarExito() = runBlocking {
        for (estado in listOf(400, 401, 403, 404, 409, 500)) {
            codigo = estado
            val resultado = repositorio.invitarIntegrante(7, "b@tecsup.edu.pe")
            assertTrue(resultado.isFailure)
            assertEquals(estado, (resultado.exceptionOrNull() as HttpException).code())
        }
        assertEquals(6, peticiones.size)
    }
}