from django.db import models


class Habilidad(models.Model):
    id = models.BigAutoField(primary_key=True)
    nombre = models.CharField(max_length=255, unique=True)
    area = models.CharField(max_length=255, null=True, blank=True)

    class Meta:
        managed = False
        db_table = '"usuario"."habilidad"'
        ordering = ['nombre', 'id']


class Tecnologia(models.Model):
    id = models.BigAutoField(primary_key=True)
    nombre = models.CharField(max_length=255, unique=True)

    class Meta:
        managed = False
        db_table = '"usuario"."tecnologia"'
        ordering = ['nombre', 'id']


class Usuario(models.Model):
    id = models.BigAutoField(primary_key=True)
    email = models.EmailField(max_length=255, unique=True)
    contrasena_hash = models.CharField(max_length=255)
    nombre = models.CharField(max_length=255)
    fecha_registro = models.DateTimeField(null=True, blank=True)
    bloqueada = models.BooleanField(default=False)

    class Meta:
        managed = False
        db_table = '"usuario"."usuario"'
        ordering = ['id']
