package com.colab.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record RequerimientoNoFuncionalDto(
        @NotBlank String descripcion,
        String codigo,
        String categoria) {}
