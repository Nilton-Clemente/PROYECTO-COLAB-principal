package com.prieto.colab_movil.ui.perfil

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prieto.colab_movil.data.model.HabilidadDto
import com.prieto.colab_movil.ui.components.ColabBarraSecundaria
import com.prieto.colab_movil.ui.components.EstadoCarga
import com.prieto.colab_movil.ui.components.IconoHabilidad

private val Fondo = Color(0xFFFAF8FF)
private val Primario = Color(0xFF3525CD)
private val Texto = Color(0xFF131B2E)
private val Secundario = Color(0xFF464555)

internal fun descripcionArea(area: String): String = when (area.trim().lowercase()) {
    "frontend" -> "Interfaces y experiencias web."
    "backend" -> "Servicios y lógica de aplicaciones."
    "movil", "móvil" -> "Desarrollo de aplicaciones móviles."
    "base de datos", "bases de datos" -> "Almacenamiento y gestión de datos."
    "gestion", "gestión" -> "Organización y gestión de proyectos."
    "calidad" -> "Pruebas y calidad del software."
    else -> "Habilidades de esta categoría."
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(onBack: () -> Unit, viewModel: CatalogoViewModel = viewModel()) {
    val estado by viewModel.uiState.collectAsState()
    val datos = estado as? CatalogoUiState.Success
    var busqueda by remember { mutableStateOf("") }
    var seleccion by remember { mutableStateOf<HabilidadDto?>(null) }
    var nivel by remember { mutableStateOf<NivelHabilidad?>(null) }
    var expandida by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val guardando = datos?.guardando == true

    BackHandler(enabled = seleccion == null) { if (!guardando) onBack() }
    BackHandler(enabled = seleccion != null && guardando) { }
    LaunchedEffect(estado) {
        if (estado is CatalogoUiState.Error) seleccion = null
    }
    LaunchedEffect(datos?.mensaje) {
        datos?.mensaje?.let { snackbar.showSnackbar(it); viewModel.consumirMensaje() }
    }
    Scaffold(
        containerColor = Fondo,
        topBar = { ColabBarraSecundaria("Catálogo de habilidades", onVolver = onBack, habilitado = !guardando) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        when (val actual = estado) {
            CatalogoUiState.Loading -> EstadoCarga(Modifier.padding(padding))
            is CatalogoUiState.Error -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(actual.mensaje, color = Secundario)
                Button(onClick = { viewModel.cargar() }) { Text("Reintentar") }
            }
            is CatalogoUiState.Success -> {
                val filtradas = actual.habilidades.filter {
                    it.nombre.contains(busqueda.trim(), ignoreCase = true) ||
                        (it.area ?: "General").contains(busqueda.trim(), ignoreCase = true)
                }
                val grupos = filtradas.groupBy { it.area?.trim().takeUnless { area -> area.isNullOrEmpty() } ?: "General" }
                    .toSortedMap(String.CASE_INSENSITIVE_ORDER)
                val idsPerfil = actual.perfil.map { it.id }.toSet()
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        OutlinedTextField(value = busqueda, onValueChange = { busqueda = it; expandida = null },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            label = { Text("Buscar habilidades") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                            singleLine = true, shape = RoundedCornerShape(14.dp))
                    }
                    if (grupos.isEmpty()) item {
                        Text(if (actual.habilidades.isEmpty()) "El catálogo no tiene habilidades disponibles." else "No encontramos habilidades para esa búsqueda.",
                            modifier = Modifier.padding(24.dp), color = Secundario)
                    }
                    grupos.forEach { (area, habilidades) ->
                        item(key = area) {
                            Column {
                                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(area, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Texto)
                                        Text(descripcionArea(area), fontSize = 12.sp, color = Secundario)
                                    }
                                    if (habilidades.size > 4) TextButton(onClick = { expandida = if (expandida == area) null else area }) {
                                        Text(if (expandida == area) "Ver menos" else "Ver más")
                                    }
                                }
                                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    items(if (expandida == area) habilidades else habilidades.take(4), key = { it.id }) { habilidad ->
                                        TarjetaCatalogoHabilidad(habilidad, habilidad.id in idsPerfil, !guardando) {
                                            seleccion = habilidad
                                            nivel = null
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    seleccion?.let { habilidad ->
        val yaEsta = datos?.perfil?.any { it.id == habilidad.id } == true
        if (!guardando && yaEsta) {
            LaunchedEffect(habilidad.id) { seleccion = null }
        } else {
            ModalBottomSheet(onDismissRequest = { if (!guardando) seleccion = null },
                dragHandle = { BottomSheetDefaults.DragHandle() }) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconoHabilidad(habilidad.nombre, habilidad.icono, Modifier.size(52.dp))
                        Column { Text(habilidad.nombre, fontSize = 19.sp, fontWeight = FontWeight.Bold); Text(habilidad.area ?: "General", color = Secundario) }
                    }
                    Text("Selecciona tu nivel de dominio", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("Elige el nivel con el que te identificas.", color = Secundario, fontSize = 14.sp)
                    NivelHabilidad.entries.forEach { opcion ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .border(1.dp, if (nivel == opcion) Primario else Color(0xFFDDE0F3), RoundedCornerShape(16.dp))
                            .clickable(enabled = !guardando) { nivel = opcion }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = nivel == opcion, onClick = { nivel = opcion }, enabled = !guardando)
                            Text(opcion.etiqueta, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Button(onClick = { nivel?.let { viewModel.agregar(habilidad, it) } },
                        enabled = nivel != null && !guardando, modifier = Modifier.fillMaxWidth()) {
                        Text(if (guardando) "Guardando…" else "Añadir habilidad a mi perfil")
                    }
                    TextButton(onClick = { seleccion = null }, enabled = !guardando, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun TarjetaCatalogoHabilidad(habilidad: HabilidadDto, agregada: Boolean, habilitado: Boolean, onClick: () -> Unit) {
    Column(Modifier.width(90.dp).clickable(enabled = habilitado && !agregada, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            IconoHabilidad(habilidad.nombre, habilidad.icono,
                Modifier.size(64.dp).clip(RoundedCornerShape(17.dp)).background(Color.White).padding(10.dp))
            Box(Modifier.align(Alignment.BottomEnd).size(24.dp).clip(CircleShape)
                .background(if (agregada) Color(0xFF6CF8BB) else Primario), contentAlignment = Alignment.Center) {
                Text(if (agregada) "✓" else "+", color = if (agregada) Color(0xFF006C49) else Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Text(habilidad.nombre, color = Texto, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (agregada) Text("En tu perfil", color = Color(0xFF006C49), fontSize = 11.sp)
    }
}