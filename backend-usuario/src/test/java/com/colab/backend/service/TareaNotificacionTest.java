package com.colab.backend.service;

import com.colab.backend.domain.*;
import com.colab.backend.dto.TareaRequest;
import com.colab.backend.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TareaNotificacionTest {
    private final TareaRepository tareas = mock(TareaRepository.class);
    private final EtapaRepository etapas = mock(EtapaRepository.class);
    private final IntegranteRepository integrantes = mock(IntegranteRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PlanificacionRepository planes = mock(PlanificacionRepository.class);
    private final DependenciaTareaRepository dependencias = mock(DependenciaTareaRepository.class);
    private final NotificacionService notificaciones = mock(NotificacionService.class);
    private final CicloProyectoService ciclo = mock(CicloProyectoService.class);
    private final TareaService service = new TareaService(tareas, etapas, integrantes, usuarios, planes,
            dependencias, notificaciones, ciclo);
    private final Usuario usuario = new Usuario();
    private final Integrante integrante = new Integrante();
    private final Proyecto proyecto = new Proyecto();
    private final Planificacion plan = new Planificacion();
    private final Etapa etapa = new Etapa();
    private final Tarea tarea = new Tarea();

    @BeforeEach
    void preparar() {
        usuario.setId(1L); usuario.setEmail("ana@example.test");
        proyecto.setId(7L); proyecto.setNombre("Matrícula");
        integrante.setId(2L); integrante.setUsuario(usuario); integrante.setProyecto(proyecto);
        integrante.setRol("CREADOR"); integrante.setEstado("ACTIVO");
        plan.setId(3L); plan.setProyecto(proyecto); plan.setEstado("PROPUESTA");
        etapa.setId(4L); etapa.setNombre("Desarrollo"); etapa.setPlanificacion(plan);
        tarea.setId(5L); tarea.setNombre("API"); tarea.setEtapa(etapa); tarea.setResponsable(integrante);
        tarea.setEstado("PENDIENTE");
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(usuario.getEmail(), null, List.of()));
        when(usuarios.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(integrantes.findByUsuarioIdAndProyectoId(1L, 7L)).thenReturn(Optional.of(integrante));
        when(integrantes.findById(2L)).thenReturn(Optional.of(integrante));
        when(integrantes.findByProyectoId(7L)).thenReturn(List.of(integrante));
        when(planes.findByProyectoId(7L)).thenReturn(Optional.of(plan));
        when(etapas.findById(4L)).thenReturn(Optional.of(etapa));
        when(tareas.findById(5L)).thenReturn(Optional.of(tarea));
        when(dependencias.findPredecesorasByTareaId(any())).thenReturn(List.of());
        when(dependencias.findSucesorasByTareaId(any())).thenReturn(List.of());
    }

    @AfterEach
    void limpiar() { SecurityContextHolder.clearContext(); }

    @Test
    void asignacionIncluyeNombreCodigoYProyectoReal() {
        service.crear(7L, new TareaRequest("API", null, 4L, 2L, "ALTA", 2, 1));
        verify(notificaciones).crear(usuario, "ASIGNACION",
                "Se te asignó la tarea \"API\" en el proyecto \"Matrícula\" (TEC-7).", proyecto);
        verifyNoMoreInteractions(notificaciones);
    }

    @Test
    void reasignacionIncluyeNombreCodigoYProyectoReal() {
        service.reasignar(7L, 5L, 2L);
        verify(notificaciones).crear(usuario, "CAMBIO_RESPONSABLE",
                "Ahora eres responsable de la tarea \"API\" en el proyecto \"Matrícula\" (TEC-7).", proyecto);
        verifyNoMoreInteractions(notificaciones);
    }

    @Test
    void cambioDeEstadoIncluyeNombreCodigoYProyectoReal() {
        plan.setEstado("ACTIVA");
        service.cambiarEstado(7L, 5L, "EN_PROGRESO");
        verify(notificaciones).crear(usuario, "CAMBIO_ESTADO",
                "La tarea \"API\" del proyecto \"Matrícula\" (TEC-7) cambió de estado a EN_PROGRESO.", proyecto);
        verifyNoMoreInteractions(notificaciones);
    }

    @Test
    void desbloqueoIncluyeElProyectoDeLaTareaLiberada() {
        plan.setEstado("ACTIVA"); tarea.setEstado("EN_PROGRESO");
        var sucesora = new Tarea(); sucesora.setId(6L); sucesora.setNombre("Pruebas");
        sucesora.setEtapa(etapa); sucesora.setEstado("PENDIENTE"); sucesora.setResponsable(integrante);
        var dependencia = new DependenciaTarea(); dependencia.setTarea(sucesora); dependencia.setTareaPredecesora(tarea);
        when(dependencias.findSucesorasByTareaId(5L)).thenReturn(List.of(dependencia));
        service.cambiarEstado(7L, 5L, "COMPLETADA");
        verify(notificaciones).crear(usuario, "DESBLOQUEO",
                "La tarea \"Pruebas\" del proyecto \"Matrícula\" (TEC-7) quedó desbloqueada (sus dependencias se completaron).",
                proyecto);
        verify(notificaciones).crear(eq(usuario), eq("CAMBIO_ESTADO"), contains("TEC-7"), eq(proyecto));
        verifyNoMoreInteractions(notificaciones);
    }
}