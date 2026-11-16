from django.core.management.base import BaseCommand

from core.models import Habilidad, Tecnologia

HABILIDADES = [
    ('Desarrollo Frontend React', 'Desarrollo de Software'),
    ('Diseño de Interfaces (UI)', 'Diseño & Experiencia'),
    ('Gestión Ágil Scrum', 'Gestión de Proyectos'),
    ('Machine Learning Básico', 'Inteligencia Artificial'),
]

TECNOLOGIAS = ['React', 'PostgreSQL', 'Docker', 'FastAPI']


class Command(BaseCommand):
    help = 'Carga datos de prueba (habilidades y tecnologías) de forma idempotente.'

    def handle(self, *args, **options):
        for nombre, area in HABILIDADES:
            _, creado = Habilidad.objects.get_or_create(nombre=nombre, defaults={'area': area})
            self.stdout.write(f'{"+" if creado else "="} Habilidad: {nombre}')

        for nombre in TECNOLOGIAS:
            _, creado = Tecnologia.objects.get_or_create(nombre=nombre)
            self.stdout.write(f'{"+" if creado else "="} Tecnología: {nombre}')

        self.stdout.write(self.style.SUCCESS('Datos de prueba cargados.'))