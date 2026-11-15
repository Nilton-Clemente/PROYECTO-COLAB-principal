package com.colab.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetContrasenaRequest(
        @NotBlank @Size(min = 6, max = 100) String contrasena) {}