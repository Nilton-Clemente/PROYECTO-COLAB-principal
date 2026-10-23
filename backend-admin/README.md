# Backend de Administración (Django)

Backend del área de **Administración**.

- **Responsable:** Persona 3
- **Tecnología:** Python + Django, PostgreSQL (esquema `admin`, migraciones de Django).

## Puesta en marcha

1. Crear el proyecto con `django-admin startproject` dentro de esta carpeta.
2. Configurar la conexión a PostgreSQL apuntando al esquema `admin`.

## Responsabilidades principales

Operaciones administrativas del sistema (funciones por definir — ver [`docs/03-objetivos-y-alcance.md`](../docs/03-objetivos-y-alcance.md)).

> **Nota:** este backend comparte la misma instancia PostgreSQL que `backend-usuario`, pero en un esquema separado (`admin`). Ver [`docs/ARQUITECTURA.md`](../docs/ARQUITECTURA.md#4-base-de-datos-compartida).
