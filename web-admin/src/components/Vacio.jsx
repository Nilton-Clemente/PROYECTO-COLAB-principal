import { IconSearch } from './Icons.jsx';

// Estado vacío de una lista, con acción opcional para limpiar filtros.
export default function Vacio({ titulo, mensaje, onLimpiar, etiquetaBoton = 'Limpiar filtros' }) {
  return (
    <div className="adm-vacio">
      <IconSearch />
      <h2>{titulo}</h2>
      <p>{mensaje}</p>
      {onLimpiar && (
        <button className="btn btn--secundario" type="button" onClick={onLimpiar}>{etiquetaBoton}</button>
      )}
    </div>
  );
}