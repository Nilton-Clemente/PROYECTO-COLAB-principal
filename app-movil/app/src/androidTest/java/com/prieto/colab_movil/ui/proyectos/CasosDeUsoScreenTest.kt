package com.prieto.colab_movil.ui.proyectos

import android.app.Activity
import android.app.Instrumentation
import android.content.ContentValues
import android.content.Intent
import android.content.IntentFilter
import android.provider.MediaStore
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.prieto.colab_movil.data.model.*
import com.prieto.colab_movil.data.repository.ProyectoRepository
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CasosDeUsoScreenTest {
    @get:Rule val compose = createComposeRule()

    private val casoCompleto = CasoUsoDto(
        "Registrar avance", "Descripción", "Estudiante", "Sesión iniciada", "Abrir tarea",
        "1. Escribir\n2. Guardar", "Cancelar", "Sin conexión", "Avance registrado", "Minutos positivos"
    )

    private fun mostrar(repo: RepositorioPrueba, editar: Boolean = false, inicial: ProyectoResponseDto? = repo.proyecto) {
        val vm = CrearProyectoViewModel(repo)
        compose.setContent {
            CrearProyectoScreen(
                proyectoId = if (editar) 7 else null,
                proyectoInicial = inicial,
                viewModel = vm,
                onGuardar = {}, onCancelar = {}
            )
        }
        compose.waitForIdle()
    }

    private fun abrirCasos() {
        compose.onNodeWithText("Casos de Uso").performScrollTo().performClick()
    }

    private fun guardar(editar: Boolean = false) {
        Espresso.closeSoftKeyboard()
        compose.onNodeWithText(if (editar) "Guardar Cambios" else "Guardar Proyecto").performClick()
        compose.onNodeWithText("Confirmar").performClick()
        compose.waitForIdle()
    }

    @Test
    fun creacionEnviaTodosLosCamposIntroducidos() {
        val repo = RepositorioPrueba()
        mostrar(repo)
        abrirCasos()
        compose.onNodeWithText("+ Añadir caso de uso").performScrollTo().performClick()
        val campos = linkedMapOf(
            "nombre" to " Registrar avance ", "descripcion" to "Descripción", "actores" to "Estudiante",
            "precondiciones" to "Sesión iniciada", "disparador" to "Abrir tarea",
            "flujoPrincipal" to "1. Escribir\n2. Guardar", "flujosAlternativos" to "Cancelar",
            "excepciones" to "Sin conexión", "postcondiciones" to "Avance registrado",
            "reglasDeNegocio" to "Minutos positivos"
        )
        campos.forEach { (campo, valor) ->
            compose.onNodeWithTag("caso-0-$campo").performScrollTo().performTextReplacement(valor)
        }
        guardar()
        compose.runOnIdle {
            assertEquals(1, repo.creaciones)
            assertEquals(listOf(casoCompleto), repo.ultimoRequest!!.casosDeUso)
        }
    }

    @Test
    fun edicionSinAbrirSeccionConservaTodosLosCampos() {
        val repo = RepositorioPrueba(listOf(casoCompleto))
        mostrar(repo, editar = true)
        guardar(editar = true)
        compose.runOnIdle {
            assertEquals(1, repo.ediciones)
            assertEquals(listOf(casoCompleto), repo.ultimoRequest!!.casosDeUso)
        }
    }

    @Test
    fun edicionPrecargaPermiteModificarYEliminarOtroCaso() {
        val repo = RepositorioPrueba(listOf(casoCompleto, CasoUsoDto("Descartar")))
        mostrar(repo, editar = true)
        abrirCasos()
        compose.onNodeWithTag("caso-0-nombre").performScrollTo().assertTextContains(casoCompleto.nombre)
        compose.onNodeWithTag("caso-0-nombre").performTextReplacement("Nombre actualizado")
        compose.onNodeWithText("Eliminar caso 2").performScrollTo().performClick()
        guardar(editar = true)
        compose.runOnIdle {
            assertEquals(listOf(casoCompleto.copy(nombre = "Nombre actualizado")), repo.ultimoRequest!!.casosDeUso)
        }
    }

    @Test
    fun eliminarUltimoCasoEnviaListaVacia() {
        val repo = RepositorioPrueba(listOf(casoCompleto))
        mostrar(repo, editar = true)
        abrirCasos()
        compose.onNodeWithText("Eliminar caso 1").performScrollTo().performClick()
        guardar(editar = true)
        compose.runOnIdle { assertEquals(emptyList<CasoUsoDto>(), repo.ultimoRequest!!.casosDeUso) }
    }

    @Test
    fun casoSinNombreBloqueaPeticionSinDescartarElBorrador() {
        val repo = RepositorioPrueba()
        mostrar(repo)
        abrirCasos()
        compose.onNodeWithText("+ Añadir caso de uso").performScrollTo().performClick()
        compose.onNodeWithTag("caso-0-descripcion").performScrollTo().performTextReplacement("Borrador")
        guardar()
        compose.onNodeWithText("El nombre de cada caso de uso es obligatorio. Completa o elimina los casos vacíos.")
            .assertIsDisplayed()
        compose.onNodeWithText("Aceptar").performClick()
        compose.onNodeWithTag("caso-0-descripcion").performScrollTo().assertTextContains("Borrador")
        compose.runOnIdle { assertEquals(0, repo.creaciones); assertNull(repo.ultimoRequest) }
    }

    @Test
    fun proyectoNuevoSinDatosInicialesPuedeGuardarseSinCasos() {
        val repo = RepositorioPrueba()
        mostrar(repo, inicial = null)
        compose.onNodeWithText("Nombre del proyecto").performScrollTo().performTextInput("Proyecto nuevo")
        guardar()
        compose.runOnIdle {
            assertEquals("Proyecto nuevo", repo.ultimoRequest!!.nombre)
            assertEquals(emptyList<CasoUsoDto>(), repo.ultimoRequest!!.casosDeUso)
        }
    }

    private fun pegarJson(json: String) {
        compose.onNodeWithText("Importar JSON").performScrollTo().performClick()
        compose.onNodeWithTag("importar-json-contenido").performScrollTo().performTextReplacement(json)
        Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Validar JSON").performClick()
        compose.waitForIdle()
    }

    @Test
    fun importarJsonCompletaTodosLosDatosSinGuardarAutomaticamente() {
        val repo = RepositorioPrueba()
        mostrar(repo, inicial = null)
        val json = """{
            "nombre":"Importado", "descripcion":"Descripción importada", "problematica":"Problema",
            "objetivoGeneral":"Objetivo", "objetivosEspecificos":["Primero","Segundo"],
            "alcance":"Alcance", "restricciones":"Restricciones", "fechaEntrega":"2027-06-30",
            "tecnologias":["kotlin"],
            "requerimientosFuncionales":[{"codigo":"RF-01","descripcion":"Permitir: registrar"},"Consultar: tareas"],
            "requerimientosNoFuncionales":[{"descripcion":"Seguro","categoria":"Seguridad"}],
            "casosDeUso":[${Gson().toJson(casoCompleto)}]
        }"""
        pegarJson(json)
        compose.onNodeWithText("Confirmar importación").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, repo.creaciones) }
        compose.onNodeWithText("Reemplazar formulario").performClick()
        compose.onNodeWithText("Datos importados. Revisa el formulario y pulsa Guardar Proyecto para registrarlo.").assertExists()
        compose.runOnIdle { assertEquals(0, repo.creaciones) }
        compose.onNodeWithText("Importado").performScrollTo().performTextReplacement("Importado revisado")
        guardar()
        compose.runOnIdle {
            val datos = repo.ultimoRequest!!
            assertEquals("Importado revisado", datos.nombre)
            assertEquals("Descripción importada", datos.descripcion)
            assertEquals("Problema", datos.problematica)
            assertEquals("Objetivo", datos.objetivoGeneral)
            assertEquals("Primero\nSegundo", datos.objetivosEspecificos)
            assertEquals("Alcance", datos.alcance)
            assertEquals("Restricciones", datos.restricciones)
            assertEquals("2027-06-30", datos.fechaEntrega)
            assertEquals(listOf(1L), datos.tecnologiasIds)
            assertEquals(listOf(RequerimientoFuncionalDto("RF-01", "Permitir: registrar"),
                RequerimientoFuncionalDto(null, "Consultar: tareas")), datos.requerimientosFuncionales)
            assertEquals("Seguridad", datos.requerimientosNoFuncionales!!.single().categoria)
            assertEquals(listOf(casoCompleto), datos.casosDeUso)
        }
    }

    @Test
    fun jsonInvalidoConservaElFormularioAnterior() {
        val repo = RepositorioPrueba(listOf(casoCompleto))
        mostrar(repo)
        pegarJson("""{"nombre":"Otro","casosDeUso":[{"nombre":""}]}""")
        compose.onNodeWithText("casosDeUso[0].nombre es obligatorio y no puede estar vacío.").assertExists()
        compose.onNodeWithText("Cancelar importación").performClick()
        guardar()
        compose.runOnIdle {
            assertEquals("Proyecto", repo.ultimoRequest!!.nombre)
            assertEquals(listOf(casoCompleto), repo.ultimoRequest!!.casosDeUso)
        }
    }

    @Test
    fun cancelarLaConfirmacionNoReemplazaDatos() {
        val repo = RepositorioPrueba(listOf(casoCompleto))
        mostrar(repo)
        pegarJson("""{"nombre":"No aplicar"}""")
        compose.onNodeWithText("Confirmar importación").assertIsDisplayed()
        compose.onNodeWithText("Cancelar importación").performClick()
        guardar()
        compose.runOnIdle {
            assertEquals("Proyecto", repo.ultimoRequest!!.nombre)
            assertEquals(listOf(casoCompleto), repo.ultimoRequest!!.casosDeUso)
        }
    }

    @Test
    fun importarJsonMinimoLimpiaListasYCamposAnteriores() {
        val repo = RepositorioPrueba(listOf(casoCompleto))
        mostrar(repo)
        pegarJson("""{"nombre":"Solo nombre"}""")
        compose.onNodeWithText("Reemplazar formulario").performClick()
        guardar()
        compose.runOnIdle {
            assertEquals("Solo nombre", repo.ultimoRequest!!.nombre)
            assertTrue(repo.ultimoRequest!!.casosDeUso!!.isEmpty())
            assertTrue(repo.ultimoRequest!!.tecnologiasIds!!.isEmpty())
            assertNull(repo.ultimoRequest!!.descripcion)
        }
    }

    @Test
    fun tecnologiaDesconocidaBloqueaImportacion() {
        val repo = RepositorioPrueba()
        mostrar(repo)
        pegarJson("""{"nombre":"P","tecnologias":["Inexistente"]}""")
        compose.onNodeWithText("La tecnología 'Inexistente' no está en el catálogo. Revisa el nombre o recarga el catálogo.").assertExists()
        compose.onNodeWithText("Reemplazar formulario").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, repo.creaciones) }
    }

    @Test
    fun cargaEjemploDesdeArchivoIncluidoEnLaApp() {
        val repo = RepositorioPrueba()
        mostrar(repo)
        compose.onNodeWithText("Importar JSON").performScrollTo().performClick()
        compose.onNodeWithText("Cargar ejemplo JSON").performScrollTo().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasTestTag("importar-json-contenido") and hasText("Gestor de proyectos colaborativos", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Validar JSON").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Reemplazar formulario").performClick()
        guardar()
        compose.runOnIdle {
            assertEquals("Gestor de proyectos colaborativos", repo.ultimoRequest!!.nombre)
            assertEquals("Crear proyecto", repo.ultimoRequest!!.casosDeUso!!.single().nombre)
        }
    }

    @Test
    fun opcionImportarNoApareceAlEditar() {
        mostrar(RepositorioPrueba(), editar = true)
        compose.onNodeWithText("Importar JSON").assertDoesNotExist()
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun importaArchivoSeleccionadoConElLectorDeContenidoAndroid() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val resolver = instrumentation.targetContext.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "colab-import-test-${System.nanoTime()}.json")
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
        })!!
        val monitor = instrumentation.addMonitor(
            IntentFilter(Intent.ACTION_OPEN_DOCUMENT).apply {
                addDataType("*/*")
                addCategory(Intent.CATEGORY_OPENABLE)
            },
            Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(uri)), true
        )
        try {
            resolver.openOutputStream(uri)!!.use {
                it.write("""{"nombre":"Planificación desde archivo","casosDeUso":[{"nombre":"Importar archivo","actores":"Estudiante"}]}""".toByteArray(Charsets.UTF_8))
            }
            val repo = RepositorioPrueba()
            mostrar(repo)
            compose.onNodeWithText("Importar JSON").performScrollTo().performClick()
            compose.onNodeWithText("Seleccionar archivo JSON").performScrollTo().performClick()
            compose.waitUntil(5_000) {
                compose.onAllNodes(hasTestTag("importar-json-contenido") and hasText("Planificación desde archivo", substring = true))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Validar JSON").performClick()
            compose.waitForIdle()
            compose.onNodeWithText("Reemplazar formulario").performClick()
            guardar()
            compose.runOnIdle {
                assertEquals("Planificación desde archivo", repo.ultimoRequest!!.nombre)
                assertEquals("Estudiante", repo.ultimoRequest!!.casosDeUso!!.single().actores)
            }
        } finally {
            instrumentation.removeMonitor(monitor)
            resolver.delete(uri, null, null)
        }
    }

    @Test
    fun cancelarSelectorDeArchivoNoModificaElFormulario() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation.addMonitor(
            IntentFilter(Intent.ACTION_OPEN_DOCUMENT).apply {
                addDataType("*/*")
                addCategory(Intent.CATEGORY_OPENABLE)
            },
            Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null), true
        )
        try {
            val repo = RepositorioPrueba(listOf(casoCompleto))
            mostrar(repo)
            compose.onNodeWithText("Importar JSON").performScrollTo().performClick()
            compose.onNodeWithText("Seleccionar archivo JSON").performScrollTo().performClick()
            compose.waitForIdle()
            compose.onNodeWithText("Cancelar importación").performClick()
            guardar()
            compose.runOnIdle {
                assertEquals("Proyecto", repo.ultimoRequest!!.nombre)
                assertEquals(listOf(casoCompleto), repo.ultimoRequest!!.casosDeUso)
            }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    private class RepositorioPrueba(casos: List<CasoUsoDto> = emptyList()) : ProyectoRepository {
        val proyecto = Gson().fromJson("""{"id":7,"nombre":"Proyecto"}""", ProyectoResponseDto::class.java)
            .copy(casosDeUso = casos)
        var ultimoRequest: ProyectoRequest? = null
        var creaciones = 0
        var ediciones = 0
        override suspend fun listarTecnologias(): Result<List<TecnologiaDto>> = Result.success(listOf(TecnologiaDto(1, "Kotlin")))
        override suspend fun verProyecto(id: Long) = Result.success(proyecto)
        override suspend fun crearProyecto(request: ProyectoRequest): Result<ProyectoResponseDto> {
            creaciones++
            ultimoRequest = request
            return Result.success(proyecto.copy(casosDeUso = request.casosDeUso))
        }
        override suspend fun editarProyecto(id: Long, request: ProyectoRequest): Result<ProyectoResponseDto> {
            assertEquals(7L, id)
            ediciones++
            ultimoRequest = request
            return Result.success(proyecto.copy(casosDeUso = request.casosDeUso))
        }
        override suspend fun obtenerPerfil(): Result<PerfilResponseDto> = error("No se usa en este formulario")
        override suspend fun listarProyectos(): Result<List<ProyectoResponseDto>> = error("No se usa en este formulario")
        override suspend fun verEquipo(id: Long): Result<List<IntegranteResponseDto>> = error("No se usa en este formulario")
        override suspend fun listarInvitaciones(): Result<List<InvitacionResponseDto>> = error("No se usa en este formulario")
        override suspend fun aceptarInvitacion(id: Long): Result<Unit> = error("No se usa en este formulario")
        override suspend fun rechazarInvitacion(id: Long): Result<Unit> = error("No se usa en este formulario")
    }
}