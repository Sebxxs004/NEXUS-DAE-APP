import { useState, useEffect, useRef } from 'react';
import axios from 'axios';

// 2 horas en milisegundos: 2 * 60 * 60 * 1000 = 7,200,000 ms
export const DURACION_SIMULACION_MS = 2 * 60 * 60 * 1000;

function getCurrentUserEquipoId() {
  try {
    const raw = localStorage.getItem('nexus_current_user');
    if (!raw) return 'general';
    const user = JSON.parse(raw);
    return user?.equipo?.id || 'general';
  } catch {
    return 'general';
  }
}

function getCurrentUserName() {
  try {
    const raw = localStorage.getItem('nexus_current_user');
    if (!raw) return 'Usuario';
    const user = JSON.parse(raw);
    return user?.nombreCompleto || user?.username || 'Usuario';
  } catch {
    return 'Usuario';
  }
}

/**
 * Aplica una reducción de tiempo (penalización) compartida a nivel de equipo.
 * Se sincroniza inmediatamente en el backend para que cualquier integrante del grupo
 * (fiscal o asistente en cualquier dispositivo) lo reciba en tiempo real.
 */
export async function deductTimerMinutes(minutes, reason = 'Penalización de tiempo', byUser = null) {
  const equipoId = getCurrentUserEquipoId();
  const userName = byUser || getCurrentUserName();

  try {
    const response = await axios.post(`http://localhost:8080/api/equipos/${encodeURIComponent(equipoId)}/timer/penalty`, {
      minutes,
      reason,
      byUser: userName
    });

    const penaltyData = response.data?.lastPenalty || {
      id: `local-${Date.now()}`,
      minutes,
      reason,
      byUser: userName,
      timestamp: Date.now()
    };

    // Disparar animación interactiva de reducción de tiempo inmediatamente
    window.dispatchEvent(new CustomEvent('nexus_penalty_applied', { detail: penaltyData }));
    window.dispatchEvent(new Event('nexus_timer_updated'));
    return response.data;
  } catch (err) {
    console.warn('Backend timer penalty endpoint falló, aplicando ajuste localmente:', err);
    // Fallback local
    const startTimeStr = localStorage.getItem('nexus_timer_start');
    if (startTimeStr) {
      const startTime = parseInt(startTimeStr, 10);
      const updatedStart = startTime - minutes * 60 * 1000;
      localStorage.setItem('nexus_timer_start', updatedStart.toString());
    }

    const fallbackPenalty = {
      id: `fallback-${Date.now()}`,
      minutes,
      reason,
      byUser: userName,
      timestamp: Date.now()
    };
    window.dispatchEvent(new CustomEvent('nexus_penalty_applied', { detail: fallbackPenalty }));
    window.dispatchEvent(new Event('nexus_timer_updated'));
  }
}

export function useTimer() {
  const [timeLeft, setTimeLeft] = useState(DURACION_SIMULACION_MS);
  const [isFinished, setIsFinished] = useState(false);
  const [lastPenalty, setLastPenalty] = useState(null);
  
  const lastPenaltyIdRef = useRef(null);
  const equipoIdRef = useRef(getCurrentUserEquipoId());

  // Polling y sincronización periódica con el backend
  useEffect(() => {
    let isMounted = true;
    equipoIdRef.current = getCurrentUserEquipoId();

    const fetchServerTimer = async () => {
      try {
        const res = await axios.get(`http://localhost:8080/api/equipos/${encodeURIComponent(equipoIdRef.current)}/timer`);
        if (!isMounted) return;

        const data = res.data;
        if (typeof data.timeLeftMs === 'number') {
          setTimeLeft(data.timeLeftMs);
          if (data.timeLeftMs <= 0) {
            setIsFinished(true);
            window.dispatchEvent(new Event('nexus_timer_finished'));
          }
        }

        // Si el backend reporta una penalización reciente que no habíamos visto,
        // disparamos la animación interactiva para este usuario (ej. si el otro integrante cometió el error)
        if (data.lastPenalty && data.lastPenalty.id !== lastPenaltyIdRef.current) {
          // Si es el primer montaje, guardamos el id sin disparar para no alertar al iniciar sesión
          if (lastPenaltyIdRef.current !== null) {
            setLastPenalty(data.lastPenalty);
            window.dispatchEvent(new CustomEvent('nexus_penalty_applied', { detail: data.lastPenalty }));
          }
          lastPenaltyIdRef.current = data.lastPenalty.id;
        } else if (lastPenaltyIdRef.current === null && data.lastPenalty) {
          lastPenaltyIdRef.current = data.lastPenalty.id;
        }

      } catch {
        // Fallback local con localStorage
        const startTimeStr = localStorage.getItem('nexus_timer_start');
        if (!startTimeStr) {
          localStorage.setItem('nexus_timer_start', Date.now().toString());
        } else {
          const startTime = parseInt(startTimeStr, 10);
          const elapsed = Date.now() - startTime;
          const remaining = Math.max(0, DURACION_SIMULACION_MS - elapsed);
          setTimeLeft(remaining);
          if (remaining <= 0) {
            setIsFinished(true);
            window.dispatchEvent(new Event('nexus_timer_finished'));
          }
        }
      }
    };

    fetchServerTimer();
    // Consultar cada 2.5 segundos para sincronización en tiempo real entre miembros del equipo
    const syncInterval = setInterval(fetchServerTimer, 2500);

    // Cuenta regresiva suave local de 1 segundo entre sincronizaciones
    const tickInterval = setInterval(() => {
      setTimeLeft(prev => {
        const next = Math.max(0, prev - 1000);
        if (next === 0 && !isFinished) {
          setIsFinished(true);
          window.dispatchEvent(new Event('nexus_timer_finished'));
        }
        return next;
      });
    }, 1000);

    const handleLocalUpdated = () => {
      fetchServerTimer();
    };

    window.addEventListener('nexus_timer_updated', handleLocalUpdated);

    return () => {
      isMounted = false;
      clearInterval(syncInterval);
      clearInterval(tickInterval);
      window.removeEventListener('nexus_timer_updated', handleLocalUpdated);
    };
  }, [isFinished]);

  const formatTime = (ms) => {
    if (ms <= 0) return "00:00:00";
    const totalSeconds = Math.floor(ms / 1000);
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;
    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  };

  return { timeLeft, formatTime, isFinished, deductTimerMinutes, lastPenalty };
}
