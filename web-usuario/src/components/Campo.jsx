import { useState } from 'react';
import { IconAlert, IconEye, IconEyeOff } from './Icons.jsx';

// Campo de formulario con icono, ayuda a la derecha del label y mensaje de error.
export default function Campo({ id, label, ayuda, icono, error, type = 'text', ...inputProps }) {
  const [visible, setVisible] = useState(false);
  const esClave = type === 'password';

  return (
    <div className="campo">
      <div className="campo__fila">
        <label htmlFor={id}>{label}</label>
        {ayuda}
      </div>
      <div className={`campo__caja${error ? ' campo__caja--error' : ''}`}>
        {icono}
        <input
          id={id}
          type={esClave && visible ? 'text' : type}
          aria-invalid={error ? 'true' : undefined}
          aria-describedby={error ? `${id}-error` : undefined}
          {...inputProps}
        />
        {esClave && (
          <button
            type="button"
            className="campo__ojo"
            onClick={() => setVisible((v) => !v)}
            aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
          >
            {visible ? <IconEyeOff /> : <IconEye />}
          </button>
        )}
      </div>
      {error && (
        <p className="campo__error" id={`${id}-error`}>
          <IconAlert width={13} height={13} /> {error}
        </p>
      )}
    </div>
  );
}
