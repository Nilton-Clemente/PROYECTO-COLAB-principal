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

  // Con token, 401/403 significan token vencido o inválido (Spring responde 403 sin entry point).
  if (token && (res.status === 401 || res.status === 403)) {
    alExpirarSesion();
    throw new ApiError(res.status, 'Tu sesión expiró. Inicia sesión nuevamente.');
  }

  if (res.status >= 500) {
    throw new ApiError(
      res.status,
      'El servidor no respondió. Verifica que backend-usuario y PostgreSQL estén en ejecución.',
    );
  }

  if (!res.ok) throw new ApiError(res.status, 'No se pudo completar la solicitud.');
  if (res.status === 204) return null;
  return res.json();
}
