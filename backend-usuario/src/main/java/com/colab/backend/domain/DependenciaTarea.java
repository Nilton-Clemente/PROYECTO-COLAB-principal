package com.colab.backend.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dependencia "finish-to-start" entre tareas (RN-12).
 */
@Entity
@Table(name = "dependencia_tarea")
@Getter
@Setter
@NoArgsConstructor
public class DependenciaTarea {

    @EmbeddedId
    private DependenciaTareaId id;

    @MapsId("tareaId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarea_id")
    private Tarea tarea;

    @MapsId("tareaPredecesoraId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarea_predecesora_id")
    private Tarea tareaPredecesora;
}
