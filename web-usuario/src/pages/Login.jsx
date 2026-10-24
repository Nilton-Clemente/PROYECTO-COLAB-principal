import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import Campo from '../components/Campo.jsx';
import Alerta from '../components/Alerta.jsx';
import { IconLock, IconMail } from '../components/Icons.jsx';
import { DOMINIO_INSTITUCIONAL } from '../config.js';
import { validarCorreoInstitucional } from '../utils.js';

export default function Login() {
  const { iniciarSesion, aviso } = useAuth();
  const [email, setEmail] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [recordar, setRecordar] = useState(true);
  const [errores, setErrores] = useState({});
  const [errorServidor, setErrorServidor] = useState(null);
  const [enviando, setEnviando] = useState(false);

  async function enviar(e) {
    e.preventDefault();
    const nuevos = {
      email: validarCorreoInstitucional(email),
      contrasena: contrasena ? null : 'Ingresa tu contraseña.',
    };
    setErrores(nuevos);
    setErrorServidor(null);
    if (nuevos.email || nuevos.contrasena) return;

    setEnviando(true);
    try {
      await iniciarSesion(email.trim().toLowerCase(), contrasena, recordar);
    } catch (err) {
      setErrorServidor(
        err.status === 401
          ? { titulo: 'Acceso denegado', texto: 'Credenciales incorrectas. Verifica tu correo o contraseña e inténtalo nuevamente.' }
          : { titulo: 'No se pudo iniciar sesión', texto: err.message },
      );
      setEnviando(false);
    }
  }

  return (
    <main className="auth">
      <form className="tarjeta auth__tarjeta" onSubmit={enviar} noValidate>
        <h1>Bienvenido a Colab</h1>
        <p className="auth__sub">Planifica y gestiona tus proyectos académicos de software en TECSUP.</p>

        {aviso && !errorServidor && <Alerta titulo="Sesión finalizada" tipo="info">{aviso}</Alerta>}
        {errorServidor && <Alerta titulo={errorServidor.titulo}>{errorServidor.texto}</Alerta>}

        <Campo
          id="email"
          label="Correo institucional"
          ayuda={<span className="campo__ayuda">@{DOMINIO_INSTITUCIONAL}</span>}
          icono={<IconMail />}
          type="email"
          autoComplete="username"
          placeholder={`nombre.apellido@${DOMINIO_INSTITUCIONAL}`}
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          error={errores.email}
        />

        <Campo
          id="contrasena"
          label="Contraseña"
          icono={<IconLock />}
          type="password"
          autoComplete="current-password"
          placeholder="Tu contraseña"
          value={contrasena}
          onChange={(e) => setContrasena(e.target.value)}
          error={errores.contrasena}
        />

        <label className="check">
          <input type="checkbox" checked={recordar} onChange={(e) => setRecordar(e.target.checked)} />
          Mantener sesión activa en este equipo
        </label>

        <button className="btn btn--primario btn--bloque" type="submit" disabled={enviando}>
          {enviando ? 'Ingresando…' : 'Iniciar sesión'}
        </button>

        <p className="auth__pie">
          ¿No tienes una cuenta aún? <Link to="/registro">Regístrate aquí</Link>
        </p>
      </form>
    </main>
  );
}
