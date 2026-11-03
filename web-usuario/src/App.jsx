import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext.jsx';
import Layout from './components/Layout.jsx';
import Login from './pages/Login.jsx';
import Registro from './pages/Registro.jsx';
import Proyectos from './pages/Proyectos.jsx';
import ProyectoDetalle from './pages/ProyectoDetalle.jsx';
import Invitaciones from './pages/Invitaciones.jsx';
import Perfil from './pages/Perfil.jsx';

function Privada({ children }) {
  const { token } = useAuth();
  return token ? children : <Navigate to="/login" replace />;
}

function SoloInvitados({ children }) {
  const { token } = useAuth();
  return token ? <Navigate to="/proyectos" replace /> : children;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<SoloInvitados><Login /></SoloInvitados>} />
      <Route path="/registro" element={<SoloInvitados><Registro /></SoloInvitados>} />
      <Route element={<Privada><Layout /></Privada>}>
        <Route path="/proyectos" element={<Proyectos />} />
        <Route path="/proyectos/:id/:tab?" element={<ProyectoDetalle />} />
        <Route path="/invitaciones" element={<Invitaciones />} />
        <Route path="/perfil" element={<Perfil />} />
      </Route>
      <Route path="*" element={<Navigate to="/proyectos" replace />} />
    </Routes>
  );
}
