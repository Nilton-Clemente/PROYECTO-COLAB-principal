import { IconAlert } from './Icons.jsx';

export default function Alerta({ titulo, children, tipo = 'error' }) {
  return (
    <div className={`alerta alerta--${tipo}`} role="alert">
      <IconAlert width={18} height={18} />
      <div>
        <strong>{titulo}</strong>
        <p>{children}</p>
      </div>
    </div>
  );
}
