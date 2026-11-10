export const MIN_HORAS_AVANCE = 0.5;
export const MAX_HORAS_AVANCE = 16;
export const MIN_NOTA_AVANCE = 20;
export const MAX_NOTA_AVANCE = 1000;

export const OPCIONES_ESTADO_AVANCE = [
  { id: 'PENDIENTE', titulo: 'Pendiente', detalle: 'Trabajo por iniciar' },
  { id: 'EN_PROGRESO', titulo: 'En progreso', detalle: 'Desarrollo activo' },
  { id: 'BLOQUEADA', titulo: 'Bloqueada', detalle: 'Calculado por dependencias' },
  { id: 'COMPLETADA', titulo: 'Completada', detalle: 'Trabajo terminado' },
];

// Las transiciones coinciden con TareaService.transicionValida (RN-07).
export function permiteEstadoAvance(actual, nuevo) {
  if (!OPCIONES_ESTADO_AVANCE.some(({ id }) => id === actual)) return false;
  if (actual === nuevo) return true;
  if (actual === 'PENDIENTE') return nuevo === 'EN_PROGRESO';
  if (actual === 'EN_PROGRESO') return nuevo === 'PENDIENTE' || nuevo === 'COMPLETADA';
  return false;
}

export function validarAvance({ horas, nota, estadoActual, nuevoEstado }) {
  const errores = {};
  if (!Number.isFinite(horas) || horas < MIN_HORAS_AVANCE || horas > MAX_HORAS_AVANCE || !Number.isInteger(horas * 2)) {
    errores.horas = 'Registra entre 0.5 y 16 horas, en intervalos de 30 minutos.';
  }
  const texto = typeof nota === 'string' ? nota.trim() : '';
  if (texto.length < MIN_NOTA_AVANCE || texto.length > MAX_NOTA_AVANCE) {
    errores.nota = `Describe el trabajo realizado con entre ${MIN_NOTA_AVANCE} y ${MAX_NOTA_AVANCE} caracteres.`;
  }
  if (!permiteEstadoAvance(estadoActual, nuevoEstado)) {
    errores.estado = 'El cambio de estado no está permitido. Revisa el estado actual de la tarea.';
  }
  return errores;
}

export function datosAvance(horas, nota) {
  return { minutos: Math.round(horas * 60), nota: nota.trim() };
}

export function resumenTiempo(totalMinutos, horasSesion) {
  const acumuladas = totalMinutos / 60;
  const sesion = Number.isFinite(horasSesion) && horasSesion > 0 ? horasSesion : 0;
  return { acumuladas, sesion, proyectadas: acumuladas + sesion };
}

// El backend no ofrece una operación atómica para tiempo + estado.
// Si el POST se guardó, nunca debe repetirse para reintentar solamente el PUT.
export async function guardarAvanceConEstado({ registrar, cambiarEstado }, { horas, nota, estadoActual, nuevoEstado }) {
  const errores = validarAvance({ horas, nota, estadoActual, nuevoEstado });
  if (Object.keys(errores).length) throw new Error(Object.values(errores)[0]);
  const registro = await registrar(datosAvance(horas, nota));
  if (estadoActual === nuevoEstado) return { registro, tarea: null, errorEstado: null };
  try {
    const tarea = await cambiarEstado(nuevoEstado);
    return { registro, tarea, errorEstado: null };
  } catch (errorEstado) {
    return { registro, tarea: null, errorEstado };
  }
}