import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { FaArrowLeft, FaFolderOpen, FaChartBar, FaClock, FaBriefcase, FaUserGraduate, FaUserTie } from 'react-icons/fa';
import { useTimer } from '../utils/useTimer';
import { useGlobal } from '../context/GlobalContext';

export default function Inicio() {
  const navigate = useNavigate();
  const [isPlayingTransition, setIsPlayingTransition] = useState(false);
  const videoRef = useRef(null);
  
  const { timeLeft, formatTime } = useTimer();
  const { currentUser, logoutUser, casos } = useGlobal();

  const equipo = currentUser?.equipo;
  const integrantes = equipo?.integrantes || [];
  const totalFiscales = integrantes.filter(i => i.rol === 'FISCAL').length;
  const totalAsistentes = integrantes.filter(i => i.rol === 'ASISTENTE').length;

  const handleGestionesClick = () => {
    if (videoRef.current) {
      videoRef.current.currentTime = 0;
      videoRef.current.play().then(() => {
        setIsPlayingTransition(true);
      }).catch(() => {
        setIsPlayingTransition(true);
      });
    } else {
      setIsPlayingTransition(true);
    }
  };

  const handleVideoEnd = () => {
    setIsPlayingTransition(false);
    navigate('/cases');
  };

  const handleBackToLogin = () => {
    logoutUser();
    navigate('/');
  };

  return (
    <div className="inicio-bg" style={{ position: 'relative' }}>
      
      {/* Video Transition Overlay (Pre-renderizado para evitar pantalla negra) */}
      <div
        style={{
          position: 'fixed',
          top: 0, left: 0, width: '100vw', height: '100vh',
          backgroundColor: 'transparent',
          zIndex: 9999,
          display: isPlayingTransition ? 'flex' : 'none',
          alignItems: 'center',
          justifyContent: 'center',
          pointerEvents: isPlayingTransition ? 'auto' : 'none'
        }}
      >
        <video 
          ref={videoRef}
          src="/videos/VIDEO2.mp4" 
          preload="auto"
          onEnded={handleVideoEnd}
          style={{ width: '100%', height: '100%', objectFit: 'cover' }}
        />
      </div>

      {/* Top Left: Volver al login */}
      <button 
        onClick={handleBackToLogin}
        style={{
          position: 'absolute',
          top: '20px',
          left: '20px',
          backgroundColor: 'rgba(0,0,0,0.6)',
          color: 'white',
          border: '1px solid rgba(255,255,255,0.2)',
          padding: '8px 15px',
          borderRadius: '8px',
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          cursor: 'pointer',
          backdropFilter: 'blur(5px)',
          fontWeight: 'bold',
          zIndex: 10
        }}
      >
        <FaArrowLeft /> Volver al login
      </button>

      {/* Top Right: Tiempo */}
      <div className="timer-clock-badge" style={{
        position: 'absolute',
        top: '20px',
        right: '20px',
        backgroundColor: '#EF4444',
        color: 'white',
        padding: '8px 15px',
        borderRadius: '20px',
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        fontWeight: 'bold',
        fontSize: '14px',
        boxShadow: '0 4px 6px rgba(0,0,0,0.3)',
        zIndex: 10
      }}>
        <div style={{ width: '8px', height: '8px', backgroundColor: 'white', borderRadius: '50%' }}></div>
        TIEMPO {formatTime(timeLeft)}
      </div>

      {/* Top Center: Progress Bar */}
      <div style={{
        position: 'absolute',
        top: '25px',
        left: '50%',
        transform: 'translateX(-50%)',
        width: '200px',
        height: '10px',
        backgroundColor: 'rgba(255,255,255,0.3)',
        border: '1px solid white',
        borderRadius: '5px',
        overflow: 'hidden',
        zIndex: 10
      }}>
        <div style={{ 
          width: `${(timeLeft / (2 * 60 * 60 * 1000)) * 100}%`, 
          height: '100%', 
          backgroundColor: '#EF4444',
          transition: 'width 1s linear'
        }}></div>
      </div>

      {/* Center Content */}
      <div style={{
        position: 'absolute',
        top: '32%',
        left: '50%',
        transform: 'translate(-50%, -50%)',
        textAlign: 'center',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        width: '100%',
        zIndex: 10
      }}>
        <div style={{
          backgroundColor: 'rgba(56, 189, 248, 0.2)',
          border: '1px solid #38BDF8',
          color: '#7DD3FC',
          padding: '4px 14px',
          borderRadius: '20px',
          fontSize: '12px',
          fontWeight: 'bold',
          marginBottom: '10px',
          display: 'inline-flex',
          alignItems: 'center',
          gap: '8px',
          backdropFilter: 'blur(5px)'
        }}>
          <FaUserTie /> ROL ACTIVO: {currentUser?.rol || 'FISCAL'}
          {currentUser?.nombreCompleto ? ` · ${currentUser.nombreCompleto}` : ''}
        </div>

        <h1 style={{ 
          color: 'white', 
          fontSize: '2.5rem', 
          fontWeight: 'bold',
          textShadow: '0 2px 10px rgba(0,0,0,0.8)',
          marginBottom: '16px',
          marginTop: 0
        }}>
          Bienvenido a tu Despacho
        </h1>
        
        <img src="/assets/PRISMA-DAE.png" alt="Brain Logo" style={{ width: '80px', filter: 'drop-shadow(0 0 10px rgba(255,255,255,0.8))' }} />
        
        <div style={{ color: 'white', marginTop: '10px', textShadow: '0 2px 4px rgba(0,0,0,0.8)' }}>
          <div style={{ fontWeight: 'bold', fontSize: '1.2rem', letterSpacing: '2px' }}>NEXUS</div>
          <div style={{ fontSize: '0.7rem', opacity: 0.8, letterSpacing: '1px' }}>DIRECCIÓN DE ALTOS ESTUDIOS</div>
        </div>

        {/* 3 Pills */}
        <div style={{ display: 'flex', gap: '15px', marginTop: '20px', flexWrap: 'wrap', justifyContent: 'center' }}>
          <div style={{ backgroundColor: 'rgba(255,255,255,0.1)', border: '1px solid rgba(255,255,255,0.2)', padding: '5px 15px', borderRadius: '20px', display: 'flex', alignItems: 'center', gap: '5px', color: 'white', fontSize: '0.85rem', backdropFilter: 'blur(5px)' }}>
            <FaClock color="#FBBF24" /> Jornada: 2 horas
          </div>
          <div style={{ backgroundColor: 'rgba(255,255,255,0.1)', border: '1px solid rgba(255,255,255,0.2)', padding: '5px 15px', borderRadius: '20px', display: 'flex', alignItems: 'center', gap: '5px', color: 'white', fontSize: '0.85rem', backdropFilter: 'blur(5px)' }}>
            <FaBriefcase color="#FBBF24" /> Casos activos: {casos.length || 65}
          </div>
          <div style={{ backgroundColor: 'rgba(255,255,255,0.1)', border: '1px solid rgba(255,255,255,0.2)', padding: '5px 15px', borderRadius: '20px', display: 'flex', alignItems: 'center', gap: '5px', color: 'white', fontSize: '0.85rem', backdropFilter: 'blur(5px)' }}>
            <FaUserGraduate color="#FBBF24" />{' '}
            {equipo
              ? `${equipo.nombre} (${totalFiscales}F / ${totalAsistentes}A)`
              : 'Equipo: Sin asignar'}
          </div>
        </div>
      </div>

      {/* Bottom Buttons */}
      <motion.div 
        initial={{ opacity: 0, y: 50 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.8 }}
        style={{
          position: 'absolute',
          bottom: '40px',
          width: '100%',
          display: 'flex',
          justifyContent: 'space-between',
          padding: '0 40px',
          zIndex: 10
        }}
      >
        {/* Left Button */}
        <button 
          onClick={handleGestionesClick}
          style={{
            backgroundColor: '#FDE047',
            color: 'black',
            padding: '12px 20px',
            border: 'none',
            borderRadius: '8px',
            boxShadow: '0 4px 15px rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            gap: '15px',
            cursor: 'pointer',
            textAlign: 'left',
            minWidth: '250px',
            transition: 'transform 0.2s'
          }}
          onMouseOver={(e) => e.currentTarget.style.transform = 'scale(1.02)'}
          onMouseOut={(e) => e.currentTarget.style.transform = 'scale(1)'}
        >
          <FaFolderOpen size={24} />
          <div>
            <div style={{ fontWeight: 'bold', fontSize: '14px' }}>Procesos del despacho</div>
            <div style={{ fontSize: '10px', opacity: 0.7 }}>Noticias criminales y expedientes</div>
          </div>
        </button>

        {/* Right Button */}
        <button 
          onClick={() => navigate('/board/demo')}
          style={{
            backgroundColor: '#FDE047',
            color: 'black',
            padding: '12px 20px',
            border: 'none',
            borderRadius: '8px',
            boxShadow: '0 4px 15px rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            gap: '15px',
            cursor: 'pointer',
            textAlign: 'left',
            minWidth: '250px',
            transition: 'transform 0.2s'
          }}
          onMouseOver={(e) => e.currentTarget.style.transform = 'scale(1.02)'}
          onMouseOut={(e) => e.currentTarget.style.transform = 'scale(1)'}
        >
          <div>
            <div style={{ fontWeight: 'bold', fontSize: '14px' }}>Toma de decisiones</div>
            <div style={{ fontSize: '10px', opacity: 0.7 }}>Patrones y conexiones</div>
          </div>
          <FaChartBar size={24} />
        </button>
      </motion.div>
    </div>
  );
}
