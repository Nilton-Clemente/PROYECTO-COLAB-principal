package com.prieto.colab_movil.ui.proyectos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prieto.colab_movil.data.model.IntegranteResponseDto

private val Primary = Color(0xFF3525CD)
private val PrimaryContainer = Color(0xFF4F46E5)
private val SurfaceLowest = Color(0xFFFFFFFF)
private val SurfaceLow = Color(0xFFF2F3FF)
private val TextPrimary = Color(0xFF131B2E)
private val TextSecondary = Color(0xFF464555)
private val Green = Color(0xFF006C49)
private val GreenBackground = Color(0xFF6CF8BB)
private val Red = Color(0xFF93000A)
private val RedBackground = Color(0xFFFFDAD6)

@Composable
fun EquipoProyectoContent(
    equipoDto: List<IntegranteResponseDto>,
    onInvitar: (String) -> Unit = {}
) {
    var mostrarInvitar by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Equipo del Proyecto",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                "${equipoDto.size} integrantes",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        CapacidadSquad(equipoDto.size)

        Button(
            onClick = { mostrarInvitar = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text("＋  Invitar integrante", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }

        Text(
            "Miembros del Squad",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        if (equipoDto.isEmpty()) {
            Text(
                "Aún no hay integrantes en este proyecto.",
                fontSize = 13.sp,
                color = TextSecondary
            )
        } else {
            equipoDto.forEach { integrante ->
                TarjetaIntegrante(integrante)
            }
        }
    }

    if (mostrarInvitar) {
        InvitarIntegranteSheet(
            onDismiss = { mostrarInvitar = false },
            onInvitar = { email ->
                mostrarInvitar = false
                onInvitar(email)
            }
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvitarIntegranteSheet(
    onDismiss: () -> Unit,
    onInvitar: (String) -> Unit
) {
    var correo by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceLowest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                "Invitar integrante",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Ingresa el correo institucional del estudiante.",
                fontSize = 14.sp,
                color = TextSecondary
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it },
                label = { Text("Correo institucional") },
                placeholder = { Text("estudiante@tecsup.edu.pe") },
                singleLine = true,
                isError = error != null,
                modifier = Modifier.fillMaxWidth()
            )
            if (error != null) {
                Text(
                    error ?: "",
                    color = Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (correo.contains("@") && correo.contains(".")) {
                        onInvitar(correo.trim())
                    } else {
                        error = "Ingresa un correo válido."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Invitar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CapacidadSquad(total: Int) {
    val capacidad = 5
    val progreso = (total / capacidad.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLow)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE2DFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("♧", fontSize = 28.sp, color = Primary, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "CAPACIDAD DE SQUAD",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        "$total de $capacidad integrantes",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(9.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDADFFD))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progreso)
                            .clip(CircleShape)
                            .background(Primary)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Text(
                    "${total * 20}%",
                    fontSize = 13.sp,
                    color = Green,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TarjetaIntegrante(integrante: IntegranteResponseDto) {
    val propietario = integrante.rol == "CREADOR"
    val pendiente = integrante.estado == "PENDIENTE"
    val iniciales = iniciales(integrante.nombre)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2DFFF)),
                contentAlignment = Alignment.Center
            ) {
                Text(iniciales, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        integrante.nombre,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (propietario) {
                        Text(" ✓", color = Primary, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    integrante.email,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(6.dp))

            Column(horizontalAlignment = Alignment.End) {
                if (propietario) {
                    EtiquetaEstado("Propietario", PrimaryContainer, Color.White)
                    Spacer(Modifier.height(4.dp))
                }
                EtiquetaEstado(
                    texto = if (pendiente) "Pendiente" else "Activo",
                    fondo = if (pendiente) RedBackground else GreenBackground,
                    textoColor = if (pendiente) Red else Green
                )
            }
        }
    }
}

@Composable
private fun EtiquetaEstado(texto: String, fondo: Color, textoColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(fondo)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            texto,
            color = textoColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

private fun iniciales(nombre: String): String {
    val partes = nombre.trim().split(" ").filter { it.isNotBlank() }
    return when {
        partes.isEmpty() -> "US"
        partes.size == 1 -> partes.first().take(1).uppercase()
        else -> (partes.first().take(1) + partes.last().take(1)).uppercase()
    }
}