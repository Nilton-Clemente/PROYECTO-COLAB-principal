package com.colab.backend.dto;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class RequerimientosDtoTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void rfSoloExponeDescripcion() {
        RequerimientoFuncionalDto rf = objectMapper.readValue(
                "{\"descripcion\":\"Permitir registrar usuarios\"}", RequerimientoFuncionalDto.class);
        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(rf)).size()).isEqualTo(1);
        assertThat(rf.descripcion()).isEqualTo("Permitir registrar usuarios");
    }

    @Test
    void rnfConservaCategoriaOpcionalSinCodigo() {
        RequerimientoNoFuncionalDto rnf = objectMapper.readValue(
                "{\"descripcion\":\"Proteger contraseñas\",\"categoria\":\"Seguridad\"}",
                RequerimientoNoFuncionalDto.class);
        var json = objectMapper.readTree(objectMapper.writeValueAsString(rnf));
        assertThat(json.has("codigo")).isFalse();
        assertThat(rnf.categoria()).isEqualTo("Seguridad");
        assertThat(objectMapper.readValue("{\"descripcion\":\"Responder rápido\"}",
                RequerimientoNoFuncionalDto.class).categoria()).isNull();
    }

    @Test
    void descripcionesSiguenSiendoObligatorias() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(new RequerimientoFuncionalDto(" "))).hasSize(1);
            assertThat(factory.getValidator().validate(new RequerimientoNoFuncionalDto(null, null))).hasSize(1);
        }
    }
}