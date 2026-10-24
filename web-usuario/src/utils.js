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
