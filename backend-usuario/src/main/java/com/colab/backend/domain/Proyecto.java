package com.colab.backend.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "proyecto")
@Getter
@Setter
@NoArgsConstructor
public class Proyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;
    private String problematica;

    @Column(name = "objetivo_general")
    private String objetivoGeneral;

    @Column(name = "objetivos_especificos")
    private String objetivosEspecificos;

    private String alcance;
    private String restricciones;

    @Column(name = "fecha_entrega")
    private LocalDate fechaEntrega;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private String tipo = "SOFTWARE";

    @OneToMany(mappedBy = "proyecto", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Integrante> integrantes = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "proyecto_tecnologia",
            joinColumns = @JoinColumn(name = "proyecto_id"),
            inverseJoinColumns = @JoinColumn(name = "tecnologia_id"))
    private Set<Tecnologia> tecnologias = new HashSet<>();

    @OneToMany(mappedBy = "proyecto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequerimientoFuncional> requerimientosFuncionales = new ArrayList<>();

    @OneToMany(mappedBy = "proyecto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequerimientoNoFuncional> requerimientosNoFuncionales = new ArrayList<>();

    @OneToMany(mappedBy = "proyecto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CasoUso> casosDeUso = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (tipo == null) {
            tipo = "SOFTWARE";
        }
    }
}
