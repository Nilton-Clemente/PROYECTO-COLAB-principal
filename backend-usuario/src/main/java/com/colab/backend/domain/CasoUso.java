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
 * Caso de uso registrado del proyecto (1:N con Proyecto).
 */
@Entity
@Table(name = "caso_uso")
@Getter
@Setter
@NoArgsConstructor
public class CasoUso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    /** Codigo de negocio, p. ej. "CU-01". */
    private String codigo;

    private String nombre;

    private String descripcion;
}
