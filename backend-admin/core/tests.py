from unittest.mock import patch

from django.contrib.auth import get_user_model
from django.core.management import call_command
from django.core.management.base import CommandError
from django.db import connection
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


class HabilidadApiTests(TestCase):
    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        if connection.vendor != 'sqlite':
            raise RuntimeError('Estas pruebas aisladas requieren test_settings con SQLite.')
        with connection.cursor() as cursor:
            cursor.execute("ATTACH DATABASE ':memory:' AS usuario")
            cursor.execute('CREATE TABLE usuario.habilidad (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre VARCHAR(255) NOT NULL UNIQUE, area VARCHAR(255))')
            cursor.execute('CREATE TABLE usuario.usuario_habilidad (usuario_id INTEGER NOT NULL, habilidad_id INTEGER NOT NULL)')
            cursor.execute('CREATE TABLE usuario.tecnologia (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre VARCHAR(255) NOT NULL UNIQUE)')
            cursor.execute('CREATE TABLE usuario.proyecto_tecnologia (proyecto_id INTEGER NOT NULL, tecnologia_id INTEGER NOT NULL)')

    @classmethod
    def tearDownClass(cls):
        with connection.cursor() as cursor:
            cursor.execute('DROP TABLE usuario.proyecto_tecnologia')
            cursor.execute('DROP TABLE usuario.tecnologia')
            cursor.execute('DROP TABLE usuario.usuario_habilidad')
            cursor.execute('DROP TABLE usuario.habilidad')
        super().tearDownClass()

    def setUp(self):
        self.cliente = APIClient()
        self.admin = get_user_model().objects.create_user(username='admin-habilidades', password='clave', is_staff=True)

    def test_requiere_administrador(self):
        self.assertEqual(self.cliente.get('/api/admin/habilidades/').status_code, 401)
        usuario = get_user_model().objects.create_user(username='normal-habilidades', password='clave')
        self.cliente.force_authenticate(user=usuario)
        self.assertEqual(self.cliente.get('/api/admin/habilidades/').status_code, 403)

    def test_crud_validacion_y_borrado_en_uso(self):
        self.cliente.force_authenticate(user=self.admin)
        url = '/api/admin/habilidades/'
        self.assertEqual(self.cliente.post(url, {'nombre': ''}, format='json').status_code, 400)
        creado = self.cliente.post(url, {'nombre': 'Java', 'area': 'Backend'}, format='json')
        self.assertEqual(creado.status_code, 201)
        detalle = f"{url}{creado.data['id']}/"
        self.assertEqual(len(self.cliente.get(url).data), 1)
        self.assertEqual(self.cliente.post(url, {'nombre': 'Java'}, format='json').status_code, 400)
        self.assertEqual(self.cliente.patch(detalle, {'area': 'Software'}, format='json').status_code, 200)
        with connection.cursor() as cursor:
            cursor.execute('INSERT INTO usuario.usuario_habilidad VALUES (%s, %s)', [1, creado.data['id']])
        self.assertEqual(self.cliente.delete(detalle).status_code, 409)
        with connection.cursor() as cursor:
            cursor.execute('DELETE FROM usuario.usuario_habilidad')
        self.assertEqual(self.cliente.delete(detalle).status_code, 204)
        self.assertEqual(self.cliente.get(detalle).status_code, 404)

    def test_crud_tecnologia_y_proteccion_de_asociaciones(self):
        url = '/api/admin/tecnologias/'
        self.assertEqual(self.cliente.get(url).status_code, 401)
        self.cliente.force_authenticate(user=self.admin)
        self.assertEqual(self.cliente.post(url, {'nombre': ' '}, format='json').status_code, 400)
        creado = self.cliente.post(url, {'nombre': 'React'}, format='json')
        self.assertEqual(creado.status_code, 201)
        detalle = f"{url}{creado.data['id']}/"
        self.assertEqual(self.cliente.post(url, {'nombre': 'React'}, format='json').status_code, 400)
        self.assertEqual(len(self.cliente.get(url).data), 1)
        self.assertEqual(self.cliente.patch(detalle, {'nombre': 'Kotlin'}, format='json').status_code, 200)
        with connection.cursor() as cursor:
            cursor.execute('INSERT INTO usuario.proyecto_tecnologia VALUES (%s, %s)', [1, creado.data['id']])
        self.assertEqual(self.cliente.delete(detalle).status_code, 409)
        with connection.cursor() as cursor:
            cursor.execute('DELETE FROM usuario.proyecto_tecnologia')
        self.assertEqual(self.cliente.delete(detalle).status_code, 204)
        self.assertEqual(self.cliente.get(detalle).status_code, 404)
