package com.colab.backend.service;

import com.colab.backend.domain.*;
import com.colab.backend.dto.*;
import com.colab.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AvanceService {
    private final RegistroAvanceRepository registros;
    private final TareaRepository tareas;
    private final UsuarioRepository usuarios;
    private final IntegranteRepository integrantes;
    private final TareaService tareaService;

    public AvanceService(RegistroAvanceRepository registros, TareaRepository tareas,
                         UsuarioRepository usuarios, IntegranteRepository integrantes, TareaService tareaService) {
        this.registros = registros;
        this.tareas = tareas;
        this.usuarios = usuarios;
        this.integrantes = integrantes;
        this.tareaService = tareaService;
    }

    @Transactional
    public RegistroAvanceResponse registrar(Long proyectoId, Long tareaId, RegistroAvanceRequest request) {
        Integrante integrante = integranteActual(proyectoId);
        Tarea tarea = tareaDe(proyectoId, tareaId);
        if (!"CREADOR".equals(integrante.getRol()) && (tarea.getResponsable() == null
                || !integrante.getId().equals(tarea.getResponsable().getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el responsable o el creador pueden registrar avances");
        }
        RegistroAvance registro = new RegistroAvance();
        registro.setTarea(tarea);
        registro.setUsuario(integrante.getUsuario());
        registro.setMinutos(request.minutos());
        registro.setFecha(LocalDateTime.now());
        registro.setNota(request.nota());
        return aResponse(registros.save(registro));
    }

    @Transactional(readOnly = true)
    public HistorialAvanceResponse historial(Long proyectoId, Long tareaId) {
        integranteActual(proyectoId);
        tareaDe(proyectoId, tareaId);
        List<RegistroAvanceResponse> lista = registros.findByTareaIdOrderByFechaDescIdDesc(tareaId)
                .stream().map(this::aResponse).toList();
        return new HistorialAvanceResponse(lista.stream().mapToLong(RegistroAvanceResponse::minutos).sum(), lista);
    }

    @Transactional(readOnly = true)
    public MetricasProyectoResponse metricas(Long proyectoId) {
        integranteActual(proyectoId);
        List<TareaDetalleResponse> lista = tareaService.listarTodas(proyectoId);
        List<MetricasProyectoResponse.MetricasIntegrante> responsables = integrantes.findByProyectoId(proyectoId).stream()
                .filter(i -> "ACTIVO".equals(i.getEstado()))
                .sorted(java.util.Comparator.comparing(Integrante::getId))
                .map(i -> {
                    List<TareaDetalleResponse> propias = lista.stream().filter(t -> i.getId().equals(t.responsableId())).toList();
                    return new MetricasProyectoResponse.MetricasIntegrante(i.getId(), i.getUsuario().getEmail(),
                            propias.size(), contar(propias, "PENDIENTE"), contar(propias, "EN_PROGRESO"),
                            contar(propias, "COMPLETADA"), contar(propias, "BLOQUEADA"));
                }).toList();
        long completadas = contar(lista, "COMPLETADA");
        return new MetricasProyectoResponse(lista.size(), contar(lista, "PENDIENTE"), contar(lista, "EN_PROGRESO"),
                completadas, contar(lista, "BLOQUEADA"), lista.isEmpty() ? 0 : 100.0 * completadas / lista.size(), responsables);
    }

    private long contar(List<TareaDetalleResponse> lista, String estado) {
        return lista.stream().filter(t -> estado.equals(t.estado())).count();
    }

    private Integrante integranteActual(Long proyectoId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        Usuario usuario = usuarios.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        return integrantes.findByUsuarioIdAndProyectoId(usuario.getId(), proyectoId)
                .filter(i -> "ACTIVO".equals(i.getEstado()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No eres integrante activo del proyecto"));
    }

    private Tarea tareaDe(Long proyectoId, Long tareaId) {
        return tareas.findById(tareaId)
                .filter(t -> proyectoId.equals(t.getEtapa().getPlanificacion().getProyecto().getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada en el proyecto"));
    }

    private RegistroAvanceResponse aResponse(RegistroAvance r) {
        return new RegistroAvanceResponse(r.getId(), r.getTarea().getId(), r.getUsuario().getId(),
                r.getUsuario().getNombre(), r.getMinutos(), r.getFecha(), r.getNota());
    }
}