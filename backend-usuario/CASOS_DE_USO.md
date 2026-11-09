# Casos de uso detallados

Los endpoints de creación (`POST /api/proyectos`), edición (`PUT /api/proyectos/{id}`)
y consulta de proyectos admiten una especificación detallada en `casosDeUso`.
Las operaciones requieren el token JWT del usuario.

Solo `nombre` es obligatorio dentro de cada caso de uso. Los demás campos son
textos opcionales; se pueden omitir o enviar como `null`. No se solicita ni se
devuelve `codigo` en RF, RNF o casos de uso. La categoría del RNF sigue siendo opcional.
Los pasos se pueden escribir con saltos de línea (`\n` en JSON); no son arrays.

## Ejemplo de creación

```json
{
  "nombre": "ColabTask",
  "casosDeUso": [
    {
      "nombre": "Invitar integrante",
      "descripcion": "Permite invitar a un usuario registrado al equipo de un proyecto.",
      "actores": "Creador del proyecto y usuario destinatario.",
      "precondiciones": "El creador ha iniciado sesión. El destinatario está registrado y no pertenece al equipo.",
      "disparador": "El creador selecciona Invitar integrante.",
      "flujoPrincipal": "1. El creador introduce el correo.\n2. El sistema valida la invitación.\n3. El sistema registra la invitación pendiente.\n4. El destinatario recibe una notificación.\n5. El destinatario acepta.\n6. El sistema lo incorpora al equipo.",
      "flujosAlternativos": "El destinatario rechaza la invitación y no se incorpora al equipo.",
      "excepciones": "Si el destinatario no existe, ya pertenece al equipo o tiene una invitación pendiente, el sistema rechaza la operación.",
      "postcondiciones": "Si acepta, el destinatario queda como integrante activo. Si rechaza, no se incorpora.",
      "reglasDeNegocio": "Solo el creador puede invitar. El destinatario se incorpora únicamente después de aceptar."
    }
  ]
}
```

Este ejemplo demuestra el formato de creación; para generar una planificación
siguen siendo necesarios el objetivo general, fecha de entrega, al menos un RF,
al menos una tecnología y dos integrantes activos con habilidades (RN-18).
Los casos de uso no se convierten en un requisito obligatorio para planificar.

## Persistencia y contexto de IA

- Flyway aplica `V10__caso_uso_detallado.sql` al iniciar el backend, agregando
  ocho columnas `TEXT` opcionales a `usuario.caso_uso`. No se modifican las
  migraciones anteriores ni se eliminan los casos existentes.
- Los detalles se guardan y se devuelven al crear, editar, consultar y listar proyectos.
- Los códigos se retiraron de los DTO, entidades y contexto enviado a la IA.
  Las columnas históricas `codigo` permanecen sin utilizar en la base de datos
  para conservar los datos existentes; no se requiere una migración para retirarlos de la API.
- Al generar la planificación se incluyen los detalles no vacíos en el prompt
  enviado a `PlanIAClient`, incluido el proveedor real OpenWebUI.
- El prompt indica que se usen para permisos, validaciones, dependencias y pruebas,
  sin inventar reglas faltantes. Las dudas deben convertirse en tareas de aclaración.
- En modo `mock`, el contexto también se construye, pero la respuesta sigue siendo
  una plantilla fija; ese proveedor no interpreta los detalles.
- Los formularios web/móvil deben enviar los nuevos campos para aprovecharlos.
  Los controles para editar la especificación detallada no se han añadido a las interfaces.
- La eliminación de códigos se limita al backend. La web y la app móvil no se
  modifican y pueden conservar referencias al campo anterior.

## Edición

`PUT /api/proyectos/{id}` reemplaza la lista completa de casos de uso, como antes.
Para conservar los detalles al editar, el cliente debe reenviarlos; si los omite,
los detalles del caso reemplazado quedan en `null`.