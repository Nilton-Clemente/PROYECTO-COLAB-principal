import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { IconLogo } from './Icons.jsx';

function claseEnlace({ isActive }) {
  return isActive ? 'shell__enlace shell__enlace--activo' : 'shell__enlace';
}

export default function Layout() {
  const { email, cerrarSesion } = useAuth();

  return (
    <div className="shell">
      <aside className="shell__lateral">
        <div className="shell__marca">
          <span className="shell__logo"><IconLogo /></span>
          <div>
            <strong>Colab</strong>
            <small>Administración</small>
          </div>
        </div>

        <nav className="shell__nav" aria-label="Principal">
          <NavLink to="/usuarios" className={claseEnlace}>Usuarios</NavLink>
          <NavLink to="/habilidades" className={claseEnlace}>Habilidades</NavLink>
          <NavLink to="/tecnologias" className={claseEnlace}>Tecnologías</NavLink>
        </nav>

        <div className="shell__sesion">
          {email && <small className="shell__email" title={email}>{email}</small>}
          <button type="button" className="shell__salir" onClick={cerrarSesion}>Cerrar sesión</button>
        </div>
      </aside>

      <div className="shell__cuerpo">
        <header className="shell__barra">
          <h1>Panel de administración</h1>
        </header>
        <main className="shell__contenido">
          <Outlet />
        </main>
        <footer className="shell__pie">
          <span>Colab © {new Date().getFullYear()} Departamento de Tecnología Digital TECSUP</span>
        </footer>
      </div>
    </div>
  );
}