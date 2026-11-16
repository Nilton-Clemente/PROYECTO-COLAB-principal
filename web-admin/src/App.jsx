import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext.jsx';
import Layout from './components/Layout.jsx';
import Login from './pages/Login.jsx';
import Usuarios from './pages/Usuarios.jsx';
import Habilidades from './pages/Habilidades.jsx';
import Tecnologias from './pages/Tecnologias.jsx';

function Privada({ children }) {
  const { token } = useAuth();
  return token ? children : <Navigate to="/login" replace />;
}

function SoloInvitados({ children }) {
  const { token } = useAuth();
  return token ? <Navigate to="/usuarios" replace /> : children;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<SoloInvitados><Login /></SoloInvitados>} />
      <Route element={<Privada><Layout /></Privada>}>
        <Route path="/usuarios" element={<Usuarios />} />
        <Route path="/habilidades" element={<Habilidades />} />
        <Route path="/tecnologias" element={<Tecnologias />} />
      </Route>
      <Route path="*" element={<Navigate to="/usuarios" replace />} />
    </Routes>
  );
}