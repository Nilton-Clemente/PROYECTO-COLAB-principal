import assert from 'node:assert/strict';
import test from 'node:test';
import { accionesProyecto, coincideProyecto, confirmaNombreProyecto, estadoProyecto, ESTADOS_PROYECTO, proyectoCerrado } from './cicloProyecto.js';

test('traduce los cinco estados del servidor y no supone que un estado ausente sea un borrador', () => {
  assert.deepEqual(Object.keys(ESTADOS_PROYECTO).map((valor) => estadoProyecto(valor).etiqueta),
    ['Sin planificación', 'Planificación propuesta', 'En desarrollo', 'Finalizado', 'Cancelado']);
  assert.equal(estadoProyecto(undefined).etiqueta, 'Estado no disponible');
});

test('ofrece edición solo al creador sin planificación y respeta los permisos de la API', () => {
  assert.deepEqual(accionesProyecto({ estado: 'SIN_PLANIFICACION', puedeEliminar: true }, true), ['editar', 'eliminar']);
  assert.deepEqual(accionesProyecto({ estado: 'PLANIFICACION_PROPUESTA', puedeEliminar: true }, true), ['eliminar']);
  assert.deepEqual(accionesProyecto({ estado: 'EN_DESARROLLO', puedeCancelar: true }, true), ['cancelar']);
  assert.deepEqual(accionesProyecto({ estado: 'EN_DESARROLLO', puedeCancelar: false }, true), []);
  assert.deepEqual(accionesProyecto({ estado: 'FINALIZADO', puedeEliminar: true }, true), ['eliminar']);
  assert.deepEqual(accionesProyecto({ estado: 'CANCELADO', puedeEliminar: true }, true), ['eliminar']);
  assert.deepEqual(accionesProyecto({ estado: 'SIN_PLANIFICACION' }, false), []);
  assert.deepEqual(accionesProyecto({ estado: 'CANCELADO' }, false), []);
});

test('combina los filtros por estado, rol y búsqueda por nombre o tecnología', () => {
  const proyecto = { estado: 'CANCELADO', miRol: 'CREADOR', nombre: 'Agenda', tecnologias: [{ nombre: 'React' }] };
  assert.equal(coincideProyecto(proyecto, { rol: 'todos', estado: '', busqueda: '' }), true);
  assert.equal(coincideProyecto(proyecto, { rol: 'CREADOR', estado: 'CANCELADO', busqueda: ' react ' }), true);
  assert.equal(coincideProyecto(proyecto, { rol: 'todos', estado: 'CANCELADO', busqueda: 'AGE' }), true);
  assert.equal(coincideProyecto(proyecto, { rol: 'INTEGRANTE', estado: 'CANCELADO', busqueda: 'react' }), false);
  assert.equal(coincideProyecto(proyecto, { rol: 'CREADOR', estado: 'FINALIZADO', busqueda: 'react' }), false);
  assert.equal(coincideProyecto(proyecto, { rol: 'CREADOR', estado: 'CANCELADO', busqueda: 'java' }), false);
});

test('requiere escribir el nombre exacto, ignorando únicamente los espacios exteriores', () => {
  assert.equal(confirmaNombreProyecto('Colab TECSUP', ' Colab TECSUP '), true);
  assert.equal(confirmaNombreProyecto('Colab TECSUP', 'colab tecsup'), false);
  assert.equal(confirmaNombreProyecto('Colab TECSUP', 'Colab'), false);
  assert.equal(confirmaNombreProyecto('', ''), false);
});

test('solo da por cerrado un proyecto finalizado o cancelado por el servidor', () => {
  assert.deepEqual(Object.keys(ESTADOS_PROYECTO).filter(proyectoCerrado), ['FINALIZADO', 'CANCELADO']);
});