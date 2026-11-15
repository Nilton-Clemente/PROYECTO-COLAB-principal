import { tokenStore } from '../auth/tokenStore.js';

// Todas las rutas viven bajo /api/admin y las resuelve el proxy de Vite en desarrollo.
const BASE = '/api/admin';

let alExpirarSesion = null;

export function onSesionExpirada(callback) {
  alExpirarSesion = callback;
}

function mensajeDeError(respuesta, datos) {
  if (datos && typeof datos.detail === 'string') return datos.detail;
  if (datos && typeof datos.message === 'string') return datos.message;
  return `El servidor respondió con un error (${respuesta.status}).`;
}

export async function pedir(ruta, { metodo = 'GET', cuerpo, token = tokenStore.get() } = {}) {
  const cabeceras = {};
  if (cuerpo !== undefined) cabeceras['Content-Type'] = 'application/json';
  if (token) cabeceras.Authorization = `Bearer ${token}`;

  let respuesta;
  try {
    respuesta = await fetch(`${BASE}${ruta}`, {
      method: metodo,
      headers: cabeceras,
      body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
    });
  } catch {
    const error = new Error('No se pudo conectar con el servidor. Verifica tu conexión.');
    error.status = 0;
    throw error;
  }

  const texto = await respuesta.text();
  let datos = null;
  if (texto) {
    try {
      datos = JSON.parse(texto);
    } catch {
      datos = null;
    }
  }

  if (!respuesta.ok) {
    if (respuesta.status === 401 && token && alExpirarSesion) alExpirarSesion();
    const error = new Error(mensajeDeError(respuesta, datos));
    error.status = respuesta.status;
    error.datos = datos;
    throw error;
  }

  return datos;
}