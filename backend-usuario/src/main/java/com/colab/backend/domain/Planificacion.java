package com.colab.backend.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Propuesta de planificación generada con IA (una sola vez por proyecto, RN-02).
 */
@Entity
@Table(name = "planificacion")
@Getter
@Setter
@NoArgsConstructor
public class Planificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false, unique = true)
    private Proyecto proyecto;

    /** BORRADOR / PROPUESTA / ACTIVA / FINALIZADA */
    @Column(nullable = false)
    private String estado;

    @Column(name = "fecha_generacion")
    private LocalDateTime fechaGeneracion;

    @OneToMany(mappedBy = "planificacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Etapa> etapas = new ArrayList<>();
}
