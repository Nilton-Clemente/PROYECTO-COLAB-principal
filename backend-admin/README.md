# Backend de administración

Servicio Django conectado a PostgreSQL. Django administra sus propias tablas en el esquema `admin`; las tablas del esquema `usuario` corresponden al backend Spring Boot y sus migraciones Flyway.

## Configuración local

1. Crear un entorno virtual e instalar `requirements.txt`.
2. Configurar `DJANGO_SECRET_KEY`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_HOST` y `DB_PORT`. Opcionalmente, configurar `DJANGO_DEBUG` y `DJANGO_ALLOWED_HOSTS`. Para el restablecimiento de contraseñas, configurar `ADMIN_API_KEY` (clave compartida con backend-usuario) y, si corresponde, `BACKEND_USUARIO_URL` (por defecto `http://localhost:8081`).
3. Crear previamente el esquema `admin` en PostgreSQL, con un usuario que tenga los permisos necesarios, y ejecutar `python manage.py migrate`.
4. Configurar `ADMIN_PASSWORD` y ejecutar `python manage.py crear_admin`. El nombre de usuario predeterminado es `admin@tecsup.edu.pe`; se puede cambiar con `ADMIN_EMAIL`. La contraseña de ejemplo acordada es solo para desarrollo, no para producción.
5. Iniciar el servidor con `python manage.py runserver`.

El login JWT está disponible en `POST /api/admin/auth/login/`, con los campos `username` y `password`. El token se renueva en `POST /api/admin/auth/refresh/`. Los endpoints DRF protegidos requieren un usuario activo con `is_staff`.

## Catálogos (HU-19)

Los recursos `/api/admin/habilidades/` y `/api/admin/tecnologias/` permiten listar y crear con GET y POST; para consultar, editar o eliminar un elemento se usa `/{id}/` con GET, PUT/PATCH o DELETE. Los nombres son obligatorios y únicos; las habilidades admiten un área opcional. Se responde con 409 si se intenta eliminar una habilidad asignada a usuarios o una tecnología vinculada a proyectos. Las tablas son gestionadas por Flyway, no por migraciones Django.

## Cuentas de usuario (HU-18)

`GET /api/admin/usuarios/` lista las cuentas del esquema `usuario` y acepta el parámetro `q` para buscar por email o nombre. `GET /api/admin/usuarios/{id}/` devuelve el detalle de una cuenta. Todas las acciones siguientes requieren token JWT de un usuario `is_staff`:

- `POST /api/admin/usuarios/{id}/bloquear/` y `POST /api/admin/usuarios/{id}/desbloquear/` escriben directamente el campo `bloqueada` en `usuario.usuario`.
- `POST /api/admin/usuarios/{id}/restablecer_contrasena/` recibe `{"contrasena": "..."}` (6–100 caracteres) y delega el cálculo del hash BCrypt en backend-usuario mediante `PUT /api/admin/usuarios/{id}/contrasena` con el header `X-Admin-Key`. Django no almacena ni conoce el hash; por eso la clave tiene que coincidir en ambos servicios: Spring Boot la lee de `colab.admin.api-key` (`ADMIN_API_KEY`) y Django de `ADMIN_API_KEY`.

Ejecutar las pruebas aisladas con `python manage.py test core --settings=backend_admin.test_settings`. Estas usan SQLite y no sustituyen las pruebas de integración con PostgreSQL y los esquemas reales.
