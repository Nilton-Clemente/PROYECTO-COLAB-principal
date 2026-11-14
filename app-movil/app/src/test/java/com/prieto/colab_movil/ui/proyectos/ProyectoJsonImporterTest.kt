package com.prieto.colab_movil.ui.proyectos

import com.prieto.colab_movil.data.model.TecnologiaDto
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File

class ProyectoJsonImporterTest {
    private val catalogo = listOf(TecnologiaDto(1, "Kotlin"), TecnologiaDto(2, "PostgreSQL"))

    private fun parsear(json: String) = ProyectoJsonImporter.parsear(json, catalogo)

    private fun rechaza(json: String, mensaje: String) {
        val error = assertThrows(IllegalArgumentException::class.java) { parsear(json) }
        assertTrue("Mensaje recibido: ${error.message}", error.message.orEmpty().contains(mensaje))
    }

    @Test
    fun importaEjemploIncluidoConTodosLosCamposDelCasoDeUso() {
        val ejemplo = File("src/main/assets/proyecto-ejemplo.json").readText()
        val datos = parsear(ejemplo).request
        assertEquals("Gestor de proyectos colaborativos", datos.nombre)
        assertEquals(3, datos.objetivosEspecificos!!.lines().size)
        assertEquals("2027-06-30", datos.fechaEntrega)
        assertEquals("RF-01", datos.requerimientosFuncionales!!.single().codigo)
        assertEquals("Seguridad", datos.requerimientosNoFuncionales!!.single().categoria)
        val caso = datos.casosDeUso!!.single()
        listOf(caso.nombre, caso.descripcion, caso.actores, caso.precondiciones, caso.disparador,
            caso.flujoPrincipal, caso.flujosAlternativos, caso.excepciones, caso.postcondiciones,
            caso.reglasDeNegocio).forEach { assertFalse(it.isNullOrBlank()) }
        assertTrue(caso.flujoPrincipal!!.contains("\n"))
    }

    @Test
    fun admiteProyectoMinimoYCamposOpcionalesNulos() {
        val datos = parsear("""{"nombre":"  Proyecto  ","descripcion":null,"casosDeUso":null}""").request
        assertEquals("Proyecto", datos.nombre)
        assertNull(datos.descripcion)
        assertNull(datos.fechaEntrega)
        assertTrue(datos.casosDeUso!!.isEmpty())
        assertTrue(datos.tecnologiasIds!!.isEmpty())
    }

    @Test
    fun resuelveTecnologiasPorNombreOIdSinDuplicar() {
        val datos = parsear("""{"nombre":"P","tecnologias":[" kotlin ","KOTLIN"],"tecnologiasIds":[2,1]}""")
        assertEquals(listOf(1L, 2L), datos.request.tecnologiasIds)
        assertEquals(listOf("Kotlin", "PostgreSQL"), datos.tecnologias)
    }

    @Test
    fun noDescartaSilenciosamenteTecnologiasDesconocidas() {
        rechaza("""{"nombre":"P","tecnologias":["Desconocida"]}""", "no está en el catálogo")
        rechaza("""{"nombre":"P","tecnologiasIds":[999]}""", "ID 999")
        val error = assertThrows(IllegalArgumentException::class.java) {
            ProyectoJsonImporter.parsear("""{"nombre":"P","tecnologiasIds":[1]}""", emptyList())
        }
        assertTrue(error.message!!.contains("catálogo"))
    }

    @Test
    fun idsDebenSerEnterosPositivosSinDesbordamiento() {
        listOf("1.5", "0", "-1", "true", "\"1\"", "null", "9223372036854775808").forEach {
            rechaza("""{"nombre":"P","tecnologiasIds":[$it]}""", "entero positivo")
        }
    }

    @Test
    fun validaNombreObligatorioYSusTipos() {
        listOf("{}", """{"nombre":null}""", """{"nombre":"  "}""").forEach { rechaza(it, "nombre") }
        listOf("7", "true", "[]", "{}").forEach { rechaza("""{"nombre":$it}""", "texto") }
    }

    @Test
    fun respetaLimitesDelFormularioSinTruncarDatos() {
        rechaza("""{"nombre":"${"x".repeat(61)}"}""", "60 caracteres")
        rechaza("""{"nombre":"P","descripcion":"${"x".repeat(181)}"}""", "180 caracteres")
        rechaza("""{"nombre":"P","problematica":"${"x".repeat(301)}"}""", "300 caracteres")
        assertEquals(60, parsear("""{"nombre":"${"x".repeat(60)}"}""").request.nombre.length)
    }

