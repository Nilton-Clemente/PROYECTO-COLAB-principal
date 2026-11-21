package com.colab.backend.repository;

import com.colab.backend.domain.Integrante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IntegranteRepository extends JpaRepository<Integrante, Long> {

    List<Integrante> findByUsuarioId(Long usuarioId);

    List<Integrante> findByProyectoId(Long proyectoId);

    Optional<Integrante> findByUsuarioIdAndProyectoId(Long usuarioId, Long proyectoId);

    boolean existsByUsuarioIdAndProyectoId(Long usuarioId, Long proyectoId);

    List<Integrante> findByUsuarioIdAndEstado(Long usuarioId, String estado);

    @Query("select i.proyecto.id from Integrante i where i.id = :id")
    Optional<Long> buscarProyectoId(@Param("id") Long id);
}
