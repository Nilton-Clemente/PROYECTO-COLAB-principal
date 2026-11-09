package com.colab.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RegistroAvanceRequest(@NotNull @Positive Integer minutos, @Size(max = 1000) String nota) {}