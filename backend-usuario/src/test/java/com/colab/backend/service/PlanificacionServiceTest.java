package com.colab.backend.service;

import com.colab.backend.domain.*;
import com.colab.backend.dto.PlanificacionResponse;
import com.colab.backend.dto.PlanificacionUpdateRequest;
import com.colab.backend.ia.PlanIAClient;
import com.colab.backend.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PlanificacionServiceTest {
    private final ProyectoRepository proyectos = mock(ProyectoRepository.class);
    private final IntegranteRepository integrantes = mock(IntegranteRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PlanificacionRepository planificaciones = mock(PlanificacionRepository.class);
    private final DependenciaTareaRepository dependencias = mock(DependenciaTareaRepository.class);
    private final ConfirmacionRepository confirmaciones = mock(ConfirmacionRepository.class);
    private final PlanIAClient ia = mock(PlanIAClient.class);
    private final PlanificacionService service = new PlanificacionService(proyectos, integrantes, usuarios,
            planificaciones, dependencias, confirmaciones, ia, new ObjectMapper());
    private final Proyecto proyecto = new Proyecto();

    @BeforeEach
    void preparar() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("ana@example.com", null, List.of()));
        proyecto.setId(1L);
        proyecto.setNombre("ColabTask");
        proyecto.setObjetivoGeneral("Organizar proyectos");
        proyecto.setFechaEntrega(LocalDate.now().plusDays(30));
        Tecnologia tecnologia = new Tecnologia();
        tecnologia.setNombre("Java");
        proyecto.getTecnologias().add(tecnologia);
        RequerimientoFuncional requerimiento = new RequerimientoFuncional();
        requerimiento.setDescripcion("Invitar integrantes");
        proyecto.getRequerimientosFuncionales().add(requerimiento);
        Integrante creador = integrante(2L, "ana@example.com");
        creador.setRol("CREADOR");
        Integrante segundo = integrante(3L, "luis@example.com");
        when(usuarios.findByEmail("ana@example.com")).thenReturn(Optional.of(creador.getUsuario()));
        when(proyectos.findById(1L)).thenReturn(Optional.of(proyecto));
        when(integrantes.findByUsuarioIdAndProyectoId(2L, 1L)).thenReturn(Optional.of(creador));
        when(integrantes.findByProyectoId(1L)).thenReturn(List.of(creador, segundo));
        when(ia.generarPlanificacionJson(anyString(), anyString(), anyList())).thenReturn("""
                {"etapas":[{"nombre":"Desarrollo","orden":1,"tareas":[
                  {"id":"t1","nombre":"Implementar invitaciones","duracionDias":1,"dependencias":[]}
                ]}]}
                """);
        when(planificaciones.saveAndFlush(any())).thenAnswer(invocation -> {
            Planificacion plan = invocation.getArgument(0);
            plan.setId(4L);
            plan.getEtapas().forEach(e -> {
                e.setId(5L);
                e.getTareas().forEach(t -> t.setId(6L));
            });
            return plan;
        });
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void enviaEspecificacionCompletaAlClienteIa() {
        CasoUso caso = new CasoUso();
        caso.setNombre("Invitar integrante");
        caso.setDescripcion("Invitar al equipo");
        caso.setActores("Creador y destinatario");
        caso.setPrecondiciones("Usuario registrado");
        caso.setDisparador("Seleccionar Invitar");
        caso.setFlujoPrincipal("1. Registrar invitación.\n2. Notificar al destinatario.");
        caso.setFlujosAlternativos("El creador cancela");
        caso.setExcepciones("Rechazar invitaciones duplicadas");
        caso.setPostcondiciones("Invitación pendiente");
        caso.setReglasDeNegocio("Incorporar solo después de aceptar");
        proyecto.getCasosDeUso().add(caso);

        assertThat(service.generar(1L).estado()).isEqualTo("PROPUESTA");

        ArgumentCaptor<String> sistema = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> usuario = ArgumentCaptor.forClass(String.class);
        verify(ia).generarPlanificacionJson(sistema.capture(), usuario.capture(),
                eq(List.of("ana@example.com", "luis@example.com")));
        assertThat(usuario.getValue()).contains(
                "- Invitar integrante: Invitar al equipo",
                "Actores: Creador y destinatario",
                "Precondiciones: Usuario registrado",
                "Disparador: Seleccionar Invitar",
                "Flujo principal: 1. Registrar invitación.\n2. Notificar al destinatario.",
                "Flujos alternativos: El creador cancela",
                "Excepciones: Rechazar invitaciones duplicadas",
                "Postcondiciones: Invitación pendiente",
                "Reglas de negocio: Incorporar solo después de aceptar");
        assertThat(sistema.getValue()).contains("no inventes reglas faltantes", "tareas de aclaración",
                "tareas de implementación y pruebas");
    }

    @Test
    void permiteCasosAnterioresSinDetallesOpcionales() {
        CasoUso caso = new CasoUso();
        caso.setNombre("Invitar integrante");
        caso.setActores(" ");
        proyecto.getCasosDeUso().add(caso);

        service.generar(1L);

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(ia).generarPlanificacionJson(anyString(), prompt.capture(), anyList());
        assertThat(prompt.getValue()).contains("Invitar integrante: sin información")
                .doesNotContain("Actores:", "Precondiciones:", "null");
    }

    @Test
    void enviaRequerimientosSinCodigosYConCategoria() {
        RequerimientoNoFuncional rnf = new RequerimientoNoFuncional();
        rnf.setDescripcion("Proteger los datos");
        rnf.setCategoria("Seguridad");
        proyecto.getRequerimientosNoFuncionales().add(rnf);

        service.generar(1L);

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(ia).generarPlanificacionJson(anyString(), prompt.capture(), anyList());
        assertThat(prompt.getValue()).contains("- Invitar integrantes", "(Seguridad) Proteger los datos")
                .doesNotContain("RF-", "RNF-", "CU-");
    }

    @Test
    void permiteProyectosSinCasosDeUso() {
        service.generar(1L);

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(ia).generarPlanificacionJson(anyString(), prompt.capture(), anyList());
        assertThat(prompt.getValue()).contains("(sin casos de uso)");
    }

    @Test
    void actualizarRechazaSiLaPlanificacionNoEstaEnPropuesta() {
        Planificacion plan = new Planificacion();
        plan.setId(4L);
        plan.setEstado("ACTIVA");
        plan.setProyecto(proyecto);
        when(planificaciones.findByProyectoId(1L)).thenReturn(Optional.of(plan));

        Integrante creador = integrante(2L, "ana@example.com");
        creador.setRol("CREADOR");
        when(integrantes.findByUsuarioIdAndProyectoId(2L, 1L)).thenReturn(Optional.of(creador));

        ResponseStatusException ex = catchThrowableOfType(() -> service.actualizar(1L,
                new PlanificacionUpdateRequest(List.of())), ResponseStatusException.class);
        assertThat(ex).isNotNull();
        assertThat(ex.getReason()).contains("PROPUESTA");
    }

    @Test
    void actualizarRechazaPlanificacionesConInconsistencias() {
        Planificacion plan = new Planificacion();
        plan.setId(4L);
        plan.setEstado("PROPUESTA");
        plan.setProyecto(proyecto);
        when(planificaciones.findByProyectoId(1L)).thenReturn(Optional.of(plan));

        Integrante creador = integranteActivo(2L, "ana@example.com", "CREADOR");
        Integrante segundo = integranteActivo(3L, "luis@example.com", "INTEGRANTE");
        when(integrantes.findByProyectoId(1L)).thenReturn(List.of(creador, segundo));
        when(integrantes.findByUsuarioIdAndProyectoId(2L, 1L)).thenReturn(Optional.of(creador));

        PlanificacionUpdateRequest request = new PlanificacionUpdateRequest(List.of(
                new PlanificacionUpdateRequest.EtapaUpdate(null, "MVP", 1, List.of(
                        new PlanificacionUpdateRequest.TareaUpdate(null, "t1", "Endpoint", null,
                                segundo.getId(), "MEDIA", 0, List.of())))));

        ResponseStatusException ex = catchThrowableOfType(() -> service.actualizar(1L, request),
                ResponseStatusException.class);
        assertThat(ex).isNotNull();
        assertThat(ex.getReason()).contains("duración positiva");
    }

    @Test
    void actualizarReiniciaConfirmacionesYGuardaLaPropuesta() {
        Planificacion plan = new Planificacion();
        plan.setId(4L);
        plan.setEstado("PROPUESTA");
        plan.setProyecto(proyecto);
        when(planificaciones.findByProyectoId(1L)).thenReturn(Optional.of(plan));
        when(confirmaciones.findByPlanificacionId(4L)).thenReturn(List.of());
        when(dependencias.findByPlanificacionId(4L)).thenReturn(List.of());

        Integrante creador = integranteActivo(2L, "ana@example.com", "CREADOR");
        Integrante segundo = integranteActivo(3L, "luis@example.com", "INTEGRANTE");
        when(integrantes.findByProyectoId(1L)).thenReturn(List.of(creador, segundo));
        when(integrantes.findByUsuarioIdAndProyectoId(2L, 1L)).thenReturn(Optional.of(creador));

        PlanificacionUpdateRequest request = new PlanificacionUpdateRequest(List.of(
                new PlanificacionUpdateRequest.EtapaUpdate(null, "MVP", 1, List.of(
                        new PlanificacionUpdateRequest.TareaUpdate(null, "t1", "Endpoint", "desc",
                                segundo.getId(), "ALTA", 2, List.of())))));

        PlanificacionResponse response = service.actualizar(1L, request);

        assertThat(response.estado()).isEqualTo("PROPUESTA");
        assertThat(response.etapas()).hasSize(1);
        verify(confirmaciones).findByPlanificacionId(4L);
        verify(confirmaciones).deleteAll(anyList());
        verify(dependencias).deleteAll(anyList());
    }

    private Integrante integranteActivo(Long integranteId, String email, String rol) {
        Integrante i = integrante(integranteId, email);
        i.setId(integranteId);
        i.setRol(rol);
        return i;
    }

    private Integrante integrante(Long id, String email) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail(email);
        Habilidad habilidad = new Habilidad();
        habilidad.setNombre("Backend");
        UsuarioHabilidad perfil = new UsuarioHabilidad();
        perfil.setHabilidad(habilidad);
        usuario.getHabilidades().add(perfil);
        Integrante integrante = new Integrante();
        integrante.setUsuario(usuario);
        integrante.setEstado("ACTIVO");
        integrante.setRol("INTEGRANTE");
        return integrante;
    }
}