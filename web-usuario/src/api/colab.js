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

// Guardado atómico de la planificación editada (CU-09).
export const guardarPlanificacion = (token, proyectoId, datos) =>
  request(`/api/proyectos/${proyectoId}/planificacion`, { method: 'PUT', token, body: datos });

// Edición manual de etapas, tareas y dependencias de una planificación.
export const listarTareas = (token, proyectoId) => request(`/api/proyectos/${proyectoId}/tareas`, { token });
export const crearEtapa = (token, proyectoId, datos) =>
  request(`/api/proyectos/${proyectoId}/etapas`, { method: 'POST', token, body: datos });
export const editarEtapa = (token, proyectoId, etapaId, datos) =>
  request(`/api/proyectos/${proyectoId}/etapas/${etapaId}`, { method: 'PUT', token, body: datos });
export const eliminarEtapa = (token, proyectoId, etapaId) =>
  request(`/api/proyectos/${proyectoId}/etapas/${etapaId}`, { method: 'DELETE', token });
export const crearTarea = (token, proyectoId, datos) =>
  request(`/api/proyectos/${proyectoId}/tareas`, { method: 'POST', token, body: datos });
export const editarTarea = (token, proyectoId, tareaId, datos) =>
  request(`/api/proyectos/${proyectoId}/tareas/${tareaId}`, { method: 'PUT', token, body: datos });
export const eliminarTarea = (token, proyectoId, tareaId) =>
  request(`/api/proyectos/${proyectoId}/tareas/${tareaId}`, { method: 'DELETE', token });
export const agregarDependencia = (token, proyectoId, tareaId, tareaPredecesoraId) =>
  request(`/api/proyectos/${proyectoId}/tareas/${tareaId}/dependencias`, { method: 'POST', token, body: { tareaPredecesoraId } });
export const quitarDependencia = (token, proyectoId, tareaId, tareaPredecesoraId) =>
  request(`/api/proyectos/${proyectoId}/tareas/${tareaId}/dependencias/${tareaPredecesoraId}`, { method: 'DELETE', token });

// Tareas y confirmación individual de la planificación.
export const misTareas = (token, proyectoId) => request(`/api/proyectos/${proyectoId}/tareas/mis-tareas`, { token });
export const confirmarPlanificacion = (token, proyectoId) =>
  request(`/api/proyectos/${proyectoId}/planificacion/confirmar`, { method: 'POST', token });
export const solicitarCambiosPlanificacion = (token, proyectoId, comentario) =>
  request(`/api/proyectos/${proyectoId}/planificacion/solicitar-cambios`, { method: 'POST', token, body: { comentario } });
export const listarConfirmaciones = (token, proyectoId) => request(`/api/proyectos/${proyectoId}/planificacion/confirmaciones`, { token });
export const historialAvances = (token, proyectoId, tareaId) => request(`/api/proyectos/${proyectoId}/tareas/${tareaId}/avances`, { token });
export const registrarAvance = (token, proyectoId, tareaId, datos) =>
  request(`/api/proyectos/${proyectoId}/tareas/${tareaId}/avances`, { method: 'POST', token, body: datos });
export const cambiarEstadoTarea = (token, proyectoId, tareaId, nuevoEstado) =>
  request(`/api/proyectos/${proyectoId}/tareas/${tareaId}/estado`, { method: 'PUT', token, body: { nuevoEstado } });
export const editarProyecto = (token, id, datos) =>
  request(`/api/proyectos/${id}`, { method: 'PUT', token, body: datos });
