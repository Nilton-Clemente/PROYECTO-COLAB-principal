import { useEffect, useState, useSyncExternalStore } from 'react';
import { useOutletContext } from 'react-router-dom';
import Modal from '../components/Modal.jsx';
import { IconTrash } from '../components/Icons.jsx';
import { fechaNotificacion, proyectoNotificacion, tipoNotificacion } from '../notificaciones.js';

export function TarjetaNotificacion({ aviso, bloqueado, guardando, onMarcarLeida }) {
  return (
    <li className={`tarjeta notificacion${aviso.leida ? '' : ' notificacion--sin-leer'}`}>
      <div className="notificacion__cabecera">
        <span className="notificacion__tipo">{tipoNotificacion(aviso.tipoEvento)}</span>
        <span className={`chip ${aviso.leida ? 'chip--gris' : 'chip--azul'}`}>{aviso.leida ? 'Leída' : 'Sin leer'}</span>
      </div>
      <h2 className="notificacion__proyecto">{proyectoNotificacion(aviso)}</h2>
      <p className="notificacion__mensaje">{aviso.mensaje}</p>
      <div className="notificacion__pie">
        <span className="notificacion__fecha">{fechaNotificacion(aviso.fechaCreacion)}</span>
        {!aviso.leida && (
          <button type="button" className="btn btn--ghost btn--chico" disabled={bloqueado}
            aria-label={`Marcar como leída: ${proyectoNotificacion(aviso)}. ${aviso.mensaje}`}
            onClick={() => onMarcarLeida(aviso.id)}>
            {guardando ? 'Guardando…' : 'Marcar como leída'}
          </button>
        )}
      </div>
    </li>
  );
}

export function ConfirmarEliminarLeidas({ onCancelar, onConfirmar }) {
  return (
    <Modal titulo="¿Eliminar notificaciones leídas?" onCerrar={onCancelar}>
      <div className="modal__form">
        <p>Se eliminarán todas tus notificaciones marcadas como leídas. Las que están sin leer se conservarán. Esta acción no se puede deshacer.</p>
        <div className="modal__pie">
          <button type="button" className="btn btn--ghost" onClick={onCancelar}>Cancelar</button>
          <button type="button" className="btn btn--peligro" onClick={onConfirmar}>Eliminar</button>
        </div>
      </div>
    </Modal>
  );
}

export function NotificacionesContenido({ estado, onMarcarLeida, onEliminarLeidas, onReintentar }) {
  const [confirmar, setConfirmar] = useState(false);
  const ocupado = estado.cargando || estado.operacion !== null;
  const bloqueado = ocupado || estado.desactualizado;
  const puedeEliminar = !bloqueado && Boolean(estado.lista?.some((aviso) => aviso.leida));
  return (
    <section className="notificaciones" aria-labelledby="notificaciones-titulo" aria-busy={ocupado}>
      <div className="titulo-fila">
        <div>
          <h1 className="titulo" id="notificaciones-titulo">Notificaciones</h1>
          <p className="seccion__sub">Solicitudes de cambios y avisos de tus proyectos.</p>
          <p className="notificaciones__contador" role="status">
            {estado.noLeidas == null ? 'Contador no disponible' : `${estado.noLeidas} sin leer`}
            {estado.contadorError && estado.noLeidas != null && ' · Sin actualizar'}
          </p>
        </div>
        <button type="button" className="btn btn--ghost" disabled={!puedeEliminar} onClick={() => setConfirmar(true)}>
          <IconTrash /> {estado.operacion === 'eliminar' ? 'Eliminando…' : 'Eliminar leídas'}
        </button>
      </div>
      {ocupado && <p className="notificaciones__cargando" role="status">{estado.operacion ? 'Guardando cambios…' : 'Cargando notificaciones…'}</p>}
      {estado.error && (
        <div className="notificaciones__error" role="alert">
          <p>{estado.error}</p>
          {estado.desactualizado && <p>No se pudieron actualizar los datos. Se conserva la última bandeja conocida.</p>}
          <button type="button" className="btn btn--ghost" disabled={ocupado} onClick={onReintentar}>Reintentar</button>
        </div>
      )}
      {estado.lista?.length === 0 && <p className="vacio">Todavía no tienes notificaciones.</p>}
      {estado.lista?.length > 0 && (
        <ul className="notificaciones__lista">
          {estado.lista.map((aviso) => <TarjetaNotificacion key={aviso.id} aviso={aviso}
            bloqueado={bloqueado} guardando={estado.operacion === aviso.id} onMarcarLeida={onMarcarLeida} />)}
        </ul>
      )}
      {confirmar && puedeEliminar && <ConfirmarEliminarLeidas onCancelar={() => setConfirmar(false)} onConfirmar={() => {
        setConfirmar(false);
        onEliminarLeidas();
      }} />}
    </section>
  );
}

export default function Notificaciones() {
  const { bandeja } = useOutletContext();
  const estado = useSyncExternalStore(bandeja.subscribe, bandeja.getSnapshot, bandeja.getSnapshot);
  useEffect(() => { bandeja.cargar(); }, [bandeja]);
  return <NotificacionesContenido estado={estado} onMarcarLeida={bandeja.marcarLeida}
    onEliminarLeidas={bandeja.eliminarLeidas} onReintentar={bandeja.cargar} />;
}