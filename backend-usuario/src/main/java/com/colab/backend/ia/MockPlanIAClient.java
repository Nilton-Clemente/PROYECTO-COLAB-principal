package com.colab.backend.ia;

import java.util.List;

/**
 * Implementación de pruebas de {@link PlanIAClient}: devuelve un JSON de ejemplo
 * sin depender de la red de TECSUP.
 *
 * <p>Para que la propuesta sea válida, asigna como responsables los correos reales
 * del equipo (recibidos por parámetro) en lugar de valores fijos.</p>
 */
public class MockPlanIAClient implements PlanIAClient {

    private static final String PLANTILLA = """
            {
              "etapas": [
                {
                  "nombre": "Análisis",
                  "orden": 1,
                  "tareas": [
                    {
                      "id": "t1",
                      "nombre": "Definir requerimientos",
                      "descripcion": "Redactar los RF del proyecto.",
                      "responsable": "%s",
                      "habilidadesRequeridas": ["Análisis", "Requerimientos"],
                      "prioridad": "ALTA",
                      "duracionDias": 2,
                      "dependencias": []
                    },
                    {
                      "id": "t2",
                      "nombre": "Validar requerimientos",
                      "descripcion": "Revisar los RF con el equipo.",
                      "responsable": "%s",
                      "habilidadesRequeridas": ["Análisis"],
                      "prioridad": "ALTA",
                      "duracionDias": 1,
                      "dependencias": ["t1"]
                    }
                  ]
                },
                {
                  "nombre": "Desarrollo",
                  "orden": 2,
                  "tareas": [
                    {
                      "id": "t3",
                      "nombre": "Implementar la solución",
                      "descripcion": "Desarrollar la solución de software.",
                      "responsable": "%s",
                      "habilidadesRequeridas": ["Backend", "Frontend"],
                      "prioridad": "MEDIA",
                      "duracionDias": 3,
                      "dependencias": ["t2"]
                    }
                  ]
                }
              ]
            }
            """;

    @Override
    public String generarPlanificacionJson(String promptSistema, String promptUsuario, List<String> correosIntegrantes) {
        String r1 = correo(correosIntegrantes, 0);
        String r2 = correo(correosIntegrantes, 1);
        String r3 = correo(correosIntegrantes, 2, r1);
        return PLANTILLA.formatted(r1, r2, r3);
    }

    private String correo(List<String> correos, int indice) {
        return correo(correos, indice, "integrante@ejemplo.com");
    }

    private String correo(List<String> correos, int indice, String fallback) {
        if (correos == null || correos.size() <= indice) {
            return fallback;
        }
        return correos.get(indice);
    }
}

