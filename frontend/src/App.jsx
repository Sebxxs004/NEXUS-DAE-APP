import { Routes, Route } from 'react-router-dom';
import { GlobalProvider } from './context/GlobalContext';
import Login from './pages/Login';
import Inicio from './pages/Inicio';
import InicioAsistente from './pages/InicioAsistente';
import AdminPanel from './pages/AdminPanel';
import CaseSelection from './pages/CaseSelection';
import Board from './pages/Board';
import DistractionAlertOverlay from './components/DistractionAlertOverlay';
import TimePenaltyAlertOverlay from './components/TimePenaltyAlertOverlay';

function App() {
  return (
    <GlobalProvider>
      <DistractionAlertOverlay />
      <TimePenaltyAlertOverlay />
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/admin" element={<AdminPanel />} />
        <Route path="/inicio" element={<Inicio />} />
        <Route path="/inicio-asistente" element={<InicioAsistente />} />
        <Route path="/cases" element={<CaseSelection />} />
        <Route path="/board/:caseId" element={<Board />} />
      </Routes>
    </GlobalProvider>
  );
}

export default App;
