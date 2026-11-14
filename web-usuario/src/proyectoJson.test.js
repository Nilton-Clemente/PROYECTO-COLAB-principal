import assert from 'node:assert/strict';
import test from 'node:test';
import { EJEMPLO_PROYECTO_JSON, leerProyectoJson } from './proyectoJson.js';

const leer = (valor, catalogo = []) => leerProyectoJson(JSON.stringify(valor), catalogo);

test('JSON mínimo crea un proyecto con solo nombre y listas vacías', () => {
  const datos = leer({ nombre: ' Proyecto rápido ' });
  assert.equal(datos.nombre, 'Proyecto rápido');
  assert.equal(datos.fechaEntrega, null);
  assert.equal(datos.descripcion, null);
  for (const campo of ['tecnologiasIds', 'requerimientosFuncionales', 'requerimientosNoFuncionales', 'casosDeUso']) {
    assert.deepEqual(datos[campo], []);
  }
});

test('la plantilla es válida y conserva todos los detalles y saltos de línea', () => {
  const original = JSON.stringify(EJEMPLO_PROYECTO_JSON);
  const datos = leerProyectoJson(original, []);
  assert.deepEqual(datos, EJEMPLO_PROYECTO_JSON);
  assert.equal(datos.casosDeUso[0].flujoPrincipal.split('\n').length, 3);
  assert.equal(JSON.stringify(EJEMPLO_PROYECTO_JSON), original);
});

test('permite campos opcionales null y casos antiguos con solo nombre', () => {
  const datos = leer({ nombre: 'Proyecto', tecnologiasIds: null, requerimientosFuncionales: null,
    requerimientosNoFuncionales: [{ descripcion: ' Seguro ', categoria: null }],
    casosDeUso: [{ nombre: 'Caso básico', descripcion: null }] });
  assert.deepEqual(datos.tecnologiasIds, []);
  assert.equal(datos.requerimientosNoFuncionales[0].categoria, null);
  assert.equal(datos.casosDeUso[0].actores, null);
});

test('rechaza JSON inválido, comentarios y bloques markdown', () => {
  for (const contenido of ['', '{', '{"nombre":"Proyecto",}', '```json\n{"nombre":"Proyecto"}\n```', '// comentario\n{}']) {
    assert.throws(() => leerProyectoJson(contenido), /JSON inválido/);
  }
});

test('la raíz debe ser un objeto y el nombre debe ser texto no vacío', () => {
  for (const valor of [null, [], 42, 'Proyecto']) assert.throws(() => leer(valor), /objeto JSON/);
  for (const nombre of [undefined, null, '', '  ']) assert.throws(() => leer({ nombre }), /nombre es obligatorio/);
  for (const nombre of [true, 12, [], {}]) assert.throws(() => leer({ nombre }), /nombre debe ser texto/);
});

test('los campos de texto no admiten arrays ni otros tipos', () => {
  for (const campo of ['descripcion', 'problematica', 'objetivoGeneral', 'objetivosEspecificos', 'alcance', 'restricciones']) {
    assert.throws(() => leer({ nombre: 'Proyecto', [campo]: ['Paso 1'] }), new RegExp(campo + ' debe ser texto'));
  }
  assert.throws(() => leer({ nombre: 'Proyecto', casosDeUso: [{ nombre: 'Caso', flujoPrincipal: ['Paso'] }] }), /casosDeUso\[0\].flujoPrincipal/);
});

test('rechaza campos desconocidos y códigos antiguos sin descartarlos silenciosamente', () => {
  for (const campo of ['id', 'tipo', 'tecnologias', 'objetivoGenerall', '__proto__']) {
    assert.throws(() => leer(JSON.parse(`{"nombre":"Proyecto","${campo}":"valor"}`)), /campo no admitido/);
  }
  for (const campo of ['requerimientosFuncionales', 'requerimientosNoFuncionales', 'casosDeUso']) {
    assert.throws(() => leer({ nombre: 'Proyecto', [campo]: [{ codigo: '01', descripcion: 'Dato', nombre: 'Caso' }] }), /campo no admitido/);
  }
});

test('valida arrays y cada elemento con una ruta precisa', () => {
  for (const campo of ['tecnologiasIds', 'requerimientosFuncionales', 'requerimientosNoFuncionales', 'casosDeUso']) {
    assert.throws(() => leer({ nombre: 'Proyecto', [campo]: {} }), new RegExp(campo + ' debe ser un array'));
  }
  for (const campo of ['requerimientosFuncionales', 'requerimientosNoFuncionales', 'casosDeUso']) {
    assert.throws(() => leer({ nombre: 'Proyecto', [campo]: [null] }), /\[0\] debe ser un objeto JSON/);
    assert.throws(() => leer({ nombre: 'Proyecto', [campo]: [{}] }), /\[0\]\..*es obligatorio/);
  }
});

test('valida los IDs de tecnologías contra el catálogo y elimina duplicados', () => {
  const datos = leer({ nombre: 'Proyecto', tecnologiasIds: [2, 2, 5] }, [{ id: 2 }, { id: 5 }]);
  assert.deepEqual(datos.tecnologiasIds, [2, 5]);
  for (const id of ['2', 0, -1, 1.5, null, Number.MAX_SAFE_INTEGER + 1]) {
    assert.throws(() => leer({ nombre: 'Proyecto', tecnologiasIds: [id] }), /ID numérico entero positivo/);
  }
  assert.throws(() => leer({ nombre: 'Proyecto', tecnologiasIds: [9] }), /ID 9 no existe/);
  assert.throws(() => leerProyectoJson('{"nombre":"Proyecto","tecnologiasIds":[2]}'), /Carga el catálogo/);
  assert.equal(leerProyectoJson('{"nombre":"Proyecto"}').nombre, 'Proyecto');
});

test('valida fechas reales sin depender del huso horario', () => {
  for (const fechaEntrega of ['2028-02-29', '2026-11-30', '2000-02-29', '0001-01-01']) {
    assert.equal(leer({ nombre: 'Proyecto', fechaEntrega }).fechaEntrega, fechaEntrega);
  }
  for (const fechaEntrega of ['2026-02-29', '1900-02-29', '2026-04-31', '2026-00-01', '2026-13-01', '2026-11-00', '0000-01-01']) {
    assert.throws(() => leer({ nombre: 'Proyecto', fechaEntrega }), /fecha válida/);
  }
  for (const fechaEntrega of ['', '10/11/2026', '2026-1-01', '2026-11-10T00:00:00Z', 123]) {
    assert.throws(() => leer({ nombre: 'Proyecto', fechaEntrega }), /YYYY-MM-DD/);
  }
});