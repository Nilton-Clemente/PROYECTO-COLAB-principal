package com.colab.backend;

import com.colab.backend.domain.CasoUso;
import com.colab.backend.domain.Proyecto;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CasoUsoPersistenceTests {
    @Autowired
    private EntityManager entityManager;

    @Test
    void persisteYRecuperaLaEspecificacionCompletaConTextoLargo() {
        Proyecto proyecto = new Proyecto();
        proyecto.setNombre("Proyecto de prueba de casos de uso");
        CasoUso caso = new CasoUso();
        caso.setProyecto(proyecto);
        caso.setNombre("Invitar integrante");
        caso.setActores("Creador y destinatario");
        caso.setPrecondiciones("Destinatario registrado");
        caso.setDisparador("Seleccionar Invitar");
        String flujo = "Paso detallado de la invitación.\n".repeat(30);
        caso.setFlujoPrincipal(flujo);
        caso.setFlujosAlternativos("Rechazar invitación");
        caso.setExcepciones("Invitación duplicada");
        caso.setPostcondiciones("Integrante activo si acepta");
        caso.setReglasDeNegocio("Solo el creador invita");
        proyecto.getCasosDeUso().add(caso);

        entityManager.persist(proyecto);
        entityManager.flush();
        Long id = caso.getId();
        entityManager.clear();

        CasoUso recuperado = entityManager.find(CasoUso.class, id);
        assertThat(recuperado.getActores()).isEqualTo("Creador y destinatario");
        assertThat(recuperado.getPrecondiciones()).isEqualTo("Destinatario registrado");
        assertThat(recuperado.getDisparador()).isEqualTo("Seleccionar Invitar");
        assertThat(recuperado.getFlujoPrincipal()).isEqualTo(flujo);
        assertThat(recuperado.getFlujosAlternativos()).isEqualTo("Rechazar invitación");
        assertThat(recuperado.getExcepciones()).isEqualTo("Invitación duplicada");
        assertThat(recuperado.getPostcondiciones()).isEqualTo("Integrante activo si acepta");
        assertThat(recuperado.getReglasDeNegocio()).isEqualTo("Solo el creador invita");
    }

    @Test
    void persisteCasoBasicoSinDetallesOpcionales() {
        Proyecto proyecto = new Proyecto();
        proyecto.setNombre("Proyecto básico de prueba");
        CasoUso caso = new CasoUso();
        caso.setNombre("Registrar usuario");
        caso.setProyecto(proyecto);
        proyecto.getCasosDeUso().add(caso);

        entityManager.persist(proyecto);
        entityManager.flush();
        Long id = caso.getId();
        entityManager.clear();

        CasoUso recuperado = entityManager.find(CasoUso.class, id);
        assertThat(recuperado.getNombre()).isEqualTo("Registrar usuario");
        assertThat(recuperado.getPrecondiciones()).isNull();
        assertThat(recuperado.getFlujoPrincipal()).isNull();
    }
}