import { useState } from 'react';
import { urlBackend } from '../api/client.js';
import { rutaIconoHabilidad } from '../habilidades.js';
import { iniciales } from '../utils.js';

export default function IconoHabilidad({ habilidad }) {
  const ruta = rutaIconoHabilidad(habilidad.icono);
  const [fallida, setFallida] = useState(null);

  return (
    <span className="habilidad-icono" aria-hidden="true">
      {ruta && ruta !== fallida
        ? <img key={ruta} src={urlBackend(ruta)} alt="" onError={() => setFallida(ruta)} />
        : iniciales(habilidad.nombre) || '◇'}
    </span>
  );
}