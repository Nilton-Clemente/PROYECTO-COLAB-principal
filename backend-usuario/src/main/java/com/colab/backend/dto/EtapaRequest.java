package com.colab.backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Datos para crear o editar una fase (etapa) de la planificación de un proyecto.
 */
public record EtapaRequest(
        @NotBlank String nombre,
        Integer orden) {}
