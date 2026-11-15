package com.prieto.colab_movil.ui.proyectos

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.prieto.colab_movil.data.model.TecnologiaDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

@Composable
internal fun ImportarProyectoJsonDialog(
    catalogo: List<TecnologiaDto>,
    onRecargarCatalogo: () -> Unit,
    onImportar: (ProyectoJsonImportado) -> Unit,
    onCerrar: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var contenido by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    var pendiente by remember { mutableStateOf<ProyectoJsonImportado?>(null) }

    fun leerArchivo(abrir: () -> InputStream?) {
        cargando = true
        error = null
        scope.launch {
            try {
                contenido = withContext(Dispatchers.IO) {
                    val input = abrir() ?: throw IllegalArgumentException("No se pudo abrir el archivo seleccionado.")
                    input.use { ProyectoJsonImporter.leerArchivo(it) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "No se pudo leer el archivo JSON."
            } finally {
                cargando = false
            }
        }
    }

    val selector = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) leerArchivo { context.contentResolver.openInputStream(uri) }
    }

    AlertDialog(
        onDismissRequest = { if (!cargando) onCerrar() },
        title = { Text(if (pendiente == null) "Importar proyecto desde JSON" else "Confirmar importación") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val datos = pendiente
                if (datos == null) {
                    Text("Selecciona un archivo UTF-8 de hasta 1 MiB o pega el JSON. Se validará antes de completar el formulario.")
                    OutlinedButton(
                        onClick = { selector.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                        enabled = !cargando,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Seleccionar archivo JSON") }
                    TextButton(
                        onClick = { leerArchivo { context.assets.open("proyecto-ejemplo.json") } },
                        enabled = !cargando
                    ) { Text("Cargar ejemplo JSON") }
                    Text("Tecnologías disponibles: ${catalogo.size}. Usa nombres del catálogo o tecnologiasIds.")
                    TextButton(onClick = onRecargarCatalogo, enabled = !cargando) { Text("Recargar catálogo") }
                    OutlinedTextField(
                        value = contenido,
                        onValueChange = {
                            if (it.toByteArray(Charsets.UTF_8).size <= ProyectoJsonImporter.MAX_BYTES) {
                                contenido = it
                                error = null
                            } else error = "El JSON supera el límite de 1 MiB."
                        },
                        label = { Text("Contenido JSON") },
                        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).testTag("importar-json-contenido"),
                        minLines = 6,
                        enabled = !cargando,
                        isError = error != null
                    )
                    if (cargando) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                } else {
                    val request = datos.request
                    Text("Proyecto: ${request.nombre}")
                    Text("${datos.tecnologias.size} tecnologías, ${request.requerimientosFuncionales.orEmpty().size} requerimientos funcionales, " +
                        "${request.requerimientosNoFuncionales.orEmpty().size} no funcionales y ${request.casosDeUso.orEmpty().size} casos de uso.")
                    Text("Se reemplazarán los datos actuales del formulario, incluidos los campos ausentes en el JSON. Podrás revisarlos y editarlos antes de guardar.")
                    Text("Importar no registra el proyecto en el servidor.")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !cargando && (pendiente != null || contenido.isNotBlank()),
                onClick = {
                    val datos = pendiente
                    if (datos != null) {
                        onImportar(datos)
                    } else {
                        cargando = true
                        error = null
                        scope.launch {
                            try {
                                pendiente = withContext(Dispatchers.Default) { ProyectoJsonImporter.parsear(contenido, catalogo) }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                error = e.message ?: "No se pudo validar el JSON."
                            } finally {
                                cargando = false
                            }
                        }
                    }
                }
            ) { Text(if (pendiente == null) "Validar JSON" else "Reemplazar formulario") }
        },
        dismissButton = {
            TextButton(onClick = onCerrar, enabled = !cargando) { Text("Cancelar importación") }
            if (pendiente != null) {
                TextButton(onClick = { pendiente = null }) { Text("Volver al JSON") }
            }
        }
    )
}