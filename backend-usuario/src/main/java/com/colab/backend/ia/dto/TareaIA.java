package com.colab.backend.ia.dto;

import java.util.List;

/**
 * Tarea dentro de una etapa generada por la IA.
 *
 * <p>{@code id} es el identificador breve y único usado para referenciar
 * dependencias; {@code responsable} es el correo de un integrante existente.</p>
 */
public record TareaIA(
        String id,
        String nombre,
        String descripcion,
        String responsable,
        List<String> habilidadesRequeridas,
        String prioridad,
        Integer duracionDias,
        List<String> dependencias) {}
