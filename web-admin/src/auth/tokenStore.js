// Guarda el token JWT de administración. Si `recordar` es falso se usa sessionStorage
// (la sesión se pierde al cerrar la pestaña); si es verdadero, localStorage.

const CLAVE_ACCESS = 'colabadmin.access';
const CLAVE_REFRESH = 'colabadmin.refresh';
const CLAVE_EMAIL = 'colabadmin.email';

function leer(clave) {
  return localStorage.getItem(clave) ?? sessionStorage.getItem(clave);
}

function almacen(recordar) {
  return recordar ? localStorage : sessionStorage;
}

export const tokenStore = {
  get() {
    return leer(CLAVE_ACCESS);
  },
  getEmail() {
    return leer(CLAVE_EMAIL);
  },
  guardar({ access, refresh, email }, recordar) {
    this.limpiar();
    const destino = almacen(recordar);
    destino.setItem(CLAVE_ACCESS, access);
    destino.setItem(CLAVE_REFRESH, refresh);
    destino.setItem(CLAVE_EMAIL, email);
  },
  limpiar() {
    [localStorage, sessionStorage].forEach((a) => {
      a.removeItem(CLAVE_ACCESS);
      a.removeItem(CLAVE_REFRESH);
      a.removeItem(CLAVE_EMAIL);
    });
  },
};