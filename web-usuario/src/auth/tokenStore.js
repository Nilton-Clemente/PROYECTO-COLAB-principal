const KEY = 'colab.token';

// "Mantener sesión activa" → localStorage; si no, solo dura mientras la pestaña esté abierta.
export const tokenStore = {
  get() {
    try {
      return sessionStorage.getItem(KEY) || localStorage.getItem(KEY);
    } catch {
      return null;
    }
  },
  set(token, recordar) {
    this.clear();
    try {
      (recordar ? localStorage : sessionStorage).setItem(KEY, token);
    } catch {
      /* almacenamiento no disponible */
    }
  },
  clear() {
    try {
      sessionStorage.removeItem(KEY);
      localStorage.removeItem(KEY);
    } catch {
      /* almacenamiento no disponible */
    }
  },
};
