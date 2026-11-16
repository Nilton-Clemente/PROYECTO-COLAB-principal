package com.prieto.colab_movil.ui.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import com.prieto.colab_movil.data.model.HabilidadDto
import com.prieto.colab_movil.ui.components.IconoHabilidad

private val Primario = Color(0xFF3525CD)
private val Lavanda = Color(0xFFE2DFFF)
private val Borde = Color(0xFFC7C4D8)
private val Texto = Color(0xFF131B2E)
private val Secundario = Color(0xFF464555)
private val Rojo = Color(0xFF93000A)

@Composable
internal fun ContenidoPerfilRedisenado(
    state: PerfilUiState.Success,
    onEliminar: (Long) -> Unit,
    onCerrarSesion: () -> Unit,
    paddingValues: PaddingValues
) {
    var mostrarTodas by remember { mutableStateOf(false) }
    var mostrarAviso by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { PerfilEstudiante(nombre = state.nombre, email = state.email, onCerrarSesion = onCerrarSesion) }
        item {
            SeccionHabilidades(state.habilidades, mostrarTodas, { mostrarTodas = !mostrarTodas }, onEliminar)
        }
        item { ExplorarCatalogo { mostrarAviso = true } }
    }

    if (mostrarAviso) {
        AlertDialog(
            onDismissRequest = { mostrarAviso = false },
            title = { Text("Catálogo de habilidades") },
            text = { Text("La pantalla del catálogo estará disponible próximamente.") },
            confirmButton = { TextButton(onClick = { mostrarAviso = false }) { Text("Entendido") } }
        )
    }
}

@Composable
internal fun SeccionHabilidades(
    habilidades: List<HabilidadDto>,
    mostrarTodas: Boolean,
    onMostrarTodas: () -> Unit,
    onEliminar: (Long) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .border(1.dp, Borde, RoundedCornerShape(22.dp))
            .background(Color.White, RoundedCornerShape(22.dp)).padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Mis habilidades", modifier = Modifier.weight(1f), fontSize = 19.sp,
                fontWeight = FontWeight.Bold, color = Texto)
            TextButton(onClick = onMostrarTodas, enabled = habilidades.isNotEmpty(),
                contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text(if (mostrarTodas) "Ver menos" else "Ver todas ›", fontSize = 12.sp, color = Primario)
            }
        }
        Surface(color = Lavanda, shape = CircleShape) {
            Text("${habilidades.size} habilidades", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                color = Primario, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text("Tecnologías que forman parte de tu perfil.", fontSize = 13.sp, color = Secundario)
        Spacer(Modifier.height(16.dp))
        if (habilidades.isEmpty()) {
            Text("Todavía no tienes habilidades agregadas.", fontSize = 13.sp, color = Secundario)
        } else if (mostrarTodas) {
            habilidades.forEach { habilidad ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconoHabilidad(habilidad.nombre, habilidad.icono, modifier = Modifier.size(32.dp), tamanoInicial = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(habilidad.nombre, fontWeight = FontWeight.SemiBold, color = Texto, fontSize = 14.sp)
                        Text(habilidad.area ?: "General", fontSize = 11.sp, color = Secundario)
                    }
                    TextButton(onClick = { onEliminar(habilidad.id) }) { Text("Eliminar", color = Rojo, fontSize = 11.sp) }
                }
                HorizontalDivider(color = Lavanda)
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                habilidades.forEach { habilidad ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(82.dp)) {
                        Box(modifier = Modifier.size(70.dp).clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFEAEDFF)).border(1.dp, Borde, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center) {
                            IconoHabilidad(habilidad.nombre, habilidad.icono, modifier = Modifier.size(42.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(habilidad.nombre, fontSize = 11.sp, color = Texto, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ExplorarCatalogo(onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().border(1.dp, Borde, RoundedCornerShape(22.dp))
        .clip(RoundedCornerShape(22.dp)).background(Color.White).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Lavanda),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Search, contentDescription = null, tint = Primario)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Explorar catálogo de habilidades", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Texto)
            Spacer(Modifier.height(4.dp))
            Text("Descubre nuevas competencias tecnológicas", fontSize = 12.sp, color = Secundario)
        }
        Text("→", modifier = Modifier.padding(start = 8.dp), color = Texto, fontSize = 22.sp)
    }
}