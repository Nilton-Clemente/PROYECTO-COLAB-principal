import Modal from './Modal.jsx';

// Modal genérico de confirmación / aviso. Reutilizable para eliminaciones y avisos de conflicto.
export default function ModalConfirmacion({ titulo, mensaje, onCerrar, confirmar = null, variante = 'estandar' }) {
  return (
    <Modal titulo={titulo} onCerrar={onCerrar} variante={variante} clase="modal--confirmacion">
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
    </Modal>
  );
}