package com.prieto.colab_movil.ui.notificaciones

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prieto.colab_movil.data.model.NotificacionResponseDto
import com.prieto.colab_movil.ui.components.ColabActualizar
import com.prieto.colab_movil.ui.components.ColabBarraSecundaria
import com.prieto.colab_movil.ui.components.EstadoCarga

private val Fondo = Color(0xFFFAF8FF)
private val Primario = Color(0xFF3525CD)
private val Texto = Color(0xFF131B2E)
private val Secundario = Color(0xFF464555)

@Composable
fun NotificacionesScreen(
    onBack: () -> Unit,
    viewModel: NotificacionesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) viewModel.cargar()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    Scaffold(
        containerColor = Fondo,
        topBar = { ColabBarraSecundaria("Notificaciones", onBack) }
    ) { padding ->
        when (val estado = uiState) {
            NotificacionesUiState.Loading -> EstadoCarga(Modifier.padding(padding))
            is NotificacionesUiState.Error -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(estado.mensaje, color = Secundario)
                Button(onClick = viewModel::cargar) { Text("Reintentar") }
            }
            is NotificacionesUiState.Success -> ColabActualizar(
                actualizando = estado.actualizando,
                habilitado = !estado.ocupado,
                onActualizar = viewModel::cargar,
                modifier = Modifier.padding(padding).testTag("notificaciones-actualizar")
            ) {
                NotificacionesContent(estado, viewModel::marcarLeida, viewModel::eliminarLeidas)
            }
        }
    }
}

@Composable
internal fun NotificacionesContent(
    estado: NotificacionesUiState.Success,
    onMarcarLeida: (Long) -> Unit,
    onEliminarLeidas: () -> Unit
) {
    var confirmarEliminacion by remember { mutableStateOf(false) }

    if (confirmarEliminacion && estado.puedeEliminarLeidas) AlertDialog(
        onDismissRequest = { confirmarEliminacion = false },
        title = { Text("¿Eliminar notificaciones leídas?") },
        text = { Text("Se eliminarán todas tus notificaciones marcadas como leídas. Las que están sin leer se conservarán. Esta acción no se puede deshacer.") },
        confirmButton = {
            TextButton(onClick = {
                confirmarEliminacion = false
                onEliminarLeidas()
            }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = { confirmarEliminacion = false }) { Text("Cancelar") } }
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("notificaciones-lista"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Tu bandeja", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Texto)
                    Text("${estado.noLeidas} sin leer", color = Primario, fontWeight = FontWeight.SemiBold)
                }
                TextButton(
                    onClick = { confirmarEliminacion = true },
                    enabled = estado.puedeEliminarLeidas,
                    modifier = Modifier.testTag("eliminar-notificaciones-leidas")
                ) {
                    Icon(Icons.Outlined.DeleteOutline, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (estado.eliminandoLeidas) "Eliminando…" else "Eliminar leídas")
                }
            }
            Text(
                "Aquí recibes las solicitudes de cambios de tus integrantes y los avisos de tus proyectos.",
                color = Secundario, fontSize = 14.sp
            )
        }
        estado.aviso?.let { aviso -> item { Text(aviso, color = MaterialTheme.colorScheme.error) } }
        if (estado.notificaciones.isEmpty()) item {
            Text("Todavía no tienes notificaciones.", Modifier.padding(vertical = 32.dp), color = Secundario)
        }
        items(estado.notificaciones, key = { it.id }) { notificacion ->
            NotificacionCard(
                notificacion,
                habilitado = !estado.ocupado && !estado.desactualizado,
                marcando = estado.marcandoId == notificacion.id,
                onMarcarLeida = { onMarcarLeida(notificacion.id) }
            )
        }
    }
}

@Composable
private fun NotificacionCard(
    notificacion: NotificacionResponseDto,
    habilitado: Boolean,
    marcando: Boolean,
    onMarcarLeida: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("notificacion-${notificacion.id}")
            .background(if (notificacion.leida) Color.White else Color(0xFFEEF0FF), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            when (notificacion.tipoEvento) {
                "PLANIFICACION" -> "Planificación"
                "ASIGNACION", "ASSIGNACION" -> "Asignación de tarea"
                "CAMBIO_ESTADO" -> "Cambio de estado"
                "CAMBIO_RESPONSABLE" -> "Cambio de responsable"
                "DESBLOQUEO" -> "Tarea desbloqueada"
                else -> "Aviso del proyecto"
            }, color = Primario, fontWeight = FontWeight.Bold
        )
        val nombreProyecto = notificacion.proyectoNombre?.takeIf { it.isNotBlank() }
        val codigoProyecto = notificacion.proyectoCodigo?.takeIf { it.isNotBlank() }
            ?: notificacion.proyectoId?.let { "TEC-$it" }
        Text(
            listOfNotNull(nombreProyecto, codigoProyecto).joinToString(" · ")
                .ifEmpty { "Proyecto no identificado" },
            modifier = Modifier.testTag("notificacion-proyecto-${notificacion.id}"),
            color = Texto, fontWeight = FontWeight.SemiBold, fontSize = 14.sp
        )
        Text(if (notificacion.leida) "Leída" else "Sin leer", color = Secundario, fontSize = 12.sp)
        Text(notificacion.mensaje, color = Texto, fontSize = 15.sp, lineHeight = 22.sp)
        notificacion.fechaCreacion?.let {
            // El backend devuelve fecha local sin zona: mostrarla sin inventar conversiones horarias.
            Text(it.replace('T', ' '), color = Secundario, fontSize = 12.sp)
        }
        if (!notificacion.leida) TextButton(
            onClick = onMarcarLeida, enabled = habilitado,
            modifier = Modifier.testTag("leer-notificacion-${notificacion.id}")
        ) { Text(if (marcando) "Guardando…" else "Marcar como leída") }
    }
}