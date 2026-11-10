import assert from 'node:assert/strict';
import test from 'node:test';
import { datosAvance, guardarAvanceConEstado, permiteEstadoAvance, resumenTiempo, validarAvance } from './avances.js';

const formulario = { horas: 2.5, nota: 'Implementación de validaciones y pruebas unitarias.', estadoActual: 'EN_PROGRESO', nuevoEstado: 'COMPLETADA' };

test('convierte horas a minutos y envía únicamente el contrato del backend', () => {
  assert.deepEqual(datosAvance(2.5, '  Trabajo realizado\ncon pruebas.  '), { minutos: 150, nota: 'Trabajo realizado\ncon pruebas.' });
  assert.deepEqual(datosAvance(0.5, 'Nota'), { minutos: 30, nota: 'Nota' });
});

test('acepta los límites y los intervalos de media hora del formulario', () => {
  for (const horas of [0.5, 1, 2.5, 4, 6, 16]) {
    assert.deepEqual(validarAvance({ ...formulario, horas }), {});
  }
});

test('rechaza tiempo vacío, no numérico, fuera de rango o sin intervalos de 30 minutos', () => {
  for (const horas of ['', null, undefined, NaN, Infinity, '2.5', 0, -1, 0.25, 1.1, 16.5]) {
    assert.ok(validarAvance({ ...formulario, horas }).horas, `Debe rechazar ${horas}`);
  }
});

test('valida descripción de 20 a 1000 caracteres sin contar espacios de los extremos', () => {
  for (const nota of ['', ' '.repeat(30), 'a'.repeat(19), 'a'.repeat(1001), null]) {
    assert.ok(validarAvance({ ...formulario, nota }).nota);
  }
  for (const nota of ['a'.repeat(20), 'a'.repeat(1000), `  ${'a'.repeat(20)}  `]) {
    assert.deepEqual(validarAvance({ ...formulario, nota }), {});
  }
});

test('ofrece únicamente el estado actual y las transiciones manuales admitidas por el backend', () => {
  const permitidos = {
    PENDIENTE: ['PENDIENTE', 'EN_PROGRESO'],
    EN_PROGRESO: ['PENDIENTE', 'EN_PROGRESO', 'COMPLETADA'],
    BLOQUEADA: ['BLOQUEADA'],
    COMPLETADA: ['COMPLETADA'],
  };
  for (const actual of Object.keys(permitidos)) {
    for (const nuevo of [...Object.keys(permitidos), 'OTRO']) {
      assert.equal(permiteEstadoAvance(actual, nuevo), permitidos[actual].includes(nuevo));
    }
  }
  assert.equal(permiteEstadoAvance('OTRO', 'OTRO'), false);
  assert.ok(validarAvance({ ...formulario, estadoActual: 'BLOQUEADA' }).estado);
});

test('proyecta tiempo real sin inferir horas estimadas ni porcentajes de completitud', () => {
  assert.deepEqual(resumenTiempo(1440, 4), { acumuladas: 24, sesion: 4, proyectadas: 28 });
  assert.deepEqual(resumenTiempo(90, 0), { acumuladas: 1.5, sesion: 0, proyectadas: 1.5 });
  assert.deepEqual(resumenTiempo(90, ''), { acumuladas: 1.5, sesion: 0, proyectadas: 1.5 });
});

test('guarda el tiempo antes del estado y conserva la respuesta real del registro', async () => {
  const llamadas = [];
  const registro = { id: 5, minutos: 150 };
  const tarea = { estado: 'COMPLETADA' };
  const resultado = await guardarAvanceConEstado({
    registrar: async (datos) => { llamadas.push(['registro', datos]); return registro; },
    cambiarEstado: async (estado) => { llamadas.push(['estado', estado]); return tarea; },
  }, formulario);
  assert.deepEqual(llamadas, [['registro', datosAvance(formulario.horas, formulario.nota)], ['estado', 'COMPLETADA']]);
  assert.deepEqual(resultado, { registro, tarea, errorEstado: null });
});

test('no realiza PUT cuando solo se registra tiempo sin cambiar el estado', async () => {
  const resultado = await guardarAvanceConEstado({
    registrar: async () => ({ id: 6 }),
    cambiarEstado: async () => assert.fail('No debe cambiar el estado'),
  }, { ...formulario, nuevoEstado: 'EN_PROGRESO' });
  assert.deepEqual(resultado, { registro: { id: 6 }, tarea: null, errorEstado: null });
});

test('si falla el registro no cambia el estado ni reintenta automáticamente el POST', async () => {
  let intentos = 0;
  await assert.rejects(guardarAvanceConEstado({
    registrar: async () => { intentos += 1; throw new Error('Sin conexión'); },
    cambiarEstado: async () => assert.fail('No debe cambiar el estado'),
  }, formulario), /Sin conexión/);
  assert.equal(intentos, 1);
});

test('si falla el estado devuelve el registro ya guardado sin duplicar tiempo', async () => {
  let intentos = 0;
  const errorEstado = new Error('Conflicto de estado');
  const resultado = await guardarAvanceConEstado({
    registrar: async () => { intentos += 1; return { id: 7, minutos: 150 }; },
    cambiarEstado: async () => { throw errorEstado; },
  }, formulario);
  assert.equal(intentos, 1);
  assert.deepEqual(resultado, { registro: { id: 7, minutos: 150 }, tarea: null, errorEstado });
});

test('una validación fallida no envía ninguna operación al backend', async () => {
  await assert.rejects(guardarAvanceConEstado({
    registrar: async () => assert.fail('No debe registrar tiempo'),
    cambiarEstado: async () => assert.fail('No debe cambiar el estado'),
  }, { ...formulario, horas: 0 }), /intervalos de 30 minutos/);
});