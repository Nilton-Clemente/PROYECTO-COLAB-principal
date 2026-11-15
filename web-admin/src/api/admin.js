import { pedir } from './client.js';

// SimpleJWT espera "username" (el correo del administrador) y "password".
export async function login(correo, contrasena) {
  return pedir('/auth/login/', {
    metodo: 'POST',
    cuerpo: { username: correo, password: contrasena },
    token: null,
  });
}