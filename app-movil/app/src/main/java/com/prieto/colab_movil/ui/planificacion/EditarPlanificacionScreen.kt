package com.prieto.colab_movil.ui.planificacion

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prieto.colab_movil.data.model.IntegranteResponseDto

private val EditBackground = Color(0xFFFAF8FF)
private val EditPrimary = Color(0xFF3525CD)
private val EditPrimaryContainer = Color(0xFFE2DFFF)
private val EditSurface = Color.White
private val EditSurfaceLow = Color(0xFFF2F3FF)
private val EditSurfaceHigh = Color(0xFFE2E7FF)
private val EditText = Color(0xFF131B2E)
private val EditMuted = Color(0xFF646477)
private val EditError = Color(0xFFBA1A1A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarPlanificacionScreen(
    proyectoId: Long,
    onBack: () -> Unit,
    onGuardado: () -> Unit,
    viewModel: EditarPlanificacionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSaving = (uiState as? EditarPlanificacionUiState.Success)?.isSaving == true

    LaunchedEffect(proyectoId) { viewModel.cargar(proyectoId) }
    BackHandler {
        if (!isSaving) onBack()
    }

    Scaffold(
        containerColor = EditBackground,
        topBar = {
            TopAppBar(
                title = { Text("Editar planificación", color = EditText, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isSaving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = EditText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = EditBackground)
            )
        }
    ) { padding ->
        when (val state = uiState) {
            EditarPlanificacionUiState.Loading -> LoadingEditor(Modifier.padding(padding))
            is EditarPlanificacionUiState.Error -> EditorError(
                modifier = Modifier.padding(padding),
                message = state.message,
                onRetry = { viewModel.cargar(proyectoId) },
                onBack = onBack
            )
            is EditarPlanificacionUiState.Success -> EditorContent(
                modifier = Modifier.padding(padding),
                initialStages = state.etapas,
                members = state.equipo,
                isSaving = state.isSaving,
                errorMessage = state.errorMessage,
                onDiscard = onBack,
                onSave = { original, current -> viewModel.guardar(proyectoId, original, current, onGuardado) }
            )
        }
    }
}

@Composable
private fun LoadingEditor(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = EditPrimary)
    }
}

@Composable
private fun EditorError(modifier: Modifier, message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("No se pudo abrir el editor", color = EditText, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message, color = EditError, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = EditPrimary)) { Text("Reintentar") }
        TextButton(onClick = onBack) { Text("Volver", color = EditPrimary) }
    }
}

