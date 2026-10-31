package com.colab.backend.ia.dto;

import java.util.List;

/**
 * Etapa dentro de la planificación generada por la IA.
 */
public record EtapaIA(String nombre, Integer orden, List<TareaIA> tareas) {}
