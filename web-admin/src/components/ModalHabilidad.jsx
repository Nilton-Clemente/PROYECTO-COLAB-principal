import { useState } from 'react';
import { IconError } from './Icons.jsx';

export default function ModalHabilidad({ habilidad, onCerrar, onGuardar }) {
  const esEdicion = Boolean(habilidad);
  const [nombre, setNombre] = useState(habilidad?.nombre ?? '');
  const [area, setArea] = useState(habilidad?.area ?? '');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  async function enviar(e) {
    e.preventDefault();
    if (!nombre.trim()) {
      setError('El nombre de la habilidad es obligatorio.');
      return;
    }
    setEnviando(true);
    setError('');
    try {
      await onGuardar({ nombre: nombre.trim(), area: area.trim() });
    } catch (err) {
      setError(err.message || 'No se pudo guardar la habilidad.');
      setEnviando(false);
    }
  }

  return (
    <div
      className="modal-overlay"
      onClick={(e) => {
        if (e.target === e.currentTarget) onCerrar();
      }}
    >
      <section className="modal" role="dialog" aria-modal="true" aria-labelledby="modal-habilidad-titulo">
        <header className="modal__cabecera">
          <div>
            <h2 className="modal__titulo" id="modal-habilidad-titulo">
              {esEdicion ? 'Editar habilidad' : 'Nueva habilidad'}
            </h2>
            <p className="modal__usuario">{esEdicion ? 'Modificando registro del catálogo.' : 'Añadir al catálogo formativo.'}</p>
          </div>
          <button className="modal__cerrar" type="button" onClick={onCerrar} aria-label="Cerrar">×</button>
        </header>

        <form className="modal__cuerpo" onSubmit={enviar} noValidate>
          <div className="modal__campo">
            <label className="modal__label" htmlFor="habilidad-nombre">Nombre de la habilidad *</label>
            <input
              id="habilidad-nombre"
              className="modal__input"
              placeholder="Ej. Desarrollo Frontend React"
              maxLength={100}
              value={nombre}
              onChange={(e) => { setNombre(e.target.value); setError(''); }}
            />
          </div>

          <div className="modal__campo">
            <label className="modal__label" htmlFor="habilidad-area">Área disciplinar (opcional)</label>
            <input
              id="habilidad-area"
              className="modal__input"
              placeholder="Ej. Desarrollo de Software"
              maxLength={255}
              value={area}
              onChange={(e) => { setArea(e.target.value); setError(''); }}
            />
          </div>

          {error && (
            <div className="modal__validacion" role="alert">
              <IconError />
              <span>{error}</span>
            </div>
          )}

          <footer className="modal__pie">
            <button className="btn btn--secundario" type="button" onClick={onCerrar}>Cancelar</button>
            <button className="btn btn--primario" type="submit" disabled={enviando}>
              {enviando ? 'Guardando…' : esEdicion ? 'Guardar cambios' : 'Crear habilidad'}
            </button>
          </footer>
        </form>
      </section>
    </div>
  );
}