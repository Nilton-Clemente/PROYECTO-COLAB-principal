from django.contrib import admin
from django.db import connection

from .models import Habilidad, Tecnologia, Usuario


def _usuarios_de_habilidad(habilidad_id):
    with connection.cursor() as cursor:
        cursor.execute(
            'SELECT u.email FROM "usuario"."usuario" u '
            'JOIN "usuario"."usuario_habilidad" uh ON uh.usuario_id = u.id '
            'WHERE uh.habilidad_id = %s ORDER BY u.email',
            [habilidad_id],
        )
        return [fila[0] for fila in cursor.fetchall()]


def _proyectos_de_tecnologia(tecnologia_id):
    with connection.cursor() as cursor:
        cursor.execute(
            'SELECT p.nombre FROM "usuario"."proyecto" p '
            'JOIN "usuario"."proyecto_tecnologia" pt ON pt.proyecto_id = p.id '
            'WHERE pt.tecnologia_id = %s ORDER BY p.nombre',
            [tecnologia_id],
        )
        return [fila[0] for fila in cursor.fetchall()]


@admin.register(Habilidad)
class HabilidadAdmin(admin.ModelAdmin):
    list_display = ('id', 'nombre', 'area', 'cantidad_usuarios')
    search_fields = ('nombre', 'area')
    list_filter = ('area',)
    readonly_fields = ('usuarios_asociados',)

    @admin.display(description='Usuarios asignados')
    def cantidad_usuarios(self, obj):
        return len(_usuarios_de_habilidad(obj.pk))

    @admin.display(description='Usuarios asignados')
    def usuarios_asociados(self, obj):
        emails = _usuarios_de_habilidad(obj.pk)
        return ', '.join(emails) if emails else '—'


@admin.register(Tecnologia)
class TecnologiaAdmin(admin.ModelAdmin):
    list_display = ('id', 'nombre', 'cantidad_proyectos')
    search_fields = ('nombre',)
    readonly_fields = ('proyectos_asociados',)

    @admin.display(description='Proyectos asociados')
    def cantidad_proyectos(self, obj):
        return len(_proyectos_de_tecnologia(obj.pk))

    @admin.display(description='Proyectos asociados')
    def proyectos_asociados(self, obj):
        nombres = _proyectos_de_tecnologia(obj.pk)
        return ', '.join(nombres) if nombres else '—'


@admin.register(Usuario)
class UsuarioAdmin(admin.ModelAdmin):
    list_display = ('id', 'email', 'nombre', 'fecha_registro', 'bloqueada')
    search_fields = ('email', 'nombre')
    list_filter = ('bloqueada',)
    # El hash de contraseña es de Spring Boot (BCrypt): no se muestra ni se edita.
    fields = ('id', 'email', 'nombre', 'fecha_registro', 'bloqueada')
    readonly_fields = ('id', 'email', 'nombre', 'fecha_registro', 'bloqueada')

    def has_add_permission(self, request):
        return False

    def has_delete_permission(self, request, obj=None):
        return False