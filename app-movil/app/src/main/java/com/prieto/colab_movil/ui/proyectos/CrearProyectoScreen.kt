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
import com.prieto.colab_movil.data.model.CasoUsoDto
import com.prieto.colab_movil.data.model.ProyectoRequest
import com.prieto.colab_movil.data.model.ProyectoResponseDto
import com.prieto.colab_movil.data.model.RequerimientoFuncionalDto
import com.prieto.colab_movil.data.model.RequerimientoNoFuncionalDto

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
    proyectoId: Long? = null,
    proyectoInicial: ProyectoResponseDto? = null,
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
        var nombre by remember(proyectoInicial) { mutableStateOf(proyectoInicial?.nombre ?: "") }
        var descripcion by remember(proyectoInicial) { mutableStateOf(proyectoInicial?.descripcion ?: "") }
        var problematica by remember(proyectoInicial) { mutableStateOf(proyectoInicial?.problematica ?: "") }
        var objetivoGeneral by remember(proyectoInicial) { mutableStateOf(proyectoInicial?.objetivoGeneral ?: "") }
        var alcance by remember(proyectoInicial) { mutableStateOf(proyectoInicial?.alcance ?: "") }
        var restricciones by remember(proyectoInicial) { mutableStateOf(proyectoInicial?.restricciones ?: "") }
        var fechaEntrega by remember(proyectoInicial) { mutableStateOf(formatearFechaParaEdicion(proyectoInicial?.fechaEntrega)) }
        var nuevoObjetivo by remember { mutableStateOf("") }
        var nuevoRequerimiento by remember { mutableStateOf("") }
        var nuevoRnfDescripcion by remember { mutableStateOf("") }
        var mostrarConfirmacion by remember { mutableStateOf(false) }
        var mostrarImportacion by remember { mutableStateOf(false) }
        var mensajeImportacion by remember { mutableStateOf<String?>(null) }
        var mensajeError by remember { mutableStateOf<String?>(null) }

        val objetivos = remember(proyectoInicial) {
            mutableStateListOf<String>().apply {
                addAll(proyectoInicial?.objetivosEspecificos
                    ?.split("\n")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?: emptyList())
            }
        }
        val tecnologias = remember(proyectoInicial) {
            mutableStateListOf<String>().apply {
                addAll(proyectoInicial?.tecnologias?.map { it.nombre } ?: emptyList())
            }
        }
        val requerimientos = remember(proyectoInicial) {
            mutableStateListOf<RequerimientoFuncionalDto>().apply {
                addAll(proyectoInicial?.requerimientosFuncionales.orEmpty())
            }
        }
        val requerimientosNoFuncionales = remember(proyectoInicial) {
            mutableStateListOf<RequerimientoNoFuncionalDto>().apply {
                addAll(proyectoInicial?.requerimientosNoFuncionales ?: emptyList())
            }
        }

        val casosDeUso = remember(proyectoInicial) {
            mutableStateListOf<CasoUsoDto>().apply {
                addAll(proyectoInicial?.casosDeUso.orEmpty())
            }
        }

        val seccionesAbiertas = remember {
            mutableStateListOf(true, true, false, true, false, false, false, true)
        }

        val secciones = listOf(
            "Datos Generales",
            "Objetivos del Software",
            "Alcance y Restricciones",
            "Stack Tecnológico",
            "Requerimientos Funcionales",
            "Requerimientos No Funcionales",
            "Casos de Uso",
            "Plan y Entrega"
        )

        val descripciones = listOf(
            "Identificación y contexto de la iniciativa",
            "Hitos de valor técnico y alcance funcional",
            "Fronteras técnicas y dependencias externas",
            "Arquitectura de lenguajes, frameworks y nube",
            "Funciones y comportamientos del sistema",
            "Calidad, seguridad, rendimiento y usabilidad",
            "Actores, condiciones y flujos de interacción",
            "Fecha final del proyecto"
        )

        val iconos = listOf("▣", "◎", "⌗", "◇", "☑", "⚙", "◎", "▦")

        val sugerencias = tecnologiasCatalogo.map { it.nombre }

        val progreso = listOf(nombre, descripcion, objetivoGeneral)
            .count { it.isNotBlank() }
        val porcentaje = (progreso * 100 / 3).coerceIn(0, 100)

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
                                if (proyectoId != null) "Guardar Cambios" else "Guardar Proyecto",
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
                        text = if (proyectoId != null) "Editar Proyecto" else "Crear Proyecto",
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

                    if (proyectoId == null) {
                        OutlinedButton(
                            onClick = { mostrarImportacion = true },
                            enabled = !guardando,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Importar JSON") }
                        mensajeImportacion?.let {
                            Text(it, color = GreenColor, fontSize = 13.sp)
                        }
                    }

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
                                    "Datos esenciales",
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
                                    requerimiento.codigo?.takeIf { it.isNotBlank() }
                                        ?.let { "$it: ${requerimiento.descripcion}" } ?: requerimiento.descripcion,
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
                                    requerimientos.add(parsearRequerimiento(nuevoRequerimiento.trim()))
                                    nuevoRequerimiento = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+ Añadir requerimiento")
                        }
                    }

                    // 6. Requerimientos no funcionales
                    Seccion(
                        titulo = secciones[5],
                        descripcion = descripciones[5],
                        icono = iconos[5],
                        abierta = seccionesAbiertas[5],
                        onToggle = { seccionesAbiertas[5] = !seccionesAbiertas[5] },
                        colorIcono = GreenContainer
                    ) {
                        Text(
                            "Define atributos de calidad como seguridad, rendimiento, usabilidad o disponibilidad.",
                            fontSize = 13.sp,
                            color = SecondaryText
                        )

                        requerimientosNoFuncionales.forEachIndexed { index, requerimiento ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(FieldColor, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Requerimiento no funcional",
                                        modifier = Modifier.weight(1f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextColor
                                    )
                                    TextButton(
                                        onClick = { requerimientosNoFuncionales.removeAt(index) },
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text("×", color = RedColor)
                                    }
                                }
                                Text(requerimiento.descripcion, fontSize = 12.sp, color = SecondaryText)
                            }
                        }

                        CampoTexto(
                            titulo = "Descripción del requerimiento no funcional *",
                            valor = nuevoRnfDescripcion,
                            onValorCambiado = { nuevoRnfDescripcion = it },
                            lineas = 2
                        )

                        Button(
                            onClick = {
                                val descripcionRnf = nuevoRnfDescripcion.trim()
                                if (descripcionRnf.isNotEmpty()) {
                                    requerimientosNoFuncionales.add(
                                        RequerimientoNoFuncionalDto(
                                            codigo = null,
                                            categoria = null,
                                            descripcion = descripcionRnf
                                        )
                                    )
                                    nuevoRnfDescripcion = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+ Añadir requerimiento no funcional")
                        }
                    }

                    // 7. Casos de uso
                    Seccion(
                        titulo = secciones[6],
                        descripcion = descripciones[6],
                        icono = iconos[6],
                        abierta = seccionesAbiertas[6],
                        onToggle = { seccionesAbiertas[6] = !seccionesAbiertas[6] }
                    ) {
                        CasosDeUsoFormulario(
                            casosDeUso = casosDeUso,
                            onAgregar = { casosDeUso.add(CasoUsoDto(nombre = "")) },
                            onCambiar = { index, caso -> casosDeUso[index] = caso },
                            onEliminar = { index -> casosDeUso.removeAt(index) }
                        )
                    }

                    // 8. Plan y entrega
                    Seccion(
                        titulo = secciones[7],
                        descripcion = descripciones[7],
                        icono = iconos[7],
                        abierta = seccionesAbiertas[7],
                        onToggle = { seccionesAbiertas[7] = !seccionesAbiertas[7] },
                        colorIcono = Color(0xFFFFDAD6)
                    ) {
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
            }
        }

        if (mostrarImportacion && proyectoId == null) {
            ImportarProyectoJsonDialog(
                catalogo = tecnologiasCatalogo,
                onRecargarCatalogo = { viewModel.cargarTecnologias() },
                onCerrar = { mostrarImportacion = false },
                onImportar = { importado ->
                    val datos = importado.request
                    nombre = datos.nombre
                    descripcion = datos.descripcion.orEmpty()
                    problematica = datos.problematica.orEmpty()
                    objetivoGeneral = datos.objetivoGeneral.orEmpty()
                    alcance = datos.alcance.orEmpty()
                    restricciones = datos.restricciones.orEmpty()
                    fechaEntrega = formatearFechaParaEdicion(datos.fechaEntrega)
                    objetivos.clear()
                    objetivos.addAll(datos.objetivosEspecificos.orEmpty().lines().map { it.trim() }.filter { it.isNotEmpty() })
                    tecnologias.clear()
                    tecnologias.addAll(importado.tecnologias)
                    requerimientos.clear()
                    requerimientos.addAll(datos.requerimientosFuncionales.orEmpty())
                    requerimientosNoFuncionales.clear()
                    requerimientosNoFuncionales.addAll(datos.requerimientosNoFuncionales.orEmpty())
                    casosDeUso.clear()
                    casosDeUso.addAll(datos.casosDeUso.orEmpty())
                    nuevoObjetivo = ""
                    nuevoRequerimiento = ""
                    nuevoRnfDescripcion = ""
                    mensajeError = null
                    viewModel.resetState()
                    mostrarImportacion = false
                    mensajeImportacion = "Datos importados. Revisa el formulario y pulsa Guardar Proyecto para registrarlo."
                }
            )
        }

        if (mostrarConfirmacion) {
            AlertDialog(
                onDismissRequest = { mostrarConfirmacion = false },
                title = { Text(if (proyectoId != null) "Guardar cambios" else "Guardar proyecto") },
                text = {
                    Text(
                        "¿Deseas guardar \"$nombre\" con " +
                            "${objetivos.size} objetivos, " +
                            "${tecnologias.size} tecnologías y " +
                            "${requerimientos.size} requerimientos funcionales y " +
                            "${requerimientosNoFuncionales.size} no funcionales, y " +
                            "${casosDeUso.size} casos de uso?"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (nombre.isBlank()) {
                                mostrarConfirmacion = false
                                mensajeError = "El nombre del proyecto es obligatorio."
                            } else if (casosDeUso.any { it.nombre.isBlank() }) {
                                mostrarConfirmacion = false
                                seccionesAbiertas[6] = true
                                mensajeError = "El nombre de cada caso de uso es obligatorio. Completa o elimina los casos vacíos."
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
                                    requerimientosFuncionales = requerimientos.toList(),
                                    requerimientosNoFuncionales = requerimientosNoFuncionales.toList(),
                                    casosDeUso = casosDeUso.map { it.normalizado() }
                                )
                                mostrarConfirmacion = false
                                if (proyectoId != null) {
                                    viewModel.editarProyecto(proyectoId, request)
                                } else {
                                    viewModel.crearProyecto(request)
                                }
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

private fun formatearFechaParaEdicion(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    val partes = iso.split("-")
    return if (partes.size == 3) "${partes[2]}/${partes[1]}/${partes[0]}" else iso
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
