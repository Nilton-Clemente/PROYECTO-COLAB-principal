import { useCallback } from 'react';
import { useToast } from '../components/ToastProvider.jsx';

// Devuelve una función que copia al portapapeles y notifica el resultado.
export function useCopiar() {
  const { mostrarToast } = useToast();
  return useCallback(
    async (texto) => {
      try {
        await navigator.clipboard.writeText(String(texto));
        mostrarToast('Identificador copiado', `Código ${texto} en el portapapeles.`);
      } catch {
        mostrarToast('No se pudo copiar', 'Comprueba los permisos del navegador.', 'error');
      }
    },
    [mostrarToast],
  );
}