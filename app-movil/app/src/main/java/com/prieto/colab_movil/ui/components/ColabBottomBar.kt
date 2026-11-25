package com.prieto.colab_movil.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class SeccionPrincipal { PROYECTOS, INVITACIONES, PERFIL }

@Composable
fun ColabBottomBar(
    seleccionada: SeccionPrincipal,
    onProyectos: () -> Unit,
    onInvitaciones: () -> Unit,
    onPerfil: () -> Unit,
    hayInvitaciones: Boolean = false,
    iconosRellenos: Boolean = false,
    colorNoSeleccionado: Color = Color(0xFF64647A),
    elevacion: Dp = 3.dp
) {
    val colores = NavigationBarItemDefaults.colors(
        selectedIconColor = Color(0xFF3525CD), selectedTextColor = Color(0xFF3525CD),
        indicatorColor = Color.Transparent, unselectedIconColor = colorNoSeleccionado,
        unselectedTextColor = colorNoSeleccionado
    )
    NavigationBar(containerColor = Color.White, tonalElevation = elevacion) {
        NavigationBarItem(
            selected = seleccionada == SeccionPrincipal.PROYECTOS, onClick = onProyectos, colors = colores,
            icon = { Icon(if (iconosRellenos) Icons.Filled.Folder else Icons.Outlined.Folder, null) },
            label = { Text("Proyectos", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
        )
        NavigationBarItem(
            selected = seleccionada == SeccionPrincipal.INVITACIONES, onClick = onInvitaciones, colors = colores,
            icon = {
                BadgedBox(badge = {
                    if (hayInvitaciones) {
                        Badge(modifier = Modifier.semantics { contentDescription = "Invitaciones pendientes" },
                            containerColor = Color(0xFF960014))
                    }
                }) {
                    Icon(if (iconosRellenos) Icons.Filled.Mail else Icons.Outlined.Mail, null)
                }
            },
            label = { Text("Invitaciones", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
        )
        NavigationBarItem(
            selected = seleccionada == SeccionPrincipal.PERFIL, onClick = onPerfil, colors = colores,
            icon = { Icon(if (iconosRellenos) Icons.Filled.Person else Icons.Outlined.Person, null) },
            label = { Text("Mi Perfil", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
        )
    }
}