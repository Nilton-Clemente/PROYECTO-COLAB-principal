package com.prieto.colab_movil.ui.proyectos

import com.prieto.colab_movil.data.model.IntegranteResponseDto
import java.text.Normalizer
import java.util.Locale

enum class ParticipacionProyecto(val etiqueta: String) {
    CREADOR("Creado por mí"),
    PARTICIPANTE("Participante"),
    SIN_VERIFICAR("Rol por verificar")
}

internal fun determinarParticipacion(
    emailUsuario: String?,
    equipo: List<IntegranteResponseDto>
): ParticipacionProyecto {
    val email = emailUsuario?.trim()?.takeIf { it.isNotEmpty() }
        ?: return ParticipacionProyecto.SIN_VERIFICAR
    val integrante = equipo.firstOrNull {
        it.estado == "ACTIVO" && it.email.trim().equals(email, ignoreCase = true)
    }
    return when (integrante?.rol) {
        "CREADOR" -> ParticipacionProyecto.CREADOR
        "INTEGRANTE" -> ParticipacionProyecto.PARTICIPANTE
        else -> ParticipacionProyecto.SIN_VERIFICAR
    }
}

internal fun filtrarProyectos(
    proyectos: List<Proyecto>,
    estado: Filtro = Filtro.TODOS,
    busqueda: String = ""
): List<Proyecto> {
    val termino = normalizarBusqueda(busqueda)
    return proyectos.filter { proyecto ->
        estado.coincide(proyecto.estado) && (termino.isEmpty() || normalizarBusqueda(
            "${proyecto.codigo} ${proyecto.titulo} ${proyecto.descripcion.orEmpty()} ${proyecto.tecnologias.joinToString(" ") { it.nombre }}"
        ).contains(termino))
    }
}

private fun normalizarBusqueda(valor: String): String = Normalizer.normalize(valor, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).trim()