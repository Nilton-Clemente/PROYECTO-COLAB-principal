# EP-09 — Ciclo de vida del proyecto

Los endpoints requieren `Authorization: Bearer <token>`. La planificación es la fuente de verdad; no existe un segundo estado que sincronizar en la tabla `proyecto`.

## Estados y acciones del creador

| `estado` de proyecto | `estado` de planificación | Cancelar | Eliminar |
| --- | --- | --- | --- |
| `SIN_PLANIFICACION` | No existe | No | Sí |
| `PLANIFICACION_PROPUESTA` | `PROPUESTA` o `BORRADOR` | No | Sí |
| `EN_DESARROLLO` | `ACTIVA` | Sí | No |
| `FINALIZADO` | `FINALIZADA` | No | Sí |
| `CANCELADO` | `CANCELADA` | No | Sí |

Al completar la última tarea de una planificación activa, el backend guarda la fecha y finaliza el proyecto en la misma transacción. Una planificación sin tareas no finaliza. La migración V14 también finaliza planificaciones activas históricas que ya tenían todas sus tareas completadas; la fecha queda en `null` porque no se conoce el momento real. Si el cliente registra tiempo y completa la última tarea con dos solicitudes, debe confirmar primero el registro del tiempo.

## API para web y móvil

`GET /api/proyectos` y `GET /api/proyectos/{id}` conservan los campos anteriores y añaden los siguientes (ejemplo para el creador después de cancelar):

```json
{
  "estado": "CANCELADO",
  "fechaFinalizacion": null,
  "fechaCancelacion": "2026-11-20T14:30:00",
  "puedeCancelar": false,
  "puedeEliminar": true
}
```

Las fechas son ISO 8601 locales del servidor, sin desplazamiento horario. Para quien no sea creador ambos permisos son `false`. Después de completar una tarea, vuelve a consultar el proyecto para actualizar su etiqueta y permisos; la respuesta del cambio de estado sigue siendo el detalle de la tarea.

- **`POST /api/proyectos/{id}/cancelar`**, sin cuerpo: `200 OK` con el proyecto actualizado. Devuelve `403` si el usuario no es el creador, `404` si no existe y `409` si no está en desarrollo.
- **`DELETE /api/proyectos/{id}`**: `204 No Content` si se elimina. Devuelve `403` si el usuario no es el creador, `404` si no existe y `409` si sigue en desarrollo. Es definitivo: el proyecto desaparece para el equipo y se borran los datos propios del proyecto, no las cuentas ni los catálogos compartidos. La interfaz debe solicitar confirmación antes de llamar al endpoint.
- **Finalizado o cancelado:** se conservan las consultas a la planificación, las tareas, las métricas y los avances. Registrar tiempo o cambiar el estado de una tarea requiere planificación activa y devuelve `409` si no lo está. No hay reapertura en esta versión.