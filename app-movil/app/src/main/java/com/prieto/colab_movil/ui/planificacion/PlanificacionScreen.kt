package com.prieto.colab_movil.ui.planificacion

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prieto.colab_movil.data.model.IntegranteResponseDto
import com.prieto.colab_movil.data.model.PlanificacionResponseDto
import com.prieto.colab_movil.data.model.ProyectoResponseDto

// -----------------------------------------------------------------------------
// Colores tomados del DESIGN.md
// -----------------------------------------------------------------------------
private val AppBackground = Color(0xFFFAF8FF)
private val AppSurface = Color(0xFFFFFFFF)
private val SurfaceLow = Color(0xFFF2F3FF)
private val SurfaceContainer = Color(0xFFEAEDFF)
private val SurfaceHigh = Color(0xFFE2E7FF)
private val SurfaceHighest = Color(0xFFDAE2FD)
private val Primary = Color(0xFF3525CD)
private val PrimaryFixed = Color(0xFFE2DFFF)
private val OnSurface = Color(0xFF131B2E)
private val OnSurfaceVariant = Color(0xFF464555)
private val Outline = Color(0xFF777587)
private val SecondaryContainer = Color(0xFF6CF8BB)
private val OnSecondaryContainer = Color(0xFF00714D)
private val ErrorContainer = Color(0xFFFFDAD6)
private val ErrorText = Color(0xFF93000A)

// -----------------------------------------------------------------------------
// Modelos de UI (derivados de los DTO del backend, sin datos estáticos)
// -----------------------------------------------------------------------------
private enum class TaskStatus(val label: String) {
    PENDING("PENDIENTE"),
    IN_PROGRESS("EN PROGRESO"),
    COMPLETED("COMPLETADA"),
    BLOCKED("BLOQUEADA");

    companion object {
        fun fromBackend(value: String?): TaskStatus = when (value?.trim()?.uppercase()) {
            "EN_PROGRESO", "EN PROGRESO", "IN_PROGRESS" -> IN_PROGRESS
            "COMPLETADA", "COMPLETED", "FINALIZADA" -> COMPLETED
            "BLOQUEADA", "BLOCKED" -> BLOCKED
            else -> PENDING
        }
    }
}

private enum class TaskPriority(val label: String) {
    LOW("BAJA"),
    MEDIUM("MEDIA"),
    HIGH("ALTA");

    companion object {
        fun fromBackend(value: String?): TaskPriority = when (value?.trim()?.uppercase()) {
            "ALTA", "HIGH" -> HIGH
            "BAJA", "LOW" -> LOW
            else -> MEDIUM
        }
    }
}

private data class ProjectTask(
    val id: String,
    val title: String,
    val description: String,
    val startDate: String,
    val durationDays: Int,
    val dependencies: List<String>,
    val assignee: String,
    val initials: String,
    val status: TaskStatus,
    val priority: TaskPriority
) {
    val duration: String get() = "$durationDays días"
}

private data class ProjectPhase(
    val name: String,
    val duration: String,
    val tasks: List<ProjectTask>
)

private val MONTHS = mapOf(
    1 to "Ene", 2 to "Feb", 3 to "Mar", 4 to "Abr", 5 to "May", 6 to "Jun",
    7 to "Jul", 8 to "Ago", 9 to "Sep", 10 to "Oct", 11 to "Nov", 12 to "Dic"
)

private fun taskCode(id: Long) = "TK-$id"

private fun formatDate(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    val parts = iso.split("-")
    if (parts.size != 3) return iso
    val month = parts[1].toIntOrNull()?.let { MONTHS[it] }
    return "${parts[2]} ${month ?: parts[1]}"
}

private fun initialsOf(nombre: String?): String {
    if (nombre.isNullOrBlank()) return "?"
    return nombre.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()
        .ifBlank { "?" }
}

