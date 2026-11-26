import assert from 'node:assert/strict';
import { after, before, test } from 'node:test';
import { fileURLToPath } from 'node:url';
import { createElement } from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { StaticRouter } from 'react-router-dom/server.js';
import { createServer } from 'vite';

let servidor;
let MisHabilidades;
let AccesoCatalogoHabilidades;
let CabeceraPerfil;
let Layout;

before(async () => {
  servidor = await createServer({
    root: fileURLToPath(new URL('..', import.meta.url)),
    server: { middlewareMode: true, hmr: false, watch: null },
    appType: 'custom',
  });
  ({ MisHabilidades, AccesoCatalogoHabilidades } = await servidor.ssrLoadModule('/src/components/HabilidadesPerfil.jsx'));
  ({ CabeceraPerfil } = await servidor.ssrLoadModule('/src/pages/Perfil.jsx'));
  ({ default: Layout } = await servidor.ssrLoadModule('/src/components/Layout.jsx'));
  // El layout consume el contexto de autenticación incluso al renderizar en servidor.
  const { AuthProvider } = await servidor.ssrLoadModule('/src/auth/AuthContext.jsx');
  Layout = createElement(AuthProvider, null, createElement(Layout));
});

after(async () => { await servidor?.close(); });

const habilidades = Array.from({ length: 9 }, (_, i) => ({
  id: i + 1, nombre: `Competencia ${i + 1}`, icono: null, nivel: 'INTERMEDIO',
}));

function renderizar(props = {}) {
  return renderToStaticMarkup(createElement(MisHabilidades, {
    habilidades, ocupadas: new Set(), mostrarTodas: false,
    onMostrarTodas: () => {}, onEliminar: () => {}, verTodasRef: { current: null }, ...props,
  }));
}

test('el perfil muestra siete habilidades reales, el contador total y las restantes', () => {
  const html = renderizar();
  assert.equal((html.match(/class="perfil-habilidades__tarjeta"/g) ?? []).length, 7);
  assert.match(html, /9 habilidades/);
  assert.match(html, />\+2<\/strong>/);
  assert.match(html, /aria-label="Ver 2 habilidades más"/);
  assert.match(html, /aria-expanded="false"/);
  assert.doesNotMatch(html, /<h3>Competencia 8<\/h3>/);
});

test('al expandir se muestran todas las habilidades sin tarjeta de restantes', () => {
  const html = renderizar({ mostrarTodas: true });
  assert.equal((html.match(/class="perfil-habilidades__tarjeta"/g) ?? []).length, 9);
  assert.match(html, /Ver menos/);
  assert.match(html, /aria-expanded="true"/);
  assert.doesNotMatch(html, /class="perfil-habilidades__mas"/);
});

test('no inventa habilidades adicionales cuando hay siete o menos', () => {
  for (const cantidad of [1, 7]) {
    const html = renderizar({ habilidades: habilidades.slice(0, cantidad) });
    assert.doesNotMatch(html, /Ver todas|Ver más|perfil-habilidades__mas/);
  }
  const html = renderizar({ habilidades: habilidades.slice(0, 8) });
  assert.match(html, /aria-label="Ver 1 habilidad más"/);
});

test('el estado vacío invita a explorar el catálogo y no dibuja tarjetas', () => {
  const html = renderizar({ habilidades: [] });
  assert.match(html, /0 habilidades/);
  assert.match(html, /Aún no tienes habilidades/);
  assert.doesNotMatch(html, /<li|Ver todas|Ver más/);
});

test('conserva los niveles oficiales y no asigna una insignia a niveles desconocidos', () => {
  for (const [nivel, texto, clase] of [
    ['BASICO', 'Básico', 'basico'], ['INTERMEDIO', 'Intermedio', 'intermedio'], ['AVANZADO', 'Avanzado', 'avanzado'],
  ]) {
    const html = renderizar({ habilidades: [{ ...habilidades[0], nivel }] });
    assert.match(html, new RegExp(`perfil-habilidades__indicador--${clase}`));
    assert.ok(html.includes(texto));
  }
  const html = renderizar({ habilidades: [{ ...habilidades[0], nivel: null }] });
  assert.match(html, /Sin nivel/);
  assert.doesNotMatch(html, /perfil-habilidades__indicador/);
});

test('utiliza el SVG del backend y mantiene la alternativa cuando no hay icono', () => {
  const html = renderizar({ habilidades: [{ ...habilidades[0], nombre: 'Spring Boot', icono: 'spring' }] });
  assert.match(html, /src="[^"]*\/iconos\/devicon\/spring.svg"/);
  const sinIcono = renderizar({ habilidades: [{ ...habilidades[0], nombre: 'Spring Boot' }] });
  assert.match(sinIcono, />SB<\/span>/);
});

test('la eliminación conserva su etiqueta accesible y se bloquea mientras está pendiente', () => {
  const html = renderizar({ habilidades: [habilidades[0]], ocupadas: new Set([1]) });
  assert.match(html, /disabled=""/);
  assert.match(html, /aria-label="Quitar Competencia 1 de mi perfil"/);
  assert.match(html, /Quitando…/);
});

test('el acceso al catálogo usa la ruta existente, sin diálogos de demostración', () => {
  const html = renderToStaticMarkup(createElement(StaticRouter, { location: '/perfil' },
    createElement(AccesoCatalogoHabilidades)));
  assert.match(html, /Explorar catálogo de habilidades/);
  assert.match(html, /href="\/perfil\/habilidades"/);
  assert.match(html, /Ir al catálogo/);
  assert.doesNotMatch(html, /role="dialog"/);
});

test('cerrar sesión aparece dentro de los datos del perfil y ejecuta la acción recibida', () => {
  let cierres = 0;
  const props = {
    perfil: { nombre: 'Estudiante de prueba', email: 'prueba@tecsup.edu.pe', habilidades: [] },
    onCerrarSesion: () => { cierres += 1; },
  };
  const html = renderToStaticMarkup(createElement(CabeceraPerfil, props));
  assert.match(html, /<section[^>]*aria-label="Datos del perfil"/);
  assert.match(html, /<button[^>]*type="button"[^>]*>Cerrar sesión<\/button>/);
  const boton = CabeceraPerfil(props).props.children.at(-1);
  boton.props.onClick();
  assert.equal(cierres, 1);
});

test('el encabezado conserva el enlace al perfil y ya no ofrece cerrar sesión', () => {
  const html = renderToStaticMarkup(createElement(StaticRouter, { location: '/perfil' }, Layout));
  assert.match(html, /href="\/perfil"/);
  assert.doesNotMatch(html, /Cerrar sesión/);
});