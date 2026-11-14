package com.colab.backend.dto;

import java.util.List;

/**
 * Planificación completa que envía el editor (CU-09) para reemplazar la
 * propuesta de forma atómica.
 *
 * <p>{@code id} es {@code null} para elementos nuevos. {@code key} es una
 * clave única dentro del request que identifica cada tarea para poder
 * resolver dependencias entre tareas nuevas o existentes.</p>
 */
public record PlanificacionUpdateRequest(List<EtapaUpdate> etapas) {

    public record EtapaUpdate(Long id, String nombre, Integer orden, List<TareaUpdate> tareas) {}

    public record TareaUpdate(
            Long id,
            String key,
            String nombre,
            String descripcion,
            Long responsableId,
            String prioridad,
            Integer duracionDias,
            List<String> dependencias) {}
}