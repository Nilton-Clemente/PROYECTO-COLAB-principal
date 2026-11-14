import { useEffect, useRef, useState } from 'react';
import { useAuth } from '../auth/AuthContext.jsx';
import * as api from '../api/colab.js';
import { EJEMPLO_PROYECTO_JSON, leerProyectoJson } from '../proyectoJson.js';
import Alerta from './Alerta.jsx';
import Modal from './Modal.jsx';

export default function ModalProyectoJson({ onCerrar, onGuardado }) {
  const { token } = useAuth();
  const [contenido, setContenido] = useState('');
  const [tecnologias, setTecnologias] = useState(null);
  const [estadoCatalogo, setEstadoCatalogo] = useState('cargando');
  const [reintento, setReintento] = useState(0);
  const [error, setError] = useState(null);
  const [enviando, setEnviando] = useState(false);
  const envioEnCurso = useRef(false);
  const editor = useRef(null);

  useEffect(() => {
    let activo = true;
    setEstadoCatalogo('cargando');
    api.listarTecnologias(token).then((lista) => {
      if (activo) { setTecnologias(lista); setEstadoCatalogo('ok'); }
    }).catch(() => { if (activo) setEstadoCatalogo('error'); });
    return () => { activo = false; };
  }, [token, reintento]);

  function insertarEjemplo() {
    if (contenido.trim() && !window.confirm('¿Reemplazar el JSON actual por la plantilla de ejemplo?')) return;
    setContenido(JSON.stringify(EJEMPLO_PROYECTO_JSON, null, 2));
    setError(null);
    editor.current?.focus();
  }

  async function enviar(e) {
    e.preventDefault();
    if (envioEnCurso.current) return;
    let datos;
    try {
      datos = leerProyectoJson(contenido, tecnologias);
    } catch (err) {
      setError(err.message);
      editor.current?.focus();
      return;
    }
    envioEnCurso.current = true;
    setEnviando(true);
    setError(null);
    try {
      onGuardado(await api.crearProyecto(token, datos));
    } catch (err) {
      setError(err.status === 400 ? 'El backend rechazó los datos. Revisa el contenido y los IDs de tecnologías antes de reintentar.' : err.message);
    } finally {
      envioEnCurso.current = false;
      setEnviando(false);
    }
  }

  return (
    <Modal titulo="Crear proyecto desde JSON" subtitulo="Método rápido: pega un objeto con los datos del proyecto." ancho="ancho" onCerrar={onCerrar}>
      <form className="modal__form" onSubmit={enviar}>
        <p className="nota nota--info">Solo <strong>nombre</strong> es obligatorio. Las listas son arrays; los objetivos, flujos y demás detalles son textos. Usa <code>\n</code> para saltos de línea dentro del JSON. No incluyas códigos ni IDs de proyecto.</p>
        <div className="proyecto-json__acciones">
          <button type="button" className="btn btn--ghost btn--chico" disabled={enviando} onClick={insertarEjemplo}>Insertar plantilla de ejemplo</button>
        </div>
        <details className="caso-uso-detalles">
          <summary>IDs de tecnologías disponibles</summary>
          <p className="campo__nota">Usa estos números en tecnologiasIds, por ejemplo [1, 2]. Puedes dejar [] para completar las tecnologías después. fechaEntrega usa YYYY-MM-DD o null.</p>
          {estadoCatalogo === 'cargando' && <p role="status">Cargando catálogo…</p>}
          {estadoCatalogo === 'error' && (
            <div>
              <p className="campo__error">No se pudo cargar el catálogo. Puedes crear sin tecnologías o reintentar.</p>
              <button type="button" className="btn btn--ghost btn--chico" disabled={enviando} onClick={() => setReintento((n) => n + 1)}>Reintentar catálogo</button>
            </div>
          )}
          {estadoCatalogo === 'ok' && (tecnologias.length ? (
            <ul className="proyecto-json__catalogo">{tecnologias.map((t) => <li key={t.id}><code>{t.id}</code> — {t.nombre}</li>)}</ul>
          ) : <p className="campo__nota">No hay tecnologías registradas.</p>)}
        </details>
        <div className="campo">
          <label htmlFor="p-json">Datos del proyecto en JSON</label>
          <textarea ref={editor} id="p-json" className="proyecto-json__editor" rows={18} spellCheck={false} autoCapitalize="off" autoCorrect="off" disabled={enviando} value={contenido}
            placeholder={'{\n  "nombre": "Mi proyecto"\n}'} aria-invalid={Boolean(error)} aria-describedby={error ? 'p-json-ayuda p-json-error' : 'p-json-ayuda'}
            onChange={(e) => { setContenido(e.target.value); setError(null); }} />
          <small id="p-json-ayuda" className="campo__nota">El proyecto se guardará al pulsar Crear proyecto. Esto no genera automáticamente la planificación con IA.</small>
        </div>
        {error && <div id="p-json-error"><Alerta titulo="No se pudo crear el proyecto">{error}</Alerta></div>}
        <div className="modal__pie">
          <button type="button" className="btn btn--ghost" disabled={enviando} onClick={onCerrar}>Cancelar</button>
          <button type="submit" className="btn btn--primario" disabled={enviando}>{enviando ? 'Creando…' : 'Crear proyecto'}</button>
        </div>
      </form>
    </Modal>
  );
}