import { useMemo, useState } from 'react';
import * as api from '../api/admin.js';
import ModalCatalogo from '../components/ModalCatalogo.jsx';
import IconoCatalogo from '../components/IconoCatalogo.jsx';
import ModalConfirmacion from '../components/ModalConfirmacion.jsx';
import EstadoTabla from '../components/EstadoTabla.jsx';
import Vacio from '../components/Vacio.jsx';
import { useListado } from '../hooks/useListado.js';
import { useCopiar } from '../hooks/useCopiar.js';
import { useToast } from '../components/ToastProvider.jsx';
import { IconCopy, IconEdit, IconPlus, IconSearch, IconTrash } from '../components/Icons.jsx';

const CAMPOS_TECNOLOGIA = [
  { nombre: 'nombre', etiqueta: 'Nombre', obligatorio: true, placeholder: 'Ej. React, Docker, FastAPI…', maxLength: 255 },
];

export default function Tecnologias() {
  const { datos: tecnologias, setDatos: setTecnologias, cargando, error, recargar } = useListado(api.listarTecnologias);
  const { mostrarToast } = useToast();
  const copiar = useCopiar();
  const [busqueda, setBusqueda] = useState('');
  const [modalAbierto, setModalAbierto] = useState(false);
  const [editando, setEditando] = useState(null);
  const [eliminando, setEliminando] = useState(null);
  const [conflicto, setConflicto] = useState(null);
  const [enProceso, setEnProceso] = useState(false);

  const filtradas = useMemo(() => {
    const q = busqueda.trim().toLowerCase();
    return tecnologias.filter((t) => !q || t.nombre.toLowerCase().includes(q));
  }, [tecnologias, busqueda]);

  function abrirModal(tecnologia = null) {
    setEditando(tecnologia);
    setModalAbierto(true);
  }

  async function guardarTecnologia(datos) {
    if (editando) {
      const actualizada = await api.actualizarTecnologia(editando.id, datos);
      setTecnologias((prev) => prev.map((t) => (t.id === actualizada.id ? actualizada : t)));
      mostrarToast('Tecnología actualizada', `"${actualizada.nombre}" se actualizó correctamente.`);
    } else {
      const creada = await api.crearTecnologia(datos);
      setTecnologias((prev) => [...prev, creada]);
      mostrarToast('Tecnología creada', `"${creada.nombre}" se añadió al catálogo.`);
    }
    setModalAbierto(false);
    setEditando(null);
  }

  async function confirmarEliminar() {
    const objetivo = eliminando;
    setEnProceso(true);
    try {
      await api.eliminarTecnologia(objetivo.id);
      setTecnologias((prev) => prev.filter((t) => t.id !== objetivo.id));
      mostrarToast('Tecnología eliminada', `"${objetivo.nombre}" se eliminó del catálogo.`);
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
            <span className="adm-cabecera__eyebrow">Catálogo · Tecnologías</span>
            <h1 className="adm-cabecera__titulo">Catálogo de tecnologías</h1>
            <p className="adm-cabecera__descripcion">Stack tecnológico y herramientas disponibles para los proyectos colaborativos de TECSUP.</p>
          </div>
          <button className="btn btn--primario" type="button" onClick={() => abrirModal()}>
            <IconPlus /> Nueva tecnología
          </button>
        </div>
      </section>

      <section className="adm-bar tarjeta">
        <div className="adm-busqueda">
          <span className="adm-busqueda__icono"><IconSearch /></span>
          <input
            className="adm-busqueda__input"
            type="search"
            placeholder="Buscar por nombre de tecnología…"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
        </div>
      </section>
<section className="adm-tabla tarjeta">
        <EstadoTabla cargando={cargando} error={error} onReintentar={recargar} cargandoTexto="Cargando tecnologías…" />

        {!cargando && !error && (
          <>
            <div className="adm-tabla__scroll">
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Nombre</th>
                    <th className="adm-tabla__acciones-col">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {filtradas.map((t) => (
                    <tr key={t.id}>
                      <td>
                        <span className="adm-id">
                          {t.id}
                          <button className="adm-copiar" type="button" title="Copiar ID" onClick={() => copiar(t.id)}>
                            <IconCopy />
                          </button>
                        </span>
                      </td>
                      <td>
                        <div className="adm-usuario">
                          <IconoCatalogo key={t.icono} icono={t.icono} nombre={t.nombre} className="adm-icono-catalogo" />
                          <span className="adm-usuario__nombre">{t.nombre}</span>
                        </div>
                      </td>
                      <td>
                        <div className="adm-acciones">
                          <button className="adm-accion" type="button" title="Editar tecnología" onClick={() => abrirModal(t)}>
                            <IconEdit />
                          </button>
                          <button className="adm-accion adm-accion--peligro" type="button" title="Eliminar tecnología" onClick={() => setEliminando(t)}>
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
                titulo="No se encontraron tecnologías"
                mensaje="Prueba modificando el texto de búsqueda."
                onLimpiar={() => setBusqueda('')}
              />
            )}

            <footer className="adm-tabla__pie">
              Mostrando {filtradas.length} de {tecnologias.length} tecnologías
            </footer>
          </>
        )}
      </section>

      {modalAbierto && (
        <ModalCatalogo
          titulo={editando ? 'Editar tecnología' : 'Nueva tecnología'}
          subtitulo={editando ? 'Modificando registro del catálogo.' : 'Añadir al catálogo formativo.'}
          registro={editando}
          campos={CAMPOS_TECNOLOGIA}
          onCerrar={() => { setModalAbierto(false); setEditando(null); }}
          onGuardar={guardarTecnologia}
        />
      )}

      {eliminando && (
        <ModalConfirmacion
          titulo="Eliminar tecnología"
          mensaje={<>¿Confirma eliminar la tecnología <strong>{eliminando.nombre}</strong> (ID {eliminando.id})? Esta acción no se puede deshacer.</>}
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