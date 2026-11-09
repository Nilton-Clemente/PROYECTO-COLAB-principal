import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import Campo from '../components/Campo.jsx';
import Alerta from '../components/Alerta.jsx';
import { IconCheck, IconLock, IconMail, IconUser } from '../components/Icons.jsx';
import { DOMINIO_INSTITUCIONAL, MIN_CONTRASENA } from '../config.js';
import { validarCorreoInstitucional } from '../utils.js';

function nivelSeguridad(c) {
  const cumple = c.length >= MIN_CONTRASENA && /[A-Za-z]/.test(c) && /\d/.test(c);
  if (!cumple) return { cumple, nivel: c ? 1 : 0, texto: c ? 'Débil' : '' };
  const extra = (c.length >= 10 ? 1 : 0) + (/[^A-Za-z0-9]/.test(c) ? 1 : 0) + (/[a-z]/.test(c) && /[A-Z]/.test(c) ? 1 : 0);
  return extra >= 2 ? { cumple, nivel: 3, texto: 'Fuerte' } : { cumple, nivel: 2, texto: 'Media' };
}

export default function Registro() {
  const { registrar } = useAuth();
  const [nombre, setNombre] = useState('');
  const [email, setEmail] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [acepta, setAcepta] = useState(false);
  const [errores, setErrores] = useState({});
  const [errorServidor, setErrorServidor] = useState(null);
  const [enviando, setEnviando] = useState(false);

  const seguridad = nivelSeguridad(contrasena);

  async function enviar(e) {
    e.preventDefault();
    const nuevos = {
      nombre: nombre.trim() ? null : 'Ingresa tu nombre y apellidos.',
      email: validarCorreoInstitucional(email),
      contrasena: seguridad.cumple ? null : `Mínimo ${MIN_CONTRASENA} caracteres, con letras y números.`,
      acepta: acepta ? null : 'Debes aceptar los términos para continuar.',
    };
    setErrores(nuevos);
    setErrorServidor(null);
    if (Object.values(nuevos).some(Boolean)) return;

    setEnviando(true);
    try {
      await registrar(nombre.trim(), email.trim().toLowerCase(), contrasena);
    } catch (err) {
      setErrorServidor(
        err.status === 400
          ? { titulo: 'Correo ya registrado', texto: 'Ese correo ya tiene una cuenta. Si eres estudiante de TECSUP, inicia sesión.' }
          : { titulo: 'No se pudo crear la cuenta', texto: err.message },
      );
      setEnviando(false);
    }
  }

  return (
    <main className="auth">
      <form className="tarjeta auth__tarjeta" onSubmit={enviar} noValidate>
        <h1>Crear cuenta</h1>
        <p className="auth__sub">Únete a la comunidad de desarrollo de proyectos de software en TECSUP.</p>

        {errorServidor && (
          <Alerta titulo={errorServidor.titulo}>
            {errorServidor.texto} {errorServidor.titulo === 'Correo ya registrado' && <Link to="/login">Ir a iniciar sesión</Link>}
          </Alerta>
        )}

        <Campo
          id="nombre"
          label="Nombre y apellidos"
          icono={<IconUser />}
          autoComplete="name"
          value={nombre}
          onChange={(e) => setNombre(e.target.value)}
          error={errores.nombre}
        />
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
          autoComplete="new-password"
          value={contrasena}
          onChange={(e) => setContrasena(e.target.value)}
          error={errores.contrasena}
        />

        <div className="seguridad" aria-live="polite">
          <div className="seguridad__fila">
            <span>Nivel de seguridad</span>
            <strong>{seguridad.texto}</strong>
          </div>
          <div className={`seguridad__barra seguridad__barra--${seguridad.nivel}`}>
            <i /><i /><i />
          </div>
          <p className={seguridad.cumple ? 'seguridad__ok' : ''}>
            <IconCheck width={13} height={13} /> Mínimo {MIN_CONTRASENA} caracteres (incluye letras y números)
          </p>
        </div>

        <label className="check">
          <input type="checkbox" checked={acepta} onChange={(e) => setAcepta(e.target.checked)} />
          Acepto los términos y condiciones y la política de integridad académica de TECSUP.
        </label>
        {errores.acepta && <p className="campo__error">{errores.acepta}</p>}

        <button className="btn btn--primario btn--bloque" type="submit" disabled={enviando}>
          {enviando ? 'Creando cuenta…' : 'Crear cuenta'}
        </button>

        <p className="auth__pie">
          ¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link>
        </p>
      </form>
    </main>
  );
}
