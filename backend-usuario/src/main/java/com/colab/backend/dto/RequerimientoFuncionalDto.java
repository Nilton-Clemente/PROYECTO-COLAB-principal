package com.colab.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record RequerimientoFuncionalDto(String codigo, @NotBlank String descripcion) {
    public RequerimientoFuncionalDto(String descripcion) { this(null, descripcion); }
}
