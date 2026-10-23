# Colab

> **Colab** es una aplicación **móvil y web** que facilita la planificación y organización de proyectos académicos de software, usando inteligencia artificial (TECSUP — "La BestIA") para generar una propuesta de planificación personalizada.

## Documentación

Toda la documentación del proyecto vive en [`docs/`](docs/README.md).

## Estructura del repositorio (monorepo)

| Carpeta | Componente | Tecnología | Responsable |
|---|---|---|---|
| [`docs/`](docs/README.md) | Documentación | Markdown | Equipo |
| [`backend-usuario/`](backend-usuario/README.md) | Backend de Usuario | Spring Boot + PostgreSQL (esquema `usuario`) | Persona 1 |
| [`app-movil/`](app-movil/README.md) | App móvil | Android (Kotlin + Jetpack Compose) | Persona 2 |
| [`web-usuario/`](web-usuario/README.md) | Web de usuario | React | Persona 3 |
| [`backend-admin/`](backend-admin/README.md) | Backend de Administración | Django + PostgreSQL (esquema `admin`) | Persona 3 |
| [`web-admin/`](web-admin/README.md) | Web de Administración | React | Persona 3 |

## Cómo levantar cada componente

*(Provisional — las instrucciones definitivas estarán en el `README.md` de cada carpeta.)*

- **`backend-usuario/`** (Spring Boot): compilar con Maven/Gradle y ejecutar el jar o `./mvnw spring-boot:run`. Requiere PostgreSQL con el esquema `usuario`.
- **`app-movil/`** (Android Studio): abrir la carpeta en Android Studio y ejecutar en emulador/dispositivo; apunta a la API de `backend-usuario`.
- **`web-usuario/`** y **`web-admin/`** (React): `npm install` y `npm run dev`.
- **`backend-admin/`** (Django): `pip install -r requirements.txt`, `python manage.py migrate` y `python manage.py runserver`.

## Acuerdos importantes

- **Contrato de API primero:** `backend-usuario` publica el OpenAPI (por ejemplo en `backend-usuario/src/main/resources/openapi/`); los clientes móvil y web consumen únicamente ese contrato.
- **IA con mock por defecto:** se trabaja con `MockPlanIAClient`; la implementación real (OpenWebUI de TECSUP) solo funciona dentro de la red del instituto. Ver [`docs/04-analisis/08-integracion-ia.md`](docs/04-analisis/08-integracion-ia.md).
- **Secretos:** `USER_API_KEY`, contraseñas y archivos `.env` van en variables de entorno; nunca se versionan.
- **Base de datos:** una única instancia PostgreSQL con dos esquemas separados (`usuario` y `admin`). Ver [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md).

## Ramas y trabajo

- Ramas cortas por funcionalidad (p. ej. `backend/auth`, `mobile/login`) y merge a `main`.
- Cada persona trabaja en su carpeta para minimizar conflictos.
