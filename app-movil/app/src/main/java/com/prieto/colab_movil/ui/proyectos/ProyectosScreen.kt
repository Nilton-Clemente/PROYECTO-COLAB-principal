package com.prieto.colab_movil.ui.proyectos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.prieto.colab_movil.data.model.EstadoProyecto
import com.prieto.colab_movil.data.model.estadoProyecto
import com.prieto.colab_movil.data.model.ProyectoResponseDto
import com.prieto.colab_movil.ui.components.ColabTopBar
import com.prieto.colab_movil.ui.components.ColabActualizar
import com.prieto.colab_movil.ui.components.ColabBottomBar
import com.prieto.colab_movil.ui.components.SeccionPrincipal

/* ───────────── Colores (tomados del DESIGN.md) ───────────── */
private val Surface = Color(0xFFFAF8FF)
private val SurfaceLowest = Color(0xFFFFFFFF)
private val SurfaceContainer = Color(0xFFEAEDFF)
private val SurfaceDim = Color(0xFFD2D9F4)
private val OnSurface = Color(0xFF131B2E)
private val OnSurfaceVariant = Color(0xFF464555)
private val Primary = Color(0xFF3525CD)
private val Secondary = Color(0xFF006C49)
private val OnErrorContainer = Color(0xFF93000A)

/* ───────────── Modelos ───────────── */
enum class Filtro(val estado: EstadoProyecto? = null) {
    TODOS, SIN_PLANIFICACION(EstadoProyecto.SIN_PLANIFICACION), PROPUESTA(EstadoProyecto.PLANIFICACION_PROPUESTA),
    EN_DESARROLLO(EstadoProyecto.EN_DESARROLLO), FINALIZADOS(EstadoProyecto.FINALIZADO), CANCELADOS(EstadoProyecto.CANCELADO);

    val label: String get() = estado?.titulo ?: "Todos"
    fun coincide(valor: EstadoProyecto) = estado == null || estado == valor
}

data class Tecnologia(val nombre: String, val icono: ImageVector)

data class Proyecto(
    val id: Long = 0L,
    val codigo: String,
    val titulo: String,
    val estado: EstadoProyecto,
    val descripcion: String?,
    val tecnologias: List<Tecnologia>,
    val fechaEntrega: String?
)

private fun mapToProyecto(dto: ProyectoResponseDto): Proyecto {
    val codigo = "TEC-${dto.id}"
    val tecnologias = dto.tecnologias?.map { t ->
        Tecnologia(
            nombre = t.nombre,
            icono = when {
                t.nombre.contains("Java", true) -> Icons.Outlined.Terminal
                t.nombre.contains("Spring", true) -> Icons.Outlined.Eco
                t.nombre.contains("React", true) -> Icons.Outlined.Code
                t.nombre.contains("Python", true) -> Icons.Outlined.Memory
                t.nombre.contains("Flutter", true) -> Icons.Outlined.Smartphone
                t.nombre.contains("Docker", true) -> Icons.Outlined.Layers
                else -> Icons.Outlined.Code
            }
        )
    } ?: emptyList()

    return Proyecto(
        id = dto.id,
        codigo = codigo,
        titulo = dto.nombre,
        estado = dto.estadoProyecto,
        descripcion = dto.descripcion?.takeIf { it.isNotBlank() },
        tecnologias = tecnologias,
        fechaEntrega = dto.fechaEntrega
    )
}

/* ───────────── Pantalla principal dinámicamente conectada ───────────── */
@Composable
fun ProyectosScreen(
    viewModel: ProyectosViewModel = viewModel(),
    onCrearClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    onProyectoClick: (Proyecto) -> Unit = {},
    onInvitacionesClick: () -> Unit = {},
    proyectoEliminado: Long? = null,
    onEliminacionConsumida: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(proyectoEliminado) {
        proyectoEliminado?.let {
            viewModel.retirarEliminado(it)
            onEliminacionConsumida()
        }
    }
    DisposableEffect(lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) viewModel.cargarDatos()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    when (val state = uiState) {
        is ProyectosUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Surface),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary)
            }
        }
        is ProyectosUiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Surface)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = state.message, color = OnSurfaceVariant, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.cargarDatos() }) {
                    Text("Reintentar")
                }
            }
        }
        is ProyectosUiState.Success -> {
            val pendientes = state.invitaciones.map { it.proyectoId }.toSet()
            val proyectos = state.proyectos.filterNot { it.id == proyectoEliminado || it.id in pendientes }.map(::mapToProyecto)
            val hayInvitaciones = state.invitaciones.isNotEmpty()
            ProyectosContent(
                nombre = state.nombre,
                proyectos = proyectos,
                onCrearClick = onCrearClick,
                onNotificacionesClick = onNotificacionesClick,
                onPerfilClick = onPerfilClick,
                onProyectoClick = onProyectoClick,
                onInvitacionesClick = onInvitacionesClick,
                hayInvitaciones = hayInvitaciones,
                actualizando = state.actualizando,
                aviso = state.aviso,
                onActualizar = { viewModel.cargarDatos() }
            )
        }
    }
}

