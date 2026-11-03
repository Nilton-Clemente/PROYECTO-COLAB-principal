import Avatares from '../../components/Avatares.jsx';
import { diasHasta, formatearFecha, iniciales, lineas, ROL } from '../../utils.js';

function Bloque({ titulo, subtitulo, extra, children, clase = '' }) {
  return (
    <section className={`tarjeta bloque ${clase}`}>
      <div className="bloque__cab">
        <div>
          <h2>{titulo}</h2>
          {subtitulo && <p className="bloque__sub">{subtitulo}</p>}
        </div>
        {extra}
      </div>
      {children}
    </section>
  );
}

// Una sola línea → párrafo; varias → lista con viñetas.
function ListaTexto({ texto }) {
  const items = lineas(texto);
  if (items.length <= 1) return <p className="texto-largo">{items[0]}</p>;
  return <ul className="vinetas">{items.map((t, i) => <li key={i}>{t}</li>)}</ul>;
}

function FechaLimite({ iso }) {
  const dias = diasHasta(iso);
  let aviso = 'Sin fecha de entrega definida';
  let tono = '';
  if (dias !== null) {
    if (dias > 1) aviso = `Faltan ${dias} días`;
    else if (dias === 1) aviso = 'Falta 1 día';
    else if (dias === 0) { aviso = 'La entrega es hoy'; tono = 'fecha--alerta'; }
    else { aviso = `Venció hace ${Math.abs(dias)} ${Math.abs(dias) === 1 ? 'día' : 'días'}`; tono = 'fecha--alerta'; }
  }
  return (
    <section className={`tarjeta lateral ${tono}`}>
      <span className="lateral__etiqueta">Fecha límite oficial</span>
      <strong className="fecha__valor">{iso ? formatearFecha(iso) : '—'}</strong>
      <p className="lateral__sub">{aviso}</p>
    </section>
  );
}

function EquipoLateral({ equipo, onVerEquipo }) {
  const lista = equipo.filter((m) => m.estado === 'ACTIVO');
  return (
    <section className="tarjeta lateral">
      <div className="bloque__cab">
        <h2>Equipo de trabajo</h2>
        <button type="button" className="enlace" onClick={onVerEquipo}>Ver todo</button>
      </div>
      <ul className="mini-equipo">
        {lista.slice(0, 5).map((m) => (
          <li key={m.integranteId}>
            <span className="avatar avatar--medio" aria-hidden="true">{iniciales(m.nombre)}</span>
            <div>
              <strong>{m.nombre}</strong>
              <small>{ROL[m.rol] ?? m.rol}</small>
            </div>
          </li>
        ))}
      </ul>
      {lista.length > 5 && <p className="lateral__sub"><Avatares personas={lista.slice(5)} limite={4} /> y {lista.length - 5} más</p>}
      <button type="button" className="btn btn--ghost btn--bloque" onClick={onVerEquipo}>Gestionar equipo</button>
    </section>
  );
}

