package com.prieto.colab_movil.ui.metricas

import com.prieto.colab_movil.data.model.EstadoProyecto
import com.prieto.colab_movil.data.model.IntegranteResponseDto
import com.prieto.colab_movil.data.model.MetricasIntegranteDto
import com.prieto.colab_movil.data.model.ProyectoResponseDto
import com.prieto.colab_movil.data.model.estadoProyecto
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val locale = Locale.forLanguageTag("es-PE")

internal fun porcentajeTareas(cantidad: Long, total: Long): Double? =
    if (total > 0) cantidad.toDouble() / total * 100 else null

internal fun textoPorcentaje(porcentaje: Double): String {
    val formato = NumberFormat.getPercentInstance(locale).apply { maximumFractionDigits = 1 }
    return when {
        porcentaje >= 99.95 && porcentaje < 100 -> "< ${formato.format(1)}"
        porcentaje > 0 && porcentaje < 0.05 -> "< ${formato.format(0.001)}"
        else -> formato.format(porcentaje / 100)
    }
}

internal fun nombreResponsable(avance: MetricasIntegranteDto, integrante: IntegranteResponseDto?): String =
    integrante?.nombre?.takeIf { it.isNotBlank() } ?: avance.email?.takeIf { it.isNotBlank() } ?: "Integrante"

private fun formatoIso() = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }

internal fun fechaEntregaVisible(fecha: String?): String {
    if (fecha.isNullOrBlank()) return "Sin fecha de entrega"
    return runCatching {
        val dia = requireNotNull(formatoIso().parse(fecha))
        SimpleDateFormat("d MMM yyyy", locale).format(dia)
    }.getOrDefault(fecha)
}

internal fun entregaVencida(proyecto: ProyectoResponseDto, hoy: Date = Date()): Boolean =
    proyecto.estadoProyecto == EstadoProyecto.EN_DESARROLLO &&
        proyecto.fechaEntrega?.let { it < formatoIso().format(hoy) } == true