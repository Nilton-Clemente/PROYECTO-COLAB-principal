import { useEffect, useState } from 'react';
import { useAuth } from '../auth/AuthContext.jsx';
import * as api from '../api/colab.js';
import Modal from './Modal.jsx';
import Alerta from './Alerta.jsx';
import { IconPlus, IconTrash } from './Icons.jsx';
import { casoUsoFormulario, casoUsoSinNombre, datosProyecto, DETALLES_CASO_USO } from '../proyecto.js';

const CATEGORIAS = ['Seguridad', 'Rendimiento', 'Usabilidad', 'Disponibilidad', 'Mantenibilidad', 'Compatibilidad'];
const vacio = { rf: () => ({ descripcion: '' }), rnf: () => ({ categoria: '', descripcion: '' }), cu: casoUsoFormulario };

function Area({ id, label, ayuda, valor, onChange, filas = 3 }) {
  return (
    <div className="campo">
      <label htmlFor={id}>{label}</label>
      <textarea id={id} rows={filas} value={valor} onChange={(e) => onChange(e.target.value)} />
      {ayuda && <small className="campo__nota">{ayuda}</small>}
    </div>
  );
}

// Lista editable de filas (requerimientos y casos de uso).
function Filas({ titulo, filas, onChange, nueva, claseCampos = '', children }) {
  const cambiar = (i, parcial) => onChange(filas.map((f, k) => (k === i ? { ...f, ...parcial } : f)));
  return (
    <fieldset className="campo filas">
      <legend>{titulo}</legend>
      {filas.map((f, i) => (
        <div className="fila" key={i}>
          <span className="fila__numero" aria-hidden="true">{i + 1}</span>
          <div className={`fila__campos ${claseCampos}`.trim()}>{children(f, (parcial) => cambiar(i, parcial), i)}</div>
          <button type="button" className="icono-btn" aria-label={`Quitar elemento ${i + 1}`} onClick={() => onChange(filas.filter((_, k) => k !== i))}>
            <IconTrash />
          </button>
        </div>
      ))}
      <button type="button" className="btn btn--ghost btn--chico" onClick={() => onChange([...filas, nueva()])}>
        <IconPlus /> Agregar
      </button>
    </fieldset>
  );
}

