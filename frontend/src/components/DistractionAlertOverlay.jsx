import React, { useState, useEffect, useRef } from 'react';
import { useLocation } from 'react-router-dom';
import axios from 'axios';
import { FaEnvelope, FaUser, FaSave, FaFolderOpen, FaClock, FaShareSquare } from 'react-icons/fa';
import { useGlobal } from '../context/GlobalContext';
import { deductTimerMinutes } from '../utils/useTimer';

const CHECK_INTERVAL_MS = 10 * 60 * 1000; // Cada 10 minutos
const ALERT_TIMEOUT_SECONDS = 180; // 3 minutos en pantalla para leer y decidir coherentemente

// Múltiples situaciones/diálogos para el ASISTENTE
// - tipo: 'correo' -> usa /assets/correo-asistente.jpg
// - tipo: 'presencial' -> usa /assets/user-view-asistente.jpg
// - delitoRelacionado: opcional, vincula a un tipo de delito de los casos activos
// - radicadoDirecto: opcional, si apunta a un radicado puntual
const SITUACIONES_ASISTENTE = [
  {
    id: 'ASIST-EXTORSION-1',
    tipo: 'correo',
    delitoRelacionado: 'extorsion',
    titulo: 'CORREO ENTRGP · SOLICITUD DE IMPULSO Y COPIAS',
    subtitulo: 'Bandeja Institucional · Sistema ARKIVA',
    remitente: 'Representante de Víctimas · Caso Mercafamilia S.A.',
    dialogo: '“Buen día. Solicito de manera urgente al despacho copia de las últimas actuaciones y que se me informe si ya se proyectó orden a Policía Judicial frente a las nuevas amenazas extorsivas reportadas por conductores en la región. Advierto próximo vencimiento de términos para tutela.”',
    opciones: [
      { label: 'Revisar expediente y proyectar oficio de respuesta para firma del Fiscal (−2 min)', penalty: 2 },
      { label: 'Responder directamente entregando información reservada de la investigación (−5 min)', penalty: 5 },
      { label: 'Dejar pasar el correo sin registrar constancia en el expediente (0 min)', penalty: 0 }
    ]
  },
  {
    id: 'ASIST-EXTORSION-2',
    tipo: 'presencial',
    delitoRelacionado: 'extorsion',
    titulo: 'CIUDADANO EN VENTANILLA DEL DESPACHO',
    subtitulo: 'Atención Presencial · Módulo de Asistente',
    remitente: 'Compareciente presencial en recepción',
    dialogo: '“Buenas, vengo urgente porque soy conductor de una empresa transportadora y me están exigiendo dinero unos sujetos que se hacen llamar alias ‘Camilo’ y ‘Pepo’. Traigo capturas de WhatsApp y números de teléfono, pero necesito que me reciban esto ya mismo porque temo por mi vida.”',
    opciones: [
      { label: 'Atender al ciudadano, recepcionar los elementos y rotular alerta urgente al Fiscal (−5 min)', penalty: 5 },
      { label: 'Decirle que vuelva otro día porque el despacho está ocupado (−8 min por pérdida de evidencia)', penalty: 8 },
      { label: 'Registrar datos básicos de contacto y agendar entrevista prioritaria en el despacho (0 min)', penalty: 0 }
    ]
  },
  {
    id: 'ASIST-MINERIA-1',
    tipo: 'correo',
    delitoRelacionado: 'explotacion ilicita de yacimiento minero',
    titulo: 'REQUERIMIENTO URGENTE · POLICÍA JUDICIAL',
    subtitulo: 'Bandeja Institucional · Investigador CTI',
    remitente: 'Investigador de Campo · Seccional Minería y Medio Ambiente',
    dialogo: '“Estimado Asistente: requerimos confirmar hoy mismo si las noticias criminales por minería ilegal ingresadas al despacho comparten la misma maquinaria pesada y coordenadas satelitales en zona rural, para consolidar un solo informe ejecutivo para el Fiscal Delegado.”',
    opciones: [
      { label: 'Cruzar los radicados en el sistema y enviar matriz de coincidencias al investigador (−2 min)', penalty: 2 },
      { label: 'Archivar el correo sin cruzar la información con los demás casos del despacho (0 min)', penalty: 0 }
    ]
  },
  {
    id: 'ASIST-PAE-1',
    tipo: 'presencial',
    delitoRelacionado: 'peculado',
    titulo: 'ABOGADO DEFENSOR EN EL DESPACHO',
    subtitulo: 'Atención Presencial · Solicitud de Principio de Oportunidad',
    remitente: 'Abogado Defensor y Compareciente',
    dialogo: '“Doctor(a), vengo con mi defendido quien trabajó como coordinador logístico en contratos del PAE y desea entregar información verificable sobre sobornos y facturación ficticia a cambio de que el Fiscal evalúe un principio de oportunidad.”',
    opciones: [
      { label: 'Recepcionar la solicitud formal, verificar anexos y dar traslado inmediato al Fiscal (−5 min)', penalty: 5 },
      { label: 'Rechazar de plano la solicitud en ventanilla sin consultar al Fiscal Delegado (−8 min)', penalty: 8 },
      { label: 'Radicar el memorial y programar sesión de trabajo conjunto con el Fiscal (0 min)', penalty: 0 }
    ]
  }
];

