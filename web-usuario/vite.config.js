import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// El backend-usuario no define CORS, por eso en desarrollo el navegador habla
// con Vite (mismo origen) y Vite reenvía /api/* al backend.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const target = env.VITE_API_PROXY_TARGET || 'http://localhost:8081';

  const proxy = { '/api': { target, changeOrigin: true } };

  return {
    plugins: [react()],
    server: { port: 5173, proxy },
    preview: { port: 5173, proxy },
  };
});
