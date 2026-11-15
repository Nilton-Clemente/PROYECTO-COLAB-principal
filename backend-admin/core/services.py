"""Servicios de integración con el backend de usuario (Spring Boot).

El backend de administración no calcula ni almacena hashes de contraseña de los
usuarios; según la arquitectura (opción B) delega esa responsabilidad en
backend-usuario mediante el endpoint interno PUT /api/admin/usuarios/{id}/contrasena,
autenticado con la clave compartida X-Admin-Key.
"""

import json
import urllib.error
import urllib.request

from django.conf import settings


class BackendUsuarioError(Exception):
    """Error devuelto por el backend de usuario."""

    def __init__(self, codigo_estado, detalle=''):
        self.codigo_estado = codigo_estado
        self.detalle = detalle
        super().__init__(detalle or f'Error {codigo_estado} del backend de usuario')


def _url_base():
    return getattr(settings, 'BACKEND_USUARIO_URL', 'http://localhost:8081').rstrip('/')


def restablecer_contrasena(usuario_id, contrasena):
    """Solicita a backend-usuario que reemplace el hash de la contraseña."""
    url = f'{_url_base()}/api/admin/usuarios/{usuario_id}/contrasena'
    cuerpo = json.dumps({'contrasena': contrasena}).encode('utf-8')
    peticion = urllib.request.Request(
        url,
        data=cuerpo,
        method='PUT',
        headers={
            'Content-Type': 'application/json',
            'X-Admin-Key': getattr(settings, 'ADMIN_API_KEY', ''),
        },
    )
    try:
        with urllib.request.urlopen(peticion, timeout=10):
            return
    except urllib.error.HTTPError as exc:
        raise BackendUsuarioError(exc.code, _extraer_detalle(exc))


def _extraer_detalle(exc):
    try:
        payload = json.loads(exc.read().decode('utf-8'))
    except Exception:
        return ''
    return payload.get('message') or payload.get('detail') or ''