    @Test
    fun validaFechasCalendarioYFormatoIso() {
        listOf("2027-02-29", "2026-04-31", "30/06/2027", "2027-6-1", "2027-13-01", "0000-01-01").forEach {
            rechaza("""{"nombre":"P","fechaEntrega":"$it"}""", "fecha válida")
        }
        assertEquals("2028-02-29", parsear("""{"nombre":"P","fechaEntrega":"2028-02-29"}""").request.fechaEntrega)
    }

    @Test
    fun admiteObjetivosComoListaOComoTexto() {
        assertEquals("Primero\nSegundo", parsear("""{"nombre":"P","objetivosEspecificos":[" Primero ","Segundo"]}""").request.objetivosEspecificos)
        assertEquals("Primero\nSegundo", parsear("""{"nombre":"P","objetivosEspecificos":"Primero\nSegundo"}""").request.objetivosEspecificos)
        rechaza("""{"nombre":"P","objetivosEspecificos":[7]}""", "objetivosEspecificos[0]")
    }

    @Test
    fun conservaDescripcionConDosPuntosYCodigosDeRequerimientos() {
        val datos = parsear("""{"nombre":"P","requerimientosFuncionales":[{"codigo":"RF-01","descripcion":"Permitir: registrar"},"Consultar: tareas"],"requerimientosNoFuncionales":[{"descripcion":"Seguro","categoria":"Seguridad"},"Rápido"]}""").request
        assertEquals("RF-01", datos.requerimientosFuncionales!![0].codigo)
        assertEquals("Permitir: registrar", datos.requerimientosFuncionales[0].descripcion)
        assertNull(datos.requerimientosFuncionales[1].codigo)
        assertEquals("Consultar: tareas", datos.requerimientosFuncionales[1].descripcion)
        assertEquals("Seguridad", datos.requerimientosNoFuncionales!![0].categoria)
        assertEquals("Rápido", datos.requerimientosNoFuncionales[1].descripcion)
    }

    @Test
    fun validaListasYCamposAnidadosConRutasClaras() {
        rechaza("""{"nombre":"P","casosDeUso":{}}""", "casosDeUso debe ser una lista")
        rechaza("""{"nombre":"P","casosDeUso":[null]}""", "casosDeUso[0]")
        rechaza("""{"nombre":"P","casosDeUso":[{"descripcion":"Sin nombre"}]}""", "casosDeUso[0].nombre")
        rechaza("""{"nombre":"P","casosDeUso":[{"nombre":"CU","actores":[]}]}""", "casosDeUso[0].actores")
        rechaza("""{"nombre":"P","requerimientosFuncionales":[{}]}""", "requerimientosFuncionales[0].descripcion")
        rechaza("""{"nombre":"P","requerimientosNoFuncionales":[{"descripcion":""}]}""", "requerimientosNoFuncionales[0].descripcion")
    }

    @Test
    fun rechazaCamposDesconocidosYClavesDuplicadas() {
        rechaza("""{"nombre":"P","casosUso":[]}""", "casosUso")
        rechaza("""{"nombre":"P","casosDeUso":[{"nombre":"CU","actor":"X"}]}""", "actor")
        rechaza("""{"nombre":"P","nombre":"Q"}""", "duplicado")
    }

    @Test
    fun rechazaJsonMalFormadoYDocumentosMultiples() {
        listOf("", "[]", "null", "{", "{nombre:'P'}", """{"nombre":"P",}""",
            """{/* comentario */ "nombre":"P"}""", """{"nombre":"P"} {}""").forEach {
            assertThrows("Debe rechazar: $it", IllegalArgumentException::class.java) { parsear(it) }
        }
    }

    @Test
    fun limitaTamanoYProfundidadDelDocumento() {
        rechaza(" ".repeat(ProyectoJsonImporter.MAX_BYTES + 1), "1 MiB")
        rechaza("[".repeat(34) + "0" + "]".repeat(34), "anidación")
    }

    @Test
    fun leeArchivoUtf8ConAcentosYBom() {
        val texto = """{"nombre":"Planificación ágil"}"""
        val input = ByteArrayInputStream(("\uFEFF" + texto).toByteArray(Charsets.UTF_8))
        assertEquals(texto, ProyectoJsonImporter.leerArchivo(input))
        assertEquals("Planificación ágil", parsear("\uFEFF" + texto).request.nombre)
    }

    @Test
    fun rechazaArchivoDemasiadoGrandeOCodificacionInvalida() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            ProyectoJsonImporter.leerArchivo(ByteArrayInputStream(ByteArray(ProyectoJsonImporter.MAX_BYTES + 1)))
        }
        assertTrue(error.message!!.contains("1 MiB"))
        val codificacion = assertThrows(IllegalArgumentException::class.java) {
            ProyectoJsonImporter.leerArchivo(ByteArrayInputStream(byteArrayOf(0xC3.toByte(), 0x28)))
        }
        assertTrue(codificacion.message!!.contains("UTF-8"))
    }
}