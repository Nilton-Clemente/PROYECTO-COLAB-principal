from django.db import connection, transaction
from django.db.models import Q
from django.http import Http404
from rest_framework import mixins, status, viewsets
from rest_framework.response import Response

from .models import Habilidad, Tecnologia, Usuario
from .serializers import HabilidadSerializer, TecnologiaSerializer, UsuarioSerializer


class HabilidadViewSet(viewsets.ModelViewSet):
    queryset = Habilidad.objects.all()
    serializer_class = HabilidadSerializer

    def destroy(self, request, *args, **kwargs):
        with transaction.atomic():
            try:
                habilidad = Habilidad.objects.select_for_update().get(pk=kwargs['pk'])
            except Habilidad.DoesNotExist:
                raise Http404
            with connection.cursor() as cursor:
                cursor.execute(
                    'SELECT 1 FROM "usuario"."usuario_habilidad" WHERE habilidad_id = %s LIMIT 1',
                    [habilidad.pk],
                )
                if cursor.fetchone():
                    return Response(
                        {'detail': 'No se puede eliminar una habilidad asignada a usuarios.'},
                        status=status.HTTP_409_CONFLICT,
                    )
            habilidad.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


class TecnologiaViewSet(viewsets.ModelViewSet):
    queryset = Tecnologia.objects.all()
    serializer_class = TecnologiaSerializer

    def destroy(self, request, *args, **kwargs):
        with transaction.atomic():
            try:
                tecnologia = Tecnologia.objects.select_for_update().get(pk=kwargs['pk'])
            except Tecnologia.DoesNotExist:
                raise Http404
            with connection.cursor() as cursor:
                cursor.execute(
                    'SELECT 1 FROM "usuario"."proyecto_tecnologia" WHERE tecnologia_id = %s LIMIT 1',
                    [tecnologia.pk],
                )
                if cursor.fetchone():
                    return Response(
                        {'detail': 'No se puede eliminar una tecnología asociada a proyectos.'},
                        status=status.HTTP_409_CONFLICT,
                    )
            tecnologia.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


class UsuarioViewSet(mixins.ListModelMixin, mixins.RetrieveModelMixin, viewsets.GenericViewSet):
    queryset = Usuario.objects.all()
    serializer_class = UsuarioSerializer

    def get_queryset(self):
        qs = super().get_queryset()
        q = self.request.query_params.get('q')
        if q:
            qs = qs.filter(Q(email__icontains=q) | Q(nombre__icontains=q))
        return qs
