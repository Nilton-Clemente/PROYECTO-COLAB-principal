import { estadoProyecto } from '../cicloProyecto.js';

export default function EstadoProyecto({ estado }) {
  const { etiqueta, color } = estadoProyecto(estado);
  return <span className={`chip chip--${color} estado-proyecto`}>{etiqueta}</span>;
}