import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { FaSignInAlt, FaBookOpen, FaPowerOff, FaUserShield, FaLock, FaUser } from 'react-icons/fa';
import axios from 'axios';
import { useGlobal } from '../context/GlobalContext';

export default function Login() {
  const navigate = useNavigate();
  const { loginUser } = useGlobal();
  const [showModal, setShowModal] = useState(false);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [authError, setAuthError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleStart = () => {
    setAuthError('');
    setShowModal(true);
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    setAuthError('');

    if (!username.trim() || !password.trim()) {
      setAuthError('Por favor ingrese su usuario y contraseña.');
      return;
    }

    setSubmitting(true);
    try {
      const response = await axios.post('http://localhost:8080/api/auth/login', {
        username: username.trim(),
        password: password
      });

      const userData = response.data;
      loginUser(userData);

      if (userData.rol === 'ADMIN') {
        navigate('/admin');
      } else if (userData.rol === 'ASISTENTE') {
        localStorage.setItem('nexus_timer_start', Date.now().toString());
        navigate('/inicio-asistente');
      } else {
        // FISCAL
        localStorage.setItem('nexus_timer_start', Date.now().toString());
        navigate('/inicio');
      }
    } catch (err) {
      if (err.response && err.response.data && err.response.data.message) {
        setAuthError(err.response.data.message);
      } else {
        setAuthError('No se pudo conectar con el servidor de autenticación (puerto 8080).');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ 
      backgroundImage: "url('/assets/fondo-login.png')", 
      backgroundSize: 'cover',
      backgroundPosition: 'center',
      height: '100vh',
      width: '100vw',
      display: 'flex',
      alignItems: 'center',
      paddingLeft: '5rem',
      position: 'relative'
    }}>
      
      <div style={{ maxWidth: '420px', color: 'white', zIndex: 10 }}>
        {/* Top logo area */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '20px' }}>
          <div style={{ backgroundColor: '#F59E0B', color: 'black', padding: '5px 8px', fontWeight: 'bold', borderRadius: '4px', fontSize: '14px' }}>FGN</div>
          <div>
            <div style={{ fontWeight: 'bold', fontSize: '14px' }}>NEXUS DAE</div>
            <div style={{ fontSize: '10px' }}>FISCALÍA GENERAL DE LA NACIÓN</div>
          </div>
        </div>

        {/* Status */}
        <div style={{ 
          display: 'inline-flex', 
          alignItems: 'center', 
          gap: '8px', 
          backgroundColor: 'rgba(255,255,255,0.1)', 
          padding: '5px 12px', 
          borderRadius: '20px',
          border: '1px solid rgba(255,255,255,0.2)',
          fontSize: '12px',
          marginBottom: '30px',
          backdropFilter: 'blur(5px)'
        }}>
          <div style={{ width: '8px', height: '8px', backgroundColor: '#10B981', borderRadius: '50%' }}></div>
          Sistema activo · Fiscales y Asistentes
        </div>

        {/* Titles */}
        <h1 style={{ fontSize: '3rem', fontWeight: '800', lineHeight: '1.1', marginBottom: '20px' }}>
          Actividad de <br/>
          simulación <br/>
          interactiva <br/>
          de Despacho <br/>
          <span style={{ color: '#F59E0B' }}>Fiscal</span>
        </h1>
        <p style={{ fontSize: '14px', marginBottom: '40px', opacity: 0.9, lineHeight: '1.5' }}>
          Acceso autorizado para Fiscales Delegados, Asistentes Judiciales y Administración de Equipos.
        </p>

        {/* Buttons */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
          
          <button onClick={handleStart} style={{ 
            backgroundColor: '#FDE047', 
            color: 'black', 
            border: 'none',
            borderRadius: '8px',
            padding: '15px 20px',
            display: 'flex',
            alignItems: 'center',
            gap: '15px',
            cursor: 'pointer',
            textAlign: 'left',
            transition: 'transform 0.2s',
            boxShadow: '0 4px 6px rgba(0,0,0,0.1)'
          }}
          onMouseOver={(e) => e.currentTarget.style.transform = 'scale(1.02)'}
          onMouseOut={(e) => e.currentTarget.style.transform = 'scale(1)'}
          >
            <FaSignInAlt size={24} color="rgba(0,0,0,0.5)" />
            <div>
              <div style={{ fontWeight: 'bold', fontSize: '16px' }}>Ingresar a NEXUS</div>
              <div style={{ fontSize: '12px', opacity: 0.8 }}>Iniciar sesión por rol (Fiscal / Asistente / Admin)</div>
            </div>
          </button>

          <button style={{ 
            backgroundColor: 'rgba(255,255,255,0.1)', 
            color: 'white', 
            border: '1px solid rgba(255,255,255,0.3)',
            backdropFilter: 'blur(5px)',
            borderRadius: '8px',
            padding: '15px 20px',
            display: 'flex',
            alignItems: 'center',
            gap: '15px',
            cursor: 'pointer',
            textAlign: 'left',
            transition: 'all 0.2s'
          }}
          onMouseOver={(e) => { e.currentTarget.style.transform = 'scale(1.02)'; e.currentTarget.style.backgroundColor = 'rgba(255,255,255,0.2)'; }}
          onMouseOut={(e) => { e.currentTarget.style.transform = 'scale(1)'; e.currentTarget.style.backgroundColor = 'rgba(255,255,255,0.1)'; }}
          >
            <FaBookOpen size={24} color="white" />
            <div>
              <div style={{ fontWeight: 'bold', fontSize: '16px' }}>Leer instrucciones</div>
              <div style={{ fontSize: '12px', opacity: 0.7 }}>Cómo funciona el simulador</div>
            </div>
          </button>

          <button style={{ 
            backgroundColor: 'rgba(239, 68, 68, 0.1)', 
            color: '#EF4444', 
            border: '1px solid rgba(239, 68, 68, 0.5)',
            backdropFilter: 'blur(5px)',
            borderRadius: '8px',
            padding: '15px 20px',
            display: 'flex',
            alignItems: 'center',
            gap: '15px',
            cursor: 'pointer',
            textAlign: 'left',
            transition: 'all 0.2s'
          }}
          onMouseOver={(e) => { e.currentTarget.style.transform = 'scale(1.02)'; e.currentTarget.style.backgroundColor = 'rgba(239, 68, 68, 0.2)'; }}
          onMouseOut={(e) => { e.currentTarget.style.transform = 'scale(1)'; e.currentTarget.style.backgroundColor = 'rgba(239, 68, 68, 0.1)'; }}
          >
            <FaPowerOff size={24} color="#EF4444" />
            <div>
              <div style={{ fontWeight: 'bold', fontSize: '16px' }}>Salir del sistema</div>
              <div style={{ fontSize: '12px', opacity: 0.7 }}>Cerrar la aplicación</div>
            </div>
          </button>

        </div>
      </div>

      {/* MODAL DE AUTENTICACIÓN POR ROL */}
      <AnimatePresence>
        {showModal && (
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            style={{
              position: 'absolute',
              top: 0, left: 0, width: '100%', height: '100%',
              backgroundColor: 'rgba(0,0,0,0.8)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              zIndex: 100
            }}
          >
            <motion.div 
              initial={{ scale: 0.9, y: 50 }}
              animate={{ scale: 1, y: 0 }}
              exit={{ scale: 0.9, y: 50 }}
              style={{
                backgroundColor: '#1E293B',
                padding: '36px',
                borderRadius: '16px',
                width: '100%',
                maxWidth: '440px',
                border: '1px solid #334155',
                boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.6)'
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                <h2 style={{ margin: 0, color: 'white', display: 'flex', alignItems: 'center', gap: '10px', fontSize: '20px' }}>
                  <FaUserShield color="#F59E0B" /> Acceso NEXUS DAE
                </h2>
                <button onClick={() => setShowModal(false)} style={{ background: 'none', border: 'none', color: '#94A3B8', fontSize: '20px', cursor: 'pointer' }}>✖</button>
              </div>

              <p style={{ color: '#94A3B8', marginBottom: '20px', fontSize: '13px', lineHeight: '1.5' }}>
                Ingrese sus credenciales asignadas. El sistema identificará automáticamente su rol (<strong>Fiscal</strong>, <strong>Asistente</strong> o <strong>Administrador</strong>) y cargará su equipo de trabajo.
              </p>

              {authError && (
                <div style={{
                  backgroundColor: 'rgba(239, 68, 68, 0.15)',
                  border: '1px solid #EF4444',
                  color: '#FCA5A5',
                  padding: '10px 14px',
                  borderRadius: '8px',
                  fontSize: '13px',
                  marginBottom: '15px'
                }}>
                  {authError}
                </div>
              )}

              <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
                  <FaUser color="#64748B" style={{ position: 'absolute', left: '14px' }} />
                  <input 
                    type="text" 
                    placeholder="Usuario (ej. fiscal1, asistente1, admin)" 
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    style={{ width: '100%', padding: '12px 12px 12px 40px', borderRadius: '8px', border: '1px solid #334155', backgroundColor: '#0F172A', color: 'white', outline: 'none' }}
                    required
                  />
                </div>

                <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
                  <FaLock color="#64748B" style={{ position: 'absolute', left: '14px' }} />
                  <input 
                    type="password" 
                    placeholder="Contraseña" 
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    style={{ width: '100%', padding: '12px 12px 12px 40px', borderRadius: '8px', border: '1px solid #334155', backgroundColor: '#0F172A', color: 'white', outline: 'none' }}
                    required
                  />
                </div>
                
                <button 
                  type="submit" 
                  disabled={submitting}
                  style={{ 
                    marginTop: '6px', padding: '14px', backgroundColor: '#FDE047', color: 'black', 
                    fontWeight: 'bold', border: 'none', borderRadius: '8px', cursor: submitting ? 'wait' : 'pointer', transition: 'background-color 0.2s',
                    opacity: submitting ? 0.7 : 1
                  }}
                  onMouseOver={(e) => e.currentTarget.style.backgroundColor = '#FACC15'}
                  onMouseOut={(e) => e.currentTarget.style.backgroundColor = '#FDE047'}
                >
                  {submitting ? 'VERIFICANDO...' : 'INGRESAR AL SISTEMA'}
                </button>
              </form>

              <div style={{ marginTop: '20px', paddingTop: '14px', borderTop: '1px solid #334155', fontSize: '11px', color: '#64748B', display: 'flex', flexDirection: 'column', gap: '4px' }}>
                <div style={{ color: '#94A3B8', fontWeight: 'bold' }}>Accesos iniciales por defecto:</div>
                <div>• Admin: <code style={{ color: '#FDE047' }}>admin</code> / <code style={{ color: '#FDE047' }}>admin123</code></div>
                <div>• Fiscal: <code style={{ color: '#38BDF8' }}>fiscal1</code> / <code style={{ color: '#38BDF8' }}>1234</code></div>
                <div>• Asistente: <code style={{ color: '#34D399' }}>asistente1</code> / <code style={{ color: '#34D399' }}>1234</code></div>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

    </div>
  );
}
