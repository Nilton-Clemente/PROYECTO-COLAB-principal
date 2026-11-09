import { DOMINIO_INSTITUCIONAL } from './config.js';

export function validarCorreoInstitucional(valor) {
  const email = valor.trim().toLowerCase();
  if (!email) return 'Ingresa tu correo institucional.';
  const ok = new RegExp(`^[^\\s@]+@${DOMINIO_INSTITUCIONAL.replace(/\./g, '\\.')}$`).test(email);
  return ok ? null : `Ingresa un correo válido con dominio ${DOMINIO_INSTITUCIONAL.split('.')[0].toUpperCase()}.`;
}

// El catálogo del backend viene sin tildes; se muestran bien en pantalla.
const ETIQUETAS = {
  Movil: 'Móvil',
  Gestion: 'Gestión',
  Analisis: 'Análisis',
  'Gestion de proyectos': 'Gestión de proyectos',
};
export const etiqueta = (texto) => ETIQUETAS[texto] ?? texto;

export const iniciales = (nombre = '') =>
  nombre
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join('');

// 'YYYY-MM-DD' → Date local (evita el desfase de zona horaria de new Date(iso)).
export function parseFecha(iso) {
  if (!iso) return null;
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d);
}
export function formatearFecha(valor) {
  const f = typeof valor === 'string' ? parseFecha(valor) : valor;
  return f ? f.toLocaleDateString('es-PE', { day: 'numeric', month: 'short', year: 'numeric' }) : 'Sin fecha';
}

export const ROL = { CREADOR: 'Creador', INTEGRANTE: 'Integrante' };
export const ESTADO = { ACTIVO: 'Activo', PENDIENTE: 'Pendiente de aceptación' };

// Texto multilínea → ítems (quita viñetas y numeración al inicio de cada línea).
export const lineas = (texto) =>
  (texto ?? '')
    .split('\n')
    .map((l) => l.replace(/^\s*(?:[-•*]|\d+[.)])\s*/, '').trim())
    .filter(Boolean);

export function diasHasta(iso) {
  const f = parseFecha(iso);
  if (!f) return null;
  const hoy = new Date();
  hoy.setHours(0, 0, 0, 0);
  return Math.round((f - hoy) / 86400000);
}

export const fechaCorta = (iso) => {
  const f = parseFecha(iso);
  return f ? f.toLocaleDateString('es-PE', { day: 'numeric', month: 'short' }) : '—';
};

export const diasEntre = (a, b) => Math.round((b - a) / 86400000);
export const codigoTarea = (id) => `TK-${String(id).padStart(3, '0')}`;
export const ESTADO_TAREA = { PENDIENTE: 'Pendiente', EN_PROGRESO: 'En progreso', COMPLETADA: 'Completada', BLOQUEADA: 'Bloqueada' };
export const PRIORIDAD = { ALTA: 'Alta', MEDIA: 'Media', BAJA: 'Baja' };

// Requisitos del servidor para generar la planificación (RN-18) que se pueden comprobar en el cliente.
export function requisitosPlanificacion(proyecto, integrantesActivos) {
  return [
    { texto: 'Objetivo general definido', ok: Boolean(proyecto.objetivoGeneral?.trim()) },
    { texto: 'Fecha de entrega definida', ok: Boolean(proyecto.fechaEntrega) },
    { texto: 'Al menos un requerimiento funcional', ok: proyecto.requerimientosFuncionales.length > 0 },
    { texto: 'Al menos una tecnología seleccionada', ok: proyecto.tecnologias.length > 0 },
    { texto: 'Al menos dos integrantes activos en el equipo', ok: integrantesActivos >= 2 },
  ];
}
