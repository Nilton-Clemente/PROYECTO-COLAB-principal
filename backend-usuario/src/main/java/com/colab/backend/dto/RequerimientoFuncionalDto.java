package com.colab.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record RequerimientoFuncionalDto(
        @NotBlank String descripcion,
        String codigo) {}
