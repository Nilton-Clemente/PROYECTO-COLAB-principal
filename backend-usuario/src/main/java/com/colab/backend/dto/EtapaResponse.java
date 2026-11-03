package com.colab.backend.dto;

import java.util.List;

public record EtapaResponse(
        Long id,
        String nombre,
        Integer orden,
        List<TareaResponse> tareas) {}