private fun buildPhases(
    planificacion: PlanificacionResponseDto,
    equipo: List<IntegranteResponseDto>
): Pair<List<ProjectPhase>, List<ProjectTask>> {
    val nombrePorEmail = equipo.associate { it.email to it.nombre }
    val allTasks = mutableListOf<ProjectTask>()

    val phases = planificacion.etapas.orEmpty()
        .sortedBy { it.orden }
        .map { etapa ->
            val tareas = etapa.tareas.orEmpty()
            val tasks = tareas.map { t ->
                val assignee = t.responsableEmail
                    ?.let { email -> nombrePorEmail[email] ?: email }
                    ?: "Sin asignar"
                ProjectTask(
                    id = taskCode(t.id),
                    title = t.nombre,
                    description = t.descripcion ?: "Sin descripción.",
                    startDate = "${formatDate(t.fechaInicio)} - ${formatDate(t.fechaFin)}",
                    durationDays = t.duracionDias ?: 1,
                    dependencies = t.dependencias.orEmpty().map(::taskCode),
                    assignee = assignee,
                    initials = initialsOf(assignee),
                    status = TaskStatus.fromBackend(t.estado),
                    priority = TaskPriority.fromBackend(t.prioridad)
                )
            }
            allTasks.addAll(tasks)
            ProjectPhase(
                name = etapa.nombre,
                duration = "${tareas.sumOf { it.duracionDias ?: 0 }} días",
                tasks = tasks
            )
        }

    return phases to allTasks
}

private fun estadoPlanLabel(estado: String?): String = when (estado?.trim()?.uppercase()) {
    "ACTIVA" -> "Planificación activa"
    "FINALIZADA" -> "Planificación finalizada"
    "BORRADOR" -> "Borrador de planificación"
    "PROPUESTA" -> "Propuesta validada por IA y Backend"
    else -> "Planificación generada por IA"
}

// -----------------------------------------------------------------------------
// Pantalla principal
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanificacionScreen(
    proyectoId: Long,
    viewModel: PlanificacionViewModel = viewModel(),
    onBack: () -> Unit = {},
    onValidateRules: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(proyectoId) {
        viewModel.cargar(proyectoId)
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Planificación",
                        color = OnSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = OnSurface
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("P", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is PlanificacionUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Primary
                    )
                }
                is PlanificacionUiState.Generating -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Primary)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Generando planificación con IA…",
                            color = OnSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
                is PlanificacionUiState.Empty -> {
                    EmptyPlanificacion(onGenerar = { viewModel.generar(proyectoId) })
                }
                is PlanificacionUiState.Error -> {
                    ErrorPlanificacion(
                        message = state.message,
                        onRetry = {
                            if (state.fromGeneration) viewModel.generar(proyectoId)
                            else viewModel.cargar(proyectoId)
                        },
                        onBack = onBack
                    )
                }
                is PlanificacionUiState.Success -> {
                    val (phases, allTasks) = buildPhases(state.planificacion, state.equipo)
                    PlanificacionContenido(
                        proyecto = state.proyecto,
                        planificacion = state.planificacion,
                        phases = phases,
                        allTasks = allTasks,
                        onValidateRules = onValidateRules
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPlanificacion(onGenerar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Aún no hay planificación para este proyecto.",
            color = OnSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Genera una propuesta de planificación con IA a partir de la información registrada.",
            color = OnSurfaceVariant,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onGenerar,
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text("Generar planificación con IA")
        }
    }
}