// Múltiples situaciones/diálogos para el FISCAL
// - tipo: 'correo' -> usa /assets/correo-electronico.jpeg
// - tipo: 'presencial' -> usa /assets/user-view.jpeg
const SITUACIONES_FISCAL = [
  {
    id: 'FISC-EXTORSION-1',
    tipo: 'correo',
    delitoRelacionado: 'extorsion',
    titulo: 'NUEVA SOLICITUD · DERECHO DE PETICIÓN',
    subtitulo: 'Bandeja Institucional · Plataforma ARKIVA',
    remitente: 'Jorge Alberto Rincón · Representante Mercafamilia S.A.',
    dialogo: '“Señor Fiscal: Solicitamos de manera respetuosa se informe el estado actual de la investigación por extorsión agravada, dado que empleados continúan recibiendo amenazas. Se advierte término perentorio legal de respuesta de 10 días hábiles bajo riesgo de acción de tutela.”',
    opciones: [
      { label: 'Instruir al asistente y proyectar respuesta de fondo dentro del término legal (−2 min)', penalty: 2 },
      { label: 'Ordenar de inmediato inspección judicial a fuentes de extorsión (−5 min)', penalty: 5 },
      { label: 'Postergar revisión para priorizar análisis de audiencias concentradas (0 min)', penalty: 0 }
    ]
  },
  {
    id: 'FISC-PECULADO-1',
    tipo: 'presencial',
    delitoRelacionado: 'peculado',
    titulo: 'USUARIO EN DESPACHO · SOLICITUD PRINCIPIO DE OPORTUNIDAD',
    subtitulo: 'Atención al Usuario · Comparecencia con Abogado Defensor',
    remitente: 'Carlos Andrés Ramírez Pabón (Contratista PAE) y Apoderado',
    dialogo: '“Señor Fiscal Delegado: Comparezco voluntariamente con mi defensor. Fui coordinador logístico en contratación del PAE y tengo pruebas directas de alteración de planillas, sobrecostos y los funcionarios públicos involucrados a cambio de un principio de oportunidad.”',
    opciones: [
      { label: 'Recibir personalmente al compareciente y recepcionar matriz de colaboración (−5 min)', penalty: 5 },
      { label: 'Remitir al usuario con el judicante/asistente sin verificar la urgencia probatoria (−8 min)', penalty: 8 },
      { label: 'Agendar fecha formal para audiencia de manifestación y verificación de garantías (0 min)', penalty: 0 }
    ]
  },
  {
    id: 'FISC-HURTO-1',
    tipo: 'correo',
    delitoRelacionado: 'hurto calificado',
    titulo: 'ALERTA DEL INVESTIGADOR CTI · FUENTE NO FORMAL',
    subtitulo: 'Policía Judicial · Seccional Norte de Santander',
    remitente: 'Investigador Juan Vargas P.',
    dialogo: '“Señor Fiscal: Fuente de campo reporta presencia de grupo delincuencial ejecutando hurtos y asaltos coordinados con los mismos alias y modus operandi ya indagados. Se requiere orden inmediata para interceptación de comunicaciones y actos urgentes.”',
    opciones: [
      { label: 'Emitir orden a Policía Judicial para verificación urgente y solicitud de audiencias (−3 min)', penalty: 3 },
      { label: 'Solicitar informe previo de caracterización antes de ordenar actos de investigación (−2 min)', penalty: 2 },
      { label: 'Dejar en bandeja de entrada pendiente de conexidad con otros despachos (0 min)', penalty: 0 }
    ]
  },
  {
    id: 'FISC-EXTORSION-2',
    tipo: 'presencial',
    delitoRelacionado: 'extorsion',
    titulo: 'LÍDER COMUNITARIO EN DESPACHO · DENUNCIA DE SEGURIDAD',
    subtitulo: 'Atención al Ciudadano · Recepción Despacho Fiscal',
    remitente: 'Vocero Gremial de Transportadores',
    dialogo: '“Doctor, venimos varios transportadores que no hemos radicado denuncia formal por miedo. Si no hay medidas de protección cautelares esta semana frente a las llamadas extorsivas, paralizaremos las rutas de abastecimiento en la región.”',
    opciones: [
      { label: 'Atender la emergencia, recepcionar la ampliación y activar medidas de protección urgente (−5 min)', penalty: 5 },
      { label: 'Indicarle que formule la denuncia únicamente por el canal virtual de la Fiscalía (−4 min)', penalty: 4 },
      { label: 'Coordinar mesa de trabajo urgente con Policía Nacional y Gaula para las 2:00 p.m. (0 min)', penalty: 0 }
    ]
  }
];

