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
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.filled.Star as StarFilled
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prieto.colab_movil.data.model.ProyectoResponseDto

/* ───────────── Colores (tomados del DESIGN.md) ───────────── */
private val Surface = Color(0xFFFAF8FF)
private val SurfaceLowest = Color(0xFFFFFFFF)
private val SurfaceLow = Color(0xFFF2F3FF)
private val SurfaceContainer = Color(0xFFEAEDFF)
private val SurfaceHigh = Color(0xFFE2E7FF)
private val SurfaceDim = Color(0xFFD2D9F4)
private val OnSurface = Color(0xFF131B2E)
private val OnSurfaceVariant = Color(0xFF464555)
private val Primary = Color(0xFF3525CD)
private val PrimaryContainer = Color(0xFF4F46E5)
private val PrimaryFixed = Color(0xFFE2DFFF)
private val Secondary = Color(0xFF006C49)
private val SecondaryFixed = Color(0xFF6FFBBE)
private val OnSecondaryFixed = Color(0xFF002113)
private val OnSecondaryFixedVariant = Color(0xFF005236)
private val Tertiary = Color(0xFF960014)
private val ErrorContainer = Color(0xFFFFDAD6)
private val OnErrorContainer = Color(0xFF93000A)

/* ───────────── Modelos ───────────── */
enum class EstadoProyecto { EN_CURSO, PLANIFICACION, FINALIZADO }

enum class Filtro(val label: String) {
    TODOS("Todos"), EN_PROGRESO("En progreso"), FINALIZADOS("Finalizados")
}

data class Tecnologia(val nombre: String, val icono: ImageVector)

sealed interface Insignia {
    data class Fecha(val texto: String) : Insignia
    data class Sprint(val texto: String) : Insignia
    data class Nota(val texto: String) : Insignia
}

data class Proyecto(
    val id: Long = 0L,
    val codigo: String,
    val titulo: String,
    val estado: EstadoProyecto,
    val etapaTexto: String,
    val etapaIcono: ImageVector,
    val progreso: Float,
    val tecnologias: List<Tecnologia>,
    val iniciales: List<String>,
    val miembros: Int,
    val insignia: Insignia
)

