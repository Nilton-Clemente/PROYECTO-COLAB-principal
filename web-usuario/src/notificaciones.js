const TIPOS = {
  PLANIFICACION: 'Planificación',
  ASIGNACION: 'Asignación de tarea',
  CAMBIO_ESTADO: 'Cambio de estado',
  CAMBIO_RESPONSABLE: 'Cambio de responsable',
  DESBLOQUEO: 'Tarea desbloqueada',
};

export const tipoNotificacion = (tipo) => TIPOS[tipo] ?? 'Aviso del proyecto';

export function proyectoNotificacion(aviso) {
  const nombre = aviso.proyectoNombre?.trim();
  const codigo = aviso.proyectoCodigo?.trim() || (aviso.proyectoId != null ? `TEC-${aviso.proyectoId}` : '');
  return [nombre, codigo].filter(Boolean).join(' · ') || 'Proyecto no identificado';
}

// LocalDateTime no incluye zona: conservar la hora que entrega el backend.
export function fechaNotificacion(fecha) {
  if (!fecha) return 'Fecha no disponible';
  const partes = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}:\d{2}(?::\d{2})?)(?:\.\d+)?$/.exec(fecha);
  return partes ? `${partes[3]}/${partes[2]}/${partes[1]} ${partes[4]}` : fecha;
}

export function validarNotificacion(aviso) {
  if (!aviso || !Number.isSafeInteger(aviso.id) || aviso.id <= 0 ||
      typeof aviso.mensaje !== 'string' || typeof aviso.leida !== 'boolean') {
    throw new Error('El servidor devolvió una notificación inválida.');
  }
  return aviso;
}

export function validarBandeja(lista) {
  if (!Array.isArray(lista)) throw new Error('No se pudo confirmar la bandeja de notificaciones.');
  lista.forEach(validarNotificacion);
  if (new Set(lista.map((aviso) => aviso.id)).size !== lista.length) {
    throw new Error('El servidor devolvió notificaciones duplicadas.');
  }
  return lista;
}

export function validarContador(datos) {
  if (!Number.isSafeInteger(datos?.noLeidas) || datos.noLeidas < 0) {
    throw new Error('No se pudo consultar el contador de notificaciones.');
  }
  return datos.noLeidas;
}

/** Estado compartido por la navegación y la bandeja, aislado por sesión. */
export function crearBandejaNotificaciones(api) {
  let estado = {
    lista: null, noLeidas: null, cargando: false, operacion: null,
    error: '', desactualizado: false, contadorError: false,
  };
  const oyentes = new Set();
  let revision = 0;
  let contando = null;
  const ocupado = () => estado.cargando || estado.operacion !== null;
  const publicar = (cambios) => {
    estado = { ...estado, ...cambios };
    oyentes.forEach((oyente) => oyente());
  };
  const actualizarLista = (lista) => publicar({
    lista, noLeidas: lista.filter((aviso) => !aviso.leida).length,
    desactualizado: false, contadorError: false,
  });

  async function consultar(aviso = '') {
    try {
      actualizarLista(validarBandeja(await api.listar()));
      publicar({ error: aviso });
    } catch (err) {
      publicar({
        desactualizado: true, contadorError: true,
        error: [aviso, err.message || 'No se pudieron cargar las notificaciones.'].filter(Boolean).join('\n'),
      });
    }
  }

  async function cargar() {
    if (ocupado()) return;
    revision++;
    publicar({ cargando: true, error: '' });
    try { await consultar(); }
    finally { publicar({ cargando: false }); }
  }

  async function refrescarContador() {
    if (ocupado()) return;
    if (contando) return contando;
    const actual = revision;
    contando = (async () => {
      try {
        const noLeidas = validarContador(await api.contar());
        if (actual === revision) publicar({ noLeidas, contadorError: false });
      } catch {
        if (actual === revision) publicar({ contadorError: true });
      }
    })();
    try { await contando; }
    finally { contando = null; }
  }

  async function marcarLeida(id) {
    if (ocupado() || estado.desactualizado || !estado.lista?.some((aviso) => aviso.id === id && !aviso.leida)) return;
    revision++;
    publicar({ operacion: id, error: '' });
    try {
      const aviso = validarNotificacion(await api.marcarLeida(id));
      if (aviso.id !== id || !aviso.leida) throw new Error('El servidor no confirmó la lectura de esta notificación.');
      actualizarLista(estado.lista.map((actual) => actual.id === id ? aviso : actual));
    } catch (err) {
      // Puede haberse guardado aunque se perdiera la respuesta. Consultar, no repetir el PUT.
      await consultar(err.message || 'No se pudo confirmar la lectura.');
    } finally { publicar({ operacion: null }); }
  }

  async function eliminarLeidas() {
    if (ocupado() || estado.desactualizado || !estado.lista?.some((aviso) => aviso.leida)) return;
    revision++;
    publicar({ operacion: 'eliminar', error: '' });
    try {
      await api.eliminarLeidas();
      // No resucitar lo que el servidor confirmó borrado si falla la consulta posterior.
      actualizarLista(estado.lista.filter((aviso) => !aviso.leida));
      await consultar();
    } catch (err) {
      // Un fallo de red no garantiza que el DELETE no se haya aplicado.
      await consultar(err.message || 'No se pudo confirmar la eliminación de las notificaciones leídas.');
    } finally { publicar({ operacion: null }); }
  }

  return {
    getSnapshot: () => estado,
    subscribe: (oyente) => { oyentes.add(oyente); return () => oyentes.delete(oyente); },
    cargar, refrescarContador, marcarLeida, eliminarLeidas,
  };
}