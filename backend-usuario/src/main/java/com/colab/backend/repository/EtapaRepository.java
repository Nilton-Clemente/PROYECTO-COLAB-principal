package com.colab.backend.repository;

import com.colab.backend.domain.Etapa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EtapaRepository extends JpaRepository<Etapa, Long> {

    List<Etapa> findByPlanificacionId(Long planificacionId);
}
