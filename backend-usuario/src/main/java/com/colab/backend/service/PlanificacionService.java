package com.colab.backend.service;

import com.colab.backend.domain.DependenciaTarea;
import com.colab.backend.domain.DependenciaTareaId;
import com.colab.backend.domain.Etapa;
import com.colab.backend.domain.Integrante;
import com.colab.backend.domain.Planificacion;
import com.colab.backend.domain.Proyecto;
import com.colab.backend.domain.Tarea;
import com.colab.backend.domain.Usuario;
import com.colab.backend.dto.EtapaResponse;
import com.colab.backend.dto.PlanificacionResponse;
import com.colab.backend.dto.TareaResponse;
import com.colab.backend.ia.PlanIAClient;
import com.colab.backend.ia.dto.EtapaIA;
import com.colab.backend.ia.dto.PlanificacionIAResponse;
import com.colab.backend.ia.dto.TareaIA;
import com.colab.backend.repository.DependenciaTareaRepository;
import com.colab.backend.repository.IntegranteRepository;
import com.colab.backend.repository.PlanificacionRepository;
import com.colab.backend.repository.ProyectoRepository;
import com.colab.backend.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Genera la planificación con IA (CU-08): valida la información mínima (RN-18),
 * arma el prompt, invoca a la IA, valida la salida (RN-19), calcula las fechas
 * (RN-20) y persiste la propuesta en estado PROPUESTA.
 */
@Service
public class PlanificacionService {

    private final ProyectoRepository proyectoRepository;
    private final IntegranteRepository integranteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PlanificacionRepository planificacionRepository;
    private final DependenciaTareaRepository dependenciaTareaRepository;
    private final PlanIAClient planIAClient;
    private final ObjectMapper objectMapper;

