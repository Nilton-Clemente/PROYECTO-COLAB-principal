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
      <main className="login-page">
        <section className="login-container" role="alert">
          <header className="header">
            <h1>Algo salió mal</h1>
            <p>Ocurrió un error inesperado al mostrar esta pantalla.</p>
          </header>
          <button type="button" className="submit-button" onClick={() => window.location.assign('/login')}>
            Volver al inicio de sesión
          </button>
        </section>
      </main>
    );
  }
}