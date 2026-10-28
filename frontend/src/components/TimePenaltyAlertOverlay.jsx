import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { FaClock, FaTimes } from 'react-icons/fa';

export default function TimePenaltyAlertOverlay() {
  const [currentPenalty, setCurrentPenalty] = useState(null);

  useEffect(() => {
    const handlePenalty = (e) => {
      const detail = e.detail;
      if (!detail || !detail.minutes) return;

      setCurrentPenalty(detail);

      // Efecto suave de pulso en el reloj
      try {
        const clocks = document.querySelectorAll('.timer-clock-badge');
        clocks.forEach(clock => {
          clock.classList.add('penalty-shake-active');
          setTimeout(() => clock.classList.remove('penalty-shake-active'), 700);
        });
      } catch (err) {
        console.error(err);
      }

      // Animación corta y no invasiva: 2.2 segundos
      const timer = setTimeout(() => {
        setCurrentPenalty(null);
      }, 2200);

      return () => clearTimeout(timer);
    };

    window.addEventListener('nexus_penalty_applied', handlePenalty);
    return () => window.removeEventListener('nexus_penalty_applied', handlePenalty);
  }, []);

  if (!currentPenalty) return null;

  // Limpiar el texto de la decisión si viene con prefijo
  const decisionText = currentPenalty.reason
    ? currentPenalty.reason
        .replace(/^Decisión tomada:\s*/i, '')
        .replace(/^Decisión en interrupción:\s*/i, '')
    : 'Ajuste de tiempo por decisión';

  return (
    <AnimatePresence>
      <div
        style={{
          position: 'fixed',
          top: '18px',
          right: '20px',
          zIndex: 9999999,
          pointerEvents: 'none',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'flex-end',
          fontFamily: 'sans-serif'
        }}
      >
        {/* 1. Indicador flotante efímero sobre el reloj: −X min subiendo y desvaneciéndose */}
        <motion.div
          initial={{ opacity: 1, y: 0, scale: 1 }}
          animate={{ opacity: 0, y: -26, scale: 1.15 }}
          transition={{ duration: 1.5, ease: 'easeOut' }}
          style={{
            position: 'absolute',
            top: '-2px',
            right: '150px',
            color: '#FBBF24',
            fontWeight: '900',
            fontSize: '18px',
            textShadow: '0 2px 6px rgba(0,0,0,0.8), 0 0 10px rgba(251, 191, 36, 0.5)',
            pointerEvents: 'none'
          }}
        >
          −{currentPenalty.minutes} min
        </motion.div>

        {/* 2. Tarjeta sutil y elegante justo debajo del reloj */}
        <motion.div
          initial={{ opacity: 0, y: -10, scale: 0.95 }}
          animate={{ opacity: 1, y: 44, scale: 1 }}
          exit={{ opacity: 0, y: -8, scale: 0.95 }}
          transition={{ duration: 0.2 }}
          style={{
            pointerEvents: 'auto',
            backgroundColor: 'rgba(15, 23, 42, 0.95)',
            border: '1px solid rgba(251, 191, 36, 0.45)',
            borderRadius: '12px',
            padding: '10px 14px',
            width: '320px',
            maxWidth: '85vw',
            boxShadow: '0 8px 24px rgba(0,0,0,0.5), 0 0 12px rgba(251, 191, 36, 0.15)',
            backdropFilter: 'blur(10px)',
            color: 'white',
            display: 'flex',
            flexDirection: 'column',
            gap: '4px'
          }}
        >
          {/* Fila superior: Cantidad de minutos y botón de cierre rápido */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <FaClock color="#FBBF24" size={14} />
              <span style={{
                color: '#FBBF24',
                fontWeight: 'bold',
                fontSize: '14px',
                letterSpacing: '0.3px'
              }}>
                −{currentPenalty.minutes} minutos
              </span>
            </div>

            <button
              onClick={() => setCurrentPenalty(null)}
              style={{
                background: 'none',
                border: 'none',
                color: '#94A3B8',
                cursor: 'pointer',
                padding: '2px',
                fontSize: '12px'
              }}
            >
              <FaTimes />
            </button>
          </div>

          {/* Fila inferior: Decisión tomada */}
          <div style={{
            fontSize: '12px',
            color: '#E2E8F0',
            lineHeight: '1.4'
          }}>
            <strong style={{ color: '#94A3B8' }}>Decisión tomada:</strong> {decisionText}
          </div>

          {currentPenalty.byUser && (
            <div style={{ fontSize: '10px', color: '#64748B', marginTop: '2px' }}>
              Registrado por: {currentPenalty.byUser}
            </div>
          )}
        </motion.div>
      </div>
    </AnimatePresence>
  );
}
