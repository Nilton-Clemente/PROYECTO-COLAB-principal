package com.colab.backend.dto;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class CasoUsoDtoTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void aceptaJsonAnteriorConSoloNombre() {
        ProyectoRequest request = objectMapper.readValue("""
                {"nombre":"Proyecto", "casosDeUso":[{"nombre":"Crear proyecto"}]}
                """, ProyectoRequest.class);
        CasoUsoDto caso = request.casosDeUso().getFirst();

        assertThat(caso.nombre()).isEqualTo("Crear proyecto");
        assertThat(caso.actores()).isNull();
        assertThat(caso.flujoPrincipal()).isNull();
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(request)).isEmpty();
        }
    }

    @Test
    void conservaTodosLosDetallesAlLeerYEscribirJson() {
        CasoUsoDto caso = objectMapper.readValue("""
                {
                  "nombre":"Invitar integrante", "descripcion":"Invitar al equipo",
                  "actores":"Creador y destinatario", "precondiciones":"Sesión iniciada",
                  "disparador":"Seleccionar Invitar", "flujoPrincipal":"1. Registrar invitación.\\n2. Notificar.",
                  "flujosAlternativos":"Cancelar", "excepciones":"Invitación duplicada",
                  "postcondiciones":"Invitación pendiente", "reglasDeNegocio":"Solo el creador invita"
                }
                """, CasoUsoDto.class);

        assertThat(objectMapper.readValue(objectMapper.writeValueAsString(caso), CasoUsoDto.class))
                .isEqualTo(caso);
        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(caso)).has("codigo")).isFalse();
        assertThat(caso.actores()).isEqualTo("Creador y destinatario");
        assertThat(caso.precondiciones()).isEqualTo("Sesión iniciada");
        assertThat(caso.disparador()).isEqualTo("Seleccionar Invitar");
        assertThat(caso.flujoPrincipal()).isEqualTo("1. Registrar invitación.\n2. Notificar.");
        assertThat(caso.flujosAlternativos()).isEqualTo("Cancelar");
        assertThat(caso.excepciones()).isEqualTo("Invitación duplicada");
        assertThat(caso.postcondiciones()).isEqualTo("Invitación pendiente");
        assertThat(caso.reglasDeNegocio()).isEqualTo("Solo el creador invita");
    }

    @Test
    void sigueExigiendoNombreDentroDelProyecto() {
        ProyectoRequest request = objectMapper.readValue("""
                {"nombre":"Proyecto", "casosDeUso":[{"nombre":" ", "precondiciones":"Sesión iniciada"}]}
                """, ProyectoRequest.class);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(request))
                    .anySatisfy(v -> assertThat(v.getPropertyPath().toString())
                            .isEqualTo("casosDeUso[0].nombre"));
        }
    }
}