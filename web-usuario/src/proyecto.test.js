import assert from 'node:assert/strict';
import test from 'node:test';
import { casoUsoFormulario, casoUsoSinNombre, datosProyecto, DETALLES_CASO_USO } from './proyecto.js';

const camposDetalle = ['actores', 'precondiciones', 'disparador', 'flujoPrincipal',
  'flujosAlternativos', 'excepciones', 'postcondiciones', 'reglasDeNegocio'];

function enviar(cambios = {}) {
  return datosProyecto({ datos: { nombre: ' Proyecto ' }, rf: [], rnf: [], cu: [], seleccion: new Set(), ...cambios });
}

test('el formulario incluye exactamente los ocho detalles del backend', () => {
  assert.deepEqual(DETALLES_CASO_USO.map(({ campo }) => campo), camposDetalle);
  assert.deepEqual(Object.keys(casoUsoFormulario()), ['nombre', 'descripcion', ...camposDetalle]);
  assert.ok(Object.values(casoUsoFormulario()).every((valor) => valor === ''));
});

test('admite casos anteriores con solo nombre y campos null', () => {
  const formulario = casoUsoFormulario({ nombre: 'Registrar usuario', descripcion: null, actores: null });
  assert.equal(formulario.nombre, 'Registrar usuario');
  assert.ok(Object.entries(formulario).filter(([campo]) => campo !== 'nombre').every(([, valor]) => valor === ''));
  const caso = enviar({ cu: [formulario] }).casosDeUso[0];
  assert.equal(caso.nombre, 'Registrar usuario');
  assert.ok(Object.entries(caso).filter(([campo]) => campo !== 'nombre').every(([, valor]) => valor === null));
});

test('crear envía todos los detalles como textos y conserva numeración y saltos de línea', () => {
  const caso = casoUsoFormulario({ nombre: ' Invitar integrante ', descripcion: ' Invitar al equipo ',
    ...Object.fromEntries(camposDetalle.map((campo) => [campo, ' Texto: ' + campo + ' '])),
    flujoPrincipal: '  1. Validar permisos.\n2. Registrar invitación.  ' });
  const enviado = enviar({ cu: [caso] }).casosDeUso[0];
  assert.equal(enviado.nombre, 'Invitar integrante');
  assert.equal(enviado.descripcion, 'Invitar al equipo');
  assert.equal(enviado.flujoPrincipal, '1. Validar permisos.\n2. Registrar invitación.');
  for (const campo of camposDetalle.filter((c) => c !== 'flujoPrincipal')) {
    assert.equal(enviado[campo], 'Texto: ' + campo);
  }
});

test('editar solo el nombre del proyecto conserva todos los detalles recibidos', () => {
  const recibido = { nombre: 'Invitar integrante', descripcion: 'Invitar al equipo',
    ...Object.fromEntries(camposDetalle.map((campo) => [campo, '1. Paso para ' + campo + '\n2. Resultado.'])) };
  const formulario = casoUsoFormulario(recibido);
  const enviado = enviar({ datos: { nombre: 'Proyecto renombrado' }, cu: [formulario] });
  assert.equal(enviado.nombre, 'Proyecto renombrado');
  assert.deepEqual(enviado.casosDeUso, [recibido]);
  assert.equal(formulario.flujoPrincipal, recibido.flujoPrincipal);
});

test('vaciar detalles envía null para permitir el borrado intencional', () => {
  const caso = casoUsoFormulario({ nombre: 'Caso', actores: 'Usuario', flujoPrincipal: '1. Acción' });
  caso.actores = '';
  caso.flujoPrincipal = ' \n ';
  const enviado = enviar({ cu: [caso] }).casosDeUso[0];
  assert.equal(enviado.actores, null);
  assert.equal(enviado.flujoPrincipal, null);
});

test('no envía codigo ni propiedades ajenas al contrato en RF, RNF o casos de uso', () => {
  const enviado = enviar({
    rf: [{ codigo: 'RF-01', descripcion: ' Registrar usuario ' }],
    rnf: [{ codigo: 'RNF-01', descripcion: ' Respuesta rápida ', categoria: ' ' }],
    cu: [casoUsoFormulario({ codigo: 'CU-01', id: 42, nombre: 'Registrar usuario' })],
  });
  assert.deepEqual(enviado.requerimientosFuncionales, [{ descripcion: 'Registrar usuario' }]);
  assert.deepEqual(enviado.requerimientosNoFuncionales, [{ descripcion: 'Respuesta rápida', categoria: null }]);
  assert.ok(!JSON.stringify(enviado).includes('codigo'));
  assert.ok(!('id' in enviado.casosDeUso[0]));
});

test('permite proyectos sin casos y omite filas totalmente vacías', () => {
  const vacio = casoUsoFormulario();
  assert.equal(casoUsoSinNombre([vacio]), -1);
  assert.deepEqual(enviar({ cu: [vacio], rf: [{ descripcion: ' ' }], rnf: [{ descripcion: '' }] }).casosDeUso, []);
  assert.deepEqual(enviar().casosDeUso, []);
});

test('detecta cada campo con datos sin nombre para no descartarlos silenciosamente', () => {
  for (const campo of ['descripcion', ...camposDetalle]) {
    assert.equal(casoUsoSinNombre([casoUsoFormulario(), casoUsoFormulario({ [campo]: 'Dato importante' })]), 1);
  }
  assert.equal(casoUsoSinNombre([casoUsoFormulario({ nombre: 'Válido', actores: 'Usuario' })]), -1);
});

test('mantiene los otros datos del proyecto y la selección de tecnologías', () => {
  const enviado = enviar({ datos: { nombre: 'Proyecto', descripcion: ' Descripción ', problematica: 'Problema',
    objetivoGeneral: 'Objetivo', objetivosEspecificos: '1. Uno\n2. Dos', alcance: 'Alcance',
    restricciones: 'Restricciones', fecha: '2026-12-01' }, seleccion: new Set([3, 8]) });
  assert.deepEqual(enviado, { nombre: 'Proyecto', descripcion: 'Descripción', problematica: 'Problema',
    objetivoGeneral: 'Objetivo', objetivosEspecificos: '1. Uno\n2. Dos', alcance: 'Alcance',
    restricciones: 'Restricciones', fechaEntrega: '2026-12-01', tecnologiasIds: [3, 8],
    requerimientosFuncionales: [], requerimientosNoFuncionales: [], casosDeUso: [] });
});