export default function ModalProyecto({ proyecto, onCerrar, onGuardado }) {
  const editando = Boolean(proyecto);
  const { token } = useAuth();
  const [d, setD] = useState(() => ({
    nombre: proyecto?.nombre ?? '',
    descripcion: proyecto?.descripcion ?? '',
    problematica: proyecto?.problematica ?? '',
    objetivoGeneral: proyecto?.objetivoGeneral ?? '',
    objetivosEspecificos: proyecto?.objetivosEspecificos ?? '',
    alcance: proyecto?.alcance ?? '',
    restricciones: proyecto?.restricciones ?? '',
    fecha: proyecto?.fechaEntrega ?? '',
  }));
  const [rf, setRf] = useState(() => (proyecto?.requerimientosFuncionales ?? []).map((r) => ({ codigo: r.codigo, descripcion: r.descripcion })));
  const [rnf, setRnf] = useState(() => (proyecto?.requerimientosNoFuncionales ?? []).map((r) => ({ codigo: r.codigo, categoria: r.categoria ?? '', descripcion: r.descripcion })));
  const [cu, setCu] = useState(() => (proyecto?.casosDeUso ?? []).map(casoUsoFormulario));
  const [tecnologias, setTecnologias] = useState([]);
  const [seleccion, setSeleccion] = useState(() => new Set((proyecto?.tecnologias ?? []).map((t) => t.id)));
  const [errorNombre, setErrorNombre] = useState(null);
  const [errorCasoUso, setErrorCasoUso] = useState(null);
  const [errorServidor, setErrorServidor] = useState(null);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    api.listarTecnologias(token).then(setTecnologias).catch(() => {});
  }, [token]);

  const poner = (campo) => (valor) => setD((prev) => ({ ...prev, [campo]: valor }));
  const alternar = (id) =>
    setSeleccion((prev) => {
      const sig = new Set(prev);
      if (sig.has(id)) sig.delete(id);
      else sig.add(id);
      return sig;
    });
  async function enviar(e) {
    e.preventDefault();
    if (!d.nombre.trim()) return setErrorNombre('Ingresa el nombre del proyecto.');
    const casoSinNombre = casoUsoSinNombre(cu);
    if (casoSinNombre >= 0) {
      setErrorCasoUso(`El caso de uso ${casoSinNombre + 1} tiene información, pero no tiene nombre.`);
      return;
    }
    setErrorNombre(null);
    setErrorCasoUso(null);
    setErrorServidor(null);
    setEnviando(true);
    const datos = datosProyecto({ datos: d, rf, rnf, cu, seleccion });
    try {
      const guardado = editando ? await api.editarProyecto(token, proyecto.id, datos) : await api.crearProyecto(token, datos);
      onGuardado(guardado);
    } catch (err) {
      setErrorServidor(err.message);
      setEnviando(false);
    }
  }

  return (
    <Modal titulo={editando ? 'Editar proyecto' : 'Crear proyecto'} subtitulo="Solo el nombre es obligatorio; el resto se puede completar después." ancho="ancho" onCerrar={onCerrar}>
      <form className="modal__form" onSubmit={enviar} noValidate>
        {errorServidor && <Alerta titulo={editando ? 'No se pudo guardar' : 'No se pudo crear el proyecto'}>{errorServidor}</Alerta>}

        <div className="campo">
          <label htmlFor="p-nombre">Nombre del proyecto</label>
          <div className={`campo__caja${errorNombre ? ' campo__caja--error' : ''}`}>
            <input id="p-nombre" value={d.nombre} maxLength={255} autoFocus onChange={(e) => { poner('nombre')(e.target.value); setErrorNombre(null); }} />
          </div>
          {errorNombre && <p className="campo__error">{errorNombre}</p>}
        </div>

        <Area id="p-desc" label="Descripción" valor={d.descripcion} onChange={poner('descripcion')} />
        <Area id="p-prob" label="Problemática" valor={d.problematica} onChange={poner('problematica')} />
        <Area id="p-og" label="Objetivo general" ayuda="Obligatorio para generar la planificación con IA." valor={d.objetivoGeneral} onChange={poner('objetivoGeneral')} filas={2} />
        <Area id="p-oe" label="Objetivos específicos" ayuda="Uno por línea." valor={d.objetivosEspecificos} onChange={poner('objetivosEspecificos')} />
        <div className="modal__dos">
          <Area id="p-alc" label="Alcance" ayuda="Uno por línea." valor={d.alcance} onChange={poner('alcance')} />
          <Area id="p-res" label="Restricciones técnicas" ayuda="Una por línea." valor={d.restricciones} onChange={poner('restricciones')} />
        </div>

        <div className="campo">
          <label htmlFor="p-fecha">Fecha de entrega</label>
          <div className="campo__caja"><input id="p-fecha" type="date" value={d.fecha} onChange={(e) => poner('fecha')(e.target.value)} /></div>
        </div>

        {tecnologias.length > 0 && (
          <fieldset className="campo tecnologias">
            <legend>Tecnologías</legend>
            <div className="tecnologias__lista">
              {tecnologias.map((t) => (
                <button key={t.id} type="button" className={seleccion.has(t.id) ? 'tab tab--activa' : 'tab'} aria-pressed={seleccion.has(t.id)} onClick={() => alternar(t.id)}>
                  {t.nombre}
                </button>
              ))}
            </div>
          </fieldset>
        )}

        <Filas titulo="Requerimientos funcionales" filas={rf} onChange={setRf} nueva={vacio.rf}>
          {(f, set, i) => <>{f.codigo && <code>{f.codigo}</code>}<input aria-label={`Descripción del requerimiento funcional ${i + 1}`} placeholder="Descripción" value={f.descripcion} onChange={(e) => set({ descripcion: e.target.value })} /></>}
        </Filas>

        <Filas titulo="Requerimientos no funcionales" filas={rnf} onChange={setRnf} nueva={vacio.rnf}>
          {(f, set, i) => (
            <>
              {f.codigo && <code>{f.codigo}</code>}
              <input aria-label={`Categoría del requerimiento no funcional ${i + 1}`} list="categorias-rnf" placeholder="Categoría" value={f.categoria} onChange={(e) => set({ categoria: e.target.value })} />
              <input aria-label={`Descripción del requerimiento no funcional ${i + 1}`} placeholder="Descripción" value={f.descripcion} onChange={(e) => set({ descripcion: e.target.value })} />
            </>
          )}
        </Filas>
        <datalist id="categorias-rnf">{CATEGORIAS.map((c) => <option key={c} value={c} />)}</datalist>

        <Filas titulo="Casos de uso clave" filas={cu} onChange={(filas) => { setCu(filas); setErrorCasoUso(null); }} nueva={vacio.cu} claseCampos="fila__campos--caso">
          {(f, set, i) => (
            <div className="caso-uso__campos">
              <div className="caso-uso__resumen">
                <input aria-label={`Nombre del caso de uso ${i + 1}`} placeholder="Nombre" value={f.nombre} onChange={(e) => set({ nombre: e.target.value })} />
                <input aria-label={`Descripción del caso de uso ${i + 1}`} placeholder="Descripción" value={f.descripcion} onChange={(e) => set({ descripcion: e.target.value })} />
              </div>
              <details className="caso-uso-detalles">
                <summary>Agregar detalles para la IA (opcionales)</summary>
                <div className="caso-uso__detalles">
                  {DETALLES_CASO_USO.map(({ campo, etiqueta, ayuda, filas = 2 }) => (
                    <div className="campo" key={campo}>
                      <label htmlFor={`cu-${i}-${campo}`}>{etiqueta}</label>
                      <textarea id={`cu-${i}-${campo}`} rows={filas} value={f[campo]} onChange={(e) => set({ [campo]: e.target.value })} />
                      <small className="campo__nota">{ayuda}</small>
                    </div>
                  ))}
                </div>
              </details>
            </div>
          )}
        </Filas>
        {errorCasoUso && <p className="campo__error" role="alert">{errorCasoUso}</p>}

        <div className="modal__pie">
          <button type="button" className="btn btn--ghost" onClick={onCerrar}>Cancelar</button>
          <button type="submit" className="btn btn--primario" disabled={enviando}>
            {enviando ? 'Guardando…' : editando ? 'Guardar cambios' : 'Crear proyecto'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
