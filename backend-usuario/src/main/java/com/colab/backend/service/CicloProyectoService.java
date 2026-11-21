package com.colab.backend.service;

import com.colab.backend.domain.EstadoProyecto;
import com.colab.backend.domain.Planificacion;
import com.colab.backend.domain.Proyecto;
import com.colab.backend.repository.ProyectoRepository;
import com.colab.backend.repository.TareaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class CicloProyectoService {
    private final ProyectoRepository proyectos;
    private final TareaRepository tareas;

    public CicloProyectoService(ProyectoRepository proyectos, TareaRepository tareas) {
        this.proyectos = proyectos;
        this.tareas = tareas;
    }

    /** Todas las escrituras del ciclo adquieren el mismo bloqueo antes de consultar el estado. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Proyecto bloquearProyecto(Long proyectoId) {
        return proyectos.findByIdForUpdate(proyectoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no existe"));
    }

    public void verificarEnDesarrollo(Planificacion plan) {
        if (EstadoProyecto.desde(plan) != EstadoProyecto.EN_DESARROLLO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El proyecto no está en desarrollo");
        }
    }

    /** La planificación es gestionada y el proyecto ya está bloqueado en la transacción llamante. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void finalizarSiCompleto(Planificacion plan) {
        if (EstadoProyecto.desde(plan) == EstadoProyecto.EN_DESARROLLO
                && tareas.existsByEtapaPlanificacionId(plan.getId())
                && !tareas.existsByEtapaPlanificacionIdAndEstadoNot(plan.getId(), "COMPLETADA")) {
            plan.setEstado("FINALIZADA");
            plan.setFechaFinalizacion(LocalDateTime.now());
        }
    }
}