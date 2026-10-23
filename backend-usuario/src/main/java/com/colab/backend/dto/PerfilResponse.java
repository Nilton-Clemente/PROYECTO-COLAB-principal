package com.colab.backend.dto;

import java.util.List;

public record PerfilResponse(String email, String nombre, List<HabilidadDto> habilidades) {}
