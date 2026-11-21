import { etiqueta } from './utils.js';

export const LIMITE_HABILIDADES_PERFIL = 7;
export const NIVELES_HABILIDAD = Object.freeze({ BASICO: 'Básico', INTERMEDIO: 'Intermedio', AVANZADO: 'Avanzado' });

export const nivelValido = (nivel) => Object.hasOwn(NIVELES_HABILIDAD, nivel);
export const etiquetaNivel = (nivel) => nivelValido(nivel) ? NIVELES_HABILIDAD[nivel] : nivel?.trim() || 'Sin nivel';
export const areaHabilidad = (habilidad) => habilidad.area?.trim() || 'General';

const normalizar = (valor) => valor.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase().trim();
const orden = new Intl.Collator('es', { numeric: true, sensitivity: 'base' });

export const ordenarHabilidades = (habilidades) =>
  [...habilidades].sort((a, b) => orden.compare(etiqueta(a.nombre), etiqueta(b.nombre)) || a.id - b.id);

export function agruparHabilidades(habilidades, busqueda = '', area = '') {
  const grupos = new Map();
  const termino = normalizar(busqueda);
  for (const habilidad of ordenarHabilidades(habilidades)) {
    const categoria = areaHabilidad(habilidad);
    if (area && categoria !== area) continue;
    if (termino && !normalizar(`${etiqueta(habilidad.nombre)} ${etiqueta(categoria)}`).includes(termino)) continue;
    if (!grupos.has(categoria)) grupos.set(categoria, []);
    grupos.get(categoria).push(habilidad);
  }
  return [...grupos].sort(([a], [b]) => orden.compare(etiqueta(a), etiqueta(b)));
}

export function rutaIconoHabilidad(icono) {
  return typeof icono === 'string' && /^[a-z0-9][a-z0-9-]*$/.test(icono) ? `/iconos/devicon/${icono}.svg` : null;
}