import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useOutletContext, useParams } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import * as api from '../api/colab.js';
import AccionesProyecto from '../components/AccionesProyecto.jsx';
import EstadoProyecto from '../components/EstadoProyecto.jsx';
import ModalCicloProyecto from '../components/ModalCicloProyecto.jsx';
import ModalInvitar from '../components/ModalInvitar.jsx';
import ModalProyecto from '../components/ModalProyecto.jsx';
import { IconPlus } from '../components/Icons.jsx';
import Informacion from './proyecto/Informacion.jsx';
import Equipo from './proyecto/Equipo.jsx';
import Planificacion from './proyecto/Planificacion.jsx';
import { formatearFecha, requisitosPlanificacion } from '../utils.js';
import { accionesProyecto, proyectoCerrado } from '../cicloProyecto.js';

const TABS = ['equipo', 'planificacion'];

const MENSAJES_GENERAR = {
  400: 'No se pudo generar: faltan datos del proyecto, la IA devolvió un resultado inválido o menos de dos integrantes activos tienen habilidades en su perfil.',
  403: 'Solo el creador del proyecto puede generar la planificación.',
  409: 'La planificación ya fue generada.',
  502: 'La IA no respondió correctamente. Inténtalo de nuevo en unos minutos (en el backend, IA_PROVIDER=mock funciona sin la red de TECSUP).',
};

export default function ProyectoDetalle() {
  const { id, tab } = useParams();
  return <Detalle key={id} id={id} tab={tab} />;
}

