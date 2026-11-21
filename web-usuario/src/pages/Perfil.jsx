import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useOutletContext } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import * as api from '../api/colab.js';
import { IconTrash } from '../components/Icons.jsx';
import IconoHabilidad from '../components/IconoHabilidad.jsx';
import { etiquetaNivel, LIMITE_HABILIDADES_PERFIL, ordenarHabilidades } from '../habilidades.js';
import { etiqueta, iniciales } from '../utils.js';

function TarjetaHabilidad({ habilidad, ocupada, onEliminar }) {
  return (
    <li className="perfil-habilidades__tarjeta">
      <IconoHabilidad habilidad={habilidad} />
      <h3>{etiqueta(habilidad.nombre)}</h3>
      <span className="perfil-habilidades__nivel">{etiquetaNivel(habilidad.nivel)}</span>
      <button
        type="button"
        className="btn perfil-habilidades__quitar"
        disabled={ocupada}
        onClick={() => onEliminar(habilidad)}
        aria-label={`Quitar ${etiqueta(habilidad.nombre)} de mi perfil`}
      >
        <IconTrash aria-hidden="true" /> Quitar
      </button>
    </li>
  );
}

export default function Perfil() {
  const { token } = useAuth();
  const { avisar } = useOutletContext();
  const [estado, setEstado] = useState('cargando'); // cargando | ok | error
  const [errorCarga, setErrorCarga] = useState('');
  const [perfil, setPerfil] = useState(null);
  const [ocupadas, setOcupadas] = useState(() => new Set());
  const [mostrarTodas, setMostrarTodas] = useState(false);
  const verTodasRef = useRef(null);

  const cargar = useCallback(async () => {
    setEstado('cargando');
    try {
      setPerfil(await api.obtenerPerfil(token));
      setEstado('ok');
    } catch (err) {
      setErrorCarga(err.message);
      setEstado('error');
    }
  }, [token]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const marcar = (id, activa) =>
    setOcupadas((prev) => {
      const sig = new Set(prev);
      if (activa) sig.add(id);
      else sig.delete(id);
      return sig;
    });

  async function eliminar(h) {
    marcar(h.id, true);
    try {
      await api.eliminarHabilidad(token, h.id);
      setPerfil((p) => ({ ...p, habilidades: p.habilidades.filter((x) => x.id !== h.id) }));
      avisar(`${etiqueta(h.nombre)} se quitó de tu perfil.`, 'ok');
    } catch (err) {
      avisar(`No se pudo eliminar ${etiqueta(h.nombre)}. ${err.message}`);
    } finally {
      marcar(h.id, false);
    }
  }

  const habilidades = ordenarHabilidades(perfil?.habilidades ?? []);
  const restantes = Math.max(0, habilidades.length - LIMITE_HABILIDADES_PERFIL);
  const visibles = mostrarTodas ? habilidades : habilidades.slice(0, LIMITE_HABILIDADES_PERFIL);

  return (
    <>
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

            <section className="tarjeta perfil-habilidades" aria-labelledby="mis-habilidades">
              <div className="perfil-habilidades__cab">
                <div>
                  <div className="seccion__cab">
                    <h2 id="mis-habilidades">Mis habilidades</h2>
                    <span className="contador">{habilidades.length} {habilidades.length === 1 ? 'habilidad' : 'habilidades'}</span>
                  </div>
                  <p className="seccion__sub">Tecnologías y competencias que has registrado en tu perfil.</p>
                </div>
                {restantes > 0 && (
                  <button ref={verTodasRef} type="button" className="btn perfil-habilidades__ver" aria-expanded={mostrarTodas}
                    aria-controls="lista-mis-habilidades" onClick={() => setMostrarTodas((valor) => !valor)}>
                    {mostrarTodas ? 'Ver menos' : 'Ver todas'} <span aria-hidden="true">›</span>
                  </button>
                )}
              </div>
              {habilidades.length === 0 ? (
                <p className="vacio">Aún no tienes habilidades. Explora el catálogo para agregar tus competencias.</p>
              ) : (
                <ul className="perfil-habilidades__grilla" id="lista-mis-habilidades">
                  {visibles.map((h) => (
                    <TarjetaHabilidad key={h.id} habilidad={h} ocupada={ocupadas.has(h.id)} onEliminar={eliminar} />
                  ))}
                  {!mostrarTodas && restantes > 0 && (
                    <li className="perfil-habilidades__mas">
                      <button type="button" className="btn" onClick={() => {
                        setMostrarTodas(true);
                        verTodasRef.current?.focus();
                      }} aria-label={`Ver ${restantes} habilidades más`}>
                        <strong aria-hidden="true">+{restantes}</strong> Ver más
                      </button>
                    </li>
                  )}
                </ul>
              )}
            </section>

            <section className="tarjeta perfil-habilidades__acceso" aria-labelledby="catalogo">
              <div>
                <h2 id="catalogo">Explorar catálogo de habilidades</h2>
                <p className="seccion__sub">Descubre habilidades y añade a tu perfil las que dominas.</p>
              </div>
              <Link to="/perfil/habilidades" className="btn btn--primario">
                Ir al catálogo <span aria-hidden="true">→</span>
              </Link>
            </section>
          </>
        )}
      
    </>
  );
}
