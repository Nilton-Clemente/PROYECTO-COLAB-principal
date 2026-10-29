package com.prieto.colab_movil.ui.proyectos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prieto.colab_movil.data.model.ProyectoRequest
import com.prieto.colab_movil.data.model.RequerimientoFuncionalDto

private val Primary = Color(0xFF3525CD)
private val PrimaryContainer = Color(0xFFE5E2FF)
private val BackgroundColor = Color(0xFFFAF8FF)
private val SurfaceColor = Color.White
private val FieldColor = Color(0xFFF0F1FF)
private val TextColor = Color(0xFF131B2E)
private val SecondaryText = Color(0xFF55556A)
private val GreenColor = Color(0xFF00875A)
private val GreenContainer = Color(0xFF6CF8BB)
private val RedColor = Color(0xFFBC1D25)

@Composable
fun CrearProyectoScreen(
    viewModel: CrearProyectoViewModel = viewModel(),
    onGuardar: () -> Unit,
    onCancelar: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val tecnologiasCatalogo by viewModel.tecnologias.collectAsState()
    val guardando = uiState is CrearProyectoUiState.Cargando

    LaunchedEffect(uiState) {
        if (uiState is CrearProyectoUiState.Exito) {
            onGuardar()
        }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Primary,
            onPrimary = Color.White,
            background = BackgroundColor,
            surface = SurfaceColor,
            onSurface = TextColor,
            secondary = GreenColor
        )
    ) {
        var clave by remember { mutableStateOf("") }
        var nombre by remember { mutableStateOf("") }
        var descripcion by remember { mutableStateOf("") }
        var problematica by remember { mutableStateOf("") }
        var objetivoGeneral by remember { mutableStateOf("") }
        var alcance by remember { mutableStateOf("") }
        var restricciones by remember { mutableStateOf("") }
        var cantidadSprints by remember { mutableIntStateOf(1) }
        var fechaEntrega by remember { mutableStateOf("") }
        var nuevoObjetivo by remember { mutableStateOf("") }
        var nuevaTecnologia by remember { mutableStateOf("") }
        var mostrarDialogoTecnologia by remember { mutableStateOf(false) }
        var mostrarConfirmacion by remember { mutableStateOf(false) }
        var mensajeError by remember { mutableStateOf<String?>(null) }

        val objetivos = remember { mutableStateListOf<String>() }
        val tecnologias = remember { mutableStateListOf<String>() }
        val requerimientos = remember { mutableStateListOf<String>() }

        val seccionesAbiertas = remember {
            mutableStateListOf(true, true, false, true, false, true)
        }

        val secciones = listOf(
            "Datos Generales",
            "Objetivos del Software",
            "Alcance y Restricciones",
            "Stack Tecnológico",
            "Requerimientos Funcionales",
            "Plan y Entrega"
        )

        val descripciones = listOf(
            "Identificación y contexto de la iniciativa",
            "Hitos de valor técnico y alcance funcional",
            "Fronteras técnicas y dependencias externas",
            "Arquitectura de lenguajes, frameworks y nube",
            "Casos de uso clave y roles de usuario",
            "Sprints programados y fecha final del ciclo"
        )

        val iconos = listOf("▣", "◎", "⌗", "◇", "☑", "▦")

        val sugerencias = tecnologiasCatalogo.map { it.nombre }

        val progreso = listOf(clave, nombre, descripcion, objetivoGeneral)
            .count { it.isNotBlank() }
        val porcentaje = (progreso * 100 / 4).coerceIn(0, 100)

        Scaffold(
            containerColor = BackgroundColor,
            bottomBar = {
                Surface(
                    color = SurfaceColor,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancelar,
                            modifier = Modifier
                                .weight(0.85f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextColor
                            )
                        ) {
                            Text("Cancelar")
                        }

                        Button(
                            onClick = {
                                mostrarConfirmacion = true
                            },
                            modifier = Modifier
                                .weight(1.65f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary
                            )
                        ) {
                            Text("▣", fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Guardar Proyecto",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceColor.copy(alpha = 0.75f))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "‹", fontSize = 34.sp, color = TextColor)
                    Spacer(Modifier.width(18.dp))
                    Text(
                        text = "Crear Proyecto",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextColor,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("♙", color = Color.White, fontSize = 22.sp)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // Progreso del formulario
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(9.dp).background(Primary, CircleShape))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Paso 1 de 6",
                                    color = Primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "$porcentaje% completado",
                                    color = SecondaryText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            LinearProgressIndicator(
                                progress = { porcentaje / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = Primary,
                                trackColor = FieldColor
                            )

                        }
                    }

                    // 1. Datos generales
                    Seccion(
                        titulo = secciones[0],
                        descripcion = descripciones[0],
                        icono = iconos[0],
                        abierta = seccionesAbiertas[0],
                        onToggle = { seccionesAbiertas[0] = !seccionesAbiertas[0] }
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(modifier = Modifier.width(105.dp)) {
                                EtiquetaCampo("Clave")
                                OutlinedTextField(
                                    value = clave,
                                    onValueChange = { clave = it.take(4) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = coloresCampo()
                                )
                                Text("COL-", color = Primary, fontSize = 10.sp)
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                EtiquetaCampo("Nombre del Proyecto *")
                                OutlinedTextField(
                                    value = nombre,
                                    onValueChange = { nombre = it.take(60) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    placeholder = { Text("Nombre del proyecto") },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = coloresCampo()
                                )
                                Contador(nombre.length, 60)
                            }
                        }

                        CampoTexto(
                            titulo = "Descripción Breve",
                            valor = descripcion,
                            onValorCambiado = { descripcion = it.take(180) },
                            maximo = 180,
                            lineas = 3
                        )

                        CampoTexto(
                            titulo = "Problemática abordada",
                            valor = problematica,
                            onValorCambiado = { problematica = it.take(300) },
                            maximo = 300,
                            lineas = 4
                        )
                    }

                    // 2. Objetivos
                    Seccion(
                        titulo = secciones[1],
                        descripcion = descripciones[1],
                        icono = iconos[1],
                        abierta = seccionesAbiertas[1],
                        onToggle = { seccionesAbiertas[1] = !seccionesAbiertas[1] },
                        colorIcono = GreenContainer
                    ) {
                        CampoTexto(
                            titulo = "Objetivo General",
                            valor = objetivoGeneral,
                            onValorCambiado = { objetivoGeneral = it },
                            lineas = 2
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EtiquetaCampo(
                                "Objetivos Específicos",
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${objetivos.size} definidos",
                                color = Primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        objetivos.forEachIndexed { index, objetivo ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(FieldColor, RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "✓",
                                    color = GreenColor,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    objetivo,
                                    modifier = Modifier.weight(1f),
                                    color = TextColor,
                                    fontSize = 13.sp
                                )
                                TextButton(
                                    onClick = { objetivos.removeAt(index) },
                                    contentPadding = PaddingValues(4.dp)
                                ) {
                                    Text("✕", color = SecondaryText)
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = nuevoObjetivo,
                                onValueChange = { nuevoObjetivo = it },
                                modifier = Modifier.weight(1f),
                                placeholder = {
                                    Text("Añadir objetivo medible...", fontSize = 12.sp)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = coloresCampo()
                            )

                            Button(
                                onClick = {
                                    if (nuevoObjetivo.isNotBlank()) {
                                        objetivos.add(nuevoObjetivo.trim())
                                        nuevoObjetivo = ""
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(
                                    horizontal = 12.dp,
                                    vertical = 12.dp
                                )
                            ) {
                                Text("+ Añadir", fontSize = 12.sp)
                            }
                        }
                    }

                    // 3. Alcance y restricciones
                    Seccion(
                        titulo = secciones[2],
                        descripcion = descripciones[2],
                        icono = iconos[2],
                        abierta = seccionesAbiertas[2],
                        onToggle = { seccionesAbiertas[2] = !seccionesAbiertas[2] }
                    ) {
                        CampoTexto(
                            titulo = "Frontera del MVP (En alcance)",
                            valor = alcance,
                            onValorCambiado = { alcance = it },
                            lineas = 3
                        )

                        CampoTexto(
                            titulo = "Restricciones de Hardware y Red",
                            valor = restricciones,
                            onValorCambiado = { restricciones = it },
                            lineas = 3
                        )
                    }

                    // 4. Stack tecnológico
                    Seccion(
                        titulo = secciones[3],
                        descripcion = descripciones[3],
                        icono = iconos[3],
                        abierta = seccionesAbiertas[3],
                        onToggle = { seccionesAbiertas[3] = !seccionesAbiertas[3] }
                    ) {
                        EtiquetaCampo("Tecnologías Seleccionadas")

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FieldColor, RoundedCornerShape(14.dp))
                                .padding(10.dp)
                        ) {
                            tecnologias.chunked(2).forEach { grupo ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    grupo.forEach { tecnologia ->
                                        val esVerde = tecnologia in listOf(
                                            "PostgreSQL",
                                            "Spring Boot",
                                            "Firebase",
                                            "Kotlin"
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = if (esVerde) Color(0xFFD5FFEC) else PrimaryContainer,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(
                                                    horizontal = 10.dp,
                                                    vertical = 7.dp
                                                ),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "●",
                                                    fontSize = 9.sp,
                                                    color = if (esVerde) GreenColor else Primary
                                                )
                                                Spacer(Modifier.width(5.dp))
                                                Text(
                                                    tecnologia,
                                                    modifier = Modifier.weight(1f),
                                                    fontSize = 11.sp,
                                                    color = TextColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                TextButton(
                                                    onClick = { tecnologias.remove(tecnologia) },
                                                    contentPadding = PaddingValues(0.dp),
                                                    modifier = Modifier.size(22.dp)
                                                ) {
                                                    Text("×", fontSize = 17.sp, color = TextColor)
                                                }
                                            }
                                        }
                                    }

                                    if (grupo.size == 1) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                            }
                        }

                        EtiquetaCampo("Sugerencias recomendadas para TECSUP:")

                        if (sugerencias.isEmpty()) {
                            Text(
                                "Cargando catálogo de tecnologías...",
                                fontSize = 12.sp,
                                color = SecondaryText
                            )
                        }

                        sugerencias.chunked(2).forEach { grupo ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                grupo.forEach { tecnologia ->
                                    val seleccionada = tecnologia in tecnologias

                                    SuggestionChip(
                                        onClick = {
                                            if (seleccionada) {
                                                tecnologias.remove(tecnologia)
                                            } else {
                                                tecnologias.add(tecnologia)
                                            }
                                        },
                                        label = {
                                            Text(
                                                (if (seleccionada) "✓ " else "+ ") + tecnologia,
                                                fontSize = 11.sp
                                            )
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = if (seleccionada) {
                                                Color(0xFFD5FFEC)
                                            } else {
                                                FieldColor
                                            }
                                        ),
                                        border = null
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { mostrarDialogoTecnologia = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+ Agregar otra tecnología")
                        }
                    }

                    // 5. Requerimientos funcionales
                    Seccion(
                        titulo = secciones[4],
                        descripcion = descripciones[4],
                        icono = iconos[4],
                        abierta = seccionesAbiertas[4],
                        onToggle = { seccionesAbiertas[4] = !seccionesAbiertas[4] }
                    ) {
                        Text(
                            "Define los casos de uso principales del proyecto.",
                            fontSize = 13.sp,
                            color = SecondaryText
                        )

                        requerimientos.forEachIndexed { index, requerimiento ->
                            var marcado by remember(requerimiento) {
                                mutableStateOf(false)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(FieldColor, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = marcado,
                                    onCheckedChange = { marcado = it }
                                )
                                Text(
                                    requerimiento,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 12.sp,
                                    color = TextColor
                                )
                                TextButton(
                                    onClick = { requerimientos.removeAt(index) }
                                ) {
                                    Text("×", color = RedColor)
                                }
                            }
                        }

                        var nuevoRequerimiento by remember {
                            mutableStateOf("")
                        }

                        OutlinedTextField(
                            value = nuevoRequerimiento,
                            onValueChange = { nuevoRequerimiento = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Nuevo requerimiento funcional") },
                            placeholder = {
                                Text("Ej. RF-04: Gestión de proyectos")
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = coloresCampo()
                        )

                        Button(
                            onClick = {
                                if (nuevoRequerimiento.isNotBlank()) {
                                    requerimientos.add(nuevoRequerimiento.trim())
                                    nuevoRequerimiento = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+ Añadir requerimiento")
                        }
                    }

                    // 6. Plan y entrega
                    Seccion(
                        titulo = secciones[5],
                        descripcion = descripciones[5],
                        icono = iconos[5],
                        abierta = seccionesAbiertas[5],
                        onToggle = { seccionesAbiertas[5] = !seccionesAbiertas[5] },
                        colorIcono = Color(0xFFFFDAD6)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                EtiquetaCampo("Cantidad de Sprints")

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(FieldColor, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "$cantidadSprints Sprints",
                                        fontSize = 13.sp,
                                        color = TextColor
                                    )
                                    Column {
                                        TextButton(
                                            onClick = {
                                                if (cantidadSprints < 12) cantidadSprints++
                                            },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("⌃", color = TextColor)
                                        }
                                        TextButton(
                                            onClick = {
                                                if (cantidadSprints > 1) cantidadSprints--
                                            },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("⌄", color = TextColor)
                                        }
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                EtiquetaCampo("Entrega Final")

                                OutlinedTextField(
                                    value = fechaEntrega,
                                    onValueChange = { fechaEntrega = it.take(10) },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("DD/MM/AAAA") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = coloresCampo()
                                )
                            }
                        }

                        Text(
                            "Duración estimada: ${cantidadSprints * 2} semanas",
                            fontSize = 12.sp,
                            color = SecondaryText
                        )
                    }
                }
            }
        }

        if (mostrarDialogoTecnologia) {
            AlertDialog(
                onDismissRequest = { mostrarDialogoTecnologia = false },
                title = { Text("Agregar tecnología") },
                text = {
                    OutlinedTextField(
                        value = nuevaTecnologia,
                        onValueChange = { nuevaTecnologia = it },
                        label = { Text("Nombre de la tecnología") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val tecnologia = nuevaTecnologia.trim()
                            if (tecnologia.isNotEmpty() && tecnologia !in tecnologias) {
                                tecnologias.add(tecnologia)
                            }
                            nuevaTecnologia = ""
                            mostrarDialogoTecnologia = false
                        }
                    ) {
                        Text("Agregar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { mostrarDialogoTecnologia = false }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (mostrarConfirmacion) {
            AlertDialog(
                onDismissRequest = { mostrarConfirmacion = false },
                title = { Text("Guardar proyecto") },
                text = {
                    Text(
                        "¿Deseas guardar \"$nombre\" con " +
                            "${objetivos.size} objetivos, " +
                            "${tecnologias.size} tecnologías y " +
                            "${requerimientos.size} requerimientos?"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (nombre.isBlank()) {
                                mostrarConfirmacion = false
                                mensajeError = "El nombre del proyecto es obligatorio."
                            } else {
                                val request = ProyectoRequest(
                                    nombre = nombre.trim(),
                                    descripcion = descripcion.ifBlank { null },
                                    problematica = problematica.ifBlank { null },
                                    objetivoGeneral = objetivoGeneral.ifBlank { null },
                                    objetivosEspecificos = objetivos.joinToString("\n").ifBlank { null },
                                    alcance = alcance.ifBlank { null },
                                    restricciones = restricciones.ifBlank { null },
                                    fechaEntrega = convertirFecha(fechaEntrega),
                                    tecnologiasIds = tecnologias.mapNotNull { nombreTec ->
                                        tecnologiasCatalogo.firstOrNull {
                                            it.nombre.equals(nombreTec.trim(), ignoreCase = true)
                                        }?.id
                                    },
                                    requerimientosFuncionales = requerimientos.map {
                                        parsearRequerimiento(it)
                                    },
                                    requerimientosNoFuncionales = null,
                                    casosDeUso = null
                                )
                                mostrarConfirmacion = false
                                viewModel.crearProyecto(request)
                            }
                        }
                    ) {
                        Text("Confirmar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { mostrarConfirmacion = false }
                    ) {
                        Text("Volver")
                    }
                }
            )
        }

        if (guardando) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Guardando proyecto") },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Enviando datos al servidor...")
                    }
                },
                confirmButton = {}
            )
        }

        val errorMostrado = mensajeError
            ?: (uiState as? CrearProyectoUiState.Error)?.message
        if (errorMostrado != null) {
            AlertDialog(
                onDismissRequest = {
                    mensajeError = null
                    viewModel.resetState()
                },
                title = { Text("No se pudo guardar") },
                text = { Text(errorMostrado) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            mensajeError = null
                            viewModel.resetState()
                        }
                    ) {
                        Text("Aceptar")
                    }
                }
            )
        }
    }
}

private fun convertirFecha(fecha: String): String? {
    val partes = fecha.trim().split("/")
    if (partes.size == 3) {
        val dia = partes[0].toIntOrNull()
        val mes = partes[1].toIntOrNull()
        val anio = partes[2].toIntOrNull()
        if (dia != null && mes != null && anio != null) {
            return "%04d-%02d-%02d".format(anio, mes, dia)
        }
    }
    return null
}

private fun parsearRequerimiento(texto: String): RequerimientoFuncionalDto {
    val idx = texto.indexOf(':')
    return if (idx > 0) {
        RequerimientoFuncionalDto(
            codigo = texto.substring(0, idx).trim(),
            descripcion = texto.substring(idx + 1).trim()
        )
    } else {
        RequerimientoFuncionalDto(codigo = null, descripcion = texto.trim())
    }
}

@Composable
fun Seccion(
    titulo: String,
    descripcion: String,
    icono: String,
    abierta: Boolean,
    onToggle: () -> Unit,
    colorIcono: Color = PrimaryContainer,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column {
            Surface(
                onClick = onToggle,
                color = if (abierta) Color(0xFFF0F1FF) else SurfaceColor
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(colorIcono, RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            icono,
                            fontSize = 21.sp,
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            titulo,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextColor
                        )
                        Text(
                            descripcion,
                            fontSize = 12.sp,
                            color = SecondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        if (abierta) "⌃" else "⌄",
                        fontSize = 23.sp,
                        color = SecondaryText
                    )
                }
            }

            if (abierta) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(15.dp),
                    content = contenido
                )
            }
        }
    }
}

@Composable
fun EtiquetaCampo(
    texto: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = texto,
        modifier = modifier.padding(bottom = 5.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = SecondaryText
    )
}

@Composable
fun CampoTexto(
    titulo: String,
    valor: String,
    onValorCambiado: (String) -> Unit,
    maximo: Int? = null,
    lineas: Int = 2
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EtiquetaCampo(titulo, modifier = Modifier.weight(1f))
            if (maximo != null) {
                Text(
                    "${valor.length}/$maximo",
                    fontSize = 11.sp,
                    color = SecondaryText
                )
            }
        }

        OutlinedTextField(
            value = valor,
            onValueChange = {
                onValorCambiado(
                    if (maximo != null) it.take(maximo) else it
                )
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = lineas,
            maxLines = lineas + 2,
            shape = RoundedCornerShape(12.dp),
            colors = coloresCampo()
        )
    }
}

@Composable
fun Contador(
    actual: Int,
    maximo: Int
) {
    Text(
        text = "$actual/$maximo",
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 3.dp),
        textAlign = TextAlign.End,
        fontSize = 10.sp,
        color = SecondaryText
    )
}

@Composable
fun coloresCampo() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = FieldColor,
    unfocusedContainerColor = FieldColor,
    disabledContainerColor = FieldColor,
    focusedBorderColor = Primary,
    unfocusedBorderColor = Color.Transparent,
    cursorColor = Primary,
    focusedTextColor = TextColor,
    unfocusedTextColor = TextColor
)
