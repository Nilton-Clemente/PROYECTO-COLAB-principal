import { request } from './client.js';

// Contrato de backend-usuario
export const login = (email, contrasena) =>
  request('/api/auth/login', { method: 'POST', body: { email, contrasena } });

export const registro = (nombre, email, contrasena) =>
  request('/api/auth/registro', { method: 'POST', body: { nombre, email, contrasena } });

export const obtenerPerfil = (token) => request('/api/usuario/perfil', { token });
export const listarCatalogo = (token) => request('/api/habilidades', { token });

export const agregarHabilidad = (token, id) =>
  request(`/api/usuario/habilidades/${id}`, { method: 'PUT', token });

export const eliminarHabilidad = (token, id) =>
  request(`/api/usuario/habilidades/${id}`, { method: 'DELETE', token });
