// Cascarón base de todos los modales: overlay con cierre al hacer clic fuera,
// cabecera con título/subtítulo y botón de cerrar, y contenido libre.
export default function Modal({ titulo, subtitulo = null, onCerrar, variante = 'estandar', clase = '', children }) {
  return (
    <div
      className="modal-overlay"
      onClick={(e) => {
        if (e.target === e.currentTarget) onCerrar();
      }}
    >
      <section className={`modal ${clase}`.trim()} role="dialog" aria-modal="true" aria-label={titulo}>
        <header className={`modal__cabecera${variante === 'aviso' ? ' modal__cabecera--aviso' : ''}`}>
          <div>
            <h2 className="modal__titulo">{titulo}</h2>
            {subtitulo && <p className="modal__usuario">{subtitulo}</p>}
          </div>
          <button className="modal__cerrar" type="button" onClick={onCerrar} aria-label="Cerrar">×</button>
        </header>
        {children}
      </section>
    </div>
  );
}