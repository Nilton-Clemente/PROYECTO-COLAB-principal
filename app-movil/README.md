# App móvil (Android)

Aplicación móvil para los estudiantes.

- **Responsable:** Persona 2
- **Tecnología:** Android, Kotlin, Jetpack Compose.

## Puesta en marcha

1. Crear el proyecto con **Android Studio** apuntando a esta carpeta (Kotlin + Jetpack Compose).
2. Consumir la API de `backend-usuario` mediante el contrato OpenAPI.

## Responsabilidades principales

Pantallas de usuario: autenticación, proyectos, equipo, generación de planificación, "Mis tareas" y notificaciones.

Ver acuerdos en el [README raíz](../README.md).

## Iconos de habilidades

El perfil carga los SVG de habilidades desde `backend-usuario`, usando la
misma `base.url` configurada en `local.properties` (por defecto
`http://10.0.2.2:8081/` para el emulador). En un teléfono físico debe configurarse
una dirección del backend alcanzable desde el dispositivo. La inicial de la
habilidad se muestra si no tiene icono, mientras carga o si falla la descarga.
La app usa Coil con soporte SVG y caché; un icono nunca descargado requiere
conexión. La prueba automática de error funciona sin el backend; para comprobar
la carga real, iniciar `backend-usuario` y asignar un icono desde administración.

## Eliminación de proyectos

Desde **Detalle de proyecto → Más acciones → Eliminar proyecto**, se muestra una
confirmación con el nombre del proyecto y la advertencia de que también se borran
sus tareas y avances. Basta pulsar **Eliminar**; ya no se pide escribir el nombre.
**Cancelar** cierra el diálogo sin enviar ninguna petición. Se conservan los
permisos y estados admitidos por el backend, el bloqueo de operaciones simultáneas
y la espera de confirmación del servidor antes de retirar el proyecto.

## Invitaciones a proyectos

Desde **Detalle de proyecto → Equipo → Invitar integrante**, el creador puede
invitar por correo a una cuenta registrada, mientras el proyecto no tenga
planificación. La app envía `POST /api/proyectos/{id}/invitaciones`, informa el
resultado y vuelve a consultar el equipo para mostrar el estado `PENDIENTE`.
El botón evita envíos simultáneos y muestra errores de usuario no registrado,
permisos o invitación existente. Si no puede confirmarse el envío, se debe revisar
el equipo antes de reintentar.

El destinatario debe iniciar sesión y abrir **Invitaciones → Pendientes**; puede
deslizar hacia abajo para actualizar. Las invitaciones son internas de la app,
no notificaciones push ni correos electrónicos. Se pueden usar ambas cuentas
en el mismo teléfono cerrando sesión entre ellas.

## Notificaciones de solicitudes de cambios

La campana abre la bandeja del usuario autenticado (`GET /api/notificaciones`).
Desde **Mis proyectos** muestra la cantidad de avisos sin leer y se actualiza al
volver a la pantalla o deslizar para recargar. La bandeja permite marcar cada aviso como leído mediante
`PUT /api/notificaciones/{id}/leida`, sin eliminar el mensaje ni aprobar la solicitud.
En lugar del botón de actualizar, **Eliminar leídas** pide confirmación y utiliza
`DELETE /api/notificaciones/leidas` (204 sin cuerpo) para borrar solo las leídas de
la cuenta autenticada; conserva las no leídas. Se deshabilita si no hay leídas,
los datos están desactualizados o hay una operación en curso. No se retiran avisos
antes de la confirmación del servidor; si se pierde la respuesta se consulta la
bandeja sin repetir automáticamente el borrado. Requiere desplegar el backend con
el nuevo endpoint; esta operación no requiere una migración adicional.

Cuando un integrante solicita cambios desde **Mis tareas**, el backend avisa al
creador del proyecto. Las nuevas notificaciones incluyen el nombre del solicitante,
el nombre/código del proyecto y su comentario completo (incluido el contexto de la
tarea si se indicó). Los mensajes anteriores se conservan tal como se guardaron.
También se muestran los demás avisos reales que el backend envíe a esa cuenta.

Cada aviso nuevo guarda el proyecto de origen y muestra su **nombre y código
(TEC-id)** en una línea independiente. Esto incluye activaciones de planificación,
asignaciones, reasignaciones, cambios de estado y desbloqueos. El nombre se conserva
como era al generar el aviso, aunque el proyecto se renombre o elimine. Los avisos
históricos sin ese contexto muestran **Proyecto no identificado**; no se les asigna
un proyecto por coincidencias de texto. Se requiere actualizar/reiniciar el backend
para aplicar la migración `V15__notificacion_proyecto.sql` y generar nuevos avisos
con estos datos; una app actualizada sigue aceptando respuestas del backend anterior.

