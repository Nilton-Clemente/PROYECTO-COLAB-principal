# Backend de administración

Servicio Django conectado a PostgreSQL. Django administra sus propias tablas en el esquema `admin`; las tablas del esquema `usuario` corresponden al backend Spring Boot y sus migraciones Flyway.

## Configuración local

1. Crear un entorno virtual e instalar `requirements.txt`.
2. Configurar `DJANGO_SECRET_KEY`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_HOST` y `DB_PORT`. Opcionalmente, configurar `DJANGO_DEBUG` y `DJANGO_ALLOWED_HOSTS`.
3. Crear previamente el esquema `admin` en PostgreSQL, con un usuario que tenga los permisos necesarios, y ejecutar `python manage.py migrate`.
4. Configurar `ADMIN_PASSWORD` y ejecutar `python manage.py crear_admin`. El nombre de usuario predeterminado es `admin@tecsup.edu.pe`; se puede cambiar con `ADMIN_EMAIL`. La contraseña de ejemplo acordada es solo para desarrollo, no para producción.
5. Iniciar el servidor con `python manage.py runserver`.

El login JWT está disponible en `POST /api/admin/auth/login/`, con los campos `username` y `password`. El token se renueva en `POST /api/admin/auth/refresh/`. Los endpoints DRF protegidos requieren un usuario activo con `is_staff`.

## Catálogos (HU-19)

Los recursos `/api/admin/habilidades/` y `/api/admin/tecnologias/` permiten listar y crear con GET y POST; para consultar, editar o eliminar un elemento se usa `/{id}/` con GET, PUT/PATCH o DELETE. Los nombres son obligatorios y únicos; las habilidades admiten un área opcional. Se responde con 409 si se intenta eliminar una habilidad asignada a usuarios o una tecnología vinculada a proyectos. Las tablas son gestionadas por Flyway, no por migraciones Django.

Ejecutar las pruebas aisladas con `python manage.py test core --settings=backend_admin.test_settings`. Estas usan SQLite y no sustituyen las pruebas de integración con PostgreSQL y los esquemas reales.
