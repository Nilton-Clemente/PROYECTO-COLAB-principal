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

import java.time.LocalDateTime;

/**
 * Confirmación individual de cada integrante sobre la propuesta (15.15).
 *
 * <p>Estados: PENDIENTE / ACEPTADA / CAMBIOS_SOLICITADOS.</p>
 */
@Entity
@Table(name = "confirmacion")
@Getter
@Setter
@NoArgsConstructor
public class Confirmacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planificacion_id", nullable = false)
    private Planificacion planificacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "integrante_id", nullable = false)
    private Integrante integrante;

    /** PENDIENTE / ACEPTADA / CAMBIOS_SOLICITADOS */
    @Column(nullable = false)
    private String estado;

    private String comentario;

    private LocalDateTime fecha;
}
