import { Component } from 'react';

// Evita la pantalla en blanco si una vista lanza un error de renderizado.
export default class ErrorBoundary extends Component {
  state = { error: null };

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error, info) {
    console.error('Error de renderizado:', error, info.componentStack);
  }

  render() {
    if (!this.state.error) return this.props.children;
    return (
      <main className="auth">
        <div className="tarjeta auth__tarjeta" role="alert">
          <h1>Algo salió mal</h1>
          <p className="auth__sub">Ocurrió un error inesperado al mostrar esta pantalla.</p>
          <button type="button" className="btn btn--primario btn--bloque" onClick={() => window.location.assign('/proyectos')}>
            Volver a mis proyectos
          </button>
        </div>
      </main>
    );
  }
}
