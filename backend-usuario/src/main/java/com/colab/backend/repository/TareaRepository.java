package com.colab.backend.repository;

import com.colab.backend.domain.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TareaRepository extends JpaRepository<Tarea, Long> {

    @Query("select t from Tarea t where t.etapa.planificacion.proyecto.id = :proyectoId")
    List<Tarea> findByProyectoId(@Param("proyectoId") Long proyectoId);

    @Query("select t from Tarea t where t.responsable.id = :integranteId and t.etapa.planificacion.proyecto.id = :proyectoId")
    List<Tarea> findByResponsableIdAndProyectoId(@Param("integranteId") Long integranteId,
                                                 @Param("proyectoId") Long proyectoId);
}
