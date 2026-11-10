import assert from 'node:assert/strict';
import test from 'node:test';
import { puedeRegistrarAvance } from './tareas.js';

test('no permite registrar avances mientras la planificación no esté activa', () => {
  for (const estadoPlan of [undefined, null, 'BORRADOR', 'PROPUESTA', 'FINALIZADA']) {
    for (const estado of ['PENDIENTE', 'EN_PROGRESO', 'BLOQUEADA', 'COMPLETADA']) {
      assert.equal(puedeRegistrarAvance(estadoPlan, { estado }), false);
    }
  }
});

test('permite registrar avances en tareas no completadas de una planificación activa', () => {
  for (const estado of ['PENDIENTE', 'EN_PROGRESO', 'BLOQUEADA']) {
    assert.equal(puedeRegistrarAvance('ACTIVA', { estado }), true);
  }
});

test('no permite registrar avances en tareas completadas aunque la planificación esté activa', () => {
  assert.equal(puedeRegistrarAvance('ACTIVA', { estado: 'COMPLETADA' }), false);
});