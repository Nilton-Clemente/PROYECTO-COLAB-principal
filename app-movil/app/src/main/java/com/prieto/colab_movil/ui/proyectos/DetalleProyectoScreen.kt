package com.prieto.colab_movil.ui.proyectos

import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.prieto.colab_movil.ui.components.ColabBarraSecundaria
import com.prieto.colab_movil.ui.components.ColabActualizar
import com.prieto.colab_movil.data.model.IntegranteResponseDto
import com.prieto.colab_movil.data.model.ProyectoResponseDto
import com.prieto.colab_movil.data.model.EstadoProyecto
import com.prieto.colab_movil.data.model.estadoProyecto

/* ───────────── Colores (DESIGN.md) ───────────── */
private val Surface = Color(0xFFFAF8FF)
private val SurfaceLowest = Color(0xFFFFFFFF)
private val SurfaceLow = Color(0xFFF2F3FF)
private val SurfaceContainer = Color(0xFFEAEDFF)
private val SurfaceHighest = Color(0xFFDAE2FD)
private val OnSurface = Color(0xFF131B2E)
private val OnSurfaceVariant = Color(0xFF464555)
private val Outline = Color(0xFF777587)
private val Primary = Color(0xFF3525CD)
private val PrimaryFixed = Color(0xFFE2DFFF)
private val Secondary = Color(0xFF006C49)
private val SecondaryContainer = Color(0xFF6CF8BB)
private val OnSecondaryContainer = Color(0xFF00714D)
private val ErrorContainer = Color(0xFFFFDAD6)
private val OnErrorContainer = Color(0xFF93000A)

/* ───────────── Enum y Clases de Soporte UI ───────────── */
enum class Pestana(val titulo: String) { INFO("Información"), EQUIPO("Equipo") }

enum class Etiqueta(val texto: String, val fondo: Color, val fg: Color) {
    ALTA("Alta Prioridad", ErrorContainer, OnErrorContainer),
    MEDIA("Media Prioridad", SecondaryContainer, OnSecondaryContainer),
    RENDIMIENTO("Rendimiento", ErrorContainer, OnErrorContainer),
    SEGURIDAD("Seguridad", SurfaceHighest, OnSurface)
}

data class Requerimiento(val codigo: String, val titulo: String, val detalle: String, val etiqueta: Etiqueta)
data class Tech(val nombre: String, val destacado: Boolean = false, val neutro: Boolean = false)
data class Miembro(val iniciales: String, val nombre: String, val rol: String, val tag: String)

@Composable
fun DetalleProyectoScreen(
    proyectoId: Long = 1L,
    viewModel: DetalleProyectoViewModel = viewModel(),
    onBack: () -> Unit = {},
    onEditar: () -> Unit = {},
    onGenerarPlanificacion: () -> Unit = {},
    onMisTareas: () -> Unit = {},
    onEliminado: (Long) -> Unit = {},
    onMetricas: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val detalle = uiState as? DetalleProyectoUiState.Success
    val ocupado = detalle?.accionEnCurso != null || detalle?.enviandoInvitacion == true
    var confirmando by rememberSaveable(proyectoId) { mutableStateOf<AccionProyecto?>(null) }

    DisposableEffect(lifecycle, proyectoId, viewModel) {
        val observer = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) viewModel.cargarDetalle(proyectoId)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val eliminado = uiState as? DetalleProyectoUiState.Eliminado
    LaunchedEffect(eliminado) { eliminado?.let { onEliminado(it.proyectoId) } }
    BackHandler(enabled = ocupado) { }

    Scaffold(
        containerColor = Surface,
        topBar = {
            ColabBarraSecundaria("Detalle de proyecto", onVolver = onBack, habilitado = !ocupado, descripcionVolver = "Regresar") {
                if (detalle != null) MenuProyecto(detalle.acciones, detalle.puedeModificar) { accion ->
                    if (accion == AccionProyecto.EDITAR) onEditar() else confirmando = accion
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is DetalleProyectoUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Primary
                    )
                }
                is DetalleProyectoUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            color = OnErrorContainer,
                            textAlign = TextAlign.Center,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.cargarDetalle(proyectoId) },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
                is DetalleProyectoUiState.Success -> {
                    ColabActualizar(actualizando = state.actualizando, habilitado = !state.actualizando && !ocupado,
                        onActualizar = { viewModel.cargarDetalle(proyectoId) }) {
                    DetalleProyectoContenido(
                        proyectoDto = state.proyecto,
                        equipoDto = state.equipo,
                        tienePlanificacion = state.tienePlanificacion,
                        onGenerarPlanificacion = onGenerarPlanificacion,
                        onMisTareas = onMisTareas,
                        onMetricas = onMetricas,
                        habilitarAcciones = state.puedeModificar,
                        puedeGenerar = state.soyCreador && state.proyecto.estadoProyecto == EstadoProyecto.SIN_PLANIFICACION,
                        ocupado = ocupado,
                        aviso = state.aviso,
                        onInvitar = { email -> viewModel.invitarIntegrante(proyectoId, email) }
                    )
                    }
                }
                is DetalleProyectoUiState.Eliminado -> Unit
            }
        }
    }
    confirmando?.takeIf { detalle?.puedeModificar == true && it in detalle.acciones }?.let { accion ->
        ConfirmarCicloProyecto(proyectoId, detalle!!.proyecto.nombre, accion, onCerrar = { confirmando = null }) { texto ->
            confirmando = null
            viewModel.cambiarCiclo(proyectoId, accion, texto)
        }
    }
}

