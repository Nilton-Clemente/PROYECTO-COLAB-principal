import { useRef, useState } from 'react';
import { confirmaNombreProyecto } from '../cicloProyecto.js';
import Alerta from './Alerta.jsx';
import Modal from './Modal.jsx';

export default function ModalCicloProyecto({ accion, proyecto, onCerrar, onConfirmar }) {
  const [texto, setTexto] = useState('');
  const [ocupado, setOcupado] = useState(false);
  const [error, setError] = useState('');
  const enCurso = useRef(false);
  const eliminar = accion === 'eliminar';
  const valido = !eliminar || confirmaNombreProyecto(proyecto.nombre, texto);

  async function confirmar(evento) {
    evento.preventDefault();
    if (!valido || enCurso.current) return;
    enCurso.current = true;
    setOcupado(true);
    setError('');
    try {
      await onConfirmar();
    } catch (err) {
      setError(err.message);
    } finally {
      enCurso.current = false;
      setOcupado(false);
    }
  }

  return (
    <Modal titulo={eliminar ? 'Eliminar proyecto' : 'Cancelar proyecto'} onCerrar={onCerrar} bloqueado={ocupado}>
      <form className="modal__form" onSubmit={confirmar} aria-busy={ocupado}>
        <p>{eliminar ? 'Vas a eliminar ' : 'Vas a cancelar '}<strong>{proyecto.nombre}</strong>.</p>
        <p>{eliminar
          ? 'El borrado es definitivo. Se perderán la planificación, las tareas y el historial del equipo, si existen.'
          : 'El equipo podrá consultar la planificación, las tareas y el historial, pero ya no podrá registrar avances ni modificar el trabajo.'}</p>
        {eliminar && (
          <label className="campo">
            <span>Escribe el nombre del proyecto para confirmar</span>
            <span className="campo__caja"><input value={texto} onChange={(evento) => setTexto(evento.target.value)}
              disabled={ocupado} autoFocus autoComplete="off" spellCheck={false} required /></span>
          </label>
        )}
        {error && <Alerta titulo={eliminar ? 'No se pudo eliminar el proyecto' : 'No se pudo cancelar el proyecto'}>{error}</Alerta>}
        <div className="modal__pie">
          <button type="button" className="btn btn--ghost" autoFocus={!eliminar} disabled={ocupado} onClick={onCerrar}>Volver</button>
          <button type="submit" className="btn btn--peligro" disabled={!valido || ocupado}>
            {eliminar ? (ocupado ? 'Eliminando…' : 'Eliminar definitivamente') : (ocupado ? 'Cancelando…' : 'Confirmar cancelación')}
          </button>
        </div>
      </form>
    </Modal>
  );
}