import { useMemo, useState } from 'react';
import { IconAlert, IconCheck } from '../../components/Icons.jsx';
import { codigoTarea, diasEntre, ESTADO_TAREA, fechaCorta, iniciales, parseFecha, PRIORIDAD } from '../../utils.js';

const FILTROS = [
  ['todas', 'Todas'],
  ['PENDIENTE', 'Pendientes'],
  ['EN_PROGRESO', 'En progreso'],
  ['COMPLETADA', 'Completadas'],
  ['BLOQUEADA', 'Bloqueadas'],
];
const ESTADO_PLAN = { PROPUESTA: 'Propuesta lista para revisión' };

function rango(tareas) {
  const ini = tareas.map((t) => parseFecha(t.fechaInicio)).filter(Boolean);
  const fin = tareas.map((t) => parseFecha(t.fechaFin)).filter(Boolean);
  if (!ini.length || !fin.length) return null;
  return diasEntre(new Date(Math.min(...ini)), new Date(Math.max(...fin)));
}

function SinPlanificacion({ requisitos, soyCreador, generando, error, onGenerar, onEditar }) {
  const completo = requisitos.every((r) => r.ok);
  return (
    <section className="tarjeta vacio-grande">
      <h2>Aún no hay planificación</h2>
      <p>La IA propone etapas, tareas, responsables, dependencias y fechas a partir de la información del proyecto y las habilidades del equipo.</p>

      {soyCreador ? (
        <>
          <ul className="requisitos" aria-label="Requisitos para generar">
            {requisitos.map((r) => (
              <li key={r.texto} className={r.ok ? 'requisito requisito--ok' : 'requisito'}>
                {r.ok ? <IconCheck width={15} height={15} /> : <IconAlert width={15} height={15} />} {r.texto}
              </li>
            ))}
            <li className="requisito requisito--nota">Al menos dos integrantes activos deben tener habilidades en su perfil (lo valida el servidor).</li>
          </ul>
          {error && <div className="alerta alerta--error" role="alert"><IconAlert width={18} height={18} /><p>{error}</p></div>}
          <div className="vacio-grande__acciones">
            {!completo && <button type="button" className="btn btn--ghost" onClick={onEditar}>Completar información</button>}
            <button type="button" className="btn btn--primario" disabled={!completo || generando} onClick={onGenerar}>
              {generando ? 'Generando planificación…' : 'Generar planificación con IA'}
            </button>
          </div>
          {generando && <p className="seccion__sub" role="status">La IA está trabajando; puede tardar hasta un minuto. No cierres esta página.</p>}
        </>
      ) : (
        <p><strong>El creador del proyecto aún no genera la planificación.</strong></p>
      )}
    </section>
  );
}

function TarjetaTarea({ tarea, nombres }) {
  const responsable = tarea.responsableEmail;
  return (
    <li className="tarea">
      <div className="tarea__cab">
        <code>{codigoTarea(tarea.id)}</code>
        <span className={`prio prio--${tarea.prioridad}`}>{PRIORIDAD[tarea.prioridad] ?? tarea.prioridad}</span>
      </div>
      <h4>{tarea.nombre}</h4>
      {tarea.descripcion && <p className="tarea__desc">{tarea.descripcion}</p>}
      <div className="tarea__meta">
        <span>{fechaCorta(tarea.fechaInicio)} — {fechaCorta(tarea.fechaFin)}</span>
        <span>{tarea.duracionDias} {tarea.duracionDias === 1 ? 'día' : 'días'}</span>
      </div>
      <p className="tarea__deps">
        {tarea.dependencias.length ? `Depende de: ${tarea.dependencias.map(codigoTarea).join(', ')}` : 'Sin dependencias'}
      </p>
      <div className="tarea__pie">
        {responsable ? (
          <div className="tarea__resp">
            <span className="avatar avatar--mini" aria-hidden="true">{iniciales(nombres[responsable] ?? responsable)}</span>
            <div><strong>{nombres[responsable] ?? responsable}</strong><small>{responsable}</small></div>
          </div>
        ) : <span className="tarea__resp tarea__resp--nadie">Sin asignar</span>}
        <span className={`estado-tarea estado-tarea--${tarea.estado}`}>{ESTADO_TAREA[tarea.estado] ?? tarea.estado}</span>
      </div>
    </li>
  );
}

