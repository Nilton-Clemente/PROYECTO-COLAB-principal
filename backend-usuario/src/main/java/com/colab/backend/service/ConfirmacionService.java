package com.colab.backend.service;

import com.colab.backend.domain.Confirmacion;
import com.colab.backend.domain.Integrante;
import com.colab.backend.domain.Planificacion;
import com.colab.backend.domain.Tarea;
import com.colab.backend.domain.Usuario;
import com.colab.backend.dto.ConfirmacionResponse;
import com.colab.backend.repository.ConfirmacionRepository;
import com.colab.backend.repository.IntegranteRepository;
import com.colab.backend.repository.PlanificacionRepository;
import com.colab.backend.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Confirmación individual de la propuesta (RF-20 / RF-21) y activación de la
 * planificación cuando todos confirman (RF-22 / RN-03). La solicitud de cambios
 * mantiene la planificación en PROPUESTA y reinicia las confirmaciones (RN-04).
 */
@Service
public class ConfirmacionService {

    private final PlanificacionRepository planificacionRepository;
    private final ConfirmacionRepository confirmacionRepository;
    private final IntegranteRepository integranteRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    public ConfirmacionService(PlanificacionRepository planificacionRepository,
                               ConfirmacionRepository confirmacionRepository,
                               IntegranteRepository integranteRepository,
                               UsuarioRepository usuarioRepository,
                               NotificacionService notificacionService) {
        this.planificacionRepository = planificacionRepository;
        this.confirmacionRepository = confirmacionRepository;
        this.integranteRepository = integranteRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public ConfirmacionResponse confirmar(Long proyectoId) {
        Usuario u = usuarioActual();
        Planificacion plan = planificacion(proyectoId);
        Integrante integrante = integranteActivo(u, proyectoId);

        if (!"PROPUESTA".equals(plan.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede confirmar una planificación en estado PROPUESTA");
        }
        if (!tieneTareasAsignadas(plan, integrante)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No tienes tareas asignadas que confirmar");
        }

        Confirmacion c = confirmacionRepository
                .findByPlanificacionIdAndIntegranteId(plan.getId(), integrante.getId())
                .orElseGet(Confirmacion::new);
        c.setPlanificacion(plan);
        c.setIntegrante(integrante);
        c.setEstado("ACEPTADA");
        c.setComentario(null);
        c.setFecha(LocalDateTime.now());
        confirmacionRepository.save(c);

        activarSiTodosConfirmaron(plan);
        return aResponse(c);
    }

    @Transactional
    public ConfirmacionResponse solicitarCambios(Long proyectoId, String comentario) {
        Usuario u = usuarioActual();
        Planificacion plan = planificacion(proyectoId);
        Integrante integrante = integranteActivo(u, proyectoId);

        if (!"PROPUESTA".equals(plan.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede solicitar cambios en una planificación en estado PROPUESTA");
        }

        Confirmacion c = confirmacionRepository
                .findByPlanificacionIdAndIntegranteId(plan.getId(), integrante.getId())
                .orElseGet(Confirmacion::new);
        c.setPlanificacion(plan);
        c.setIntegrante(integrante);
        c.setEstado("CAMBIOS_SOLICITADOS");
        c.setComentario(comentario);
        c.setFecha(LocalDateTime.now());
        confirmacionRepository.save(c);

        // RN-04: las confirmaciones del resto del equipo se reinician.
        List<Confirmacion> otras = confirmacionRepository.findByPlanificacionId(plan.getId()).stream()
                .filter(otra -> !otra.getIntegrante().getId().equals(integrante.getId()))
                .toList();
        confirmacionRepository.deleteAll(otras);

        // RN-22: notificar al Creador que se solicitaron cambios.
        Integrante creador = creadorDe(plan);
        if (creador != null) {
            notificacionService.crear(creador.getUsuario(), "PLANIFICACION",
                    integrante.getUsuario().getNombre() + " solicitó cambios en la planificación: " + comentario);
        }

        return aResponse(c);
    }

    @Transactional(readOnly = true)
    public List<ConfirmacionResponse> listar(Long proyectoId) {
        Usuario u = usuarioActual();
        Planificacion plan = planificacion(proyectoId);
        if (!integranteRepository.existsByUsuarioIdAndProyectoId(u.getId(), proyectoId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No perteneces a este proyecto");
        }
        return confirmacionRepository.findByPlanificacionId(plan.getId()).stream()
                .map(this::aResponse)
                .toList();
    }

    private void activarSiTodosConfirmaron(Planificacion plan) {
        List<Integrante> conTareas = integrantesConTareas(plan);
        if (conTareas.isEmpty()) {
            return;
        }
        Set<Long> confirmados = confirmacionRepository.findByPlanificacionId(plan.getId()).stream()
                .filter(c -> "ACEPTADA".equals(c.getEstado()))
                .map(c -> c.getIntegrante().getId())
                .collect(Collectors.toSet());
        boolean todos = conTareas.stream().allMatch(i -> confirmados.contains(i.getId()));
        if (todos) {
            plan.setEstado("ACTIVA");
            planificacionRepository.save(plan);

            // RN-22: notificar a todos los integrantes que la planificación se activó.
            for (Integrante i : conTareas) {
                notificacionService.crear(i.getUsuario(), "PLANIFICACION",
                        "La planificación del proyecto quedó ACTIVA.");
            }
        }
    }

    private List<Integrante> integrantesConTareas(Planificacion plan) {
        return plan.getEtapas().stream()
                .flatMap(e -> e.getTareas().stream())
                .map(Tarea::getResponsable)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private boolean tieneTareasAsignadas(Planificacion plan, Integrante integrante) {
        return integrantesConTareas(plan).stream()
                .anyMatch(i -> i.getId().equals(integrante.getId()));
    }

    private Planificacion planificacion(Long proyectoId) {
        return planificacionRepository.findByProyectoId(proyectoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El proyecto no tiene planificación"));
    }

    /** Devuelve el integrante con rol CREADOR de la planificación, o null. */
    private Integrante creadorDe(Planificacion plan) {
        return integranteRepository.findByProyectoId(plan.getProyecto().getId()).stream()
                .filter(i -> "CREADOR".equals(i.getRol()))
                .findFirst()
                .orElse(null);
    }

    private Integrante integranteActivo(Usuario u, Long proyectoId) {
        return integranteRepository.findByUsuarioIdAndProyectoId(u.getId(), proyectoId)
                .filter(i -> "ACTIVO".equals(i.getEstado()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "No eres un integrante activo de este proyecto"));
    }

    private ConfirmacionResponse aResponse(Confirmacion c) {
        Integrante i = c.getIntegrante();
        return new ConfirmacionResponse(
                c.getId(),
                i.getId(),
                i.getUsuario().getEmail(),
                i.getUsuario().getNombre(),
                c.getEstado(),
                c.getComentario(),
                c.getFecha());
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
