import { useCallback, useEffect, useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import * as api from '../api/colab.js';
import { iniciales } from '../utils.js';

export default function Layout() {
  const { token, cerrarSesion } = useAuth();
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
            <button type="button" className="enlace-peligro" onClick={cerrarSesion}>Cerrar sesión</button>
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
        {usuario && <Outlet context={{ usuario, pendientes, refrescarInvitaciones, avisar }} />}
      </main>

      <footer className="pie">
        <span><strong className="logo logo--chico">Colab</strong> © {new Date().getFullYear()} Departamento de Tecnología Digital TECSUP.</span>
      </footer>

      {aviso && <div className={`toast toast--${aviso.tipo}`} role="alert">{aviso.texto}</div>}
    </div>
  );
}
