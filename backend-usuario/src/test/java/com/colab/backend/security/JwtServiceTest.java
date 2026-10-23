package com.colab.backend.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "clave-de-prueba-para-jwt-de-mas-de-32-caracteres-1234567890";
    private static final long EXPIRACION_MS = 3600_000L;

    private final JwtService jwtService = new JwtService(SECRET, EXPIRACION_MS);

    @Test
    void generaTokenQueContieneElEmailComoSubject() {
        String token = jwtService.generarToken("usuario@test.com");

        assertThat(token).isNotBlank();
        assertThat(jwtService.extraerEmail(token)).isEqualTo("usuario@test.com");
    }

    @Test
    void tokenRecienGeneradoEsValido() {
        String token = jwtService.generarToken("usuario@test.com");
        assertThat(jwtService.esValido(token)).isTrue();
    }

    @Test
    void tokenInvalidoNoEsValido() {
        assertThat(jwtService.esValido("token-invalido")).isFalse();
    }
}
