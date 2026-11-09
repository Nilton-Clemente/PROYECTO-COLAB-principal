package com.colab.backend.dto;

import java.util.List;

public record HistorialAvanceResponse(long totalMinutos, List<RegistroAvanceResponse> registros) {}