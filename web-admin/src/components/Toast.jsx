import { IconCheck, IconError } from './Icons.jsx';

export default function Toast({ toast }) {
  if (!toast) return null;
  const esError = toast.tipo === 'error';
  return (
    <div className={`toast${esError ? ' toast--error' : ''}`} role="alert">
      <span className="toast__icono">{esError ? <IconError /> : <IconCheck />}</span>
      <div>
        <div className="toast__titulo">{toast.titulo}</div>
        <div className="toast__mensaje">{toast.mensaje}</div>
      </div>
    </div>
  );
}