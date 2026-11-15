package com.prieto.colab_movil.ui.proyectos

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.prieto.colab_movil.data.model.*
import java.io.InputStream
import java.io.StringReader
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.text.SimpleDateFormat
import java.util.Locale

internal data class ProyectoJsonImportado(
    val request: ProyectoRequest,
    val tecnologias: List<String>
)

internal object ProyectoJsonImporter {
    const val MAX_BYTES = 1_048_576

    fun leerArchivo(input: InputStream): String {
        val bytes = input.readNBytesCompat(MAX_BYTES + 1)
        require(bytes.size <= MAX_BYTES) { "El archivo JSON supera el límite de 1 MiB." }
        return try {
            Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString().removePrefix("\uFEFF")
        } catch (_: java.nio.charset.CharacterCodingException) {
            throw IllegalArgumentException("El archivo debe estar codificado en UTF-8.")
        }
    }

    fun parsear(texto: String, catalogo: List<TecnologiaDto>): ProyectoJsonImportado {
        require(texto.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "El JSON supera el límite de 1 MiB." }
        val raiz = try {
            JsonReader(StringReader(texto.removePrefix("\uFEFF"))).use { reader ->
                val valor = leerValor(reader, 0)
                require(reader.peek() == JsonToken.END_DOCUMENT) { "Debe haber un único objeto JSON." }
                valor
            }
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (_: java.io.IOException) {
            throw IllegalArgumentException("JSON inválido. Revisa las comillas, comas y llaves.")
        } catch (_: IllegalStateException) {
            throw IllegalArgumentException("JSON inválido. Revisa la estructura del documento.")
        }
        val obj = raiz.objeto("proyecto")
        obj.validarCampos(setOf("nombre", "descripcion", "problematica", "objetivoGeneral",
            "objetivosEspecificos", "alcance", "restricciones", "fechaEntrega", "tecnologias",
            "tecnologiasIds", "requerimientosFuncionales", "requerimientosNoFuncionales", "casosDeUso"), "proyecto")
        val nombre = obj.texto("nombre", "nombre", obligatorio = true, maximo = 60)!!
        val fecha = obj.texto("fechaEntrega", "fechaEntrega")?.also {
            val formato = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }
            require(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(it) &&
                runCatching { formato.parse(it) }.isSuccess) { "fechaEntrega debe ser una fecha válida con formato AAAA-MM-DD." }
        }
        val objetivos = obj.get("objetivosEspecificos")?.takeUnless { it.isJsonNull }?.let { valor ->
            if (valor.isJsonArray) valor.asJsonArray.mapIndexed { index, item ->
                item.textoRequerido("objetivosEspecificos[$index]")
            }.joinToString("\n") else valor.textoRequerido("objetivosEspecificos")
        }
        val seleccionadas = mutableListOf<TecnologiaDto>()
        obj.lista("tecnologias").forEachIndexed { index, item ->
            val tecnologia = item.textoRequerido("tecnologias[$index]")
            seleccionadas.add(catalogo.firstOrNull { it.nombre.equals(tecnologia, ignoreCase = true) }
                ?: throw IllegalArgumentException("La tecnología '$tecnologia' no está en el catálogo. Revisa el nombre o recarga el catálogo."))
        }
        obj.lista("tecnologiasIds").forEachIndexed { index, item ->
            val id = if (item.isJsonPrimitive && item.asJsonPrimitive.isNumber) {
                runCatching { item.asBigDecimal.longValueExact() }.getOrNull()
            } else null
            require(id != null && id > 0) { "tecnologiasIds[$index] debe ser un número entero positivo." }
            seleccionadas.add(catalogo.firstOrNull { it.id == id }
                ?: throw IllegalArgumentException("La tecnología con ID $id no está en el catálogo. Recarga el catálogo o revisa el ID."))
        }
        val rf = obj.lista("requerimientosFuncionales").mapIndexed { index, item ->
            val ruta = "requerimientosFuncionales[$index]"
            if (item.isJsonPrimitive) RequerimientoFuncionalDto(null, item.textoRequerido(ruta))
            else item.objeto(ruta).let {
                it.validarCampos(setOf("codigo", "descripcion"), ruta)
                RequerimientoFuncionalDto(it.texto("codigo", "$ruta.codigo"),
                    it.texto("descripcion", "$ruta.descripcion", obligatorio = true)!!)
            }
        }
        val rnf = obj.lista("requerimientosNoFuncionales").mapIndexed { index, item ->
            val ruta = "requerimientosNoFuncionales[$index]"
            if (item.isJsonPrimitive) RequerimientoNoFuncionalDto(null, item.textoRequerido(ruta), null)
            else item.objeto(ruta).let {
                it.validarCampos(setOf("codigo", "descripcion", "categoria"), ruta)
                RequerimientoNoFuncionalDto(it.texto("codigo", "$ruta.codigo"),
                    it.texto("descripcion", "$ruta.descripcion", obligatorio = true)!!,
                    it.texto("categoria", "$ruta.categoria"))
            }
        }
        val casos = obj.lista("casosDeUso").mapIndexed { index, item ->
            val ruta = "casosDeUso[$index]"
            val cu = item.objeto(ruta)
            cu.validarCampos(setOf("nombre", "descripcion", "actores", "precondiciones", "disparador",
                "flujoPrincipal", "flujosAlternativos", "excepciones", "postcondiciones", "reglasDeNegocio"), ruta)
            CasoUsoDto(
                nombre = cu.texto("nombre", "$ruta.nombre", obligatorio = true)!!,
                descripcion = cu.texto("descripcion", "$ruta.descripcion"),
                actores = cu.texto("actores", "$ruta.actores"),
                precondiciones = cu.texto("precondiciones", "$ruta.precondiciones"),
                disparador = cu.texto("disparador", "$ruta.disparador"),
                flujoPrincipal = cu.texto("flujoPrincipal", "$ruta.flujoPrincipal"),
                flujosAlternativos = cu.texto("flujosAlternativos", "$ruta.flujosAlternativos"),
                excepciones = cu.texto("excepciones", "$ruta.excepciones"),
                postcondiciones = cu.texto("postcondiciones", "$ruta.postcondiciones"),
                reglasDeNegocio = cu.texto("reglasDeNegocio", "$ruta.reglasDeNegocio")
            )
        }
        val tecnologias = seleccionadas.distinctBy { it.id }
        return ProyectoJsonImportado(ProyectoRequest(
            nombre = nombre,
            descripcion = obj.texto("descripcion", "descripcion", maximo = 180),
            problematica = obj.texto("problematica", "problematica", maximo = 300),
            objetivoGeneral = obj.texto("objetivoGeneral", "objetivoGeneral"),
            objetivosEspecificos = objetivos,
            alcance = obj.texto("alcance", "alcance"),
            restricciones = obj.texto("restricciones", "restricciones"),
            fechaEntrega = fecha, tecnologiasIds = tecnologias.map { it.id },
            requerimientosFuncionales = rf, requerimientosNoFuncionales = rnf, casosDeUso = casos
        ), tecnologias.map { it.nombre })
    }

