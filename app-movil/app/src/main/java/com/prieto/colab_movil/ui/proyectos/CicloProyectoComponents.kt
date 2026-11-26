package com.prieto.colab_movil.ui.proyectos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prieto.colab_movil.data.model.EstadoProyecto

@Composable
internal fun EstadoProyectoBadge(estado: EstadoProyecto) {
    val (fondo, texto) = when (estado) {
        EstadoProyecto.PLANIFICACION_PROPUESTA -> Color(0xFFFFF3D6) to Color(0xFF805500)
        EstadoProyecto.EN_DESARROLLO -> Color(0xFFE8EFFF) to Color(0xFF254D9C)
        EstadoProyecto.FINALIZADO -> Color(0xFFD8F5E8) to Color(0xFF066B4B)
        EstadoProyecto.CANCELADO -> Color(0xFFFDECEB) to Color(0xFFB42318)
        else -> Color(0xFFEEF0F6) to Color(0xFF464555)
    }
    Surface(shape = CircleShape, color = fondo, contentColor = texto) {
        Text(estado.titulo, Modifier.padding(horizontal = 9.dp, vertical = 4.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun MenuProyecto(acciones: List<AccionProyecto>, habilitado: Boolean, onAccion: (AccionProyecto) -> Unit) {
    if (acciones.isEmpty()) return
    var abierto by remember { mutableStateOf(false) }
    LaunchedEffect(habilitado) { if (!habilitado) abierto = false }
    Box {
        IconButton(onClick = { abierto = true }, enabled = habilitado) {
            Icon(Icons.Outlined.MoreVert, contentDescription = "Más acciones del proyecto")
        }
        DropdownMenu(expanded = abierto && habilitado, onDismissRequest = { abierto = false }) {
            acciones.forEach { accion ->
                DropdownMenuItem(text = {
                    Text(accion.titulo, color = if (accion == AccionProyecto.EDITAR) LocalContentColor.current else MaterialTheme.colorScheme.error)
                }, onClick = { abierto = false; onAccion(accion) })
            }
        }
    }
}

@Composable
internal fun ConfirmarCicloProyecto(
    nombre: String, accion: AccionProyecto, onCerrar: () -> Unit, onConfirmar: () -> Unit
) {
    val eliminar = accion == AccionProyecto.ELIMINAR
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(accion.titulo) },
        text = {
            Text(if (eliminar) "¿Quieres eliminar definitivamente «$nombre»? Se eliminarán también sus tareas y avances. Esta acción no se puede deshacer."
            else "Se detendrá «$nombre». El equipo podrá consultar la planificación, las tareas y el historial en modo de solo lectura.")
        },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text(if (eliminar) "Eliminar" else "Confirmar cancelación", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text(if (eliminar) "Cancelar" else "Volver") } }
    )
}