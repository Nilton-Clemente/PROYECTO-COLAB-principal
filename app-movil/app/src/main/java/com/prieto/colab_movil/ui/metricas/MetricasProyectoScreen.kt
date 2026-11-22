package com.prieto.colab_movil.ui.metricas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prieto.colab_movil.data.model.MetricasProyectoDto
import com.prieto.colab_movil.data.model.estadoProyecto
import com.prieto.colab_movil.data.repository.MetricasProyectoDatos
import com.prieto.colab_movil.ui.components.ColabActualizar
import com.prieto.colab_movil.ui.components.ColabBarraSecundaria
import com.prieto.colab_movil.ui.proyectos.EstadoProyectoBadge
import com.prieto.colab_movil.ui.proyectos.iniciales

private val Fondo = Color(0xFFFAF8FF)
private val Suave = Color(0xFFF2F3FF)
private val Primario = Color(0xFF3525CD)
private val Verde = Color(0xFF006C49)
private val Rojo = Color(0xFF93000A)
private val Gris = Color(0xFF464555)
private val Pista = Color(0xFFE2E7FF)

@Composable
fun MetricasProyectoScreen(
    proyectoId: Long,
    onBack: () -> Unit,
    viewModel: MetricasProyectoViewModel = viewModel()
) {
    val estado by viewModel.uiState.collectAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, proyectoId, viewModel) {
        val observer = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) viewModel.cargar(proyectoId)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = Fondo,
        topBar = {
            ColabBarraSecundaria("Métricas del proyecto", onVolver = onBack, descripcionVolver = "Volver al proyecto")
        }
    ) { padding ->
        when (val actual = estado) {
            MetricasProyectoUiState.Cargando -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primario)
            }
            is MetricasProyectoUiState.Error -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(actual.mensaje, color = Rojo)
                if (actual.permiteReintentar) TextButton(onClick = { viewModel.cargar(proyectoId) }) { Text("Reintentar") }
            }
            is MetricasProyectoUiState.Listo -> ColabActualizar(
                actualizando = actual.actualizando, habilitado = !actual.actualizando,
                onActualizar = { viewModel.cargar(proyectoId) }, modifier = Modifier.padding(padding)
            ) {
                ContenidoMetricas(actual.datos, actual.aviso, onBack)
            }
        }
    }
}

@Composable
private fun ContenidoMetricas(datos: MetricasProyectoDatos, aviso: String?, onBack: () -> Unit) {
    val proyecto = datos.proyecto
    val metricas = datos.metricas
    val equipoPorId = datos.equipo.associateBy { it.integranteId }
    LazyColumn(
        Modifier.fillMaxSize().testTag("metricas-contenido"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (aviso != null) item { Text(aviso, color = Rojo, style = MaterialTheme.typography.bodySmall) }
        item {
            TarjetaMetricas(color = Suave) {
                EstadoProyectoBadge(proyecto.estadoProyecto)
                Text(proyecto.nombre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Entrega: ${fechaEntregaVisible(proyecto.fechaEntrega)}", color = Gris)
                if (entregaVencida(proyecto)) Text("Fecha de entrega vencida", color = Rojo, fontWeight = FontWeight.SemiBold)
                if (proyecto.estadoProyecto.cerrado) Text("Consulta del avance registrado antes del cierre.", color = Gris)
            }
        }
        if (metricas.totalTareas == 0L) {
            item {
                TarjetaMetricas {
                    Titulo("Aún no hay tareas para medir")
                    Text("Las métricas estarán disponibles cuando el proyecto tenga una planificación con tareas.", color = Gris)
                    TextButton(onClick = onBack) { Text("Volver al detalle del proyecto") }
                }
            }
        } else {
            item { ProgresoGeneral(metricas) }
            item { DistribucionTareas(metricas) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Titulo("Avance por integrante")
                    Text("Tareas completadas sobre las asignadas a cada integrante activo.", color = Gris, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (metricas.porResponsable.isNullOrEmpty()) item { Text("No hay integrantes activos para mostrar.", color = Gris) }
            items(metricas.porResponsable.orEmpty(), key = { it.integranteId }) { responsable ->
                val integrante = equipoPorId[responsable.integranteId]
                AvanceIntegrante(
                    nombre = nombreResponsable(responsable, integrante),
                    creador = integrante?.rol == "CREADOR",
                    completadas = responsable.completadas,
                    total = responsable.totalTareas
                )
            }
        }
    }
}

@Composable
private fun ProgresoGeneral(metricas: MetricasProyectoDto) {
    TarjetaMetricas {
        Titulo("Progreso general")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val avance = metricas.porcentajeCompletadas
            Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { (avance / 100).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxSize(), color = Primario, trackColor = Pista, strokeWidth = 6.dp
                )
                Text(textoPorcentaje(avance), color = Primario, fontWeight = FontWeight.Bold)
            }
            Text("${metricas.completadas} de ${metricas.totalTareas} tareas completadas", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DistribucionTareas(metricas: MetricasProyectoDto) {
    val estados = listOf(
        Triple("Completadas", metricas.completadas, Verde), Triple("En progreso", metricas.enProgreso, Primario),
        Triple("Pendientes", metricas.pendientes, Gris), Triple("Bloqueadas", metricas.bloqueadas, Rojo)
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Titulo("Estado de las tareas")
        estados.chunked(2).forEach { fila ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { (titulo, cantidad, color) ->
                    val porcentaje = requireNotNull(porcentajeTareas(cantidad, metricas.totalTareas))
                    TarjetaMetricas(Modifier.weight(1f)) {
                        Text(titulo, color = Gris, style = MaterialTheme.typography.bodySmall)
                        Text("$cantidad · ${textoPorcentaje(porcentaje)}", color = color, fontWeight = FontWeight.Bold)
                        BarraProgreso(porcentaje, color)
                    }
                }
            }
        }
    }
}

@Composable
private fun AvanceIntegrante(nombre: String, creador: Boolean, completadas: Long, total: Long) {
    val porcentaje = porcentajeTareas(completadas, total)
    TarjetaMetricas {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(34.dp).background(Pista, CircleShape), contentAlignment = Alignment.Center) {
                Text(iniciales(nombre), color = Primario, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
            Column(Modifier.weight(1f)) {
                Text(nombre, fontWeight = FontWeight.SemiBold)
                if (creador) Text("Creador", color = Primario, style = MaterialTheme.typography.bodySmall)
            }
            if (porcentaje != null) Text(textoPorcentaje(porcentaje), color = Primario, fontWeight = FontWeight.Bold)
        }
        if (porcentaje == null) Text("Sin tareas asignadas", color = Gris, style = MaterialTheme.typography.bodySmall)
        else {
            Text("$completadas de $total tareas completadas", color = Gris, style = MaterialTheme.typography.bodySmall)
            BarraProgreso(porcentaje, Primario)
        }
    }
}

@Composable
private fun BarraProgreso(porcentaje: Double, color: Color) {
    LinearProgressIndicator(progress = { (porcentaje / 100).toFloat().coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().height(6.dp), color = color, trackColor = Pista)
}

@Composable
private fun TarjetaMetricas(modifier: Modifier = Modifier, color: Color = Color.White, contenido: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = contenido)
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}