@Composable
private fun DetalleProyectoContenido(
    proyectoDto: ProyectoResponseDto,
    equipoDto: List<IntegranteResponseDto>,
    tienePlanificacion: Boolean,
    onGenerarPlanificacion: () -> Unit,
    onMisTareas: () -> Unit,
    onMetricas: () -> Unit,
    habilitarAcciones: Boolean,
    puedeGenerar: Boolean,
    ocupado: Boolean,
    aviso: String?,
    onInvitar: (String) -> Unit
) {
    var pestana by remember { mutableStateOf(Pestana.INFO) }

    // Mapeo dinámico
    val codigo = "#TEC-${proyectoDto.id}"
    val titulo = proyectoDto.nombre
    val descripcion = proyectoDto.descripcion ?: "Sin descripción registrada."
    val problematica = proyectoDto.problematica ?: "No se registró una problemática específica."
    val objetivoGeneral = proyectoDto.objetivoGeneral ?: "No se especificó un objetivo general."
    val objetivosEspecificos = proyectoDto.objetivosEspecificos
        ?.split("\n")
        ?.map { it.trim('-').trim() }
        ?.filter { it.isNotBlank() }
        .orEmpty()

    val stack = proyectoDto.tecnologias?.map { t ->
        Tech(
            nombre = t.nombre,
            destacado = t.nombre.contains("Spring", true) || t.nombre.contains("React", true),
            neutro = t.nombre.contains("Docker", true)
        )
    } ?: emptyList()

    val requerimientos = mutableListOf<Requerimiento>()
    proyectoDto.requerimientosFuncionales?.forEachIndexed { i, rf ->
        requerimientos.add(
            Requerimiento(
                codigo = rf.codigo ?: "RF-0${i + 1}",
                titulo = "Requerimiento Funcional",
                detalle = rf.descripcion,
                etiqueta = if (i == 0) Etiqueta.ALTA else Etiqueta.MEDIA
            )
        )
    }
    proyectoDto.requerimientosNoFuncionales?.forEachIndexed { i, rnf ->
        requerimientos.add(
            Requerimiento(
                codigo = rnf.codigo ?: "RNF-0${i + 1}",
                titulo = rnf.categoria ?: "Calidad y Rendimiento",
                detalle = rnf.descripcion,
                etiqueta = Etiqueta.RENDIMIENTO
            )
        )
    }

    val equipo = equipoDto.map { m ->
        val iniciales = m.nombre.split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2)
            .joinToString("")
            .uppercase()
        val rol = m.rol ?: "Desarrollador"
        val isLead = rol.contains("Lead", true) || rol.contains("Scrum", true)
        Miembro(
            iniciales = if (iniciales.isNotBlank()) iniciales else "US",
            nombre = m.nombre,
            rol = rol,
            tag = if (isLead) "Lead" else "Miembro"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (aviso != null) Text(aviso, color = OnErrorContainer, fontSize = 13.sp)
        if (ocupado) LinearProgressIndicator(Modifier.fillMaxWidth())
        val estado = proyectoDto.estadoProyecto
        Cabecera(codigo, titulo, descripcion, estado, tienePlanificacion, puedeGenerar, onGenerarPlanificacion, habilitarAcciones)
        if (estado.cerrado) Text("Proyecto en modo de solo lectura. Puedes consultar la planificación, las tareas y el historial.", color = OnSurfaceVariant)
        if (tienePlanificacion) OutlinedButton(
            onClick = onMisTareas,
            enabled = habilitarAcciones,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Outlined.Checklist, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (estado.cerrado) "Mis tareas e historial" else "Mis tareas y confirmaciones", fontWeight = FontWeight.SemiBold)
        }
        OutlinedButton(
            onClick = onMetricas,
            enabled = !ocupado,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Outlined.BarChart, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Ver métricas", fontWeight = FontWeight.SemiBold)
        }
        Tabs(pestana, equipo.size) { pestana = it }
        ResumenCard(proyectoDto)

        when (pestana) {
            Pestana.INFO -> InfoTab(problematica, objetivoGeneral, objetivosEspecificos, stack, requerimientos)
            Pestana.EQUIPO -> EquipoProyectoContent(
                equipoDto = equipoDto,
                puedeInvitar = puedeGenerar && habilitarAcciones && !tienePlanificacion,
                onInvitar = onInvitar
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

/* ───────────── Cabecera, tabs y resumen ───────────── */
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun Cabecera(codigo: String, titulo: String, descripcion: String, estado: EstadoProyecto, tienePlanificacion: Boolean, puedeGenerar: Boolean, onGenerarPlanificacion: () -> Unit, habilitarAcciones: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(codigo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, color = OnSurfaceVariant)
            EstadoProyectoBadge(estado)
        }
        Text(titulo, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, color = OnSurface)
        Text(descripcion, fontSize = 13.sp, lineHeight = 19.sp, color = OnSurfaceVariant)
        if (tienePlanificacion || puedeGenerar) Button(
            onClick = onGenerarPlanificacion,
            enabled = habilitarAcciones,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Icon(
                imageVector = if (tienePlanificacion) Icons.Outlined.Visibility else Icons.Outlined.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (tienePlanificacion) "Ver planificación" else "Generar planificación con IA",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun Tabs(seleccionada: Pestana, miembros: Int, onSelect: (Pestana) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainer)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Pestana.entries.forEach { t ->
            val activa = t == seleccionada
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (activa) SurfaceLowest else Color.Transparent)
                    .clickable { onSelect(t) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (t == Pestana.EQUIPO) "${t.titulo} ($miembros)" else t.titulo,
                    fontSize = 14.sp, fontWeight = if (activa) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (activa) Primary else OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun Tarjeta(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceLowest)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) { content() }
}

@Composable
private fun TituloSeccion(icono: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icono, null, tint = Primary, modifier = Modifier.size(22.dp))
        Text(texto, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
    }
}

@Composable
private fun ResumenCard(proyecto: ProyectoResponseDto) {
    Tarjeta {
        TituloSeccion(Icons.Outlined.Event, "Fechas del proyecto")
        Text("Entrega: ${proyecto.fechaEntrega ?: "Por definir"}", color = OnSurfaceVariant)
        when (proyecto.estadoProyecto) {
            EstadoProyecto.FINALIZADO -> proyecto.fechaFinalizacion?.let { Text("Finalización: ${it.substringBefore('T')}", color = OnSurfaceVariant) }
            EstadoProyecto.CANCELADO -> proyecto.fechaCancelacion?.let { Text("Cancelación: ${it.substringBefore('T')}", color = OnSurfaceVariant) }
            else -> Unit
        }
    }
}

/* ───────────── Pestaña Información ───────────── */
@Composable
private fun InfoTab(
    problematica: String,
    objetivoGeneral: String,
    objetivosEspecificos: List<String>,
    stack: List<Tech>,
    requerimientos: List<Requerimiento>
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ProblematicaCard(problematica)
        ObjetivosCard(objetivoGeneral, objetivosEspecificos)
        if (stack.isNotEmpty()) {
            StackCard(stack)
        }
        if (requerimientos.isNotEmpty()) {
            RequerimientosCard(requerimientos)
        }
    }
}

@Composable
private fun ProblematicaCard(texto: String) {
    var abierto by remember { mutableStateOf(true) }
    Tarjeta {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { abierto = !abierto },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TituloSeccion(Icons.Outlined.Assignment, "Resumen y Problemática")
            Icon(if (abierto) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = OnSurface)
        }
        AnimatedVisibility(visible = abierto) {
            Text(texto, fontSize = 15.sp, lineHeight = 22.sp, color = OnSurface)
        }
    }
}

@Composable
private fun ObjetivosCard(general: String, especificos: List<String>) {
    Tarjeta {
        TituloSeccion(Icons.Outlined.Flag, "Objetivos del Proyecto")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceLow)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("OBJETIVO GENERAL", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Primary)
            Text(general, fontSize = 15.sp, lineHeight = 22.sp, color = OnSurface)
        }
        if (especificos.isNotEmpty()) {
            Text("OBJETIVOS ESPECÍFICOS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
            especificos.forEachIndexed { i, texto ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier.size(22.dp).clip(CircleShape).background(PrimaryFixed),
                        contentAlignment = Alignment.Center
                    ) { Text("${i + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Primary) }
                    Text(texto, fontSize = 13.sp, lineHeight = 19.sp, color = OnSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StackCard(stack: List<Tech>) {
    Tarjeta {
        TituloSeccion(Icons.Outlined.Layers, "Stack Tecnológico")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            stack.forEach { t ->
                val fondo = if (t.destacado) SecondaryContainer else SurfaceContainer
                val fg = if (t.destacado) OnSecondaryContainer else OnSurfaceVariant
                val punto = when {
                    t.destacado -> Secondary
                    t.neutro -> Outline
                    else -> Primary
                }
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(fondo)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(punto))
                    Text(t.nombre, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = fg)
                }
            }
        }
    }
}

@Composable
private fun RequerimientosCard(reqs: List<Requerimiento>) {
    Tarjeta {
        TituloSeccion(Icons.Outlined.Checklist, "Requerimientos Clave")
        reqs.forEach { r ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceLow)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(r.codigo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, color = Primary)
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(r.etiqueta.fondo)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(r.etiqueta.texto, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = r.etiqueta.fg)
                    }
                }
                Text(r.titulo, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                Text(r.detalle, fontSize = 13.sp, lineHeight = 18.sp, color = OnSurfaceVariant)
            }
        }
    }
}

/* ───────────── Pestaña Equipo ───────────── */
@Composable
private fun EquipoTab(equipo: List<Miembro>) {
    Tarjeta {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Integrantes del Proyecto", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
            Text("${equipo.size} miembros", fontSize = 12.sp, color = OnSurfaceVariant)
        }
        if (equipo.isEmpty()) {
            Text("No hay integrantes registrados en el equipo aún.", fontSize = 13.sp, color = OnSurfaceVariant)
        } else {
            equipo.forEach { m ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceLow)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(PrimaryFixed),
                            contentAlignment = Alignment.Center
                        ) { Text(m.iniciales, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Primary) }
                        Column {
                            Text(m.nombre, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                            Text(m.rol, fontSize = 12.sp, color = OnSurfaceVariant)
                        }
                    }
                    val lead = m.tag == "Lead"
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (lead) SecondaryContainer else SurfaceHighest)
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            m.tag, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = if (lead) OnSecondaryContainer else OnSurface
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DetalleProyectoPreview() {
    DetalleProyectoScreen()
}
