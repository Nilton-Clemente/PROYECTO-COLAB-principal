const ESTADOS_TAREAS = [
  { campo: 'completadas', etiqueta: 'Completadas' },
  { campo: 'enProgreso', etiqueta: 'En progreso' },
  { campo: 'pendientes', etiqueta: 'Pendientes' },
  { campo: 'bloqueadas', etiqueta: 'Bloqueadas' },
];

const formatoPorcentaje = new Intl.NumberFormat('es-PE', { style: 'percent', maximumFractionDigits: 1 });

export const porcentajeTareas = (cantidad, total) => total > 0 ? (cantidad / total) * 100 : null;

export function mostrarPorcentaje(porcentaje) {
  if (porcentaje === null) return '—';
  // Evitar que el redondeo muestre un cierre del 100 % con tareas aún abiertas.
  if (porcentaje >= 99.95 && porcentaje < 100) return `< ${formatoPorcentaje.format(1)}`;
  if (porcentaje > 0 && porcentaje < 0.05) return `< ${formatoPorcentaje.format(0.001)}`;
  return formatoPorcentaje.format(porcentaje / 100);
}

export function distribucionTareas(metricas) {
  return ESTADOS_TAREAS.map(({ campo, etiqueta }) => ({
    campo, etiqueta, cantidad: metricas[campo], porcentaje: porcentajeTareas(metricas[campo], metricas.totalTareas),
  }));
}

export function avancePorIntegrante(porResponsable, equipo) {
  const integrantes = new Map(equipo.map((integrante) => [integrante.integranteId, integrante]));
  return porResponsable.map((avance) => ({
    ...avance,
    nombre: integrantes.get(avance.integranteId)?.nombre || avance.email,
    rol: integrantes.get(avance.integranteId)?.rol,
    porcentaje: porcentajeTareas(avance.completadas, avance.totalTareas),
  }));
}

export function entregaVencida({ fechaEntrega, estado }, hoy = new Date()) {
  if (estado !== 'EN_DESARROLLO' || !fechaEntrega) return false;
  const fechaLocal = `${String(hoy.getFullYear()).padStart(4, '0')}-${String(hoy.getMonth() + 1).padStart(2, '0')}-${String(hoy.getDate()).padStart(2, '0')}`;
  return fechaEntrega < fechaLocal;
}