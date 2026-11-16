package com.colab.backend.controller;

import com.colab.backend.dto.HabilidadDto;
import com.colab.backend.dto.HabilidadPerfilDto;
import com.colab.backend.dto.PerfilResponse;
import com.colab.backend.service.PerfilService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PerfilControllerTest {
    private final PerfilService servicio = mock(PerfilService.class);
    private MockMvc mvc;

    @BeforeEach
    void preparar() { mvc = MockMvcBuilders.standaloneSetup(new PerfilController(servicio)).build(); }

    @Test
    void perfilIncluyeNivelYCatalogoNoLoIncluye() throws Exception {
        when(servicio.obtenerPerfil()).thenReturn(new PerfilResponse("ana@example.com", "Ana",
                List.of(new HabilidadPerfilDto(2L, "Java", "Backend", "AVANZADO"))));
        when(servicio.listarCatalogo()).thenReturn(List.of(new HabilidadDto(2L, "Java", "Backend")));
        mvc.perform(get("/api/usuario/perfil")).andExpect(status().isOk())
                .andExpect(jsonPath("$.habilidades[0].nivel").value("AVANZADO"));
        mvc.perform(get("/api/habilidades")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nivel").doesNotExist());
    }

    @Test
    void recibeNivelYOpcionAntiguaSinNivel() throws Exception {
        mvc.perform(put("/api/usuario/habilidades/2").param("nivel", "BASICO"))
                .andExpect(status().isNoContent());
        mvc.perform(put("/api/usuario/habilidades/2")).andExpect(status().isNoContent());
        verify(servicio).agregarHabilidad(2L, "BASICO");
        verify(servicio).agregarHabilidad(2L, null);
    }

    @Test
    void nivelInvalidoRetorna400() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nivel inválido"))
                .when(servicio).agregarHabilidad(2L, "EXPERTO");
        mvc.perform(put("/api/usuario/habilidades/2").param("nivel", "EXPERTO"))
                .andExpect(status().isBadRequest());
    }
}