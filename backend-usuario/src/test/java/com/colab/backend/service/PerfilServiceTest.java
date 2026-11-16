package com.colab.backend.service;

import com.colab.backend.domain.Habilidad;
import com.colab.backend.domain.Usuario;
import com.colab.backend.domain.UsuarioHabilidad;
import com.colab.backend.domain.UsuarioHabilidadId;
import com.colab.backend.repository.HabilidadRepository;
import com.colab.backend.repository.UsuarioHabilidadRepository;
import com.colab.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PerfilServiceTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final HabilidadRepository habilidades = mock(HabilidadRepository.class);
    private final UsuarioHabilidadRepository relaciones = mock(UsuarioHabilidadRepository.class);
    private final PerfilService service = new PerfilService(usuarios, habilidades, relaciones);
    private final Usuario usuario = new Usuario();
    private final Habilidad habilidad = new Habilidad();
    private final UsuarioHabilidadId id = new UsuarioHabilidadId(1L, 2L);

    @BeforeEach
    void preparar() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("ana@example.com", null, List.of()));
        usuario.setId(1L);
        usuario.setEmail("ana@example.com");
        usuario.setNombre("Ana");
        habilidad.setId(2L);
        habilidad.setNombre("Java");
        habilidad.setArea("Backend");
        when(usuarios.findByEmail("ana@example.com")).thenReturn(Optional.of(usuario));
    }

    @AfterEach
    void limpiar() { SecurityContextHolder.clearContext(); }

    @ParameterizedTest
    @ValueSource(strings = {"BASICO", "INTERMEDIO", "AVANZADO"})
    void registraNivelValido(String nivel) {
        when(habilidades.findById(2L)).thenReturn(Optional.of(habilidad));
        service.agregarHabilidad(2L, nivel);
        ArgumentCaptor<UsuarioHabilidad> captor = ArgumentCaptor.forClass(UsuarioHabilidad.class);
        verify(relaciones).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(id);
        assertThat(captor.getValue().getUsuario()).isSameAs(usuario);
        assertThat(captor.getValue().getNivel()).isEqualTo(nivel);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "Avanzado", "EXPERTO", "A"})
    void rechazaNivelInvalidoSinGuardar(String nivel) {
        assertThatThrownBy(() -> service.agregarHabilidad(2L, nivel))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(relaciones, never()).save(any());
    }

    @Test
    void actualizaRelacionExistenteYNoBorraNivelSiSeOmite() {
        when(habilidades.findById(2L)).thenReturn(Optional.of(habilidad));
        UsuarioHabilidad existente = relacion("BASICO");
        when(relaciones.findById(id)).thenReturn(Optional.of(existente));
        service.agregarHabilidad(2L, "AVANZADO");
        assertThat(existente.getNivel()).isEqualTo("AVANZADO");
        service.agregarHabilidad(2L, null);
        assertThat(existente.getNivel()).isEqualTo("AVANZADO");
        verify(relaciones, times(2)).save(existente);
    }

    @Test
    void nuevaRelacionSinNivelSigueAdmitida() {
        when(habilidades.findById(2L)).thenReturn(Optional.of(habilidad));
        service.agregarHabilidad(2L, null);
        ArgumentCaptor<UsuarioHabilidad> captor = ArgumentCaptor.forClass(UsuarioHabilidad.class);
        verify(relaciones).save(captor.capture());
        assertThat(captor.getValue().getNivel()).isNull();
    }

    @Test
    void perfilIncluyeNivelYRespetaValorHistoricoNulo() {
        UsuarioHabilidad uh = relacion(null);
        usuario.getHabilidades().add(uh);
        habilidad.setIcono("java");
        assertThat(service.obtenerPerfil().habilidades().getFirst().nivel()).isNull();
        assertThat(service.obtenerPerfil().habilidades().getFirst().icono()).isEqualTo("java");
        uh.setNivel("INTERMEDIO");
        assertThat(service.obtenerPerfil().habilidades().getFirst().nivel()).isEqualTo("INTERMEDIO");
    }

    @Test
    void catalogoIncluyeIconoOpcional() {
        when(habilidades.findAll()).thenReturn(List.of(habilidad));
        assertThat(service.listarCatalogo().getFirst().icono()).isNull();
        habilidad.setIcono("java");
        assertThat(service.listarCatalogo().getFirst().icono()).isEqualTo("java");
    }

    @Test
    void relacionesYEliminacionUsanElUsuarioAutenticado() {
        when(habilidades.findById(2L)).thenReturn(Optional.of(habilidad));
        when(relaciones.existsById(id)).thenReturn(true);
        service.agregarHabilidad(2L, "BASICO");
        verify(relaciones).findById(id);
        service.eliminarHabilidad(2L);
        verify(relaciones).deleteById(id);
        verify(relaciones, never()).deleteById(new UsuarioHabilidadId(99L, 2L));
    }

    @Test
    void habilidadDesconocidaDevuelve404() {
        assertThatThrownBy(() -> service.agregarHabilidad(99L, "BASICO"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private UsuarioHabilidad relacion(String nivel) {
        UsuarioHabilidad relacion = new UsuarioHabilidad();
        relacion.setId(id);
        relacion.setUsuario(usuario);
        relacion.setHabilidad(habilidad);
        relacion.setNivel(nivel);
        return relacion;
    }
}