@Composable
fun ProyectosContent(
    nombre: String = "Alonso",
    proyectos: List<Proyecto> = emptyList(),
    onCrearClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    onProyectoClick: (Proyecto) -> Unit = {},
    onInvitacionesClick: () -> Unit = {},
    hayInvitaciones: Boolean = false,
    actualizando: Boolean = false,
    aviso: String? = null,
    onActualizar: () -> Unit = {}
) {
    var filtro by rememberSaveable { mutableStateOf(Filtro.TODOS) }

    val visibles = proyectos.filter { filtro.coincide(it.estado) }
    val conteos = Filtro.entries.associateWith { f -> proyectos.count { f.coincide(it.estado) } }

    Scaffold(
        containerColor = Surface,
        topBar = { ColabTopBar(onNotificacionesClick, onPerfilClick, colorIconoNotificaciones = OnSurfaceVariant) },
        bottomBar = {
            ColabBottomBar(SeccionPrincipal.PROYECTOS, onProyectos = {},
                onInvitaciones = onInvitacionesClick, onPerfil = onPerfilClick,
                hayInvitaciones = hayInvitaciones, colorNoSeleccionado = OnSurfaceVariant)
        }
    ) { padding ->
        ColabActualizar(actualizando = actualizando, habilitado = !actualizando, onActualizar = onActualizar, modifier = Modifier.padding(padding)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (aviso != null) item { Text(aviso, color = OnErrorContainer, fontSize = 13.sp) }
            item { Saludo(nombre, proyectos.size, onCrearClick) }
            item {
                FiltrosRow(filtro, conteos) { filtro = it }
            }
            if (visibles.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (filtro == Filtro.TODOS) "No hay proyectos disponibles" else "No hay proyectos con este estado", color = OnSurfaceVariant, fontSize = 14.sp)
                    }
                }
            } else {
                items(visibles, key = { it.codigo }) { p ->
                    ProyectoCard(p, onClick = { onProyectoClick(p) })
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        }
    }
}

/* ───────────── Cabecera y filtros ───────────── */
@Composable
private fun Saludo(nombre: String, total: Int, onCrear: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Hola, $nombre 👋",
                fontSize = 24.sp, lineHeight = 32.sp,
                fontWeight = FontWeight.Bold, color = OnSurface
            )
            Text(
                text = buildAnnotatedString {
                    append("Tienes ")
                    withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.SemiBold)) {
                        append("$total proyectos")
                    }
                    append(" vinculados a este ciclo")
                },
                fontSize = 12.sp, color = OnSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(Primary)
                .clickable(onClick = onCrear)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Outlined.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Text("Crear", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FiltrosRow(seleccionado: Filtro, conteos: Map<Filtro, Int>, onSelect: (Filtro) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(Filtro.entries) { f ->
            val activo = f == seleccionado
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (activo) Primary else SurfaceContainer)
                    .clickable { onSelect(f) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    f.label, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    color = if (activo) Color.White else OnSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (activo) Color.White.copy(alpha = 0.2f) else SurfaceDim)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        "${conteos[f] ?: 0}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = if (activo) Color.White else OnSurfaceVariant
                    )
                }
            }
        }
    }
}

/* ───────────── Tarjeta de proyecto ───────────── */
@Composable
private fun ProyectoCard(p: Proyecto, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceLowest)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        p.codigo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace, color = OnSurfaceVariant
                    )
                    EstadoProyectoBadge(p.estado)
                }
                Text(
                    p.titulo, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold,
                    color = OnSurface, maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                p.descripcion?.let { Text(it, color = OnSurfaceVariant, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            p.tecnologias.forEach { TechChip(it) }
        }

        FechaEntrega(p.fechaEntrega)
    }
}

@Composable
private fun TechChip(t: Tecnologia) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(SurfaceContainer)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(t.icono, null, tint = Secondary, modifier = Modifier.size(14.dp))
        Text(t.nombre, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = OnSurfaceVariant)
    }
}

@Composable
private fun FechaEntrega(fecha: String?) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceContainer)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Outlined.Event, null, tint = Primary, modifier = Modifier.size(16.dp))
        Text("Entrega: ${fecha ?: "Por definir"}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
    }
}




