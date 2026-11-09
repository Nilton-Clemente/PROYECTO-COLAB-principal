import { useCallback, useEffect, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import * as api from '../api/colab.js';
import { IconCheck } from '../components/Icons.jsx';
import { formatearFecha, iniciales } from '../utils.js';

function TarjetaInvitacion({ invitacion, ocupada, onAceptar, onRechazar }) {
  const { proyectoNombre, proyecto, creador } = invitacion;
  return (
    <li className="tarjeta invitacion">
      <div className="invitacion__cab">
        <div>
          <span className="chip chip--verde">{proyecto?.tipo === 'SOFTWARE' ? 'Software' : proyecto?.tipo ?? 'Proyecto'}</span>
          <h3>{proyectoNombre}</h3>
        </div>
        {proyecto?.fechaEntrega && <span className="proyecto__fecha">Entrega: {formatearFecha(proyecto.fechaEntrega)}</span>}
      </div>

      <p className="invitacion__rol">Rol asignado: <span className="chip chip--gris">Integrante</span></p>
      {proyecto?.descripcion && <p className="proyecto__desc">{proyecto.descripcion}</p>}

      {creador && (
        <div className="invitado-por">
          <span className="avatar avatar--medio" aria-hidden="true">{iniciales(creador.nombre)}</span>
          <div>
            <strong>{creador.nombre}</strong>
            <small>{creador.email}</small>
            <small>Creador del proyecto</small>
          </div>
        </div>
      )}

      {proyecto?.tecnologias?.length > 0 && (
        <div className="proyecto__techs">
          {proyecto.tecnologias.map((t) => <span key={t.id} className="etiqueta">{t.nombre}</span>)}
        </div>
      )}

      <div className="invitacion__acciones">
        <button type="button" className="btn btn--ghost" disabled={ocupada} onClick={() => onRechazar(invitacion)}>Rechazar</button>
        <button type="button" className="btn btn--verde" disabled={ocupada} onClick={() => onAceptar(invitacion)}>
          <IconCheck /> Aceptar invitación
        </button>
      </div>
    </li>
  );
}

export default function Invitaciones() {
  const { token } = useAuth();
  const { refrescarInvitaciones, avisar } = useOutletContext();
  const [estado, setEstado] = useState('cargando');
  const [error, setError] = useState('');
  const [lista, setLista] = useState([]);
  const [ocupada, setOcupada] = useState(null);

  const cargar = useCallback(async () => {
    setEstado('cargando');
    try {
      const invitaciones = await api.misInvitaciones(token);
      // Detalle del proyecto y su creador (el backend permite verlos a quien tiene la invitación).
      const enriquecidas = await Promise.all(
        invitaciones.map(async (inv) => {
          const [proyecto, equipo] = await Promise.all([
            api.verProyecto(token, inv.proyectoId).catch(() => null),
            api.verEquipo(token, inv.proyectoId).catch(() => []),
          ]);
          return { ...inv, proyecto, creador: equipo.find((m) => m.rol === 'CREADOR') ?? null };
        }),
      );
      setLista(enriquecidas);
      setEstado('ok');
    } catch (err) {
      setError(err.message);
      setEstado('error');
    }
  }, [token]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  async function responder(inv, aceptar) {
    if (!aceptar && !window.confirm(`¿Rechazar la invitación a "${inv.proyectoNombre}"?`)) return;
    setOcupada(inv.integranteId);
    try {
      if (aceptar) await api.aceptarInvitacion(token, inv.integranteId);
      else await api.rechazarInvitacion(token, inv.integranteId);
      setLista((l) => l.filter((x) => x.integranteId !== inv.integranteId));
      refrescarInvitaciones();
      avisar(aceptar ? `Te uniste a "${inv.proyectoNombre}".` : 'Invitación rechazada.', 'ok');
    } catch (err) {
      avisar(err.status === 409 ? 'Esta invitación ya fue procesada.' : err.message);
      if (err.status === 409 || err.status === 404) cargar();
    } finally {
      setOcupada(null);
    }
  }

  return (
    <>
      <section>
        <div className="titulo-fila">
          <div>
            <h1 className="titulo">Invitaciones recibidas</h1>
            <p className="seccion__sub">Revisa las solicitudes para unirte a equipos de proyectos de software en TECSUP.</p>
          </div>
          {estado === 'ok' && <span className="estadistica estadistica--chica"><span>Pendientes</span><strong>{lista.length}</strong></span>}
        </div>
      </section>

      {estado === 'cargando' && <p className="estado">Cargando invitaciones…</p>}

      {estado === 'error' && (
        <div className="estado estado--error" role="alert">
          <p>{error}</p>
          <button type="button" className="btn btn--primario" onClick={cargar}>Reintentar</button>
        </div>
      )}

      {estado === 'ok' && lista.length === 0 && <p className="vacio">No tienes invitaciones pendientes.</p>}

      {estado === 'ok' && lista.length > 0 && (
        <ul className="invitaciones">
          {lista.map((inv) => (
            <TarjetaInvitacion
              key={inv.integranteId}
              invitacion={inv}
              ocupada={ocupada === inv.integranteId}
              onAceptar={(i) => responder(i, true)}
              onRechazar={(i) => responder(i, false)}
            />
          ))}
        </ul>
      )}
    </>
  );
}
