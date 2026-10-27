package com.colab.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

public record ProyectoRequest(
        @NotBlank String nombre,
        String descripcion,
        String problematica,
        String objetivoGeneral,
        String objetivosEspecificos,
        String alcance,
        String restricciones,
        LocalDate fechaEntrega,
        List<Long> tecnologiasIds,
        @Valid List<RequerimientoFuncionalDto> requerimientosFuncionales,
        @Valid List<RequerimientoNoFuncionalDto> requerimientosNoFuncionales,
        @Valid List<CasoUsoDto> casosDeUso
) {}

