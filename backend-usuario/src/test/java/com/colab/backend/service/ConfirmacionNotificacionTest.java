package com.colab.backend.service;

import com.colab.backend.domain.*;
import com.colab.backend.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConfirmacionNotificacionTest {
    private final PlanificacionRepository planes = mock(PlanificacionRepository.class);
    private final ConfirmacionRepository confirmaciones = mock(ConfirmacionRepository.class);
    private final IntegranteRepository integrantes = mock(IntegranteRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final NotificacionService notificaciones = mock(NotificacionService.class);
    private final CicloProyectoService ciclo = mock(CicloProyectoService.class);
    private final ConfirmacionService service = new ConfirmacionService(planes, confirmaciones, integrantes, usuarios, notificaciones, ciclo);
    private final Usuario solicitante = new Usuario();
    private final Usuario propietario = new Usuario();
    private final Integrante integrante = new Integrante();
    private final Integrante creador = new Integrante();
    private final Planificacion plan = new Planificacion();

    @BeforeEach
    void preparar() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("ana@example.test", null, List.of()));
        solicitante.setId(1L); solicitante.setNombre("Ana"); solicitante.setEmail("ana@example.test");
        propietario.setId(2L); propietario.setNombre("Alex");
        integrante.setId(11L); integrante.setUsuario(solicitante); integrante.setRol("INTEGRANTE"); integrante.setEstado("ACTIVO");
        creador.setId(12L); creador.setUsuario(propietario); creador.setRol("CREADOR"); creador.setEstado("ACTIVO");
        Proyecto proyecto = new Proyecto(); proyecto.setId(7L); proyecto.setNombre("Matrícula");
        plan.setId(3L); plan.setProyecto(proyecto); plan.setEstado("PROPUESTA");
        when(usuarios.findByEmail("ana@example.test")).thenReturn(Optional.of(solicitante));
        when(planes.findByProyectoId(7L)).thenReturn(Optional.of(plan));
        when(integrantes.findByUsuarioIdAndProyectoId(1L, 7L)).thenReturn(Optional.of(integrante));
        when(integrantes.findByProyectoId(7L)).thenReturn(List.of(integrante, creador));
        when(confirmaciones.findByPlanificacionIdAndIntegranteId(3L, 11L)).thenReturn(Optional.empty());
        when(confirmaciones.findByPlanificacionId(3L)).thenReturn(List.of());
    }

    @AfterEach
    void limpiar() { SecurityContextHolder.clearContext(); }

    @Test
    void solicitudNotificaSoloAlCreadorConProyectoPersonaYComentarioCompleto() {
        String comentario = "TK-21 · API de matrícula\nNecesito revisar los cupos y la duración.";
        var respuesta = service.solicitarCambios(7L, comentario);
        assertThat(respuesta.estado()).isEqualTo("CAMBIOS_SOLICITADOS");
        assertThat(respuesta.comentario()).isEqualTo(comentario);
        verify(notificaciones).crear(propietario, "PLANIFICACION",
                "Ana solicitó cambios en la planificación del proyecto \"Matrícula\" (TEC-7): " + comentario,
                plan.getProyecto());
        verifyNoMoreInteractions(notificaciones);
    }

    @Test
    void activacionNotificaConNombreCodigoYProyectoDeOrigen() {
        var etapa = new Etapa(); etapa.setPlanificacion(plan);
        var tarea = new Tarea(); tarea.setResponsable(integrante); tarea.setEtapa(etapa);
        etapa.getTareas().add(tarea); plan.getEtapas().add(etapa);
        var aceptada = new Confirmacion(); aceptada.setIntegrante(integrante); aceptada.setEstado("ACEPTADA");
        when(confirmaciones.findByPlanificacionId(3L)).thenReturn(List.of(aceptada));
        service.confirmar(7L);
        assertThat(plan.getEstado()).isEqualTo("ACTIVA");
        verify(notificaciones).crear(solicitante, "PLANIFICACION",
                "La planificación del proyecto \"Matrícula\" (TEC-7) quedó ACTIVA.", plan.getProyecto());
        verifyNoMoreInteractions(notificaciones);
    }

    @Test
    void solicitudReiniciaOtrasConfirmacionesSinBorrarLaDelSolicitante() {
        Confirmacion propia = new Confirmacion(); propia.setIntegrante(integrante);
        Confirmacion otra = new Confirmacion(); otra.setIntegrante(creador);
        when(confirmaciones.findByPlanificacionId(3L)).thenReturn(List.of(propia, otra));
        service.solicitarCambios(7L, "Revisar la duración");
        verify(confirmaciones).deleteAll(List.of(otra));
        assertThat(plan.getEstado()).isEqualTo("PROPUESTA");
    }

    @Test
    void noGeneraNotificacionSiLaPlanificacionYaEstaActiva() {
        plan.setEstado("ACTIVA");
        assertThatThrownBy(() -> service.solicitarCambios(7L, "Cambios"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        verifyNoInteractions(notificaciones);
        verify(confirmaciones, never()).save(any());
    }

    @Test
    void integrantePendienteNoPuedeEnviarSolicitudNiNotificar() {
        integrante.setEstado("PENDIENTE");
        assertThatThrownBy(() -> service.solicitarCambios(7L, "Cambios"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        verifyNoInteractions(notificaciones);
    }
}