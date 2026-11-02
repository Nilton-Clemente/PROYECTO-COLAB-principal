package com.prieto.colab_movil.ui.proyectos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val Primary = Color(0xFF3525CD)

@Composable
fun EditarProyectoScreen(
    proyectoId: Long,
    onGuardado: () -> Unit,
    onCancelar: () -> Unit,
    viewModel: EditarProyectoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(proyectoId) {
        viewModel.cargarProyecto(proyectoId)
    }

    when (val state = uiState) {
        is EditarProyectoUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        }
        is EditarProyectoUiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    state.message,
                    color = Color(0xFF93000A),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.cargarProyecto(proyectoId) }) {
                    Text("Reintentar")
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onCancelar) {
                    Text("Volver", color = Primary)
                }
            }
        }
        is EditarProyectoUiState.Success -> {
            CrearProyectoScreen(
                proyectoId = proyectoId,
                proyectoInicial = state.proyecto,
                onGuardar = onGuardado,
                onCancelar = onCancelar
            )
        }
    }
}