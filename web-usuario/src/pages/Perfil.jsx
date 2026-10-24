import { useCallback, useEffect, useMemo, useState } from 'react';
import { useAuth } from '../auth/AuthContext.jsx';
import * as api from '../api/colab.js';
import { IconPlus, IconSearch, IconTrash } from '../components/Icons.jsx';
import { etiqueta, iniciales } from '../utils.js';

function TarjetaHabilidad({ habilidad, accion, ocupada, onAccion }) {
  const agregar = accion === 'agregar';
  return (
    <li className="habilidad">
      <div>
        <span className="chip">{etiqueta(habilidad.area ?? 'General')}</span>
        <h3>{etiqueta(habilidad.nombre)}</h3>
      </div>
      <button
        type="button"
        className={`btn ${agregar ? 'btn--verde' : 'btn--peligro'} btn--bloque`}
        disabled={ocupada}
        onClick={() => onAccion(habilidad)}
        aria-label={`${agregar ? 'Agregar' : 'Eliminar'} ${etiqueta(habilidad.nombre)}`}
      >
        {agregar ? <IconPlus /> : <IconTrash />}
        {agregar ? 'Agregar' : 'Eliminar'}
      </button>
    </li>
  );
}

export default function Perfil() {
  const { token, cerrarSesion } = useAuth();
  const [estado, setEstado] = useState('cargando'); // cargando | ok | error
  const [errorCarga, setErrorCarga] = useState('');
  const [perfil, setPerfil] = useState(null);
  const [catalogo, setCatalogo] = useState([]);
  const [ocupadas, setOcupadas] = useState(() => new Set());
  const [area, setArea] = useState('Todas');
  const [busqueda, setBusqueda] = useState('');
  const [aviso, setAviso] = useState(null);

  const cargar = useCallback(async () => {
    setEstado('cargando');
    try {
      const [p, c] = await Promise.all([api.obtenerPerfil(token), api.listarCatalogo(token)]);
      setPerfil(p);
      setCatalogo(c);
      setEstado('ok');
    } catch (err) {
      setErrorCarga(err.message);
      setEstado('error');
    }
  }, [token]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  useEffect(() => {
    if (!aviso) return undefined;
    const t = setTimeout(() => setAviso(null), 4500);
    return () => clearTimeout(t);
  }, [aviso]);

  const marcar = (id, activa) =>
    setOcupadas((prev) => {
      const sig = new Set(prev);
      if (activa) sig.add(id);
      else sig.delete(id);
      return sig;
    });

  async function agregar(h) {
    marcar(h.id, true);
    try {
      await api.agregarHabilidad(token, h.id);
      setPerfil((p) => ({ ...p, habilidades: [...p.habilidades, h] }));
    } catch (err) {
      setAviso(`No se pudo agregar ${etiqueta(h.nombre)}. ${err.message}`);
    } finally {
      marcar(h.id, false);
    }
  }

  async function eliminar(h) {
    marcar(h.id, true);
    try {
      await api.eliminarHabilidad(token, h.id);
      setPerfil((p) => ({ ...p, habilidades: p.habilidades.filter((x) => x.id !== h.id) }));
    } catch (err) {
      setAviso(`No se pudo eliminar ${etiqueta(h.nombre)}. ${err.message}`);
    } finally {
      marcar(h.id, false);
    }
  }

  const areas = useMemo(() => ['Todas', ...new Set(catalogo.map((h) => h.area).filter(Boolean))], [catalogo]);

  const disponibles = useMemo(() => {
    if (!perfil) return [];
    const mias = new Set(perfil.habilidades.map((h) => h.id));
    const q = busqueda.trim().toLowerCase();
    return catalogo.filter(
      (h) =>
        !mias.has(h.id) &&
        (area === 'Todas' || h.area === area) &&
        (!q || etiqueta(h.nombre).toLowerCase().includes(q)),
    );
  }, [catalogo, perfil, area, busqueda]);

  const totalDisponibles = perfil ? catalogo.length - perfil.habilidades.length : 0;

  return (
    <div className="app">
      <header className="barra">
        <div className="barra__interior">
          <span className="logo">Colab</span>
          <nav aria-label="Principal">
            <span className="nav__activo" aria-current="page">Mi perfil</span>
          </nav>
          <div className="barra__usuario">
            {perfil && (
              <div className="barra__datos">
                <span className="avatar avatar--chico" aria-hidden="true">{iniciales(perfil.nombre)}</span>
                <div>
                  <strong>{perfil.nombre}</strong>
                  <small>{perfil.email}</small>
                </div>
              </div>
            )}
            <button type="button" className="enlace-peligro" onClick={cerrarSesion}>
              Cerrar sesión
            </button>
          </div>
        </div>
      </header>

      <main className="contenido">
        {estado === 'cargando' && <p className="estado">Cargando tu perfil…</p>}

        {estado === 'error' && (
          <div className="estado estado--error" role="alert">
            <p>{errorCarga}</p>
            <button type="button" className="btn btn--primario" onClick={cargar}>Reintentar</button>
          </div>
        )}

        {estado === 'ok' && (
          <>
            <section className="tarjeta portada" aria-label="Datos del perfil">
              <span className="avatar" aria-hidden="true">{iniciales(perfil.nombre)}</span>
              <div>
                <h1>{perfil.nombre}</h1>
                <p className="portada__correo">{perfil.email}</p>
                <p className="portada__meta">
                  {perfil.habilidades.length === 1
                    ? '1 habilidad en tu perfil'
                    : `${perfil.habilidades.length} habilidades en tu perfil`}
                </p>
              </div>
            </section>

            <section aria-labelledby="mis-habilidades">
              <div className="seccion__cab">
                <h2 id="mis-habilidades">Mis habilidades</h2>
                <span className="contador">{perfil.habilidades.length}</span>
              </div>
              <p className="seccion__sub">Tecnologías y competencias asignadas para tus proyectos de sprint.</p>

              {perfil.habilidades.length === 0 ? (
                <p className="vacio">Aún no tienes habilidades. Elige las que domines en el catálogo de abajo.</p>
              ) : (
                <ul className="grilla">
                  {perfil.habilidades.map((h) => (
                    <TarjetaHabilidad key={h.id} habilidad={h} accion="eliminar" ocupada={ocupadas.has(h.id)} onAccion={eliminar} />
                  ))}
                </ul>
              )}
            </section>

            <section aria-labelledby="catalogo">
              <div className="seccion__cab">
                <h2 id="catalogo">Catálogo disponible</h2>
                <span className="contador contador--verde">{totalDisponibles}</span>
              </div>
              <p className="seccion__sub">Explora y añade nuevas tecnologías a tu perfil de estudiante.</p>

              <div className="filtros">
                <div className="filtros__tabs" role="group" aria-label="Filtrar por área">
                  {areas.map((a) => (
                    <button
                      key={a}
                      type="button"
                      className={a === area ? 'tab tab--activa' : 'tab'}
                      aria-pressed={a === area}
                      onClick={() => setArea(a)}
                    >
                      {etiqueta(a)}
                    </button>
                  ))}
                </div>
                <label className="busqueda">
                  <IconSearch />
                  <input
                    type="search"
                    placeholder="Buscar en catálogo…"
                    value={busqueda}
                    onChange={(e) => setBusqueda(e.target.value)}
                    aria-label="Buscar en catálogo"
                  />
                </label>
              </div>

              {disponibles.length === 0 ? (
                <p className="vacio">
                  {totalDisponibles === 0
                    ? 'Ya agregaste todas las habilidades del catálogo.'
                    : 'Ninguna habilidad coincide con tu búsqueda.'}
                </p>
              ) : (
                <ul className="grilla">
                  {disponibles.map((h) => (
                    <TarjetaHabilidad key={h.id} habilidad={h} accion="agregar" ocupada={ocupadas.has(h.id)} onAccion={agregar} />
                  ))}
                </ul>
              )}
            </section>
          </>
        )}
      </main>

      <footer className="pie">
        <span><strong className="logo logo--chico">Colab</strong> © {new Date().getFullYear()} Departamento de Tecnología Digital TECSUP.</span>
      </footer>

      {aviso && <div className="toast" role="alert">{aviso}</div>}
    </div>
  );
}
