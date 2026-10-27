package com.colab.backend.repository;

import com.colab.backend.domain.Tecnologia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TecnologiaRepository extends JpaRepository<Tecnologia, Long> {
}
