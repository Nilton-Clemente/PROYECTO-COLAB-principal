package com.prieto.colab_movil.ui.tareas

import com.prieto.colab_movil.data.model.estadoProyecto
import com.prieto.colab_movil.ui.proyectos.EstadoProyectoBadge

import androidx.compose.ui.platform.testTag

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prieto.colab_movil.ui.components.ColabActualizar
import com.prieto.colab_movil.ui.components.ColabBarraSecundaria
import com.prieto.colab_movil.data.model.TareaDetalleResponseDto
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val Indigo = Color(0xFF3525CD)
private val SurfaceBg = Color(0xFFFAF8FF)
private val SurfaceLow = Color(0xFFF2F3FF)
private val SurfaceContainer = Color(0xFFEAEDFF)
private val Ink = Color(0xFF131B2E)
private val Muted = Color(0xFF464555)
private val Green = Color(0xFF006C49)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisTareasScreen(
    proyectoId: Long,
    onBack: () -> Unit,
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
    var pedirCambios by rememberSaveable(proyectoId) { mutableStateOf(false) }
    var borradorSolicitud by rememberSaveable(proyectoId) { mutableStateOf("") }

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
            ColabBarraSecundaria("Mis tareas", onVolver = onBack, habilitado = !ocupado, descripcionVolver = "Volver al proyecto") {
                IconButton(onClick = { viewModel.cargar(proyectoId) }, enabled = !ocupado) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar tareas", tint = Indigo)
                }
            }
        },
        bottomBar = {
            if (estado != null && !estado.cerrado && estado.datos.tareas.isNotEmpty()) Surface(color = Color.White, shadowElevation = 8.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    if (estado.datos.tareas.isNotEmpty()) {
                        Text(
                            if (estado.activa) "Planificación activa · ${estado.datos.tareas.size} tareas asignadas"
                            else if (estado.confirmadas) "Esperando confirmaciones del equipo"
                            else "0 de ${estado.datos.tareas.size} confirmadas",
                            color = Muted, fontSize = 12.sp
                        )
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!estado.activa && estado.puedeSolicitarCambios) {
                            OutlinedButton(
                                onClick = { pedirCambios = true },
                                enabled = !ocupado,
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.EditNote, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Solicitar cambios", fontSize = 12.sp)
                            }
                        }
                        Button(
                            onClick = {
                                if (estado.activa) seleccionarTarea = true else confirmarTodas = true
                            },
                            enabled = if (estado.activa) estado.puedeRegistrarAvance else estado.puedeConfirmar,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo)
                        ) {
                            Icon(Icons.Default.Checklist, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (estado.activa) "Registrar avance" else "Confirmar todas", fontSize = 12.sp)
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
            is MisTareasUiState.Success -> ColabActualizar(
                actualizando = state.isRefreshing, habilitado = !state.ocupado,
                onActualizar = { viewModel.cargar(proyectoId) }, modifier = Modifier.padding(padding).testTag("tareas-actualizar")
            ) {
                MisTareasContenido(state = state, modifier = Modifier.fillMaxSize(), onRegistrarAvance = onRegistrarAvance)
            }
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
    if (confirmarTodas && estado != null && !estado.activa && !estado.cerrado) {
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
    if (pedirCambios && estado != null && !estado.activa && !estado.cerrado) {
        AlertDialog(
            onDismissRequest = { if (!ocupado) pedirCambios = false },
            title = { Text("Solicitar cambios") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Se enviará al creador como solicitud de cambios de la planificación y se reiniciarán las confirmaciones del resto del equipo.", fontSize = 12.sp)
                    OutlinedTextField(
                        value = borradorSolicitud,
                        onValueChange = { borradorSolicitud = it },
                        enabled = !ocupado,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp),
                        label = { Text("Motivos del ajuste") },
                        minLines = 3
                    )
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        TextButton(onClick = { borradorSolicitud = "$borradorSolicitud\nSugiero ampliar el plazo en 2 días.".trim() }, enabled = !ocupado) { Text("+2 días") }
                        TextButton(onClick = { borradorSolicitud = "$borradorSolicitud\nSolicito apoyo de otro integrante.".trim() }, enabled = !ocupado) { Text("Pedir apoyo") }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.solicitarCambios(proyectoId, borradorSolicitud) {
                            pedirCambios = false
                            borradorSolicitud = ""
                        }
                    },
                    enabled = estado.puedeSolicitarCambios && borradorSolicitud.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Enviar solicitud")
                }
            },
            dismissButton = { TextButton(onClick = { if (!ocupado) pedirCambios = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun MisTareasContenido(
    state: MisTareasUiState.Success,
    modifier: Modifier,
    onRegistrarAvance: (Long) -> Unit
) {
    val datos = state.datos
    var filtroNombre by rememberSaveable(datos.proyecto.id) { mutableStateOf(FiltroTareas.TODAS.name) }
    val filtro = FiltroTareas.valueOf(filtroNombre)
    var detallesId by rememberSaveable(datos.proyecto.id) { mutableStateOf<Long?>(null) }
    var busqueda by rememberSaveable(datos.proyecto.id) { mutableStateOf("") }
    var filtroEstadoNombre by rememberSaveable(datos.proyecto.id) { mutableStateOf(FiltroEstadoTareas.TODAS.name) }
    val filtroEstado = FiltroEstadoTareas.valueOf(filtroEstadoNombre)
    val enEjecucion = state.activa || state.cerrado
    val tareasBase = if (enEjecucion) datos.tareas else state.tareasVisibles(filtro)
    val visibles = filtrarMisTareas(tareasBase, busqueda, if (enEjecucion) filtroEstado else FiltroEstadoTareas.TODAS)
    val tareasPorId = datos.planificacion?.etapas.orEmpty().flatMap { it.tareas.orEmpty() }.associateBy { it.id }

    LazyColumn(
        modifier = modifier.testTag("mis-tareas-lista"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                Modifier.fillMaxWidth().background(
                    Brush.linearGradient(listOf(SurfaceContainer, Color(0xFFF5F3FF))), RoundedCornerShape(20.dp)
                ).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Folder, null, tint = Indigo, modifier = Modifier.size(18.dp))
                    Text(datos.proyecto.nombre, color = Indigo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("Hola, ${datos.perfil.nombre}", color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text(when {
                    state.cerrado -> "Consulta tus tareas y el historial de trabajo del proyecto."
                    state.activa -> "Organiza tu trabajo y registra los avances de tus tareas."
                    else -> "Revisa tus asignaciones antes de confirmar la planificación."
                }, color = Muted, fontSize = 13.sp)
                EstadoProyectoBadge(datos.proyecto.estadoProyecto)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ResumenTareas("Asignadas", datos.tareas.size, Indigo, Modifier.weight(1f))
                    ResumenTareas("En progreso", datos.tareas.count { it.estado == "EN_PROGRESO" }, Indigo, Modifier.weight(1f))
                    ResumenTareas("Completadas", datos.tareas.count { it.estado == "COMPLETADA" }, Green, Modifier.weight(1f))
                }
            }
        }
        if (datos.tareas.isNotEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = busqueda, onValueChange = { busqueda = it },
                    modifier = Modifier.fillMaxWidth().testTag("buscar-mis-tareas"),
                    singleLine = true, shape = RoundedCornerShape(14.dp),
                    label = { Text("Buscar en mis tareas") },
                    placeholder = { Text("Nombre, código o etapa", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Muted) },
                    trailingIcon = {
                        if (busqueda.isNotEmpty()) IconButton(onClick = { busqueda = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
                )
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (enEjecucion) FiltroEstadoTareas.entries.forEach { opcion ->
                        FilterChip(
                            selected = filtroEstado == opcion,
                            onClick = { filtroEstadoNombre = opcion.name },
                            label = { Text("${opcion.titulo} (${filtrarMisTareas(datos.tareas, "", opcion).size})", fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Indigo, selectedLabelColor = Color.White)
                        )
                    } else {
                    FiltroTareas.entries.forEach { opcion ->
                        FilterChip(
                            selected = filtro == opcion,
                            onClick = { filtroNombre = opcion.name },
                            label = { Text("${opcion.titulo} (${state.tareasVisibles(opcion).size})", fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Indigo, selectedLabelColor = Color.White)
                        )
                    }
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
                state.cerrado -> "El proyecto está cerrado. Puedes consultar el historial de tus tareas en modo de solo lectura."
                state.activa -> "El equipo confirmó la planificación. Ya puedes registrar sesiones de trabajo y actualizar el estado de cada tarea."
                datos.planificacion.estado != "PROPUESTA" -> "La planificación no está en estado PROPUESTA. Puedes consultar tus tareas, pero no confirmar ni solicitar cambios."
                else -> "La confirmación abarca todas tus tareas. Solicitar cambios notifica al creador y reinicia las confirmaciones del resto del equipo."
            })
        }
        datos.confirmacion?.takeUnless { state.activa || state.cerrado }?.let { confirmacion ->
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
        if (datos.tareas.isNotEmpty()) item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (state.cerrado) "Historial de tareas" else "Tus asignaciones", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("${visibles.size} de ${datos.tareas.size}", color = Muted, fontSize = 12.sp)
            }
        }
        if (datos.tareas.isNotEmpty() && visibles.isEmpty()) item {
            Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Search, null, tint = Indigo, modifier = Modifier.size(28.dp))
                Text(if (busqueda.isBlank()) "No hay tareas en esta categoría." else "No hay tareas que coincidan con tu búsqueda.", color = Muted)
                TextButton(onClick = { busqueda = ""; filtroNombre = FiltroTareas.TODAS.name; filtroEstadoNombre = FiltroEstadoTareas.TODAS.name }) {
                    Text("Mostrar todas las tareas")
                }
            }
        }
        items(visibles, key = { it.id }) { tarea ->
            TareaAsignadaCard(
                tarea = tarea,
                confirmada = state.confirmadas,
                dependencias = tarea.dependencias.orEmpty().joinToString("\n") { id ->
                    "TK-$id${tareasPorId[id]?.nombre?.let { " · $it" }.orEmpty()}"
                }.ifBlank { "Sin dependencias previas" },
                onDetalles = { detallesId = tarea.id },
                activa = state.activa,
                cerrado = state.cerrado,
                puedeRegistrarAvance = state.puedeRegistrarAvance,
                onRegistrarAvance = { onRegistrarAvance(tarea.id) }
            )
        }
    }

    datos.tareas.firstOrNull { it.id == detallesId }?.let { tarea ->
        AlertDialog(
            onDismissRequest = { detallesId = null },
            title = { Text("TK-${tarea.id} · ${tarea.nombre}") },
            text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tarea.descripcion?.takeIf { it.isNotBlank() } ?: "Sin descripción registrada.")
                    Text("Etapa: ${tarea.etapaNombre}")
                    Text("Responsable: ${tarea.responsableEmail ?: datos.perfil.email}")
                    Text("Estado de ejecución: ${etiquetaEstadoTarea(tarea.estado)}")
                    Text("Prioridad: ${tarea.prioridad ?: "Sin prioridad"}")
                    Text(ventanaTarea(tarea))
                    Text("Dependencias", fontWeight = FontWeight.SemiBold)
                    Text(tarea.dependencias.orEmpty().joinToString("\n") { id ->
                        "TK-$id${tareasPorId[id]?.nombre?.let { " · $it" }.orEmpty()}"
                    }.ifBlank { "Sin dependencias previas" })
                }
            },
            confirmButton = { TextButton(onClick = { detallesId = null }) { Text("Cerrar") } }
        )
    }
}

@Composable
private fun ResumenTareas(titulo: String, cantidad: Int, color: Color, modifier: Modifier) {
    Column(modifier.background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(12.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(cantidad.toString(), color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(titulo, color = Muted, fontSize = 10.sp)
    }
}

private fun etiquetaEstadoTarea(estado: String?): String =
    EstadoTarea.entries.firstOrNull { it.name == estado }?.titulo ?: estado?.takeIf { it.isNotBlank() } ?: "Sin estado"

@Composable
private fun EtiquetaTarea(texto: String, fondo: Color, color: Color) {
    Surface(color = fondo, shape = RoundedCornerShape(8.dp)) {
        Text(texto, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TareaAsignadaCard(
    tarea: TareaDetalleResponseDto,
    confirmada: Boolean,
    dependencias: String,
    onDetalles: () -> Unit,
    activa: Boolean,
    cerrado: Boolean,
    puedeRegistrarAvance: Boolean,
    onRegistrarAvance: () -> Unit
) {
    val colorEstado = when (tarea.estado) {
        "COMPLETADA" -> Green
        "BLOQUEADA" -> Color(0xFF93000A)
        "EN_PROGRESO" -> Indigo
        else -> Muted
    }
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().testTag("tarea-${tarea.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE3E5F1))
    ) {
      Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("TK-${tarea.id}", color = Indigo, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(tarea.etapaNombre, Modifier.weight(1f), color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(tarea.nombre, color = Ink, fontSize = 18.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            EtiquetaTarea(etiquetaEstadoTarea(tarea.estado), colorEstado.copy(alpha = 0.09f), colorEstado)
            EtiquetaTarea(tarea.prioridad?.takeIf { it.isNotBlank() } ?: "Sin prioridad",
                if (tarea.prioridad == "ALTA") Color(0xFFFFEDEA) else SurfaceLow,
                if (tarea.prioridad == "ALTA") Color(0xFF93000A) else Muted)
            if (!activa && !cerrado) {
                EtiquetaTarea(if (confirmada) "Asignación confirmada" else "Por confirmar",
                    if (confirmada) Color(0xFFD8F5E8) else SurfaceContainer,
                    if (confirmada) Green else Indigo)
            }
        }
        Text(tarea.descripcion?.takeIf { it.isNotBlank() } ?: "Sin descripción registrada.",
            color = Muted, fontSize = 13.sp, lineHeight = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Column(
            Modifier.fillMaxWidth().background(SurfaceLow, RoundedCornerShape(12.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Schedule, null, tint = Indigo, modifier = Modifier.size(17.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Fechas estimadas", color = Muted, fontSize = 10.sp)
                    Text(ventanaTarea(tarea), color = Ink, fontSize = 12.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Link, null, tint = Indigo, modifier = Modifier.size(17.dp))
                Text(dependencias, color = Muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(when {
                cerrado -> "Solo lectura"
                activa -> "Planificación activa"
                confirmada -> "Asignación confirmada"
                else -> "Por confirmar en conjunto"
            }, Modifier.weight(1f), color = if (confirmada) Green else Muted, fontSize = 11.sp)
            TextButton(onClick = onDetalles) { Text("Ver detalles", fontSize = 12.sp) }
        }
        if (cerrado) {
            OutlinedButton(onClick = onRegistrarAvance, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Ver historial de avances") }
        } else if (activa) {
            Button(onClick = onRegistrarAvance, enabled = puedeRegistrarAvance, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo)) {
                Icon(Icons.Default.EditNote, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text("Registrar avance de esta tarea")
            }
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
