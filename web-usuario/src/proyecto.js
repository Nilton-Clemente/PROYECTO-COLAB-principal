// Campos de texto opcionales compartidos por el formulario y la consulta.
export const DETALLES_CASO_USO = [
  { campo: 'actores', etiqueta: 'Actores', ayuda: 'Personas o sistemas que participan.' },
  { campo: 'precondiciones', etiqueta: 'Precondiciones', ayuda: 'Qué debe cumplirse antes de empezar.' },
  { campo: 'disparador', etiqueta: 'Disparador', ayuda: 'Evento que inicia el caso de uso.' },
  { campo: 'flujoPrincipal', etiqueta: 'Flujo principal', ayuda: 'Escribe los pasos en orden, uno por línea.', filas: 4 },
  { campo: 'flujosAlternativos', etiqueta: 'Flujos alternativos', ayuda: 'Variantes del flujo principal.' },
  { campo: 'excepciones', etiqueta: 'Excepciones', ayuda: 'Errores y cómo debe responder el sistema.' },
  { campo: 'postcondiciones', etiqueta: 'Postcondiciones', ayuda: 'Resultado o estado al finalizar.' },
  { campo: 'reglasDeNegocio', etiqueta: 'Reglas de negocio', ayuda: 'Permisos, restricciones y validaciones del negocio.' },
];

const CAMPOS_CASO_USO = ['nombre', 'descripcion', ...DETALLES_CASO_USO.map(({ campo }) => campo)];
const texto = (valor) => valor?.trim() || null;

export function casoUsoFormulario(caso = {}) {
  return Object.fromEntries(CAMPOS_CASO_USO.map((campo) => [campo, caso[campo] ?? '']));
}

// Las filas totalmente vacías se omiten; las que contienen detalles necesitan nombre.
export function casoUsoSinNombre(casos) {
  return casos.findIndex((caso) => !texto(caso.nombre)
    && CAMPOS_CASO_USO.some((campo) => texto(caso[campo])));
}

export function datosProyecto({ datos, rf, rnf, cu, seleccion }) {
  return {
    nombre: datos.nombre.trim(),
    descripcion: texto(datos.descripcion),
    problematica: texto(datos.problematica),
    objetivoGeneral: texto(datos.objetivoGeneral),
    objetivosEspecificos: texto(datos.objetivosEspecificos),
    alcance: texto(datos.alcance),
    restricciones: texto(datos.restricciones),
    fechaEntrega: datos.fecha || null,
    tecnologiasIds: [...seleccion],
    requerimientosFuncionales: rf.filter((r) => texto(r.descripcion))
      .map((r) => ({ descripcion: texto(r.descripcion) })),
    requerimientosNoFuncionales: rnf.filter((r) => texto(r.descripcion))
      .map((r) => ({ categoria: texto(r.categoria), descripcion: texto(r.descripcion) })),
    casosDeUso: cu.filter((c) => texto(c.nombre))
      .map((c) => Object.fromEntries(CAMPOS_CASO_USO.map((campo) => [campo, texto(c[campo])]))),
  };
}