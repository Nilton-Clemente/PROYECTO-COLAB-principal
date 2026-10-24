import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext.jsx';
import Login from './pages/Login.jsx';
import Registro from './pages/Registro.jsx';
import Perfil from './pages/Perfil.jsx';

function Privada({ children }) {
  const { token } = useAuth();
  return token ? children : <Navigate to="/login" replace />;
}

function SoloInvitados({ children }) {
  const { token } = useAuth();
  return token ? <Navigate to="/perfil" replace /> : children;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<SoloInvitados><Login /></SoloInvitados>} />
      <Route path="/registro" element={<SoloInvitados><Registro /></SoloInvitados>} />
      <Route path="/perfil" element={<Privada><Perfil /></Privada>} />
      <Route path="*" element={<Navigate to="/perfil" replace />} />
    </Routes>
  );
}
