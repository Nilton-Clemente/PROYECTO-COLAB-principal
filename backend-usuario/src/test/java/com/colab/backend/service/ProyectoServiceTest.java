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
        when(proyectos.findByIdForUpdate(2L)).thenReturn(Optional.of(proyecto));
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
    void crearYEditarRequerimientosConCodigos() {
        RequerimientoFuncionalDto rf = new RequerimientoFuncionalDto("Permitir crear proyectos");
        RequerimientoNoFuncionalDto rnf = new RequerimientoNoFuncionalDto("Proteger los datos", "Seguridad");
        ProyectoRequest request = new ProyectoRequest("ColabTask", null, null, null, null, null, null, null,
                null, List.of(rf), List.of(rnf), null);

        ProyectoResponse creado = service.crearProyecto(request);
        assertThat(creado.requerimientosFuncionales()).containsExactly(new RequerimientoFuncionalDto("RF-01", rf.descripcion()));
        assertThat(creado.requerimientosNoFuncionales()).containsExactly(new RequerimientoNoFuncionalDto("RNF-01", rnf.descripcion(), rnf.categoria()));

        ProyectoResponse editado = service.editarProyecto(2L, request);
        assertThat(editado.requerimientosFuncionales()).containsExactly(new RequerimientoFuncionalDto("RF-01", rf.descripcion()));
        assertThat(editado.requerimientosNoFuncionales()).containsExactly(new RequerimientoNoFuncionalDto("RNF-01", rnf.descripcion(), rnf.categoria()));
        assertThat(proyecto.getRequerimientosFuncionales().getFirst().getDescripcion()).isEqualTo(rf.descripcion());
        assertThat(proyecto.getRequerimientosNoFuncionales().getFirst().getCategoria()).isEqualTo("Seguridad");
    }

    @Test
    void conservaCodigosAlEditarYNoReutilizaLosEliminados() {
        ProyectoRequest primero = requerimientos(List.of(new RequerimientoFuncionalDto("Uno"), new RequerimientoFuncionalDto("Dos")),
                List.of(new RequerimientoNoFuncionalDto("Seguro", "Seguridad"), new RequerimientoNoFuncionalDto("Rápido", "Rendimiento")));
        service.editarProyecto(2L, primero);
        var rfOriginal = proyecto.getRequerimientosFuncionales().get(1);
        var rnfOriginal = proyecto.getRequerimientosNoFuncionales().get(1);
        ProyectoResponse editado = service.editarProyecto(2L, requerimientos(
                List.of(new RequerimientoFuncionalDto("RF-02", "Dos modificado"), new RequerimientoFuncionalDto("Tres")),
                List.of(new RequerimientoNoFuncionalDto("RNF-02", "Más rápido", "Rendimiento"), new RequerimientoNoFuncionalDto("Disponible", "Disponibilidad"))));
        assertThat(editado.requerimientosFuncionales()).extracting(RequerimientoFuncionalDto::codigo).containsExactly("RF-02", "RF-03");
        assertThat(editado.requerimientosNoFuncionales()).extracting(RequerimientoNoFuncionalDto::codigo).containsExactly("RNF-02", "RNF-03");
        assertThat(proyecto.getRequerimientosFuncionales().getFirst()).isSameAs(rfOriginal);
        assertThat(proyecto.getRequerimientosNoFuncionales().getFirst()).isSameAs(rnfOriginal);
    }

    @Test
    void rechazaCodigosAjenosYDuplicados() {
        service.editarProyecto(2L, requerimientos(List.of(new RequerimientoFuncionalDto("Uno")), List.of()));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.editarProyecto(2L, requerimientos(
                List.of(new RequerimientoFuncionalDto("RF-99", "Ajeno")), List.of())))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.editarProyecto(2L, requerimientos(
                List.of(new RequerimientoFuncionalDto("RF-01", "Uno"), new RequerimientoFuncionalDto("RF-01", "Dos")), List.of())))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    private ProyectoRequest requerimientos(List<RequerimientoFuncionalDto> rf, List<RequerimientoNoFuncionalDto> rnf) {
        return new ProyectoRequest("ColabTask", null, null, null, null, null, null, null, null, rf, rnf, null);
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