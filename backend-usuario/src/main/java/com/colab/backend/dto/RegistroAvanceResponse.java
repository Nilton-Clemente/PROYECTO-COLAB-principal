package com.colab.backend.dto;

import java.time.LocalDateTime;

public record RegistroAvanceResponse(Long id, Long tareaId, Long usuarioId, String usuarioNombre,
                                    Integer minutos, LocalDateTime fecha, String nota) {}