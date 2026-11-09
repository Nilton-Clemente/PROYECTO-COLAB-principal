package com.colab.backend.service;

import com.colab.backend.domain.*;
import com.colab.backend.dto.*;
import com.colab.backend.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AvanceServiceTest {
    private final RegistroAvanceRepository registros = mock(RegistroAvanceRepository.class);
    private final TareaRepository tareas = mock(TareaRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final IntegranteRepository integrantes = mock(IntegranteRepository.class);
    private final TareaService tareaService = mock(TareaService.class);
    private final AvanceService service = new AvanceService(registros, tareas, usuarios, integrantes, tareaService);
    private final Usuario usuario = new Usuario();
    private final Integrante integrante = new Integrante();
    private final Tarea tarea = new Tarea();

    @BeforeEach
    void preparar() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("test@example.com", null, List.of()));
        usuario.setId(1L); usuario.setNombre("Ana"); usuario.setEmail("test@example.com");
        integrante.setId(2L); integrante.setUsuario(usuario); integrante.setEstado("ACTIVO"); integrante.setRol("INTEGRANTE");
        Proyecto proyecto = new Proyecto(); proyecto.setId(3L);
        Planificacion plan = new Planificacion(); plan.setProyecto(proyecto);
        Etapa etapa = new Etapa(); etapa.setPlanificacion(plan);
        tarea.setId(4L); tarea.setEtapa(etapa); tarea.setResponsable(integrante);
        when(usuarios.findByEmail("test@example.com")).thenReturn(Optional.of(usuario));
        when(integrantes.findByUsuarioIdAndProyectoId(1L, 3L)).thenReturn(Optional.of(integrante));
        when(integrantes.findByProyectoId(3L)).thenReturn(List.of(integrante));
    }

    @AfterEach
    void limpiar() { SecurityContextHolder.clearContext(); }

    @Test
    void registraSesionDelResponsable() {
        when(tareas.findById(4L)).thenReturn(Optional.of(tarea));
        when(registros.save(any())).thenAnswer(i -> i.getArgument(0));
        RegistroAvanceResponse respuesta = service.registrar(3L, 4L, new RegistroAvanceRequest(30, "Diseño"));
        assertThat(respuesta.minutos()).isEqualTo(30);
        assertThat(respuesta.usuarioId()).isEqualTo(1L);
        assertThat(respuesta.fecha()).isNotNull();
    }

    @Test
    void impideRegistrarSesionEnTareaAjena() {
        tarea.setResponsable(null);
        when(tareas.findById(4L)).thenReturn(Optional.of(tarea));
        assertThatThrownBy(() -> service.registrar(3L, 4L, new RegistroAvanceRequest(30, null)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        verify(registros, never()).save(any());
    }

    @Test
    void historialSumaSesiones() {
        when(tareas.findById(4L)).thenReturn(Optional.of(tarea));
        RegistroAvance a = registro(30); RegistroAvance b = registro(20);
        when(registros.findByTareaIdOrderByFechaDescIdDesc(4L)).thenReturn(List.of(a, b));
        HistorialAvanceResponse historial = service.historial(3L, 4L);
        assertThat(historial.totalMinutos()).isEqualTo(50);
        assertThat(historial.registros()).hasSize(2);
    }

    @Test
    void metricasIncluyenBloqueadasYNoDividenEntreCero() {
        when(tareaService.listarTodas(3L)).thenReturn(List.of(detalle("COMPLETADA", 2L), detalle("BLOQUEADA", null)));
        MetricasProyectoResponse m = service.metricas(3L);
        assertThat(m.totalTareas()).isEqualTo(2);
        assertThat(m.bloqueadas()).isEqualTo(1);
        assertThat(m.porcentajeCompletadas()).isEqualTo(50.0);
        assertThat(m.porResponsable()).hasSize(1);
        when(tareaService.listarTodas(3L)).thenReturn(List.of());
        assertThat(service.metricas(3L).porcentajeCompletadas()).isZero();
    }

    @Test
    void rechazaIntegranteInactivo() {
        integrante.setEstado("PENDIENTE");
        assertThatThrownBy(() -> service.metricas(3L)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
    }

    @Test
    void todasCompletadasAlcanzanCienPorCiento() {
        when(tareaService.listarTodas(3L)).thenReturn(List.of(detalle("COMPLETADA", 2L), detalle("COMPLETADA", 2L)));
        MetricasProyectoResponse m = service.metricas(3L);
        assertThat(m.completadas()).isEqualTo(2);
        assertThat(m.porcentajeCompletadas()).isEqualTo(100.0);
        assertThat(m.porResponsable().get(0).completadas()).isEqualTo(2);
    }

    @Test
    void distribucionIncluyeIntegrantesActivosSinTareasYExcluyeInvitadosPendientes() {
        Usuario otroUsuario = new Usuario(); otroUsuario.setEmail("otro@example.com");
        Integrante otro = new Integrante(); otro.setId(3L); otro.setUsuario(otroUsuario); otro.setEstado("ACTIVO");
        Integrante invitado = new Integrante(); invitado.setId(4L); invitado.setEstado("PENDIENTE");
        Usuario terceroUsuario = new Usuario(); terceroUsuario.setEmail("tercero@example.com");
        Integrante tercero = new Integrante(); tercero.setId(5L); tercero.setUsuario(terceroUsuario); tercero.setEstado("ACTIVO");
        when(integrantes.findByProyectoId(3L)).thenReturn(List.of(tercero, otro, invitado, integrante));
        when(tareaService.listarTodas(3L)).thenReturn(List.of(detalle("COMPLETADA", 2L),
                detalle("EN_PROGRESO", 2L), detalle("COMPLETADA", 3L), detalle("PENDIENTE", null)));
        MetricasProyectoResponse m = service.metricas(3L);
        assertThat(m.totalTareas()).isEqualTo(4);
        assertThat(m.porResponsable()).hasSize(3);
        assertThat(m.porResponsable().get(0).totalTareas()).isEqualTo(2);
        assertThat(m.porResponsable().get(0).completadas()).isEqualTo(1);
        assertThat(m.porResponsable().get(1).email()).isEqualTo("otro@example.com");
        assertThat(m.porResponsable().get(1).totalTareas()).isEqualTo(1);
        assertThat(m.porResponsable().get(1).completadas()).isEqualTo(1);
        assertThat(m.porResponsable().get(2).totalTareas()).isZero();
        assertThat(m.porResponsable().get(2).completadas()).isZero();
    }

    @Test
    void metricasReflejanCambioDeEstadoEnSiguienteConsulta() {
        when(tareaService.listarTodas(3L)).thenReturn(List.of(detalle("EN_PROGRESO", 2L)),
                List.of(detalle("COMPLETADA", 2L)));
        assertThat(service.metricas(3L).porcentajeCompletadas()).isZero();
        assertThat(service.metricas(3L).porcentajeCompletadas()).isEqualTo(100.0);
    }

    private RegistroAvance registro(int minutos) {
        RegistroAvance r = new RegistroAvance(); r.setTarea(tarea); r.setUsuario(usuario);
        r.setMinutos(minutos); r.setFecha(LocalDateTime.now()); return r;
    }

    private TareaDetalleResponse detalle(String estado, Long responsableId) {
        return new TareaDetalleResponse(4L, "Tarea", null, 5L, "Etapa", responsableId,
                "test@example.com", "MEDIA", estado, 1, null, null, 1, List.of());
    }
}