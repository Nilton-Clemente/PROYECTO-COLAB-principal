package com.colab.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CasoUsoDto(
        @NotBlank String nombre,
        String descripcion,
        String actores,
        String precondiciones,
        String disparador,
        String flujoPrincipal,
        String flujosAlternativos,
        String excepciones,
        String postcondiciones,
        String reglasDeNegocio) {}
