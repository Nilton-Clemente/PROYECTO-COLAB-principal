import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// El backend-admin (Django) no define CORS: en desarrollo el navegador habla con
// Vite (mismo origen) y Vite reenvía /api/* hacia el backend de administración.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const target = env.VITE_API_PROXY_TARGET || 'http://localhost:8000';

  const proxy = { '/api': { target, changeOrigin: true } };

  return {
    plugins: [react()],
    server: { port: 5174, proxy },
    preview: { port: 5174, proxy },
  };
});