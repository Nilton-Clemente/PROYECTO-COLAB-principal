import { useState } from 'react';

export default function IconoCatalogo({ icono, nombre = '', className = '' }) {
  const [fallo, setFallo] = useState(false);
  const seguro = typeof icono === 'string' && /^[a-z0-9-]+$/.test(icono);
  if (!seguro || fallo) {
    return <span className={className} aria-hidden="true">{nombre.trim().charAt(0).toUpperCase() || '•'}</span>;
  }
  return <img className={className} src={`/iconos/devicon/${icono}.svg`} alt="" onError={() => setFallo(true)} />;
}