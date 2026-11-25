package com.prieto.colab_movil.ui.metricas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import com.prieto.colab_movil.data.model.IntegranteResponseDto
import com.prieto.colab_movil.data.model.MetricasIntegranteDto
import com.prieto.colab_movil.data.model.MetricasProyectoDto
import com.prieto.colab_movil.data.model.ProyectoResponseDto
import com.prieto.colab_movil.data.repository.MetricasProyectoDatos
import com.prieto.colab_movil.data.repository.MetricasProyectoRepository
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MetricasProyectoScreenTest {
    @get:Rule val compose = createComposeRule()

    private val proyecto = Gson().fromJson(
        """{"id":7,"nombre":"Proyecto Colab","estado":"EN_DESARROLLO","fechaEntrega":"2020-01-01"}""",
        ProyectoResponseDto::class.java
    )
    private val datos = MetricasProyectoDatos(
        proyecto,
        MetricasProyectoDto(4, 1, 1, 1, 1, 25.0, listOf(
            MetricasIntegranteDto(1, "ana@example.test", 2, 1),
            MetricasIntegranteDto(2, "luis@example.test", 0, 0)
        )),
        listOf(IntegranteResponseDto(1, "ana@example.test", "Ana Torres", "CREADOR", "ACTIVO"))
    )

    private class Repo(var respuesta: Result<MetricasProyectoDatos>) : MetricasProyectoRepository {
        var consultas = 0
        override suspend fun cargar(proyectoId: Long): Result<MetricasProyectoDatos> {
            assertEquals(7L, proyectoId)
            consultas++
            return respuesta
        }
    }

    private fun mostrar(repo: Repo, onBack: () -> Unit = {}): MetricasProyectoViewModel {
        val vm = MetricasProyectoViewModel(repo)
        compose.setContent { Box(Modifier.width(320.dp)) { MetricasProyectoScreen(7, onBack, vm) } }
        return vm
    }

    private fun llegarATexto(texto: String) {
        compose.onNodeWithTag("metricas-contenido").performScrollToNode(hasText(texto))
        compose.onNodeWithText(texto).assertIsDisplayed()
    }

    private fun actualizarConGesto() {
        val contenido = compose.onNodeWithTag("metricas-contenido")
        contenido.performScrollToIndex(0)
        contenido.performTouchInput { swipeDown() }
    }

    @Test fun muestraLasMetricasYLosResponsablesEnPantallaEstrecha() {
        mostrar(Repo(Result.success(datos)))
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Proyecto Colab").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Actualizar métricas").assertDoesNotExist()
        llegarATexto("Fecha de entrega vencida")
        llegarATexto("1 de 4 tareas completadas")
        llegarATexto("Estado de las tareas")
        llegarATexto("Bloqueadas")
        llegarATexto("Ana Torres")
        llegarATexto("1 de 2 tareas completadas")
        llegarATexto("luis@example.test")
        llegarATexto("Sin tareas asignadas")
        compose.onNodeWithText("Sprint 3").assertDoesNotExist()
    }

    @Test fun sinTareasOfreceVolverSinMostrarGraficos() {
        val vacio = datos.copy(proyecto = proyecto.copy(estado = "SIN_PLANIFICACION"),
            metricas = MetricasProyectoDto(0, 0, 0, 0, 0, 0.0, emptyList()))
        var regreso = 0
        mostrar(Repo(Result.success(vacio))) { regreso++ }
        llegarATexto("Aún no hay tareas para medir")
        compose.onNodeWithText("Progreso general").assertDoesNotExist()
        llegarATexto("Volver al detalle del proyecto")
        compose.onNodeWithText("Volver al detalle del proyecto").performClick()
        compose.runOnIdle { assertEquals(1, regreso) }
    }

    @Test fun actualizaDatosYConservaElUltimoResultadoSiFallaLaRed() {
        val repo = Repo(Result.failure(IOException("Sin conexión")))
        val vm = mostrar(repo)
        compose.waitUntil(5_000) { vm.uiState.value is MetricasProyectoUiState.Error }
        compose.runOnIdle { repo.respuesta = Result.success(datos) }
        compose.onNodeWithText("Reintentar").performClick()
        compose.waitUntil(5_000) { vm.uiState.value is MetricasProyectoUiState.Listo }
        compose.runOnIdle { repo.respuesta = Result.success(datos.copy(proyecto = proyecto.copy(estado = "CANCELADO"))) }
        actualizarConGesto()
        llegarATexto("Cancelado")
        compose.onNodeWithText("Fecha de entrega vencida").assertDoesNotExist()
        compose.runOnIdle { repo.respuesta = Result.failure(IOException("Sin conexión")) }
        actualizarConGesto()
        compose.waitUntil(5_000) { (vm.uiState.value as? MetricasProyectoUiState.Listo)?.aviso != null }
        llegarATexto("Cancelado")
        compose.onNodeWithText("Se muestran los últimos datos disponibles.", substring = true).assertExists()
        compose.runOnIdle { assertEquals(4, repo.consultas) }
    }

    @Test fun dejaDeMostrarMetricasSiSeRevocaElAcceso() {
        val repo = Repo(Result.success(datos))
        val vm = mostrar(repo)
        compose.waitUntil(5_000) { vm.uiState.value is MetricasProyectoUiState.Listo }
        compose.runOnIdle { repo.respuesta = Result.failure(HttpException(Response.error<Any>(403, "".toResponseBody()))) }
        actualizarConGesto()
        compose.waitUntil(5_000) { vm.uiState.value is MetricasProyectoUiState.Error }
        compose.onNodeWithText("Solo los integrantes activos pueden consultar las métricas del proyecto.").assertIsDisplayed()
        compose.onNodeWithText("Proyecto Colab").assertDoesNotExist()
    }
}