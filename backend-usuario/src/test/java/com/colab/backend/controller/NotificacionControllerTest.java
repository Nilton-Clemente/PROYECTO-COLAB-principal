package com.colab.backend.controller;

import com.colab.backend.service.NotificacionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NotificacionControllerTest {
    @Test
    void eliminarLeidasDevuelve204SinCuerpo() throws Exception {
        var service = mock(NotificacionService.class);
        MockMvcBuilders.standaloneSetup(new NotificacionController(service)).build()
                .perform(delete("/api/notificaciones/leidas"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(service).eliminarLeidas();
        verifyNoMoreInteractions(service);
    }

    @Test
    void eliminarLeidasNoSimulaExitoSiNoHaySesion() throws Exception {
        var service = mock(NotificacionService.class);
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado"))
                .when(service).eliminarLeidas();
        MockMvcBuilders.standaloneSetup(new NotificacionController(service)).build()
                .perform(delete("/api/notificaciones/leidas"))
                .andExpect(status().isUnauthorized());
    }
}