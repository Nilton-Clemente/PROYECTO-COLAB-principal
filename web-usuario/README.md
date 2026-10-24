# Web de usuario (React + Vite)

Cliente web de Colab para estudiantes. Consume `backend-usuario` (Spring Boot, puerto 8081).

## Funcionalidades

- **Inicio de sesión** (`/login`): `POST /api/auth/login`, guarda el JWT y entra al perfil.
  "Mantener sesión activa" → `localStorage`; desmarcado → `sessionStorage`.
- **Registro** (`/registro`): `POST /api/auth/registro` (necesario para tener un usuario con el que probar).
- **Perfil y habilidades** (`/perfil`, ruta protegida):
  - `GET /api/usuario/perfil` y `GET /api/habilidades`
  - Agregar: `PUT /api/usuario/habilidades/{id}` · Eliminar: `DELETE /api/usuario/habilidades/{id}`
  - Filtro por área y búsqueda sobre el catálogo.
- **Cerrar sesión**: borra el token y vuelve a `/login`. Si el token vence (el backend responde 401/403), cierra sesión sola y avisa.

Los datos viven en PostgreSQL (esquema `usuario`); las tablas y el catálogo inicial los crea Flyway al arrancar el backend.

## Puesta en marcha

1. **PostgreSQL**: crear la base `colab` (`CREATE DATABASE colab;`). Usuario/clave según `backend-usuario/src/main/resources/application.yml`.
2. **Backend**: en `backend-usuario/` ejecutar `./mvnw spring-boot:run` (queda en `http://localhost:8081`).
3. **Web**:
   ```bash
   cd web-usuario
   npm install
   npm run dev      # http://localhost:5173
   ```
4. Crear una cuenta en `/registro` (correo `@tecsup.edu.pe`, mínimo 6 caracteres con letras y números) y luego iniciar sesión.

## CORS

`backend-usuario` no configura CORS. En desarrollo, Vite reenvía `/api/*` al backend (ver `vite.config.js`), por lo que el navegador siempre habla con el mismo origen. Para apuntar a otro host: `VITE_API_PROXY_TARGET` (ver `.env.example`).
Para un despliegue sin proxy habrá que añadir CORS en el backend o servir ambos tras el mismo dominio.

## Estructura

```
src/
├─ api/         client.js (fetch + errores) · colab.js (endpoints)
├─ auth/        AuthContext.jsx (token, login, logout) · tokenStore.js
├─ components/  Campo, Alerta, Icons
├─ pages/       Login, Registro, Perfil
├─ config.js    dominio institucional y largo mínimo de contraseña
└─ styles.css
```
