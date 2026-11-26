import { useCallback, useEffect, useMemo, useState, useSyncExternalStore } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import * as api from '../api/colab.js';
import { iniciales } from '../utils.js';
import { IconBell } from './Icons.jsx';
import { crearBandejaNotificaciones } from '../notificaciones.js';

export function EnlaceNotificaciones({ noLeidas, contadorError, className }) {
  const descripcion = contadorError ? 'Contador sin actualizar' : noLeidas == null
    ? 'Contador no disponible' : `${noLeidas} sin leer`;
  return (
    <NavLink to="/notificaciones" className={className} aria-label={`Notificaciones: ${descripcion}`}>
      <IconBell /> Notificaciones
      {noLeidas > 0 && <span className="nav__insignia" aria-hidden="true">{noLeidas}</span>}
      {contadorError && <span className="nav__contador-error" title="Contador sin actualizar" aria-hidden="true">!</span>}
    </NavLink>
  );
}

export default function Layout() {
  const { token } = useAuth();
  const { pathname } = useLocation();
  const bandeja = useMemo(() => crearBandejaNotificaciones({
    listar: () => api.listarNotificaciones(token),
    contar: () => api.contarNotificaciones(token),
    marcarLeida: (id) => api.marcarNotificacionLeida(token, id),
    eliminarLeidas: () => api.eliminarNotificacionesLeidas(token),
  }), [token]);
  const notificaciones = useSyncExternalStore(bandeja.subscribe, bandeja.getSnapshot, bandeja.getSnapshot);
  const [usuario, setUsuario] = useState(null);
  const [error, setError] = useState('');
  const [pendientes, setPendientes] = useState(0);
  const [aviso, setAviso] = useState(null);

  const cargar = useCallback(async () => {
    setError('');
    try {
      const [perfil, invitaciones] = await Promise.all([api.obtenerPerfil(token), api.misInvitaciones(token)]);
      setUsuario({ nombre: perfil.nombre, email: perfil.email });
      setPendientes(invitaciones.length);
    } catch (err) {
      setError(err.message);
    }
  }, [token]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  useEffect(() => {
    if (!token) return undefined;
    const refrescar = () => {
      if (document.visibilityState === 'hidden') return;
      if (pathname === '/notificaciones') bandeja.cargar();
      else bandeja.refrescarContador();
    };
    // La propia página carga la bandeja al entrar; fuera de ella basta el contador.
    if (pathname !== '/notificaciones') bandeja.refrescarContador();
    window.addEventListener('focus', refrescar);
    document.addEventListener('visibilitychange', refrescar);
    return () => {
      window.removeEventListener('focus', refrescar);
      document.removeEventListener('visibilitychange', refrescar);
    };
  }, [bandeja, pathname, token]);

  useEffect(() => {
    if (!aviso) return undefined;
    const t = setTimeout(() => setAviso(null), 4500);
    return () => clearTimeout(t);
  }, [aviso]);

  const refrescarInvitaciones = useCallback(async () => {
    try {
      setPendientes((await api.misInvitaciones(token)).length);
    } catch {
      /* el contador se actualizará en la próxima carga */
    }
  }, [token]);

  const avisar = useCallback((texto, tipo = 'error') => setAviso({ texto, tipo }), []);

  const enlace = ({ isActive }) => (isActive ? 'nav__enlace nav__enlace--activo' : 'nav__enlace');

  return (
    <div className="app">
      <header className="barra">
        <div className="barra__interior">
          <span className="logo">Colab</span>
          <nav className="nav" aria-label="Principal">
            <NavLink to="/proyectos" className={enlace}>Proyectos</NavLink>
            <NavLink to="/invitaciones" className={enlace}>
              Invitaciones
              {pendientes > 0 && <span className="nav__insignia" aria-label={`${pendientes} pendientes`}>{pendientes}</span>}
            </NavLink>
            <EnlaceNotificaciones noLeidas={notificaciones.noLeidas} contadorError={notificaciones.contadorError} className={enlace} />
            <NavLink to="/perfil" className={enlace}>Mi perfil</NavLink>
          </nav>
          <div className="barra__usuario">
            {usuario && (
              <div className="barra__datos">
                <span className="avatar avatar--chico" aria-hidden="true">{iniciales(usuario.nombre)}</span>
                <div>
                  <strong>{usuario.nombre}</strong>
                  <small>{usuario.email}</small>
                </div>
              </div>
            )}
          </div>
        </div>
      </header>

      <main className="contenido">
        {!usuario && !error && <p className="estado">Cargando…</p>}
        {error && (
          <div className="estado estado--error" role="alert">
            <p>{error}</p>
            <button type="button" className="btn btn--primario" onClick={cargar}>Reintentar</button>
          </div>
        )}
        {usuario && <Outlet context={{ usuario, pendientes, refrescarInvitaciones, avisar, bandeja }} />}
      </main>

      <footer className="pie">
        <span><strong className="logo logo--chico">Colab</strong> © {new Date().getFullYear()} Departamento de Tecnología Digital TECSUP.</span>
      </footer>

      {aviso && <div className={`toast toast--${aviso.tipo}`} role="alert">{aviso.texto}</div>}
    </div>
  );
}
