package com.colab.backend.controller;

import com.colab.backend.domain.Tecnologia;
import com.colab.backend.repository.TecnologiaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TecnologiaControllerTest {
    @Test
    void devuelveIconoOpcionalEnCatalogo() throws Exception {
        TecnologiaRepository repository = mock(TecnologiaRepository.class);
        Tecnologia java = new Tecnologia();
        java.setId(1L);
        java.setNombre("Java");
        java.setIcono("java");
        Tecnologia otra = new Tecnologia();
        otra.setId(2L);
        otra.setNombre("Otra");
        when(repository.findAll()).thenReturn(List.of(java, otra));

        MockMvcBuilders.standaloneSetup(new TecnologiaController(repository)).build()
                .perform(get("/api/tecnologias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].icono").value("java"))
                .andExpect(jsonPath("$[1].icono").value(org.hamcrest.Matchers.nullValue()));
    }
}