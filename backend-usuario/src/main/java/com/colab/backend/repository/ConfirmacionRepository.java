package com.colab.backend.repository;

import com.colab.backend.domain.Confirmacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConfirmacionRepository extends JpaRepository<Confirmacion, Long> {

    List<Confirmacion> findByPlanificacionId(Long planificacionId);

    Optional<Confirmacion> findByPlanificacionIdAndIntegranteId(Long planificacionId, Long integranteId);
}
