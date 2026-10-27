package com.colab.backend.domain;

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

/**
 * Requerimiento funcional del proyecto (1:N con Proyecto).
 */
@Entity
@Table(name = "requerimiento_funcional")
@Getter
@Setter
@NoArgsConstructor
public class RequerimientoFuncional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    /** Codigo de negocio, p. ej. "RF-01". */
    private String codigo;

    private String descripcion;
}
