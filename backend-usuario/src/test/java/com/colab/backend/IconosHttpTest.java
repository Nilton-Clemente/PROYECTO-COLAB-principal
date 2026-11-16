package com.colab.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class IconosHttpTest {
    @Autowired private WebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void preparar() { mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }

    @Test
    void iconosSvgDisponiblesSinAutenticacion() throws Exception {
        for (String icono : new String[]{"java", "kotlin", "react", "spring", "postgresql"}) {
            mvc.perform(get("/iconos/devicon/" + icono + ".svg"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("image/svg+xml"))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("<svg")));
        }
    }

    @Test
    void iconoInexistenteYArchivoFueraDeRutaNoSeExponen() throws Exception {
        mvc.perform(get("/iconos/devicon/no-existe.svg")).andExpect(status().isNotFound());
        mvc.perform(get("/iconos/devicon/LICENSE.txt")).andExpect(status().is4xxClientError());
        mvc.perform(get("/iconos/devicon/%2e%2e/application.yml"))
                .andExpect(status().is4xxClientError());
    }
}