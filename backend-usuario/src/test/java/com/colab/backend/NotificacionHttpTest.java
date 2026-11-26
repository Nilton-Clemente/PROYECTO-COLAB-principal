package com.colab.backend;

import com.colab.backend.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Prueba HTTP real con PostgreSQL: solo crea y limpia cuentas propias de prueba. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificacionHttpTest {
    @LocalServerPort private int puerto;
    @Autowired private JdbcTemplate db;
    @Autowired private JwtService jwt;
    private final HttpClient http = HttpClient.newHttpClient();
    private final List<Long> usuarios = new ArrayList<>();
    private long usuario;
    private long otro;
    private String token;

    @BeforeEach
    void preparar() {
        String correo = UUID.randomUUID() + "@example.test";
        usuario = crearUsuario(correo);
        otro = crearUsuario(UUID.randomUUID() + "@example.test");
        token = jwt.generarToken(correo);
    }

    @AfterEach
    void limpiar() {
        usuarios.forEach(id -> db.update("delete from usuario.usuario where id = ?", id));
    }

    @Test
    void borraLeidasAntiguasYConProyectoSinTocarNoLeidasNiOtrasCuentas() throws Exception {
        long antigua = aviso(usuario, true);
        long conProyecto = aviso(usuario, true);
        db.update("update usuario.notificacion set proyecto_id = 7, proyecto_nombre = 'Prueba aislada' where id = ?", conProyecto);
        long sinLeer = aviso(usuario, false);
        long ajenaLeida = aviso(otro, true);
        long ajenaSinLeer = aviso(otro, false);

        var respuesta = borrar(token);
        assertThat(respuesta.statusCode()).isEqualTo(204);
        assertThat(respuesta.body()).isEmpty();
        assertThat(existe(antigua)).isFalse();
        assertThat(existe(conProyecto)).isFalse();
        assertThat(existe(sinLeer)).isTrue();
        assertThat(existe(ajenaLeida)).isTrue();
        assertThat(existe(ajenaSinLeer)).isTrue();
        assertThat(borrar(token).statusCode()).isEqualTo(204);

        var bandeja = http.send(HttpRequest.newBuilder(url("/api/notificaciones"))
                .header("Authorization", "Bearer " + token).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(bandeja.statusCode()).isEqualTo(200);
        assertThat(bandeja.body()).contains("\"id\":" + sinLeer).contains("\"leida\":false");
        assertThat(db.queryForObject("select count(*) from usuario.notificacion where usuario_id = ?", Long.class, usuario))
                .isEqualTo(1L);
        assertThat(db.queryForObject("select count(*) from usuario.flyway_schema_history where version = '15' and success", Long.class))
                .isEqualTo(1L);
    }

    @Test
    void peticionSinSesionNoBorraAvisos() throws Exception {
        long leida = aviso(usuario, true);
        var respuesta = borrar(null);
        assertThat(respuesta.statusCode()).isIn(401, 403);
        assertThat(existe(leida)).isTrue();
    }

    @Test
    void listaContextoCuentaYMarcaSoloAvisosPropios() throws Exception {
        long propia = aviso(usuario, false);
        long historica = aviso(usuario, true);
        long ajena = aviso(otro, false);
        db.update("update usuario.notificacion set proyecto_id = 7, proyecto_nombre = 'Proyecto HTTP aislado' where id = ?", propia);
        db.update("update usuario.notificacion set mensaje = 'Aviso exclusivo de otra cuenta' where id = ?", ajena);

        var bandeja = consultar("/api/notificaciones");
        assertThat(bandeja.statusCode()).isEqualTo(200);
        assertThat(bandeja.body()).contains("\"id\":" + propia + ",", "\"id\":" + historica + ",",
                "\"proyectoId\":7", "\"proyectoNombre\":\"Proyecto HTTP aislado\"", "\"proyectoCodigo\":\"TEC-7\"");
        assertThat(bandeja.body()).doesNotContain("Aviso exclusivo de otra cuenta");
        assertThat(consultar("/api/notificaciones/no-leidas").body()).contains("\"noLeidas\":1");

        var lectura = marcar(propia);
        assertThat(lectura.statusCode()).isEqualTo(200);
        assertThat(lectura.body()).contains("\"leida\":true", "\"proyectoCodigo\":\"TEC-7\"");
        assertThat(consultar("/api/notificaciones/no-leidas").body()).contains("\"noLeidas\":0");
        assertThat(marcar(ajena).statusCode()).isEqualTo(403);
        assertThat(db.queryForObject("select leida from usuario.notificacion where id = ?", Boolean.class, ajena)).isFalse();
    }

    private HttpResponse<String> consultar(String ruta) throws Exception {
        return http.send(HttpRequest.newBuilder(url(ruta)).header("Authorization", "Bearer " + token)
                .GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> marcar(long id) throws Exception {
        return http.send(HttpRequest.newBuilder(url("/api/notificaciones/" + id + "/leida"))
                .header("Authorization", "Bearer " + token).PUT(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private long crearUsuario(String correo) {
        Long id = db.queryForObject("insert into usuario.usuario (email, nombre, contrasena_hash) values (?, 'Prueba notificaciones HTTP', 'no-autenticar') returning id", Long.class, correo);
        usuarios.add(id);
        return id;
    }

    private long aviso(long destinatario, boolean leida) {
        return db.queryForObject("insert into usuario.notificacion (usuario_id, tipo_evento, mensaje, leida) values (?, 'PLANIFICACION', 'Prueba aislada de borrado', ?) returning id", Long.class, destinatario, leida);
    }

    private boolean existe(long id) {
        return Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from usuario.notificacion where id = ?)", Boolean.class, id));
    }

    private URI url(String ruta) { return URI.create("http://127.0.0.1:" + puerto + ruta); }

    private HttpResponse<String> borrar(String credencial) throws Exception {
        var peticion = HttpRequest.newBuilder(url("/api/notificaciones/leidas")).DELETE();
        if (credencial != null) peticion.header("Authorization", "Bearer " + credencial);
        return http.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
    }
}