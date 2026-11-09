const BASE = import.meta.env.VITE_API_URL ?? '';

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

let alExpirarSesion = () => {};
export function onSesionExpirada(fn) {
  alExpirarSesion = fn;
}

// Spring responde 403 tanto para "token inválido" como para reglas de negocio
// (p. ej. "No perteneces a este proyecto"). Para distinguirlos se consulta un
// endpoint autenticado liviano: si también lo rechaza, el token ya no sirve.
async function tokenRechazado(token) {
  try {
    const r = await fetch(`${BASE}/api/usuario/perfil`, { headers: { Authorization: `Bearer ${token}` } });
    return r.status === 401 || r.status === 403;
  } catch {
    return false;
  }
}

export async function request(path, { method = 'GET', body, token } = {}) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(`${BASE}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, 'No hay conexión con el servidor. Revisa tu red e inténtalo de nuevo.');
  }

  if (token && (res.status === 401 || (res.status === 403 && (await tokenRechazado(token))))) {
    alExpirarSesion();
    throw new ApiError(401, 'Tu sesión expiró. Inicia sesión nuevamente.');
  }

  if (res.status === 403) throw new ApiError(403, 'No tienes permiso para realizar esta acción.');

  if (res.status >= 500) {
    throw new ApiError(
      res.status,
      'El servidor no respondió. Verifica que backend-usuario y PostgreSQL estén en ejecución.',
    );
  }

  if (!res.ok) throw new ApiError(res.status, 'No se pudo completar la solicitud.');

  const texto = await res.text();
  return texto ? JSON.parse(texto) : null;
}
