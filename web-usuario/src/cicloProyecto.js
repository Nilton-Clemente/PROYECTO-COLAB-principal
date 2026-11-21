export const ESTADOS_PROYECTO = Object.freeze({
  SIN_PLANIFICACION: { etiqueta: 'Sin planificación', color: 'gris' },
  PLANIFICACION_PROPUESTA: { etiqueta: 'Planificación propuesta', color: 'ambar' },
  EN_DESARROLLO: { etiqueta: 'En desarrollo', color: 'azul' },
  FINALIZADO: { etiqueta: 'Finalizado', color: 'verde' },
  CANCELADO: { etiqueta: 'Cancelado', color: 'rojo' },
});

export function estadoProyecto(estado) {
  return Object.hasOwn(ESTADOS_PROYECTO, estado)
    ? ESTADOS_PROYECTO[estado]
    : { etiqueta: 'Estado no disponible', color: 'gris' };
}

export const proyectoCerrado = (estado) => estado === 'FINALIZADO' || estado === 'CANCELADO';

export function accionesProyecto(proyecto, soyCreador) {
  const acciones = [];
  if (soyCreador && proyecto.estado === 'SIN_PLANIFICACION') acciones.push('editar');
  if (proyecto.puedeCancelar === true) acciones.push('cancelar');
  if (proyecto.puedeEliminar === true) acciones.push('eliminar');
  return acciones;
}

export function coincideProyecto(proyecto, { rol, estado, busqueda }) {
  const termino = busqueda.trim().toLowerCase();
  return (rol === 'todos' || proyecto.miRol === rol)
    && (!estado || proyecto.estado === estado)
    && (!termino || proyecto.nombre.toLowerCase().includes(termino)
      || proyecto.tecnologias.some((t) => t.nombre.toLowerCase().includes(termino)));
}

export function confirmaNombreProyecto(nombre, texto) {
  return nombre.trim().length > 0 && nombre.trim() === texto.trim();
}