    private fun JsonElement.objeto(ruta: String): JsonObject {
        require(isJsonObject) { "$ruta debe ser un objeto JSON." }
        return asJsonObject
    }

    private fun JsonObject.validarCampos(permitidos: Set<String>, ruta: String) {
        val desconocidos = keySet() - permitidos
        require(desconocidos.isEmpty()) { "Campos no reconocidos en $ruta: ${desconocidos.joinToString()}." }
    }

    private fun JsonElement.textoRequerido(ruta: String): String {
        require(isJsonPrimitive && asJsonPrimitive.isString) { "$ruta debe ser texto." }
        return asString.trim().also { require(it.isNotBlank()) { "$ruta es obligatorio y no puede estar vacío." } }
    }

    private fun JsonObject.texto(campo: String, ruta: String, obligatorio: Boolean = false, maximo: Int? = null): String? {
        val valor = get(campo)?.takeUnless { it.isJsonNull }
        require(!obligatorio || valor != null) { "$ruta es obligatorio." }
        if (valor == null) return null
        require(valor.isJsonPrimitive && valor.asJsonPrimitive.isString) { "$ruta debe ser texto." }
        val texto = valor.asString.trim()
        require(!obligatorio || texto.isNotEmpty()) { "$ruta es obligatorio y no puede estar vacío." }
        require(maximo == null || texto.length <= maximo) { "$ruta no puede superar $maximo caracteres." }
        return texto.ifBlank { null }
    }

    private fun JsonObject.lista(campo: String): List<JsonElement> {
        val valor = get(campo)?.takeUnless { it.isJsonNull } ?: return emptyList()
        require(valor.isJsonArray) { "$campo debe ser una lista JSON." }
        return valor.asJsonArray.toList()
    }

    // JsonParser activa el modo permisivo; usamos JsonReader para rechazar JSON mal formado.
    private fun leerValor(reader: JsonReader, profundidad: Int): JsonElement {
        require(profundidad <= 32) { "El JSON contiene demasiados niveles de anidación." }
        return when (reader.peek()) {
            JsonToken.BEGIN_OBJECT -> JsonObject().apply {
                reader.beginObject()
                while (reader.hasNext()) {
                    val nombre = reader.nextName()
                    require(!has(nombre)) { "El campo '$nombre' está duplicado en el JSON." }
                    add(nombre, leerValor(reader, profundidad + 1))
                }
                reader.endObject()
            }
            JsonToken.BEGIN_ARRAY -> JsonArray().apply {
                reader.beginArray()
                while (reader.hasNext()) add(leerValor(reader, profundidad + 1))
                reader.endArray()
            }
            JsonToken.STRING -> JsonPrimitive(reader.nextString())
            JsonToken.NUMBER -> JsonPrimitive(reader.nextString().toBigDecimal())
            JsonToken.BOOLEAN -> JsonPrimitive(reader.nextBoolean())
            JsonToken.NULL -> { reader.nextNull(); JsonNull.INSTANCE }
            else -> throw IllegalArgumentException("JSON inválido o vacío. Debe contener un objeto de proyecto.")
        }
    }

    private fun InputStream.readNBytesCompat(limite: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (output.size() < limite) {
            val leidos = read(buffer, 0, minOf(buffer.size, limite - output.size()))
            if (leidos == -1) break
            output.write(buffer, 0, leidos)
        }
        return output.toByteArray()
    }
}