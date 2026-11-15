import { useMemo, useState } from 'react';
import * as api from '../api/admin.js';
import ModalRestablecerContrasena from '../components/ModalRestablecerContrasena.jsx';
import EstadoTabla from '../components/EstadoTabla.jsx';
import Vacio from '../components/Vacio.jsx';
import { useListado } from '../hooks/useListado.js';
import { useCopiar } from '../hooks/useCopiar.js';
import { useToast } from '../components/ToastProvider.jsx';
import {
  IconBlock, IconCheck, IconCopy, IconKey, IconSearch, IconUnlock, IconUsers,
} from '../components/Icons.jsx';
import { formatearFecha, iniciales } from '../utils.js';

const FILTROS = [
  { id: 'todos', etiqueta: 'Todos' },
  { id: 'activos', etiqueta: 'Activos' },
  { id: 'bloqueados', etiqueta: 'Bloqueados' },
];

export default function Usuarios() {
  const { datos: usuarios, setDatos: setUsuarios, cargando, error, recargar } = useListado(api.listarUsuarios);
  const { mostrarToast } = useToast();
  const copiar = useCopiar();
  const [busqueda, setBusqueda] = useState('');
  const [filtro, setFiltro] = useState('todos');
  const [modalUsuario, setModalUsuario] = useState(null);
  const [enProceso, setEnProceso] = useState(null);

  const filtrados = useMemo(() => {
    const q = busqueda.trim().toLowerCase();
    return usuarios.filter((u) => {
      const coincideBusqueda = !q
        || u.nombre.toLowerCase().includes(q)
        || u.email.toLowerCase().includes(q);
      const coincideFiltro = filtro === 'todos'
        || (filtro === 'activos' && !u.bloqueada)
        || (filtro === 'bloqueados' && u.bloqueada);
      return coincideBusqueda && coincideFiltro;
    });
  }, [usuarios, busqueda, filtro]);

  const total = usuarios.length;
  const activos = usuarios.filter((u) => !u.bloqueada).length;
  const bloqueados = total - activos;

  async function alternarBloqueo(usuario) {
    setEnProceso(usuario.id);
    try {
      await (usuario.bloqueada ? api.desbloquearUsuario(usuario.id) : api.bloquearUsuario(usuario.id));
      setUsuarios((prev) => prev.map((u) => (u.id === usuario.id ? { ...u, bloqueada: !usuario.bloqueada } : u)));
      mostrarToast(
        usuario.bloqueada ? 'Usuario habilitado' : 'Acceso revocado',
        usuario.bloqueada ? `${usuario.nombre} ahora tiene acceso al sistema.` : `${usuario.nombre} ha sido bloqueado.`,
      );
    } catch (err) {
      mostrarToast('No se pudo actualizar el usuario', err.message, 'error');
    } finally {
      setEnProceso(null);
    }
  }

  async function guardarContrasena(usuario, contrasena) {
    await api.restablecerContrasena(usuario.id, contrasena);
    setModalUsuario(null);
    mostrarToast('Contraseña actualizada', `Se restableció la contraseña de ${usuario.nombre}.`);
  }

  return (
    <div className="usuarios">
      <section className="adm-cabecera tarjeta">
        <span className="adm-cabecera__eyebrow">Módulo de cuentas · Colab</span>
        <h1 className="adm-cabecera__titulo">Gestión de usuarios</h1>
        <p className="adm-cabecera__descripcion">Supervisión y control de accesos de estudiantes y docentes en Colab.</p>
      </section>

      <section className="adm-stats" aria-label="Resumen">
        <div className="adm-stat">
          <span className="adm-stat__icono adm-stat__icono--indigo"><IconUsers /></span>
          <div>
            <div className="adm-stat__numero">{total}</div>
            <div className="adm-stat__etiqueta">Total registrados</div>
          </div>
        </div>
        <div className="adm-stat">
          <span className="adm-stat__icono adm-stat__icono--verde"><IconCheck /></span>
          <div>
            <div className="adm-stat__numero">{activos}</div>
            <div className="adm-stat__etiqueta">Cuentas activas</div>
          </div>
        </div>
        <div className="adm-stat">
          <span className="adm-stat__icono adm-stat__icono--rojo"><IconBlock /></span>
          <div>
            <div className="adm-stat__numero">{bloqueados}</div>
            <div className="adm-stat__etiqueta">Accesos bloqueados</div>
          </div>
        </div>
      </section>

      <section className="adm-bar tarjeta">
        <div className="adm-busqueda">
          <span className="adm-busqueda__icono"><IconSearch /></span>
          <input
            className="adm-busqueda__input"
            type="search"
            placeholder="Buscar por nombre o correo institucional…"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
        </div>

        <div className="adm-filtros" role="tablist" aria-label="Filtrar por estado">
          {FILTROS.map((f) => {
            const conteo = f.id === 'todos' ? total : f.id === 'activos' ? activos : bloqueados;
            return (
              <button
                key={f.id}
                type="button"
                className={`adm-filtro${filtro === f.id ? ' adm-filtro--activo' : ''}`}
                onClick={() => setFiltro(f.id)}
              >
                {f.etiqueta}
                <span className="adm-filtro__contador">{conteo}</span>
              </button>
            );
          })}
        </div>
      </section>
<section className="adm-tabla tarjeta">
        <EstadoTabla cargando={cargando} error={error} onReintentar={recargar} cargandoTexto="Cargando cuentas…" />

        {!cargando && !error && (
          <>
            <div className="adm-tabla__scroll">
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Nombre completo</th>
                    <th>Correo institucional</th>
                    <th>Fecha de registro</th>
                    <th>Estado</th>
                    <th className="adm-tabla__acciones-col">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {filtrados.map((usuario) => (
                    <tr key={usuario.id}>
                      <td>
                        <span className="adm-id">
                          {usuario.id}
                          <button className="adm-copiar" type="button" title="Copiar ID" onClick={() => copiar(usuario.id)}>
                            <IconCopy />
                          </button>
                        </span>
                      </td>
                      <td>
                        <div className="adm-usuario">
                          <span className={`adm-avatar${usuario.bloqueada ? ' adm-avatar--bloqueado' : ''}`}>
                            {iniciales(usuario.nombre)}
                          </span>
                          <span className="adm-usuario__nombre">{usuario.nombre}</span>
                        </div>
                      </td>
                      <td className="adm-celda-email">{usuario.email}</td>
                      <td>{formatearFecha(usuario.fecha_registro)}</td>
                      <td>
                        <span className={`adm-badge ${usuario.bloqueada ? 'adm-badge--bloqueado' : 'adm-badge--activo'}`}>
                          {usuario.bloqueada ? 'Bloqueado' : 'Activo'}
                        </span>
                      </td>
                      <td>
                        <div className="adm-acciones">
                          <button className="adm-accion" type="button" title="Restablecer contraseña" onClick={() => setModalUsuario(usuario)}>
                            <IconKey />
                          </button>
                          <button
                            className={`adm-accion ${usuario.bloqueada ? 'adm-accion--exito' : 'adm-accion--peligro'}`}
                            type="button"
                            disabled={enProceso === usuario.id}
                            title={usuario.bloqueada ? 'Desbloquear usuario' : 'Bloquear usuario'}
                            onClick={() => alternarBloqueo(usuario)}
                          >
                            {usuario.bloqueada ? <IconUnlock /> : <IconBlock />}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {filtrados.length === 0 && (
              <Vacio
                titulo="No se encontraron cuentas coincidentes"
                mensaje="Verifique el criterio de búsqueda o los filtros aplicados."
                onLimpiar={() => { setBusqueda(''); setFiltro('todos'); }}
              />
            )}

            <footer className="adm-tabla__pie">
              Mostrando {filtrados.length} de {total} cuentas
            </footer>
          </>
        )}
      </section>

      {modalUsuario && (
        <ModalRestablecerContrasena
          usuario={modalUsuario}
          onCerrar={() => setModalUsuario(null)}
          onGuardar={(contrasena) => guardarContrasena(modalUsuario, contrasena)}
        />
      )}
    </div>
  );
}