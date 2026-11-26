import assert from 'node:assert/strict';
import { after, before, test } from 'node:test';
import { fileURLToPath } from 'node:url';
import { createElement } from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { StaticRouter } from 'react-router-dom/server.js';
import { createServer } from 'vite';
import {
  crearBandejaNotificaciones, fechaNotificacion, proyectoNotificacion,
  tipoNotificacion, validarBandeja, validarContador,
} from './notificaciones.js';

const aviso = (id, leida = false, extra = {}) => ({
  id, leida, mensaje: 'Primera línea\nSegunda línea', tipoEvento: 'PLANIFICACION',
  fechaCreacion: '2026-11-26T10:15:00', proyectoId: 7, proyectoNombre: 'Matrícula', proyectoCodigo: 'TEC-7',
  ...extra,
});
const pendiente = () => {
  let resolve;
  let reject;
  const promise = new Promise((ok, error) => { resolve = ok; reject = error; });
  return { promise, resolve, reject };
};
function preparar(cambios = {}) {
  return crearBandejaNotificaciones({
    listar: async () => [aviso(2, true), aviso(1)],
    contar: async () => ({ noLeidas: 1 }),
    marcarLeida: async (id) => aviso(id, true),
    eliminarLeidas: async () => null,
    ...cambios,
  });
}

test('proyecto: usa solo los snapshots, sin extraer asociaciones del mensaje', () => {
  assert.equal(proyectoNotificacion(aviso(1)), 'Matrícula · TEC-7');
  assert.equal(proyectoNotificacion({ proyectoId: 3 }), 'TEC-3');
  assert.equal(proyectoNotificacion({ proyectoNombre: 'Solo nombre' }), 'Solo nombre');
  assert.equal(proyectoNotificacion({ mensaje: 'Proyecto Agenda (TEC-99)' }), 'Proyecto no identificado');
  assert.equal(proyectoNotificacion({ proyectoNombre: ' ', proyectoCodigo: '' }), 'Proyecto no identificado');
});

test('fecha: conserva la hora LocalDateTime sin adivinar la zona', () => {
  assert.equal(fechaNotificacion('2026-11-26T23:59:40.123456'), '26/11/2026 23:59:40');
  assert.equal(fechaNotificacion(null), 'Fecha no disponible');
  assert.equal(fechaNotificacion('2026-11-26T10:15:00Z'), '2026-11-26T10:15:00Z');
});

test('traduce todos los tipos emitidos y tolera nuevos tipos', () => {
  for (const tipo of ['PLANIFICACION', 'ASIGNACION', 'CAMBIO_ESTADO', 'CAMBIO_RESPONSABLE', 'DESBLOQUEO']) {
    assert.notEqual(tipoNotificacion(tipo), 'Aviso del proyecto');
  }
  assert.equal(tipoNotificacion('FUTURO'), 'Aviso del proyecto');
});

test('rechaza listas o contadores incompletos sin convertir errores en bandejas vacías', () => {
  for (const datos of [null, {}, [aviso(1), aviso(1)], [{ id: 1 }], [aviso(0)]]) {
    assert.throws(() => validarBandeja(datos));
  }
  for (const datos of [null, {}, { noLeidas: -1 }, { noLeidas: '2' }]) {
    assert.throws(() => validarContador(datos));
  }
  assert.equal(validarContador({ noLeidas: 0 }), 0);
});

test('al cargar mantiene orden y lectura; notifica y permite cancelar la suscripción', async () => {
  const bandeja = preparar();
  let cambios = 0;
  const cancelar = bandeja.subscribe(() => { cambios++; });
  await bandeja.cargar();
  assert.deepEqual(bandeja.getSnapshot().lista.map((n) => n.id), [2, 1]);
  assert.equal(bandeja.getSnapshot().noLeidas, 1);
  assert.equal(bandeja.getSnapshot().lista[1].leida, false);
  assert.ok(cambios > 0);
  cancelar();
  const antes = cambios;
  await bandeja.cargar();
  assert.equal(cambios, antes);
});

