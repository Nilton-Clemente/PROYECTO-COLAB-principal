package com.colab.backend.repository;

import com.colab.backend.domain.Proyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Proyecto p where p.id = :id")
    Optional<Proyecto> findByIdForUpdate(@Param("id") Long id);
}