function PlanMaestro({ proyecto, plan, nombres }) {
  const [filtro, setFiltro] = useState('todas');
  const [responsable, setResponsable] = useState('todos');
  const todas = useMemo(() => plan.etapas.flatMap((e) => e.tareas), [plan]);
  const completadas = todas.filter((t) => t.estado === 'COMPLETADA').length;
  const avance = todas.length ? Math.round((completadas / todas.length) * 100) : 0;
  const total = rango(todas);
  const responsables = [...new Set(todas.map((t) => t.responsableEmail).filter(Boolean))];
  const cuenta = (f) => (f === 'todas' ? todas.length : todas.filter((t) => t.estado === f).length);
  const pasa = (t) => (filtro === 'todas' || t.estado === filtro) && (responsable === 'todos' || t.responsableEmail === responsable);

  return (
    <>
      <section className="tarjeta plan-cab">
        <span className="chip chip--verde">{ESTADO_PLAN[plan.estado] ?? plan.estado}</span>
        <h2>Plan maestro: {proyecto.nombre}</h2>
        <p className="seccion__sub">Planificación generada por IA a partir de los requerimientos y las habilidades del equipo.</p>
        <dl className="resumen-plan">
          <div><dt>Duración total</dt><dd>{total ?? '—'} {total != null && 'días'}</dd></div>
          <div><dt>Etapas</dt><dd>{plan.etapas.length}</dd></div>
          <div><dt>Tareas totales</dt><dd>{todas.length}</dd></div>
          <div><dt>Avance</dt><dd>{avance}%<small>{completadas} de {todas.length} completadas</small></dd></div>
        </dl>
      </section>

      <div className="filtros">
        <div className="filtros__tabs" role="group" aria-label="Filtrar tareas por estado">
          {FILTROS.map(([clave, texto]) => (
            <button key={clave} type="button" className={clave === filtro ? 'tab tab--activa' : 'tab'} aria-pressed={clave === filtro} onClick={() => setFiltro(clave)}>
              {texto} <span className="tab__n">{cuenta(clave)}</span>
            </button>
          ))}
        </div>
        <select className="selector" value={responsable} onChange={(e) => setResponsable(e.target.value)} aria-label="Filtrar por responsable">
          <option value="todos">Todos los asignados</option>
          {responsables.map((r) => <option key={r} value={r}>{nombres[r] ?? r}</option>)}
        </select>
      </div>

      <div className="etapas">
        {plan.etapas.map((e) => {
          const visibles = e.tareas.filter(pasa);
          const dias = rango(e.tareas);
          return (
            <section key={e.id} className="etapa" aria-label={e.nombre}>
              <header className="etapa__cab">
                <div>
                  <h3>{e.nombre}</h3>
                  <small>Fase {e.orden ?? '—'}{dias != null && ` · ${dias} días`}</small>
                </div>
                <span className="contador">{visibles.length}</span>
              </header>
              {visibles.length === 0 ? <p className="etapa__vacia">Sin tareas con este filtro.</p> : (
                <ul className="tareas">{visibles.map((t) => <TarjetaTarea key={t.id} tarea={t} nombres={nombres} />)}</ul>
              )}
            </section>
          );
        })}
      </div>
    </>
  );
}

export default function Planificacion({ proyecto, plan, equipo, ...resto }) {
  const nombres = Object.fromEntries(equipo.map((m) => [m.email, m.nombre]));
  if (!plan) return <SinPlanificacion {...resto} />;
  return <PlanMaestro proyecto={proyecto} plan={plan} nombres={nombres} />;
}
