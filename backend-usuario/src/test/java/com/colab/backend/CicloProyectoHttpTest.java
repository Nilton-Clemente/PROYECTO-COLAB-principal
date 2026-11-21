package com.colab.backend;

import com.colab.backend.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class CicloProyectoHttpTest {
    @Autowired private WebApplicationContext context;
    @Autowired private JdbcTemplate db;
    @Autowired private JwtService jwt;
    private final List<Long> proyectos = new ArrayList<>();
    private MockMvc mvc;
    private String tokenCreador;
    private String tokenIntegrante;
    private long creador;
    private long integrante;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        String correoCreador = UUID.randomUUID() + "@example.test";
        String correoIntegrante = UUID.randomUUID() + "@example.test";
        creador = usuario(correoCreador);
        integrante = usuario(correoIntegrante);
        tokenCreador = jwt.generarToken(correoCreador);
        tokenIntegrante = jwt.generarToken(correoIntegrante);
    }

    @AfterEach
    void limpiar() {
        proyectos.forEach(id -> db.update("delete from usuario.proyecto where id = ?", id));
        db.update("delete from usuario.usuario where id in (?, ?)", creador, integrante);
    }

    @Test
    void finalizaSoloAlCompletarLaUltimaTareaDeTodasLasEtapasYConservaElUltimoAvance() throws Exception {
        Escenario e = escenario("ACTIVA");
        long primera = tarea(e, "EN_PROGRESO");
        long ultima = tarea(e, "EN_PROGRESO");
        mvc.perform(estado(e, primera)).andExpect(status().isOk());
        mvc.perform(auth(get(e.url()), tokenCreador)).andExpect(jsonPath("$.estado").value("EN_DESARROLLO"));

        mvc.perform(avance(e, ultima)).andExpect(status().isCreated());
        mvc.perform(estado(e, ultima)).andExpect(status().isOk());
        mvc.perform(auth(get(e.url()), tokenCreador))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("FINALIZADO"))
                .andExpect(jsonPath("$.fechaFinalizacion").isString())
                .andExpect(jsonPath("$.puedeEliminar").value(true));
        mvc.perform(auth(get("/api/proyectos"), tokenIntegrante))
                .andExpect(jsonPath("$[0].estado").value("FINALIZADO"))
                .andExpect(jsonPath("$[0].puedeEliminar").value(false));
        mvc.perform(avance(e, ultima)).andExpect(status().isConflict());
        mvc.perform(auth(get(e.avances(ultima)), tokenIntegrante))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalMinutos").value(30));
    }

    @Test
    void cancelarConservaHistorialYPermiteEliminarSoloAlCreador() throws Exception {
        Escenario e = escenario("ACTIVA");
        Escenario otro = escenario(null);
        long predecesora = tarea(e, "COMPLETADA");
        long t = tarea(e, "EN_PROGRESO");
        db.update("insert into usuario.dependencia_tarea (tarea_id, tarea_predecesora_id) values (?, ?)", t, predecesora);
        mvc.perform(avance(e, t)).andExpect(status().isCreated());
        mvc.perform(auth(delete(e.url()), tokenCreador)).andExpect(status().isConflict());
        mvc.perform(auth(post(e.url() + "/cancelar"), tokenIntegrante)).andExpect(status().isForbidden());
        mvc.perform(cancelar(e)).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"))
                .andExpect(jsonPath("$.fechaCancelacion").isString())
                .andExpect(jsonPath("$.puedeCancelar").value(false))
                .andExpect(jsonPath("$.puedeEliminar").value(true));
        mvc.perform(cancelar(e)).andExpect(status().isConflict());
        mvc.perform(estado(e, t)).andExpect(status().isConflict());
        mvc.perform(avance(e, t)).andExpect(status().isConflict());
        mvc.perform(auth(get(e.avances(t)), tokenIntegrante)).andExpect(status().isOk());
        mvc.perform(auth(get(e.url() + "/planificacion"), tokenIntegrante))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CANCELADA"));
        mvc.perform(auth(delete(e.url()), tokenIntegrante)).andExpect(status().isForbidden());
        mvc.perform(auth(delete(e.url()), tokenCreador)).andExpect(status().isNoContent());
        mvc.perform(auth(get(e.url()), tokenIntegrante)).andExpect(status().isNotFound());
        assertThat(db.queryForObject("select count(*) from usuario.registro_avance where tarea_id = ?", Long.class, t)).isZero();
        assertThat(db.queryForObject("select count(*) from usuario.dependencia_tarea where tarea_id = ?", Long.class, t)).isZero();
        assertThat(db.queryForObject("select count(*) from usuario.planificacion where id = ?", Long.class, e.plan())).isZero();
        assertThat(db.queryForObject("select count(*) from usuario.usuario where id in (?, ?)", Long.class, creador, integrante)).isEqualTo(2);
        mvc.perform(auth(get(otro.url()), tokenCreador)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"BORRADOR", "PROPUESTA", "FINALIZADA"})
    void permiteEliminarSinCancelarEnEstadosPermitidos(String estadoPlan) throws Exception {
        Escenario e = escenario(estadoPlan);
        if (estadoPlan != null) {
            tarea(e, "FINALIZADA".equals(estadoPlan) ? "COMPLETADA" : "PENDIENTE");
        }
        mvc.perform(cancelar(e)).andExpect(status().isConflict());
        mvc.perform(auth(delete(e.url()), tokenCreador)).andExpect(status().isNoContent());
    }

    @Test
    void noCierraPlanActivoVacioNiPermiteEjecutarUnaPropuesta() throws Exception {
        Escenario vacio = escenario("ACTIVA");
        mvc.perform(auth(get(vacio.url()), tokenCreador))
                .andExpect(jsonPath("$.estado").value("EN_DESARROLLO"));
        Escenario propuesta = escenario("PROPUESTA");
        long t = tarea(propuesta, "EN_PROGRESO");
        mvc.perform(estado(propuesta, t)).andExpect(status().isConflict());
        mvc.perform(avance(propuesta, t)).andExpect(status().isConflict());
    }

    @Test
    void cancelarYCompletarUltimaTareaNoPuedenGanarAmbas() throws Exception {
        Escenario e = escenario("ACTIVA");
        long t = tarea(e, "EN_PROGRESO");
        var resultados = concurrentes(() -> codigo(cancelar(e)), () -> codigo(estado(e, t)));
        assertThat(resultados).containsExactlyInAnyOrder(200, 409);
        String terminal = resultados.getFirst() == 200 ? "CANCELADO" : "FINALIZADO";
        mvc.perform(auth(get(e.url()), tokenCreador)).andExpect(jsonPath("$.estado").value(terminal));
        assertThat(db.queryForObject("select estado from usuario.tarea where id = ?", String.class, t))
                .isEqualTo(resultados.getFirst() == 200 ? "EN_PROGRESO" : "COMPLETADA");
    }

    @Test
    void completarDosTareasConcurrentementeDejaElProyectoFinalizado() throws Exception {
        Escenario e = escenario("ACTIVA");
        long a = tarea(e, "EN_PROGRESO");
        long b = tarea(e, "EN_PROGRESO");
        assertThat(concurrentes(() -> codigo(estado(e, a)), () -> codigo(estado(e, b))))
                .containsExactly(200, 200);
        mvc.perform(auth(get(e.url()), tokenCreador)).andExpect(jsonPath("$.estado").value("FINALIZADO"));
    }

    private int codigo(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request).andReturn().getResponse().getStatus();
    }

    private List<Integer> concurrentes(Callable<Integer> primero, Callable<Integer> segundo) throws Exception {
        var inicio = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var a = pool.submit(() -> { inicio.await(); return primero.call(); });
            var b = pool.submit(() -> { inicio.await(); return segundo.call(); });
            inicio.countDown();
            return List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }

    private MockHttpServletRequestBuilder cancelar(Escenario e) {
        return auth(post(e.url() + "/cancelar"), tokenCreador);
    }

    private MockHttpServletRequestBuilder estado(Escenario e, long tarea) {
        return auth(put(e.url() + "/tareas/" + tarea + "/estado"), tokenIntegrante)
                .contentType("application/json").content("{\"nuevoEstado\":\"COMPLETADA\"}");
    }

    private MockHttpServletRequestBuilder avance(Escenario e, long tarea) {
        return auth(post(e.avances(tarea)), tokenIntegrante)
                .contentType("application/json").content("{\"minutos\":30,\"nota\":\"Entrega\"}");
    }

    private long usuario(String correo) {
        return id("insert into usuario.usuario (email, nombre, contrasena_hash) values (?, 'Prueba EP-09', 'no-autenticar') returning id", correo);
    }

    private Escenario escenario(String estadoPlan) {
        long proyecto = id("insert into usuario.proyecto (nombre) values ('Proyecto EP-09') returning id");
        proyectos.add(proyecto);
        String sql = "insert into usuario.integrante (proyecto_id, usuario_id, rol, estado) values (?, ?, ?, 'ACTIVO') returning id";
        id(sql, proyecto, creador, "CREADOR");
        long responsable = id(sql, proyecto, integrante, "INTEGRANTE");
        Long plan = estadoPlan == null ? null : id(
                "insert into usuario.planificacion (proyecto_id, estado) values (?, ?) returning id", proyecto, estadoPlan);
        return new Escenario(proyecto, plan, responsable);
    }

    private long tarea(Escenario e, String estado) {
        long etapa = id("insert into usuario.etapa (planificacion_id, nombre) values (?, 'Desarrollo') returning id", e.plan());
        return id("""
                insert into usuario.tarea (etapa_id, responsable_id, nombre, prioridad, estado, duracion_dias)
                values (?, ?, 'Tarea de prueba', 'MEDIA', ?, 1) returning id
                """, etapa, e.responsable(), estado);
    }

    private long id(String sql, Object... params) {
        return db.queryForObject(sql, Long.class, params);
    }

    private record Escenario(long proyecto, Long plan, long responsable) {
        String url() { return "/api/proyectos/" + proyecto; }
        String avances(long tarea) { return url() + "/tareas/" + tarea + "/avances"; }
    }
}