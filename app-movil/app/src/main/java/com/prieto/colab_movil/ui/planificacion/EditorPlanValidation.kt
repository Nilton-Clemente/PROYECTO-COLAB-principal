package com.prieto.colab_movil.ui.planificacion

private val PRIORIDADES = setOf("ALTA", "MEDIA", "BAJA")

/** Un problema de validación del editor, con la clave del campo y un mensaje para el usuario. */
data class ErrorEdicion(val clave: String, val mensaje: String)

private fun tieneCiclos(ids: Set<String>, dependencias: Map<String, List<String>>): Boolean {
    val indegree = ids.associateWith { 0 }.toMutableMap()
    val dependientes = mutableMapOf<String, MutableList<String>>()
    for ((id, deps) in dependencias) {
        val validas = deps.filter { it in ids }
        indegree[id] = validas.size
        for (dep in validas) dependientes.getOrPut(dep) { mutableListOf() }.add(id)
    }
    val cola = ArrayDeque<String>()
    indegree.filterValues { it == 0 }.keys.forEach(cola::add)
    var visitados = 0
    while (cola.isNotEmpty()) {
        val actual = cola.removeFirst()
        visitados++
        for (dep in dependientes[actual].orEmpty()) {
            val nuevo = indegree.getValue(dep) - 1
            indegree[dep] = nuevo
            if (nuevo == 0) cola.add(dep)
        }
    }
    return visitados != ids.size
}

/**
 * Valida el borrador del editor. Espeja las reglas que el backend aplica al
 * guardar (PUT /planificacion), salvo las fechas, que solo recalcula el servidor.
 */
fun validarEditor(etapas: List<EtapaEditableUi>): List<ErrorEdicion> {
    val errores = mutableListOf<ErrorEdicion>()
    val tareas = etapas.flatMap { it.tareas }
    val ids = tareas.map { it.localId }.toSet()

    if (etapas.isEmpty()) {
        errores += ErrorEdicion("global", "Agrega al menos una etapa.")
    }

    etapas.forEach { etapa ->
        if (etapa.nombre.isBlank()) {
            errores += ErrorEdicion("etapa:${etapa.localId}:nombre", "La etapa necesita un nombre.")
        }
        etapa.tareas.forEach { tarea ->
            val prefijo = "tarea:${tarea.localId}"
            if (tarea.nombre.isBlank()) {
                errores += ErrorEdicion("$prefijo:nombre", "La tarea necesita un título.")
            }
            val dias = tarea.duracionDias.toIntOrNull()
            if (dias == null || dias <= 0) {
                errores += ErrorEdicion("$prefijo:duracionDias", "La duración debe ser mayor que cero.")
            }
            if (tarea.prioridad.trim().uppercase() !in PRIORIDADES) {
                errores += ErrorEdicion("$prefijo:prioridad", "Selecciona una prioridad.")
            }
            if (tarea.dependencias.contains(tarea.localId)) {
                errores += ErrorEdicion("$prefijo:dependencias", "No puede depender de sí misma.")
            }
            tarea.dependencias.forEach { dep ->
                if (dep !in ids) {
                    errores += ErrorEdicion("$prefijo:dependencias", "Depende de una tarea que ya no existe.")
                }
            }
        }
    }

    val dependencias = tareas.associate { it.localId to it.dependencias }
    if (tieneCiclos(ids, dependencias)) {
        errores += ErrorEdicion("global", "Hay dependencias con ciclos: una tarea se bloquea a sí misma de forma indirecta.")
    }

    return errores
}