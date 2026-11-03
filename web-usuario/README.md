# Web de usuario (React + Vite)

Cliente web de Colab para estudiantes. Consume `backend-usuario` (Spring Boot, puerto 8081).

## Funcionalidades

| Ruta | Pantalla | Endpoints de `backend-usuario` |
|---|---|---|
| `/login` | Inicio de sesión | `POST /api/auth/login` |
| `/registro` | Crear cuenta | `POST /api/auth/registro` |
| `/proyectos` | Mis proyectos (estadísticas, filtros, búsqueda, orden, crear proyecto) | `GET/POST /api/proyectos`, `GET /api/tecnologias`, `GET /api/invitaciones` |
| `/proyectos/:id` | Información del proyecto (resumen, objetivos, alcance, tecnologías, requerimientos, casos de uso) | `GET /api/proyectos/{id}`, `PUT /api/proyectos/{id}` (editar) |
| `/proyectos/:id/equipo` | Equipo: invitar, eliminar del equipo, cancelar invitación | `GET /api/proyectos/{id}/integrantes`, `POST …/invitaciones`, `DELETE …/integrantes/{id}` |
| `/proyectos/:id/planificacion` | Plan maestro generado por IA: etapas, tareas, dependencias, responsables, filtros | `GET/POST /api/proyectos/{id}/planificacion` |
| `/invitaciones` | Invitaciones recibidas: aceptar / rechazar | `GET /api/invitaciones`, `POST …/{id}/aceptar`, `POST …/{id}/rechazar` |
| `/perfil` | Perfil y habilidades | `GET /api/usuario/perfil`, `GET /api/habilidades`, `PUT/DELETE /api/usuario/habilidades/{id}` |

Cerrar sesión borra el token. Si el token vence, la app cierra sesión sola y lo avisa.
"Mantener sesión activa" usa `localStorage`; desmarcado, `sessionStorage`.

Los datos viven en PostgreSQL (esquema `usuario`); las tablas y los catálogos iniciales los crea Flyway al arrancar el backend.

### Decisiones sobre el contrato del backend

- **Sin estado, progreso ni código de proyecto**: el backend no los tiene, así que no se muestran. Las estadísticas usan datos reales (creados por mí, como integrante, próxima entrega).
- **`GET /api/proyectos` incluye proyectos con invitación pendiente**; la web los excluye de "Mis proyectos" (aparecen en Invitaciones).
- **Solo el Creador** puede invitar y eliminar integrantes; el resto ve el equipo en modo lectura.
- **No existe "reenviar invitación"** ni historial de invitaciones (rechazar borra el registro).
- **403 de negocio ≠ sesión vencida**: Spring responde 403 en ambos casos. Ante un 403 el cliente consulta `/api/usuario/perfil`; solo si también falla cierra la sesión (`src/api/client.js`).
- **Formulario de proyecto completo** (crear y editar): problemática, objetivos, alcance, restricciones, tecnologías, requerimientos y casos de uso. Los códigos (RF-01, RNF-01, CU-01) se generan según el orden.
- **Planificación (RN-18)**: solo el Creador puede generarla y el servidor exige objetivo general, fecha de entrega, ≥1 requerimiento funcional, ≥1 tecnología y ≥2 integrantes con habilidades. La web muestra esos requisitos antes de habilitar el botón. Ya generada, no se puede regenerar (409).
- **Tareas**: el backend no tiene código de tarea; se muestra `TK-<id>`. Tampoco hay endpoint para cambiar su estado ni para "validar etapas", por eso esas acciones del diseño no existen. El estado de las tareas se muestra tal como lo devuelve la API (`PENDIENTE` al generar).
- Con `IA_PROVIDER=mock` (valor por defecto del backend) la planificación se genera sin la red de TECSUP.

## Puesta en marcha

1. **PostgreSQL**: crear la base `colab` (`CREATE DATABASE colab;`). Usuario/clave según `backend-usuario/src/main/resources/application.yml`.
2. **Backend**: en `backend-usuario/` ejecutar `./mvnw spring-boot:run` (queda en `http://localhost:8081`).
3. **Web**:
   ```bash
   cd web-usuario
   npm install
   npm run dev      # http://localhost:5173
   ```
4. Crear una cuenta en `/registro` (crea dos para probar invitaciones) (correo `@tecsup.edu.pe`, mínimo 6 caracteres con letras y números) y luego iniciar sesión.

## CORS

`backend-usuario` no configura CORS. En desarrollo, Vite reenvía `/api/*` al backend (ver `vite.config.js`), por lo que el navegador siempre habla con el mismo origen. Para apuntar a otro host: `VITE_API_PROXY_TARGET` (ver `.env.example`).
Para un despliegue sin proxy habrá que añadir CORS en el backend o servir ambos tras el mismo dominio.

## Estructura

```
src/
├─ api/         client.js (fetch + errores) · colab.js (endpoints)
├─ auth/        AuthContext.jsx (token, login, logout) · tokenStore.js
├─ components/  Layout, Modal, ModalProyecto, ModalInvitar, ErrorBoundary, Campo, Alerta, Avatares, Icons
├─ pages/       Login, Registro, Proyectos, ProyectoDetalle, Invitaciones, Perfil
│  └─ proyecto/ Informacion, Equipo, Planificacion
├─ config.js    dominio institucional y largo mínimo de contraseña
└─ styles.css
```
