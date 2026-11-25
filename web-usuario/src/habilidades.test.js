import assert from 'node:assert/strict';
import test from 'node:test';
import { agruparHabilidades, etiquetaNivel, nivelValido, ordenarHabilidades, rutaIconoHabilidad } from './habilidades.js';

const catalogo = [
  { id: 1, nombre: 'React', area: 'Frontend' },
  { id: 2, nombre: 'Gestion de proyectos', area: 'Gestion' },
  { id: 3, nombre: 'Analisis', area: 'Gestion' },
  { id: 4, nombre: 'Otras competencias', area: null },
];

test('agrupa por áreas reales e incluye habilidades sin área en General', () => {
  assert.deepEqual(agruparHabilidades(catalogo).map(([area, habilidades]) => [area, habilidades.map(({ id }) => id)]), [
    ['Frontend', [1]], ['General', [4]], ['Gestion', [3, 2]],
  ]);
});

test('combina el filtro de área y la búsqueda por nombre o área sin distinguir tildes ni mayúsculas', () => {
  assert.deepEqual(agruparHabilidades(catalogo, ' ANÁLISIS ', 'Gestion'), [['Gestion', [catalogo[2]]]]);
  assert.deepEqual(agruparHabilidades(catalogo, 'front'), [['Frontend', [catalogo[0]]]]);
  assert.deepEqual(agruparHabilidades(catalogo, 'react', 'Gestion'), []);
  assert.deepEqual(agruparHabilidades([]), []);
});

test('ordena sin modificar la respuesta del servidor', () => {
  const original = catalogo.map(({ id }) => id);
  assert.deepEqual(ordenarHabilidades(catalogo).map(({ id }) => id), [3, 2, 4, 1]);
  assert.deepEqual(catalogo.map(({ id }) => id), original);
});

test('muestra los niveles oficiales, los datos históricos y la ausencia de nivel', () => {
  assert.equal(etiquetaNivel('BASICO'), 'Básico');
  assert.equal(etiquetaNivel('INTERMEDIO'), 'Intermedio');
  assert.equal(etiquetaNivel('AVANZADO'), 'Avanzado');
  assert.equal(etiquetaNivel('  Nivel histórico '), 'Nivel histórico');
  assert.equal(etiquetaNivel(null), 'Sin nivel');
  assert.equal(nivelValido(''), false);
  assert.equal(nivelValido('toString'), false);
});

test('solo construye rutas de SVG para identificadores de icono válidos', () => {
  assert.equal(rutaIconoHabilidad('spring'), '/iconos/devicon/spring.svg');
  assert.equal(rutaIconoHabilidad(null), null);
  assert.equal(rutaIconoHabilidad('../spring'), null);
  assert.equal(rutaIconoHabilidad('https://example.test/icon.svg'), null);
});