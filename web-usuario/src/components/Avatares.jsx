import { iniciales } from '../utils.js';

// Pila de avatares con iniciales (máximo `limite`) y "+n" para el resto.
export default function Avatares({ personas, limite = 3 }) {
  const visibles = personas.slice(0, limite);
  const resto = personas.length - visibles.length;
  return (
    <span className="pila">
      {visibles.map((p) => (
        <span key={p.email} className="avatar avatar--mini" title={p.nombre}>{iniciales(p.nombre)}</span>
      ))}
      {resto > 0 && <span className="avatar avatar--mini avatar--resto">+{resto}</span>}
    </span>
  );
}
