from django.apps import AppConfig
from django.db.backends.signals import connection_created


class CoreConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'core'

    def ready(self):
        # Django no crea el esquema del search_path como sí lo hace Flyway con
        # `usuario`; por eso se crea `admin` automáticamente al conectar.
        connection_created.connect(crear_esquema_admin, dispatch_uid='core.crear_esquema_admin')


def crear_esquema_admin(sender, connection, **kwargs):
    if connection.vendor != 'postgresql':
        return
    with connection.cursor() as cursor:
        cursor.execute('CREATE SCHEMA IF NOT EXISTS admin')