@Composable
private fun ErrorPlanificacion(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "No se pudo completar la operación",
            color = OnSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            color = ErrorText,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text("Reintentar")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) {
            Text("Volver", color = Primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanificacionContenido(
    proyecto: ProyectoResponseDto?,
    planificacion: PlanificacionResponseDto,
    phases: List<ProjectPhase>,
    allTasks: List<ProjectTask>,
    onValidateRules: () -> Unit
) {
    var selectedPhaseIndex by remember { mutableStateOf(0) }
    var selectedFilter by remember { mutableStateOf<TaskStatus?>(null) }
    var selectedAssignee by remember { mutableStateOf("Todos los asignados") }
    var assigneeMenuExpanded by remember { mutableStateOf(false) }

    val phase = phases.getOrNull(selectedPhaseIndex)
    val assignees = listOf("Todos los asignados") + allTasks.map { it.assignee }.distinct()

    val visibleTasks = phase?.tasks.orEmpty().filter { task ->
        val matchesStatus = selectedFilter == null || task.status == selectedFilter
        val matchesAssignee =
            selectedAssignee == "Todos los asignados" || task.assignee == selectedAssignee
        matchesStatus && matchesAssignee
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProjectHeader(proyecto, planificacion, onValidateRules)

        ProjectMetrics(allTasks, phases.size)

        StatusFilters(
            allTasks = allTasks,
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it }
        )

        // Filtro por responsable
        Box {
            Surface(
                onClick = { assigneeMenuExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = AppSurface,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Group, contentDescription = null, tint = Outline)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Responsable: $selectedAssignee",
                        modifier = Modifier.weight(1f),
                        color = OnSurface,
                        fontSize = 13.sp
                    )
                    Icon(Icons.Default.ExpandMore, contentDescription = null)
                }
            }

            DropdownMenu(
                expanded = assigneeMenuExpanded,
                onDismissRequest = { assigneeMenuExpanded = false }
            ) {
                assignees.forEach { name ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            selectedAssignee = name
                            assigneeMenuExpanded = false
                        }
                    )
                }
            }
        }

        // Navegación entre fases
        PhaseSelector(
            phases = phases,
            selectedPhaseIndex = selectedPhaseIndex,
            onPhaseSelected = { selectedPhaseIndex = it }
        )

        if (phase == null) {
            Text(
                text = "No hay etapas disponibles en esta planificación.",
                color = OnSurfaceVariant,
                modifier = Modifier.padding(vertical = 20.dp)
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        phase.name,
                        color = OnSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Fase ${selectedPhaseIndex + 1} de ${phases.size}",
                        color = OnSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                Surface(shape = RoundedCornerShape(50), color = SurfaceHighest) {
                    Text(
                        "${phase.tasks.size} tareas",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = Primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (visibleTasks.isEmpty()) {
                Text(
                    text = "No hay tareas que coincidan con los filtros seleccionados.",
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                visibleTasks.forEach { task ->
                    TaskCard(task)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

// -----------------------------------------------------------------------------
// Cabecera
// -----------------------------------------------------------------------------
@Composable
private fun ProjectHeader(
    proyecto: ProyectoResponseDto?,
    planificacion: PlanificacionResponseDto,
    onValidateRules: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallTag("PROYECTO #${proyecto?.id ?: planificacion.proyectoId}")
                SmallTag("plan-${planificacion.id}")
            }

            TextButton(onClick = onValidateRules) {
                Icon(
                    Icons.Default.Rule,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Validar reglas", color = Primary, fontSize = 11.sp)
            }
        }

        Surface(color = SecondaryContainer, shape = RoundedCornerShape(50)) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = OnSecondaryContainer,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    estadoPlanLabel(planificacion.estado),
                    color = OnSecondaryContainer,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Text(
            "Plan Maestro: ${proyecto?.nombre ?: "Proyecto"}",
            color = OnSurface,
            fontSize = 24.sp,
            lineHeight = 31.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            proyecto?.descripcion ?: "Desglose estructurado en fases de entrega técnica.",
            color = OnSurfaceVariant,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun SmallTag(text: String) {
    Surface(color = SurfaceContainer, shape = RoundedCornerShape(6.dp)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            color = OnSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// -----------------------------------------------------------------------------
// Métricas
// -----------------------------------------------------------------------------
@Composable
private fun ProjectMetrics(allTasks: List<ProjectTask>, phasesCount: Int) {
    val completed = allTasks.count { it.status == TaskStatus.COMPLETED }
    val totalDias = allTasks.sumOf { it.durationDays }
    val metrics = listOf(
        Triple("DURACIÓN TOTAL", "$totalDias Días", Icons.Default.Timer),
        Triple("ETAPAS ACTIVAS", "$phasesCount Fases", Icons.Default.AccountTree),
        Triple("TAREAS TOTALES", "${allTasks.size} Unidades", Icons.Default.Checklist),
        Triple("COMPLETADAS", "$completed de ${allTasks.size}", Icons.Default.CheckCircle)
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        metrics.chunked(2).forEach { rowMetrics ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowMetrics.forEach { (label, value, icon) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = AppSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PrimaryFixed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = Primary)
                            }
                            Spacer(Modifier.width(9.dp))
                            Column {
                                Text(
                                    label,
                                    color = OnSurfaceVariant,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    value,
                                    color = OnSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Filtros
// -----------------------------------------------------------------------------
@Composable
private fun StatusFilters(
    allTasks: List<ProjectTask>,
    selectedFilter: TaskStatus?,
    onFilterSelected: (TaskStatus?) -> Unit
) {
    val filters = listOf(
        "Todos" to null,
        "Pendientes" to TaskStatus.PENDING,
        "En progreso" to TaskStatus.IN_PROGRESS,
        "Completadas" to TaskStatus.COMPLETED,
        "Bloqueadas" to TaskStatus.BLOCKED
    )

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { (label, status) ->
            val count = if (status == null) {
                allTasks.size
            } else {
                allTasks.count { it.status == status }
            }

            FilterChip(
                selected = selectedFilter == status,
                onClick = { onFilterSelected(status) },
                label = { Text("$label ($count)", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Primary,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Selector de fases
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhaseSelector(
    phases: List<ProjectPhase>,
    selectedPhaseIndex: Int,
    onPhaseSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceHigh)
            .horizontalScroll(rememberScrollState())
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        phases.forEachIndexed { index, phase ->
            val selected = selectedPhaseIndex == index

            Surface(
                onClick = { onPhaseSelected(index) },
                modifier = Modifier.width(112.dp),
                shape = RoundedCornerShape(9.dp),
                color = if (selected) AppSurface else Color.Transparent,
                shadowElevation = if (selected) 1.dp else 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Fase ${index + 1}",
                        color = if (selected) Primary else OnSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        phase.duration,
                        color = Outline,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Tarjeta de tarea
// -----------------------------------------------------------------------------
@Composable
private fun TaskCard(task: ProjectTask) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    task.id,
                    color = Outline,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                PriorityBadge(task.priority)
            }

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    task.title,
                    color = OnSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp
                )
                Text(
                    task.description,
                    color = OnSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }

            Surface(color = SurfaceLow, shape = RoundedCornerShape(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 9.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "${task.startDate} • ${task.duration}",
                        color = OnSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (task.dependencies.isEmpty()) "Sin dependencias previas"
                    else "Depende de: ${task.dependencies.joinToString(", ")}",
                    color = if (task.dependencies.isEmpty()) Outline else Primary,
                    fontSize = 11.sp,
                    fontWeight = if (task.dependencies.isEmpty()) FontWeight.Normal else FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(PrimaryFixed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            task.initials,
                            color = Primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        task.assignee,
                        color = OnSurface,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                StatusBadge(task.status)
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Badges
// -----------------------------------------------------------------------------
@Composable
private fun PriorityBadge(priority: TaskPriority) {
    val (background, foreground) = when (priority) {
        TaskPriority.LOW -> SecondaryContainer to OnSecondaryContainer
        TaskPriority.MEDIUM -> SurfaceHigh to OnSurfaceVariant
        TaskPriority.HIGH -> ErrorContainer to ErrorText
    }

    Surface(color = background, shape = RoundedCornerShape(50)) {
        Text(
            priority.label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            color = foreground,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StatusBadge(status: TaskStatus) {
    val (background, foreground) = when (status) {
        TaskStatus.COMPLETED -> SecondaryContainer to OnSecondaryContainer
        TaskStatus.IN_PROGRESS -> PrimaryFixed to Primary
        TaskStatus.PENDING -> SurfaceHigh to OnSurfaceVariant
        TaskStatus.BLOCKED -> ErrorContainer to ErrorText
    }

    Surface(color = background, shape = RoundedCornerShape(50)) {
        Text(
            status.label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            color = foreground,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
