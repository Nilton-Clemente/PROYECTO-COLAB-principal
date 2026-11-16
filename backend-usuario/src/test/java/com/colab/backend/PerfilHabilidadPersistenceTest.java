package com.colab.backend;

import com.colab.backend.domain.Habilidad;
import com.colab.backend.domain.Usuario;
import com.colab.backend.service.PerfilService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PerfilHabilidadPersistenceTest {
    @Autowired private EntityManager entityManager;
    @Autowired private PerfilService service;

    @AfterEach
    void limpiar() { SecurityContextHolder.clearContext(); }

    @Test
    void persisteActualizaYSeparaNivelesPorUsuario() {
        String sufijo = UUID.randomUUID().toString();
        Usuario ana = usuario("ana-" + sufijo + "@example.com");
        Usuario luis = usuario("luis-" + sufijo + "@example.com");
        Habilidad java = new Habilidad();
        java.setNombre("Java-" + sufijo);
        java.setArea("Backend");
        entityManager.persist(java);
        entityManager.flush();
        Long habilidadId = java.getId();

        autenticar(ana.getEmail());
        service.agregarHabilidad(habilidadId, "BASICO");
        service.agregarHabilidad(habilidadId, "AVANZADO");
        service.agregarHabilidad(habilidadId, null);
        autenticar(luis.getEmail());
        service.agregarHabilidad(habilidadId, null);
        entityManager.flush();
        entityManager.clear();

        assertThat(service.obtenerPerfil().habilidades().getFirst().nivel()).isNull();
        autenticar(ana.getEmail());
        assertThat(service.obtenerPerfil().habilidades().getFirst().nivel()).isEqualTo("AVANZADO");
        // Cada petición HTTP usa un contexto de persistencia independiente.
        entityManager.clear();
        service.eliminarHabilidad(habilidadId);
        entityManager.flush();
        entityManager.clear();
        assertThat(service.obtenerPerfil().habilidades()).isEmpty();
        autenticar(luis.getEmail());
        assertThat(service.obtenerPerfil().habilidades()).hasSize(1);
    }

    private Usuario usuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setNombre("Usuario de prueba");
        usuario.setContrasenaHash("hash");
        entityManager.persist(usuario);
        return usuario;
    }

    private void autenticar(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(email, null, List.of()));
    }
}