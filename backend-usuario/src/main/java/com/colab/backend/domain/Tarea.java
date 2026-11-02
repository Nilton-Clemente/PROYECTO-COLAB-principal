package com.colab.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Unidad de trabajo dentro de una etapa.
 */
@Entity
@Table(name = "tarea")
@Getter
@Setter
@NoArgsConstructor
public class Tarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_id", nullable = false)
    private Etapa etapa;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Integrante responsable;

    /** ALTA / MEDIA / BAJA */
    @Column(nullable = false)
    private String prioridad;

    /** PENDIENTE / EN_PROGRESO / COMPLETADA (BLOQUEADA se calcula) */
    @Column(nullable = false)
    private String estado;

    @Column(name = "duracion_dias", nullable = false)
    private Integer duracionDias;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    private Integer orden;
}
