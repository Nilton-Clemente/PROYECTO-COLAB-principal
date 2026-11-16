import { useState } from 'react';
import Modal from './Modal.jsx';
import { IconEye, IconEyeOff, IconError } from './Icons.jsx';

function validar(nueva, confirmar) {
  if (!nueva || nueva.length < 6 || nueva.length > 100) {
    return 'La contraseña debe tener entre 6 y 100 caracteres.';
  }
  if (nueva !== confirmar) {
    return 'Las contraseñas no coinciden.';
  }
  return '';
}

export default function ModalRestablecerContrasena({ usuario, onCerrar, onGuardar }) {
  const [nueva, setNueva] = useState('');
  const [confirmar, setConfirmar] = useState('');
  const [mostrar, setMostrar] = useState(false);
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  async function enviar(e) {
    e.preventDefault();
    const mensaje = validar(nueva, confirmar);
    if (mensaje) {
      setError(mensaje);
      return;
    }
    setEnviando(true);
    setError('');
    try {
      await onGuardar(nueva);
    } catch (err) {
      setError(err.message || 'No se pudo restablecer la contraseña.');
      setEnviando(false);
    }
  }

  return (
    <Modal
      titulo="Restablecer contraseña"
      subtitulo={<>Usuario: <strong>{usuario.nombre}</strong><br /><span className="modal__email">{usuario.email}</span></>}
      onCerrar={onCerrar}
    >
      <form className="modal__cuerpo" onSubmit={enviar} noValidate>
        <div className="modal__campo">
          <label className="modal__label" htmlFor="nueva-contrasena">Nueva contraseña</label>
          <div className="modal__input-bloque">
            <input
              id="nueva-contrasena"
              className="modal__input"
              type={mostrar ? 'text' : 'password'}
              placeholder="Ingrese nueva contraseña"
              autoComplete="new-password"
              value={nueva}
              onChange={(e) => { setNueva(e.target.value); setError(''); }}
            />
            <button
              className="modal__ojo"
              type="button"
              onClick={() => setMostrar(!mostrar)}
              aria-label={mostrar ? 'Ocultar contraseña' : 'Mostrar contraseña'}
            >
              {mostrar ? <IconEyeOff /> : <IconEye />}
            </button>
          </div>
          <p className="modal__ayuda">Mínimo 6 y máximo 100 caracteres.</p>
        </div>

        <div className="modal__campo">
          <label className="modal__label" htmlFor="confirmar-contrasena">Confirmar nueva contraseña</label>
          <input
            id="confirmar-contrasena"
            className="modal__input"
            type={mostrar ? 'text' : 'password'}
            placeholder="Confirme la contraseña"
            autoComplete="new-password"
            value={confirmar}
            onChange={(e) => { setConfirmar(e.target.value); setError(''); }}
          />
        </div>

        {error && (
          <div className="modal__validacion" role="alert">
            <IconError />
            <span>{error}</span>
          </div>
        )}

        <footer className="modal__pie">
          <button className="btn btn--secundario" type="button" onClick={onCerrar}>Cancelar</button>
          <button className="btn btn--primario" type="submit" disabled={enviando}>
            {enviando ? 'Guardando…' : 'Guardar contraseña'}
          </button>
        </footer>
      </form>
    </Modal>
  );
}
