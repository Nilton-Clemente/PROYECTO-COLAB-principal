from .settings import *  # noqa: F403

SECRET_KEY = 'clave-exclusiva-para-pruebas-no-utilizar-en-produccion'
DATABASES = {'default': {'ENGINE': 'django.db.backends.sqlite3', 'NAME': ':memory:'}}
ALLOWED_HOSTS = ['testserver']

# SQLite no admite el RETURNING calificado por esquema que genera Django para
# estas tablas PostgreSQL; la inserción de prueba usa last_insert_rowid().
from django.db.backends.sqlite3.base import DatabaseFeatures
DatabaseFeatures.can_return_columns_from_insert = False

# Clave compartida para los endpoints internos de backend-usuario.
ADMIN_API_KEY = 'clave-admin-de-pruebas'