package com.colab.backend.dto;

import java.util.List;

public record MetricasProyectoResponse(long totalTareas, long pendientes, long enProgreso,
        long completadas, long bloqueadas, double porcentajeCompletadas,
        List<MetricasIntegrante> porResponsable) {
    public record MetricasIntegrante(Long integranteId, String email, long totalTareas,
            long pendientes, long enProgreso, long completadas, long bloqueadas) {}
}