package com.colab.backend.repository;

import com.colab.backend.domain.Habilidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HabilidadRepository extends JpaRepository<Habilidad, Long> {

    Optional<Habilidad> findByNombre(String nombre);
}
