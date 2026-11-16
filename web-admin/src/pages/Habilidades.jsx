import { useMemo, useState } from 'react';
import * as api from '../api/admin.js';
import ModalCatalogo from '../components/ModalCatalogo.jsx';
import ModalConfirmacion from '../components/ModalConfirmacion.jsx';
import EstadoTabla from '../components/EstadoTabla.jsx';
import Vacio from '../components/Vacio.jsx';
import { useListado } from '../hooks/useListado.js';
import { useCopiar } from '../hooks/useCopiar.js';
import { useToast } from '../components/ToastProvider.jsx';
import { IconCopy, IconEdit, IconPlus, IconSearch, IconTrash } from '../components/Icons.jsx';
import { colorArea } from '../utils.js';

const CAMPOS_HABILIDAD = [
  { nombre: 'nombre', etiqueta: 'Nombre', obligatorio: true, placeholder: 'Ej. Desarrollo Frontend React', maxLength: 100 },
  { nombre: 'area', etiqueta: 'Área', obligatorio: false, placeholder: 'Ej. Desarrollo de Software', maxLength: 255 },
];

export default function Habilidades() {
  const { datos: habilidades, setDatos: setHabilidades, cargando, error, recargar } = useListado(api.listarHabilidades);
  const { mostrarToast } = useToast();
  const copiar = useCopiar();
  const [busqueda, setBusqueda] = useState('');
  const [areaFiltro, setAreaFiltro] = useState('todas');
  const [modalAbierto, setModalAbierto] = useState(false);
  const [editando, setEditando] = useState(null);
  const [eliminando, setEliminando] = useState(null);
  const [conflicto, setConflicto] = useState(null);
  const [enProceso, setEnProceso] = useState(false);

  const areas = useMemo(() => {
    const conjunto = new Set(habilidades.map((h) => h.area).filter(Boolean));
    return [...conjunto].sort((a, b) => a.localeCompare(b));
  }, [habilidades]);

  const filtradas = useMemo(() => {
    const q = busqueda.trim().toLowerCase();
    return habilidades.filter((h) => {
      const coincideBusqueda = !q
        || h.nombre.toLowerCase().includes(q)
        || (h.area || '').toLowerCase().includes(q);
      const coincideArea = areaFiltro === 'todas' || h.area === areaFiltro;
      return coincideBusqueda && coincideArea;
    });
  }, [habilidades, busqueda, areaFiltro]);

  function abrirModal(habilidad = null) {
    setEditando(habilidad);
    setModalAbierto(true);
  }

  async function guardarHabilidad(datos) {
    if (editando) {
      const actualizada = await api.actualizarHabilidad(editando.id, datos);
      setHabilidades((prev) => prev.map((h) => (h.id === actualizada.id ? actualizada : h)));
      mostrarToast('Habilidad actualizada', `"${actualizada.nombre}" se actualizó correctamente.`);
    } else {
      const creada = await api.crearHabilidad(datos);
      setHabilidades((prev) => [...prev, creada]);
      mostrarToast('Habilidad creada', `"${creada.nombre}" se añadió al catálogo.`);
    }
    setModalAbierto(false);
    setEditando(null);
  }

  async function confirmarEliminar() {
    const objetivo = eliminando;
    setEnProceso(true);
    try {
      await api.eliminarHabilidad(objetivo.id);
      setHabilidades((prev) => prev.filter((h) => h.id !== objetivo.id));
      mostrarToast('Habilidad eliminada', `"${objetivo.nombre}" se eliminó del catálogo.`);
      setEliminando(null);
    } catch (err) {
      setEliminando(null);
      if (err.status === 409) setConflicto(err.message);
      else mostrarToast('No se pudo eliminar', err.message, 'error');
    } finally {
      setEnProceso(false);
    }
  }

  return (
    <div className="usuarios">
      <section className="adm-cabecera tarjeta">
        <div className="adm-cabecera__fila">
          <div>
            <span className="adm-cabecera__eyebrow">Catálogo · Habilidades</span>
            <h1 className="adm-cabecera__titulo">Catálogo de habilidades</h1>
            <p className="adm-cabecera__descripcion">Habilidades técnicas y blandas disponibles para los perfiles estudiantiles de TECSUP.</p>
          </div>
          <button className="btn btn--primario" type="button" onClick={() => abrirModal()}>
            <IconPlus /> Nueva habilidad
          </button>
        </div>
      </section>

      <section className="adm-bar tarjeta">
        <div className="adm-busqueda">
          <span className="adm-busqueda__icono"><IconSearch /></span>
          <input
            className="adm-busqueda__input"
            type="search"
            placeholder="Buscar por nombre o área…"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
        </div>

        <label className="adm-filtro-area">
          <span>Área:</span>
          <select className="adm-select" value={areaFiltro} onChange={(e) => setAreaFiltro(e.target.value)}>
            <option value="todas">Todas las áreas</option>
            {areas.map((a) => <option key={a} value={a}>{a}</option>)}
          </select>
        </label>
      </section>
<section className="adm-tabla tarjeta">
        <EstadoTabla cargando={cargando} error={error} onReintentar={recargar} cargandoTexto="Cargando habilidades…" />

        {!cargando && !error && (
          <>
            <div className="adm-tabla__scroll">
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Nombre</th>
                    <th>Área</th>
                    <th className="adm-tabla__acciones-col">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {filtradas.map((h) => (
                    <tr key={h.id}>
                      <td>
                        <span className="adm-id">
                          {h.id}
                          <button className="adm-copiar" type="button" title="Copiar ID" onClick={() => copiar(h.id)}>
                            <IconCopy />
                          </button>
                        </span>
                      </td>
                      <td><span className="adm-usuario__nombre">{h.nombre}</span></td>
                      <td>
                        {h.area ? (
                          <span className="adm-area">
                            <span className="adm-area__punto" style={{ background: colorArea(h.area) }} />
                            {h.area}
                          </span>
                        ) : (
                          <span className="adm-sin-area">Sin área</span>
                        )}
                      </td>
                      <td>
                        <div className="adm-acciones">
                          <button className="adm-accion" type="button" title="Editar habilidad" onClick={() => abrirModal(h)}>
                            <IconEdit />
                          </button>
                          <button className="adm-accion adm-accion--peligro" type="button" title="Eliminar habilidad" onClick={() => setEliminando(h)}>
                            <IconTrash />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {filtradas.length === 0 && (
              <Vacio
                titulo="No se encontraron habilidades"
                mensaje="Prueba modificando el texto de búsqueda o el área seleccionada."
                onLimpiar={() => { setBusqueda(''); setAreaFiltro('todas'); }}
              />
            )}

            <footer className="adm-tabla__pie">
              Mostrando {filtradas.length} de {habilidades.length} habilidades
            </footer>
          </>
        )}
      </section>

      {modalAbierto && (
        <ModalCatalogo
          titulo={editando ? 'Editar habilidad' : 'Nueva habilidad'}
          subtitulo={editando ? 'Modificando registro del catálogo.' : 'Añadir al catálogo formativo.'}
          registro={editando}
          campos={CAMPOS_HABILIDAD}
          onCerrar={() => { setModalAbierto(false); setEditando(null); }}
          onGuardar={guardarHabilidad}
        />
      )}

      {eliminando && (
        <ModalConfirmacion
          titulo="Eliminar habilidad"
          mensaje={<>¿Confirma eliminar la habilidad <strong>{eliminando.nombre}</strong> (ID {eliminando.id})? Esta acción no se puede deshacer.</>}
          onCerrar={() => setEliminando(null)}
          confirmar={{ etiqueta: 'Eliminar', onConfirmar: confirmarEliminar, cargando: enProceso }}
        />
      )}

      {conflicto && (
        <ModalConfirmacion titulo="No se puede eliminar" mensaje={conflicto} variante="aviso" onCerrar={() => setConflicto(null)} />
      )}
    </div>
  );
}