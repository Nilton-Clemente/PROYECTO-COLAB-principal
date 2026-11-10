package com.prieto.colab_movil.ui.tareas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prieto.colab_movil.data.model.TareaDetalleResponseDto
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val Indigo = Color(0xFF3525CD)
private val SurfaceBg = Color(0xFFFAF8FF)
private val SurfaceLow = Color(0xFFF2F3FF)
private val SurfaceContainer = Color(0xFFEAEDFF)
private val SurfaceHigh = Color(0xFFDAE2FD)
private val Ink = Color(0xFF131B2E)
private val Muted = Color(0xFF464555)
private val Green = Color(0xFF006C49)
private val GreenContainer = Color(0xFF6CF8BB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisTareasScreen(
    proyectoId: Long,
    onBack: () -> Unit,
    onPerfil: () -> Unit,
    viewModel: MisTareasViewModel = viewModel(),
    onRegistrarAvance: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val estado = uiState as? MisTareasUiState.Success
    val ocupado = estado?.ocupado == true
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val snackbar = remember { SnackbarHostState() }
    var confirmarTodas by rememberSaveable(proyectoId) { mutableStateOf(false) }
    var seleccionarTarea by rememberSaveable(proyectoId) { mutableStateOf(false) }

    DisposableEffect(lifecycle, proyectoId, viewModel) {
        val observer = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) viewModel.cargar(proyectoId)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    // Detectar la confirmación del último compañero sin exigir salir de la pantalla.
    LaunchedEffect(lifecycle, proyectoId, viewModel) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                delay(30_000)
                val actual = viewModel.uiState.value as? MisTareasUiState.Success
                if (actual?.datos?.planificacion?.estado == "PROPUESTA" && !actual.ocupado) {
                    viewModel.cargar(proyectoId)
                }
            }
        }
    }
    LaunchedEffect(estado?.mensaje) {
        estado?.mensaje?.let {
            snackbar.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }
    BackHandler(enabled = ocupado) { /* Evitar abandonar una petición en curso. */ }

    Scaffold(
        containerColor = SurfaceBg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Mis tareas", color = Ink, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !ocupado) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver al proyecto", tint = Indigo)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.cargar(proyectoId) }, enabled = !ocupado) {
                        Icon(Icons.Default.Refresh, "Actualizar tareas", tint = Indigo)
                    }
                    IconButton(onClick = onPerfil, enabled = !ocupado) {
                        Icon(Icons.Default.Person, "Mi perfil", tint = Indigo)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceBg)
            )
        },
        bottomBar = {
            Surface(color = SurfaceBg, shadowElevation = 8.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    if (estado != null && estado.datos.tareas.isNotEmpty()) {
                        Text(
                            if (estado.activa) "Planificación activa · ${estado.datos.tareas.size} tareas asignadas"
                            else if (estado.confirmadas) "Esperando confirmaciones del equipo"
                            else "0 de ${estado.datos.tareas.size} confirmadas",
                            color = Muted, fontSize = 12.sp
                        )
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = onBack, enabled = !ocupado) { Text("Volver al proyecto") }
                        Button(
                            onClick = {
                                if (estado?.activa == true) seleccionarTarea = true else confirmarTodas = true
                            },
                            enabled = if (estado?.activa == true) estado.puedeRegistrarAvance else estado?.puedeConfirmar == true,
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo)
                        ) {
                            Icon(Icons.Default.Checklist, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (estado?.activa == true) "Registrar avance" else "Confirmar todas", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    ) { padding ->
        when (val state = uiState) {
            MisTareasUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Indigo)
            }
            is MisTareasUiState.Error -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(state.mensaje, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.cargar(proyectoId) }) { Text("Reintentar") }
            }
            is MisTareasUiState.Success -> MisTareasContenido(
                state = state,
                modifier = Modifier.fillMaxSize().padding(padding),
                onSolicitarCambios = { tareaId, comentario, onEnviado ->
                    viewModel.solicitarCambios(proyectoId, tareaId, comentario, onEnviado)
                },
                onRegistrarAvance = onRegistrarAvance
            )
        }
    }

    if (seleccionarTarea && estado?.activa == true) {
        AlertDialog(
            onDismissRequest = { seleccionarTarea = false },
            title = { Text("Selecciona una tarea") },
            text = {
                LazyColumn(Modifier.heightIn(max = 350.dp)) {
                    items(estado.datos.tareas, key = { it.id }) { tarea ->
                        TextButton(onClick = { seleccionarTarea = false; onRegistrarAvance(tarea.id) }, enabled = estado.puedeRegistrarAvance) {
                            Text("TK-${tarea.id} · ${tarea.nombre}")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { seleccionarTarea = false }) { Text("Cerrar") } }
        )
    }
    if (confirmarTodas && estado != null && !estado.activa) {
        AlertDialog(
            onDismissRequest = { confirmarTodas = false },
            title = { Text("Confirmar tus tareas") },
            text = { Text("Confirmarás la estimación y el alcance de todas tus ${estado.datos.tareas.size} tareas asignadas. Esta confirmación se guarda para tu participación en la planificación.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarTodas = false
                    viewModel.confirmar(proyectoId)
                }, enabled = estado.puedeConfirmar) { Text("Confirmar todas") }
            },
            dismissButton = { TextButton(onClick = { confirmarTodas = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun MisTareasContenido(
    state: MisTareasUiState.Success,
    modifier: Modifier,
    onSolicitarCambios: (Long, String, () -> Unit) -> Unit,
    onRegistrarAvance: (Long) -> Unit
) {
    val datos = state.datos
    var filtroNombre by rememberSaveable(datos.proyecto.id) { mutableStateOf(FiltroTareas.TODAS.name) }
    val filtro = FiltroTareas.valueOf(filtroNombre)
    var tareaSolicitudId by rememberSaveable(datos.proyecto.id) { mutableStateOf<Long?>(null) }
    var borrador by rememberSaveable(datos.proyecto.id) { mutableStateOf("") }
    var detallesId by rememberSaveable(datos.proyecto.id) { mutableStateOf<Long?>(null) }
    var descartarSolicitud by remember { mutableStateOf(false) }
    val tareasPorId = datos.planificacion?.etapas.orEmpty().flatMap { it.tareas.orEmpty() }.associateBy { it.id }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                Modifier.fillMaxWidth().background(SurfaceContainer, RoundedCornerShape(18.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, null, tint = Indigo)
                    Spacer(Modifier.width(8.dp))
                    Text(datos.proyecto.nombre, color = Indigo, fontWeight = FontWeight.SemiBold)
                }
                Text("Hola, ${datos.perfil.nombre}", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(if (state.activa) "Registra el trabajo realizado en tus ${datos.tareas.size} tareas asignadas."
                else "Revisa la estimación y el alcance de tus ${datos.tareas.size} tareas asignadas.", color = Muted, fontSize = 13.sp)
                Text("Planificación: ${datos.planificacion?.estado ?: "Sin generar"}", color = Muted, fontSize = 12.sp)
                if (!state.activa) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    FiltroTareas.entries.forEach { opcion ->
                        FilterChip(
                            selected = filtro == opcion,
                            onClick = { filtroNombre = opcion.name },
                            label = { Text("${opcion.titulo} (${state.tareasVisibles(opcion).size})", fontSize = 12.sp) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Indigo, selectedLabelColor = Color.White)
                        )
                    }
                }
            }
        }
        if (state.ocupado) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Indigo) }
        item {
            AvisoTareas(when {
                state.datosDesactualizados -> "No se pudo verificar el estado actual. Actualiza los datos antes de confirmar o solicitar cambios."
                datos.planificacion == null -> "El proyecto aún no tiene una planificación. Las tareas aparecerán cuando se genere y se te asigne trabajo."
                datos.tareas.isEmpty() -> "No tienes tareas asignadas en este proyecto."
                state.activa -> "El equipo confirmó la planificación. Ya puedes registrar sesiones de trabajo y actualizar el estado de cada tarea."
                datos.planificacion.estado != "PROPUESTA" -> "La planificación no está en estado PROPUESTA. Puedes consultar tus tareas, pero no confirmar ni solicitar cambios."
                else -> "La confirmación abarca todas tus tareas. Solicitar cambios notifica al creador y reinicia las confirmaciones del resto del equipo."
            })
        }
        datos.confirmacion?.takeUnless { state.activa }?.let { confirmacion ->
            item {
                AvisoTareas(
                    when (confirmacion.estado) {
                        "ACEPTADA" -> "Asignaciones confirmadas${confirmacion.fecha?.let { " el ${formatearFecha(it, true)}" }.orEmpty()}."
                        "CAMBIOS_SOLICITADOS" -> "Cambios solicitados: ${confirmacion.comentario.orEmpty()}"
                        else -> "Confirmación: ${confirmacion.estado}"
                    }
                )
            }
        }
        if (!state.activa && datos.tareas.isNotEmpty() && state.tareasVisibles(filtro).isEmpty()) {
            item { Text("No hay tareas en esta categoría.", color = Muted) }
        }
        items(if (state.activa) datos.tareas else state.tareasVisibles(filtro), key = { it.id }) { tarea ->
            TareaAsignadaCard(
                tarea = tarea,
                confirmada = state.confirmadas,
                dependencias = tarea.dependencias.orEmpty().joinToString("\n") { id ->
                    "TK-$id${tareasPorId[id]?.nombre?.let { " · $it" }.orEmpty()}"
                }.ifBlank { "Sin dependencias previas" },
                puedeSolicitarCambios = state.puedeSolicitarCambios,
                onAjustes = { tareaSolicitudId = tarea.id; borrador = "" },
                onDetalles = { detallesId = tarea.id },
                activa = state.activa,
                puedeRegistrarAvance = state.puedeRegistrarAvance,
                onRegistrarAvance = { onRegistrarAvance(tarea.id) }
            )
        }
    }

    val tareaSolicitud = datos.tareas.firstOrNull { it.id == tareaSolicitudId }
    if (tareaSolicitud != null && !state.activa) {
        fun cerrarSolicitud() {
            if (state.ocupado) return
            if (borrador.isBlank()) tareaSolicitudId = null else descartarSolicitud = true
        }
        AlertDialog(
            onDismissRequest = { cerrarSolicitud() },
            title = { Text("Solicitar ajuste · TK-${tareaSolicitud.id}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(tareaSolicitud.nombre, fontWeight = FontWeight.SemiBold)
                    Text("Se enviará al creador como solicitud de cambios de la planificación y se reiniciarán las confirmaciones del resto del equipo.", fontSize = 12.sp)
                    OutlinedTextField(
                        value = borrador,
                        onValueChange = { borrador = it },
                        enabled = !state.ocupado,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp),
                        label = { Text("Motivos del ajuste") },
                        minLines = 3
                    )
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        TextButton(onClick = { borrador = "$borrador\nSugiero ampliar el plazo en 2 días.".trim() }, enabled = !state.ocupado) { Text("+2 días") }
                        TextButton(onClick = { borrador = "$borrador\nSolicito apoyo de otro integrante.".trim() }, enabled = !state.ocupado) { Text("Pedir apoyo") }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onSolicitarCambios(tareaSolicitud.id, borrador) { tareaSolicitudId = null; borrador = "" } },
                    enabled = state.puedeSolicitarCambios && borrador.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Enviar solicitud")
                }
            },
            dismissButton = { TextButton(onClick = { cerrarSolicitud() }, enabled = !state.ocupado) { Text("Cancelar") } }
        )
    }
    if (descartarSolicitud) {
        AlertDialog(
            onDismissRequest = { descartarSolicitud = false },
            title = { Text("¿Descartar el borrador?") },
            text = { Text("El texto de esta solicitud todavía no se ha enviado.") },
            confirmButton = { TextButton(onClick = { descartarSolicitud = false; tareaSolicitudId = null; borrador = "" }) { Text("Descartar") } },
            dismissButton = { TextButton(onClick = { descartarSolicitud = false }) { Text("Seguir editando") } }
        )
    }
    datos.tareas.firstOrNull { it.id == detallesId }?.let { tarea ->
        AlertDialog(
            onDismissRequest = { detallesId = null },
            title = { Text("TK-${tarea.id} · ${tarea.nombre}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tarea.descripcion?.takeIf { it.isNotBlank() } ?: "Sin descripción registrada.")
                    Text("Etapa: ${tarea.etapaNombre}")
                    Text("Responsable: ${tarea.responsableEmail ?: datos.perfil.email}")
                    Text("Estado de ejecución: ${tarea.estado ?: "Sin estado"}")
                    Text("Prioridad: ${tarea.prioridad ?: "Sin prioridad"}")
                    Text(ventanaTarea(tarea))
                }
            },
            confirmButton = { TextButton(onClick = { detallesId = null }) { Text("Cerrar") } }
        )
    }
}

