import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { obtenerMetricasProyecto } from '../../api/colab.js';
import { useAuth } from '../../auth/AuthContext.jsx';
import Alerta from '../../components/Alerta.jsx';
import EstadoProyecto from '../../components/EstadoProyecto.jsx';
import { proyectoCerrado } from '../../cicloProyecto.js';
import { avancePorIntegrante, distribucionTareas, entregaVencida, mostrarPorcentaje } from '../../metricasProyecto.js';
import { formatearFecha, iniciales, ROL } from '../../utils.js';
import './MetricasProyecto.css';

function BarraAvance({ porcentaje, etiqueta }) {
  return <progress className="metricas__progreso" max={100} value={porcentaje} aria-label={etiqueta} />;
}

function EstadoTareas({ metricas }) {
  const estados = distribucionTareas(metricas);
  return (
    <section className="tarjeta metricas__panel" aria-labelledby="metricas-estados">
      <h3 id="metricas-estados">Estado de las tareas</h3>
      <p className="seccion__sub">Distribución de las tareas de todo el proyecto.</p>
      <div className="metricas__distribucion" aria-hidden="true">
        {estados.map(({ campo, porcentaje }) => (
          <span key={campo} className={`metricas__color--${campo}`} style={{ flexBasis: `${porcentaje}%` }} />
        ))}
      </div>
      <dl className="metricas__estados">
        {estados.map(({ campo, etiqueta, cantidad, porcentaje }) => (
          <div key={campo}>
            <dt><span className={`metricas__punto metricas__color--${campo}`} aria-hidden="true" />{etiqueta}</dt>
            <dd><strong>{cantidad}</strong><small>{mostrarPorcentaje(porcentaje)}</small></dd>
          </div>
        ))}
      </dl>
    </section>
  );
}

function AvanceIntegrantes({ metricas, equipo }) {
  const integrantes = avancePorIntegrante(metricas.porResponsable, equipo);
  return (
    <section className="tarjeta metricas__panel" aria-labelledby="metricas-integrantes">
      <h3 id="metricas-integrantes">Avance por integrante</h3>
      <p className="seccion__sub">Tareas completadas sobre las asignadas a cada integrante activo.</p>
      {integrantes.length === 0 ? <p className="vacio">No hay integrantes activos para mostrar.</p> : (
        <ul className="metricas__integrantes">
          {integrantes.map(({ integranteId, nombre, rol, porcentaje, completadas, totalTareas }) => (
            <li key={integranteId}>
              <div className="metricas__integrante-cab">
                <div className="metricas__identidad">
                  <span className="avatar avatar--chico" aria-hidden="true">{iniciales(nombre)}</span>
                  <div><strong>{nombre}</strong>{ROL[rol] && <small>{ROL[rol]}</small>}</div>
                </div>
                {porcentaje !== null && <strong className="metricas__porcentaje">{mostrarPorcentaje(porcentaje)}</strong>}
              </div>
              {porcentaje === null ? <p className="metricas__subdato">Sin tareas asignadas</p> : (
                <>
                  <p className="metricas__subdato">{completadas} de {totalTareas} tareas completadas</p>
                  <BarraAvance porcentaje={porcentaje} etiqueta={`Avance de ${nombre}`} />
                </>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

export default function MetricasProyecto({ proyecto, equipo, tienePlanificacion }) {
  const { token } = useAuth();
  const [intento, setIntento] = useState(0);
  const [carga, setCarga] = useState({ estado: 'cargando' });
  const { id } = proyecto;

  useEffect(() => {
    if (!tienePlanificacion) return undefined;
    let vigente = true;
    setCarga({ estado: 'cargando' });
    obtenerMetricasProyecto(token, id).then(
      (metricas) => { if (vigente) setCarga({ estado: 'ok', metricas }); },
      (error) => { if (vigente) setCarga({ estado: 'error', error }); },
    );
    return () => { vigente = false; };
  }, [token, id, tienePlanificacion, intento]);

  const { metricas, error } = carga;
  const sinTareas = !tienePlanificacion || metricas?.totalTareas === 0;
  const sinAcceso = error?.status === 403;

  return (
    <section className="metricas" aria-labelledby="metricas-titulo">
      <div className="metricas__cabecera">
        <h2 id="metricas-titulo">Métricas del proyecto</h2>
        <p className="seccion__sub">Consulta el avance de las tareas y su distribución en el equipo.</p>
      </div>
      {sinTareas ? (
        <div className="tarjeta vacio-grande">
          <h3>Aún no hay tareas para medir</h3>
          <p>{tienePlanificacion ? 'La planificación todavía no tiene tareas.' : 'Las métricas estarán disponibles cuando el proyecto tenga una planificación con tareas.'}</p>
          <Link className="btn btn--ghost" to={`/proyectos/${id}/planificacion`}>Ver planificación</Link>
        </div>
      ) : carga.estado === 'cargando' ? (
        <p className="estado" role="status">Cargando métricas…</p>
      ) : carga.estado === 'error' ? (
        <div className="metricas__error">
          <Alerta titulo="No se pudieron cargar las métricas">
            {sinAcceso ? 'Solo los integrantes activos pueden consultar las métricas de este proyecto.' : error.message}
          </Alerta>
          {!sinAcceso && <button type="button" className="btn btn--ghost" onClick={() => setIntento((valor) => valor + 1)}>Reintentar</button>}
        </div>
      ) : (
        <>
          <div className="metricas__resumen">
            <section className="tarjeta metricas__panel metricas__resumen-principal">
              <h3>Progreso general</h3>
              <strong className="metricas__valor">{mostrarPorcentaje(metricas.porcentajeCompletadas)}</strong>
              <BarraAvance porcentaje={metricas.porcentajeCompletadas} etiqueta="Progreso general del proyecto" />
              <p>{metricas.completadas} de {metricas.totalTareas} tareas completadas</p>
            </section>
            <section className="tarjeta metricas__panel metricas__entrega">
              <h3>Entrega y estado</h3>
              <strong>{proyecto.fechaEntrega
                ? <time dateTime={proyecto.fechaEntrega}>{formatearFecha(proyecto.fechaEntrega)}</time>
                : 'Sin fecha de entrega'}</strong>
              <div className="metricas__etiquetas">
                <EstadoProyecto estado={proyecto.estado} />
                {entregaVencida(proyecto) && <span className="chip chip--rojo">Fecha de entrega vencida</span>}
              </div>
              {proyectoCerrado(proyecto.estado) && <p>Consulta del avance registrado antes del cierre.</p>}
            </section>
          </div>
          <div className="metricas__detalle">
            <EstadoTareas metricas={metricas} />
            <AvanceIntegrantes metricas={metricas} equipo={equipo} />
          </div>
        </>
      )}
    </section>
  );
}