package com.colab.backend.dto;

import java.time.LocalDateTime;

public record NotificacionResponse(
        Long id,
        String tipoEvento,
        String mensaje,
        boolean leida,
        LocalDateTime fechaCreacion) {}