    public PlanificacionService(ProyectoRepository proyectoRepository,
                                IntegranteRepository integranteRepository,
                                UsuarioRepository usuarioRepository,
                                PlanificacionRepository planificacionRepository,
                                DependenciaTareaRepository dependenciaTareaRepository,
                                PlanIAClient planIAClient,
                                ObjectMapper objectMapper) {
        this.proyectoRepository = proyectoRepository;
        this.integranteRepository = integranteRepository;
        this.usuarioRepository = usuarioRepository;
        this.planificacionRepository = planificacionRepository;
        this.dependenciaTareaRepository = dependenciaTareaRepository;
        this.planIAClient = planIAClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PlanificacionResponse generar(Long proyectoId) {
        Usuario u = usuarioActual();
        Proyecto p = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no existe"));

        verificarCreador(u, proyectoId);

        if (planificacionRepository.existsByProyectoId(proyectoId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La planificación ya fue generada (RN-02)");
        }

        List<Integrante> equipo = integranteRepository.findByProyectoId(proyectoId).stream()
                .filter(i -> "ACTIVO".equals(i.getEstado()))
                .toList();

        validarInformacionMinima(p, equipo);

        String json;
        try {
            List<String> correos = equipo.stream()
                    .map(i -> i.getUsuario().getEmail())
                    .toList();
            json = planIAClient.generarPlanificacionJson(promptSistema(), construirPromptUsuario(p, equipo), correos);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo obtener respuesta de la IA: " + e.getMessage(), e);
        }

        PlanificacionIAResponse ia;
        try {
            ia = objectMapper.readValue(limpiarJson(json), PlanificacionIAResponse.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "La IA devolvió un JSON inválido: " + e.getMessage(), e);
        }

        Map<String, Integrante> porEmail = equipo.stream()
                .collect(Collectors.toMap(
                        i -> i.getUsuario().getEmail(),
                        i -> i,
                        (a, b) -> a));

        PlanConstruido pc = construirEntidades(p, ia, porEmail);
        validarDependencias(pc.tareaPorId(), pc.dependencias());
        List<String> orden = ordenTopologico(pc.tareaPorId(), pc.dependencias());
        calcularFechas(orden, pc.tareaPorId(), pc.dependencias(), p.getFechaEntrega());

        Planificacion plan = pc.planificacion();
        planificacionRepository.saveAndFlush(plan);

        List<DependenciaTarea> dependencias = new ArrayList<>();
        for (Map.Entry<Tarea, String> e : pc.idPorTarea().entrySet()) {
            Tarea tarea = e.getKey();
            for (String depId : pc.dependencias().getOrDefault(e.getValue(), List.of())) {
                DependenciaTarea d = new DependenciaTarea();
                d.setId(new DependenciaTareaId(tarea.getId(), pc.tareaPorId().get(depId).getId()));
                d.setTarea(tarea);
                d.setTareaPredecesora(pc.tareaPorId().get(depId));
                dependencias.add(d);
            }
        }
        dependenciaTareaRepository.saveAll(dependencias);
        dependenciaTareaRepository.flush();

        return aResponse(plan);
    }
    private record PlanConstruido(
            Planificacion planificacion,
            Map<String, Tarea> tareaPorId,
            Map<String, List<String>> dependencias,
            Map<Tarea, String> idPorTarea) {}

    private PlanConstruido construirEntidades(Proyecto p, PlanificacionIAResponse ia,
                                              Map<String, Integrante> porEmail) {
        if (ia == null || ia.etapas() == null || ia.etapas().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "La IA no devolvió etapas en su respuesta");
        }

        Map<String, Tarea> tareaPorId = new LinkedHashMap<>();
        Map<String, List<String>> dependencias = new LinkedHashMap<>();
        Map<Tarea, String> idPorTarea = new LinkedHashMap<>();

        Planificacion plan = new Planificacion();
        plan.setProyecto(p);
        plan.setEstado("PROPUESTA");
        plan.setFechaGeneracion(LocalDateTime.now());

        int idxEtapa = 0;
        int contadorAuto = 0;
        for (EtapaIA eia : ia.etapas()) {
            Etapa etapa = new Etapa();
            etapa.setPlanificacion(plan);
            etapa.setNombre(eia.nombre());
            etapa.setOrden(eia.orden() != null ? eia.orden() : ++idxEtapa);

            if (eia.tareas() != null) {
                int ordenTarea = 0;
                for (TareaIA tia : eia.tareas()) {
                    Tarea tarea = nuevaTarea(tia, etapa, porEmail, ++ordenTarea);
                    String idTarea = (tia.id() != null && !tia.id().isBlank())
                            ? tia.id()
                            : ("__auto_" + (++contadorAuto));
                    if (tareaPorId.containsKey(idTarea)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "La IA repitió el id de tarea: " + idTarea);
                    }
                    tareaPorId.put(idTarea, tarea);
                    idPorTarea.put(tarea, idTarea);
                    dependencias.put(idTarea, tia.dependencias() == null ? List.of() : tia.dependencias());
                    etapa.getTareas().add(tarea);
                }
            }
            plan.getEtapas().add(etapa);
        }
        return new PlanConstruido(plan, tareaPorId, dependencias, idPorTarea);
    }
    private void validarDependencias(Map<String, Tarea> tareaPorId,
                                     Map<String, List<String>> dependencias) {
        for (Map.Entry<String, List<String>> e : dependencias.entrySet()) {
            for (String dep : e.getValue()) {
                if (dep == null || !tareaPorId.containsKey(dep)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "La tarea '" + e.getKey() + "' depende de una tarea inexistente: " + dep);
                }
            }
        }
    }

    private List<String> ordenTopologico(Map<String, Tarea> tareaPorId,
                                         Map<String, List<String>> dependencias) {
        Map<String, Integer> indegree = new HashMap<>();
        Map<String, List<String>> dependientes = new HashMap<>();
        for (String id : tareaPorId.keySet()) {
            List<String> preds = dependencias.getOrDefault(id, List.of());
            indegree.put(id, preds.size());
            for (String pred : preds) {
                dependientes.computeIfAbsent(pred, k -> new ArrayList<>()).add(id);
            }
        }
        ArrayDeque<String> cola = new ArrayDeque<>();
        for (Map.Entry<String, Integer> e : indegree.entrySet()) {
            if (e.getValue() == 0) {
                cola.add(e.getKey());
            }
        }
        List<String> orden = new ArrayList<>();
        while (!cola.isEmpty()) {
            String actual = cola.poll();
            orden.add(actual);
            for (String dependiente : dependientes.getOrDefault(actual, List.of())) {
                int nuevo = indegree.get(dependiente) - 1;
                indegree.put(dependiente, nuevo);
                if (nuevo == 0) {
                    cola.add(dependiente);
                }
            }
        }
        if (orden.size() != tareaPorId.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La salida de la IA tiene dependencias con ciclos (RN-11)");
        }
        return orden;
    }

    private void calcularFechas(List<String> ordenIds, Map<String, Tarea> tareaPorId,
                                Map<String, List<String>> dependencias, LocalDate fechaEntrega) {
        LocalDate ancla = fechaEntrega;
        Map<String, LocalDate> finPorId = new HashMap<>();
        for (String id : ordenIds) {
            Tarea tarea = tareaPorId.get(id);
            LocalDate inicio = ancla;
            for (String dep : dependencias.getOrDefault(id, List.of())) {
                LocalDate finDep = finPorId.get(dep);
                if (finDep != null && finDep.isAfter(inicio)) {
                    inicio = finDep;
                }
            }
            tarea.setFechaInicio(inicio);
            LocalDate fin = inicio.plusDays(tarea.getDuracionDias());
            tarea.setFechaFin(fin);
            finPorId.put(id, fin);
        }
        LocalDate maxFin = finPorId.values().stream().max(LocalDate::compareTo).orElse(ancla);
        long shift = ChronoUnit.DAYS.between(maxFin, fechaEntrega);
        for (Tarea tarea : tareaPorId.values()) {
            tarea.setFechaInicio(tarea.getFechaInicio().plusDays(shift));
            tarea.setFechaFin(tarea.getFechaFin().plusDays(shift));
        }
    }
    private void validarInformacionMinima(Proyecto p, List<Integrante> equipo) {
        if (isBlank(p.getObjetivoGeneral())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta el objetivo general (RN-18)");
        }
        if (p.getFechaEntrega() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta la fecha de entrega (RN-18)");
        }
        if (p.getRequerimientosFuncionales() == null || p.getRequerimientosFuncionales().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Se requiere al menos un requerimiento funcional (RN-18)");
        }
        if (p.getTecnologias() == null || p.getTecnologias().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Se requiere al menos una tecnología (RN-18)");
        }
        long conHabilidades = equipo.stream()
                .filter(i -> i.getUsuario().getHabilidades() != null && !i.getUsuario().getHabilidades().isEmpty())
                .count();
        if (conHabilidades < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Se requieren al menos dos integrantes con perfil de habilidades (RN-18)");
        }
    }

    private Tarea nuevaTarea(TareaIA tia, Etapa etapa, Map<String, Integrante> porEmail, int orden) {
        Tarea tarea = new Tarea();
        tarea.setEtapa(etapa);
        if (isBlank(tia.nombre())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La IA devolvió una tarea sin nombre");
        }
        tarea.setNombre(tia.nombre());
        tarea.setDescripcion(tia.descripcion());

        String prioridad = tia.prioridad() == null ? "MEDIA" : tia.prioridad().toUpperCase();
        if (!Set.of("ALTA", "MEDIA", "BAJA").contains(prioridad)) {
            prioridad = "MEDIA";
        }
        tarea.setPrioridad(prioridad);

        if (tia.duracionDias() == null || tia.duracionDias() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La tarea '" + tia.nombre() + "' debe tener duración positiva (RN-19)");
        }
        tarea.setDuracionDias(tia.duracionDias());
        tarea.setEstado("PENDIENTE");
        tarea.setOrden(orden);

        if (!isBlank(tia.responsable())) {
            Integrante resp = porEmail.get(tia.responsable());
            if (resp == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El responsable '" + tia.responsable() + "' no es un integrante del proyecto (RN-19)");
            }
            tarea.setResponsable(resp);
        }
        return tarea;
    }

    private String promptSistema() {
        return """
                Eres un asistente experto en planificación de proyectos de software académicos.
                Tu tarea es convertir la información del proyecto y del equipo en una propuesta de planificación.
                Responde ÚNICAMENTE con JSON válido, sin markdown ni texto adicional.
                Estructura esperada:
                {
                  "etapas": [
                    {
                      "nombre": "...",
                      "orden": 1,
                      "tareas": [
                        {
                          "id": "t1",
                          "nombre": "...",
                          "descripcion": "...",
                          "responsable": "correo.del.integrante@ejemplo.com",
                          "habilidadesRequeridas": ["..."],
                          "prioridad": "ALTA|MEDIA|BAJA",
                          "duracionDias": 2,
                          "dependencias": ["t0"]
                        }
                      ]
                    }
                  ]
                }
                Reglas:
                - "id" es un identificador breve y ÚNICO de cada tarea (usado para referenciar dependencias).
                - "responsable" debe ser el correo EXACTO de uno de los integrantes listados.
                - "dependencias" referencia los "id" de las tareas predecesoras (finish-to-start).
                - "duracionDias" debe ser un entero mayor que 0.
                - "prioridad" solo puede ser ALTA, MEDIA o BAJA.
                No inventes correos; usa únicamente los integrantes provistos.
                """;
    }

    private String construirPromptUsuario(Proyecto p, List<Integrante> equipo) {
        StringBuilder sb = new StringBuilder();
        sb.append("Planifica el siguiente proyecto de software:\n\n");
        sb.append("NOMBRE: ").append(nvl(p.getNombre())).append("\n");
        sb.append("DESCRIPCIÓN: ").append(nvl(p.getDescripcion())).append("\n");
        sb.append("PROBLEMÁTICA: ").append(nvl(p.getProblematica())).append("\n");
        sb.append("OBJETIVO GENERAL: ").append(nvl(p.getObjetivoGeneral())).append("\n");
        sb.append("OBJETIVOS ESPECÍFICOS: ").append(nvl(p.getObjetivosEspecificos())).append("\n");
        sb.append("ALCANCE: ").append(nvl(p.getAlcance())).append("\n");
        sb.append("RESTRICCIONES: ").append(nvl(p.getRestricciones())).append("\n");
        sb.append("FECHA DE ENTREGA: ")
                .append(p.getFechaEntrega() == null ? "sin definir" : p.getFechaEntrega()).append("\n\n");
        sb.append("TECNOLOGÍAS: ").append(p.getTecnologias() == null ? "" :
                p.getTecnologias().stream().map(t -> t.getNombre()).collect(Collectors.joining(", "))).append("\n\n");
        sb.append("REQUERIMIENTOS FUNCIONALES:\n");
        if (p.getRequerimientosFuncionales() == null || p.getRequerimientosFuncionales().isEmpty()) {
            sb.append("- (sin requerimientos funcionales)\n");
        } else {
            for (var rf : p.getRequerimientosFuncionales()) {
                sb.append("- ").append(rf.getCodigo() == null ? "" : rf.getCodigo() + " ")
                        .append(nvl(rf.getDescripcion())).append("\n");
            }
        }
        sb.append("\nREQUERIMIENTOS NO FUNCIONALES:\n");
        if (p.getRequerimientosNoFuncionales() == null || p.getRequerimientosNoFuncionales().isEmpty()) {
            sb.append("- (sin requerimientos no funcionales)\n");
        } else {
            for (var rnf : p.getRequerimientosNoFuncionales()) {
                sb.append("- ").append(rnf.getCodigo() == null ? "" : rnf.getCodigo() + " ")
                        .append(rnf.getCategoria() == null ? "" : "(" + rnf.getCategoria() + ") ")
                        .append(nvl(rnf.getDescripcion())).append("\n");
            }
        }
        sb.append("\nCASOS DE USO:\n");
        if (p.getCasosDeUso() == null || p.getCasosDeUso().isEmpty()) {
            sb.append("- (sin casos de uso)\n");
        } else {
            for (var cu : p.getCasosDeUso()) {
                sb.append("- ").append(cu.getCodigo() == null ? "" : cu.getCodigo() + " ")
                        .append(nvl(cu.getNombre()))
                        .append(": ").append(nvl(cu.getDescripcion())).append("\n");
            }
        }
        sb.append("\nEQUIPO (correo — nombre | habilidades):\n");
        for (Integrante i : equipo) {
            Usuario u = i.getUsuario();
            String habilidades = u.getHabilidades() == null ? "" : u.getHabilidades().stream()
                    .map(uh -> uh.getHabilidad().getNombre())
                    .collect(Collectors.joining(", "));
            sb.append("- ").append(u.getEmail())
                    .append(" (rol: ").append(i.getRol()).append(")")
                    .append(" — habilidades: ").append(habilidades).append("\n");
        }
        return sb.toString();
    }

    private String limpiarJson(String json) {
        if (json == null) {
            return json;
        }
        String limpio = json.trim();
        if (limpio.startsWith("```")) {
            int inicio = limpio.indexOf('\n');
            if (inicio >= 0 && inicio + 1 < limpio.length()) {
                limpio = limpio.substring(inicio + 1);
            }
            int fin = limpio.lastIndexOf("```");
            if (fin >= 0) {
                limpio = limpio.substring(0, fin);
            }
        }
        int corchete = limpio.indexOf('{');
        if (corchete > 0) {
            limpio = limpio.substring(corchete);
        }
        int cierre = limpio.lastIndexOf('}');
        if (cierre >= 0 && cierre + 1 < limpio.length()) {
            limpio = limpio.substring(0, cierre + 1);
        }
        return limpio;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String nvl(String s) {
        return s == null || s.isBlank() ? "sin información" : s;
    }

@Transactional(readOnly = true)
    public PlanificacionResponse obtener(Long proyectoId) {
        Usuario u = usuarioActual();
        if (!integranteRepository.existsByUsuarioIdAndProyectoId(u.getId(), proyectoId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No perteneces a este proyecto");
        }
        Planificacion plan = planificacionRepository.findByProyectoId(proyectoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El proyecto no tiene planificación"));
        return aResponse(plan);
    }

    private PlanificacionResponse aResponse(Planificacion plan) {
        Map<Long, List<Long>> deps = dependenciasDe(plan.getId());
        List<EtapaResponse> etapas = plan.getEtapas().stream()
                .sorted(Comparator.comparing(Etapa::getOrden, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(e -> new EtapaResponse(e.getId(), e.getNombre(), e.getOrden(),
                        e.getTareas().stream()
                                .sorted(Comparator.comparing(Tarea::getOrden,
                                        Comparator.nullsLast(Comparator.naturalOrder())))
                                .map(t -> new TareaResponse(
                                        t.getId(),
                                        t.getNombre(),
                                        t.getDescripcion(),
                                        t.getResponsable() != null
                                                ? t.getResponsable().getUsuario().getEmail()
                                                : null,
                                        t.getPrioridad(),
                                        t.getEstado(),
                                        t.getDuracionDias(),
                                        t.getFechaInicio(),
                                        t.getFechaFin(),
                                        deps.getOrDefault(t.getId(), List.of())))
                                .toList()))
                .toList();
        return new PlanificacionResponse(plan.getId(), plan.getProyecto().getId(), plan.getEstado(), etapas);
    }

    private Map<Long, List<Long>> dependenciasDe(Long planificacionId) {
        return dependenciaTareaRepository.findByPlanificacionId(planificacionId).stream()
                .collect(Collectors.groupingBy(
                        d -> d.getTarea().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(d -> d.getTareaPredecesora().getId(), Collectors.toList())));
    }

    private void verificarCreador(Usuario u, Long proyectoId) {
        integranteRepository.findByUsuarioIdAndProyectoId(u.getId(), proyectoId)
                .filter(i -> "CREADOR".equals(i.getRol()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Solo el Creador puede generar la planificación"));
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