@Composable
private fun AvisoTareas(texto: String) {
    Row(Modifier.fillMaxWidth().background(SurfaceLow, RoundedCornerShape(12.dp)).padding(12.dp)) {
        Icon(Icons.Default.Info, null, tint = Indigo, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(9.dp))
        Text(texto, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun TareaAsignadaCard(
    tarea: TareaDetalleResponseDto,
    confirmada: Boolean,
    dependencias: String,
    puedeSolicitarCambios: Boolean,
    onAjustes: () -> Unit,
    onDetalles: () -> Unit,
    activa: Boolean,
    puedeRegistrarAvance: Boolean,
    onRegistrarAvance: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = SurfaceHigh, shape = RoundedCornerShape(5.dp)) {
                Text("TK-${tarea.id}", Modifier.padding(6.dp), color = Ink, fontSize = 11.sp)
            }
            Text(tarea.etapaNombre, Modifier.weight(1f), color = Indigo, fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = if (confirmada) GreenContainer else SurfaceContainer, shape = CircleShape) {
                Text(if (confirmada) "Asignación confirmada" else "Por confirmar", Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = if (confirmada) Green else Indigo, fontSize = 11.sp)
            }
            Text(tarea.prioridad ?: "Sin prioridad", color = if (tarea.prioridad == "ALTA") Color(0xFF93000A) else Muted, fontSize = 11.sp)
        }
        Text(tarea.nombre, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(tarea.descripcion?.takeIf { it.isNotBlank() } ?: "Sin descripción registrada.", color = Muted, fontSize = 13.sp)
        Column(
            Modifier.fillMaxWidth().background(SurfaceLow, RoundedCornerShape(10.dp)).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(if (confirmada) "VENTANA VALIDADA" else "VENTANA ESTIMADA", color = Muted, fontSize = 10.sp)
            Text(ventanaTarea(tarea), color = Ink, fontSize = 12.sp)
            Text("DEPENDENCIAS", color = Muted, fontSize = 10.sp)
            Text(dependencias, color = Ink, fontSize = 12.sp)
            Text("Estado de ejecución: ${tarea.estado ?: "Sin estado"}", color = Muted, fontSize = 12.sp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            if (confirmada) Icon(Icons.Default.CheckCircle, "Asignación confirmada", tint = Green)
            else Text("Confirma con «Confirmar todas»", Modifier.weight(1f), color = Muted, fontSize = 11.sp)
            TextButton(onClick = onDetalles) { Text("Ver detalles", fontSize = 12.sp) }
        }
        if (puedeSolicitarCambios) {
            OutlinedButton(onClick = onAjustes, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.EditNote, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text("Solicitar ajuste")
            }
        }
        if (activa) {
            Button(onClick = onRegistrarAvance, enabled = puedeRegistrarAvance, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo)) {
                Icon(Icons.Default.EditNote, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text("Registrar avance de esta tarea")
            }
        }
    }
}

private fun ventanaTarea(tarea: TareaDetalleResponseDto): String {
    val inicio = tarea.fechaInicio?.let { formatearFecha(it) } ?: "Inicio sin programar"
    val fin = tarea.fechaFin?.let { formatearFecha(it) } ?: "Fin sin programar"
    return "$inicio – $fin${tarea.duracionDias?.let { " ($it días)" }.orEmpty()}"
}

private fun formatearFecha(valor: String, incluirHora: Boolean = false): String = try {
    // SimpleDateFormat permite conservar minSdk 24 sin añadir desugaring de java.time.
    val entrada = SimpleDateFormat(if (incluirHora) "yyyy-MM-dd'T'HH:mm:ss" else "yyyy-MM-dd", Locale.ROOT)
    entrada.isLenient = false
    val salida = SimpleDateFormat(if (incluirHora) "dd MMM yyyy, HH:mm" else "dd MMM yyyy", Locale.forLanguageTag("es"))
    entrada.parse(valor)?.let(salida::format) ?: valor
} catch (_: Exception) {
    valor
}
