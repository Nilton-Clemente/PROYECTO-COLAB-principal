package com.prieto.colab_movil.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColabTopBar(
    onNotificaciones: () -> Unit,
    onPerfil: () -> Unit,
    colorIconoNotificaciones: Color = Color(0xFF64647A)
) {
    TopAppBar(
        title = { Text("Colab", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color(0xFF131B2E)) },
        actions = {
            IconButton(onClick = onNotificaciones) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Notificaciones", tint = colorIconoNotificaciones)
            }
            Box(
                modifier = Modifier.padding(end = 16.dp).size(34.dp).clip(CircleShape)
                    .background(Color(0xFF3525CD)).clickable(role = Role.Button, onClick = onPerfil),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Person, contentDescription = "Mi perfil", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAF8FF))
    )
}