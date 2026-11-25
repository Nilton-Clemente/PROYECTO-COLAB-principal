import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useOutletContext } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import * as api from '../api/colab.js';
import Alerta from '../components/Alerta.jsx';
import { IconCheck, IconPlus, IconSearch } from '../components/Icons.jsx';
import IconoHabilidad from '../components/IconoHabilidad.jsx';
import Modal from '../components/Modal.jsx';
import { agruparHabilidades, etiquetaNivel, nivelValido, NIVELES_HABILIDAD } from '../habilidades.js';
import { etiqueta } from '../utils.js';

const DESCRIPCIONES_NIVEL = {
  BASICO: 'Fundamentos y ejercicios',
  INTERMEDIO: 'Aplicación en proyectos',
  AVANZADO: 'Diseño y uso autónomo',
};

function DialogoNivel({ habilidad, guardada, onGuardar, onCerrar }) {
  const [nivel, setNivel] = useState(nivelValido(guardada?.nivel) ? guardada.nivel : '');
  const [error, setError] = useState('');
  const [guardando, setGuardando] = useState(false);
  const nombre = etiqueta(habilidad.nombre);

  async function guardar(evento) {
    evento.preventDefault();
    if (guardando || !nivelValido(nivel)) return;
    setGuardando(true);
    setError('');
    try {
      await onGuardar(habilidad, nivel);
    } catch (err) {
      setError(err.message);
      setGuardando(false);
    }
  }

  return (
    <Modal titulo={`${guardada ? 'Editar nivel de' : 'Añadir'} ${nombre}`} ancho="ancho"
      onCerrar={onCerrar} bloqueado={guardando}>
      <form className="modal__form" onSubmit={guardar} aria-busy={guardando}>
        <div className="nivel-habilidad__introduccion">
          <IconoHabilidad habilidad={habilidad} />
          <p>Elige el nivel que mejor describa tu experiencia con esta habilidad.</p>
        </div>
        <fieldset className="nivel-habilidad" disabled={guardando}>
          <legend>Selecciona tu nivel de dominio</legend>
          <div className="nivel-habilidad__opciones">
            {Object.entries(NIVELES_HABILIDAD).map(([valor, texto]) => (
              <label key={valor} className={`nivel-habilidad__opcion${nivel === valor ? ' nivel-habilidad__opcion--activa' : ''}`}>
                <input type="radio" name="nivel-habilidad" value={valor} checked={nivel === valor}
                  onChange={() => setNivel(valor)} required />
                <span><strong>{texto}</strong><small>{DESCRIPCIONES_NIVEL[valor]}</small></span>
              </label>
            ))}
          </div>
        </fieldset>
        {error && <Alerta titulo="No se pudo guardar la habilidad">{error}</Alerta>}
        <div className="modal__pie">
          <button className="btn btn--ghost" type="button" disabled={guardando} onClick={onCerrar}>Cancelar</button>
          <button className="btn btn--primario" type="submit" disabled={guardando || !nivelValido(nivel)}>
            {guardando ? 'Guardando…' : guardada ? 'Guardar nivel' : 'Añadir a mi perfil'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

function TarjetaCatalogo({ habilidad, guardada, onAbrir }) {
  const nombre = etiqueta(habilidad.nombre);
  return (
    <li className="habilidad catalogo-habilidades__tarjeta">
      <div className="catalogo-habilidades__datos">
        <IconoHabilidad habilidad={habilidad} />
        {guardada && <span className="chip catalogo-habilidades__nivel">{etiquetaNivel(guardada.nivel)}</span>}
      </div>
      <h3>{nombre}</h3>
      <div className="catalogo-habilidades__acciones">
        {guardada && <span className="catalogo-habilidades__en-perfil"><IconCheck aria-hidden="true" /> En tu perfil</span>}
        <button type="button" className={`btn ${guardada ? 'btn--ghost' : 'btn--primario'}`}
          onClick={(evento) => onAbrir(habilidad, evento.currentTarget)}
          aria-label={guardada ? `Editar nivel de ${nombre}` : `Añadir ${nombre}`}>
          {guardada ? 'Editar' : <><IconPlus aria-hidden="true" /> Añadir</>}
        </button>
      </div>
    </li>
  );
}

export default function CatalogoHabilidades() {
  const { token } = useAuth();
  const { avisar } = useOutletContext();
  const [estado, setEstado] = useState('cargando');
  const [error, setError] = useState('');
  const [catalogo, setCatalogo] = useState([]);
  const [mias, setMias] = useState(() => new Map());
  const [busqueda, setBusqueda] = useState('');
  const [area, setArea] = useState('');
  const [elegida, setElegida] = useState(null);
  const buscarRef = useRef(null);
  const botonRef = useRef(null);
  const cargaRef = useRef(0);

  const cargar = useCallback(async () => {
    const carga = ++cargaRef.current;
    setEstado('cargando');
    try {
      const [habilidades, perfil] = await Promise.all([api.listarCatalogo(token), api.obtenerPerfil(token)]);
      if (carga !== cargaRef.current) return;
      setCatalogo(habilidades);
      setMias(new Map(perfil.habilidades.map((habilidad) => [habilidad.id, habilidad])));
      setEstado('ok');
    } catch (err) {
      if (carga !== cargaRef.current) return;
      setError(err.message);
      setEstado('error');
    }
  }, [token]);

  useEffect(() => {
    cargar();
    return () => { cargaRef.current += 1; };
  }, [cargar]);

  useEffect(() => {
    if (elegida) return undefined;
    function enfocarBusqueda(evento) {
      if ((evento.metaKey || evento.ctrlKey) && evento.key.toLowerCase() === 'k') {
        evento.preventDefault();
        buscarRef.current?.focus();
      }
    }
    window.addEventListener('keydown', enfocarBusqueda);
    return () => window.removeEventListener('keydown', enfocarBusqueda);
  }, [elegida]);

  const areas = useMemo(() => agruparHabilidades(catalogo).map(([categoria]) => categoria), [catalogo]);
  const grupos = useMemo(() => agruparHabilidades(catalogo, busqueda, area), [catalogo, busqueda, area]);
  const totalVisible = grupos.reduce((total, [, habilidades]) => total + habilidades.length, 0);

  function abrir(habilidad, boton) {
    botonRef.current = boton;
    setElegida(habilidad);
  }

  function cerrar() {
    setElegida(null);
    const boton = botonRef.current;
    window.requestAnimationFrame(() => { if (boton?.isConnected) boton.focus(); });
  }

  async function guardar(habilidad, nivel) {
    await api.agregarHabilidad(token, habilidad.id, nivel);
    const editando = mias.has(habilidad.id);
    setMias((actuales) => new Map(actuales).set(habilidad.id, { ...habilidad, nivel }));
    cerrar();
    avisar(`${etiqueta(habilidad.nombre)}: ${editando ? 'nivel actualizado' : 'habilidad añadida'} (${etiquetaNivel(nivel)}).`, 'ok');
  }

  return (
    <div className="catalogo-habilidades">
      <Link className="catalogo-habilidades__volver" to="/perfil"><span aria-hidden="true">←</span> Volver a mi perfil</Link>
      <section className="catalogo-habilidades__portada" aria-labelledby="titulo-catalogo-habilidades">
        <h1 id="titulo-catalogo-habilidades">Catálogo Oficial de Habilidades</h1>
        <p>Explora competencias y registra tu nivel de dominio en tu perfil Colab.</p>
        <div className="busqueda catalogo-habilidades__busqueda">
          <IconSearch aria-hidden="true" />
          <input ref={buscarRef} type="search" value={busqueda} onChange={(evento) => setBusqueda(evento.target.value)}
            aria-label="Buscar habilidades" placeholder="Busca por nombre o área…" />
          <kbd aria-hidden="true">⌘ / Ctrl + K</kbd>
        </div>
      </section>

      {estado === 'cargando' && <p className="estado" role="status">Cargando catálogo de habilidades…</p>}
      {estado === 'error' && (
        <div className="estado estado--error" role="alert">
          <p>{error}</p>
          <button type="button" className="btn btn--primario" onClick={cargar}>Reintentar</button>
        </div>
      )}
      {estado === 'ok' && (
        <>
          {areas.length > 0 && (
            <div className="filtros catalogo-habilidades__filtros">
              <div className="filtros__tabs" role="group" aria-label="Filtrar por área">
                {['', ...areas].map((categoria) => (
                  <button key={categoria} type="button" className={`tab${area === categoria ? ' tab--activa' : ''}`}
                    aria-pressed={area === categoria} onClick={() => setArea(categoria)}>
                    {categoria ? etiqueta(categoria) : 'Todas'}
                  </button>
                ))}
              </div>
              <span className="catalogo-habilidades__resultados" role="status">{totalVisible} {totalVisible === 1 ? 'habilidad' : 'habilidades'}</span>
            </div>
          )}
          {grupos.length === 0 && <p className="vacio">{catalogo.length === 0 ? 'No hay habilidades en el catálogo.' : 'Ninguna habilidad coincide con tu búsqueda.'}</p>}
          {grupos.map(([categoria, habilidades]) => (
            <section key={categoria} className="catalogo-habilidades__grupo" aria-label={etiqueta(categoria)}>
              <div className="seccion__cab"><h2>{etiqueta(categoria)}</h2><span className="contador">{habilidades.length}</span></div>
              <ul className="grilla">
                {habilidades.map((habilidad) => (
                  <TarjetaCatalogo key={habilidad.id} habilidad={habilidad} guardada={mias.get(habilidad.id)} onAbrir={abrir} />
                ))}
              </ul>
            </section>
          ))}
        </>
      )}
      {elegida && <DialogoNivel habilidad={elegida} guardada={mias.get(elegida.id)} onCerrar={cerrar} onGuardar={guardar} />}
    </div>
  );
}