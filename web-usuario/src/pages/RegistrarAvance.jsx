import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useOutletContext, useParams } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import * as api from '../api/colab.js';
import Alerta from '../components/Alerta.jsx';
import { codigoTarea, ESTADO_TAREA, fechaCorta, iniciales, PRIORIDAD } from '../utils.js';
import { puedeRegistrarAvance } from '../tareas.js';
import {
  guardarAvanceConEstado, MAX_HORAS_AVANCE, MAX_NOTA_AVANCE,
  MIN_HORAS_AVANCE, MIN_NOTA_AVANCE, OPCIONES_ESTADO_AVANCE,
  permiteEstadoAvance, resumenTiempo, validarAvance,
} from '../avances.js';

const horasTexto = (horas) => horas.toLocaleString('es-PE', { minimumFractionDigits: 1, maximumFractionDigits: 2 });
const fechaRegistro = (fecha) => new Date(fecha).toLocaleString('es-PE', { dateStyle: 'medium', timeStyle: 'short' });

export default function RegistrarAvance() {
  const { id: proyectoId, tareaId } = useParams();
  return <PantallaAvance key={`${proyectoId}:${tareaId}`} proyectoId={proyectoId} tareaId={tareaId} />;
}

function PantallaAvance({ proyectoId, tareaId }) {
  const { token } = useAuth();
  const [carga, setCarga] = useState({ estado: 'cargando' });
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    let vigente = true;
    setCarga({ estado: 'cargando' });
    async function cargar() {
      try {
        const [proyecto, plan, tareas] = await Promise.all([
          api.verProyecto(token, proyectoId),
          api.obtenerPlanificacion(token, proyectoId),
          api.misTareas(token, proyectoId),
        ]);
        const tarea = tareas.find((item) => String(item.id) === tareaId);
        if (!tarea) throw new Error('La tarea no existe en tus asignaciones de este proyecto o ya no eres su responsable.');
        // Un fallo del historial no se interpreta como cero horas.
        const historial = await api.historialAvances(token, proyectoId, tareaId);
        if (vigente) setCarga({ estado: 'ok', proyecto, plan, tarea, historial });
      } catch (err) {
        if (vigente) setCarga({ estado: 'error', error: err.message });
      }
    }
    cargar();
    return () => { vigente = false; };
  }, [token, proyectoId, tareaId, intento]);

  const volver = `/proyectos/${proyectoId}/mis-tareas`;
  if (carga.estado === 'cargando') return <p className="estado">Cargando tarea e historial de avances…</p>;
  if (carga.estado === 'error') return (
    <div className="estado estado--error" role="alert">
      <p>{carga.error}</p>
      <button type="button" className="btn btn--primario" onClick={() => setIntento((valor) => valor + 1)}>Reintentar</button>
      <Link to={volver}>← Volver a mis tareas</Link>
    </div>
  );
  return <FormularioAvance {...carga} proyectoId={proyectoId} tareaId={tareaId} onRecargar={() => setIntento((valor) => valor + 1)} />;
}

