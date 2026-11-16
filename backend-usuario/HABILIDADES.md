# Niveles de habilidades del usuario

Las habilidades pertenecen al catálogo general; el nivel es una autoevaluación
asociada a cada usuario y no modifica el catálogo ni se usa en la planificación con IA.

Con el JWT del usuario, `PUT /api/usuario/habilidades/{habilidadId}?nivel=AVANZADO`
agrega la habilidad o actualiza su nivel. Los valores aceptados son `BASICO`,
`INTERMEDIO` y `AVANZADO` (mayúsculas y sin tilde). Cualquier otro valor,
incluido uno vacío, devuelve HTTP 400. La operación correcta devuelve HTTP 204.

Por compatibilidad, se admite omitir `nivel`: una relación nueva queda sin nivel
y una existente conserva el que tenía. Los clientes futuros deberán solicitar
la selección antes de añadir la habilidad. El backend mantiene temporalmente
opcional este parámetro para no interrumpir a los clientes actuales.

`GET /api/usuario/perfil` devuelve `nivel` en cada habilidad del usuario; puede
ser `null` para registros anteriores o solicitudes sin nivel. Los valores de
texto libre guardados históricamente se devuelven sin alterarlos. El catálogo
`GET /api/habilidades` conserva sus campos `id`, `nombre` y `area`, sin nivel.
La columna `usuario.usuario_habilidad.nivel` ya existe; no se aplica migración
ni se asigna un valor predeterminado a datos históricos.