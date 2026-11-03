package com.colab.backend.ia;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Implementación real de {@link PlanIAClient} que llama al servicio OpenWebUI
 * de TECSUP ("La BestIA"), compatible con la API de OpenAI
 * (POST /api/chat/completions).
 *
 * <p>Requiere estar dentro de la red del instituto y disponer de un TOKEN
 * (ver docs/04-analisis/08-integracion-ia.md).</p>
 */
public class OpenWebUIPlanIAClient implements PlanIAClient {

    private final String urlBase;
    private final String apiKey;
    private final String model;
    private final RestClient restClient;

    public OpenWebUIPlanIAClient(String urlBase, String apiKey, String model) {
        this.urlBase = urlBase == null ? "" : urlBase.replaceAll("/+$", "");
        this.apiKey = apiKey;
        this.model = model;
        // Forzar HTTP/1.1: el HttpClient del JDK por defecto intenta negociar HTTP/2 (h2c)
        // con cabeceras de upgrade, y el servidor de OpenWebUI (uvicorn) rechaza esa
        // petición con "Invalid HTTP request received".
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.restClient = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public String generarPlanificacionJson(String promptSistema, String promptUsuario, List<String> correosIntegrantes) {
        // La implementación real ignora "correosIntegrantes": el prompt ya incluye el equipo.
        Map<String, Object> cuerpo = Map.of(
                "model", model,
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content", promptSistema),
                        Map.of("role", "user", "content", promptUsuario)
                )
        );

        Map<String, Object> respuesta;
        try {
            respuesta = restClient.post()
                    .uri(urlBase + "/api/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "Error al llamar a la IA (HTTP " + e.getStatusCode().value()
                            + "): " + e.getResponseBodyAsString(), e);
        }

        return extraerContenido(respuesta);
    }

    /**
     * Extrae el texto generado desde choices[0].message.content
     * (formato compatible con OpenAI).
     */
    private String extraerContenido(Map<String, Object> respuesta) {
        if (respuesta == null) {
            throw new IllegalStateException("La IA devolvió una respuesta vacía.");
        }

        Object choices = respuesta.get("choices");
        if (!(choices instanceof List<?> lista) || lista.isEmpty()) {
            throw new IllegalStateException("La IA no devolvió 'choices' en la respuesta.");
        }

        Object primer = lista.get(0);
        if (!(primer instanceof Map<?, ?> choice)) {
            throw new IllegalStateException("Formato inesperado en choices[0].");
        }

        Object message = choice.get("message");
        if (!(message instanceof Map<?, ?> msg)) {
            throw new IllegalStateException("La respuesta no contiene 'message'.");
        }

        Object content = msg.get("content");
        if (content == null) {
            throw new IllegalStateException("La respuesta no contiene 'content'.");
        }
        return content.toString();
    }
}
