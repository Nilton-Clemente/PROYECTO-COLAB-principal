export function iniciales(nombre = '') {
  return nombre
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join('');
}

export function formatearFecha(valor) {
  if (!valor) return '—';
  const fecha = new Date(valor);
  if (Number.isNaN(fecha.getTime())) return '—';
  return fecha.toLocaleDateString('es-PE', { day: 'numeric', month: 'short', year: 'numeric' });
}

const PALETA_AREAS = ['#6355d9', '#00845d', '#007c83', '#4f46e5', '#bc1d25', '#b7791f', '#a0449b'];

export function colorArea(nombre = '') {
  if (!nombre) return '#777587';
  let hash = 0;
  for (const c of nombre) hash = (hash * 31 + c.charCodeAt(0)) >>> 0;
  return PALETA_AREAS[hash % PALETA_AREAS.length];
}