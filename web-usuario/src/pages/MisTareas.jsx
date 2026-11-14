import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useOutletContext, useParams } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import * as api from '../api/colab.js';
import Modal from '../components/Modal.jsx';
import { codigoTarea, ESTADO_TAREA, fechaCorta, PRIORIDAD } from '../utils.js';
import { puedeRegistrarAvance } from '../tareas.js';

const FILTROS = [
  ['todas', 'Todas'],
  ['pendientes', 'Pendientes'],
  ['progreso', 'En progreso'],
  ['completadas', 'Completadas'],
  ['bloqueadas', 'Bloqueadas'],
];

function coincideFiltro(tarea, filtro) {
  if (filtro === 'todas') return true;
  if (filtro === 'pendientes') return tarea.estado === 'PENDIENTE';
  if (filtro === 'progreso') return tarea.estado === 'EN_PROGRESO';
  if (filtro === 'completadas') return tarea.estado === 'COMPLETADA';
  return tarea.estado === 'BLOQUEADA';
}

function minutosTexto(minutos) {
  if (!minutos) return 'Sin avances registrados';
  const horas = Math.floor(minutos / 60);
  const resto = minutos % 60;
  return `${horas ? `${horas} h` : ''}${horas && resto ? ' ' : ''}${resto ? `${resto} min` : ''}`;
}

function TarjetaTarea({ tarea, minutos, puedeRegistrar, onRegistrar }) {
  const bloqueada = tarea.estado === 'BLOQUEADA';
  return (
    <article className={`tarjeta mis-tareas__tarjeta ${bloqueada ? 'mis-tareas__tarjeta--bloqueada' : ''}`}>
      <div className="mis-tareas__tarjeta-cab">
        <div className="mis-tareas__identidad">
          <code>{codigoTarea(tarea.id)}</code>
          <span>Fase: {tarea.etapaNombre}</span>
          <span className={`prio prio--${tarea.prioridad}`}>{PRIORIDAD[tarea.prioridad] ?? tarea.prioridad}</span>
        </div>
        <span className={`estado-tarea estado-tarea--${tarea.estado}`}>{ESTADO_TAREA[tarea.estado] ?? tarea.estado}</span>
      </div>

      <h2>{tarea.nombre}</h2>
      {tarea.descripcion && <p className="mis-tareas__descripcion">{tarea.descripcion}</p>}

      <div className="mis-tareas__meta">
        <span>Fecha: <strong>{fechaCorta(tarea.fechaInicio)} — {fechaCorta(tarea.fechaFin)}</strong></span>
        <span>{tarea.duracionDias} {tarea.duracionDias === 1 ? 'día' : 'días'} estimados</span>
        <span className={bloqueada ? 'mis-tareas__dependencia mis-tareas__dependencia--bloqueada' : 'mis-tareas__dependencia'}>
          {tarea.dependencias.length ? `Depende de: ${tarea.dependencias.map(codigoTarea).join(', ')}` : 'Sin dependencias'}
        </span>
      </div>

      <footer className="mis-tareas__tarjeta-pie">
        <div>
          <strong>Avance registrado</strong>
          <small>{minutosTexto(minutos)}</small>
        </div>
        <div className="mis-tareas__acciones-tarea">
          {puedeRegistrar && <button type="button" className="btn btn--ghost btn--chico" onClick={() => onRegistrar(tarea)}>Registrar avance</button>}
        </div>
      </footer>
    </article>
  );
}

