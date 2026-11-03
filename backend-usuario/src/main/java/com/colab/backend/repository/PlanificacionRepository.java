package com.colab.backend.repository;

import com.colab.backend.domain.Planificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanificacionRepository extends JpaRepository<Planificacion, Long> {

    boolean existsByProyectoId(Long proyectoId);

    Optional<Planificacion> findByProyectoId(Long proyectoId);
}