private fun mapToProyecto(dto: ProyectoResponseDto): Proyecto {
    val codigo = "TEC-${dto.id}"
    val estado = EstadoProyecto.EN_CURSO
    val etapaTexto = dto.descripcion?.takeIf { it.isNotBlank() } ?: "En desarrollo"
    val etapaIcono = Icons.Outlined.Flag
    val progreso = 0.75f
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

    val iniciales = listOf("AL", "TC")
    val miembros = 2
    val insignia = Insignia.Fecha(dto.fechaEntrega ?: "Por definir")

    return Proyecto(
        id = dto.id,
        codigo = codigo,
        titulo = dto.nombre,
        estado = estado,
        etapaTexto = etapaTexto,
        etapaIcono = etapaIcono,
        progreso = progreso,
        tecnologias = tecnologias,
        iniciales = iniciales,
        miembros = miembros,
        insignia = insignia
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
    onOpcionesClick: (Proyecto) -> Unit = {},
    onInvitacionesClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.cargarDatos()
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
            val proyectos = state.proyectos.map { mapToProyecto(it) }
            val hayInvitaciones = state.invitaciones.isNotEmpty()
            ProyectosContent(
                nombre = state.nombre,
                proyectos = proyectos,
                onCrearClick = onCrearClick,
                onNotificacionesClick = onNotificacionesClick,
                onPerfilClick = onPerfilClick,
                onProyectoClick = onProyectoClick,
                onOpcionesClick = onOpcionesClick,
                onInvitacionesClick = onInvitacionesClick,
                hayInvitaciones = hayInvitaciones
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
    onOpcionesClick: (Proyecto) -> Unit = {},
    onInvitacionesClick: () -> Unit = {},
    hayInvitaciones: Boolean = false
) {
    var filtro by remember { mutableStateOf(Filtro.TODOS) }

    val visibles = when (filtro) {
        Filtro.TODOS -> proyectos
        Filtro.EN_PROGRESO -> proyectos.filter { it.estado != EstadoProyecto.FINALIZADO }
        Filtro.FINALIZADOS -> proyectos.filter { it.estado == EstadoProyecto.FINALIZADO }
    }
    val conteos = mapOf(
        Filtro.TODOS to proyectos.size,
        Filtro.EN_PROGRESO to proyectos.count { it.estado != EstadoProyecto.FINALIZADO },
        Filtro.FINALIZADOS to proyectos.count { it.estado == EstadoProyecto.FINALIZADO }
    )

    Scaffold(
        containerColor = Surface,
        topBar = { ColabTopBar(onNotificacionesClick, onPerfilClick) },
        bottomBar = { ColabBottomBar(hayInvitaciones, onInvitacionesClick, onPerfilClick) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Saludo(nombre, proyectos.size, onCrearClick) }
            item { Metricas(sprintsActivos = 2, entregasPendientes = 1) }
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
                        Text("No hay proyectos disponibles", color = OnSurfaceVariant, fontSize = 14.sp)
                    }
                }
            } else {
                items(visibles, key = { it.codigo }) { p ->
                    ProyectoCard(p, onClick = { onProyectoClick(p) }, onOpciones = { onOpcionesClick(p) })
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

/* ───────────── Barras ───────────── */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColabTopBar(onNotificaciones: () -> Unit, onPerfil: () -> Unit) {
    TopAppBar(
        title = {
            Text("Colab", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = OnSurface)
        },
        actions = {
            IconButton(onClick = onNotificaciones) {
                Icon(Icons.Outlined.Notifications, "Notificaciones", tint = OnSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Primary)
                    .clickable(onClick = onPerfil),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Person, "Mi perfil", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
    )
}

@Composable
private fun ColabBottomBar(hayInvitaciones: Boolean, onInvitaciones: () -> Unit, onPerfil: () -> Unit) {
    NavigationBar(containerColor = SurfaceLowest) {
        val colores = NavigationBarItemDefaults.colors(
            selectedIconColor = Primary,
            selectedTextColor = Primary,
            indicatorColor = Color.Transparent,
            unselectedIconColor = OnSurfaceVariant,
            unselectedTextColor = OnSurfaceVariant
        )
        NavigationBarItem(
            selected = true, onClick = {}, colors = colores,
            icon = { Icon(Icons.Outlined.Folder, null) },
            label = { Text("Proyectos", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
        )
        NavigationBarItem(
            selected = false, onClick = onInvitaciones, colors = colores,
            icon = {
                BadgedBox(badge = {
                    if (hayInvitaciones) Badge(containerColor = Tertiary)
                }) { Icon(Icons.Outlined.Mail, null) }
            },
            label = { Text("Invitaciones", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
        )
        NavigationBarItem(
            selected = false, onClick = onPerfil, colors = colores,
            icon = { Icon(Icons.Outlined.Person, null) },
            label = { Text("Mi Perfil", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
        )
    }
}

/* ───────────── Cabecera, métricas y filtros ───────────── */
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
private fun Metricas(sprintsActivos: Int, entregasPendientes: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainer)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricaItem("Sprints Activos", sprintsActivos, Secondary)
        MetricaItem("Entregas Pend.", entregasPendientes, Primary)
    }
}

@Composable
private fun MetricaItem(titulo: String, valor: Int, punto: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceLowest)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(titulo, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(punto))
            Text("$valor", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnSurface)
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
private fun ProyectoCard(p: Proyecto, onClick: () -> Unit, onOpciones: () -> Unit) {
    val finalizado = p.estado == EstadoProyecto.FINALIZADO
    val colorProgreso = if (finalizado) Secondary else Primary

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
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        p.codigo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace, color = OnSurfaceVariant
                    )
                    EstadoBadge(p.estado)
                }
                Text(
                    p.titulo, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold,
                    color = OnSurface, maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onOpciones, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.MoreVert, "Opciones de proyecto", tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceLow)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                if (finalizado) "100%" else "${(p.progreso * 100).toInt()}% completado",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorProgreso,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
            BarraProgreso(p.progreso, colorProgreso)
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            p.tecnologias.forEach { TechChip(it) }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AvatarStack(p.iniciales)
                Text("${p.miembros} miembros", fontSize = 13.sp, color = OnSurfaceVariant)
            }
            InsigniaPie(p.insignia)
        }
    }
}

@Composable
private fun BarraProgreso(progreso: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(SurfaceHigh)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progreso.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
private fun EstadoBadge(estado: EstadoProyecto) {
    val (texto, fondo, fg) = when (estado) {
        EstadoProyecto.EN_CURSO -> Triple("En curso", SecondaryFixed, OnSecondaryFixedVariant)
        EstadoProyecto.PLANIFICACION -> Triple("Planificación", PrimaryFixed, Primary)
        EstadoProyecto.FINALIZADO -> Triple("Finalizado", SecondaryFixed, OnSecondaryFixedVariant)
    }
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(fondo)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (estado == EstadoProyecto.FINALIZADO) {
            Icon(Icons.Outlined.Verified, null, tint = fg, modifier = Modifier.size(14.dp))
        } else {
            Box(Modifier.size(6.dp).clip(CircleShape).background(if (estado == EstadoProyecto.EN_CURSO) Secondary else Primary))
        }
        Text(texto, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg)
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
private fun AvatarStack(iniciales: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
        iniciales.forEachIndexed { i, texto ->
            val (bg, fg) = when {
                texto.startsWith("+") -> PrimaryFixed to Primary
                texto == "AL" -> SecondaryFixed to OnSecondaryFixed
                else -> listOf(
                    Color(0xFFC3C0FF) to Primary,
                    Color(0xFFB8E6D4) to Secondary,
                    Color(0xFFDAE2FD) to OnSurface
                )[i % 3]
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SurfaceLowest)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Text(texto, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
            }
        }
    }
}

@Composable
private fun InsigniaPie(insignia: Insignia) {
    when (insignia) {
        is Insignia.Fecha -> PieChip(Icons.Outlined.Event, insignia.texto, ErrorContainer, OnErrorContainer)
        is Insignia.Sprint -> PieChip(Icons.Outlined.Schedule, insignia.texto, SurfaceHigh, OnSurfaceVariant)
        is Insignia.Nota -> PieChip(Icons.Filled.StarFilled, insignia.texto, SecondaryFixed, OnSecondaryFixed, bold = true)
    }
}

@Composable
private fun PieChip(icono: ImageVector, texto: String, fondo: Color, fg: Color, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(fondo)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icono, null, tint = fg, modifier = Modifier.size(16.dp))
        Text(texto, fontSize = 13.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold, color = fg)
    }
}




