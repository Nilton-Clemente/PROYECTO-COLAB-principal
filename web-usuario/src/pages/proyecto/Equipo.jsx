import { useMemo } from 'react';
import { IconPlus, IconTrash } from '../../components/Icons.jsx';
import { ESTADO, iniciales, ROL } from '../../utils.js';

const PESO = { ACTIVO: 1, PENDIENTE: 2 };
const orden = (m) => (m.rol === 'CREADOR' ? 0 : PESO[m.estado] ?? 3);

function Fila({ miembro, puedeGestionar, ocupado, onQuitar }) {
  const esCreador = miembro.rol === 'CREADOR';
  const pendiente = miembro.estado === 'PENDIENTE';
  return (
    <li className="miembro">
      <span className="avatar avatar--medio" aria-hidden="true">{iniciales(miembro.nombre)}</span>
      <div className="miembro__datos">
        <div className="miembro__nombre">
          <strong>{miembro.nombre}</strong>
          <span className={esCreador ? 'chip' : 'chip chip--gris'}>{ROL[miembro.rol] ?? miembro.rol}</span>
          <span className={pendiente ? 'chip chip--ambar' : 'chip chip--verde'}>{ESTADO[miembro.estado] ?? miembro.estado}</span>
        </div>
        <small>{miembro.email}</small>
      </div>
      {esCreador ? (
        <span className="miembro__propietario">Propietario</span>
      ) : (
        puedeGestionar && (
          <button type="button" className="btn btn--peligro" disabled={ocupado} onClick={() => onQuitar(miembro)}>
            <IconTrash /> {pendiente ? 'Cancelar invitación' : 'Eliminar del equipo'}
          </button>
        )
      )}
    </li>
  );
}

export default function Equipo({ equipo, soyCreador, ocupado, onInvitar, onQuitar }) {
  const ordenado = useMemo(() => [...equipo].sort((a, b) => orden(a) - orden(b)), [equipo]);
  const activos = equipo.filter((m) => m.estado === 'ACTIVO').length;
  const pendientes = equipo.filter((m) => m.estado === 'PENDIENTE').length;

  return (
    <section aria-label="Equipo del proyecto">
      <div className="equipo__cab tarjeta">
        <div>
          <strong>Integrantes del equipo</strong>
          <p className="seccion__sub">
            {activos} {activos === 1 ? 'activo' : 'activos'}
            {pendientes > 0 && ` · ${pendientes} ${pendientes === 1 ? 'invitación pendiente' : 'invitaciones pendientes'}`}
          </p>
        </div>
        {soyCreador && (
          <button type="button" className="btn btn--primario" onClick={onInvitar}><IconPlus /> Invitar integrante</button>
        )}
      </div>
      <ul className="miembros">
        {ordenado.map((m) => (
          <Fila key={m.integranteId} miembro={m} puedeGestionar={soyCreador} ocupado={ocupado === m.integranteId} onQuitar={onQuitar} />
        ))}
      </ul>
    </section>
  );
}
