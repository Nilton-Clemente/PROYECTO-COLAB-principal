package com.colab.backend.repository;

import com.colab.backend.domain.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Notificacion n where n.usuario.id = :usuarioId and n.leida = true")
    int eliminarLeidasDeUsuario(@Param("usuarioId") Long usuarioId);
}
