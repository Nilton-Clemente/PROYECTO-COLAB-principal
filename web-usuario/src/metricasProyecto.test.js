import assert from 'node:assert/strict';
import test from 'node:test';
import { avancePorIntegrante, distribucionTareas, entregaVencida, mostrarPorcentaje, porcentajeTareas } from './metricasProyecto.js';

test('calcula la distribución a partir de las cuatro cantidades de la API', () => {
  const estados = distribucionTareas({ totalTareas: 8, completadas: 4, enProgreso: 2, pendientes: 1, bloqueadas: 1 });
  assert.deepEqual(estados.map(({ campo, cantidad, porcentaje }) => [campo, cantidad, porcentaje]), [
    ['completadas', 4, 50], ['enProgreso', 2, 25], ['pendientes', 1, 12.5], ['bloqueadas', 1, 12.5],
  ]);
});

test('distingue un total vacío de un proyecto con tareas pero sin avances', () => {
  assert.equal(porcentajeTareas(0, 0), null);
  assert.equal(porcentajeTareas(0, 3), 0);
  assert.equal(mostrarPorcentaje(null), '—');
  assert.match(mostrarPorcentaje(0), /^0\s*%$/u);
});

test('no redondea al 100 % cuando aún quedan tareas por completar', () => {
  assert.match(mostrarPorcentaje(99.99), /^< 100\s*%$/u);
  assert.match(mostrarPorcentaje(100), /^100\s*%$/u);
  assert.match(mostrarPorcentaje(0.01), /^< 0[.,]1\s*%$/u);
});

test('une los responsables con su nombre y rol por ID y no inventa avance sin asignaciones', () => {
  const responsables = [
    { integranteId: 1, email: 'uno@example.test', totalTareas: 3, completadas: 2 },
    { integranteId: 2, email: 'dos@example.test', totalTareas: 0, completadas: 0 },
  ];
  const equipo = [{ integranteId: 1, nombre: 'Primer integrante', rol: 'CREADOR' }];
  const resultado = avancePorIntegrante(responsables, equipo);
  assert.equal(resultado[0].nombre, 'Primer integrante');
  assert.equal(resultado[0].rol, 'CREADOR');
  assert.equal(resultado[0].porcentaje, porcentajeTareas(2, 3));
  assert.equal(resultado[1].nombre, 'dos@example.test');
  assert.equal(resultado[1].porcentaje, null);
});

test('solo marca vencida una fecha anterior a hoy si el proyecto sigue en desarrollo', () => {
  const hoy = new Date(2026, 10, 21, 23, 59);
  const estado = 'EN_DESARROLLO';
  assert.equal(entregaVencida({ estado, fechaEntrega: '2026-11-20' }, hoy), true);
  assert.equal(entregaVencida({ estado, fechaEntrega: '2026-11-21' }, hoy), false);
  assert.equal(entregaVencida({ estado, fechaEntrega: '2026-11-22' }, hoy), false);
  assert.equal(entregaVencida({ estado, fechaEntrega: null }, hoy), false);
  for (const cerrado of ['CANCELADO', 'FINALIZADO', 'PLANIFICACION_PROPUESTA']) {
    assert.equal(entregaVencida({ estado: cerrado, fechaEntrega: '2026-11-20' }, hoy), false);
  }
});