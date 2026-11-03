package com.colab.backend.dto;

import java.util.List;

/**
 * Planificación ya persistida, devuelta al cliente.
 */
public record PlanificacionResponse(
        Long id,
        Long proyectoId,
        String estado,
        List<EtapaResponse> etapas) {}
