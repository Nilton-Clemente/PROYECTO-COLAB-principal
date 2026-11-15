import { useState } from 'react';
import { useAuth } from '../auth/AuthContext.jsx';
import {
  IconLogo, IconMail, IconLock, IconEye, IconEyeOff,
  IconArrowRight, IconSpinner, IconError, IconShield,
} from '../components/Icons.jsx';

export default function Login() {
  const { iniciarSesion, aviso } = useAuth();
  const [correo, setCorreo] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [recordar, setRecordar] = useState(true);
  const [mostrar, setMostrar] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState(null);

  async function enviar(e) {
    e.preventDefault();
    if (!correo.trim() || !contrasena) {
      setError({ titulo: 'Datos incompletos', texto: 'Ingresa tu correo institucional y tu contraseña.' });
      return;
    }
    setEnviando(true);
    setError(null);
    try {
      await iniciarSesion(correo, contrasena, recordar);
    } catch (err) {
      setError(
        err.status === 401
          ? { titulo: 'Credenciales incorrectas', texto: 'Verifique su correo institucional o contraseña de acceso.' }
          : { titulo: 'No se pudo iniciar sesión', texto: err.message },
      );
      setEnviando(false);
    }
  }

  return (
    <main className="login-page">
      <div className="background-circle circle-one" aria-hidden="true" />
      <div className="background-circle circle-two" aria-hidden="true" />

      <section className="login-container">
        <header className="header">
          <div className="header-top">
            <div className="brand">
              <span className="brand-icon"><IconLogo /></span>
              <span className="brand-name">Colab</span>
            </div>
            <span className="role-badge">
              <span className="status-dot" /> Administración
            </span>
          </div>
          <div>
            <h1>Panel de administración</h1>
            <p>Acceso restringido para la gestión de cuentas institucionales y catálogos de plataforma.</p>
          </div>
        </header>

        {aviso && !error && (
          <div className="alert error-alert" role="alert">
            <IconError />
            <div className="alert-content">
              <span className="alert-title">Sesión finalizada</span>
              <span className="alert-description">{aviso}</span>
            </div>
          </div>
        )}

        {error && (
          <div className="alert error-alert" role="alert">
            <IconError />
            <div className="alert-content">
              <span className="alert-title">{error.titulo}</span>
              <span className="alert-description">{error.texto}</span>
            </div>
            <button className="alert-close" type="button" onClick={() => setError(null)}>×</button>
          </div>
        )}

        <form className="login-form" onSubmit={enviar} noValidate>
          <div className="form-group">
            <label className="form-label" htmlFor="email">
              <span>Correo institucional</span>
              <span className="form-hint">@tecsup.edu.pe</span>
            </label>
            <div className="input-container">
              <span className="input-icon"><IconMail /></span>
              <input
                id="email"
                className="form-input"
                type="email"
                autoComplete="username"
                placeholder="usuario@tecsup.edu.pe"
                value={correo}
                onChange={(e) => setCorreo(e.target.value)}
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="contrasena">
              Contraseña
            </label>
            <div className="input-container">
              <span className="input-icon"><IconLock /></span>
              <input
                id="contrasena"
                className="form-input"
                type={mostrar ? 'text' : 'password'}
                autoComplete="current-password"
                placeholder="••••••••••••"
                value={contrasena}
                onChange={(e) => setContrasena(e.target.value)}
              />
              <button
                className="password-toggle"
                type="button"
                onClick={() => setMostrar(!mostrar)}
                aria-label={mostrar ? 'Ocultar contraseña' : 'Mostrar contraseña'}
              >
                {mostrar ? <IconEyeOff /> : <IconEye />}
              </button>
            </div>
          </div>

          <div className="remember-container">
            <label className="remember-label">
              <input type="checkbox" checked={recordar} onChange={(e) => setRecordar(e.target.checked)} />
              <span>Recordar sesión administrativa</span>
            </label>
          </div>

          <button className="submit-button" type="submit" disabled={enviando}>
            <span>{enviando ? 'Ingresando…' : 'Iniciar sesión'}</span>
            {enviando ? <IconSpinner className="loading-icon" /> : <IconArrowRight />}
          </button>
        </form>

        <footer className="footer">
          <IconShield />
          <span>Acceso exclusivo del personal administrativo</span>
        </footer>
      </section>
    </main>
  );
}