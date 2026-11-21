import os

from django.contrib.auth import get_user_model
from django.core.management.base import BaseCommand, CommandError


class Command(BaseCommand):
    help = 'Crea la cuenta de administrador inicial si no existe.'

    def handle(self, *args, **options):
        email = os.environ.get('ADMIN_EMAIL', 'admin@tecsup.edu.pe')
        password = os.environ.get('ADMIN_PASSWORD')
        if not password:
            raise CommandError('Define ADMIN_PASSWORD antes de crear el administrador.')
        usuario = get_user_model()
        if usuario.objects.filter(username=email).exists():
            self.stdout.write('La cuenta de administrador ya existe.')
            return
        usuario.objects.create_superuser(username=email, email=email, password=password)
        self.stdout.write(f'Cuenta de administrador creada: {email}')