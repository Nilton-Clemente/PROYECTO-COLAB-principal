import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useOutletContext } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import * as api from '../api/colab.js';
import Avatares from '../components/Avatares.jsx';
import ModalProyecto from '../components/ModalProyecto.jsx';
import { IconPlus, IconSearch } from '../components/Icons.jsx';
import { formatearFecha, parseFecha, ROL } from '../utils.js';

const FILTROS = [
  ['todos', 'Todos'],
  ['CREADOR', 'Creados por mí'],
  ['INTEGRANTE', 'Como integrante'],
];

function Estadistica({ valor, etiqueta }) {
  return (
    <div className="estadistica">
      <span>{etiqueta}</span>
      <strong>{valor}</strong>
    </div>
  );
}

function TarjetaProyecto({ proyecto }) {
  const activos = proyecto.equipo.filter((m) => m.estado === 'ACTIVO');
  const techs = proyecto.tecnologias;
  return (
    <li className="proyecto">
      <div className="proyecto__etiquetas">
        <span className="chip chip--verde">{proyecto.tipo === 'SOFTWARE' ? 'Software' : proyecto.tipo}</span>
        <span className={proyecto.miRol === 'CREADOR' ? 'chip' : 'chip chip--gris'}>{ROL[proyecto.miRol]}</span>
      </div>
      <h3>{proyecto.nombre}</h3>
      <p className="proyecto__desc">{proyecto.descripcion || 'Sin descripción.'}</p>
      {techs.length > 0 && (
        <div className="proyecto__techs">
          {techs.slice(0, 3).map((t) => <span key={t.id} className="etiqueta">{t.nombre}</span>)}
          {techs.length > 3 && <span className="etiqueta">+{techs.length - 3}</span>}
        </div>
      )}
      <div className="proyecto__pie">
        <span className="proyecto__equipo">
          <Avatares personas={activos} />
          {activos.length} {activos.length === 1 ? 'integrante' : 'integrantes'}
        </span>
        <span className="proyecto__fecha">{formatearFecha(proyecto.fechaEntrega)}</span>
        <Link to={`/proyectos/${proyecto.id}`} aria-label={`Ver detalles de ${proyecto.nombre}`}>Ver detalles →</Link>
      </div>
    </li>
  );
}

