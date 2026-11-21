package com.colab.backend.domain;

/** Estado público derivado de la única planificación del proyecto. */
public enum EstadoProyecto {
    SIN_PLANIFICACION, PLANIFICACION_PROPUESTA, EN_DESARROLLO, FINALIZADO, CANCELADO;

    public static EstadoProyecto desde(Planificacion plan) {
        if (plan == null) {
            return SIN_PLANIFICACION;
        }
        return switch (plan.getEstado()) {
            case "BORRADOR", "PROPUESTA" -> PLANIFICACION_PROPUESTA;
            case "ACTIVA" -> EN_DESARROLLO;
            case "FINALIZADA" -> FINALIZADO;
            case "CANCELADA" -> CANCELADO;
            default -> throw new IllegalStateException("Estado de planificación desconocido: " + plan.getEstado());
        };
    }

    public boolean permiteEliminar() {
        return this != EN_DESARROLLO;
    }

    public boolean permiteCancelar() {
        return this == EN_DESARROLLO;
    }

    public boolean esTerminal() {
        return this == FINALIZADO || this == CANCELADO;
    }
}