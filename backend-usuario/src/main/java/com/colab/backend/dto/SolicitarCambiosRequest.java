package com.colab.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record SolicitarCambiosRequest(@NotBlank String comentario) {}
