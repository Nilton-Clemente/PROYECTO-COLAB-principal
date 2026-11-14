package com.prieto.colab_movil.ui.proyectos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.prieto.colab_movil.data.model.CasoUsoDto

@Composable
fun CasosDeUsoFormulario(
    casosDeUso: List<CasoUsoDto>,
    onAgregar: () -> Unit,
    onCambiar: (Int, CasoUsoDto) -> Unit,
    onEliminar: (Int) -> Unit
) {
    Text("Describe las interacciones entre los actores y el sistema. Solo el nombre es obligatorio.")
    if (casosDeUso.isEmpty()) {
        Text("Todavía no hay casos de uso.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    casosDeUso.forEachIndexed { index, caso ->
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Caso de uso ${index + 1}", modifier = Modifier.weight(1f))
                    TextButton(onClick = { onEliminar(index) }) {
                        Text("Eliminar caso ${index + 1}", color = MaterialTheme.colorScheme.error)
                    }
                }
                CampoCasoUso("Nombre del caso de uso *", caso.nombre, index, "nombre") {
                    onCambiar(index, caso.copy(nombre = it))
                }
                CampoCasoUso("Descripción", caso.descripcion, index, "descripcion") {
                    onCambiar(index, caso.copy(descripcion = it))
                }
                CampoCasoUso("Actores", caso.actores, index, "actores") {
                    onCambiar(index, caso.copy(actores = it))
                }
                CampoCasoUso("Precondiciones", caso.precondiciones, index, "precondiciones") {
                    onCambiar(index, caso.copy(precondiciones = it))
                }
                CampoCasoUso("Disparador", caso.disparador, index, "disparador") {
                    onCambiar(index, caso.copy(disparador = it))
                }
                CampoCasoUso("Flujo principal", caso.flujoPrincipal, index, "flujoPrincipal") {
                    onCambiar(index, caso.copy(flujoPrincipal = it))
                }
                CampoCasoUso("Flujos alternativos", caso.flujosAlternativos, index, "flujosAlternativos") {
                    onCambiar(index, caso.copy(flujosAlternativos = it))
                }
                CampoCasoUso("Excepciones", caso.excepciones, index, "excepciones") {
                    onCambiar(index, caso.copy(excepciones = it))
                }
                CampoCasoUso("Postcondiciones", caso.postcondiciones, index, "postcondiciones") {
                    onCambiar(index, caso.copy(postcondiciones = it))
                }
                CampoCasoUso("Reglas de negocio", caso.reglasDeNegocio, index, "reglasDeNegocio") {
                    onCambiar(index, caso.copy(reglasDeNegocio = it))
                }
            }
        }
    }

    Button(onClick = onAgregar, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Text("+ Añadir caso de uso")
    }
}

@Composable
private fun CampoCasoUso(
    titulo: String,
    valor: String?,
    indice: Int,
    campo: String,
    onCambiar: (String) -> Unit
) {
    OutlinedTextField(
        value = valor.orEmpty(),
        onValueChange = onCambiar,
        label = { Text(titulo) },
        modifier = Modifier.fillMaxWidth().testTag("caso-$indice-$campo"),
        minLines = if (campo == "nombre") 1 else 2,
        shape = RoundedCornerShape(12.dp),
        colors = coloresCampo()
    )
}

internal fun CasoUsoDto.normalizado(): CasoUsoDto = copy(
    nombre = nombre.trim(),
    descripcion = descripcion?.trim()?.ifBlank { null },
    actores = actores?.trim()?.ifBlank { null },
    precondiciones = precondiciones?.trim()?.ifBlank { null },
    disparador = disparador?.trim()?.ifBlank { null },
    flujoPrincipal = flujoPrincipal?.trim()?.ifBlank { null },
    flujosAlternativos = flujosAlternativos?.trim()?.ifBlank { null },
    excepciones = excepciones?.trim()?.ifBlank { null },
    postcondiciones = postcondiciones?.trim()?.ifBlank { null },
    reglasDeNegocio = reglasDeNegocio?.trim()?.ifBlank { null }
)