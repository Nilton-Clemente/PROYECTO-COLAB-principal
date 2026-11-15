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
