package com.colab.backend.service;

import com.colab.backend.domain.Etapa;
import com.colab.backend.domain.Planificacion;
import com.colab.backend.domain.Tarea;
import com.colab.backend.domain.Usuario;
import com.colab.backend.dto.EtapaRequest;
import com.colab.backend.dto.EtapaResponse;
import com.colab.backend.dto.TareaResponse;
import com.colab.backend.repository.DependenciaTareaRepository;
import com.colab.backend.repository.EtapaRepository;
import com.colab.backend.repository.IntegranteRepository;
import com.colab.backend.repository.PlanificacionRepository;
import com.colab.backend.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestión manual de fases (etapas) dentro de un proyecto.
 *
 * <p>Permite al Creador registrar, editar y eliminar las fases que dividen el
 * trabajo de la planificación, y a cualquier integrante listarlas/consultarlas.</p>
 */
@Service
public class EtapaService {

    private final EtapaRepository etapaRepository;
    private final PlanificacionRepository planificacionRepository;
    private final IntegranteRepository integranteRepository;
    private final UsuarioRepository usuarioRepository;
    private final DependenciaTareaRepository dependenciaTareaRepository;

    public EtapaService(EtapaRepository etapaRepository,
                        PlanificacionRepository planificacionRepository,
                        IntegranteRepository integranteRepository,
                        UsuarioRepository usuarioRepository,
                        DependenciaTareaRepository dependenciaTareaRepository) {
        this.etapaRepository = etapaRepository;
        this.planificacionRepository = planificacionRepository;
        this.integranteRepository = integranteRepository;
        this.usuarioRepository = usuarioRepository;
        this.dependenciaTareaRepository = dependenciaTareaRepository;
    }

    /** Listar todas las fases de un proyecto (cualquier integrante). */
    @Transactional(readOnly = true)
    public List<EtapaResponse> listar(Long proyectoId) {
        Usuario u = usuarioActual();
        verificarMiembro(u, proyectoId);
        Planificacion plan = planificacion(proyectoId);
        Map<Long, List<Long>> deps = dependenciasDe(plan.getId());
        return plan.getEtapas().stream()
                .sorted(Comparator.comparing(Etapa::getOrden, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(e -> aResponse(e, deps))
                .toList();
    }

    /** Obtener una fase concreta de un proyecto (cualquier integrante). */
    @Transactional(readOnly = true)
    public EtapaResponse obtener(Long proyectoId, Long etapaId) {
        Usuario u = usuarioActual();
        verificarMiembro(u, proyectoId);
        Etapa etapa = etapaDe(proyectoId, etapaId);
        return aResponse(etapa, dependenciasDe(etapa.getPlanificacion().getId()));
    }

    /** Crear una fase dentro de la planificación del proyecto (solo Creador). */
    @Transactional
    public EtapaResponse crear(Long proyectoId, EtapaRequest request) {
        Usuario u = usuarioActual();
        verificarCreador(u, proyectoId);
        Planificacion plan = planificacion(proyectoId);

        Etapa etapa = new Etapa();
        etapa.setPlanificacion(plan);
        etapa.setNombre(request.nombre());
        etapa.setOrden(request.orden());
        etapaRepository.save(etapa);

        return aResponse(etapa, dependenciasDe(plan.getId()));
    }

    /** Editar el nombre y/u orden de una fase (solo Creador). */
    @Transactional
    public EtapaResponse editar(Long proyectoId, Long etapaId, EtapaRequest request) {
        Usuario u = usuarioActual();
        verificarCreador(u, proyectoId);
        Etapa etapa = etapaDe(proyectoId, etapaId);

        etapa.setNombre(request.nombre());
        etapa.setOrden(request.orden());
        etapaRepository.save(etapa);

        return aResponse(etapa, dependenciasDe(etapa.getPlanificacion().getId()));
    }

    /** Eliminar una fase y, en cascada, sus tareas (solo Creador). */
    @Transactional
    public void eliminar(Long proyectoId, Long etapaId) {
        Usuario u = usuarioActual();
        verificarCreador(u, proyectoId);
        Etapa etapa = etapaDe(proyectoId, etapaId);
        etapaRepository.delete(etapa);
    }

    private EtapaResponse aResponse(Etapa e, Map<Long, List<Long>> deps) {
        List<TareaResponse> tareas = e.getTareas().stream()
                .sorted(Comparator.comparing(Tarea::getOrden, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(t -> new TareaResponse(
                        t.getId(),
                        t.getNombre(),
                        t.getDescripcion(),
                        t.getResponsable() != null ? t.getResponsable().getUsuario().getEmail() : null,
                        t.getPrioridad(),
                        t.getEstado(),
                        t.getDuracionDias(),
                        t.getFechaInicio(),
                        t.getFechaFin(),
                        deps.getOrDefault(t.getId(), List.of())))
                .toList();
        return new EtapaResponse(e.getId(), e.getNombre(), e.getOrden(), tareas);
    }

    private Map<Long, List<Long>> dependenciasDe(Long planificacionId) {
        return dependenciaTareaRepository.findByPlanificacionId(planificacionId).stream()
                .collect(Collectors.groupingBy(
                        d -> d.getTarea().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(d -> d.getTareaPredecesora().getId(), Collectors.toList())));
    }

    private Planificacion planificacion(Long proyectoId) {
        return planificacionRepository.findByProyectoId(proyectoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El proyecto no tiene planificación"));
    }

    private Etapa etapaDe(Long proyectoId, Long etapaId) {
        Etapa etapa = etapaRepository.findById(etapaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fase no existe"));
        if (!etapa.getPlanificacion().getProyecto().getId().equals(proyectoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La fase no pertenece al proyecto");
        }
        return etapa;
    }

    private void verificarMiembro(Usuario u, Long proyectoId) {
        if (!integranteRepository.existsByUsuarioIdAndProyectoId(u.getId(), proyectoId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No perteneces a este proyecto");
        }
    }

    private void verificarCreador(Usuario u, Long proyectoId) {
        integranteRepository.findByUsuarioIdAndProyectoId(u.getId(), proyectoId)
                .filter(i -> "CREADOR".equals(i.getRol()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Solo el Creador puede realizar esta acción"));
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
