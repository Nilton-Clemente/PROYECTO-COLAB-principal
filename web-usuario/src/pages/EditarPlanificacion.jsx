import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useOutletContext, useParams } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import * as api from '../api/colab.js';
import { codigoTarea, diasEntre, fechaCorta, iniciales, parseFecha, PRIORIDAD } from '../utils.js';
import { IconPlus, IconTrash } from '../components/Icons.jsx';

let siguienteTemporal = 0;
const temporal = (tipo) => `${tipo}-nuevo-${++siguienteTemporal}`;
const esPersistido = (id) => typeof id === 'number';
const copiar = (valor) => JSON.parse(JSON.stringify(valor));

function rango(tareas) {
  const inicio = tareas.map((t) => parseFecha(t.fechaInicio)).filter(Boolean);
  const fin = tareas.map((t) => parseFecha(t.fechaFin)).filter(Boolean);
  if (!inicio.length || !fin.length) return null;
  return diasEntre(new Date(Math.min(...inicio)), new Date(Math.max(...fin)));
}

function prepararPlan(plan, tareasDetalle) {
  const detallePorId = new Map(tareasDetalle.map((t) => [t.id, t]));
  return plan.etapas.map((etapa, indiceEtapa) => ({
    id: etapa.id,
    nombre: etapa.nombre,
    orden: etapa.orden ?? indiceEtapa + 1,
    tareas: etapa.tareas.map((tarea, indiceTarea) => {
      const detalle = detallePorId.get(tarea.id) ?? {};
      return {
        ...tarea,
        ...detalle,
        descripcion: tarea.descripcion ?? '',
        etapaId: etapa.id,
        orden: detalle.orden ?? indiceTarea + 1,
        dependencias: detalle.dependencias ?? tarea.dependencias ?? [],
      };
    }),
  }));
}

function EntradaEtapa({ etapa, activa, cantidadDias, onSeleccionar }) {
  return (
    <button type="button" className={activa ? 'editor-plan__pestana editor-plan__pestana--activa' : 'editor-plan__pestana'} onClick={onSeleccionar}>
      <span className="editor-plan__punto" />
      <span>
        <strong>Fase {etapa.orden}: {etapa.nombre}</strong>
        <small>{etapa.tareas.length} {etapa.tareas.length === 1 ? 'tarea' : 'tareas'} · {cantidadDias} días estimados</small>
      </span>
    </button>
  );
}

function CampoTarea({ etiqueta, children }) {
  return <label className="editor-plan__campo"><span>{etiqueta}</span>{children}</label>;
}

