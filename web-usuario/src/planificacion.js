// Reglas de validación del editor de planificación (cliente, HU-06).
// Espeja lo que el backend valida al guardar (PUT /planificacion), salvo las
// fechas, que solo el servidor puede recalcular.

const PRIORIDADES = new Set(['ALTA', 'MEDIA', 'BAJA']);

function tieneCiclos(ids, dependencias) {
  const indegree = new Map([...ids].map((id) => [id, 0]));
  const dependientes = new Map();
  for (const [id, deps] of dependencias.entries()) {
    const validas = deps.filter((dep) => ids.has(dep));
    indegree.set(id, validas.length);
    for (const dep of validas) {
      if (!dependientes.has(dep)) dependientes.set(dep, []);
      dependientes.get(dep).push(id);
    }
  }
  const cola = [...indegree.entries()].filter(([, n]) => n === 0).map(([id]) => id);
  let visitados = 0;
  while (cola.length) {
    const actual = cola.shift();
    visitados += 1;
    for (const dep of dependientes.get(actual) ?? []) {
      const nuevo = indegree.get(dep) - 1;
      indegree.set(dep, nuevo);
      if (nuevo === 0) cola.push(dep);
    }
  }
  return visitados !== ids.size;
}

/**
 * Devuelve { errores: [{ clave, mensaje }] }.
 * `clave` identifica el campo (p. ej. "tarea:12:duracionDias") o es "global"
 * para problemas transversales (sin etapas, ciclos).
 */
export function validarEditor(etapas) {
  const errores = [];
  const tareas = etapas.flatMap((etapa) => etapa.tareas ?? []);
  const ids = new Set(tareas.map((tarea) => String(tarea.id)));

  if (!etapas.length) {
    errores.push({ clave: 'global', mensaje: 'Agrega al menos una etapa.' });
  }

  for (const etapa of etapas) {
    if (!etapa.nombre.trim()) {
      errores.push({ clave: `etapa:${etapa.id}:nombre`, mensaje: 'La etapa necesita un nombre.' });
    }
    for (const tarea of etapa.tareas ?? []) {
      const prefijo = `tarea:${tarea.id}`;
      if (!tarea.nombre.trim()) {
        errores.push({ clave: `${prefijo}:nombre`, mensaje: 'La tarea necesita un título.' });
      }
      const dias = Number(tarea.duracionDias);
      if (!Number.isInteger(dias) || dias < 1) {
        errores.push({ clave: `${prefijo}:duracionDias`, mensaje: 'La duración debe ser al menos un día.' });
      }
      if (!PRIORIDADES.has(tarea.prioridad)) {
        errores.push({ clave: `${prefijo}:prioridad`, mensaje: 'Selecciona una prioridad.' });
      }
      const deps = (tarea.dependencias ?? []).map(String);
      if (deps.includes(String(tarea.id))) {
        errores.push({ clave: `${prefijo}:dependencias`, mensaje: 'No puede depender de sí misma.' });
      }
      for (const dep of deps) {
        if (!ids.has(dep)) {
          errores.push({ clave: `${prefijo}:dependencias`, mensaje: 'Depende de una tarea que ya no existe.' });
        }
      }
    }
  }

  const dependencias = new Map(tareas.map((t) => [String(t.id), (t.dependencias ?? []).map(String)]));
  if (tieneCiclos(ids, dependencias)) {
    errores.push({ clave: 'global', mensaje: 'Hay dependencias con ciclos (una tarea se bloquea a sí misma de forma indirecta).' });
  }

  return { errores };
}