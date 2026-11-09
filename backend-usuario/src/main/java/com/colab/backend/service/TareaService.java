package com.colab.backend.service;

import com.colab.backend.domain.DependenciaTarea;
import com.colab.backend.domain.DependenciaTareaId;
import com.colab.backend.domain.Etapa;
import com.colab.backend.domain.Integrante;
import com.colab.backend.domain.Planificacion;
import com.colab.backend.domain.Tarea;
import com.colab.backend.domain.Usuario;
import com.colab.backend.dto.TareaDetalleResponse;
import com.colab.backend.dto.TareaRequest;
import com.colab.backend.repository.DependenciaTareaRepository;
import com.colab.backend.repository.EtapaRepository;
import com.colab.backend.repository.IntegranteRepository;
import com.colab.backend.repository.PlanificacionRepository;
import com.colab.backend.repository.TareaRepository;
import com.colab.backend.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gestión manual de tareas y dependencias (RF-23 a RF-32; CU-11/CU-12/CU-13)
 * con control de estados (RN-05 a RN-08), bloqueo/desbloqueo por dependencias
 * (RN-09/RN-10) y prevención de ciclos (RN-11).
 */
@Service
public class TareaService {

    private final TareaRepository tareaRepository;
    private final EtapaRepository etapaRepository;
    private final IntegranteRepository integranteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PlanificacionRepository planificacionRepository;
    private final DependenciaTareaRepository dependenciaTareaRepository;
    private final NotificacionService notificacionService;

    public TareaService(TareaRepository tareaRepository,
                        EtapaRepository etapaRepository,
                        IntegranteRepository integranteRepository,
                        UsuarioRepository usuarioRepository,
                        PlanificacionRepository planificacionRepository,
                        DependenciaTareaRepository dependenciaTareaRepository,
                        NotificacionService notificacionService) {
        this.tareaRepository = tareaRepository;
        this.etapaRepository = etapaRepository;
        this.integranteRepository = integranteRepository;
        this.usuarioRepository = usuarioRepository;
        this.planificacionRepository = planificacionRepository;
        this.dependenciaTareaRepository = dependenciaTareaRepository;
        this.notificacionService = notificacionService;
    }

    /** RF-32 — Vista de todas las tareas del proyecto. */
    @Transactional(readOnly = true)
    public List<TareaDetalleResponse> listarTodas(Long proyectoId) {
        Usuario u = usuarioActual();
        verificarMiembro(u, proyectoId);
        return tareaRepository.findByProyectoId(proyectoId).stream()
                .map(this::aDetalle)
                .toList();
    }

    /** RF-31 — Vista "Mis tareas" (tareas asignadas al usuario actual). */
    @Transactional(readOnly = true)
    public List<TareaDetalleResponse> misTareas(Long proyectoId) {
        Usuario u = usuarioActual();
        Integrante integrante = integranteDe(u, proyectoId);
        return tareaRepository.findByResponsableIdAndProyectoId(integrante.getId(), proyectoId).stream()
                .map(this::aDetalle)
                .toList();
    }

