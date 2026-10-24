import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import * as api from '../api/colab.js';
import { onSesionExpirada } from '../api/client.js';
import { tokenStore } from './tokenStore.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => tokenStore.get());
  const [aviso, setAviso] = useState(null);

  const cerrarSesion = useCallback(() => {
    tokenStore.clear();
    setToken(null);
  }, []);

  useEffect(() => {
    onSesionExpirada(() => {
      cerrarSesion();
      setAviso('Tu sesión expiró. Inicia sesión nuevamente.');
    });
  }, [cerrarSesion]);

  const iniciarSesion = useCallback(async (email, contrasena, recordar) => {
    const { token: nuevo } = await api.login(email, contrasena);
    tokenStore.set(nuevo, recordar);
    setAviso(null);
    setToken(nuevo);
  }, []);

  const registrar = useCallback(async (nombre, email, contrasena) => {
    const { token: nuevo } = await api.registro(nombre, email, contrasena);
    tokenStore.set(nuevo, true);
    setAviso(null);
    setToken(nuevo);
  }, []);

  const value = useMemo(
    () => ({ token, aviso, iniciarSesion, registrar, cerrarSesion }),
    [token, aviso, iniciarSesion, registrar, cerrarSesion],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
