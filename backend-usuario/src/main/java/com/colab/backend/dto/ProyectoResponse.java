package com.colab.backend.dto;

import java.time.LocalDate;
import java.util.List;

public record ProyectoResponse(
        Long id,
        String nombre,
        String descripcion,
        String problematica,
        String objetivoGeneral,
        String objetivosEspecificos,
        String alcance,
        String restricciones,
        LocalDate fechaEntrega,
        String tipo,
        List<TecnologiaDto> tecnologias,
        List<RequerimientoFuncionalDto> requerimientosFuncionales,
        List<RequerimientoNoFuncionalDto> requerimientosNoFuncionales,
        List<CasoUsoDto> casosDeUso
) {}