    /** RF-23 — Crear tarea manualmente (solo Creador). */
    @Transactional
    public TareaDetalleResponse crear(Long proyectoId, TareaRequest request) {
        Usuario u = usuarioActual();
        verificarCreador(u, proyectoId);
        Planificacion plan = planificacion(proyectoId);

        Etapa etapa = etapaRepository.findById(request.etapaId())
                .filter(e -> e.getPlanificacion().getId().equals(plan.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La etapa no pertenece a la planificación de este proyecto"));

        Tarea t = new Tarea();
        aplicarCampos(t, etapa, request);
        t.setEstado("PENDIENTE");
        tareaRepository.save(t);

        // RN-22: notificar asignación de tarea al responsable.
        if (t.getResponsable() != null) {
            notificacionService.crear(t.getResponsable().getUsuario(), "ASIGNACION",
                    "Se te asignó la tarea \"" + t.getNombre() + "\" en el proyecto \""
                            + etapa.getPlanificacion().getProyecto().getNombre() + "\".");
        }
        return aDetalle(t);
    }

    /** RF-25 — Eliminar tarea (solo Creador). */
    @Transactional
    public void eliminar(Long proyectoId, Long tareaId) {
        Usuario u = usuarioActual();
        verificarCreador(u, proyectoId);
        Tarea t = tareaDe(proyectoId, tareaId);
        tareaRepository.delete(t);
    }

    /** RF-24 — Editar tarea (solo Creador). */
    @Transactional
    public TareaDetalleResponse editar(Long proyectoId, Long tareaId, TareaRequest request) {
        Usuario u = usuarioActual();
        verificarCreador(u, proyectoId);
        Tarea t = tareaDe(proyectoId, tareaId);

        Etapa etapa = etapaRepository.findById(request.etapaId())
                .filter(e -> e.getPlanificacion().getId().equals(t.getEtapa().getPlanificacion().getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La etapa no pertenece a la planificación de este proyecto"));

        aplicarCampos(t, etapa, request);
        tareaRepository.save(t);
        return aDetalle(t);
    }

    /** RF-26 — Reasignar responsable manualmente (solo Creador). */
    @Transactional
    public TareaDetalleResponse reasignar(Long proyectoId, Long tareaId, Long responsableId) {
        Usuario u = usuarioActual();
        verificarCreador(u, proyectoId);
        Tarea t = tareaDe(proyectoId, tareaId);

        if (responsableId == null) {
            t.setResponsable(null);
        } else {
            Integrante responsable = integranteRepository.findById(responsableId)
                    .filter(i -> i.getProyecto().getId().equals(proyectoId))
                    .filter(i -> "ACTIVO".equals(i.getEstado()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "El responsable debe ser un integrante activo del proyecto"));
            t.setResponsable(responsable);
        }
        tareaRepository.save(t);

        // RN-22: notificar reasignación al nuevo responsable.
        if (t.getResponsable() != null) {
            notificacionService.crear(t.getResponsable().getUsuario(), "CAMBIO_RESPONSABLE",
                    "Ahora eres responsable de la tarea \"" + t.getNombre() + "\".");
        }
        return aDetalle(t);
    }

    /** RF-28 — Cambiar estado de una tarea (responsable o Creador, con transiciones RN-07). */
    @Transactional
    public TareaDetalleResponse cambiarEstado(Long proyectoId, Long tareaId, String nuevoEstado) {
        Usuario u = usuarioActual();
        Tarea t = tareaDe(proyectoId, tareaId);
        String estadoNormalizado = nuevoEstado.toUpperCase();

        verificarPuedeGestionar(u, proyectoId, t);

        String estadoActual = estadoEfectivo(t);
        if (!transicionValida(estadoActual, estadoNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transición no permitida: " + estadoActual + " → " + estadoNormalizado);
        }

        t.setEstado(estadoNormalizado);
        tareaRepository.save(t);

        // RN-10: al completar una tarea, desbloquear automáticamente sus dependientes.
        if ("COMPLETADA".equals(estadoNormalizado)) {
            desbloquearSucesoras(t);
        }

        // RN-22: notificar al Creador el cambio de estado.
        Integrante creador = creadorDe(t.getEtapa().getPlanificacion().getProyecto().getId());
        if (creador != null) {
            notificacionService.crear(creador.getUsuario(), "CAMBIO_ESTADO",
                    "La tarea \"" + t.getNombre() + "\" cambió de estado a " + estadoNormalizado + ".");
        }

        return aDetalle(t);
    }

    /** RF-27 — Definir dependencia (finish-to-start) sin permitir ciclos (RN-11). */
    @Transactional
    public TareaDetalleResponse agregarDependencia(Long proyectoId, Long tareaId, Long predecesoraId) {
        Usuario u = usuarioActual();
        verificarCreador(u, proyectoId);
        Tarea tarea = tareaDe(proyectoId, tareaId);
        Tarea predecesora = tareaDe(proyectoId, predecesoraId);

        if (tarea.getId().equals(predecesora.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Una tarea no puede depender de sí misma");
        }
        if (creaCiclo(tarea, predecesora)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La dependencia crearía un ciclo");
        }

        DependenciaTareaId id = new DependenciaTareaId(tarea.getId(), predecesora.getId());
        if (!dependenciaTareaRepository.existsById(id)) {
            DependenciaTarea d = new DependenciaTarea();
            d.setId(id);
            d.setTarea(tarea);
            d.setTareaPredecesora(predecesora);
            dependenciaTareaRepository.save(d);
        }

        // Re-evaluar bloqueo de la tarea afectada (RN-09).
        actualizarBloqueo(tarea);

        return aDetalle(tarea);
    }

    /** Quitar una dependencia de una tarea. */
    @Transactional
    public TareaDetalleResponse quitarDependencia(Long proyectoId, Long tareaId, Long predecesoraId) {
        Usuario u = usuarioActual();
        verificarCreador(u, proyectoId);
        Tarea tarea = tareaDe(proyectoId, tareaId);

        DependenciaTareaId id = new DependenciaTareaId(tareaId, predecesoraId);
        dependenciaTareaRepository.deleteById(id);

        // Al quitar una dependencia, la tarea podría quedar desbloqueada.
        boolean bloqueada = tieneDependenciasIncompletas(tarea);
        if (!bloqueada && "BLOQUEADA".equals(tarea.getEstado())) {
            tarea.setEstado("PENDIENTE");
            tareaRepository.save(tarea);
        }

        return aDetalle(tarea);
    }

    private void aplicarCampos(Tarea t, Etapa etapa, TareaRequest request) {
        t.setEtapa(etapa);
        t.setNombre(request.nombre());
        t.setDescripcion(request.descripcion());
        t.setPrioridad(request.prioridad().toUpperCase());
        t.setDuracionDias(request.duracionDias());
        t.setOrden(request.orden());

        if (request.responsableId() == null) {
            t.setResponsable(null);
        } else {
            Integrante responsable = integranteRepository.findById(request.responsableId())
                    .filter(i -> i.getProyecto().getId().equals(etapa.getPlanificacion().getProyecto().getId()))
                    .filter(i -> "ACTIVO".equals(i.getEstado()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "El responsable debe ser un integrante activo del proyecto"));
            t.setResponsable(responsable);
        }
    }

    /** RN-07 — transiciones manuales permitidas. */
    private boolean transicionValida(String estadoActual, String nuevo) {
        return switch (estadoActual) {
            case "PENDIENTE" -> "EN_PROGRESO".equals(nuevo);
            case "EN_PROGRESO" -> "COMPLETADA".equals(nuevo) || "PENDIENTE".equals(nuevo);
            default -> false;
        };
    }

    /**
     * RN-06 — el estado BLOQUEADA se calcula: si la tarea está PENDIENTE y tiene
     * dependencias incompletas, se expone como BLOQUEADA (sin persistirlo).
     */
    private String estadoEfectivo(Tarea t) {
        if ("PENDIENTE".equals(t.getEstado()) && tieneDependenciasIncompletas(t)) {
            return "BLOQUEADA";
        }
        return t.getEstado();
    }

    private boolean tieneDependenciasIncompletas(Tarea t) {
        return !dependenciaTareaRepository.findPredecesorasByTareaId(t.getId()).stream()
                .allMatch(d -> "COMPLETADA".equals(d.getTareaPredecesora().getEstado()));
    }

    /** RN-10 — al completar una tarea, desbloquear sus dependientes que queden libres. */
    private void desbloquearSucesoras(Tarea t) {
        List<Tarea> sucesoras = dependenciaTareaRepository.findSucesorasByTareaId(t.getId()).stream()
                .map(DependenciaTarea::getTarea)
                .filter(s -> "PENDIENTE".equals(s.getEstado()))
                .toList();
        for (Tarea sucesora : sucesoras) {
            if (!tieneDependenciasIncompletas(sucesora)) {
                // RN-22: notificar desbloqueo al responsable de la tarea liberada.
                if (sucesora.getResponsable() != null) {
                    notificacionService.crear(sucesora.getResponsable().getUsuario(), "DESBLOQUEO",
                            "La tarea \"" + sucesora.getNombre() + "\" quedó desbloqueada (sus dependencias se completaron).");
                }
            }
        }
    }

    /** RN-09/RN-10 — ajuste de estado PENDIENTE conforme a dependencias (calculado). */
    private void actualizarBloqueo(Tarea tarea) {
        // El estado BLOQUEADA es calculado, no se persiste; no se modifica la fila.
    }

    /** RN-11 — detectar si agregar (tarea -> predecesora) crea un ciclo. */
    private boolean creaCiclo(Tarea tarea, Tarea predecesora) {
        Set<Long> visitados = new HashSet<>();
        Deque<Tarea> pila = new ArrayDeque<>();
        pila.push(predecesora);
        while (!pila.isEmpty()) {
            Tarea actual = pila.pop();
            if (actual.getId().equals(tarea.getId())) {
                return true;
            }
            if (!visitados.add(actual.getId())) {
                continue;
            }
            for (DependenciaTarea d : dependenciaTareaRepository.findPredecesorasByTareaId(actual.getId())) {
                pila.push(d.getTareaPredecesora());
            }
        }
        return false;
    }

    private Tarea tareaDe(Long proyectoId, Long tareaId) {
        Tarea t = tareaRepository.findById(tareaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no existe"));
        if (!t.getEtapa().getPlanificacion().getProyecto().getId().equals(proyectoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La tarea no pertenece al proyecto");
        }
        return t;
    }

    private Planificacion planificacion(Long proyectoId) {
        return planificacionRepository.findByProyectoId(proyectoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El proyecto no tiene planificación"));
    }

    private Integrante integranteDe(Usuario u, Long proyectoId) {
        return integranteRepository.findByUsuarioIdAndProyectoId(u.getId(), proyectoId)
                .filter(i -> "ACTIVO".equals(i.getEstado()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "No eres un integrante activo de este proyecto"));
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

    /** Devuelve el integrante con rol CREADOR del proyecto, o null si no existe. */
    private Integrante creadorDe(Long proyectoId) {
        return integranteRepository.findByProyectoId(proyectoId).stream()
                .filter(i -> "CREADOR".equals(i.getRol()))
                .findFirst()
                .orElse(null);
    }

    /** RN-14/RN-15 — Creador puede cambiar cualquier tarea; integrante solo las suyas. */
    private void verificarPuedeGestionar(Usuario u, Long proyectoId, Tarea t) {
        Integrante integrante = integranteRepository.findByUsuarioIdAndProyectoId(u.getId(), proyectoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No perteneces a este proyecto"));

        boolean esCreador = "CREADOR".equals(integrante.getRol());
        boolean esResponsable = t.getResponsable() != null && t.getResponsable().getId().equals(integrante.getId());
        if (!esCreador && !esResponsable) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el responsable o el Creador pueden cambiar el estado");
        }
    }

    private TareaDetalleResponse aDetalle(Tarea t) {
        List<Long> predecesoras = dependenciaTareaRepository.findPredecesorasByTareaId(t.getId()).stream()
                .map(d -> d.getTareaPredecesora().getId())
                .toList();
        return new TareaDetalleResponse(
                t.getId(),
                t.getNombre(),
                t.getDescripcion(),
                t.getEtapa().getId(),
                t.getEtapa().getNombre(),
                t.getResponsable() != null ? t.getResponsable().getId() : null,
                t.getResponsable() != null ? t.getResponsable().getUsuario().getEmail() : null,
                t.getPrioridad(),
                estadoEfectivo(t),
                t.getDuracionDias(),
                t.getFechaInicio(),
                t.getFechaFin(),
                t.getOrden(),
                predecesoras);
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

