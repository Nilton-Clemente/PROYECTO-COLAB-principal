from unittest.mock import patch

from django.contrib.auth import get_user_model
from django.core.management import call_command
from django.core.management.base import CommandError
from django.test import TestCase, override_settings
from django.urls import path
from rest_framework.response import Response
from rest_framework.test import APIClient
from rest_framework.views import APIView


class RecursoProtegido(APIView):
    def get(self, request):
        return Response({'ok': True})


from backend_admin.urls import urlpatterns as rutas_principales

urlpatterns = rutas_principales + [path('prueba-protegida/', RecursoProtegido.as_view())]


@override_settings(ROOT_URLCONF=__name__)
class AutenticacionAdminTests(TestCase):
    def setUp(self):
        usuarios = get_user_model()
        usuarios.objects.create_user(username='normal', password='clave-normal-segura')
        usuarios.objects.create_superuser(username='admin', email='admin@example.com', password='clave-admin-segura')
        self.cliente = APIClient()

    def token(self, usuario, clave):
        return self.cliente.post('/api/admin/auth/login/', {'username': usuario, 'password': clave}, format='json')

    def test_sin_token_no_puede_acceder(self):
        self.assertEqual(self.cliente.get('/prueba-protegida/').status_code, 401)

    def test_usuario_sin_rol_admin_no_puede_acceder(self):
        token = self.token('normal', 'clave-normal-segura').data['access']
        self.cliente.credentials(HTTP_AUTHORIZATION='Bearer ' + token)
        self.assertEqual(self.cliente.get('/prueba-protegida/').status_code, 403)

    def test_admin_puede_acceder_y_credenciales_invalidas_fallan(self):
        self.assertEqual(self.token('admin', 'incorrecta').status_code, 401)
        token = self.token('admin', 'clave-admin-segura').data['access']
        self.cliente.credentials(HTTP_AUTHORIZATION='Bearer ' + token)
        self.assertEqual(self.cliente.get('/prueba-protegida/').status_code, 200)

    def test_admin_inicial_requiere_password_y_es_idempotente(self):
        with patch.dict('os.environ', {}, clear=True):
            with self.assertRaises(CommandError):
                call_command('crear_admin')
        with patch.dict('os.environ', {'ADMIN_PASSWORD': 'clave-inicial-segura'}, clear=True):
            call_command('crear_admin')
            call_command('crear_admin')
        self.assertEqual(get_user_model().objects.filter(username='admin@tecsup.edu.pe').count(), 1)
