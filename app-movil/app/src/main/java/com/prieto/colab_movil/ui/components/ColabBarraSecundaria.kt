package com.prieto.colab_movil.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColabBarraSecundaria(
    titulo: String,
    onVolver: () -> Unit,
    habilitado: Boolean = true,
    descripcionVolver: String = "Volver",
    acciones: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(titulo, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF131B2E)) },
        navigationIcon = {
            IconButton(onClick = onVolver, enabled = habilitado) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = descripcionVolver, tint = Color(0xFF3525CD))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAF8FF)),
        actions = acciones
    )
}