export default function DistractionAlertOverlay() {
  const location = useLocation();
  const { currentUser, addAlertRecord, casos } = useGlobal();

  const isAsistente = currentUser?.rol === 'ASISTENTE';
  const roleName = isAsistente ? 'ASISTENTE' : 'FISCAL';

  // Solo monitorear cuando el usuario está dentro de las vistas de simulación
  const isSimulationRoute =
    location.pathname.startsWith('/inicio') ||
    location.pathname.startsWith('/cases') ||
    location.pathname.startsWith('/board');

  const usedSituationsRef = useRef(new Set());

  // Estados de la alerta activa
  const [activeSituacion, setActiveSituacion] = useState(null);
  const [casoVinculado, setCasoVinculado] = useState(null); // Caso al que corresponde la alerta
  const [selectedOption, setSelectedOption] = useState(null);
  const [responseText, setResponseText] = useState('');
  const [alertSecondsLeft, setAlertSecondsLeft] = useState(ALERT_TIMEOUT_SECONDS);

  // Temporizador de 3 minutos para responder la alerta en pantalla
  useEffect(() => {
    if (!activeSituacion) return;

    setAlertSecondsLeft(ALERT_TIMEOUT_SECONDS);

    const timer = setInterval(() => {
      setAlertSecondsLeft((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          // Tiempo límite expirado automáticamente
          const radicado = casoVinculado?.nombre ? casoVinculado.nombre.split('_')[0] : 'Sin radicado';
          const delito = casoVinculado?.nombre && casoVinculado.nombre.includes('_')
            ? casoVinculado.nombre.split('_')[1].split('.')[0].toUpperCase()
            : (activeSituacion.delitoRelacionado ? activeSituacion.delitoRelacionado.toUpperCase() : 'GENERAL');

          deductTimerMinutes(
            3,
            `Tiempo límite de alerta superado (3 minutos sin responder: ${activeSituacion.titulo})`,
            `${currentUser?.nombreCompleto || currentUser?.username || 'Usuario'} (${roleName})`
          );

          addAlertRecord({
            image: `${activeSituacion.titulo} (${activeSituacion.tipo === 'presencial' ? 'Atención Presencial' : 'Bandeja/Monitor'})`,
            status: 'TIEMPO_EXPIRADO (3 MINUTOS SIN RESPUESTA)',
            response: 'El usuario no seleccionó ninguna opción dentro de los 3 minutos reglamentarios.',
            rol: roleName,
            casoRadicado: radicado,
            casoDelito: delito
          });

          setActiveSituacion(null);
          setCasoVinculado(null);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [activeSituacion, casoVinculado]);

  // Reiniciar pool si cambia de usuario/rol
  useEffect(() => {
    usedSituationsRef.current = new Set();
    setActiveSituacion(null);
    setCasoVinculado(null);
  }, [currentUser?.id, isAsistente]);

  // Función para vincular un caso del despacho a la situación disparada
  const resolverCasoVinculado = (situacion) => {
    if (!casos || casos.length === 0) return null;

    // 1. Si la situación tiene un radicado directo especificado
    if (situacion.radicadoDirecto) {
      const directo = casos.find(c => c.nombre.includes(situacion.radicadoDirecto));
      if (directo) return directo;
    }

    // 2. Si la situación tiene un delito relacionado, buscar casos del despacho que coincidan
    if (situacion.delitoRelacionado) {
      const coincidenciaDelito = casos.filter(c => {
        const nombreLower = c.nombre.toLowerCase();
        return nombreLower.includes(situacion.delitoRelacionado.toLowerCase());
      });
      if (coincidenciaDelito.length > 0) {
        // Tomar uno al azar de los que coinciden con ese delito
        return coincidenciaDelito[Math.floor(Math.random() * coincidenciaDelito.length)];
      }
    }

    // 3. Fallback: vincular al primer caso disponible del despacho
    return casos[0];
  };

  const launchNextAlert = (forcedType = null) => {
    if (activeSituacion !== null) return;

    const list = isAsistente ? SITUACIONES_ASISTENTE : SITUACIONES_FISCAL;
    let pool = list.filter(s => !usedSituationsRef.current.has(s.id));
    
    if (pool.length === 0) {
      usedSituationsRef.current = new Set();
      pool = [...list];
    }

    if (forcedType === 'visitor') {
      const presenciales = pool.filter(s => s.tipo === 'presencial');
      if (presenciales.length > 0) pool = presenciales;
    } else if (forcedType === 'email') {
      const correos = pool.filter(s => s.tipo === 'correo');
      if (correos.length > 0) pool = correos;
    }

    const chosen = pool[Math.floor(Math.random() * pool.length)];
    usedSituationsRef.current.add(chosen.id);

    const casoAsociado = resolverCasoVinculado(chosen);
    setCasoVinculado(casoAsociado);

    setActiveSituacion(chosen);
    setSelectedOption(null);
    setResponseText('');
  };

  // Ciclo automático cada 10 minutos
  useEffect(() => {
    if (!isSimulationRoute) return;

    const interval = setInterval(() => {
      launchNextAlert();
    }, CHECK_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [isSimulationRoute, isAsistente, activeSituacion]);

  // Atajos de teclado para probar de inmediato:
  // Alt+A: Interrupción de monitor/correo
  // Alt+U: Interrupción presencial
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (!isSimulationRoute) return;
      if (e.altKey && (e.key === 'a' || e.key === 'A')) {
        e.preventDefault();
        launchNextAlert('email');
      } else if (e.altKey && (e.key === 'u' || e.key === 'U')) {
        e.preventDefault();
        launchNextAlert('visitor');
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isSimulationRoute, isAsistente, activeSituacion]);

  if (!isSimulationRoute || !activeSituacion) return null;

  // Confirmar decisión de situación
  const handleConfirmDecision = () => {
    if (selectedOption === null) {
      alert('Por favor selecciona una decisión ante esta interrupción.');
      return;
    }
    if (!responseText.trim()) {
      alert('Por favor escribe una breve justificación o respuesta de tu decisión para el informe.');
      return;
    }

    const opt = activeSituacion.opciones[selectedOption];
    if (opt.penalty > 0) {
      deductTimerMinutes(
        opt.penalty,
        `Decisión tomada: ${opt.label}`,
        `${currentUser?.nombreCompleto || currentUser?.username || 'Usuario'} (${roleName})`
      );
    }

    // Extraer datos del caso vinculado
    const radicado = casoVinculado?.nombre ? casoVinculado.nombre.split('_')[0] : 'Sin radicado';
    const delito = casoVinculado?.nombre && casoVinculado.nombre.includes('_')
      ? casoVinculado.nombre.split('_')[1].split('.')[0].toUpperCase()
      : (activeSituacion.delitoRelacionado ? activeSituacion.delitoRelacionado.toUpperCase() : 'GENERAL');

    addAlertRecord({
      image: `${activeSituacion.titulo} (${activeSituacion.tipo === 'presencial' ? 'Atención Presencial' : 'Bandeja/Monitor'})`,
      status: opt.label,
      response: responseText.trim(),
      rol: roleName,
      casoRadicado: radicado,
      casoDelito: delito
    });

    setActiveSituacion(null);
    setCasoVinculado(null);
  };

  // Trasladar y delegar la atención de la alerta al Asistente de Fiscal
  const handleDelegarAlAsistente = async () => {
    const radicado = casoVinculado?.nombre ? casoVinculado.nombre.split('_')[0] : 'Sin radicado';
    const delito = casoVinculado?.nombre && casoVinculado.nombre.includes('_')
      ? casoVinculado.nombre.split('_')[1].split('.')[0].toUpperCase()
      : (activeSituacion.delitoRelacionado ? activeSituacion.delitoRelacionado.toUpperCase() : 'GENERAL');

    const equipoId = currentUser?.equipo?.id || 'general';

    try {
      await axios.post(`http://localhost:8080/api/equipos/${encodeURIComponent(equipoId)}/alertas/delegar`, {
        id: `DELEG-${activeSituacion.id}-${Date.now()}`,
        titulo: activeSituacion.titulo,
        subtitulo: activeSituacion.subtitulo,
        dialogo: activeSituacion.dialogo,
        delito: delito,
        radicado: radicado,
        casoNombre: casoVinculado?.nombre || null,
        delegadoPor: currentUser?.nombreCompleto || currentUser?.username || 'Fiscal Delegado',
        opciones: [
          'Ignorar o descartar esta solicitud / actividad',
          ...activeSituacion.opciones.map(o => typeof o === 'string' ? o : o.label)
        ]
      });
    } catch (err) {
      console.warn('Error al enviar delegación al backend:', err);
    }

    addAlertRecord({
      image: `${activeSituacion.titulo} (${activeSituacion.tipo === 'presencial' ? 'Atención Presencial' : 'Bandeja/Monitor'})`,
      status: 'DELEGADA AL ASISTENTE',
      response: responseText.trim()
        ? `Fiscal delegó trámite al Asistente con instrucción: "${responseText.trim()}"`
        : 'El Fiscal Delegado trasladó la atención de esta interrupción al Asistente de Despacho.',
      rol: roleName,
      casoRadicado: radicado,
      casoDelito: delito
    });

    alert('✓ La actuación ha sido trasladada a la bandeja del Asistente.');
    setActiveSituacion(null);
    setCasoVinculado(null);
  };

  const isPresencial = activeSituacion.tipo === 'presencial';
  
  // Fondos correspondientes por rol y tipo
  const bgImage = isAsistente
    ? (isPresencial ? "url('/assets/user-view-asistente.jpg'), url('/assets/user-view.jpeg')" : "url('/assets/correo-asistente.jpg'), url('/assets/correo-electronico.jpeg')")
    : (isPresencial ? "url('/assets/user-view.jpeg')" : "url('/assets/correo-electronico.jpeg')");

  const accentColor = isAsistente ? '#34D399' : '#F59E0B';
  const accentLight = isAsistente ? '#6EE7B7' : '#FDE047';
  const badgeBg = isAsistente ? 'rgba(52, 211, 153, 0.22)' : 'rgba(245, 158, 11, 0.22)';
  const borderCard = isAsistente ? '#10B981' : '#F59E0B';

  const radicadoDisplay = casoVinculado?.nombre ? casoVinculado.nombre.split('_')[0] : 'NUNC en trámite';
  const delitoDisplay = casoVinculado?.nombre && casoVinculado.nombre.includes('_')
    ? casoVinculado.nombre.split('_')[1].split('.')[0].toUpperCase()
    : (activeSituacion.delitoRelacionado ? activeSituacion.delitoRelacionado.toUpperCase() : '');

  return (
    <div style={{
      position: 'fixed',
      top: 0, left: 0, width: '100vw', height: '100vh',
      backgroundImage: `linear-gradient(rgba(6, 14, 26, 0.65), rgba(6, 14, 26, 0.82)), ${bgImage}`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
      zIndex: 99999,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      fontFamily: 'sans-serif',
      color: 'white',
      padding: '20px'
    }}>
      <div style={{
        backgroundColor: 'rgba(10, 20, 36, 0.94)',
        border: `1.5px solid ${borderCard}`,
        borderRadius: '18px',
        width: '100%',
        maxWidth: '720px',
        overflow: 'hidden',
        boxShadow: '0 25px 50px rgba(0,0,0,0.85)',
        backdropFilter: 'blur(10px)'
      }}>
        {/* Header */}
        <div style={{
          display: 'flex', alignItems: 'center', gap: '12px',
          padding: '16px 24px',
          backgroundColor: badgeBg,
          borderBottom: `1px solid ${borderCard}44`
        }}>
          <div style={{ backgroundColor: badgeBg, padding: '10px', borderRadius: '10px', color: accentLight, display: 'flex' }}>
            {isPresencial ? <FaUser size={20} /> : <FaEnvelope size={20} />}
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ fontWeight: 'bold', fontSize: '16px', color: '#FFFFFF' }}>{activeSituacion.titulo}</div>
            <div style={{ fontSize: '12px', color: '#CBD5E1' }}>{activeSituacion.subtitulo} · {activeSituacion.remitente}</div>
          </div>
          <div style={{ fontSize: '11px', fontWeight: 'bold', color: accentLight, border: `1px solid ${borderCard}`, padding: '4px 8px', borderRadius: '6px' }}>
            {roleName}
          </div>
        </div>

        {/* Body */}
        <div style={{ padding: '22px 26px', display: 'flex', flexDirection: 'column', gap: '15px' }}>
          
          {/* Temporizador de permanencia de la alerta (3 minutos) */}
          <div style={{
            backgroundColor: alertSecondsLeft <= 30 ? 'rgba(239, 68, 68, 0.25)' : 'rgba(15, 23, 42, 0.85)',
            border: `1px solid ${alertSecondsLeft <= 30 ? '#EF4444' : 'rgba(255,255,255,0.15)'}`,
            borderRadius: '10px',
            padding: '10px 14px',
            display: 'flex',
            flexDirection: 'column',
            gap: '6px'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '12px' }}>
              <span style={{
                color: alertSecondsLeft <= 30 ? '#FCA5A5' : '#CBD5E1',
                fontWeight: 'bold',
                display: 'flex',
                alignItems: 'center',
                gap: '6px'
              }}>
                <FaClock color={alertSecondsLeft <= 30 ? '#EF4444' : accentLight} />
                TIEMPO PARA LEER Y DECIDIR ANTE ESTA ALERTA:
              </span>
              <span style={{
                fontSize: '14px',
                fontWeight: '900',
                fontFamily: 'monospace',
                color: alertSecondsLeft <= 30 ? '#EF4444' : '#FDE047',
                letterSpacing: '1px'
              }}>
                {Math.floor(alertSecondsLeft / 60).toString().padStart(2, '0')}:{(alertSecondsLeft % 60).toString().padStart(2, '0')} / 03:00
              </span>
            </div>

            {/* Barra de progreso regresiva de 3 minutos */}
            <div style={{
              width: '100%',
              height: '6px',
              backgroundColor: 'rgba(255,255,255,0.1)',
              borderRadius: '3px',
              overflow: 'hidden'
            }}>
              <div style={{
                width: `${(alertSecondsLeft / ALERT_TIMEOUT_SECONDS) * 100}%`,
                height: '100%',
                backgroundColor: alertSecondsLeft <= 30 ? '#EF4444' : (isAsistente ? '#34D399' : '#F59E0B'),
                transition: 'width 1s linear, background-color 0.3s ease'
              }} />
            </div>
          </div>

          {/* Badge de Caso / Noticia Criminal Vinculada */}
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            backgroundColor: 'rgba(30, 58, 138, 0.45)',
            border: '1px solid rgba(56, 189, 248, 0.45)',
            padding: '8px 14px',
            borderRadius: '8px',
            fontSize: '12px',
            color: '#E0F2FE'
          }}>
            <FaFolderOpen color="#38BDF8" size={16} />
            <div>
              <strong>CASO ASOCIADO A ESTA ALERTA:</strong> Radicado NUNC <span style={{ color: '#FDE047', fontWeight: 'bold' }}>{radicadoDisplay}</span>
              {delitoDisplay ? ` · Delito: ${delitoDisplay}` : ''}
            </div>
          </div>

          <div style={{
            backgroundColor: 'rgba(15, 23, 42, 0.88)',
            borderLeft: `4px solid ${accentColor}`,
            padding: '14px 18px',
            borderRadius: '8px',
            fontSize: '14px',
            lineHeight: '1.55',
            color: '#F1F5F9'
          }}>
            {activeSituacion.dialogo}
          </div>

          <div>
            <div style={{ fontSize: '13px', fontWeight: 'bold', color: accentLight, marginBottom: '8px' }}>
              ¿Qué decisión tomas ante esta situación?
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              {activeSituacion.opciones.map((op, idx) => {
                const isSelected = selectedOption === idx;
                return (
                  <label
                    key={idx}
                    onClick={() => setSelectedOption(idx)}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '10px',
                      padding: '11px 14px',
                      borderRadius: '10px',
                      cursor: 'pointer',
                      backgroundColor: isSelected ? badgeBg : 'rgba(255,255,255,0.05)',
                      border: isSelected ? `1.5px solid ${accentColor}` : '1px solid rgba(255,255,255,0.15)',
                      fontSize: '13px',
                      fontWeight: isSelected ? 'bold' : 'normal'
                    }}
                  >
                    <input
                      type="radio"
                      name="interrupcion_opt"
                      checked={isSelected}
                      onChange={() => setSelectedOption(idx)}
                      style={{ accentColor: accentColor }}
                    />
                    <span>{op.label}</span>
                  </label>
                );
              })}
            </div>
          </div>

          <div>
            <div style={{ fontSize: '12px', fontWeight: 'bold', color: '#94A3B8', marginBottom: '6px' }}>
              JUSTIFICACIÓN / ACTUACIÓN (Quedará registrada en el informe PDF de evaluación):
            </div>
            <textarea
              placeholder="Explica brevemente cómo gestionarás esta situación y por qué tomaste esta decisión..."
              value={responseText}
              onChange={(e) => setResponseText(e.target.value)}
              rows={3}
              style={{
                width: '100%',
                padding: '10px 12px',
                borderRadius: '8px',
                backgroundColor: '#0F172A',
                color: 'white',
                border: '1px solid #334155',
                fontSize: '13px',
                resize: 'none',
                outline: 'none'
              }}
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '6px', gap: '10px' }}>
            {!isAsistente && (
              <button
                onClick={handleDelegarAlAsistente}
                style={{
                  backgroundColor: 'rgba(56, 189, 248, 0.15)',
                  border: '1.5px solid #38BDF8',
                  color: '#7DD3FC',
                  borderRadius: '10px',
                  padding: '11px 18px',
                  fontWeight: 'bold',
                  fontSize: '13px',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  transition: 'background-color 0.2s'
                }}
                onMouseOver={(e) => e.currentTarget.style.backgroundColor = 'rgba(56, 189, 248, 0.3)'}
                onMouseOut={(e) => e.currentTarget.style.backgroundColor = 'rgba(56, 189, 248, 0.15)'}
              >
                <FaShareSquare /> Trasladar y Delegar al Asistente
              </button>
            )}

            <button
              onClick={handleConfirmDecision}
              style={{
                backgroundColor: accentColor,
                color: isAsistente ? '#064E3B' : '#000000',
                border: 'none',
                borderRadius: '10px',
                padding: '12px 24px',
                fontWeight: 'bold',
                fontSize: '14px',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                marginLeft: isAsistente ? 'auto' : '0'
              }}
            >
              <FaSave /> Confirmar Decisión y Continuar
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
