package com.colab.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record TareaEstadoRequest(@NotBlank String nuevoEstado) {}
