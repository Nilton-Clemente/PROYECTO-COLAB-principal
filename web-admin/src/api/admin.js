import { pedir } from './client.js';

// SimpleJWT espera "username" (el correo del administrador) y "password".
export async function login(correo, contrasena) {
  return pedir('/auth/login/', {
    metodo: 'POST',
    cuerpo: { username: correo, password: contrasena },
    token: null,
  });
}

export async function listarUsuarios() {
  return pedir('/usuarios/');
}

export async function bloquearUsuario(id) {
  return pedir(`/usuarios/${id}/bloquear/`, { metodo: 'POST' });
}

export async function desbloquearUsuario(id) {
  return pedir(`/usuarios/${id}/desbloquear/`, { metodo: 'POST' });
}

export async function restablecerContrasena(id, contrasena) {
  return pedir(`/usuarios/${id}/restablecer_contrasena/`, {
    metodo: 'POST',
    cuerpo: { contrasena },
  });
}

export async function listarHabilidades() {
  return pedir('/habilidades/');
}

export async function crearHabilidad(datos) {
  return pedir('/habilidades/', { metodo: 'POST', cuerpo: datos });
}

export async function actualizarHabilidad(id, datos) {
  return pedir(`/habilidades/${id}/`, { metodo: 'PATCH', cuerpo: datos });
}

export async function eliminarHabilidad(id) {
  return pedir(`/habilidades/${id}/`, { metodo: 'DELETE' });
}