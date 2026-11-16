import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import * as api from '../api/admin.js';
import { onSesionExpirada } from '../api/client.js';
import { tokenStore } from './tokenStore.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => tokenStore.get());
  const [email, setEmail] = useState(() => tokenStore.getEmail());
  const [aviso, setAviso] = useState(null);

  const cerrarSesion = useCallback(() => {
    tokenStore.limpiar();
    setToken(null);
    setEmail(null);
  }, []);

  useEffect(() => {
    onSesionExpirada(() => {
      cerrarSesion();
      setAviso('Tu sesión expiró. Inicia sesión nuevamente.');
    });
  }, [cerrarSesion]);

  const iniciarSesion = useCallback(async (correo, contrasena, recordar) => {
    const normalizado = correo.trim().toLowerCase();
    const { access, refresh } = await api.login(normalizado, contrasena);
    tokenStore.guardar({ access, refresh, email: normalizado }, recordar);
    setAviso(null);
    setToken(access);
    setEmail(normalizado);
  }, []);

  const value = useMemo(
    () => ({ token, email, aviso, iniciarSesion, cerrarSesion }),
    [token, email, aviso, iniciarSesion, cerrarSesion],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);