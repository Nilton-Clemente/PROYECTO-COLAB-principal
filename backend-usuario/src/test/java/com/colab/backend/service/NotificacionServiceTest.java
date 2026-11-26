package com.colab.backend.service;

import com.colab.backend.domain.Notificacion;
import com.colab.backend.domain.Proyecto;
import com.colab.backend.domain.Usuario;
import com.colab.backend.repository.NotificacionRepository;
import com.colab.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificacionServiceTest {
    private final NotificacionRepository notificaciones = mock(NotificacionRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final NotificacionService service = new NotificacionService(notificaciones, usuarios);
    private final Usuario usuario = new Usuario();

    @BeforeEach
    void preparar() {
        usuario.setId(2L); usuario.setEmail("alex@example.test");
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(usuario.getEmail(), null, List.of()));
        when(usuarios.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
    }

    @AfterEach
    void limpiar() { SecurityContextHolder.clearContext(); }

    @Test
    void crearGuardaElMensajeCompletoSinLeerParaElDestinatario() {
        service.crear(usuario, "PLANIFICACION", "Ana solicitó cambios\nTK-21: revisar cupos");
        var captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).save(captor.capture());
        assertThat(captor.getValue().getUsuario()).isSameAs(usuario);
        assertThat(captor.getValue().getMensaje()).isEqualTo("Ana solicitó cambios\nTK-21: revisar cupos");
        assertThat(captor.getValue().isLeida()).isFalse();
        assertThat(captor.getValue().getProyectoId()).isNull();
        assertThat(captor.getValue().getProyectoNombre()).isNull();
    }

    @Test
    void guardaElProyectoComoContextoHistoricoYLoDevuelveAlListarYLeer() {
        var proyecto = new Proyecto(); proyecto.setId(21L); proyecto.setNombre("Matrícula");
        service.crear(usuario, "PLANIFICACION", "Planificación activada", proyecto);
        var captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).save(captor.capture());
        var aviso = captor.getValue(); aviso.setId(7L);
        proyecto.setNombre("Nombre nuevo");
        assertThat(aviso.getProyectoId()).isEqualTo(21L);
        assertThat(aviso.getProyectoNombre()).isEqualTo("Matrícula");
        when(notificaciones.findByUsuarioIdOrderByFechaCreacionDesc(2L)).thenReturn(List.of(aviso));
        when(notificaciones.findById(7L)).thenReturn(Optional.of(aviso));
        assertThat(service.listar()).singleElement().satisfies(n -> {
            assertThat(n.proyectoId()).isEqualTo(21L);
            assertThat(n.proyectoNombre()).isEqualTo("Matrícula");
            assertThat(n.proyectoCodigo()).isEqualTo("TEC-21");
        });
        var leida = service.marcarLeida(7L);
        assertThat(leida.leida()).isTrue();
        assertThat(leida.proyectoId()).isEqualTo(21L);
        assertThat(leida.proyectoNombre()).isEqualTo("Matrícula");
        assertThat(leida.proyectoCodigo()).isEqualTo("TEC-21");
    }

    @Test
    void listaSoloLosAvisosDeLaCuentaAutenticadaYCuentaSinLeer() {
        var aviso = aviso(usuario);
        when(notificaciones.findByUsuarioIdOrderByFechaCreacionDesc(2L)).thenReturn(List.of(aviso));
        when(notificaciones.countByUsuarioIdAndLeidaFalse(2L)).thenReturn(1L);
        assertThat(service.listar()).singleElement().satisfies(n -> {
            assertThat(n.id()).isEqualTo(7L);
            assertThat(n.mensaje()).isEqualTo(aviso.getMensaje());
            assertThat(n.leida()).isFalse();
            assertThat(n.proyectoId()).isNull();
            assertThat(n.proyectoNombre()).isNull();
            assertThat(n.proyectoCodigo()).isNull();
        });
        assertThat(service.noLeidas()).isEqualTo(1L);
    }

    @Test
    void marcarLeidaConservaElMensajeYEsIdempotente() {
        var aviso = aviso(usuario);
        when(notificaciones.findById(7L)).thenReturn(Optional.of(aviso));
        assertThat(service.marcarLeida(7L).leida()).isTrue();
        assertThat(service.marcarLeida(7L).mensaje()).isEqualTo(aviso.getMensaje());
        verify(notificaciones, times(2)).save(aviso);
    }

    @Test
    void noPermiteMarcarUnaNotificacionDeOtraCuenta() {
        var otro = new Usuario(); otro.setId(99L);
        var aviso = aviso(otro);
        when(notificaciones.findById(7L)).thenReturn(Optional.of(aviso));
        assertThatThrownBy(() -> service.marcarLeida(7L))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        assertThat(aviso.isLeida()).isFalse();
        verify(notificaciones, never()).save(any());
    }

    @Test
    void notificacionInexistenteNoSeMarcaComoLeida() {
        when(notificaciones.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.marcarLeida(7L))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        verify(notificaciones, never()).save(any());
    }

    @Test
    void eliminarLeidasUsaSoloLaCuentaAutenticadaYNoRecibeIdsDelCliente() {
        service.eliminarLeidas();
        verify(notificaciones).eliminarLeidasDeUsuario(2L);
        verifyNoMoreInteractions(notificaciones);
    }

    @Test
    void eliminarLeidasSinSesionNoEjecutaElBorrado() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(service::eliminarLeidas)
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("401");
        verifyNoInteractions(notificaciones);
    }

    @Test
    void eliminarLeidasSinUsuarioRegistradoNoEjecutaElBorrado() {
        when(usuarios.findByEmail(usuario.getEmail())).thenReturn(Optional.empty());
        assertThatThrownBy(service::eliminarLeidas)
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("401");
        verifyNoInteractions(notificaciones);
    }

    @Test
    void eliminarLeidasSinAvisosEsIdempotente() {
        when(notificaciones.eliminarLeidasDeUsuario(2L)).thenReturn(0);
        service.eliminarLeidas();
        service.eliminarLeidas();
        verify(notificaciones, times(2)).eliminarLeidasDeUsuario(2L);
    }

    private Notificacion aviso(Usuario destinatario) {
        var n = new Notificacion(); n.setId(7L); n.setUsuario(destinatario); n.setTipoEvento("PLANIFICACION");
        n.setMensaje("Solicitud de Ana"); n.setFechaCreacion(LocalDateTime.of(2026, 11, 25, 10, 30));
        return n;
    }
}