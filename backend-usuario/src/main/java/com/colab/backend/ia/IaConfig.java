package com.colab.backend.ia;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selecciona la implementación de {@link PlanIAClient} según la propiedad
 * {@code colab.ia.provider}:
 * <ul>
 *   <li>{@code mock} (por defecto): {@link MockPlanIAClient}, sin red ni token.</li>
 *   <li>{@code real}: {@link OpenWebUIPlanIAClient}, contra el servicio de TECSUP.</li>
 * </ul>
 */
@Configuration
public class IaConfig {

    @Bean
    public PlanIAClient planIAClient(
            @Value("${colab.ia.provider:mock}") String provider,
            @Value("${colab.ia.openwebui-url:http://192.168.17.11:3000}") String openwebuiUrl,
            @Value("${colab.ia.api-key:}") String apiKey,
            @Value("${colab.ia.model:Qwen/Qwen3.6-35B-A3B-FP8}") String model) {
        if ("real".equalsIgnoreCase(provider)) {
            return new OpenWebUIPlanIAClient(openwebuiUrl, apiKey, model);
        }
        return new MockPlanIAClient();
    }
}