function FormularioAvance({ proyectoId, tareaId, proyecto, plan: planInicial, tarea: tareaInicial, historial: historialInicial, onRecargar }) {
  const { token } = useAuth();
  const { usuario, avisar } = useOutletContext();
  const navigate = useNavigate();
  const [plan, setPlan] = useState(planInicial);
  const [tarea, setTarea] = useState(tareaInicial);
  const [historial, setHistorial] = useState(historialInicial);
  const [horas, setHoras] = useState(1);
  const [nuevoEstado, setNuevoEstado] = useState(tareaInicial.estado);
  const [nota, setNota] = useState('');
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState('');
  const [guardado, setGuardado] = useState(false);
  const [estadoPendiente, setEstadoPendiente] = useState(null);
  const [resultadoIncierto, setResultadoIncierto] = useState(false);
  const operando = useRef(false);

  const volver = `/proyectos/${proyectoId}/mis-tareas`;
  const permitido = puedeRegistrarAvance(plan.estado, tarea);
  const errores = validarAvance({ horas, nota, estadoActual: tarea.estado, nuevoEstado });
  const bloqueado = guardando || guardado || Boolean(estadoPendiente) || resultadoIncierto || !permitido;
  const tiempo = resumenTiempo(historial.totalMinutos, guardado || estadoPendiente ? 0 : horas);
  const sinGuardar = !guardado && !estadoPendiente && (nota.length > 0 || horas !== 1 || nuevoEstado !== tarea.estado);

  useEffect(() => {
    if (!sinGuardar && !guardando) return undefined;
    const advertir = (event) => { event.preventDefault(); event.returnValue = ''; };
    window.addEventListener('beforeunload', advertir);
    return () => window.removeEventListener('beforeunload', advertir);
  }, [sinGuardar, guardando]);

  async function verificarAcceso() {
    const [actual, tareas] = await Promise.all([
      api.obtenerPlanificacion(token, proyectoId), api.misTareas(token, proyectoId),
    ]);
    const asignada = tareas.find((item) => String(item.id) === tareaId);
    if (!asignada) throw new Error('La tarea ya no está asignada a tu usuario. Vuelve a mis tareas.');
    setPlan(actual);
    setTarea(asignada);
    if (!puedeRegistrarAvance(actual.estado, asignada)) {
      throw new Error('Solo puedes registrar avances en una planificación activa y una tarea no completada.');
    }
    return asignada;
  }

  async function guardar(event) {
    event.preventDefault();
    if (operando.current || bloqueado) return;
    if (Object.keys(errores).length) { setError(Object.values(errores)[0]); return; }
    operando.current = true;
    setGuardando(true);
    setError('');
    let enviandoRegistro = false;
    try {
      const actual = await verificarAcceso();
      const fallos = validarAvance({ horas, nota, estadoActual: actual.estado, nuevoEstado });
      if (Object.keys(fallos).length) throw new Error(Object.values(fallos)[0]);
      const historialActual = await api.historialAvances(token, proyectoId, tareaId);
      setHistorial(historialActual);
      enviandoRegistro = true;
      const resultado = await guardarAvanceConEstado({
        registrar: (datos) => api.registrarAvance(token, proyectoId, tareaId, datos),
        cambiarEstado: (valor) => api.cambiarEstadoTarea(token, proyectoId, tareaId, valor),
      }, { horas, nota, estadoActual: actual.estado, nuevoEstado });
      const { registro } = resultado;
      setHistorial({
        totalMinutos: historialActual.totalMinutos + registro.minutos,
        registros: [registro, ...historialActual.registros.filter((item) => item.id !== registro.id)],
      });
      if (resultado.errorEstado) {
        setEstadoPendiente(nuevoEstado);
        setError('El tiempo y la descripción se guardaron, pero no se pudo confirmar el cambio de estado. Reintenta solo el estado; no vuelvas a registrar estas horas.');
      } else {
        if (resultado.tarea) setTarea(resultado.tarea);
        setGuardado(true);
        avisar(`Registraste ${horasTexto(registro.minutos / 60)} horas en ${codigoTarea(tarea.id)}.`, 'ok');
      }
    } catch (err) {
      if (enviandoRegistro && (err.status == null || err.status === 0 || err.status >= 500)) {
        setResultadoIncierto(true);
        setError('No se pudo confirmar la respuesta del servidor. El registro podría haberse guardado. Recarga y revisa el historial antes de registrar más horas para evitar duplicados.');
      } else {
        setError(err.message);
      }
    } finally {
      operando.current = false;
      setGuardando(false);
    }
  }

  async function reintentarEstado() {
    if (operando.current || !estadoPendiente) return;
    operando.current = true;
    setGuardando(true);
    try {
      const [actual, tareas] = await Promise.all([
        api.obtenerPlanificacion(token, proyectoId), api.misTareas(token, proyectoId),
      ]);
      const asignada = tareas.find((item) => String(item.id) === tareaId);
      if (!asignada || actual.estado !== 'ACTIVA') throw new Error('La tarea o la planificación cambiaron. Recarga para revisar su estado; las horas ya están guardadas.');
      const actualizada = asignada.estado === estadoPendiente ? asignada
        : permiteEstadoAvance(asignada.estado, estadoPendiente)
          ? await api.cambiarEstadoTarea(token, proyectoId, tareaId, estadoPendiente)
          : null;
      if (!actualizada) throw new Error('La transición ya no está permitida. Recarga para revisar la tarea; las horas ya están guardadas.');
      setTarea(actualizada);
      setPlan(actual);
      setEstadoPendiente(null);
      setGuardado(true);
      setError('');
      avisar('Estado actualizado. No se añadieron horas adicionales.', 'ok');
    } catch (err) {
      setError(`Las horas ya están guardadas. ${err.message}`);
    } finally {
      operando.current = false;
      setGuardando(false);
    }
  }

  function salir() {
    if (operando.current) return;
    if (sinGuardar && !window.confirm('¿Volver a mis tareas y descartar los cambios sin guardar?')) return;
    navigate(volver);
  }

  function descartar() {
    if (bloqueado || (sinGuardar && !window.confirm('¿Descartar los cambios del formulario?'))) return;
    setHoras(1);
    setNota('');
    setNuevoEstado(tarea.estado);
    setError('');
  }

  return (
    <form className="registro-avance" onSubmit={guardar}>
      <button type="button" className="volver registro-avance__volver" onClick={salir} disabled={guardando}>← Volver a mis tareas</button>

      <section className="tarjeta registro-avance__cabecera">
        <div>
          <div className="registro-avance__chips">
            <code>{codigoTarea(tarea.id)}</code>
            <span className="chip">{tarea.etapaNombre}</span>
            <span className={`prio prio--${tarea.prioridad}`}>Prioridad {PRIORIDAD[tarea.prioridad] ?? tarea.prioridad}</span>
          </div>
          <h1>Registrar avance</h1>
          <h2>{tarea.nombre}</h2>
          <p>{proyecto.nombre} · {fechaCorta(tarea.fechaInicio)} — {fechaCorta(tarea.fechaFin)}</p>
        </div>
        <span className={`estado-tarea estado-tarea--${tarea.estado}`}>{ESTADO_TAREA[tarea.estado] ?? tarea.estado}</span>
      </section>

      {!permitido && !guardado && <Alerta titulo="Registro no disponible" tipo="info">La planificación debe estar activa y la tarea no debe estar completada para registrar avances. Puedes consultar el historial a continuación.</Alerta>}
      {error && <Alerta titulo={estadoPendiente ? 'Avance guardado; estado pendiente' : 'Revisa el registro'}>{error}</Alerta>}
      {guardado && <div className="registro-avance__exito" role="status"><strong>Avance guardado correctamente.</strong> El historial y el total acumulado ya incluyen este registro.</div>}
      {(estadoPendiente || resultadoIncierto) && <div className="registro-avance__recuperar">
        {estadoPendiente && <button type="button" className="btn btn--primario" disabled={guardando} onClick={reintentarEstado}>{guardando ? 'Actualizando…' : 'Reintentar solo el cambio de estado'}</button>}
        <button type="button" className="btn btn--ghost" disabled={guardando} onClick={onRecargar}>Recargar tarea e historial</button>
      </div>}

      <section className="tarjeta registro-avance__resumen" aria-label="Resumen de tiempo">
        <div className="registro-avance__metricas">
          <div className="registro-avance__metrica"><span>Duración estimada</span><strong>{tarea.duracionDias} <small>días</small></strong><p>Estimación de la planificación</p></div>
          <div className="registro-avance__metrica"><span>Horas acumuladas</span><strong>{horasTexto(tiempo.acumuladas)} <small>h</small></strong><p>{historial.registros.length} {historial.registros.length === 1 ? 'registro guardado' : 'registros guardados'}</p></div>
          <div className="registro-avance__metrica registro-avance__metrica--verde"><span>Sesión por guardar</span><strong>+{horasTexto(tiempo.sesion)} <small>h</small></strong><p>{guardado || estadoPendiente ? 'La sesión ya está incluida en el total' : 'Tiempo invertido en esta sesión'}</p></div>
          <div className="registro-avance__metrica registro-avance__metrica--principal"><span>Total tras guardar</span><strong>{horasTexto(tiempo.proyectadas)} <small>h</small></strong><p>Tiempo registrado, no porcentaje completado</p></div>
        </div>
        <p className="registro-avance__ayuda">La estimación está expresada en días. No se convierte a horas porque el proyecto no define la duración de una jornada.</p>
      </section>

      <div className="registro-avance__grilla">
        <div className="registro-avance__columna">
          <section className="tarjeta registro-avance__panel">
            <h2 id="estado-avance-titulo">Estado de la tarea</h2>
            <fieldset className="registro-avance__estados" disabled={bloqueado} aria-labelledby="estado-avance-titulo" aria-describedby="estado-avance-ayuda">
              {OPCIONES_ESTADO_AVANCE.map((opcion) => <label key={opcion.id} className={`registro-avance__estado ${nuevoEstado === opcion.id ? 'registro-avance__estado--seleccionado' : ''}`}>
                <input type="radio" name="estadoAvance" value={opcion.id} checked={nuevoEstado === opcion.id} disabled={!permiteEstadoAvance(tarea.estado, opcion.id)} onChange={() => setNuevoEstado(opcion.id)} />
                <span><strong>{opcion.titulo}</strong><small>{opcion.detalle}</small></span>
              </label>)}
            </fieldset>
            <p id="estado-avance-ayuda" className="registro-avance__ayuda">Solo están disponibles el estado actual y las transiciones permitidas. El bloqueo se calcula automáticamente por dependencias.</p>
            {tarea.estado === 'BLOQUEADA' && <div className="registro-avance__nota registro-avance__nota--bloqueada">Dependencias: {tarea.dependencias.map(codigoTarea).join(', ') || 'Consulta la planificación'}. Puedes documentar el impedimento y registrar el tiempo invertido, pero no desbloquear la tarea manualmente.</div>}
            {nuevoEstado === 'COMPLETADA' && <div className="registro-avance__nota">Al guardar, la tarea se marcará como completada. Describe el resultado y las verificaciones realizadas.</div>}
          </section>

          <section className="tarjeta registro-avance__panel">
            <h2 id="horas-avance-titulo">Horas invertidas en esta sesión</h2>
            <p className="registro-avance__ayuda">Entre 0.5 y 16 horas, en pasos de 30 minutos.</p>
            <fieldset className="registro-avance__tiempo" disabled={bloqueado} aria-labelledby="horas-avance-titulo">
              <div className="registro-avance__contador">
                <button type="button" aria-label="Disminuir tiempo en 30 minutos" disabled={horas <= MIN_HORAS_AVANCE} onClick={() => setHoras((valor) => Math.max(MIN_HORAS_AVANCE, valor - 0.5))}>−</button>
                <label><span className="sr-only">Horas de la sesión</span><input type="number" value={horas} min={MIN_HORAS_AVANCE} max={MAX_HORAS_AVANCE} step="0.5" required aria-describedby="horas-avance-error" aria-invalid={Boolean(errores.horas)} onChange={(event) => setHoras(event.target.value === '' ? '' : Number(event.target.value))} /><small>horas</small></label>
                <button type="button" aria-label="Aumentar tiempo en 30 minutos" disabled={horas >= MAX_HORAS_AVANCE} onClick={() => setHoras((valor) => Math.min(MAX_HORAS_AVANCE, Number(valor) + 0.5))}>+</button>
              </div>
              <div className="registro-avance__rapidos"><span>Rápido:</span>{[1, 2.5, 4, 6].map((valor) => <button key={valor} type="button" aria-pressed={horas === valor} onClick={() => setHoras(valor)}>{horasTexto(valor)} h</button>)}</div>
            </fieldset>
            <p id="horas-avance-error" className="campo__error">{errores.horas}</p>
          </section>

          <section className="tarjeta registro-avance__autor">
            <span className="avatar avatar--chico" aria-hidden="true">{iniciales(usuario.nombre)}</span>
            <div><strong>{usuario.nombre}</strong><small>{usuario.email}</small><p>Responsable de la tarea · Usuario autenticado</p></div>
          </section>
        </div>

        <div className="registro-avance__columna">
          <section className="tarjeta registro-avance__panel">
            <label className="campo" htmlFor="nota-avance"><span className="registro-avance__titulo-campo">Descripción del trabajo realizado</span>
              <textarea id="nota-avance" value={nota} disabled={bloqueado} minLength={MIN_NOTA_AVANCE} maxLength={MAX_NOTA_AVANCE} required rows="7" aria-describedby="nota-avance-ayuda" aria-invalid={nota.length > 0 && Boolean(errores.nota)} onChange={(event) => setNota(event.target.value)} placeholder="Describe lo que realizaste, los resultados, las pruebas o el impedimento encontrado en esta sesión…" />
            </label>
            <div id="nota-avance-ayuda" className="registro-avance__descripcion-pie"><span>{nota.trim().length >= MIN_NOTA_AVANCE ? '✓ Longitud válida' : `Mínimo ${MIN_NOTA_AVANCE} caracteres, sin contar espacios al inicio y al final`}</span><span>{nota.length} / {MAX_NOTA_AVANCE}</span></div>
          </section>

          <section className="tarjeta registro-avance__panel">
            <div className="registro-avance__historial-cab"><h2>Historial de avances</h2><span>{historial.registros.length} {historial.registros.length === 1 ? 'registro' : 'registros'}</span></div>
            {historial.registros.length === 0 ? <p className="registro-avance__ayuda">Aún no hay avances registrados para esta tarea.</p> : <ol className="registro-avance__historial">{historial.registros.map((registro) => <li key={registro.id}>
              <span className="registro-avance__historial-icono" aria-hidden="true">◷</span>
              <div><strong>{horasTexto(registro.minutos / 60)} h registradas</strong><time dateTime={registro.fecha}>{fechaRegistro(registro.fecha)}</time><small>{registro.usuarioNombre}</small><p>{registro.nota || 'Sin descripción.'}</p></div>
            </li>)}</ol>}
          </section>
        </div>
      </div>

      <section className="tarjeta registro-avance__acciones">
        <div><button type="button" className="btn btn--ghost" disabled={guardando} onClick={salir}>← Volver a mis tareas</button><button type="button" className="enlace-peligro" disabled={bloqueado} onClick={descartar}>Descartar cambios</button></div>
        <div><p>Total acumulado tras guardar<strong>{horasTexto(tiempo.proyectadas)} horas</strong></p>
          {guardado ? <button type="button" className="btn btn--primario" disabled={!permitido} onClick={onRecargar}>Registrar otra sesión</button> : <button type="submit" className="btn btn--primario" disabled={bloqueado || Object.keys(errores).length > 0}>{guardando ? 'Guardando…' : '✓ Guardar avance'}</button>}
        </div>
      </section>
    </form>
  );
}