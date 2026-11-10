export function puedeRegistrarAvance(estadoPlan, tarea) {
  return estadoPlan === 'ACTIVA' && tarea.estado !== 'COMPLETADA';
}