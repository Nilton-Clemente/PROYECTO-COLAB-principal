import { useEffect, useId, useRef, useState } from 'react';

const ETIQUETAS = {
  editar: 'Editar proyecto',
  cancelar: 'Cancelar proyecto',
  eliminar: 'Eliminar proyecto',
};

export default function AccionesProyecto({ acciones, bloqueado, onAccion }) {
  const [abierto, setAbierto] = useState(false);
  const panelId = useId();
  const raiz = useRef(null);
  const boton = useRef(null);

  useEffect(() => {
    if (!abierto) return undefined;
    const cerrarFuera = (evento) => {
      if (!raiz.current?.contains(evento.target)) setAbierto(false);
    };
    document.addEventListener('pointerdown', cerrarFuera);
    return () => document.removeEventListener('pointerdown', cerrarFuera);
  }, [abierto]);

  if (!acciones.length) return null;

  return (
    <div ref={raiz} className="acciones-proyecto"
      onBlur={(evento) => {
        if (!evento.currentTarget.contains(evento.relatedTarget)) setAbierto(false);
      }}
      onKeyDown={(evento) => {
        if (abierto && evento.key === 'Escape') {
          evento.stopPropagation();
          setAbierto(false);
          boton.current?.focus();
        }
      }}>
      <button ref={boton} type="button" className="btn btn--ghost acciones-proyecto__boton"
        aria-label="Más acciones del proyecto" aria-expanded={abierto && !bloqueado}
        aria-controls={panelId} disabled={bloqueado} onClick={() => setAbierto((valor) => !valor)}>
        <span aria-hidden="true">⋯</span>
      </button>
      <div id={panelId} className="acciones-proyecto__panel" hidden={!abierto || bloqueado}>
        {acciones.map((accion) => (
          <button key={accion} type="button" className={accion === 'editar' ? '' : 'acciones-proyecto__peligro'}
            aria-haspopup="dialog" onClick={() => {
              boton.current?.focus();
              setAbierto(false);
              onAccion(accion);
            }}>
            {ETIQUETAS[accion]}
          </button>
        ))}
      </div>
    </div>
  );
}