export default function MisTareas() {
  const { id: proyectoId } = useParams();
  const navigate = useNavigate();
  const { token } = useAuth();
  const { usuario, avisar } = useOutletContext();
  const [estado, setEstado] = useState('cargando');
  const [error, setError] = useState('');
  const [proyecto, setProyecto] = useState(null);
  const [plan, setPlan] = useState(null);
  const [tareas, setTareas] = useState([]);
  const [confirmacion, setConfirmacion] = useState(null);
  const [minutosPorTarea, setMinutosPorTarea] = useState({});
  const [filtro, setFiltro] = useState('todas');
  const [busqueda, setBusqueda] = useState('');
  const [confirmando, setConfirmando] = useState(false);
  const [solicitando, setSolicitando] = useState(false);
  const [cambios, setCambios] = useState(false);
  const [comentario, setComentario] = useState('');

  const cargar = useCallback(async () => {
    setEstado('cargando');
    setError('');
    try {
      const [p, pl, misTareas, confirmaciones] = await Promise.all([
        api.verProyecto(token, proyectoId),
        api.obtenerPlanificacion(token, proyectoId),
        api.misTareas(token, proyectoId),
        api.listarConfirmaciones(token, proyectoId),
      ]);
      const historiales = await Promise.all(misTareas.map((tarea) => api.historialAvances(token, proyectoId, tarea.id).catch(() => ({ totalMinutos: 0 }))));
      setProyecto(p);
      setPlan(pl);
      setTareas(misTareas);
      setConfirmacion(confirmaciones.find((item) => item.integranteEmail === usuario.email) ?? null);
      setMinutosPorTarea(Object.fromEntries(misTareas.map((tarea, indice) => [tarea.id, historiales[indice].totalMinutos ?? 0])));
      setEstado('ok');
    } catch (err) {
      setError(err.status === 404 ? 'Este proyecto no tiene una planificación disponible.' : err.message);
      setEstado('error');
    }
  }, [proyectoId, token, usuario.email]);

  useEffect(() => { cargar(); }, [cargar]);

  const conteo = (clave) => tareas.filter((tarea) => coincideFiltro(tarea, clave)).length;
  const tareasVisibles = useMemo(() => {
    const termino = busqueda.trim().toLowerCase();
    return tareas.filter((tarea) => coincideFiltro(tarea, filtro) && (!termino || `${codigoTarea(tarea.id)} ${tarea.nombre} ${tarea.descripcion ?? ''} ${tarea.etapaNombre}`.toLowerCase().includes(termino)));
  }, [tareas, filtro, busqueda]);
  const puedeConfirmar = plan?.estado === 'PROPUESTA' && !confirmacion && tareas.length > 0;
  const cambiosSolicitados = confirmacion?.estado === 'CAMBIOS_SOLICITADOS';

  async function confirmar() {
    if (!window.confirm('¿Confirmas que las tareas y estimaciones asignadas son viables?')) return;
    setConfirmando(true);
    try {
      const respuesta = await api.confirmarPlanificacion(token, proyectoId);
      setConfirmacion(respuesta);
      setPlan(await api.obtenerPlanificacion(token, proyectoId));
      avisar('Confirmaste tus tareas asignadas. La planificación se activará cuando todos los responsables confirmen.', 'ok');
    } catch (err) {
      avisar(err.status === 409 ? 'La planificación ya no está en propuesta y no requiere confirmación.' : err.message);
      if (err.status === 409) cargar();
    } finally {
      setConfirmando(false);
    }
  }

  async function solicitarCambios(event) {
    event.preventDefault();
    if (comentario.trim().length < 10) return avisar('Describe el ajuste solicitado con al menos 10 caracteres.');
    setSolicitando(true);
    try {
      const respuesta = await api.solicitarCambiosPlanificacion(token, proyectoId, comentario.trim());
      setConfirmacion(respuesta);
      setCambios(false);
      setComentario('');
      avisar('Solicitud de cambios enviada al creador del proyecto.', 'ok');
    } catch (err) {
      avisar(err.status === 409 ? 'La planificación ya no admite solicitudes de cambio.' : err.message);
    } finally {
      setSolicitando(false);
    }
  }

  function registrarAvance(tarea) {
    if (!puedeRegistrarAvance(plan?.estado, tarea)) return;
    navigate(`/proyectos/${proyectoId}/tareas/${tarea.id}/avances/registrar`);
  }

  if (estado === 'cargando') return <p className="estado">Cargando tus tareas…</p>;
  if (estado === 'error') return (
    <div className="estado estado--error" role="alert">
      <p>{error}</p>
      <button type="button" className="btn btn--primario" onClick={cargar}>Reintentar</button>
    </div>
  );

  return (
    <section className="mis-tareas">
      <div className="mis-tareas__volver">
        <Link className="volver" to={`/proyectos/${proyectoId}`}>← Volver al detalle de {proyecto.nombre}</Link>
        <span>Planificación: {plan.estado === 'ACTIVA' ? 'Activa' : 'En propuesta'}</span>
      </div>

      <section className="tarjeta mis-tareas__portada">
        <div>
          <h1>Mis tareas asignadas</h1>
          <p>Revisa tus entregables, valida la propuesta de planificación y consulta el avance registrado de cada tarea.</p>
        </div>
        <dl>
          <div><dt>Asignadas</dt><dd>{tareas.length}</dd></div>
          <div><dt>En progreso</dt><dd>{conteo('progreso')}</dd></div>
          <div><dt>Completadas</dt><dd>{conteo('completadas')}</dd></div>
        </dl>
      </section>

      {plan.estado === 'PROPUESTA' && (
        <section className={cambiosSolicitados ? 'mis-tareas__aviso mis-tareas__aviso--alerta' : 'mis-tareas__aviso'}>
          <div>
            <strong>{cambiosSolicitados ? 'Solicitaste cambios en la planificación' : confirmacion ? 'Confirmaste tus tareas asignadas' : 'Confirmación de viabilidad pendiente'}</strong>
            <p>{cambiosSolicitados ? `Comentario enviado: ${confirmacion.comentario}` : confirmacion ? 'Tu confirmación fue registrada. La planificación se activará cuando todos los responsables confirmen.' : 'Confirma que las tareas, dependencias y estimaciones asignadas son viables antes de iniciar el trabajo.'}</p>
          </div>
          {!confirmacion && <div className="mis-tareas__aviso-acciones"><button type="button" className="btn btn--ghost btn--chico" onClick={() => setCambios(true)}>Solicitar cambios</button><button type="button" className="btn btn--primario btn--chico" disabled={confirmando} onClick={confirmar}>{confirmando ? 'Confirmando…' : 'Confirmar mis tareas'}</button></div>}
        </section>
      )}

      <div className="mis-tareas__barra">
        <div className="filtros__tabs" role="group" aria-label="Filtrar mis tareas">
          {FILTROS.map(([clave, etiqueta]) => <button key={clave} type="button" className={filtro === clave ? 'tab tab--activa' : 'tab'} aria-pressed={filtro === clave} onClick={() => setFiltro(clave)}>{etiqueta} <span className="tab__n">{conteo(clave)}</span></button>)}
        </div>
        <label className="busqueda"><span aria-hidden="true">⌕</span><input type="search" value={busqueda} onChange={(event) => setBusqueda(event.target.value)} placeholder="Buscar por tarea o fase…" aria-label="Buscar en mis tareas" /></label>
      </div>

      {tareasVisibles.length === 0 ? <section className="tarjeta vacio-grande"><h2>No hay tareas para este filtro</h2><p>{tareas.length ? 'Prueba con otro estado o término de búsqueda.' : 'Aún no tienes tareas asignadas en esta planificación.'}</p></section> : <div className="mis-tareas__lista">{tareasVisibles.map((tarea) => <TarjetaTarea key={tarea.id} tarea={tarea} minutos={minutosPorTarea[tarea.id]} puedeRegistrar={puedeRegistrarAvance(plan.estado, tarea)} onRegistrar={registrarAvance} />)}</div>}

      {cambios && <Modal titulo="Solicitar cambios en la planificación" subtitulo="La solicitud se enviará al creador y reiniciará las confirmaciones del equipo." onCerrar={() => setCambios(false)}>
        <form className="modal__form" onSubmit={solicitarCambios}>
          <label className="campo"><span>Motivo y propuesta de ajuste</span><textarea rows="5" value={comentario} onChange={(event) => setComentario(event.target.value)} placeholder="Explica qué tarea, duración, dependencia o asignación necesitas ajustar." autoFocus /></label>
          <div className="modal__pie"><button type="button" className="btn btn--ghost" onClick={() => setCambios(false)}>Cancelar</button><button type="submit" className="btn btn--primario" disabled={solicitando}>{solicitando ? 'Enviando…' : 'Enviar solicitud'}</button></div>
        </form>
      </Modal>}

    </section>
  );
}