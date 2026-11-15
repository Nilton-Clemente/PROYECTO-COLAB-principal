from .settings import *  # noqa: F403

SECRET_KEY = 'clave-exclusiva-para-pruebas-no-utilizar-en-produccion'
DATABASES = {'default': {'ENGINE': 'django.db.backends.sqlite3', 'NAME': ':memory:'}}
ALLOWED_HOSTS = ['testserver']