// Modal genérico de confirmación / aviso. Reutilizable para eliminaciones y avisos de conflicto.
export default function ModalConfirmacion({ titulo, mensaje, onCerrar, confirmar = null, variante = 'estandar' }) {
  return (
    <div
      className="modal-overlay"
      onClick={(e) => {
        if (e.target === e.currentTarget) onCerrar();
      }}
    >
      <section className="modal modal--confirmacion" role="dialog" aria-modal="true" aria-labelledby="modal-confirmacion-titulo">
        <header className={`modal__cabecera${variante === 'aviso' ? ' modal__cabecera--aviso' : ''}`}>
          <h2 className="modal__titulo" id="modal-confirmacion-titulo">{titulo}</h2>
          <button className="modal__cerrar" type="button" onClick={onCerrar} aria-label="Cerrar">×</button>
        </header>

        <div className="modal__cuerpo">
          <div className="modal__texto">{mensaje}</div>
        </div>

        <footer className="modal__pie">
          {confirmar ? (
            <>
              <button className="btn btn--secundario" type="button" onClick={onCerrar}>Cancelar</button>
              <button
                className="btn btn--peligro-solid"
                type="button"
                onClick={confirmar.onConfirmar}
                disabled={confirmar.cargando}
              >
                {confirmar.etiqueta}
              </button>
            </>
          ) : (
            <button className="btn btn--primario" type="button" onClick={onCerrar}>Entendido</button>
          )}
        </footer>
      </section>
    </div>
  );
}