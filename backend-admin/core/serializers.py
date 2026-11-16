from rest_framework import serializers

from .models import Habilidad, Tecnologia, Usuario
from .iconos import IDENTIFICADORES_ICONOS


class IconoSerializerMixin:
    def validate_icono(self, value):
        if value in (None, ''):
            return None
        if value not in IDENTIFICADORES_ICONOS:
            raise serializers.ValidationError('Selecciona un icono disponible.')
        return value


class HabilidadSerializer(IconoSerializerMixin, serializers.ModelSerializer):
    icono = serializers.CharField(required=False, allow_null=True, allow_blank=True, max_length=64)

    class Meta:
        model = Habilidad
        fields = ['id', 'nombre', 'area', 'icono']


class TecnologiaSerializer(IconoSerializerMixin, serializers.ModelSerializer):
    icono = serializers.CharField(required=False, allow_null=True, allow_blank=True, max_length=64)

    class Meta:
        model = Tecnologia
        fields = ['id', 'nombre', 'icono']


class UsuarioSerializer(serializers.ModelSerializer):
    class Meta:
        model = Usuario
        fields = ['id', 'email', 'nombre', 'fecha_registro', 'bloqueada']


class RestablecerContrasenaSerializer(serializers.Serializer):
    contrasena = serializers.CharField(min_length=6, max_length=100, trim_whitespace=False)