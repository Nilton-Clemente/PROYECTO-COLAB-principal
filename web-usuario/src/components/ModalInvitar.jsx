import { useState } from 'react';
import { useAuth } from '../auth/AuthContext.jsx';
import * as api from '../api/colab.js';
import Modal from './Modal.jsx';
import Campo from './Campo.jsx';
import Alerta from './Alerta.jsx';
import { IconAlert, IconMail } from './Icons.jsx';
import { DOMINIO_INSTITUCIONAL } from '../config.js';
import { validarCorreoInstitucional } from '../utils.js';

const MENSAJES = {
  404: 'Ese correo no pertenece a un usuario registrado en Colab.',
  409: 'Esa persona ya forma parte del proyecto o tiene una invitación pendiente.',
};

export default function ModalInvitar({ proyecto, onCerrar, onInvitado }) {
  const { token } = useAuth();
  const [email, setEmail] = useState('');
  const [errorCampo, setErrorCampo] = useState(null);
  const [errorServidor, setErrorServidor] = useState(null);
  const [enviando, setEnviando] = useState(false);

  async function enviar(e) {
    e.preventDefault();
    const problema = validarCorreoInstitucional(email);
    setErrorCampo(problema);
    setErrorServidor(null);
    if (problema) return;

    const limpio = email.trim().toLowerCase();
    setEnviando(true);
    try {
      await api.invitar(token, proyecto.id, limpio);
      onInvitado(limpio);
    } catch (err) {
      setErrorServidor(MENSAJES[err.status] ?? err.message);
      setEnviando(false);
    }
  }

  return (
    <Modal titulo="Invitar nuevo integrante al equipo" subtitulo={`Proyecto: ${proyecto.nombre}`} onCerrar={onCerrar}>
      <form className="modal__form" onSubmit={enviar} noValidate>
        <p className="seccion__sub">
          Ingresa el correo institucional del estudiante que deseas sumar al proyecto <strong>«{proyecto.nombre}»</strong>.
        </p>
        {errorServidor && <Alerta titulo="No se pudo invitar">{errorServidor}</Alerta>}
        <Campo
          id="inv-email"
          label="Correo institucional"
          ayuda={<span className="campo__ayuda">@{DOMINIO_INSTITUCIONAL}</span>}
          icono={<IconMail />}
          type="email"
          placeholder={`estudiante@${DOMINIO_INSTITUCIONAL}`}
          value={email}
          onChange={(e) => { setEmail(e.target.value); setErrorCampo(null); }}
          error={errorCampo}
          autoFocus
        />
        <p className="nota"><IconAlert width={14} height={14} /> El estudiante debe tener una cuenta activa en Colab con dominio @{DOMINIO_INSTITUCIONAL}.</p>

        <fieldset className="campo">
          <legend>Rol asignado en el proyecto</legend>
          <label className="rol-opcion">
            <input type="radio" name="rol" checked readOnly />
            <span>
              <strong>Integrante del proyecto <em className="chip chip--gris">Recomendado</em></strong>
              <small>Forma parte del equipo y puede consultar la información y la planificación del proyecto.</small>
            </span>
          </label>
        </fieldset>

        <p className="nota nota--info">
          <strong>Notificación instantánea:</strong> la invitación llegará a la bandeja de invitaciones de Colab del estudiante, donde podrá aceptarla o rechazarla.
        </p>

        <div className="modal__pie">
          <button type="button" className="btn btn--ghost" onClick={onCerrar}>Cancelar</button>
          <button type="submit" className="btn btn--primario" disabled={enviando}>{enviando ? 'Enviando…' : 'Enviar invitación'}</button>
        </div>
      </form>
    </Modal>
  );
}
