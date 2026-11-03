import { useEffect, useRef } from 'react';

// <dialog> nativo: foco atrapado, cierre con Esc y fondo bloqueado.
export default function Modal({ titulo, subtitulo, ancho, onCerrar, children }) {
  const ref = useRef(null);

  useEffect(() => {
    const d = ref.current;
    if (d && !d.open) d.showModal();
  }, []);

  return (
    <dialog
      ref={ref}
      className={ancho === 'ancho' ? 'modal modal--ancho' : 'modal'}
      aria-labelledby="modal-titulo"
      onClose={onCerrar}
      onClick={(e) => e.target === ref.current && ref.current.close()}
    >
      <div className="modal__cuerpo">
        <div className="modal__cab">
          <div>
            <h2 id="modal-titulo">{titulo}</h2>
            {subtitulo && <p className="modal__sub">{subtitulo}</p>}
          </div>
          <button type="button" className="modal__x" onClick={() => ref.current.close()} aria-label="Cerrar">×</button>
        </div>
        {children}
      </div>
    </dialog>
  );
}
