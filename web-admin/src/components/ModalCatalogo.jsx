import { useEffect, useState } from 'react';
import Modal from './Modal.jsx';
import { IconError } from './Icons.jsx';
import IconoCatalogo from './IconoCatalogo.jsx';
import { listarIconos } from '../api/admin.js';

// Modal genérico para crear/editar ítems de catálogos (habilidades, tecnologías).
// `campos` es [{ nombre, etiqueta, obligatorio, placeholder, maxLength }].
export default function ModalCatalogo({ titulo, subtitulo, registro = null, campos = [], onCerrar, onGuardar }) {
  const esEdicion = Boolean(registro);
  const [valores, setValores] = useState(() => {
    const inicial = {};
    campos.forEach((campo) => { inicial[campo.nombre] = registro?.[campo.nombre] ?? ''; });
    return inicial;
  });
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [icono, setIcono] = useState(registro?.icono ?? '');
  const [iconoModificado, setIconoModificado] = useState(false);
  const [iconos, setIconos] = useState([]);
  const [errorIconos, setErrorIconos] = useState('');
  const [cargandoIconos, setCargandoIconos] = useState(true);

  useEffect(() => {
    let activo = true;
    listarIconos().then((datos) => { if (activo) setIconos(datos); })
      .catch(() => { if (activo) setErrorIconos('No se pudieron cargar los iconos. Puedes guardar sin cambiar el icono.'); })
      .finally(() => { if (activo) setCargandoIconos(false); });
    return () => { activo = false; };
  }, []);

  function cambiar(campo, valor) {
    setValores((prev) => ({ ...prev, [campo]: valor }));
    setError('');
  }

  async function enviar(e) {
    e.preventDefault();
    const faltante = campos.find((campo) => campo.obligatorio && !String(valores[campo.nombre] ?? '').trim());
    if (faltante) {
      setError(`El campo «${faltante.etiqueta}» es obligatorio.`);
      return;
    }
    setEnviando(true);
    setError('');
    const datos = {};
    campos.forEach((campo) => { datos[campo.nombre] = String(valores[campo.nombre] ?? '').trim(); });
    if (!esEdicion && !errorIconos && !cargandoIconos) datos.icono = icono || null;
    if (esEdicion && iconoModificado) datos.icono = icono || null;
    try {
      await onGuardar(datos);
    } catch (err) {
      setError(err.message || 'No se pudo guardar.');
      setEnviando(false);
    }
  }

  return (
    <Modal titulo={titulo} subtitulo={subtitulo} onCerrar={onCerrar}>
      <form className="modal__cuerpo" onSubmit={enviar} noValidate>
        {campos.map((campo) => (
          <div className="modal__campo" key={campo.nombre}>
            <label className="modal__label" htmlFor={`catalogo-${campo.nombre}`}>
              {campo.etiqueta}{campo.obligatorio ? ' *' : ' (opcional)'}
            </label>
            <input
              id={`catalogo-${campo.nombre}`}
              className="modal__input"
              placeholder={campo.placeholder}
              maxLength={campo.maxLength}
              value={valores[campo.nombre]}
              onChange={(e) => cambiar(campo.nombre, e.target.value)}
            />
          </div>
        ))}

        <div className="modal__campo">
          <label className="modal__label" htmlFor="catalogo-icono">Icono (opcional)</label>
          {cargandoIconos ? <p role="status">Cargando iconos…</p> : errorIconos ? <p role="status">{errorIconos}</p> : (
            <div className="selector-icono">
              <IconoCatalogo key={icono} icono={icono} nombre={valores.nombre} className="selector-icono__vista" />
              <select id="catalogo-icono" className="modal__input" value={icono} onChange={(e) => { setIcono(e.target.value); setIconoModificado(true); }}>
                <option value="">Sin icono</option>
                {icono && !iconos.some((item) => item.id === icono) && <option value={icono}>Icono actual: {icono}</option>}
                {iconos.map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}
              </select>
            </div>
          )}
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
            {enviando ? 'Guardando…' : esEdicion ? 'Guardar cambios' : 'Crear'}
          </button>
        </footer>
      </form>
    </Modal>
  );
}