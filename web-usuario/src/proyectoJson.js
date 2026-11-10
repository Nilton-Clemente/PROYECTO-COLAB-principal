import { DETALLES_CASO_USO } from './proyecto.js';

export const EJEMPLO_PROYECTO_JSON = {
  nombre: 'Gestión de proyectos académicos',
  descripcion: 'Aplicación para organizar proyectos y equipos.',
  problematica: 'El equipo necesita centralizar sus actividades.',
  objetivoGeneral: 'Facilitar la coordinación y el seguimiento del proyecto.',
  objetivosEspecificos: '1. Organizar requerimientos.\n2. Planificar actividades.',
  alcance: 'Registro de proyectos, invitaciones y planificación.',
  restricciones: 'Usar las tecnologías acordadas por el equipo.',
  fechaEntrega: null,
  tecnologiasIds: [],
  requerimientosFuncionales: [{ descripcion: 'Permitir invitar integrantes al proyecto.' }],
  requerimientosNoFuncionales: [{ descripcion: 'Restringir las invitaciones al creador.', categoria: 'Seguridad' }],
  casosDeUso: [{
    nombre: 'Invitar integrante',
    descripcion: 'Incorporar un usuario registrado al equipo.',
    actores: 'Creador y destinatario',
    precondiciones: 'Sesión iniciada y destinatario registrado.',
    disparador: 'El creador selecciona Invitar.',
    flujoPrincipal: '1. Introducir correo.\n2. Validar invitación.\n3. Notificar al destinatario.',
    flujosAlternativos: 'El creador cancela la operación.',
    excepciones: 'El destinatario ya pertenece al equipo.',
    postcondiciones: 'Invitación pendiente de aceptación.',
    reglasDeNegocio: 'Solo el creador puede invitar.',
  }],
};

const TEXTOS_PROYECTO = ['descripcion', 'problematica', 'objetivoGeneral', 'objetivosEspecificos', 'alcance', 'restricciones'];
const TEXTOS_CASO = ['descripcion', ...DETALLES_CASO_USO.map(({ campo }) => campo)];

function objeto(valor, ruta, campos) {
  if (!valor || typeof valor !== 'object' || Array.isArray(valor)) {
    throw new Error(`${ruta} debe ser un objeto JSON.`);
  }
  const desconocido = Object.keys(valor).find((campo) => !campos.includes(campo));
  if (desconocido) throw new Error(`${ruta}.${desconocido}: campo no admitido. Revisa la plantilla; no se utilizan códigos.`);
}

function texto(valor, ruta, obligatorio = false) {
  if (valor != null && typeof valor !== 'string') throw new Error(`${ruta} debe ser texto, no un array ni un objeto.`);
  const resultado = valor?.trim() || null;
  if (obligatorio && !resultado) throw new Error(`${ruta} es obligatorio y no puede estar vacío.`);
  return resultado;
}

function lista(valor, ruta, convertir) {
  if (valor == null) return [];
  if (!Array.isArray(valor)) throw new Error(`${ruta} debe ser un array.`);
  return valor.map((item, i) => convertir(item, `${ruta}[${i}]`));
}

function fecha(valor) {
  if (valor == null) return null;
  if (typeof valor !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(valor)) {
    throw new Error('fechaEntrega debe usar el formato YYYY-MM-DD o ser null.');
  }
  const [anio, mes, dia] = valor.split('-').map(Number);
  const bisiesto = anio % 4 === 0 && (anio % 100 !== 0 || anio % 400 === 0);
  const dias = [31, bisiesto ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  if (anio < 1 || mes < 1 || mes > 12 || dia < 1 || dia > dias[mes - 1]) {
    throw new Error('fechaEntrega no es una fecha válida.');
  }
  return valor;
}

// Valida antes del POST para no descartar silenciosamente datos pegados.
// tecnologias=null significa que el catálogo no está disponible todavía.
export function leerProyectoJson(contenido, tecnologias = null) {
  let valor;
  try {
    valor = JSON.parse(contenido);
  } catch {
    throw new Error('JSON inválido. Revisa las comillas dobles, las comas y las llaves. Pega solo el objeto, sin bloques ``` ni comentarios.');
  }
  objeto(valor, 'proyecto', ['nombre', ...TEXTOS_PROYECTO, 'fechaEntrega', 'tecnologiasIds',
    'requerimientosFuncionales', 'requerimientosNoFuncionales', 'casosDeUso']);
  const nombre = texto(valor.nombre, 'nombre', true);
  const tecnologiasIds = lista(valor.tecnologiasIds, 'tecnologiasIds', (id, ruta) => {
    if (!Number.isSafeInteger(id) || id <= 0) throw new Error(`${ruta} debe ser un ID numérico entero positivo.`);
    if (tecnologias === null) throw new Error('No se pudo comprobar tecnologiasIds. Carga el catálogo e inténtalo de nuevo.');
    if (!tecnologias.some((t) => t.id === id)) throw new Error(`${ruta}: el ID ${id} no existe en el catálogo de tecnologías.`);
    return id;
  });
  return {
    nombre,
    ...Object.fromEntries(TEXTOS_PROYECTO.map((campo) => [campo, texto(valor[campo], campo)])),
    fechaEntrega: fecha(valor.fechaEntrega),
    tecnologiasIds: [...new Set(tecnologiasIds)],
    requerimientosFuncionales: lista(valor.requerimientosFuncionales, 'requerimientosFuncionales', (r, ruta) => {
      objeto(r, ruta, ['descripcion']);
      return { descripcion: texto(r.descripcion, `${ruta}.descripcion`, true) };
    }),
    requerimientosNoFuncionales: lista(valor.requerimientosNoFuncionales, 'requerimientosNoFuncionales', (r, ruta) => {
      objeto(r, ruta, ['descripcion', 'categoria']);
      return { descripcion: texto(r.descripcion, `${ruta}.descripcion`, true), categoria: texto(r.categoria, `${ruta}.categoria`) };
    }),
    casosDeUso: lista(valor.casosDeUso, 'casosDeUso', (c, ruta) => {
      objeto(c, ruta, ['nombre', ...TEXTOS_CASO]);
      return { nombre: texto(c.nombre, `${ruta}.nombre`, true),
        ...Object.fromEntries(TEXTOS_CASO.map((campo) => [campo, texto(c[campo], `${ruta}.${campo}`)])) };
    }),
  };
}