test('no marca ni baja el contador hasta que el PUT confirme; bloquea doble envío', async () => {
  const respuesta = pendiente();
  let llamadas = 0;
  const bandeja = preparar({ marcarLeida: () => { llamadas++; return respuesta.promise; } });
  await bandeja.cargar();
  const guardado = bandeja.marcarLeida(1);
  await bandeja.marcarLeida(1);
  assert.equal(llamadas, 1);
  assert.equal(bandeja.getSnapshot().noLeidas, 1);
  assert.equal(bandeja.getSnapshot().lista[1].leida, false);
  respuesta.resolve(aviso(1, true));
  await guardado;
  assert.equal(bandeja.getSnapshot().noLeidas, 0);
  assert.equal(bandeja.getSnapshot().lista[1].leida, true);
  assert.equal(bandeja.getSnapshot().operacion, null);
});

test('si el PUT no confirma la lectura vuelve a consultar en vez de fingir éxito', async () => {
  const bandeja = preparar({ marcarLeida: async () => aviso(1, false) });
  await bandeja.cargar();
  await bandeja.marcarLeida(1);
  assert.match(bandeja.getSnapshot().error, /no confirmó/);
  assert.equal(bandeja.getSnapshot().noLeidas, 1);
});

test('DELETE: no oculta leídas antes de confirmar ni permite repetir mientras está pendiente', async () => {
  const respuesta = pendiente();
  let borrados = 0;
  let listas = 0;
  const bandeja = preparar({
    listar: async () => ++listas === 1 ? [aviso(2, true), aviso(1)] : [aviso(1)],
    eliminarLeidas: () => { borrados++; return respuesta.promise; },
  });
  await bandeja.cargar();
  const borrado = bandeja.eliminarLeidas();
  await bandeja.eliminarLeidas();
  await bandeja.marcarLeida(1);
  assert.equal(borrados, 1);
  assert.equal(bandeja.getSnapshot().lista.length, 2);
  respuesta.resolve(null);
  await borrado;
  assert.deepEqual(bandeja.getSnapshot().lista, [aviso(1)]);
  assert.equal(bandeja.getSnapshot().noLeidas, 1);
});

test('DELETE con respuesta perdida: consulta una vez, sin repetir el borrado', async () => {
  let listas = 0;
  let borrados = 0;
  const bandeja = preparar({
    listar: async () => ++listas === 1 ? [aviso(2, true), aviso(1)] : [aviso(1)],
    eliminarLeidas: async () => { borrados++; throw new Error('Conexión perdida'); },
  });
  await bandeja.cargar();
  await bandeja.eliminarLeidas();
  assert.equal(borrados, 1);
  assert.equal(listas, 2);
  assert.deepEqual(bandeja.getSnapshot().lista, [aviso(1)]);
  assert.match(bandeja.getSnapshot().error, /Conexión perdida/);
});

test('fallo de DELETE y de recarga conserva la lista y bloquea cambios hasta recuperarse', async () => {
  let recuperado = false;
  let listas = 0;
  let borrados = 0;
  const bandeja = preparar({
    listar: async () => {
      if (++listas > 1 && !recuperado) throw new Error('Sin red');
      return [aviso(2, true), aviso(1)];
    },
    eliminarLeidas: async () => { borrados++; throw new Error('Sin confirmación'); },
  });
  await bandeja.cargar();
  await bandeja.eliminarLeidas();
  assert.equal(bandeja.getSnapshot().lista.length, 2);
  assert.equal(bandeja.getSnapshot().desactualizado, true);
  assert.equal(bandeja.getSnapshot().contadorError, true);
  await bandeja.eliminarLeidas();
  await bandeja.marcarLeida(1);
  assert.equal(borrados, 1);
  recuperado = true;
  await bandeja.cargar();
  assert.equal(bandeja.getSnapshot().desactualizado, false);
  assert.equal(bandeja.getSnapshot().error, '');
});

test('DELETE confirmado no resucita leídas si falla la recarga posterior', async () => {
  let listas = 0;
  const bandeja = preparar({ listar: async () => {
    if (++listas > 1) throw new Error('Sin red');
    return [aviso(2, true), aviso(1)];
  } });
  await bandeja.cargar();
  await bandeja.eliminarLeidas();
  assert.deepEqual(bandeja.getSnapshot().lista, [aviso(1)]);
  assert.equal(bandeja.getSnapshot().desactualizado, true);
});

