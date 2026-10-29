package com.prieto.colab_movil.ui.proyectos

import androidx.compose.animation.AnimatedVisibility
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
import com.prieto.colab_movil.data.model.IntegranteResponseDto
import com.prieto.colab_movil.data.model.ProyectoResponseDto

/* ───────────── Colores (DESIGN.md) ───────────── */
private val Surface = Color(0xFFFAF8FF)
private val SurfaceLowest = Color(0xFFFFFFFF)
private val SurfaceLow = Color(0xFFF2F3FF)
private val SurfaceContainer = Color(0xFFEAEDFF)
private val SurfaceHigh = Color(0xFFE2E7FF)
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
enum class Pestana(val titulo: String) { INFO("Información"), EQUIPO("Equipo"), HITOS("Hitos") }

enum class Etiqueta(val texto: String, val fondo: Color, val fg: Color) {
    ALTA("Alta Prioridad", ErrorContainer, OnErrorContainer),
    MEDIA("Media Prioridad", SecondaryContainer, OnSecondaryContainer),
    RENDIMIENTO("Rendimiento", ErrorContainer, OnErrorContainer),
    SEGURIDAD("Seguridad", SurfaceHighest, OnSurface)
}

data class Requerimiento(val codigo: String, val titulo: String, val detalle: String, val etiqueta: Etiqueta)
data class Tech(val nombre: String, val destacado: Boolean = false, val neutro: Boolean = false)
data class Miembro(val iniciales: String, val nombre: String, val rol: String, val tag: String)

enum class EstadoHito { COMPLETADO, EN_REVISION, PENDIENTE }
data class Hito(val titulo: String, val detalle: String, val estado: EstadoHito, val fecha: String? = null)

@Composable
fun DetalleProyectoScreen(
    proyectoId: Long = 1L,
    viewModel: DetalleProyectoViewModel = viewModel(),
    onBack: () -> Unit = {},
    onEditar: () -> Unit = {},
    onPerfil: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(proyectoId) {
        viewModel.cargarDetalle(proyectoId)
    }

    Scaffold(
        containerColor = Surface,
        topBar = { DetalleTopBar(onBack, onPerfil) }
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
                    DetalleProyectoContenido(
                        proyectoDto = state.proyecto,
                        equipoDto = state.equipo,
                        onEditar = onEditar
                    )
                }
            }
        }
    }
}

