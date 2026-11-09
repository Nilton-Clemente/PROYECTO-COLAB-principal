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

// Proyectos y equipo
export const listarProyectos = (token) => request('/api/proyectos', { token });
export const verProyecto = (token, id) => request(`/api/proyectos/${id}`, { token });
export const crearProyecto = (token, datos) => request('/api/proyectos', { method: 'POST', token, body: datos });
export const listarTecnologias = (token) => request('/api/tecnologias', { token });
export const verEquipo = (token, id) => request(`/api/proyectos/${id}/integrantes`, { token });
export const invitar = (token, id, email) =>
  request(`/api/proyectos/${id}/invitaciones`, { method: 'POST', token, body: { email } });
export const eliminarIntegrante = (token, id, integranteId) =>
  request(`/api/proyectos/${id}/integrantes/${integranteId}`, { method: 'DELETE', token });

// Invitaciones recibidas
export const misInvitaciones = (token) => request('/api/invitaciones', { token });
export const aceptarInvitacion = (token, id) => request(`/api/invitaciones/${id}/aceptar`, { method: 'POST', token });
export const rechazarInvitacion = (token, id) => request(`/api/invitaciones/${id}/rechazar`, { method: 'POST', token });

// Planificación con IA
export const obtenerPlanificacion = (token, id) => request(`/api/proyectos/${id}/planificacion`, { token });
export const generarPlanificacion = (token, id) =>
  request(`/api/proyectos/${id}/planificacion`, { method: 'POST', token });
export const editarProyecto = (token, id, datos) =>
  request(`/api/proyectos/${id}`, { method: 'PUT', token, body: datos });