test('un contador lento no sobrescribe la lista ni el contador tras una mutación', async () => {
  const respuesta = pendiente();
  const bandeja = preparar({ contar: () => respuesta.promise });
  const conteo = bandeja.refrescarContador();
  await bandeja.cargar();
  await bandeja.marcarLeida(1);
  respuesta.resolve({ noLeidas: 90 });
  await conteo;
  assert.equal(bandeja.getSnapshot().noLeidas, 0);
});

test('un fallo del contador no simula cero y no modifica los datos de otra sesión', async () => {
  let falla = false;
  const bandeja = preparar({ contar: async () => {
    if (falla) throw new Error('Error');
    return { noLeidas: 3 };
  } });
  await bandeja.refrescarContador();
  falla = true;
  await bandeja.refrescarContador();
  assert.equal(bandeja.getSnapshot().noLeidas, 3);
  assert.equal(bandeja.getSnapshot().contadorError, true);
  const otraSesion = preparar({ contar: async () => { throw new Error('Error'); } });
  await otraSesion.refrescarContador();
  assert.equal(otraSesion.getSnapshot().noLeidas, null);
  assert.equal(otraSesion.getSnapshot().lista, null);
});

test('sin leídas no ejecuta DELETE y una bandeja vacía se confirma como cero', async () => {
  let borrados = 0;
  const bandeja = preparar({ listar: async () => [], eliminarLeidas: async () => { borrados++; } });
  await bandeja.cargar();
  await bandeja.eliminarLeidas();
  assert.equal(borrados, 0);
  assert.equal(bandeja.getSnapshot().noLeidas, 0);
});

let servidor;
let vistas;
let EnlaceNotificaciones;
let api;
before(async () => {
  servidor = await createServer({
    root: fileURLToPath(new URL('..', import.meta.url)),
    server: { middlewareMode: true, hmr: false, watch: null }, appType: 'custom',
  });
  vistas = await servidor.ssrLoadModule('/src/pages/Notificaciones.jsx');
  ({ EnlaceNotificaciones } = await servidor.ssrLoadModule('/src/components/Layout.jsx'));
  api = await servidor.ssrLoadModule('/src/api/colab.js');
});
after(async () => { await servidor?.close(); });

function pantalla(cambios = {}) {
  return renderToStaticMarkup(createElement(vistas.NotificacionesContenido, {
    estado: { lista: [], noLeidas: 0, cargando: false, operacion: null, error: '', desactualizado: false, ...cambios },
    onMarcarLeida: () => {}, onEliminarLeidas: () => {}, onReintentar: () => {},
  }));
}

test('la bandeja muestra proyecto, mensaje, fecha y estado sin un botón Actualizar', () => {
  const html = pantalla({ lista: [aviso(1), aviso(2, true)], noLeidas: 1 });
  assert.match(html, /Matrícula · TEC-7/);
  assert.match(html, /Primera línea\nSegunda línea/);
  assert.match(html, /26\/11\/2026 10:15:00/);
  assert.match(html, /Sin leer/);
  assert.match(html, />Leída</);
  assert.equal((html.match(/>Marcar como leída</g) ?? []).length, 1);
  assert.match(html, /Eliminar leídas/);
  assert.doesNotMatch(html, />Actualizar</);
});

test('el estado vacío, carga y error tienen alternativas accesibles y acciones bloqueadas', () => {
  assert.match(pantalla(), /Todavía no tienes notificaciones/);
  const cargando = pantalla({ lista: null, noLeidas: null, cargando: true });
  assert.match(cargando, /aria-busy="true"/);
  assert.match(cargando, /Cargando notificaciones/);
  assert.doesNotMatch(cargando, /Todavía no tienes|0 sin leer/);
  const error = pantalla({ lista: [aviso(1), aviso(2, true)], desactualizado: true, error: 'Sin red' });
  assert.match(error, /role="alert"/);
  assert.match(error, /última bandeja conocida/);
  assert.match(error, /Reintentar/);
  assert.equal((error.match(/disabled=""/g) ?? []).length, 2);
});

test('las tarjetas no inventan contexto y escapan el texto recibido', () => {
  const html = pantalla({ lista: [aviso(1, false, {
    proyectoId: null, proyectoNombre: null, proyectoCodigo: null, mensaje: '<script>alert(1)</script>',
  })] });
  assert.match(html, /Proyecto no identificado/);
  assert.match(html, /&lt;script&gt;/);
  assert.doesNotMatch(html, /<script>/);
});

