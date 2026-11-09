package com.colab.backend.service;

import com.colab.backend.domain.Notificacion;
import com.colab.backend.domain.Usuario;
import com.colab.backend.dto.NotificacionResponse;
import com.colab.backend.repository.NotificacionRepository;
import com.colab.backend.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Notificaciones in-app (RF-33/RF-34/RF-35, RN-22). Además de los endpoints,
 * expone {@link #crear(Usuario, String, String)} para que otros servicios
 * (tareas, planificación, confirmación) generen notificaciones.
 */
@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    public NotificacionService(NotificacionRepository notificacionRepository,
                               UsuarioRepository usuarioRepository) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** RF-33 — Crea una notificación para un usuario (uso interno de otros servicios). */
    @Transactional
    public void crear(Usuario destinatario, String tipoEvento, String mensaje) {
        Notificacion n = new Notificacion();
        n.setUsuario(destinatario);
        n.setTipoEvento(tipoEvento);
        n.setMensaje(mensaje);
        n.setLeida(false);
        notificacionRepository.save(n);
    }

    /** RF-34 — Listar las notificaciones del usuario autenticado. */
    @Transactional(readOnly = true)
    public List<NotificacionResponse> listar() {
        Usuario u = usuarioActual();
        return notificacionRepository.findByUsuarioIdOrderByFechaCreacionDesc(u.getId()).stream()
                .map(this::aResponse)
                .toList();
    }

    /** RF-34 — Cantidad de notificaciones sin leer (badge). */
    @Transactional(readOnly = true)
    public long noLeidas() {
        Usuario u = usuarioActual();
        return notificacionRepository.countByUsuarioIdAndLeidaFalse(u.getId());
    }

    /** RF-35 — Marcar una notificación como leída. */
    @Transactional
    public NotificacionResponse marcarLeida(Long id) {
        Usuario u = usuarioActual();
        Notificacion n = notificacionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificación no existe"));
        if (!n.getUsuario().getId().equals(u.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La notificación no te pertenece");
        }
        n.setLeida(true);
        notificacionRepository.save(n);
        return aResponse(n);
    }

    private NotificacionResponse aResponse(Notificacion n) {
        return new NotificacionResponse(
                n.getId(),
                n.getTipoEvento(),
                n.getMensaje(),
                n.isLeida(),
                n.getFechaCreacion());
    }

    private Usuario usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        return usuarioRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }
}
