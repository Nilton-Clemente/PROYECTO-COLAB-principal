package com.colab.backend.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Vista de tarea para la gestión manual (CRUD), incluyendo los identificadores
 * de etapa y responsable necesarios para editar/reasignar.
 */
public record TareaDetalleResponse(
        Long id,
        String nombre,
        String descripcion,
        Long etapaId,
        String etapaNombre,
        Long responsableId,
        String responsableEmail,
        String prioridad,
        String estado,
        Integer duracionDias,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Integer orden,
        List<Long> dependencias) {}
