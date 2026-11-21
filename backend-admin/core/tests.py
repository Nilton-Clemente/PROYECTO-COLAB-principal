import urllib.error
from unittest.mock import patch

from django.conf import settings
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
class UsuarioApiTests(TestCase):
    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        # El esquema `usuario` ya está adjunto por HabilidadApiTests en esta suite.
        with connection.cursor() as cursor:
            cursor.execute('CREATE TABLE usuario.usuario (id INTEGER PRIMARY KEY AUTOINCREMENT, email VARCHAR(255) NOT NULL UNIQUE, contrasena_hash VARCHAR(255) NOT NULL, nombre VARCHAR(255) NOT NULL, fecha_registro TIMESTAMP, bloqueada BOOLEAN NOT NULL DEFAULT 0)')

    @classmethod
    def tearDownClass(cls):
        with connection.cursor() as cursor:
            cursor.execute('DROP TABLE usuario.usuario')
        super().tearDownClass()

    def setUp(self):
        self.cliente = APIClient()
        self.admin = get_user_model().objects.create_user(username='admin-usuarios', password='clave', is_staff=True)
        with connection.cursor() as cursor:
            cursor.execute('INSERT INTO usuario.usuario (email, contrasena_hash, nombre) VALUES (%s, %s, %s)', ['ana@tecsup.edu.pe', 'hash', 'Ana'])

    def test_listado_y_envio_requieren_administrador(self):
        self.assertEqual(self.cliente.get('/api/admin/usuarios/').status_code, 401)
        usuario_comun = get_user_model().objects.create_user(username='normal-usuarios', password='clave')
        self.cliente.force_authenticate(user=usuario_comun)
        self.assertEqual(self.cliente.get('/api/admin/usuarios/').status_code, 403)

    def test_listado_consulta_y_busqueda(self):
        self.cliente.force_authenticate(user=self.admin)
        self.assertEqual(len(self.cliente.get('/api/admin/usuarios/').data), 1)
        self.assertEqual(self.cliente.get('/api/admin/usuarios/?q=zzz').data, [])
        self.assertEqual(len(self.cliente.get('/api/admin/usuarios/?q=ana').data), 1)
        detalle = self.cliente.get('/api/admin/usuarios/1/')
        self.assertEqual(detalle.status_code, 200)
        self.assertEqual(detalle.data['email'], 'ana@tecsup.edu.pe')

    def test_bloquear_y_desbloquear(self):
        self.cliente.force_authenticate(user=self.admin)
        self.assertFalse(self.cliente.get('/api/admin/usuarios/1/').data['bloqueada'])
        self.assertTrue(self.cliente.post('/api/admin/usuarios/1/bloquear/').data['bloqueada'])
        self.assertTrue(self.cliente.get('/api/admin/usuarios/1/').data['bloqueada'])
        self.assertFalse(self.cliente.post('/api/admin/usuarios/1/desbloquear/').data['bloqueada'])

    def test_restablecer_contrasena(self):
        self.cliente.force_authenticate(user=self.admin)
        with patch('core.services.urllib.request.urlopen') as urlopen:
            respuesta = self.cliente.post(
                '/api/admin/usuarios/1/restablecer_contrasena/',
                {'contrasena': 'nueva-clave-segura'},
                format='json',
            )
            self.assertEqual(respuesta.status_code, 200)
            peticion = urlopen.call_args[0][0]
            self.assertEqual(peticion.get_full_url(), 'http://localhost:8081/api/admin/usuarios/1/contrasena')
            self.assertEqual(peticion.get_header('X-admin-key'), settings.ADMIN_API_KEY)
            self.assertIn(b'nueva-clave-segura', peticion.data)

    def test_restablecer_contrasena_valida_entrada(self):
        self.cliente.force_authenticate(user=self.admin)
        respuesta = self.cliente.post(
            '/api/admin/usuarios/1/restablecer_contrasena/',
            {'contrasena': '123'},
            format='json',
        )
        self.assertEqual(respuesta.status_code, 400)

    def test_restablecer_contrasena_error_del_backend(self):
        self.cliente.force_authenticate(user=self.admin)
        error = urllib.error.HTTPError(
            'http://localhost:8081/api/admin/usuarios/1/contrasena',
            401,
            'Unauthorized',
            None,
            None,
        )
        with patch('core.services.urllib.request.urlopen', side_effect=error):
            respuesta = self.cliente.post(
                '/api/admin/usuarios/1/restablecer_contrasena/',
                {'contrasena': 'nueva-clave-segura'},
                format='json',
            )
        self.assertEqual(respuesta.status_code, 502)
