import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import Toast from './Toast.jsx';

const ToastContext = createContext(null);

// Centraliza las notificaciones de toda la aplicación: las pantallas solo llaman
// a mostrarToast() y aquí se gestiona el estado y el temporizador de ocultado.
export function ToastProvider({ children }) {
  const [toast, setToast] = useState(null);

  const mostrarToast = useCallback((titulo, mensaje, tipo = 'exito') => {
    setToast({ titulo, mensaje, tipo });
  }, []);

  useEffect(() => {
    if (!toast) return undefined;
    const t = setTimeout(() => setToast(null), 3200);
    return () => clearTimeout(t);
  }, [toast]);

  const value = useMemo(() => ({ mostrarToast }), [mostrarToast]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <Toast toast={toast} />
    </ToastContext.Provider>
  );
}

export const useToast = () => useContext(ToastContext);