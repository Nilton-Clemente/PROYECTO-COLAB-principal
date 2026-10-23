package com.colab.backend.repository;

import com.colab.backend.domain.UsuarioHabilidad;
import com.colab.backend.domain.UsuarioHabilidadId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioHabilidadRepository extends JpaRepository<UsuarioHabilidad, UsuarioHabilidadId> {
}
