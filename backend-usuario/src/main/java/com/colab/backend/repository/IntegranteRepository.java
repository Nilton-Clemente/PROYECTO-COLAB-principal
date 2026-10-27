package com.colab.backend.repository;

import com.colab.backend.domain.Integrante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IntegranteRepository extends JpaRepository<Integrante, Long> {

    List<Integrante> findByUsuarioId(Long usuarioId);

    List<Integrante> findByProyectoId(Long proyectoId);

    Optional<Integrante> findByUsuarioIdAndProyectoId(Long usuarioId, Long proyectoId);

    boolean existsByUsuarioIdAndProyectoId(Long usuarioId, Long proyectoId);

    List<Integrante> findByUsuarioIdAndEstado(Long usuarioId, String estado);
}