Son notificaciones **in-app**, no push ni correos: requieren conexión y se consultan
al abrir/volver o actualizar; no hay recepción en segundo plano con la app cerrada.
Si falla una consulta se conserva la última bandeja conocida y se indica el error;
un fallo no se representa como una bandeja vacía ni como una lectura confirmada.

## Catálogo de habilidades

Desde **Mi perfil → Explorar catálogo de habilidades** se consultan el catálogo
y las habilidades actuales del usuario. Se puede buscar por nombre o área y
seleccionar una habilidad no registrada. Para añadirla es obligatorio elegir
Básico, Intermedio o Avanzado; el nivel se envía al backend y se consulta el
perfil al terminar. Al volver, el perfil se recarga. Si no puede confirmarse el
resultado de la operación, se debe reintentar la consulta antes de volver a
añadirla. Los niveles anteriores no especificados aparecen como tales en la
lista expandida del perfil. No se muestran descripciones ni cifras inventadas.

## Registro rápido mediante JSON

En **Crear Proyecto → Importar JSON** puedes seleccionar un archivo JSON UTF-8
(máximo 1 MiB), pegar su contenido o pulsar **Cargar ejemplo JSON**. Después de
**Validar JSON**, revisa el resumen y confirma **Reemplazar formulario**. La
importación sustituye todos los campos actuales, incluidos los ausentes del JSON;
cancelar o introducir un JSON inválido no modifica el formulario. El proyecto no
se envía al backend hasta pulsar **Guardar Proyecto** y confirmar.

El ejemplo incluido en la aplicación está en
`/Users/niltonjavier/Desktop/PROYECTO COLAB (NEW)/Proyecto-Colab/app-movil/app/src/main/assets/proyecto-ejemplo.json`.

- Se admiten los campos de `ProyectoRequest` y `tecnologias` como lista de nombres.
- `nombre` es obligatorio (máximo 60 caracteres). `descripcion` admite hasta 180
  caracteres y `problematica` hasta 300, de acuerdo con el formulario.
- `fechaEntrega` usa `AAAA-MM-DD` y debe ser una fecha válida.
- `objetivosEspecificos` admite texto separado por saltos de línea o una lista de textos.
- `tecnologias` admite nombres del catálogo (sin distinguir mayúsculas) y
  `tecnologiasIds` admite IDs numéricos de ese mismo catálogo. Se pueden combinar
  y no se duplican. Las tecnologías desconocidas bloquean la importación para
  evitar omitirlas silenciosamente; usa **Recargar catálogo** si es necesario.
- Los requerimientos admiten objetos con `descripcion` obligatoria y `codigo`
  opcional; los no funcionales también admiten `categoria`. Para facilitar la
  carga, ambas listas aceptan textos de descripción directamente.
- `casosDeUso` admite los diez campos del backend; `nombre` es obligatorio en
  cada caso y los demás son textos opcionales.
- Los campos opcionales ausentes o `null` se importan vacíos. Se rechazan campos
  desconocidos, claves duplicadas, tipos incorrectos y JSON mal formado.
- Esta opción solo está disponible al crear proyectos, no al editarlos.

## Espera y recuperación de la planificación con IA

La generación usa un cliente HTTP independiente: hasta 180 segundos de espera de
lectura y 210 segundos de límite total. Las consultas y las demás operaciones
mantienen su configuración habitual. No se reintenta automáticamente el POST.

Si se pierde la respuesta o el servidor indica que la planificación ya existe,
la pantalla consulta el resultado hasta 20 veces, con pausas de tres segundos.
Si aún no se puede confirmar, ofrece **Consultar resultado**, que no inicia otra
generación. Los errores explícitos de validación, permisos o respuesta inválida
de la IA conservan el mensaje del backend.

Los proyectos con una generación pendiente se recuerdan en memoria al salir y
volver a entrar durante el mismo proceso de la app. Esta protección no sobrevive
al cierre completo del proceso. El backend actual no expone un estado de trabajo
ni una clave de idempotencia: para garantizar la recuperación entre reinicios y
distinguir una generación todavía activa de una que terminó sin guardarse se
necesitaría ampliar ese contrato. Mientras el resultado sea incierto, la app evita
iniciar automáticamente otra generación.