export default function Informacion({ proyecto: p, equipo, onVerEquipo, onEditar, soyCreador }) {
  const objetivos = lineas(p.objetivosEspecificos);
  const hayAlcance = lineas(p.alcance).length > 0;
  const hayRestricciones = lineas(p.restricciones).length > 0;
  const nReq = p.requerimientosFuncionales.length + p.requerimientosNoFuncionales.length;
  const vacio = !p.problematica && !p.objetivoGeneral && !objetivos.length && !hayAlcance && !hayRestricciones && !nReq && !p.casosDeUso.length && !p.tecnologias.length;

  return (
    <div className="detalle">
      <div className="detalle__principal">
        {vacio && (
          <section className="tarjeta vacio-grande">
            <h2>Este proyecto aún no tiene detalle</h2>
            <p>Agrega la problemática, objetivos, alcance, requerimientos y casos de uso. También son necesarios para generar la planificación con IA.</p>
            {soyCreador && <button type="button" className="btn btn--primario" onClick={onEditar}>Completar información</button>}
          </section>
        )}

        {(p.descripcion || p.problematica) && (
          <Bloque titulo="Resumen y problemática" subtitulo="Contexto, alcance y valor del proyecto">
            {p.descripcion && <p className="texto-largo">{p.descripcion}</p>}
            {p.problematica && (
              <div className="callout">
                <strong>Problemática</strong>
                <p className="texto-largo">{p.problematica}</p>
              </div>
            )}
          </Bloque>
        )}

        {(p.objetivoGeneral || objetivos.length > 0) && (
          <Bloque titulo="Objetivos del proyecto" subtitulo="Metas a cumplir durante el desarrollo">
            {p.objetivoGeneral && (
              <div className="objetivo-general">
                <span>Objetivo general</span>
                <p>{p.objetivoGeneral}</p>
              </div>
            )}
            {objetivos.length > 0 && (
              <>
                <h3 className="subtitulo">Objetivos específicos</h3>
                <ul className="tarjetas-objetivo">
                  {objetivos.map((o, i) => (
                    <li key={i}><code>OE-{String(i + 1).padStart(2, '0')}</code><p>{o}</p></li>
                  ))}
                </ul>
              </>
            )}
          </Bloque>
        )}

        {(hayAlcance || hayRestricciones) && (
          <div className="dos-col">
            {hayAlcance && <Bloque titulo="Alcance del proyecto"><ListaTexto texto={p.alcance} /></Bloque>}
            {hayRestricciones && <Bloque titulo="Restricciones técnicas"><ListaTexto texto={p.restricciones} /></Bloque>}
          </div>
        )}

        {p.tecnologias.length > 0 && (
          <Bloque titulo="Tecnologías utilizadas" subtitulo="Stack tecnológico seleccionado" extra={<span className="contador">{p.tecnologias.length}</span>}>
            <ul className="techs">
              {p.tecnologias.map((t) => (
                <li key={t.id}><span className="tech__icono" aria-hidden="true">{t.nombre.slice(0, 2)}</span><strong>{t.nombre}</strong></li>
              ))}
            </ul>
          </Bloque>
        )}

        {nReq > 0 && (
          <Bloque titulo="Requerimientos del sistema" subtitulo="Especificación técnica del proyecto" extra={<span className="bloque__sub">{nReq} registrados</span>}>
            {p.requerimientosFuncionales.length > 0 && (
              <>
                <h3 className="subtitulo">Requerimientos funcionales (RF)</h3>
                <div className="tabla-envoltura">
                  <table className="tabla">
                    <thead><tr><th>Código</th><th>Descripción</th></tr></thead>
                    <tbody>
                      {p.requerimientosFuncionales.map((r, i) => (
                        <tr key={r.codigo ?? i}><td><code>{r.codigo ?? '—'}</code></td><td>{r.descripcion}</td></tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </>
            )}
            {p.requerimientosNoFuncionales.length > 0 && (
              <>
                <h3 className="subtitulo">Requerimientos no funcionales (RNF)</h3>
                <ul className="tarjetas-rnf">
                  {p.requerimientosNoFuncionales.map((r, i) => (
                    <li key={r.codigo ?? i}>
                      <div><code>{r.codigo ?? '—'}</code>{r.categoria && <span className="chip chip--gris">{r.categoria}</span>}</div>
                      <p>{r.descripcion}</p>
                    </li>
                  ))}
                </ul>
              </>
            )}
          </Bloque>
        )}

        {p.casosDeUso.length > 0 && (
          <Bloque titulo="Casos de uso clave" subtitulo="Interacciones principales del usuario">
            <ul className="tarjetas-cu">
              {p.casosDeUso.map((c, i) => (
                <li key={c.codigo ?? i}>
                  <code>{c.codigo ?? '—'}</code>
                  <strong>{c.nombre}</strong>
                  {c.descripcion && <p>{c.descripcion}</p>}
                </li>
              ))}
            </ul>
          </Bloque>
        )}
      </div>

      <aside className="detalle__lateral" aria-label="Resumen del proyecto">
        <FechaLimite iso={p.fechaEntrega} />
        <EquipoLateral equipo={equipo} onVerEquipo={onVerEquipo} />
      </aside>
    </div>
  );
}
