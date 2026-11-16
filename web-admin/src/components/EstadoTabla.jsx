// Estado de carga o error para el interior de una tabla/card.
export default function EstadoTabla({ cargando, error, onReintentar, cargandoTexto = 'Cargando…' }) {
  if (cargando) {
    return <div className="adm-estado"><p>{cargandoTexto}</p></div>;
  }
  if (error) {
    return (
      <div className="adm-estado" role="alert">
        <p>{error}</p>
        <button className="btn btn--primario" type="button" onClick={onReintentar}>Reintentar</button>
      </div>
    );
  }
  return null;
}