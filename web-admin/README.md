# Web de Administración (React)

Frontend de administración de la plataforma Colab (TECSUP). Consume la API del
`backend-admin` (Django) y cubre los casos HU-18 (cuentas de usuario) y HU-19
(catálogos de habilidades y tecnologías).

- **Tecnología:** React (Vite) + react-router-dom, estilos en `src/styles.css`.
- **Responsable:** Persona 3

## Puesta en marcha

1. Instalar dependencias: `npm install`.
2. Copiar `cp .env.example .env` (por defecto el proxy apunta a `http://localhost:8000`).
3. Levantar el backend `backend-admin` (que a su vez necesita `backend-usuario` y PostgreSQL).
4. Iniciar el frontend: `npm run dev` (por defecto en el puerto **5174**).

El navegador habla con Vite (mismo origen) y Vite reenvía `/api/*` al backend de
administración, por lo que no se requiere configuración de CORS en desarrollo.

## Autenticación

`POST /api/admin/auth/login/` con `{ username, password }` (el `username` es el correo
del administrador, p. ej. `admin@tecsup.edu.pe`). La respuesta JWT (`access`/`refresh`)
se guarda en `localStorage` o `sessionStorage` según la opción "Recordar sesión".

## Pantallas

- `/login` — inicio de sesión administrativo.
- `/usuarios` — listado, bloqueo/desbloqueo y restablecimiento de contraseña (HU-18).
- `/habilidades` — catálogo de habilidades (HU-19).
- `/tecnologias` — catálogo de tecnologías (HU-19).

Ver acuerdos en el [README raíz](../README.md).
