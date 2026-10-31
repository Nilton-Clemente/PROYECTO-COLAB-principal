package com.colab.backend.repository;

import com.colab.backend.domain.DependenciaTarea;
import com.colab.backend.domain.DependenciaTareaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DependenciaTareaRepository extends JpaRepository<DependenciaTarea, DependenciaTareaId> {

    @Query("select d from DependenciaTarea d where d.tarea.etapa.planificacion.id = :planificacionId")
    List<DependenciaTarea> findByPlanificacionId(@Param("planificacionId") Long planificacionId);

    /** Predecesoras de una tarea (dependencias directas de la tarea). */
    @Query("select d from DependenciaTarea d where d.tarea.id = :tareaId")
    List<DependenciaTarea> findPredecesorasByTareaId(@Param("tareaId") Long tareaId);

    /** Sucesoras de una tarea (tareas que dependen de ella). */
    @Query("select d from DependenciaTarea d where d.tareaPredecesora.id = :tareaId")
    List<DependenciaTarea> findSucesorasByTareaId(@Param("tareaId") Long tareaId);
}

