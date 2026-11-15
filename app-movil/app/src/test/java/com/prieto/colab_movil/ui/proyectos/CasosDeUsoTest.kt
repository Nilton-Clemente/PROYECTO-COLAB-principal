package com.prieto.colab_movil.ui.proyectos

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.prieto.colab_movil.data.model.CasoUsoDto
import com.prieto.colab_movil.data.model.ProyectoRequest
import com.prieto.colab_movil.data.model.ProyectoResponseDto
import com.prieto.colab_movil.data.remote.PerfilApiService
import com.prieto.colab_movil.data.remote.ProyectoApiService
import com.prieto.colab_movil.data.repository.ProyectoRepositoryImpl
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

class CasosDeUsoTest {
    private lateinit var server: HttpServer
    private lateinit var repository: ProyectoRepositoryImpl
    private val peticiones = Collections.synchronizedList(mutableListOf<Pair<String, String>>())
    private val caso = CasoUsoDto(
        nombre = "Registrar avance", descripcion = "Registrar el trabajo realizado",
        actores = "Estudiante", precondiciones = "Sesión iniciada", disparador = "Seleccionar tarea",
        flujoPrincipal = "1. Ingresar minutos\n2. Confirmar", flujosAlternativos = "Cancelar",
        excepciones = "Error de conexión", postcondiciones = "Avance guardado",
        reglasDeNegocio = "Minutos mayores que cero"
    )

    @Before
    fun preparar() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val body = exchange.requestBody.bufferedReader().use { it.readText() }
            peticiones.add("${exchange.requestMethod} ${exchange.requestURI.path}" to body)
            val response = if (exchange.requestMethod == "GET") {
                Gson().toJsonTree(request(listOf(caso))).asJsonObject
            } else {
                JsonParser.parseString(body).asJsonObject
            }
            response.addProperty("id", 7)
            val bytes = response.toString().toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        val retrofit = Retrofit.Builder().baseUrl("http://127.0.0.1:${server.address.port}/")
            .addConverterFactory(GsonConverterFactory.create()).build()
        repository = ProyectoRepositoryImpl(
            retrofit.create(ProyectoApiService::class.java),
            retrofit.create(PerfilApiService::class.java)
        )
    }

    @After
    fun cerrar() { server.stop(0) }

    @Test
    fun crearEnviaLosDiezCamposAlBackend() = runBlocking {
        val resultado = repository.crearProyecto(request(listOf(caso))).getOrThrow()
        assertEquals(listOf(caso), resultado.casosDeUso)
        assertEquals("POST /api/proyectos", peticiones.single().first)
        val json = JsonParser.parseString(peticiones.single().second).asJsonObject
            .getAsJsonArray("casosDeUso").single().asJsonObject
        assertEquals(10, json.size())
        assertFalse(json.has("codigo"))
        assertEquals(Gson().toJsonTree(caso), json)
    }

    @Test
    fun editarConservaLosCamposRecibidosEnElDetalle() = runBlocking {
        val proyecto = repository.verProyecto(7).getOrThrow()
        val resultado = repository.editarProyecto(7, request(proyecto.casosDeUso.orEmpty())).getOrThrow()
        assertEquals(listOf(caso), resultado.casosDeUso)
        assertEquals("PUT /api/proyectos/7", peticiones.last().first)
        assertEquals(Gson().toJsonTree(listOf(caso)),
            JsonParser.parseString(peticiones.last().second).asJsonObject["casosDeUso"])
    }

    @Test
    fun eliminarTodosEnviaListaVaciaEnLugarDeNull() = runBlocking {
        repository.editarProyecto(7, request(emptyList())).getOrThrow()
        val json = JsonParser.parseString(peticiones.single().second).asJsonObject
        assertEquals(0, json.getAsJsonArray("casosDeUso").size())
    }

    @Test
    fun normalizaTodosLosCamposSinPerderSaltosDeLinea() {
        val conEspacios = CasoUsoDto(
            " Nombre ", " Descripción ", " Actor ", " Precondición ", " Disparador ",
            " 1. Abrir\n2. Guardar ", " Alternativa ", " Excepción ", " Postcondición ", " Regla "
        ).normalizado()
        assertEquals(CasoUsoDto(
            "Nombre", "Descripción", "Actor", "Precondición", "Disparador",
            "1. Abrir\n2. Guardar", "Alternativa", "Excepción", "Postcondición", "Regla"
        ), conEspacios)
    }

    @Test
    fun camposOpcionalesVaciosSeNormalizanANull() {
        assertEquals(CasoUsoDto("Nombre"), CasoUsoDto(
            " Nombre ", " ", "\n", " ", " ", " ", " ", " ", " ", " "
        ).normalizado())
    }

    @Test
    fun admiteProyectosAntiguosSinCasosDeUso() {
        val proyecto = Gson().fromJson("""{"id":7,"nombre":"Proyecto antiguo"}""", ProyectoResponseDto::class.java)
        assertTrue(proyecto.casosDeUso.orEmpty().isEmpty())
    }

    private fun request(casos: List<CasoUsoDto>) = ProyectoRequest(
        nombre = "Proyecto", descripcion = null, problematica = null, objetivoGeneral = null,
        objetivosEspecificos = null, alcance = null, restricciones = null, fechaEntrega = null,
        tecnologiasIds = emptyList(), requerimientosFuncionales = emptyList(),
        requerimientosNoFuncionales = emptyList(), casosDeUso = casos
    )
}