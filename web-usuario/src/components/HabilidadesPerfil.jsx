import { Link } from 'react-router-dom';
import { IconCheck, IconTrash } from './Icons.jsx';
import IconoHabilidad from './IconoHabilidad.jsx';
import { etiquetaNivel, LIMITE_HABILIDADES_PERFIL, nivelValido } from '../habilidades.js';
import { etiqueta } from '../utils.js';

function IndicadorNivel({ nivel }) {
  if (!nivelValido(nivel)) return null;

  return (
    <span className={`perfil-habilidades__indicador perfil-habilidades__indicador--${nivel.toLowerCase()}`} aria-hidden="true">
      {nivel === 'BASICO' ? <IconCheck /> : (
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          {nivel === 'AVANZADO'
            ? <><circle cx="12" cy="8" r="5" /><path d="m8 12-2 9 6-3 6 3-2-9" /></>
            : <path d="m13 2-9 12h7l-1 8 10-12h-7l1-8Z" />}
        </svg>
      )}
    </span>
  );
}

function TarjetaHabilidad({ habilidad, ocupada, onEliminar }) {
  const nombre = etiqueta(habilidad.nombre);

  return (
    <li className="perfil-habilidades__tarjeta">
      <IndicadorNivel nivel={habilidad.nivel} />
      <IconoHabilidad habilidad={habilidad} />
      <h3>{nombre}</h3>
      <span className="perfil-habilidades__nivel">{etiquetaNivel(habilidad.nivel)}</span>
      <button type="button" className="btn perfil-habilidades__quitar" disabled={ocupada}
        onClick={() => onEliminar(habilidad)} aria-label={`Quitar ${nombre} de mi perfil`}>
        <IconTrash /> {ocupada ? 'Quitando…' : 'Quitar'}
      </button>
    </li>
  );
}

export function MisHabilidades({ habilidades, ocupadas, mostrarTodas, onMostrarTodas, onEliminar, verTodasRef }) {
  const restantes = Math.max(0, habilidades.length - LIMITE_HABILIDADES_PERFIL);
  const visibles = mostrarTodas ? habilidades : habilidades.slice(0, LIMITE_HABILIDADES_PERFIL);

  return (
    <section className="tarjeta perfil-habilidades" aria-labelledby="mis-habilidades">
      <div className="perfil-habilidades__cab">
        <div>
          <div className="seccion__cab">
            <h2 id="mis-habilidades">Mis habilidades</h2>
            <span className="contador">{habilidades.length} {habilidades.length === 1 ? 'habilidad' : 'habilidades'}</span>
          </div>
          <p className="seccion__sub">Tecnologías y competencias que has registrado para tus proyectos de sprint.</p>
        </div>
        {restantes > 0 && (
          <button ref={verTodasRef} type="button" className="btn perfil-habilidades__ver" aria-expanded={mostrarTodas}
            aria-controls="lista-mis-habilidades" onClick={() => onMostrarTodas(!mostrarTodas)}>
            {mostrarTodas ? 'Ver menos' : 'Ver todas'} <span aria-hidden="true">›</span>
          </button>
        )}
      </div>
      {habilidades.length === 0 ? (
        <p className="vacio">Aún no tienes habilidades. Explora el catálogo para agregar tus competencias.</p>
      ) : (
        <ul className="perfil-habilidades__grilla" id="lista-mis-habilidades">
          {visibles.map((habilidad) => (
            <TarjetaHabilidad key={habilidad.id} habilidad={habilidad} ocupada={ocupadas.has(habilidad.id)} onEliminar={onEliminar} />
          ))}
          {!mostrarTodas && restantes > 0 && (
            <li className="perfil-habilidades__mas">
              <button type="button" className="btn" onClick={() => {
                onMostrarTodas(true);
                verTodasRef.current?.focus();
              }} aria-controls="lista-mis-habilidades" aria-expanded={false}
                aria-label={`Ver ${restantes} ${restantes === 1 ? 'habilidad más' : 'habilidades más'}`}>
                <strong className="perfil-habilidades__restantes" aria-hidden="true">+{restantes}</strong>
                <span>Ver más</span>
                <small>Todas</small>
              </button>
            </li>
          )}
        </ul>
      )}
    </section>
  );
}

export function AccesoCatalogoHabilidades() {
  return (
    <section className="tarjeta perfil-habilidades__acceso" aria-labelledby="catalogo">
      <div className="perfil-habilidades__catalogo-texto">
        <span className="perfil-habilidades__catalogo-icono" aria-hidden="true">
          <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="9" /><path d="m16 8-2.5 5.5L8 16l2.5-5.5L16 8Z" />
          </svg>
        </span>
        <div>
          <h2 id="catalogo">Explorar catálogo de habilidades</h2>
          <p className="seccion__sub">Descubre nuevas competencias tecnológicas y añade a tu perfil las que dominas.</p>
        </div>
      </div>
      <Link to="/perfil/habilidades" className="btn perfil-habilidades__catalogo-boton">
        Ir al catálogo <span aria-hidden="true">→</span>
      </Link>
    </section>
  );
}