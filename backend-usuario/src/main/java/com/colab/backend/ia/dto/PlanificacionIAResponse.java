package com.colab.backend.ia.dto;

import java.util.List;

/**
 * Contrato de salida JSON esperado de la IA (raíz).
 */
public record PlanificacionIAResponse(List<EtapaIA> etapas) {}
