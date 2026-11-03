package com.colab.backend.dto;

import java.time.LocalDate;
import java.util.List;

public record TareaResponse(
        Long id,
        String nombre,
        String descripcion,
        String responsableEmail,
        String prioridad,
        String estado,
        Integer duracionDias,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        List<Long> dependencias) {}
