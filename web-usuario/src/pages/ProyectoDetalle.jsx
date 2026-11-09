import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useOutletContext, useParams } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import * as api from '../api/colab.js';
import ModalInvitar from '../components/ModalInvitar.jsx';
import ModalProyecto from '../components/ModalProyecto.jsx';
import { IconPlus } from '../components/Icons.jsx';
import Informacion from './proyecto/Informacion.jsx';
import Equipo from './proyecto/Equipo.jsx';
import Planificacion from './proyecto/Planificacion.jsx';
import { formatearFecha, requisitosPlanificacion } from '../utils.js';

const TABS = ['equipo', 'planificacion'];

const MENSAJES_GENERAR = {
  400: 'No se pudo generar: faltan datos del proyecto, la IA devolvió un resultado inválido o menos de dos integrantes activos tienen habilidades en su perfil.',
  403: 'Solo el creador del proyecto puede generar la planificación.',
  409: 'La planificación ya fue generada.',
  502: 'La IA no respondió correctamente. Inténtalo de nuevo en unos minutos (en el backend, IA_PROVIDER=mock funciona sin la red de TECSUP).',
};

export default function ProyectoDetalle() {
  const { id, tab } = useParams();
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
            <h1 className="titulo">{proyecto.nombre}</h1>
            {proyecto.descripcion && <p className="proy-cab__desc">{proyecto.descripcion}</p>}
          </div>
          {soyCreador && (
            <div className="proy-cab__acciones">
              {!plan && <button type="button" className="btn btn--primario" disabled={generando} onClick={generar}>{generando ? 'Generando…' : 'Generar planificación con IA'}</button>}
              <button type="button" className="btn btn--ghost" onClick={() => setInvitando(true)}><IconPlus /> Invitar integrante</button>
              <button type="button" className="btn btn--ghost" onClick={() => setEditando(true)}>Editar proyecto</button>
            </div>
          )}
        </div>

        <dl className="meta">
          <div><dt>Líder técnico</dt><dd>{creador?.nombre ?? '—'}</dd></div>
          <div><dt>Fecha de entrega</dt><dd>{proyecto.fechaEntrega ? formatearFecha(proyecto.fechaEntrega) : 'Sin definir'}</dd></div>
          <div><dt>Equipo</dt><dd>{activos} {activos === 1 ? 'integrante activo' : 'integrantes activos'}</dd></div>
          <div><dt>Planificación</dt><dd>{plan ? 'Generada' : 'Sin generar'}</dd></div>
        </dl>

        <div className="pestanas" role="tablist" aria-label="Secciones del proyecto">
          {pestana('info', 'Información')}
          {pestana('equipo', 'Equipo', <span className="tab__n">{activos}</span>)}
          {pestana('planificacion', 'Planificación', <span className={plan ? 'chip chip--verde' : 'chip chip--gris'}>{plan ? 'Generada' : 'Pendiente'}</span>)}
        </div>
      </section>

      {activa === 'info' && <Informacion proyecto={proyecto} equipo={equipo} soyCreador={soyCreador} onVerEquipo={() => ir('/equipo')} onEditar={() => setEditando(true)} />}
      {activa === 'equipo' && <Equipo equipo={equipo} soyCreador={soyCreador} ocupado={ocupado} onInvitar={() => setInvitando(true)} onQuitar={quitar} />}
      {activa === 'planificacion' && (
        <Planificacion proyecto={proyecto} plan={plan} equipo={equipo} requisitos={requisitos} soyCreador={soyCreador} generando={generando} error={errorGenerar} onGenerar={generar} onEditar={() => setEditando(true)} />
      )}

      {invitando && <ModalInvitar proyecto={proyecto} onCerrar={() => setInvitando(false)} onInvitado={alInvitar} />}
      {editando && <ModalProyecto proyecto={proyecto} onCerrar={() => setEditando(false)} onGuardado={alGuardar} />}
    </>
  );
}
