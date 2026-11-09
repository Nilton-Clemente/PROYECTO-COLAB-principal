package com.colab.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TareaRequest(
        @NotBlank String nombre,
        String descripcion,
        @NotNull Long etapaId,
        Long responsableId,
        @NotBlank String prioridad,
        @NotNull @Positive Integer duracionDias,
        Integer orden) {}