@Composable
private fun DetalleProyectoContenido(
    proyectoDto: ProyectoResponseDto,
    equipoDto: List<IntegranteResponseDto>,
    onEditar: () -> Unit
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
        ?: listOf("Análisis de requerimientos", "Diseño e implementación de interfaz", "Integración y pruebas")

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

    val hitos = listOf(
        Hito("Hito 1: Definición de Arquitectura", "Diagrama de entidades y componentes del sistema.", EstadoHito.COMPLETADO),
        Hito("Hito 2: Desarrollo del Core y APIs", "Endpoints REST e integración inicial.", EstadoHito.COMPLETADO),
        Hito("Hito 3: Pruebas e Integración Frontend", "Vistas reactivas y consumo de servicios.", EstadoHito.EN_REVISION, proyectoDto.fechaEntrega ?: "Próximamente"),
        Hito("Hito 4: Despliegue y Entrega Final", "Publicación final del proyecto.", EstadoHito.PENDIENTE)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Cabecera(codigo, titulo, descripcion, onEditar)
        Tabs(pestana, equipo.size) { pestana = it }
        ResumenCard(hitoActual = 3, hitosTotal = 4, progreso = 0.75f, entregaFinal = proyectoDto.fechaEntrega ?: "15 Dic, 2024")

        when (pestana) {
            Pestana.INFO -> InfoTab(problematica, objetivoGeneral, objetivosEspecificos, stack, requerimientos)
            Pestana.EQUIPO -> EquipoProyectoContent(equipoDto = equipoDto)
            Pestana.HITOS -> HitosTab(hitos)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DetalleTopBar(onBack: () -> Unit, onPerfil: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .height(64.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Regresar", tint = OnSurface)
            }
            Text("Detalle De Proyecto", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
        }
        IconButton(onClick = onPerfil) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Person, "Mi perfil", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/* ───────────── Cabecera, tabs y resumen ───────────── */
@Composable
private fun Cabecera(codigo: String, titulo: String, descripcion: String, onEditar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(codigo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, color = OnSurfaceVariant)
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SecondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Secondary))
                    Text("Activo", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSecondaryContainer)
                }
            }
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(PrimaryFixed)
                    .clickable(onClick = onEditar)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Outlined.Edit, null, tint = Primary, modifier = Modifier.size(16.dp))
                Text("Editar", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Primary)
            }
        }
        Text(titulo, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, color = OnSurface)
        Text(descripcion, fontSize = 13.sp, lineHeight = 19.sp, color = OnSurfaceVariant)
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
private fun ResumenCard(hitoActual: Int, hitosTotal: Int, progreso: Float, entregaFinal: String) {
    Tarjeta {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(PrimaryFixed),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Verified, null, tint = Primary, modifier = Modifier.size(20.dp)) }
                Column {
                    Text("SUPERVISIÓN ACADÉMICA", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = OnSurfaceVariant)
                    Text("Evaluación Docente", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryFixed)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    "Hito $hitoActual / $hitosTotal",
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace, color = Primary
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Progreso de Entrega", fontSize = 13.sp, color = OnSurface)
                Text("${(progreso * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Primary)
            }
            Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(SurfaceHigh)) {
                Box(Modifier.fillMaxWidth(progreso.coerceIn(0f, 1f)).height(8.dp).clip(CircleShape).background(Primary))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceLow)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Event, null, tint = Primary, modifier = Modifier.size(16.dp))
                Text("Entrega final:", fontSize = 12.sp, color = OnSurfaceVariant)
            }
            Text(entregaFinal, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OnSurface)
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

/* ───────────── Pestaña Hitos (línea de tiempo) ───────────── */
@Composable
private fun HitosTab(hitos: List<Hito>) {
    Tarjeta {
        Text("Cronograma de Entregas", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
        hitos.forEachIndexed { i, h ->
            HitoRow(h, esUltimo = i == hitos.lastIndex)
        }
    }
}

@Composable
private fun HitoRow(h: Hito, esUltimo: Boolean) {
    val (icono, fondoIcono, fgIcono) = when (h.estado) {
        EstadoHito.COMPLETADO -> Triple(Icons.Outlined.Check, Secondary, Color.White)
        EstadoHito.EN_REVISION -> Triple(Icons.Outlined.HourglassTop, Primary, Color.White)
        EstadoHito.PENDIENTE -> Triple(Icons.Outlined.Schedule, SurfaceHigh, OnSurfaceVariant)
    }
    val (etiqueta, colorEtiqueta) = when (h.estado) {
        EstadoHito.COMPLETADO -> "Completado" to Secondary
        EstadoHito.EN_REVISION -> "En Revisión" to Primary
        EstadoHito.PENDIENTE -> "Pendiente" to OnSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(fondoIcono),
                contentAlignment = Alignment.Center
            ) { Icon(icono, null, tint = fgIcono, modifier = Modifier.size(16.dp)) }
            if (!esUltimo) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(SurfaceHighest)
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f).padding(bottom = if (esUltimo) 0.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(etiqueta.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colorEtiqueta)
                if (h.fecha != null) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ErrorContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) { Text(h.fecha, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnErrorContainer) }
                }
            }
            Text(h.titulo, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
            Text(h.detalle, fontSize = 13.sp, lineHeight = 18.sp, color = OnSurfaceVariant)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DetalleProyectoPreview() {
    DetalleProyectoScreen()
}
