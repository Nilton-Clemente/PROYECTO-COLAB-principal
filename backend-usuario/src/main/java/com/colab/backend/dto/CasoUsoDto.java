package com.colab.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CasoUsoDto(
        @NotBlank String nombre,
        String codigo,
        String descripcion) {}