test('la acción de lectura pasa el id real y no existe en avisos ya leídos', () => {
  let marcado;
  const tarjeta = vistas.TarjetaNotificacion({ aviso: aviso(8), bloqueado: true, guardando: true, onMarcarLeida: (id) => { marcado = id; } });
  const boton = tarjeta.props.children.at(-1).props.children.at(-1);
  assert.equal(boton.props.disabled, true);
  assert.equal(boton.props.children, 'Guardando…');
  boton.props.onClick();
  assert.equal(marcado, 8);
  const html = renderToStaticMarkup(createElement(vistas.TarjetaNotificacion, { aviso: aviso(8, true) }));
  assert.doesNotMatch(html, /<button/);
});

test('la confirmación explica qué conserva y sus botones invocan cancelar o confirmar', () => {
  let cancelaciones = 0;
  let confirmaciones = 0;
  const props = { onCancelar: () => { cancelaciones++; }, onConfirmar: () => { confirmaciones++; } };
  const html = renderToStaticMarkup(createElement(vistas.ConfirmarEliminarLeidas, props));
  assert.match(html, /<dialog/);
  assert.match(html, /sin leer se conservarán/);
  assert.match(html, /no se puede deshacer/);
  const modal = vistas.ConfirmarEliminarLeidas(props);
  const botones = modal.props.children.props.children.at(-1).props.children;
  botones[0].props.onClick();
  assert.equal(cancelaciones, 1);
  assert.equal(confirmaciones, 0);
  botones[1].props.onClick();
  assert.equal(confirmaciones, 1);
});

test('la campana enlaza a la bandeja y distingue contador desconocido, cero y fallo', () => {
  const render = (props) => renderToStaticMarkup(createElement(StaticRouter, { location: '/notificaciones' },
    createElement(EnlaceNotificaciones, props)));
  const html = render({ noLeidas: 4 });
  assert.match(html, /href="\/notificaciones"/);
  assert.match(html, /aria-current="page"/);
  assert.match(html, /Notificaciones: 4 sin leer/);
  assert.match(html, /nav__insignia/);
  assert.doesNotMatch(render({ noLeidas: 0 }), /nav__insignia/);
  assert.match(render({ noLeidas: null }), /Contador no disponible/);
  assert.match(render({ noLeidas: 4, contadorError: true }), /Contador sin actualizar/);
});

test('los endpoints usan Bearer y DELETE 204 no intenta leer ni analizar JSON', async (t) => {
  const llamadas = [];
  t.mock.method(globalThis, 'fetch', async (url, opciones) => {
    llamadas.push({ url, ...opciones });
    if (opciones.method === 'DELETE') return {
      status: 204, ok: true,
      text: () => { throw new Error('204 no debe leer cuerpo'); },
      json: () => { throw new Error('204 no tiene JSON'); },
    };
    return new Response(JSON.stringify(url.endsWith('/no-leidas') ? { noLeidas: 1 } :
      opciones.method === 'PUT' ? aviso(1, true) : [aviso(1)]), { status: 200 });
  });
  assert.equal((await api.listarNotificaciones('sesion-prueba')).length, 1);
  assert.equal((await api.contarNotificaciones('sesion-prueba')).noLeidas, 1);
  assert.equal((await api.marcarNotificacionLeida('sesion-prueba', 1)).leida, true);
  assert.equal(await api.eliminarNotificacionesLeidas('sesion-prueba'), null);
  assert.deepEqual(llamadas.map((n) => [n.method, n.url]), [
    ['GET', '/api/notificaciones'], ['GET', '/api/notificaciones/no-leidas'],
    ['PUT', '/api/notificaciones/1/leida'], ['DELETE', '/api/notificaciones/leidas'],
  ]);
  assert.ok(llamadas.every((n) => n.headers.Authorization === 'Bearer sesion-prueba'));
});

test('backend desactualizado: 404/405 de DELETE indican revisar y reiniciar el servidor', async (t) => {
  let status = 404;
  t.mock.method(globalThis, 'fetch', async () => new Response('', { status }));
  for (status of [404, 405]) {
    await assert.rejects(api.eliminarNotificacionesLeidas('prueba'), /Actualiza o reinicia el backend/);
  }
});