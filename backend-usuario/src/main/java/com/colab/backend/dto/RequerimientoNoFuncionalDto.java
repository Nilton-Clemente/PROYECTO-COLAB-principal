package com.colab.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record RequerimientoNoFuncionalDto(String codigo, @NotBlank String descripcion, String categoria) {
    public RequerimientoNoFuncionalDto(String descripcion, String categoria) { this(null, descripcion, categoria); }
}
