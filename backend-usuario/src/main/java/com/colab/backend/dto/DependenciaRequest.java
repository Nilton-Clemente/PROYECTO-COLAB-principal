package com.colab.backend.dto;

import jakarta.validation.constraints.NotNull;

public record DependenciaRequest(@NotNull Long tareaPredecesoraId) {}
