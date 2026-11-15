import { useCallback, useEffect, useState } from 'react';

// Carga una lista al montar y expone su estado (cargando/error), los datos y
// una función para recargar. Evita repetir este patrón en cada pantalla.
export function useListado(cargarFn) {
  const [datos, setDatos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');

  const recargar = useCallback(async () => {
    setCargando(true);
    setError('');
    try {
      setDatos(await cargarFn());
    } catch (err) {
      setError(err.message);
    } finally {
      setCargando(false);
    }
  }, [cargarFn]);

  useEffect(() => {
    recargar();
  }, [recargar]);

  return { datos, setDatos, cargando, error, recargar };
}