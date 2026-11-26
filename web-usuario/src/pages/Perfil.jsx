import { useCallback, useEffect, useRef, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import * as api from '../api/colab.js';
import { AccesoCatalogoHabilidades, MisHabilidades } from '../components/HabilidadesPerfil.jsx';
import { ordenarHabilidades } from '../habilidades.js';
import { etiqueta, iniciales } from '../utils.js';

export function CabeceraPerfil({ perfil, onCerrarSesion }) {
  return (
    <section className="tarjeta portada perfil__portada" aria-label="Datos del perfil">
      <span className="avatar" aria-hidden="true">{iniciales(perfil.nombre)}</span>
      <div className="perfil__identidad">
        <h1>{perfil.nombre}</h1>
        <p className="portada__correo">{perfil.email}</p>
        <p className="portada__meta">
          {perfil.habilidades.length === 1
            ? '1 habilidad en tu perfil'
            : `${perfil.habilidades.length} habilidades en tu perfil`}
        </p>
      </div>
      <button type="button" className="btn btn--peligro perfil__cerrar-sesion" onClick={onCerrarSesion}>
        Cerrar sesión
      </button>
    </section>
  );
}

export default function Perfil() {
  const { token, cerrarSesion } = useAuth();
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

  return (
    <>
      {estado === 'cargando' && <p className="estado" role="status">Cargando tu perfil…</p>}

      {estado === 'error' && (
        <div className="estado estado--error" role="alert">
          <p>{errorCarga}</p>
          <button type="button" className="btn btn--primario" onClick={cargar}>Reintentar</button>
        </div>
      )}

      {estado === 'ok' && (
        <div className="perfil">
          <CabeceraPerfil perfil={perfil} onCerrarSesion={cerrarSesion} />

          <MisHabilidades habilidades={habilidades} ocupadas={ocupadas} mostrarTodas={mostrarTodas}
            onMostrarTodas={setMostrarTodas} onEliminar={eliminar} verTodasRef={verTodasRef} />
          <AccesoCatalogoHabilidades />
        </div>
      )}
    </>
  );
}