export default function EditarPlanificacion() {
  const { id: proyectoId } = useParams();
  const navigate = useNavigate();
  const { token } = useAuth();
  const { usuario, avisar } = useOutletContext();
  const [estado, setEstado] = useState('cargando');
  const [error, setError] = useState('');
  const [proyecto, setProyecto] = useState(null);
  const [equipo, setEquipo] = useState([]);
  const [etapas, setEtapas] = useState([]);
  const [original, setOriginal] = useState([]);
  const [etapaActivaId, setEtapaActivaId] = useState(null);
  const [guardando, setGuardando] = useState(false);

  const cargar = useCallback(async () => {
    setEstado('cargando');
    setError('');
    try {
      const [p, e, plan, tareasDetalle] = await Promise.all([
        api.verProyecto(token, proyectoId),
        api.verEquipo(token, proyectoId),
        api.obtenerPlanificacion(token, proyectoId),
        api.listarTareas(token, proyectoId),
      ]);
      const creador = e.find((integrante) => integrante.rol === 'CREADOR');
      if (creador?.email !== usuario.email) {
        setError('Solo el creador del proyecto puede editar la planificación.');
        setEstado('sin-permiso');
        return;
      }
      const datos = prepararPlan(plan, tareasDetalle);
      setProyecto(p);
      setEquipo(e.filter((integrante) => integrante.estado === 'ACTIVO'));
      setEtapas(datos);
      setOriginal(copiar(datos));
      setEtapaActivaId(datos[0]?.id ?? null);
      setEstado('ok');
    } catch (err) {
      setError(err.status === 403 ? 'No tienes permiso para editar esta planificación.' : err.message);
      setEstado('error');
    }
  }, [proyectoId, token, usuario.email]);

  useEffect(() => { cargar(); }, [cargar]);

  const etapaActiva = etapas.find((etapa) => etapa.id === etapaActivaId) ?? etapas[0];
  const todasLasTareas = useMemo(() => etapas.flatMap((etapa) => etapa.tareas), [etapas]);
  const diasActivos = etapaActiva ? etapaActiva.tareas.reduce((total, tarea) => total + Number(tarea.duracionDias || 0), 0) : 0;
  const rangoActivo = etapaActiva ? rango(etapaActiva.tareas) : null;
  const responsables = new Set(etapaActiva?.tareas.map((tarea) => tarea.responsableId).filter(Boolean));

  function actualizarEtapa(etapaId, cambios) {
    setEtapas((actual) => actual.map((etapa) => etapa.id === etapaId ? { ...etapa, ...cambios } : etapa));
  }

  function actualizarTarea(tareaId, cambios) {
    setEtapas((actual) => actual.map((etapa) => ({
      ...etapa,
      tareas: etapa.tareas.map((tarea) => tarea.id === tareaId ? { ...tarea, ...cambios } : tarea),
    })));
  }

  function nuevaEtapa() {
    const id = temporal('etapa');
    const etapa = { id, nombre: `Nueva etapa ${etapas.length + 1}`, orden: etapas.length + 1, tareas: [] };
    setEtapas((actual) => [...actual, etapa]);
    setEtapaActivaId(id);
  }

  function eliminarEtapaActiva() {
    if (!etapaActiva) return;
    if (!window.confirm(`¿Eliminar la etapa “${etapaActiva.nombre}” y todas sus tareas?`)) return;
    const restantes = etapas.filter((etapa) => etapa.id !== etapaActiva.id).map((etapa, indice) => ({ ...etapa, orden: indice + 1 }));
    setEtapas(restantes);
    setEtapaActivaId(restantes[0]?.id ?? null);
  }

  function nuevaTarea() {
    if (!etapaActiva) return;
    const responsable = equipo[0] ?? null;
    const tarea = {
      id: temporal('tarea'),
      nombre: `Nueva tarea ${etapaActiva.tareas.length + 1}`,
      descripcion: '',
      etapaId: etapaActiva.id,
      responsableId: responsable?.integranteId ?? null,
      responsableEmail: responsable?.email ?? null,
      prioridad: 'MEDIA',
      estado: 'PENDIENTE',
      duracionDias: 1,
      orden: etapaActiva.tareas.length + 1,
      dependencias: [],
      fechaInicio: null,
      fechaFin: null,
    };
    actualizarEtapa(etapaActiva.id, { tareas: [...etapaActiva.tareas, tarea] });
  }

  function eliminarTarea(tareaId) {
    const dependientes = todasLasTareas.filter((tarea) => tarea.dependencias.includes(tareaId));
    if (dependientes.length && !window.confirm('Esta tarea es dependencia de otras tareas. Al eliminarla se quitará también esa relación. ¿Continuar?')) return;
    setEtapas((actual) => actual.map((etapa) => ({
      ...etapa,
      tareas: etapa.tareas
        .filter((tarea) => tarea.id !== tareaId)
        .map((tarea, indice) => ({ ...tarea, orden: indice + 1, dependencias: tarea.dependencias.filter((id) => id !== tareaId) })),
    })));
  }

  function moverTarea(tareaId, destinoId) {
    const destino = etapas.find((etapa) => String(etapa.id) === String(destinoId));
    if (!destino || destino.id === etapaActiva.id) return;
    const tarea = etapaActiva.tareas.find((item) => item.id === tareaId);
    setEtapas((actual) => actual.map((etapa) => {
      if (etapa.id === etapaActiva.id) return { ...etapa, tareas: etapa.tareas.filter((item) => item.id !== tareaId).map((item, indice) => ({ ...item, orden: indice + 1 })) };
      if (etapa.id === destino.id) return { ...etapa, tareas: [...etapa.tareas, { ...tarea, etapaId: destino.id, orden: etapa.tareas.length + 1 }] };
      return etapa;
    }));
  }

  function cambiarDependencias(tareaId, valores) {
    const ids = [...valores]
      .map((opcion) => (/^\d+$/.test(opcion.value) ? Number(opcion.value) : opcion.value))
      .filter((id) => id !== tareaId);
    actualizarTarea(tareaId, { dependencias: ids });
  }

  function descartar() {
    if (!window.confirm('¿Descartar todos los cambios no guardados?')) return;
    const restaurado = copiar(original);
    setEtapas(restaurado);
    setEtapaActivaId(restaurado[0]?.id ?? null);
  }

  function validar() {
    if (!etapas.length) return 'Agrega al menos una etapa.';
    for (const etapa of etapas) {
      if (!etapa.nombre.trim()) return 'Cada etapa debe tener un nombre.';
      for (const tarea of etapa.tareas) {
        if (!tarea.nombre.trim()) return 'Cada tarea debe tener un título.';
        if (!Number.isInteger(Number(tarea.duracionDias)) || Number(tarea.duracionDias) < 1) return `La duración de “${tarea.nombre}” debe ser al menos un día.`;
        if (!tarea.prioridad) return `Selecciona una prioridad para “${tarea.nombre}”.`;
        if (tarea.dependencias.includes(tarea.id)) return `“${tarea.nombre}” no puede depender de sí misma.`;
      }
    }
    return null;
  }

  async function guardar() {
    const mensaje = validar();
    if (mensaje) return avisar(mensaje);
    setGuardando(true);
    try {
      const originalesPorId = new Map(original.map((etapa) => [etapa.id, etapa]));
      const tareasOriginales = original.flatMap((etapa) => etapa.tareas);
      const tareasActuales = etapas.flatMap((etapa) => etapa.tareas);
      const idsEtapasEliminadas = original.filter((etapa) => !etapas.some((actual) => actual.id === etapa.id)).map((etapa) => etapa.id);
      const idsTareasActuales = new Set(tareasActuales.filter((tarea) => esPersistido(tarea.id)).map((tarea) => tarea.id));
      const idsTareasEnEtapasEliminadas = new Set(original.filter((etapa) => idsEtapasEliminadas.includes(etapa.id)).flatMap((etapa) => etapa.tareas.map((tarea) => tarea.id)));

      // Primero quitar relaciones existentes de las tareas que se conservarán.
      await Promise.all(tareasOriginales
        .filter((tarea) => idsTareasActuales.has(tarea.id) && !idsTareasEnEtapasEliminadas.has(tarea.id))
        .flatMap((tarea) => tarea.dependencias.map((predecesoraId) => api.quitarDependencia(token, proyectoId, tarea.id, predecesoraId))));

      // Las tareas fuera de etapas eliminadas se borran explícitamente; las demás se eliminan en cascada con su etapa.
      await Promise.all(tareasOriginales
        .filter((tarea) => !idsTareasActuales.has(tarea.id) && !idsTareasEnEtapasEliminadas.has(tarea.id))
        .map((tarea) => api.eliminarTarea(token, proyectoId, tarea.id)));
      await Promise.all(idsEtapasEliminadas.map((etapaId) => api.eliminarEtapa(token, proyectoId, etapaId)));

      const idsEtapa = new Map();
      for (const [indice, etapa] of etapas.entries()) {
        const datos = { nombre: etapa.nombre.trim(), orden: indice + 1 };
        if (esPersistido(etapa.id)) {
          await api.editarEtapa(token, proyectoId, etapa.id, datos);
          idsEtapa.set(etapa.id, etapa.id);
        } else {
          const creada = await api.crearEtapa(token, proyectoId, datos);
          idsEtapa.set(etapa.id, creada.id);
        }
      }

      const idsTarea = new Map();
      for (const etapa of etapas) {
        const etapaId = idsEtapa.get(etapa.id);
        for (const [indice, tarea] of etapa.tareas.entries()) {
          const datos = {
            nombre: tarea.nombre.trim(),
            descripcion: tarea.descripcion.trim() || null,
            etapaId,
            responsableId: tarea.responsableId || null,
            prioridad: tarea.prioridad,
            duracionDias: Number(tarea.duracionDias),
            orden: indice + 1,
          };
          if (esPersistido(tarea.id)) {
            await api.editarTarea(token, proyectoId, tarea.id, datos);
            idsTarea.set(tarea.id, tarea.id);
          } else {
            const creada = await api.crearTarea(token, proyectoId, datos);
            idsTarea.set(tarea.id, creada.id);
          }
        }
      }

      for (const tarea of tareasActuales) {
        const tareaId = idsTarea.get(tarea.id);
        for (const predecesora of tarea.dependencias) {
          const predecesoraId = idsTarea.get(predecesora);
          if (tareaId && predecesoraId) await api.agregarDependencia(token, proyectoId, tareaId, predecesoraId);
        }
      }

      avisar('Planificación actualizada.', 'ok');
      navigate(`/proyectos/${proyectoId}/planificacion`);
    } catch (err) {
      avisar(`No se pudo guardar la planificación. ${err.message}`);
    } finally {
      setGuardando(false);
    }
  }

  if (estado === 'cargando') return <p className="estado">Cargando editor de planificación…</p>;
  if (estado !== 'ok') return (
    <div className="estado estado--error" role="alert">
      <p>{error}</p>
      {estado === 'error' ? <button type="button" className="btn btn--primario" onClick={cargar}>Reintentar</button> : <Link className="btn btn--primario" to={`/proyectos/${proyectoId}/planificacion`}>Volver a la planificación</Link>}
    </div>
  );

  return (
    <section className="editor-plan">
      <div className="editor-plan__cabecera">
        <div>
          <Link className="volver" to={`/proyectos/${proyectoId}/planificacion`}>← Volver al plan maestro</Link>
          <h1>Editar planificación y fases de trabajo</h1>
          <p>Gestiona las etapas, tareas, responsables, estimaciones y dependencias de <strong>{proyecto.nombre}</strong>.</p>
        </div>
        <div className="editor-plan__acciones">
          <button type="button" className="btn btn--ghost" disabled={guardando} onClick={descartar}>Descartar cambios</button>
          <button type="button" className="btn btn--primario" disabled={guardando} onClick={guardar}>{guardando ? 'Guardando…' : 'Guardar planificación'}</button>
        </div>
      </div>

      <div className="editor-plan__fases" aria-label="Etapas de la planificación">
        <div className="editor-plan__lista-fases">
          {etapas.map((etapa) => <EntradaEtapa key={etapa.id} etapa={etapa} activa={etapa.id === etapaActiva?.id} cantidadDias={etapa.tareas.reduce((total, tarea) => total + Number(tarea.duracionDias || 0), 0)} onSeleccionar={() => setEtapaActivaId(etapa.id)} />)}
        </div>
        <button type="button" className="btn btn--ghost btn--chico" disabled={guardando} onClick={nuevaEtapa}><IconPlus /> Nueva etapa</button>
      </div>

      {!etapaActiva ? (
        <section className="tarjeta vacio-grande">
          <h2>La planificación no tiene etapas</h2>
          <p>Crea una etapa para empezar a organizar las tareas.</p>
          <button type="button" className="btn btn--primario" onClick={nuevaEtapa}><IconPlus /> Crear etapa</button>
        </section>
      ) : (
        <>
          <section className="tarjeta editor-plan__etapa">
            <div className="editor-plan__etapa-datos">
              <span className="editor-plan__codigo">Fase {etapaActiva.orden}</span>
              <label className="editor-plan__nombre-etapa">
                <span>Nombre de la etapa</span>
                <input value={etapaActiva.nombre} onChange={(e) => actualizarEtapa(etapaActiva.id, { nombre: e.target.value })} />
              </label>
              <p>Las fechas se calculan automáticamente a partir de las tareas generadas. {rangoActivo != null && `Rango actual: ${rangoActivo} días.`}</p>
            </div>
            <dl className="editor-plan__metas">
              <div><dt>Tareas</dt><dd>{etapaActiva.tareas.length}</dd></div>
              <div><dt>Duración estimada</dt><dd>{diasActivos} días</dd></div>
              <div><dt>Ventana actual</dt><dd>{etapaActiva.tareas.length ? `${fechaCorta(etapaActiva.tareas[0]?.fechaInicio)} — ${fechaCorta(etapaActiva.tareas.at(-1)?.fechaFin)}` : 'Sin tareas'}</dd></div>
              <button type="button" className="btn btn--peligro btn--chico" disabled={guardando} onClick={eliminarEtapaActiva}><IconTrash /> Eliminar</button>
            </dl>
          </section>

          <div className="editor-plan__contenido">
            <div className="editor-plan__tareas">
              <div className="editor-plan__seccion-cab">
                <div><h2>Tareas de la fase ({etapaActiva.tareas.length})</h2><p>Los cambios se guardan al confirmar la planificación.</p></div>
                <span className="chip chip--verde">{etapaActiva.tareas.filter((tarea) => tarea.responsableId).length} asignadas</span>
              </div>

              {etapaActiva.tareas.map((tarea) => (
                <article className="tarjeta editor-plan__tarea" key={tarea.id}>
                  <div className="editor-plan__tarea-cab">
                    <div><code>{esPersistido(tarea.id) ? codigoTarea(tarea.id) : 'Nueva tarea'}</code><span className="estado-tarea">{tarea.estado === 'PENDIENTE' ? 'Pendiente' : tarea.estado}</span></div>
                    <button type="button" className="icono-btn" disabled={guardando} onClick={() => eliminarTarea(tarea.id)} aria-label={`Eliminar ${tarea.nombre}`}><IconTrash /></button>
                  </div>

                  <CampoTarea etiqueta="Título de la tarea"><input value={tarea.nombre} onChange={(e) => actualizarTarea(tarea.id, { nombre: e.target.value })} /></CampoTarea>
                  <CampoTarea etiqueta="Descripción"><textarea rows="2" value={tarea.descripcion} onChange={(e) => actualizarTarea(tarea.id, { descripcion: e.target.value })} /></CampoTarea>

                  <div className="editor-plan__grilla">
                    <CampoTarea etiqueta="Responsable">
                      <select value={tarea.responsableId ?? ''} onChange={(e) => {
                        const miembro = equipo.find((item) => item.integranteId === Number(e.target.value));
                        actualizarTarea(tarea.id, { responsableId: miembro?.integranteId ?? null, responsableEmail: miembro?.email ?? null });
                      }}>
                        <option value="">Sin asignar</option>
                        {equipo.map((miembro) => <option key={miembro.integranteId} value={miembro.integranteId}>{miembro.nombre} · {miembro.rol === 'CREADOR' ? 'Creador' : 'Integrante'}</option>)}
                      </select>
                    </CampoTarea>
                    <CampoTarea etiqueta="Prioridad">
                      <select value={tarea.prioridad} onChange={(e) => actualizarTarea(tarea.id, { prioridad: e.target.value })}>
                        {Object.entries(PRIORIDAD).map(([valor, texto]) => <option key={valor} value={valor}>{texto}</option>)}
                      </select>
                    </CampoTarea>
                    <CampoTarea etiqueta="Duración (días)"><input type="number" min="1" max="90" value={tarea.duracionDias} onChange={(e) => actualizarTarea(tarea.id, { duracionDias: e.target.value })} /></CampoTarea>
                    <CampoTarea etiqueta="Mover a etapa">
                      <select value={etapaActiva.id} onChange={(e) => moverTarea(tarea.id, e.target.value)}>
                        {etapas.map((etapa) => <option key={etapa.id} value={etapa.id}>Fase {etapa.orden}: {etapa.nombre}</option>)}
                      </select>
                    </CampoTarea>
                  </div>

                  <CampoTarea etiqueta="Dependencias (selecciona una o varias tareas predecesoras)">
                    <select multiple value={tarea.dependencias.map(String)} onChange={(e) => cambiarDependencias(tarea.id, e.target.selectedOptions)}>
                      {todasLasTareas.filter((opcion) => opcion.id !== tarea.id).map((opcion) => <option key={opcion.id} value={opcion.id}>{esPersistido(opcion.id) ? codigoTarea(opcion.id) : 'Nueva'} — {opcion.nombre}</option>)}
                    </select>
                  </CampoTarea>
                </article>
              ))}

              <button type="button" className="editor-plan__agregar" disabled={guardando} onClick={nuevaTarea}><IconPlus /> Agregar nueva tarea a esta fase</button>
            </div>

            <aside className="editor-plan__lateral">
              <section className="tarjeta editor-plan__resumen">
                <h2>Resumen de la fase</h2>
                <dl><div><dt>Total de tareas</dt><dd>{etapaActiva.tareas.length}</dd></div><div><dt>Duración estimada</dt><dd>{diasActivos} días</dd></div><div><dt>Responsables</dt><dd>{responsables.size}</dd></div></dl>
                <p>{etapaActiva.tareas.length ? 'Revisa las dependencias antes de guardar para conservar el orden correcto.' : 'Agrega tareas para empezar esta etapa.'}</p>
              </section>
              <section className="tarjeta editor-plan__equipo">
                <h2>Equipo activo</h2>
                {equipo.map((miembro) => {
                  const asignadas = todasLasTareas.filter((tarea) => tarea.responsableId === miembro.integranteId).length;
                  return <div key={miembro.integranteId}><span className="avatar avatar--mini">{iniciales(miembro.nombre)}</span><p><strong>{miembro.nombre}</strong><small>{miembro.rol === 'CREADOR' ? 'Creador' : 'Integrante'} · {asignadas} {asignadas === 1 ? 'tarea' : 'tareas'}</small></p></div>;
                })}
              </section>
              <p className="editor-plan__nota">Las fechas de inicio y fin las calcula el servidor según la duración, el orden y las dependencias.</p>
            </aside>
          </div>
        </>
      )}
    </section>
  );
}