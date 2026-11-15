package com.prieto.colab_movil.ui.tareas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Locale

private val AvancePrimary = Color(0xFF3525CD)
private val AvanceBackground = Color(0xFFFAF8FF)
private val AvanceContainer = Color(0xFFEAEDFF)
private val AvanceInk = Color(0xFF131B2E)
private val AvanceMuted = Color(0xFF464555)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarAvanceScreen(
    proyectoId: Long,
    tareaId: Long,
    onBack: () -> Unit,
    onPerfil: () -> Unit,
    viewModel: RegistrarAvanceViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val estado = uiState as? RegistrarAvanceUiState.Success
    val ocupado = estado?.ocupado == true
    var minutos by rememberSaveable(proyectoId, tareaId) { mutableIntStateOf(30) }
    var descripcion by rememberSaveable(proyectoId, tareaId) { mutableStateOf("") }
    var descartar by remember { mutableStateOf(false) }
    var salirAPerfil by remember { mutableStateOf(false) }
    var nuevoEstado by rememberSaveable(proyectoId, tareaId) { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val descripcionValida = descripcion.trim().length in 20..1000

    fun salir(perfil: Boolean = false) {
        if (ocupado) return
        if (descripcion.isNotBlank() || minutos != 30) {
            salirAPerfil = perfil
            descartar = true
        } else if (perfil) onPerfil() else onBack()
    }
    DisposableEffect(lifecycle, proyectoId, tareaId, viewModel) {
        val observer = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) viewModel.cargar(proyectoId, tareaId)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(estado?.mensaje) {
        estado?.mensaje?.let {
            snackbar.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }
    BackHandler { salir() }

    Scaffold(
        containerColor = AvanceBackground,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Registrar avance", color = AvanceInk, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { salir() }, enabled = !ocupado) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver a mis tareas", tint = AvancePrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.cargar(proyectoId, tareaId) }, enabled = !ocupado) {
                        Icon(Icons.Default.Refresh, "Actualizar avance", tint = AvancePrimary)
                    }
                    IconButton(onClick = { salir(true) }, enabled = !ocupado) {
                        Icon(Icons.Default.Person, "Mi perfil", tint = AvancePrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AvanceBackground)
            )
        },
        bottomBar = {
            Surface(color = AvanceBackground, shadowElevation = 8.dp) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { salir() }, enabled = !ocupado, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    Button(
                        onClick = {
                            viewModel.registrar(proyectoId, tareaId, minutos, descripcion) {
                                descripcion = ""
                                minutos = 30
                            }
                        },
                        enabled = estado?.puedeRegistrar == true && descripcionValida,
                        modifier = Modifier.weight(2f),
                        colors = ButtonDefaults.buttonColors(containerColor = AvancePrimary)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(if (ocupado) "Procesando…" else "Guardar avance")
                    }
                }
            }
        }
    ) { padding ->
        when (val state = uiState) {
            RegistrarAvanceUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AvancePrimary)
            }
            is RegistrarAvanceUiState.Error -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(state.mensaje, color = MaterialTheme.colorScheme.error)
                Button(onClick = { viewModel.cargar(proyectoId, tareaId) }) { Text("Reintentar") }
            }
            is RegistrarAvanceUiState.Success -> {
                val datos = state.datos
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding).imePadding().testTag("avance-contenido"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        SeccionAvance {
                            Text("TK-${datos.tarea.id} · ${datos.tarea.etapaNombre}", color = AvancePrimary, fontSize = 12.sp)
                            Text(datos.tarea.nombre, color = AvanceInk, fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
                            Text(datos.proyecto.nombre, color = AvanceMuted)
                            Text("Prioridad: ${datos.tarea.prioridad ?: "Sin prioridad"}", color = AvanceMuted, fontSize = 12.sp)
                            Text(datos.tarea.descripcion ?: "Sin descripción registrada.", color = AvanceMuted, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MetricaAvance("Duración estimada", datos.tarea.duracionDias?.let { "$it días" } ?: "Sin estimación", Modifier.weight(1f))
                                MetricaAvance("Tiempo acumulado", horasAvance(datos.historial.totalMinutos), Modifier.weight(1f))
                            }
                            Text("Con esta sesión: ${horasAvance(datos.historial.totalMinutos + minutos)}", color = AvancePrimary, fontSize = 12.sp)
                            Text("El tiempo invertido no equivale al porcentaje de trabajo completado.", color = AvanceMuted, fontSize = 11.sp)
                        }
                    }
                    if (state.ocupado) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = AvancePrimary) }
                    if (state.desactualizado || datos.planificacion.estado != "ACTIVA") item {
                        AvisoAvance(if (state.desactualizado) "No se pudo verificar el estado actual. Actualiza para continuar."
                        else "Solo se registran avances desde esta pantalla cuando la planificación está ACTIVA.")
                    }
                    if (state.registroIncierto) item {
                        SeccionAvance {
                            Text("Resultado del registro sin confirmar", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                            Text("La respuesta se perdió o fue inválida. Revisa el historial al final de esta pantalla antes de registrar otra sesión para evitar duplicar horas.", fontSize = 13.sp)
                            TextButton(
                                onClick = { viewModel.confirmarHistorialRevisado() },
                                enabled = !state.ocupado && !state.desactualizado
                            ) { Text("Ya revisé el historial") }
                        }
                    }
                    item {
                        SeccionAvance {
                            Text("Estado de la tarea", fontWeight = FontWeight.SemiBold, color = AvanceInk)
                            Text("Actual: ${EstadoTarea.entries.firstOrNull { it.name == datos.tarea.estado }?.titulo ?: datos.tarea.estado.orEmpty()}", color = AvancePrimary)
                            EstadoTarea.entries.chunked(2).forEach { fila ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    fila.forEach { opcion ->
                                        val seleccionada = opcion.name == datos.tarea.estado
                                        Surface(
                                            onClick = { nuevoEstado = opcion.name },
                                            enabled = state.puedeGestionar && opcion in state.transiciones,
                                            color = if (seleccionada) AvanceContainer else Color(0xFFF2F3FF),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f).heightIn(min = 72.dp)
                                        ) {
                                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(opcion.titulo, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                Text(opcion.subtitulo, color = AvanceMuted, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                            Text("Cambiar el estado es una acción independiente: no guarda la descripción ni el tiempo del formulario.", color = AvanceMuted, fontSize = 12.sp)
                            if (datos.tarea.estado == "BLOQUEADA") {
                                AvisoAvance("El bloqueo se calcula por dependencias incompletas. No puede activarse ni quitarse manualmente; puedes documentarlo en el avance.")
                            } else if (datos.tarea.estado == "COMPLETADA") {
                                Text("Una tarea completada no admite nuevas transiciones de estado.", color = AvanceMuted, fontSize = 12.sp)
                            }
                        }
                    }
                    item {
                        SeccionAvance {
                            Text("Horas invertidas en esta sesión", fontWeight = FontWeight.SemiBold, color = AvanceInk)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(horasAvance(minutos.toLong()), color = AvanceInk, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                Row {
                                    IconButton(onClick = { minutos -= 30 }, enabled = state.puedeRegistrar && minutos > 30) { Text("−", fontSize = 24.sp) }
                                    IconButton(onClick = { minutos += 30 }, enabled = state.puedeRegistrar && minutos < 960) { Text("+", fontSize = 24.sp) }
                                }
                            }
                            Text("Paso: 0,5 h · Entre 0,5 y 16 h por sesión. Se guardan como minutos.", color = AvanceMuted, fontSize = 11.sp)
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Descripción del trabajo realizado", fontWeight = FontWeight.SemiBold, color = AvanceInk)
                            OutlinedTextField(
                                value = descripcion,
                                onValueChange = { descripcion = it.take(1000) },
                                enabled = state.puedeRegistrar,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Descripción del avance") },
                                placeholder = { Text("Detalla el trabajo realizado o las dificultades encontradas.") },
                                minLines = 4,
                                isError = descripcion.isNotEmpty() && !descripcionValida,
                                supportingText = { Text("${descripcion.trim().length}/1000 caracteres · Mínimo 20") },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth().background(AvanceContainer, RoundedCornerShape(14.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = AvancePrimary, shape = CircleShape) {
                                Text(datos.perfil.nombre.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase(), Modifier.padding(12.dp), color = Color.White)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(datos.perfil.nombre, fontWeight = FontWeight.SemiBold, color = AvanceInk)
                                Text(datos.tarea.responsableEmail ?: datos.perfil.email, color = AvanceMuted, fontSize = 12.sp)
                            }
                        }
                    }
                    item { Text("Historial de sesiones (${datos.historial.registros.size})", fontWeight = FontWeight.SemiBold, color = AvanceInk) }
                    if (datos.historial.registros.isEmpty()) item { Text("Todavía no se han registrado avances.", color = AvanceMuted) }
                    items(datos.historial.registros, key = { it.id }) { registro ->
                        SeccionAvance {
                            Text("${registro.usuarioNombre} · ${horasAvance(registro.minutos.toLong())}", color = AvancePrimary, fontWeight = FontWeight.SemiBold)
                            Text(fechaAvance(registro.fecha), color = AvanceMuted, fontSize = 12.sp)
                            Text(registro.nota?.takeIf { it.isNotBlank() } ?: "Sin nota registrada.", color = AvanceInk, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
    if (descartar) {
        AlertDialog(
            onDismissRequest = { descartar = false },
            title = { Text("¿Descartar el avance sin guardar?") },
            text = { Text("La descripción y el tiempo de esta sesión aún no se han guardado.") },
            confirmButton = { TextButton(onClick = { descartar = false; if (salirAPerfil) onPerfil() else onBack() }) { Text("Descartar") } },
            dismissButton = { TextButton(onClick = { descartar = false }) { Text("Seguir editando") } }
        )
    }
    nuevoEstado?.let { nombre ->
        val opcion = EstadoTarea.valueOf(nombre)
        AlertDialog(
            onDismissRequest = { nuevoEstado = null },
            title = { Text("Cambiar estado a ${opcion.titulo.lowercase()}") },
            text = { Text("Solo se actualizará el estado. Para guardar las horas y la descripción utiliza «Guardar avance».") },
            confirmButton = {
                TextButton(onClick = { nuevoEstado = null; viewModel.cambiarEstado(proyectoId, tareaId, opcion) }, enabled = estado?.puedeGestionar == true && opcion in estado.transiciones) { Text("Actualizar estado") }
            },
            dismissButton = { TextButton(onClick = { nuevoEstado = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun SeccionAvance(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(15.dp)).padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

@Composable
private fun MetricaAvance(titulo: String, valor: String, modifier: Modifier) {
    Column(modifier.background(Color(0xFFF2F3FF), RoundedCornerShape(9.dp)).padding(10.dp)) {
        Text(titulo, fontSize = 11.sp, color = AvanceMuted)
        Text(valor, fontWeight = FontWeight.SemiBold, color = AvanceInk)
    }
}

@Composable
private fun AvisoAvance(texto: String) {
    Text(texto, Modifier.fillMaxWidth().background(AvanceContainer, RoundedCornerShape(10.dp)).padding(12.dp), color = AvanceMuted, fontSize = 12.sp)
}

internal fun horasAvance(minutos: Long): String = String.format(Locale.forLanguageTag("es"), "%.1f h", minutos / 60.0)

private fun fechaAvance(fecha: String): String = try {
    val entrada = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT).apply { isLenient = false }
    val salida = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("es"))
    entrada.parse(fecha)?.let(salida::format) ?: fecha
} catch (_: Exception) { fecha }
