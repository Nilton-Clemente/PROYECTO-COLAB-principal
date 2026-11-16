# Guía de puesta en marcha local

Esta guía cubre PostgreSQL, `backend-usuario`, `backend-admin` y los frontends opcionales. Está orientada a macOS o Linux con zsh/bash.

## Antes de empezar

- Instala PostgreSQL, Java 21, Python 3 con soporte para `venv`, Node.js y npm. El proyecto incluye Maven Wrapper.
- Mantén PostgreSQL activo y abre **una terminal por servicio**. Las variables exportadas solo están disponibles en la terminal donde se definen y sus procesos hijos.
- Sustituye los valores entre `<...>` por los tuyos. No compartas ni subas contraseñas o claves al repositorio.
- Las rutas de esta guía corresponden a esta computadora; tus compañeros deberán adaptarlas a su carpeta de clonación.
- Pulsa `Ctrl+C` en la terminal correspondiente para detener un servidor.

## 1. PostgreSQL

La configuración predeterminada utiliza la base `colab` en `localhost:5432`. Si todavía no existe, créala desde una herramienta SQL conectada a PostgreSQL:

```sql
CREATE DATABASE colab;
```

Los dos backends deben utilizar la misma base. Flyway gestiona el esquema `usuario`; Django intenta crear `admin` al conectarse. El usuario de Django necesita permiso `CREATE` en la base y los permisos necesarios sobre las tablas del esquema `usuario`. La integración completa con PostgreSQL debe comprobarse en el entorno real.

## 2. Backend de usuario: Spring Boot

En una terminal, configura la clave compartida y las credenciales reales de PostgreSQL:

```bash
cd "/Users/niltonjavier/Desktop/PROYECTO COLAB (NEW)/Proyecto-Colab/backend-usuario"
export ADMIN_API_KEY="<clave-compartida>"
export DB_USERNAME="<usuario-postgresql>"
export SPRING_DATASOURCE_PASSWORD="<clave-postgresql>"
./mvnw spring-boot:run
```

El servidor escucha por defecto en `http://localhost:8081`. La URL configurada es `jdbc:postgresql://localhost:5432/colab`; si tu base está en otro lugar, configura también `SPRING_DATASOURCE_URL`. `SPRING_DATASOURCE_PASSWORD` permite sobrescribir la contraseña de desarrollo del archivo de configuración.

Espera a que el servicio arranque y Flyway aplique sus migraciones antes de acceder a las tablas `usuario` desde Django. Deja esta terminal abierta.

## 3. Backend de administración: Django

En otra terminal, desde el directorio del backend:

```bash
cd "/Users/niltonjavier/Desktop/PROYECTO COLAB (NEW)/Proyecto-Colab/backend-admin"
python3 -m venv venv
source venv/bin/activate
python -m pip install -r requirements.txt
```

La creación del entorno y la instalación son para la preparación inicial; en una terminal nueva debes volver a activar el entorno con `source venv/bin/activate`.

Define las variables, sin comentarios al final de las líneas:

```bash
export DJANGO_SECRET_KEY="<clave-django-larga-y-secreta>"
export DJANGO_DEBUG=true
export DB_NAME=colab
export DB_USER="<usuario-postgresql>"
export DB_PASSWORD="<clave-postgresql>"
export DB_HOST=localhost
export DB_PORT=5432
export ADMIN_API_KEY="<clave-compartida>"
export BACKEND_USUARIO_URL=http://localhost:8081
```

- `ADMIN_API_KEY` debe tener **exactamente el mismo valor** en ambos backends para restablecer contraseñas. No es la contraseña de PostgreSQL.
- `DJANGO_SECRET_KEY` debe ser diferente, aleatoria y mantenerse estable entre reinicios. Puedes generar un valor con el siguiente comando y guardarlo en un lugar seguro:

```bash
python -c "import secrets; print(secrets.token_urlsafe(50))"
```

No regeneres la clave de Django en cada inicio ni publiques su valor. `DJANGO_DEBUG=true` es solo para desarrollo local.

## 4. Migraciones y cuenta administrativa

En la terminal de Django, con el entorno y las variables configuradas:

```bash
python manage.py migrate
```

Si termina sin errores, crea la cuenta administrativa y arranca el servidor:

```bash
export ADMIN_PASSWORD="<contraseña-inicial-segura>"
python manage.py crear_admin
python manage.py runserver
```

El usuario predeterminado es `admin@tecsup.edu.pe`. Para elegir otro, define `ADMIN_EMAIL` antes de ejecutar `crear_admin`. Si la cuenta ya existe, el comando **no cambia su contraseña**. El admin nativo estará en `http://localhost:8000/admin/`.

## 5. Datos de demostración opcionales

Con Django configurado y las migraciones de Spring aplicadas, ejecuta en otra terminal con el mismo entorno configurado, o antes de iniciar `runserver`:

```bash
python manage.py seed_datos
```

El comando **modifica la base**: crea habilidades y tecnologías de ejemplo si no existen. No crea usuarios, proyectos ni relaciones. Úsalo solamente en un entorno de prueba autorizado.

## 6. Frontends opcionales

En una terminal para `web-admin`:

```bash
cd "/Users/niltonjavier/Desktop/PROYECTO COLAB (NEW)/Proyecto-Colab/web-admin"
npm install
npm run dev
```

Abre `http://localhost:5174`. El proxy de Vite reenvía `/api` a Django en el puerto 8000 durante el desarrollo.

Para `web-usuario`, usa otra terminal:

```bash
cd "/Users/niltonjavier/Desktop/PROYECTO COLAB (NEW)/Proyecto-Colab/web-usuario"
npm install
npm run dev
```

Abre `http://localhost:5173`. Su proxy apunta a Spring Boot en el puerto 8081. Los proxies de Vite no sustituyen la configuración de despliegue.

## 7. Problemas frecuentes

| Mensaje o problema | Qué revisar |
|---|---|
| `SECRET_KEY setting must not be empty` | Define `DJANGO_SECRET_KEY` en la terminal de Django. |
| `fe_sendauth: no password supplied` | Verifica que `DB_PASSWORD` esté exportada, sin mostrar su valor en pantalla. |
| `password authentication failed` | Revisa el usuario y la contraseña reales de PostgreSQL. |
| `permission denied` | Revisa los permisos de la base y los esquemas con su administrador. |
| Error al restablecer contraseña | Comprueba que Spring esté activo, la URL y que ambas `ADMIN_API_KEY` coincidan. |
| Aparece `bquote>` | Hay una comilla invertida sin cerrar. Pulsa `Ctrl+C` y vuelve a escribir el comando. |

Evita añadir comentarios al final de los comandos que pegues en zsh y no publiques tus claves.