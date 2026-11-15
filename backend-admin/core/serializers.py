from rest_framework import serializers

from .models import Habilidad


class HabilidadSerializer(serializers.ModelSerializer):
    class Meta:
        model = Habilidad
        fields = ['id', 'nombre', 'area']