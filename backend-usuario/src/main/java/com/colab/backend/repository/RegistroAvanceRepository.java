package com.colab.backend.repository;

import com.colab.backend.domain.RegistroAvance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RegistroAvanceRepository extends JpaRepository<RegistroAvance, Long> {
    List<RegistroAvance> findByTareaIdOrderByFechaDescIdDesc(Long tareaId);
}