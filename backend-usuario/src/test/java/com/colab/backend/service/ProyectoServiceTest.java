package com.colab.backend.service;

import com.colab.backend.domain.*;
import com.colab.backend.dto.*;
import com.colab.backend.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProyectoServiceTest {
    private final ProyectoRepository proyectos = mock(ProyectoRepository.class);
    private final IntegranteRepository integrantes = mock(IntegranteRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final TecnologiaRepository tecnologias = mock(TecnologiaRepository.class);
    private final ProyectoService service = new ProyectoService(proyectos, integrantes, usuarios, tecnologias);
    private final Proyecto proyecto = new Proyecto();

    @BeforeEach
    void preparar() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("ana@example.com", null, List.of()));
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        when(usuarios.findByEmail("ana@example.com")).thenReturn(Optional.of(usuario));
        proyecto.setId(2L);
        when(proyectos.findById(2L)).thenReturn(Optional.of(proyecto));
        Integrante creador = new Integrante();
        creador.setRol("CREADOR");
        when(integrantes.findByUsuarioIdAndProyectoId(1L, 2L)).thenReturn(Optional.of(creador));
        when(integrantes.existsByUsuarioIdAndProyectoId(1L, 2L)).thenReturn(true);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void crearMapeaYDevuelveTodosLosDetalles() {
        CasoUsoDto caso = casoDetallado();

        ProyectoResponse respuesta = service.crearProyecto(request(caso));

        assertThat(respuesta.casosDeUso()).containsExactly(caso);
        ArgumentCaptor<Proyecto> guardado = ArgumentCaptor.forClass(Proyecto.class);
        verify(proyectos).save(guardado.capture());
        CasoUso entidad = guardado.getValue().getCasosDeUso().getFirst();
        assertThat(entidad.getProyecto()).isSameAs(guardado.getValue());
        assertThat(entidad.getActores()).isEqualTo(caso.actores());
        assertThat(entidad.getPrecondiciones()).isEqualTo(caso.precondiciones());
        assertThat(entidad.getDisparador()).isEqualTo(caso.disparador());
        assertThat(entidad.getFlujoPrincipal()).isEqualTo(caso.flujoPrincipal());
        assertThat(entidad.getFlujosAlternativos()).isEqualTo(caso.flujosAlternativos());
        assertThat(entidad.getExcepciones()).isEqualTo(caso.excepciones());
        assertThat(entidad.getPostcondiciones()).isEqualTo(caso.postcondiciones());
        assertThat(entidad.getReglasDeNegocio()).isEqualTo(caso.reglasDeNegocio());
        verify(integrantes).save(any(Integrante.class));
    }

    @Test
    void crearYEditarRequerimientosSinCodigos() {
        RequerimientoFuncionalDto rf = new RequerimientoFuncionalDto("Permitir crear proyectos");
        RequerimientoNoFuncionalDto rnf = new RequerimientoNoFuncionalDto("Proteger los datos", "Seguridad");
        ProyectoRequest request = new ProyectoRequest("ColabTask", null, null, null, null, null, null, null,
                null, List.of(rf), List.of(rnf), null);

        ProyectoResponse creado = service.crearProyecto(request);
        assertThat(creado.requerimientosFuncionales()).containsExactly(rf);
        assertThat(creado.requerimientosNoFuncionales()).containsExactly(rnf);

        ProyectoResponse editado = service.editarProyecto(2L, request);
        assertThat(editado.requerimientosFuncionales()).containsExactly(rf);
        assertThat(editado.requerimientosNoFuncionales()).containsExactly(rnf);
        assertThat(proyecto.getRequerimientosFuncionales().getFirst().getDescripcion()).isEqualTo(rf.descripcion());
        assertThat(proyecto.getRequerimientosNoFuncionales().getFirst().getCategoria()).isEqualTo("Seguridad");
    }

    @Test
    void editarActualizaLosDetallesYPermiteConsultarlos() {
        service.editarProyecto(2L, request(casoDetallado()));

        assertThat(service.verProyecto(2L).casosDeUso()).containsExactly(casoDetallado());
    }

    @Test
    void editarPuedeEliminarDetallesOpcionales() {
        service.editarProyecto(2L, request(casoDetallado()));
        CasoUsoDto basico = new CasoUsoDto("Invitar integrante", null, null,
                null, null, null, null, null, null, null);

        ProyectoResponse respuesta = service.editarProyecto(2L, request(basico));

        assertThat(respuesta.casosDeUso()).containsExactly(basico);
        assertThat(proyecto.getCasosDeUso()).hasSize(1);
        assertThat(proyecto.getCasosDeUso().getFirst().getPrecondiciones()).isNull();
    }

    private CasoUsoDto casoDetallado() {
        return new CasoUsoDto("Invitar integrante", "Invitar al equipo", "Creador",
                "Sesión iniciada", "Seleccionar Invitar", "Registrar y notificar", "Cancelar",
                "Invitación duplicada", "Invitación pendiente", "Solo el creador invita");
    }

    private ProyectoRequest request(CasoUsoDto caso) {
        return new ProyectoRequest("ColabTask", null, null, null, null, null, null, null,
                null, null, null, List.of(caso));
    }
}