export default function Proyectos() {
  const { token } = useAuth();
  const { usuario } = useOutletContext();
  const navigate = useNavigate();
  const [estado, setEstado] = useState('cargando');
  const [error, setError] = useState('');
  const [proyectos, setProyectos] = useState([]);
  const [filtro, setFiltro] = useState('todos');
  const [busqueda, setBusqueda] = useState('');
  const [orden, setOrden] = useState('recientes');
  const [creando, setCreando] = useState(false);

  const cargar = useCallback(async () => {
    setEstado('cargando');
    try {
      const [lista, invitaciones] = await Promise.all([api.listarProyectos(token), api.misInvitaciones(token)]);
      // El backend también lista proyectos con invitación pendiente: esos van en "Invitaciones".
      const pendientes = new Set(invitaciones.map((i) => i.proyectoId));
      const propios = lista.filter((p) => !pendientes.has(p.id));
      const equipos = await Promise.all(propios.map((p) => api.verEquipo(token, p.id).catch(() => [])));
      setProyectos(
        propios.map((p, i) => ({
          ...p,
          equipo: equipos[i],
          miRol: equipos[i].find((m) => m.email === usuario.email)?.rol ?? 'INTEGRANTE',
        })),
      );
      setEstado('ok');
    } catch (err) {
      setError(err.message);
      setEstado('error');
    }
  }, [token, usuario.email]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const resumen = useMemo(() => {
    const hoy = new Date().setHours(0, 0, 0, 0);
    const futuras = proyectos
      .map((p) => parseFecha(p.fechaEntrega))
      .filter((f) => f && f.getTime() >= hoy)
      .sort((a, b) => a - b);
    const creados = proyectos.filter((p) => p.miRol === 'CREADOR').length;
    return {
      total: proyectos.length,
      creados,
      integrante: proyectos.length - creados,
      proxima: futuras[0] ? formatearFecha(futuras[0]) : '—',
    };
  }, [proyectos]);

  const visibles = useMemo(() => {
    const q = busqueda.trim().toLowerCase();
    const lista = proyectos.filter(
      (p) =>
        (filtro === 'todos' || p.miRol === filtro) &&
        (!q || p.nombre.toLowerCase().includes(q) || p.tecnologias.some((t) => t.nombre.toLowerCase().includes(q))),
    );
    const porFecha = (p) => (p.fechaEntrega ? parseFecha(p.fechaEntrega).getTime() : Infinity);
    if (orden === 'entrega') lista.sort((a, b) => porFecha(a) - porFecha(b));
    else if (orden === 'nombre') lista.sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));
    else lista.sort((a, b) => b.id - a.id);
    return lista;
  }, [proyectos, filtro, busqueda, orden]);

  const conteo = (f) => (f === 'todos' ? proyectos.length : proyectos.filter((p) => p.miRol === f).length);

  return (
    <>
      <section>
        <div className="titulo-fila">
          <div>
            <h1 className="titulo">Mis proyectos</h1>
            <p className="seccion__sub">Gestiona y colabora en tus proyectos de software del ciclo académico.</p>
          </div>
          <button type="button" className="btn btn--primario" onClick={() => setCreando(true)}>
            <IconPlus /> Crear proyecto
          </button>
        </div>
      </section>

      {estado === 'cargando' && <p className="estado">Cargando tus proyectos…</p>}

      {estado === 'error' && (
        <div className="estado estado--error" role="alert">
          <p>{error}</p>
          <button type="button" className="btn btn--primario" onClick={cargar}>Reintentar</button>
        </div>
      )}

      {estado === 'ok' && proyectos.length === 0 && (
        <section className="tarjeta vacio-grande">
          <h2>Aún no tienes proyectos</h2>
          <p>Comienza organizando los objetivos, tecnologías y equipo de tu próximo proyecto de software.</p>
          <button type="button" className="btn btn--primario" onClick={() => setCreando(true)}>
            <IconPlus /> Crear primer proyecto
          </button>
        </section>
      )}

      {estado === 'ok' && proyectos.length > 0 && (
        <>
          <div className="estadisticas">
            <Estadistica etiqueta="Total proyectos" valor={resumen.total} />
            <Estadistica etiqueta="Creados por mí" valor={resumen.creados} />
            <Estadistica etiqueta="Como integrante" valor={resumen.integrante} />
            <Estadistica etiqueta="Próxima entrega" valor={resumen.proxima} />
          </div>

          <div className="filtros">
            <div className="filtros__tabs" role="group" aria-label="Filtrar proyectos">
              {FILTROS.map(([clave, texto]) => (
                <button
                  key={clave}
                  type="button"
                  className={clave === filtro ? 'tab tab--activa' : 'tab'}
                  aria-pressed={clave === filtro}
                  onClick={() => setFiltro(clave)}
                >
                  {texto} <span className="tab__n">{conteo(clave)}</span>
                </button>
              ))}
            </div>
            <div className="filtros__derecha">
              <label className="busqueda">
                <IconSearch />
                <input
                  type="search"
                  placeholder="Buscar por título o tecnología…"
                  value={busqueda}
                  onChange={(e) => setBusqueda(e.target.value)}
                  aria-label="Buscar proyectos"
                />
              </label>
              <select className="selector" value={orden} onChange={(e) => setOrden(e.target.value)} aria-label="Ordenar proyectos">
                <option value="recientes">Recientes</option>
                <option value="entrega">Entrega más próxima</option>
                <option value="nombre">Nombre (A–Z)</option>
              </select>
            </div>
          </div>

          {visibles.length === 0 ? (
            <p className="vacio">Ningún proyecto coincide con tu búsqueda.</p>
          ) : (
            <ul className="grilla grilla--proyectos">
              {visibles.map((p) => <TarjetaProyecto key={p.id} proyecto={p} />)}
            </ul>
          )}
        </>
      )}

      {creando && (
        <ModalProyecto
          onCerrar={() => setCreando(false)}
          onGuardado={(p) => navigate(`/proyectos/${p.id}`)}
        />
      )}
    </>
  );
}
