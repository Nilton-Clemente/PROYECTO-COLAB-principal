package com.colab.backend.dto;

import java.time.LocalDateTime;

public record ConfirmacionResponse(
        Long id,
        Long integranteId,
        String integranteEmail,
        String integranteNombre,
        String estado,
        String comentario,
        LocalDateTime fecha) {}
