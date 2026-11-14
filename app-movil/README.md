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
