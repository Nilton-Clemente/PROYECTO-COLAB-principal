package com.prieto.colab_movil.ui.invitaciones

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prieto.colab_movil.data.model.InvitacionResponseDto
import com.prieto.colab_movil.ui.components.EstadoCarga
import com.prieto.colab_movil.ui.components.EstadoError
import com.prieto.colab_movil.ui.components.ColabTopBar
import com.prieto.colab_movil.ui.components.ColabActualizar
import com.prieto.colab_movil.ui.components.ColabBottomBar
import com.prieto.colab_movil.ui.components.SeccionPrincipal

// ----------------------------------------------------
// COLORES (misma paleta del resto de la app)
// ----------------------------------------------------

private val Fondo = Color(0xFFFAF8FF)
private val Superficie = Color(0xFFFFFFFF)
private val Primario = Color(0xFF3525CD)
private val PrimarioContainer = Color(0xFF4F46E5)
private val Lavanda = Color(0xFFE2E7FF)
private val LavandaClara = Color(0xFFF2F3FF)
private val TextoPrincipal = Color(0xFF131B2E)
private val TextoSecundario = Color(0xFF64647A)
private val Verde = Color(0xFF006C49)
private val VerdeSuave = Color(0xFF6CF8BB)

// ----------------------------------------------------
// PANTALLA PRINCIPAL
// ----------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvitacionesScreen(
    viewModel: InvitacionesViewModel = viewModel(),
    onProyectosClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val mensaje by viewModel.mensaje.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumirMensaje()
        }
    }

    var pestanaSeleccionada by remember { mutableStateOf("Pendientes") }

    Scaffold(
        containerColor = Fondo,
        topBar = {
            ColabTopBar(onNotificaciones = onNotificacionesClick, onPerfil = onPerfilClick)
        },
        bottomBar = {
            ColabBottomBar(
                seleccionada = SeccionPrincipal.INVITACIONES, onProyectos = onProyectosClick,
                onInvitaciones = {}, onPerfil = onPerfilClick, iconosRellenos = true, elevacion = 4.dp
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { padding ->
        when (val state = uiState) {
            is InvitacionesUiState.Loading -> EstadoCarga(modifier = Modifier.padding(padding))

            is InvitacionesUiState.Error -> EstadoError(
                onReintentar = { viewModel.cargar() }, modifier = Modifier.padding(padding)
            )

            is InvitacionesUiState.Success -> {
                val invitaciones = state.invitaciones

                ColabActualizar(actualizando = state.actualizando, habilitado = !state.actualizando && !state.ocupado,
                    onActualizar = { viewModel.cargar() }, modifier = Modifier.padding(padding)) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 20.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ENCABEZADO
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                text = "Invitaciones recibidas",
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextoPrincipal
                            )

                            Text(
                                text = "Gestiona las solicitudes de tus compañeros de TECSUP para unirte a proyectos de ingeniería.",
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                color = TextoSecundario
                            )
                        }
                    }

                    // PESTAÑAS
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = pestanaSeleccionada == "Pendientes",
                                onClick = { pestanaSeleccionada = "Pendientes" },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("Pendientes")
                                        Text(
                                            text = invitaciones.size.toString(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primario,
                                    selectedLabelColor = Color.White,
                                    containerColor = Lavanda,
                                    labelColor = TextoSecundario
                                ),
                                border = null
                            )

                            FilterChip(
                                selected = pestanaSeleccionada == "Historial",
                                onClick = { pestanaSeleccionada = "Historial" },
                                label = { Text("Historial / Aceptadas") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primario,
                                    selectedLabelColor = Color.White,
                                    containerColor = Lavanda,
                                    labelColor = TextoSecundario
                                ),
                                border = null
                            )
                        }
                    }

                    if (pestanaSeleccionada == "Pendientes") {
                        if (invitaciones.isEmpty()) {
                            item { EmptyInvitaciones() }
                        } else {
                            items(
                                items = invitaciones,
                                key = { it.integranteId }
                            ) { invitacion ->
                                InvitacionCard(
                                    invitacion = invitacion,
                                    onAceptar = { viewModel.aceptar(invitacion.integranteId) },
                                    onDeclinar = { viewModel.rechazar(invitacion.integranteId) },
                                    habilitado = !state.ocupado && !state.actualizando && !state.desactualizado
                                )
                            }
                        }
                    } else {
                        item { HistorialVacio() }
                    }
                }
                }
            }
        }
    }
}

// ----------------------------------------------------
// TARJETA DE INVITACIÓN
// ----------------------------------------------------

@Composable
private fun InvitacionCard(
    invitacion: InvitacionResponseDto,
    onAceptar: () -> Unit,
    onDeclinar: () -> Unit,
    habilitado: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Lavanda),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = Primario,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Proyecto #${invitacion.proyectoId}",
                        fontSize = 12.sp,
                        color = TextoSecundario,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        text = invitacion.proyectoNombre,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextoPrincipal,
                        lineHeight = 20.sp
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LavandaClara)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Mail,
                    contentDescription = null,
                    tint = Primario,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(Modifier.width(9.dp))

                Text(
                    text = "Has sido invitado a unirte como colaborador.",
                    fontSize = 12.sp,
                    color = TextoSecundario
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAceptar,
                    enabled = habilitado,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(9.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimarioContainer),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        Icons.Default.HowToReg,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(Modifier.width(5.dp))

                    Text(
                        text = "Aceptar invitación",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = onDeclinar,
                    enabled = habilitado,
                    modifier = Modifier.height(46.dp),
                    shape = RoundedCornerShape(9.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Lavanda,
                        contentColor = TextoSecundario
                    ),
                    border = null,
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )

                    Spacer(Modifier.width(4.dp))

                    Text(
                        text = "Declinar",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// ESTADO VACÍO
// ----------------------------------------------------

@Composable
private fun EmptyInvitaciones() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Superficie),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(VerdeSuave),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Verde,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "¡Al día con tus invitaciones!",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextoPrincipal
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "No tienes invitaciones pendientes por responder. Cuando un equipo de TECSUP te sume, aparecerá aquí al instante.",
                fontSize = 12.sp,
                color = TextoSecundario,
                lineHeight = 18.sp
            )
        }
    }
}

// ----------------------------------------------------
// HISTORIAL (aún sin endpoint en el backend)
// ----------------------------------------------------

@Composable
private fun HistorialVacio() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Superficie),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Lavanda),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Primario,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Aún no hay invitaciones aceptadas",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextoPrincipal
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Cuando aceptes una invitación, el proyecto aparecerá en esta sección.",
                fontSize = 12.sp,
                color = TextoSecundario,
                lineHeight = 18.sp
            )
        }
    }
}