@Composable
private fun EditorContent(
    modifier: Modifier,
    initialStages: List<EtapaEditableUi>,
    members: List<IntegranteResponseDto>,
    isSaving: Boolean,
    errorMessage: String?,
    onDiscard: () -> Unit,
    onSave: (List<EtapaEditableUi>, List<EtapaEditableUi>) -> Unit
) {
    val originalStages = remember(initialStages) { initialStages }
    var stages by remember(initialStages) { mutableStateOf(initialStages) }
    var selectedStageId by remember(initialStages) { mutableStateOf(initialStages.firstOrNull()?.localId) }
    var stageNameInput by remember { mutableStateOf("") }
    var stageToRename by remember { mutableStateOf<String?>(null) }
    var showStageDialog by remember { mutableStateOf(false) }
    var showTaskDialog by remember { mutableStateOf(false) }

    fun replaceStage(updated: EtapaEditableUi) {
        stages = stages.map { if (it.localId == updated.localId) updated else it }
    }

    fun removeTask(taskLocalId: String) {
        stages = stages.map { stage ->
            stage.copy(tareas = stage.tareas.filterNot { it.localId == taskLocalId }.map { task ->
                task.copy(dependencias = task.dependencias - taskLocalId)
            })
        }
    }

    fun removeStage(stageLocalId: String) {
        val removedTaskIds = stages.firstOrNull { it.localId == stageLocalId }?.tareas.orEmpty().map { it.localId }.toSet()
        stages = stages.filterNot { it.localId == stageLocalId }.map { stage ->
            stage.copy(tareas = stage.tareas.map { task -> task.copy(dependencias = task.dependencias - removedTaskIds) })
        }
        selectedStageId = stages.firstOrNull()?.localId
    }

    val selectedStage = stages.firstOrNull { it.localId == selectedStageId }

    Scaffold(
        modifier = modifier,
        containerColor = EditBackground,
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().background(EditBackground).padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDiscard,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Descartar") }
                Button(
                    onClick = { onSave(originalStages, stages) },
                    enabled = !isSaving && stages.isNotEmpty() && stages.all { it.nombre.isNotBlank() && it.tareas.all { task -> task.nombre.isNotBlank() && (task.duracionDias.toIntOrNull() ?: 0) > 0 } },
                    modifier = Modifier.weight(1.55f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EditPrimary)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Guardando…")
                    } else Text("Guardar cambios", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Gestión de etapas", color = EditText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Edita el borrador y guarda todos los cambios al finalizar.", color = EditMuted, fontSize = 13.sp)
                }
            }
            if (errorMessage != null) {
                item {
                    Surface(color = Color(0xFFFFDAD6), shape = RoundedCornerShape(12.dp)) {
                        Text(errorMessage, modifier = Modifier.padding(12.dp), color = EditError, fontSize = 13.sp)
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Etapas", color = EditText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { stageNameInput = ""; stageToRename = null; showStageDialog = true }, enabled = !isSaving) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Text("Nueva etapa", color = EditPrimary)
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    stages.forEach { stage ->
                        val selected = stage.localId == selectedStageId
                        Surface(
                            onClick = { selectedStageId = stage.localId },
                            color = if (selected) EditPrimary else EditSurfaceHigh,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                "${stage.nombre} · ${stage.tareas.size}",
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                                color = if (selected) Color.White else EditText,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                    if (stages.isEmpty()) Text("Agrega una etapa para comenzar.", color = EditMuted, fontSize = 13.sp)
                }
            }
            if (selectedStage != null) {
                item {
                    StageHeader(
                        stage = selectedStage,
                        disabled = isSaving,
                        onRename = {
                            stageNameInput = selectedStage.nombre
                            stageToRename = selectedStage.localId
                            showStageDialog = true
                        },
                        onDelete = { removeStage(selectedStage.localId) }
                    )
                }
                if (selectedStage.tareas.isEmpty()) {
                    item { EmptyTasksMessage() }
                }
                items(selectedStage.tareas, key = { it.localId }) { task ->
                    EditableTaskCard(
                        task = task,
                        allTasks = stages.flatMap { it.tareas },
                        members = members,
                        disabled = isSaving,
                        onUpdate = { updated ->
                            replaceStage(selectedStage.copy(tareas = selectedStage.tareas.map { if (it.localId == task.localId) updated else it }))
                        },
                        onDelete = { removeTask(task.localId) }
                    )
                }
                item {
                    Button(
                        onClick = { showTaskDialog = true },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EditPrimaryContainer, contentColor = EditPrimary)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Agregar tarea a esta etapa", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showStageDialog) {
        AlertDialog(
            onDismissRequest = { showStageDialog = false },
            title = { Text(if (stageToRename == null) "Nueva etapa" else "Renombrar etapa") },
            text = {
                OutlinedTextField(
                    value = stageNameInput,
                    onValueChange = { stageNameInput = it },
                    label = { Text("Nombre de la etapa") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val nombre = stageNameInput.trim()
                    if (nombre.isNotBlank()) {
                        val localId = stageToRename
                        if (localId == null) {
                            val nueva = EtapaEditableUi("new-stage-${System.nanoTime()}", null, nombre, emptyList())
                            stages = stages + nueva
                            selectedStageId = nueva.localId
                        } else {
                            stages = stages.map { if (it.localId == localId) it.copy(nombre = nombre) else it }
                        }
                        showStageDialog = false
                    }
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showStageDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showTaskDialog && selectedStage != null) {
        NewTaskDialog(
            onDismiss = { showTaskDialog = false },
            onConfirm = { name ->
                val newTask = TareaEditableUi(
                    localId = "new-task-${System.nanoTime()}",
                    remoteId = null,
                    nombre = name,
                    descripcion = "",
                    responsableId = null,
                    prioridad = "MEDIA",
                    duracionDias = "1",
                    dependencias = emptyList()
                )
                replaceStage(selectedStage.copy(tareas = selectedStage.tareas + newTask))
                showTaskDialog = false
            }
        )
    }
}

@Composable
private fun StageHeader(stage: EtapaEditableUi, disabled: Boolean, onRename: () -> Unit, onDelete: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    Surface(color = EditSurface, shape = RoundedCornerShape(16.dp), shadowElevation = 1.dp) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(EditPrimary))
                Spacer(Modifier.width(8.dp))
                Text(stage.nombre, modifier = Modifier.weight(1f), color = EditText, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Box {
                    IconButton(onClick = { menuExpanded = true }, enabled = !disabled) {
                        Icon(Icons.Default.MoreVert, "Opciones de etapa", tint = EditMuted)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Renombrar etapa") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { menuExpanded = false; onRename() }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar etapa", color = EditError) },
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, null, tint = EditError) },
                            onClick = { menuExpanded = false; onDelete() }
                        )
                    }
                }
            }
            Text("${stage.tareas.size} tareas · ${stage.tareas.sumOf { it.duracionDias.toIntOrNull() ?: 0 }} días estimados", color = EditMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun EmptyTasksMessage() {
    Surface(color = EditSurfaceLow, shape = RoundedCornerShape(12.dp)) {
        Text("Esta etapa aún no tiene tareas.", modifier = Modifier.padding(16.dp), color = EditMuted, fontSize = 13.sp)
    }
}

@Composable
private fun NewTaskDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva tarea") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre de la tarea") }) },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) { Text("Agregar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun EditableTaskCard(
    task: TareaEditableUi,
    allTasks: List<TareaEditableUi>,
    members: List<IntegranteResponseDto>,
    disabled: Boolean,
    onUpdate: (TareaEditableUi) -> Unit,
    onDelete: () -> Unit
) {
    var assigneeExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }
    var dependencyExpanded by remember { mutableStateOf(false) }
    val selectedMember = members.firstOrNull { it.integranteId == task.responsableId }
    val availableDependencies = allTasks.filter { it.localId != task.localId && it.localId !in task.dependencias }

    Surface(color = EditSurface, shape = RoundedCornerShape(16.dp), shadowElevation = 1.dp) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = EditPrimaryContainer, shape = RoundedCornerShape(6.dp)) {
                    Text(if (task.remoteId == null) "NUEVA" else "TK-${task.remoteId}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = EditPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDelete, enabled = !disabled) {
                    Icon(Icons.Default.DeleteOutline, "Eliminar tarea", tint = EditError)
                }
            }
            EditorFieldLabel("Nombre de la tarea")
            OutlinedTextField(
                value = task.nombre,
                onValueChange = { onUpdate(task.copy(nombre = it)) },
                enabled = !disabled,
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                colors = editorFieldColors()
            )
            EditorFieldLabel("Descripción")
            OutlinedTextField(
                value = task.descripcion,
                onValueChange = { onUpdate(task.copy(descripcion = it)) },
                enabled = !disabled,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = editorFieldColors()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Column(Modifier.weight(1f)) {
                    EditorFieldLabel("Responsable")
                    Box {
                        OutlinedButton(onClick = { assigneeExpanded = true }, enabled = !disabled, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(9.dp)) {
                            Text(selectedMember?.nombre ?: "Sin asignar", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, color = EditText)
                        }
                        DropdownMenu(expanded = assigneeExpanded, onDismissRequest = { assigneeExpanded = false }) {
                            DropdownMenuItem(text = { Text("Sin asignar") }, onClick = { onUpdate(task.copy(responsableId = null)); assigneeExpanded = false })
                            members.filter { it.estado.equals("ACTIVO", ignoreCase = true) }.forEach { member ->
                                DropdownMenuItem(text = { Text(member.nombre) }, onClick = { onUpdate(task.copy(responsableId = member.integranteId)); assigneeExpanded = false })
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
                    EditorFieldLabel("Prioridad")
                    Box {
                        OutlinedButton(onClick = { priorityExpanded = true }, enabled = !disabled, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(9.dp)) {
                            Text(task.prioridad, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = priorityColor(task.prioridad))
                        }
                        DropdownMenu(expanded = priorityExpanded, onDismissRequest = { priorityExpanded = false }) {
                            listOf("BAJA", "MEDIA", "ALTA").forEach { priority ->
                                DropdownMenuItem(text = { Text(priority) }, onClick = { onUpdate(task.copy(prioridad = priority)); priorityExpanded = false })
                            }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Duración", color = EditMuted, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = task.duracionDias,
                    onValueChange = { onUpdate(task.copy(duracionDias = it.filter(Char::isDigit).take(3))) },
                    enabled = !disabled,
                    modifier = Modifier.width(100.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("días", fontSize = 10.sp) },
                    colors = editorFieldColors()
                )
                Spacer(Modifier.weight(1f))
                Box {
                    TextButton(onClick = { dependencyExpanded = true }, enabled = !disabled) { Text("+ Dependencia", color = EditPrimary, fontSize = 11.sp) }
                    DropdownMenu(expanded = dependencyExpanded, onDismissRequest = { dependencyExpanded = false }) {
                        if (availableDependencies.isEmpty()) DropdownMenuItem(text = { Text("No hay tareas disponibles") }, onClick = { dependencyExpanded = false })
                        availableDependencies.forEach { other ->
                            DropdownMenuItem(text = { Text(other.nombre) }, onClick = {
                                onUpdate(task.copy(dependencias = task.dependencias + other.localId))
                                dependencyExpanded = false
                            })
                        }
                    }
                }
            }
            if (task.dependencias.isEmpty()) {
                Text("Sin dependencias previas", color = EditMuted, fontSize = 11.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Depende de", color = EditMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    task.dependencias.forEach { dependencyId ->
                        val dependency = allTasks.firstOrNull { it.localId == dependencyId }
                        Surface(color = EditSurfaceHigh, shape = RoundedCornerShape(50)) {
                            Row(modifier = Modifier.padding(start = 9.dp, end = 3.dp, top = 3.dp, bottom = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(dependency?.nombre ?: "Tarea eliminada", modifier = Modifier.weight(1f, fill = false), color = EditPrimary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                IconButton(onClick = { onUpdate(task.copy(dependencias = task.dependencias - dependencyId)) }, enabled = !disabled, modifier = Modifier.size(28.dp)) {
                                    Text("×", color = EditMuted, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorFieldLabel(text: String) {
    Text(text.uppercase(), color = EditMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
}

@Composable
private fun editorFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = EditSurfaceLow,
    unfocusedContainerColor = EditSurfaceLow,
    disabledContainerColor = EditSurfaceLow,
    unfocusedBorderColor = Color.Transparent,
    focusedBorderColor = EditPrimary
)

private fun priorityColor(priority: String): Color = when (priority) {
    "ALTA" -> EditError
    "BAJA" -> Color(0xFF006C49)
    else -> EditPrimary
}