function Detalle({ id, tab }) {
  const navigate = useNavigate();
  const { token } = useAuth();
  const { usuario, avisar } = useOutletContext();
  const [estado, setEstado] = useState('cargando');
  const [error, setError] = useState(null);
  const [proyecto, setProyecto] = useState(null);
  const [equipo, setEquipo] = useState([]);
  const [plan, setPlan] = useState(null);
  const [invitando, setInvitando] = useState(false);
  const [editando, setEditando] = useState(false);
  const [accionCiclo, setAccionCiclo] = useState(null);
  const [ocupado, setOcupado] = useState(null);
  const [generando, setGenerando] = useState(false);
  const [errorGenerar, setErrorGenerar] = useState(null);
  const activa = TABS.includes(tab) ? tab : 'info';
  const ir = (destino = '') => navigate(`/proyectos/${id}${destino}`);

  const cargar = useCallback(async () => {
    setEstado('cargando');
    try {
      const [p, e, pl] = await Promise.all([
        api.verProyecto(token, id),
        api.verEquipo(token, id),
        api.obtenerPlanificacion(token, id).catch((err) => (err.status === 404 ? null : Promise.reject(err))),
      ]);
      setProyecto(p);
      setEquipo(e);
      setPlan(pl);
      setEstado('ok');
    } catch (err) {
      setError(err);
      setEstado('error');
    }
  }, [token, id]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  if (estado === 'cargando') return <p className="estado">Cargando proyecto…</p>;

  if (estado === 'error') {
    const sinAcceso = error.status === 403 || error.status === 404;
    return (
      <div className="estado estado--error" role="alert">
        <p>{sinAcceso ? 'No encontramos este proyecto o no tienes acceso a él.' : error.message}</p>
        {sinAcceso ? <Link className="btn btn--primario" to="/proyectos">Volver a mis proyectos</Link>
          : <button type="button" className="btn btn--primario" onClick={cargar}>Reintentar</button>}
      </div>
    );
  }

  const creador = equipo.find((m) => m.rol === 'CREADOR');
  const soyCreador = creador?.email === usuario.email;
  const activos = equipo.filter((m) => m.estado === 'ACTIVO').length;
  const requisitos = requisitosPlanificacion(proyecto, activos);
  const acciones = accionesProyecto(proyecto, soyCreador);
  const puedeGestionarEquipo = acciones.includes('editar');
  const cerrado = proyectoCerrado(proyecto.estado);
  const fechaCierre = proyecto.estado === 'FINALIZADO' ? proyecto.fechaFinalizacion : proyecto.fechaCancelacion;

  function abrirAccion(accion) {
    if (accion === 'editar') setEditando(true);
    else setAccionCiclo(accion);
  }

  async function confirmarAccion() {
    try {
      if (accionCiclo === 'cancelar') {
        const cancelado = await api.cancelarProyecto(token, id);
        setProyecto(cancelado);
        // El POST devuelve el proyecto, no la planificación que se está mostrando.
        setPlan((actual) => actual && { ...actual, estado: 'CANCELADA' });
        setAccionCiclo(null);
        avisar('Proyecto cancelado. El historial sigue disponible.', 'ok');
      } else if (accionCiclo === 'eliminar') {
        await api.eliminarProyecto(token, id);
        avisar('Proyecto eliminado definitivamente.', 'ok');
        navigate('/proyectos', { replace: true });
      }
    } catch (err) {
      if ([403, 404, 409].includes(err.status)) {
        setAccionCiclo(null);
        avisar(err.status === 409 ? 'El estado del proyecto cambió. Revisa las acciones disponibles.'
          : err.status === 404 ? 'El proyecto ya no está disponible.' : err.message);
        await cargar();
        return;
      }
      throw err;
    }
  }

  async function quitar(m) {
    const pendiente = m.estado === 'PENDIENTE';
    if (!window.confirm(pendiente ? `¿Cancelar la invitación a ${m.nombre}?` : `¿Eliminar a ${m.nombre} del equipo?`)) return;
    setOcupado(m.integranteId);
    try {
      await api.eliminarIntegrante(token, id, m.integranteId);
      setEquipo((lista) => lista.filter((x) => x.integranteId !== m.integranteId));
      avisar(pendiente ? 'Invitación cancelada.' : `${m.nombre} fue eliminado del equipo.`, 'ok');
    } catch (err) {
      avisar(err.message);
    } finally {
      setOcupado(null);
    }
  }

  async function alInvitar(email) {
    setInvitando(false);
    avisar(`Invitación enviada a ${email}.`, 'ok');
    try {
      setEquipo(await api.verEquipo(token, id));
    } catch (err) {
      avisar(err.message);
    }
  }

  function alGuardar(guardado) {
    setEditando(false);
    setProyecto(guardado);
    avisar('Proyecto actualizado.', 'ok');
  }

  async function generar() {
    if (requisitos.some((r) => !r.ok)) return ir('/planificacion');
    ir('/planificacion');
    setGenerando(true);
    setErrorGenerar(null);
    try {
      setPlan(await api.generarPlanificacion(token, id));
      avisar('Planificación generada.', 'ok');
      await cargar();
    } catch (err) {
      setErrorGenerar(MENSAJES_GENERAR[err.status] ?? err.message);
      if (err.status === 409) cargar();
    } finally {
      setGenerando(false);
    }
  }

  const pestana = (clave, etiqueta, extra) => {
    const seleccionada = activa === clave;
    return (
      <Link role="tab" aria-selected={seleccionada} className={seleccionada ? 'pestana pestana--activa' : 'pestana'} to={`/proyectos/${id}${clave === 'info' ? '' : `/${clave}`}`}>
        {etiqueta} {extra}
      </Link>
    );
  };

  return (
    <>
      <section className="tarjeta proy-cab">
        <div className="proy-cab__fila">
          <div>
            <Link to="/proyectos" className="volver">← Mis proyectos</Link>
            <div className="proy-cab__titulo">
              <h1 className="titulo">{proyecto.nombre}</h1>
              <EstadoProyecto estado={proyecto.estado} />
            </div>
            {proyecto.descripcion && <p className="proy-cab__desc">{proyecto.descripcion}</p>}
          </div>
          <div className="proy-cab__acciones">
            {plan && <Link className="btn btn--ghost" to={`/proyectos/${id}/mis-tareas`}>Mis tareas</Link>}
            {puedeGestionarEquipo && (
              <>
                {!plan && <button type="button" className="btn btn--primario" disabled={generando} onClick={generar}>{generando ? 'Generando…' : 'Generar planificación con IA'}</button>}
                <button type="button" className="btn btn--ghost" disabled={generando} onClick={() => setInvitando(true)}><IconPlus /> Invitar integrante</button>
              </>
            )}
            <AccionesProyecto acciones={acciones} bloqueado={generando} onAccion={abrirAccion} />
          </div>
        </div>

        <dl className="meta">
          <div><dt>Líder técnico</dt><dd>{creador?.nombre ?? '—'}</dd></div>
          <div><dt>Fecha de entrega</dt><dd>{proyecto.fechaEntrega ? formatearFecha(proyecto.fechaEntrega) : 'Sin definir'}</dd></div>
          <div><dt>Equipo</dt><dd>{activos} {activos === 1 ? 'integrante activo' : 'integrantes activos'}</dd></div>
          <div><dt>Planificación</dt><dd>{plan ? 'Generada' : 'Sin generar'}</dd></div>
          {cerrado && fechaCierre && (
            <div><dt>{proyecto.estado === 'FINALIZADO' ? 'Fecha de finalización' : 'Fecha de cancelación'}</dt>
              <dd><time dateTime={fechaCierre}>{formatearFecha(fechaCierre.slice(0, 10))}</time></dd></div>
          )}
        </dl>
        {cerrado && <p className="proy-cab__solo-lectura">El proyecto está en modo de solo lectura. Puedes consultar la planificación, las tareas y los avances registrados.</p>}

        <div className="pestanas" role="tablist" aria-label="Secciones del proyecto">
          {pestana('info', 'Información')}
          {pestana('equipo', 'Equipo', <span className="tab__n">{activos}</span>)}
          {pestana('planificacion', 'Planificación', <span className={plan ? 'chip chip--verde' : 'chip chip--gris'}>{plan ? 'Generada' : 'Pendiente'}</span>)}
        </div>
      </section>

      {activa === 'info' && <Informacion proyecto={proyecto} equipo={equipo} onVerEquipo={() => ir('/equipo')} />}
      {activa === 'equipo' && <Equipo equipo={equipo} soyCreador={puedeGestionarEquipo && !generando} ocupado={ocupado} onInvitar={() => setInvitando(true)} onQuitar={quitar} />}
      {activa === 'planificacion' && (
        <Planificacion proyecto={proyecto} plan={plan} equipo={equipo} requisitos={requisitos} soyCreador={soyCreador} generando={generando} error={errorGenerar} onGenerar={generar} />
      )}

      {invitando && <ModalInvitar proyecto={proyecto} onCerrar={() => setInvitando(false)} onInvitado={alInvitar} />}
      {editando && <ModalProyecto proyecto={proyecto} onCerrar={() => setEditando(false)} onGuardado={alGuardar} />}
      {accionCiclo && <ModalCicloProyecto accion={accionCiclo} proyecto={proyecto} onCerrar={() => setAccionCiclo(null)} onConfirmar={confirmarAccion} />}
    </>
  );
}
