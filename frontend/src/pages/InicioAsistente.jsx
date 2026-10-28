import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { motion, AnimatePresence } from 'framer-motion';
import {
  FaArrowLeft,
  FaClock,
  FaUsers,
  FaBriefcase,
  FaUserCheck,
  FaFolderOpen,
  FaTasks,
  FaSearch,
  FaEye,
  FaCheckCircle,
  FaExclamationCircle,
  FaChevronRight,
  FaSave,
  FaTimes,
  FaBan,
  FaFileDownload,
  FaFileAlt,
  FaInfoCircle,
  FaEdit,
  FaCheck,
  FaShareSquare,
  FaBell,
  FaUserTie
} from 'react-icons/fa';
import { useTimer } from '../utils/useTimer';
import { useGlobal } from '../context/GlobalContext';
import { generatePDFReport } from '../utils/PDFGenerator';

// Listado inicial de actividades operativas para el Asistente de Fiscal
// Cada actividad cuenta con su correspondencia a noticia criminal/expediente (o general),
// y sus opciones de actuación que incluyen la opción de "Ignorar".
const ACTIVIDADES_ASISTENTE_INICIALES = [
  {
    id: 'ACT-01',
    titulo: 'Revisar folios y proyectar solicitud a entidades bancarias',
    casoNombre: '110016000099202325078_estafa.png',
    radicado: '110016000099202325078',
    delito: 'ESTAFA',
    urgencia: 'ALTA',
    descripcion: 'Se radicó denuncia ciudadana con anexos digitales por transferencias bancarias no consentidas. Se requiere verificar cadena de custodia de comprobantes de pago y proyectar oficio de solicitud de extractos financieros a los bancos receptores para firma del Fiscal.',
    opciones: [
      'Ignorar o descartar esta solicitud / actividad',
      'Proyectar oficio a entidades financieras y presentar minuta al Fiscal',
      'Solicitar ampliación previa de declaración jurada a la víctima antes del requerimiento',
      'Remitir por competencia a la Seccional de Delitos Informáticos'
    ],
    estado: 'PENDIENTE',
    opcionSeleccionada: '',
    justificacion: ''
  },
  {
    id: 'ACT-02',
    titulo: 'Proyectar orden de inspección a Policía Judicial (Cámaras)',
    casoNombre: '050016000206202100084_hurto calificado.png',
    radicado: '050016000206202100084',
    delito: 'HURTO CALIFICADO',
    urgencia: 'ALTA',
    descripcion: 'La víctima allegó registros fílmicos de cámaras de seguridad del establecimiento comercial donde ocurrió el hurto con intimidación con arma de fuego. Se requiere proyectar la orden de trabajo para Policía Judicial para cotejo morfológico y cadena de custodia del medio magnético.',
    opciones: [
      'Ignorar o descartar esta solicitud / actividad',
      'Proyectar formato FPJ-04 de inspección técnica a lugares y análisis de video',
      'Ordenar citación urgente a los testigos presenciales que aparecen en la grabación',
      'Archivar preventivamente la solicitud por falta de individualización del sospechoso'
    ],
    estado: 'PENDIENTE',
    opcionSeleccionada: '',
    justificacion: ''
  },
  {
    id: 'ACT-03',
    titulo: 'Consolidar matriz de testigos y números telefónicos',
    casoNombre: '110016000099202101523_explotacion ilicita de yacimiento minero.png',
    radicado: '110016000099202101523',
    delito: 'EXPLOTACIÓN ILÍCITA DE YACIMIENTO MINERO',
    urgencia: 'MEDIA',
    descripcion: 'Organizar los datos de contacto, ubicaciones de campo y versiones preliminares recepcionadas en el informe de campo para alistar la matriz de conexidad y remitirla al Fiscal Delegado.',
    opciones: [
      'Ignorar o descartar esta solicitud / actividad',
      'Consolidar la matriz técnica de testigos y remitir de inmediato al Fiscal',
      'Solicitar complementación de georreferenciación de coordenadas al perito ambiental',
      'Convocar mesa de trabajo interinstitucional con la autoridad ambiental (CAR)'
    ],
    estado: 'PENDIENTE',
    opcionSeleccionada: '',
    justificacion: ''
  },
  {
    id: 'ACT-04',
    titulo: 'Solicitud externa de copias del proceso por apoderado judicial',
    casoNombre: null,
    radicado: 'General / Sin expediente individual asignado',
    delito: 'Atención al Ciudadano y Despacho',
    urgencia: 'NORMAL',
    descripcion: 'Un abogado defensor se presenta en ventanilla y solicita copia simple de las últimas actuaciones del despacho sin haber radicado aún el poder especial debidamente conferido ni exhibir tarjeta profesional vigente.',
    opciones: [
      'Ignorar o descartar esta solicitud / actividad',
      'Exigir presentación y reconocimiento previo del poder formal antes de permitir acceso a las piezas procesales',
      'Brindar informe verbal sobre el estado general del trámite sin expedir copias documentales',
      'Trasladar el requerimiento directamente al Fiscal Delegado para que adopte la decisión'
    ],
    estado: 'PENDIENTE',
    opcionSeleccionada: '',
    justificacion: ''
  },
  {
    id: 'ACT-05',
    titulo: 'Verificación de vencimiento de términos y control de archivo',
    casoNombre: null,
    radicado: 'General / Auditoría de Despacho',
    delito: 'Control y Gestión Procesal SPOA',
    urgencia: 'MEDIA',
    descripcion: 'Alerta preventiva del sistema de gestión: revisión del inventario del despacho para identificar indagaciones preliminares que estén próximas a cumplir el término legal sin formulación de imputación o solicitud de preclusión.',
    opciones: [
      'Ignorar o descartar esta solicitud / actividad',
      'Generar reporte consolidado de términos y entregarlo al Fiscal con fichas técnicas priorizadas',
      'Proyectar solicitud formal de prórroga para investigaciones con pluralidad de indiciados',
      'Priorizar exclusivamente los expedientes que involucren medidas cautelares activas'
    ],
    estado: 'PENDIENTE',
    opcionSeleccionada: '',
    justificacion: ''
  }
];

export default function InicioAsistente() {
  const navigate = useNavigate();
  const { timeLeft, formatTime } = useTimer();
  const { currentUser, logoutUser, casos, groups, alertRecords, addAlertRecord } = useGlobal();

  const equipo = currentUser?.equipo;
  const integrantes = equipo?.integrantes || [];
  const totalFiscales = integrantes.filter(i => i.rol === 'FISCAL').length;
  const totalAsistentes = integrantes.filter(i => i.rol === 'ASISTENTE').length;

  // Estado del modal de "Bandeja de Trabajo"
  const [showBandejaModal, setShowBandejaModal] = useState(false);

  // Lista de actividades
  const [actividades, setActividades] = useState(ACTIVIDADES_ASISTENTE_INICIALES);

  // Notificación flotante cuando el Fiscal traslada una nueva actuación en tiempo real
  const [notificacionDelegada, setNotificacionDelegada] = useState(null);
  const seenDelegatedIdsRef = useRef(new Set());

  // Modal interactivo para gestionar una actividad específica
  const [actividadModal, setActividadModal] = useState(null);
  const [opcionTemporal, setOpcionTemporal] = useState('');
  const [justificacionTemporal, setJustificacionTemporal] = useState('');
  const [mensajeExito, setMensajeExito] = useState('');

  // Vista de casos en la columna izquierda
  const [filtroCaso, setFiltroCaso] = useState('');
  const [previewCaso, setPreviewCaso] = useState(null);

  const equipoId = currentUser?.equipo?.id || 'general';

  // Sincronización en tiempo real de alertas trasladadas / delegadas por el Fiscal
  useEffect(() => {
    let isMounted = true;

    const fetchDelegatedAlerts = async () => {
      try {
        const res = await axios.get(`http://localhost:8080/api/equipos/${encodeURIComponent(equipoId)}/alertas/delegadas`);
        if (!isMounted || !Array.isArray(res.data)) return;

        const serverAlerts = res.data;
        if (serverAlerts.length === 0) return;

        setActividades(prev => {
          const currentMap = new Map(prev.map(a => [a.id, a]));
          let updatedList = [...prev];

          serverAlerts.forEach(srvAlert => {
            if (!currentMap.has(srvAlert.id)) {
              // Nueva alerta delegada por el Fiscal: se agrega a las actividades del Asistente
              const nueva = {
                id: srvAlert.id,
                titulo: srvAlert.titulo,
                casoNombre: srvAlert.casoNombre,
                radicado: srvAlert.radicado,
                delito: srvAlert.delito,
                urgencia: 'ALTA',
                descripcion: srvAlert.dialogo || srvAlert.subtitulo,
                opciones: srvAlert.opciones && srvAlert.opciones.length > 0 ? srvAlert.opciones : [
                  'Ignorar o descartar esta solicitud / actividad',
                  'Proyectar minuta de respuesta formal y remitir al Fiscal',
                  'Verificar folios y solicitar ampliación técnica a Policía Judicial'
                ],
                estado: srvAlert.estado || 'PENDIENTE',
                opcionSeleccionada: srvAlert.opcionSeleccionada || '',
                justificacion: srvAlert.justificacion || '',
                esDelegadaPorFiscal: true,
                delegadoPor: srvAlert.delegadoPor || 'Fiscal Delegado'
              };
              updatedList = [nueva, ...updatedList];
            }
          });

          return updatedList;
        });

        // Avisar al asistente si hay una nueva alerta que no ha visto
        serverAlerts.forEach(srvAlert => {
          if (!seenDelegatedIdsRef.current.has(srvAlert.id)) {
            seenDelegatedIdsRef.current.add(srvAlert.id);
            setNotificacionDelegada({
              id: srvAlert.id,
              titulo: srvAlert.titulo,
              delito: srvAlert.delito,
              delegadoPor: srvAlert.delegadoPor || 'Fiscal Delegado'
            });
            setTimeout(() => setNotificacionDelegada(null), 5500);
          }
        });

      } catch {
        // Silencioso en desconexión
      }
    };

    fetchDelegatedAlerts();
    const interval = setInterval(fetchDelegatedAlerts, 2500);
    return () => {
      isMounted = false;
      clearInterval(interval);
    };
  }, [equipoId]);

  const handleBackToLogin = () => {
    logoutUser();
    navigate('/');
  };

  // Descarga del informe en PDF en cualquier momento
  const handleDescargarInforme = () => {
    const memberNames = integrantes.length > 0 
      ? integrantes.map(i => `${i.nombreCompleto} (${i.rol})`)
      : [currentUser?.nombreCompleto || 'Asistente de Fiscal'];
    
    generatePDFReport(casos, groups, memberNames, currentUser, alertRecords);
  };

  // Abrir modal de gestión para una actividad
  const handleAbrirModalActividad = (act) => {
    setActividadModal(act);
    setOpcionTemporal(act.opcionSeleccionada || '');
    setJustificacionTemporal(act.justificacion || '');
    setMensajeExito('');
  };

  // Guardar la opción seleccionada y la justificación
  const handleGuardarGestionActividad = () => {
    if (!actividadModal) return;

    if (!opcionTemporal) {
      alert('Por favor selecciona una opción de actuación (o la opción de Ignorar).');
      return;
    }

    if (!justificacionTemporal.trim()) {
      alert('Por favor escribe la justificación o argumento de tu decisión. Este texto es obligatorio para el informe PDF.');
      return;
    }

    const isIgnorada = opcionTemporal.toLowerCase().includes('ignorar');
    const nuevoEstado = isIgnorada ? 'IGNORADA' : 'REALIZADA';

    // 1. Actualizar estado local de la actividad
    setActividades(prev => prev.map(a => {
      if (a.id === actividadModal.id) {
        return {
          ...a,
          estado: nuevoEstado,
          opcionSeleccionada: opcionTemporal,
          justificacion: justificacionTemporal.trim()
        };
      }
      return a;
    }));

    // 2. Si es una alerta delegada por el Fiscal, sincronizar la resolución en el backend
    if (actividadModal.esDelegadaPorFiscal) {
      axios.post(`http://localhost:8080/api/equipos/${encodeURIComponent(equipoId)}/alertas/resolver`, {
        alertaId: actividadModal.id,
        estado: nuevoEstado,
        opcionSeleccionada: opcionTemporal,
        justificacion: justificacionTemporal.trim(),
        resueltoPor: currentUser?.nombreCompleto || currentUser?.username || 'Asistente de Fiscal'
      }).catch(err => console.warn('Error al resolver alerta delegada en backend:', err));
    }

    // 3. Registrar en el contexto global para el informe PDF
    addAlertRecord({
      id: actividadModal.id,
      titulo: actividadModal.titulo,
      image: `Actividad [${actividadModal.id}]: ${actividadModal.titulo}`,
      status: actividadModal.esDelegadaPorFiscal 
        ? (isIgnorada ? 'DELEGADA IGNORADA' : 'DELEGADA RESUELTA')
        : (isIgnorada ? 'ACTIVIDAD IGNORADA' : 'ACTIVIDAD ATENDIDA'),
      opcionSeleccionada: opcionTemporal,
      justificacion: justificacionTemporal.trim(),
      response: `[Opción: ${opcionTemporal}] - Justificación: ${justificacionTemporal.trim()}`,
      rol: 'ASISTENTE',
      casoRadicado: actividadModal.radicado,
      casoDelito: actividadModal.delito,
      esDelegadaPorFiscal: Boolean(actividadModal.esDelegadaPorFiscal)
    });

    // Mostrar feedback y cerrar el modal
    setMensajeExito('¡Gestión guardada exitosamente y registrada para el informe PDF!');
    setTimeout(() => {
      setActividadModal(null);
      setMensajeExito('');
    }, 900);
  };

  const filteredCasos = casos.filter(c => {
    if (!filtroCaso.trim()) return true;
    const term = filtroCaso.toLowerCase();
    return c.nombre.toLowerCase().includes(term);
  });

  const pendientesCount = actividades.filter(a => a.estado === 'PENDIENTE').length;
  const realizadasCount = actividades.filter(a => a.estado === 'REALIZADA').length;
  const ignoradasCount = actividades.filter(a => a.estado === 'IGNORADA').length;

  return (
    <div className="inicio-asistente-bg" style={{ position: 'relative', width: '100vw', height: '100vh', overflow: 'hidden' }}>
      
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

      {/* Toast Notificación cuando el Fiscal delega una actuación */}
      <AnimatePresence>
        {notificacionDelegada && (
          <motion.div
            initial={{ y: -50, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            exit={{ y: -50, opacity: 0 }}
            style={{
              position: 'fixed',
              top: '75px',
              left: '50%',
              transform: 'translateX(-50%)',
              zIndex: 999999,
              backgroundColor: '#0F172A',
              border: '1.5px solid #38BDF8',
              borderRadius: '12px',
              padding: '12px 20px',
              boxShadow: '0 10px 30px rgba(0,0,0,0.6), 0 0 15px rgba(56, 189, 248, 0.3)',
              color: 'white',
              display: 'flex',
              alignItems: 'center',
              gap: '12px',
              fontFamily: 'sans-serif'
            }}
          >
            <div style={{ backgroundColor: '#0284C7', padding: '8px', borderRadius: '50%', color: 'white' }}>
              <FaBell size={16} />
            </div>
            <div>
              <div style={{ fontSize: '13px', fontWeight: 'bold', color: '#38BDF8' }}>
                ¡El Fiscal Delegado te ha trasladado una actuación!
              </div>
              <div style={{ fontSize: '12px', color: '#E2E8F0' }}>
                {notificacionDelegada.titulo}
              </div>
            </div>
            <button
              onClick={() => {
                setShowBandejaModal(true);
                setNotificacionDelegada(null);
              }}
              style={{
                backgroundColor: '#0284C7',
                color: 'white',
                border: 'none',
                borderRadius: '6px',
                padding: '6px 12px',
                fontSize: '11px',
                fontWeight: 'bold',
                cursor: 'pointer',
                marginLeft: '10px'
              }}
            >
              Abrir Bandeja
            </button>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Top Center-Right: Botón de Descargar Informe PDF */}
      <button
        onClick={handleDescargarInforme}
        style={{
          position: 'absolute',
          top: '20px',
          right: '180px',
          backgroundColor: 'rgba(30, 58, 138, 0.85)',
          color: '#93C5FD',
          border: '1px solid #3B82F6',
          padding: '8px 16px',
          borderRadius: '20px',
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          fontWeight: 'bold',
          fontSize: '13px',
          cursor: 'pointer',
          boxShadow: '0 4px 10px rgba(0,0,0,0.3)',
          backdropFilter: 'blur(5px)',
          zIndex: 10
        }}
      >
        <FaFileDownload /> Descargar Informe PDF
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
          backgroundColor: '#10B981',
          transition: 'width 1s linear'
        }}></div>
      </div>

      {/* Center Content: Estación de trabajo del Asistente */}
      <div style={{
        position: 'absolute',
        top: '46%',
        left: '50%',
        transform: 'translate(-50%, -50%)',
        textAlign: 'center',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        width: '100%',
        maxWidth: '750px',
        padding: '0 20px',
        zIndex: 10
      }}>
        <div style={{
          backgroundColor: 'rgba(16, 185, 129, 0.25)',
          border: '1px solid #34D399',
          color: '#6EE7B7',
          padding: '5px 16px',
          borderRadius: '20px',
          fontSize: '12px',
          fontWeight: 'bold',
          marginBottom: '12px',
          display: 'inline-flex',
          alignItems: 'center',
          gap: '8px',
          backdropFilter: 'blur(5px)'
        }}>
          <FaUserCheck /> ROL ACTIVO: ASISTENTE DE FISCAL
          {currentUser?.nombreCompleto ? ` · ${currentUser.nombreCompleto}` : ''}
        </div>

        <h1 style={{
          color: 'white',
          fontSize: '2.5rem',
          fontWeight: 'bold',
          textShadow: '0 2px 10px rgba(0,0,0,0.8)',
          marginBottom: '8px',
          marginTop: 0
        }}>
          Despacho · Estación del Asistente
        </h1>

        <p style={{ color: '#E2E8F0', fontSize: '15px', margin: '0 0 16px', textShadow: '0 2px 4px rgba(0,0,0,0.8)' }}>
          Gestión de expedientes, recepción de actuaciones y soporte operativo al Despacho Fiscal
        </p>

        <img src="/assets/PRISMA-DAE.png" alt="Brain Logo" style={{ width: '70px', filter: 'drop-shadow(0 0 10px rgba(52,211,153,0.8))' }} />

        <div style={{ color: 'white', marginTop: '10px', textShadow: '0 2px 4px rgba(0,0,0,0.8)' }}>
          <div style={{ fontWeight: 'bold', fontSize: '1.1rem', letterSpacing: '2px' }}>NEXUS</div>
          <div style={{ fontSize: '0.7rem', opacity: 0.8, letterSpacing: '1px' }}>DIRECCIÓN DE ALTOS ESTUDIOS</div>
        </div>

        {/* Pills informativas */}
        <div style={{ display: 'flex', gap: '15px', marginTop: '18px', flexWrap: 'wrap', justifyContent: 'center' }}>
          <div style={{ backgroundColor: 'rgba(0,0,0,0.6)', border: '1px solid rgba(52,211,153,0.4)', padding: '6px 16px', borderRadius: '20px', display: 'flex', alignItems: 'center', gap: '6px', color: 'white', fontSize: '0.85rem', backdropFilter: 'blur(5px)' }}>
            <FaClock color="#34D399" /> Jornada: 2 horas
          </div>
          <div style={{ backgroundColor: 'rgba(0,0,0,0.6)', border: '1px solid rgba(52,211,153,0.4)', padding: '6px 16px', borderRadius: '20px', display: 'flex', alignItems: 'center', gap: '6px', color: 'white', fontSize: '0.85rem', backdropFilter: 'blur(5px)' }}>
            <FaBriefcase color="#34D399" /> Casos del Despacho: {casos.length || 65}
          </div>
          <div style={{ backgroundColor: 'rgba(0,0,0,0.6)', border: '1px solid rgba(52,211,153,0.4)', padding: '6px 16px', borderRadius: '20px', display: 'flex', alignItems: 'center', gap: '6px', color: 'white', fontSize: '0.85rem', backdropFilter: 'blur(5px)' }}>
            <FaUsers color="#34D399" />{' '}
            {equipo ? `${equipo.nombre} (${totalFiscales}F / ${totalAsistentes}A)` : 'Equipo: General'}
          </div>
        </div>

        {/* BOTÓN CENTRAL: VER BANDEJA DE TRABAJO */}
        <motion.button
          onClick={() => setShowBandejaModal(true)}
          whileHover={{ scale: 1.04 }}
          whileTap={{ scale: 0.98 }}
          style={{
            marginTop: '32px',
            backgroundColor: '#34D399',
            color: '#064E3B',
            border: 'none',
            borderRadius: '12px',
            padding: '16px 36px',
            fontSize: '17px',
            fontWeight: 'bold',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '14px',
            boxShadow: '0 10px 25px rgba(52, 211, 153, 0.45)',
            transition: 'background-color 0.2s'
          }}
        >
          <FaFolderOpen size={24} />
          <span>VER BANDEJA DE ENTRADA Y EXPEDIENTES</span>
          <span style={{
            backgroundColor: pendientesCount > 0 ? '#EF4444' : '#064E3B',
            color: 'white',
            fontSize: '12px',
            padding: '3px 10px',
            borderRadius: '12px',
            fontWeight: 'bold'
          }}>
            {pendientesCount} Pendientes
          </span>
        </motion.button>
      </div>

      {/* MODAL / PANEL DE BANDEJA DE ENTRADA Y EXPEDIENTES */}
      <AnimatePresence>
        {showBandejaModal && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            style={{
              position: 'fixed',
              top: 0, left: 0, width: '100vw', height: '100vh',
              backgroundColor: 'rgba(4, 10, 18, 0.88)',
              zIndex: 9999,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: '24px',
              backdropFilter: 'blur(8px)',
              fontFamily: 'sans-serif'
            }}
          >
            <motion.div
              initial={{ scale: 0.95, y: 20 }}
              animate={{ scale: 1, y: 0 }}
              exit={{ scale: 0.95, y: 20 }}
              style={{
                width: '95vw',
                height: '92vh',
                backgroundColor: '#0A1324',
                border: '1.5px solid #10B981',
                borderRadius: '16px',
                display: 'flex',
                flexDirection: 'column',
                overflow: 'hidden',
                boxShadow: '0 25px 60px rgba(0,0,0,0.9)'
              }}
            >
              {/* Header del Panel */}
              <div style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '14px 24px',
                backgroundColor: '#0D231E',
                borderBottom: '1px solid rgba(52, 211, 153, 0.3)'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <div style={{ backgroundColor: '#34D399', color: '#064E3B', padding: '8px', borderRadius: '8px', display: 'flex' }}>
                    <FaFolderOpen size={20} />
                  </div>
                  <div>
                    <h2 style={{ margin: 0, fontSize: '18px', fontWeight: 'bold', color: '#ECFDF5' }}>
                      Bandeja de Trabajo · Asistente de Fiscal
                    </h2>
                    <p style={{ margin: 0, fontSize: '12px', color: '#A7F3D0' }}>
                      Expedientes del Despacho y Actividades por Realizar
                    </p>
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <button
                    onClick={handleDescargarInforme}
                    style={{
                      backgroundColor: 'rgba(30, 58, 138, 0.7)',
                      border: '1px solid #3B82F6',
                      color: '#93C5FD',
                      borderRadius: '8px',
                      padding: '7px 14px',
                      cursor: 'pointer',
                      fontWeight: 'bold',
                      fontSize: '12px',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '6px'
                    }}
                  >
                    <FaFileDownload /> Descargar Informe PDF
                  </button>

                  <button
                    onClick={() => setShowBandejaModal(false)}
                    style={{
                      backgroundColor: 'rgba(255,255,255,0.08)',
                      border: '1px solid rgba(255,255,255,0.2)',
                      color: '#E2E8F0',
                      borderRadius: '8px',
                      padding: '8px 16px',
                      cursor: 'pointer',
                      fontWeight: 'bold',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '6px'
                    }}
                  >
                    <FaTimes /> Cerrar Bandeja
                  </button>
                </div>
              </div>

              {/* Cuerpo del Panel: 2 Columnas (Izquierda: Casos / Derecha: Actividades) */}
              <div style={{ flex: 1, display: 'grid', gridTemplateColumns: '1.2fr 1fr', overflow: 'hidden' }}>
                
                {/* COLUMNA IZQUIERDA: EXPEDIENTES / CASOS */}
                <div style={{
                  borderRight: '1px solid #1E293B',
                  display: 'flex',
                  flexDirection: 'column',
                  padding: '18px',
                  backgroundColor: '#070E1A',
                  overflow: 'hidden'
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                    <h3 style={{ margin: 0, fontSize: '15px', color: '#38BDF8', display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <FaFolderOpen /> Expedientes Asignados ({filteredCasos.length})
                    </h3>
                  </div>

                  {/* Buscador de casos */}
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    backgroundColor: '#0F172A',
                    border: '1px solid #334155',
                    borderRadius: '8px',
                    padding: '8px 12px',
                    marginBottom: '14px'
                  }}>
                    <FaSearch color="#64748B" />
                    <input
                      type="text"
                      placeholder="Buscar caso por radicado o delito..."
                      value={filtroCaso}
                      onChange={(e) => setFiltroCaso(e.target.value)}
                      style={{ background: 'transparent', border: 'none', color: 'white', outline: 'none', width: '100%', fontSize: '13px' }}
                    />
                  </div>

                  {/* Grid / Lista de Casos con Scroll */}
                  <div style={{
                    flex: 1,
                    overflowY: 'auto',
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fill, minmax(210px, 1fr))',
                    gap: '12px',
                    paddingRight: '6px'
                  }}>
                    {filteredCasos.map((caso) => {
                      const radicado = caso.nombre.split('_')[0];
                      const delito = caso.nombre.includes('_')
                        ? caso.nombre.split('_')[1].split('.')[0].toUpperCase()
                        : 'EXPEDIENTE';

                      return (
                        <div
                          key={caso.nombre}
                          style={{
                            backgroundColor: '#0F172A',
                            border: '1px solid #1E293B',
                            borderRadius: '10px',
                            padding: '10px',
                            display: 'flex',
                            flexDirection: 'column',
                            gap: '8px',
                            transition: 'border-color 0.2s'
                          }}
                        >
                          <div style={{ height: '90px', backgroundColor: '#020617', borderRadius: '6px', overflow: 'hidden', position: 'relative' }}>
                            <img
                              src={`http://localhost:8080/api/images/${encodeURIComponent(caso.nombre)}?rol=ASISTENTE`}
                              alt="Caso Preview"
                              style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                              onError={(e) => { e.target.style.display = 'none'; }}
                            />
                            <button
                              onClick={() => setPreviewCaso(caso)}
                              style={{
                                position: 'absolute',
                                bottom: '6px',
                                right: '6px',
                                backgroundColor: 'rgba(16, 185, 129, 0.85)',
                                color: '#064E3B',
                                border: 'none',
                                borderRadius: '4px',
                                padding: '4px 8px',
                                fontSize: '11px',
                                fontWeight: 'bold',
                                cursor: 'pointer',
                                display: 'flex',
                                alignItems: 'center',
                                gap: '4px'
                              }}
                            >
                              <FaEye size={12} /> Ver
                            </button>
                          </div>

                          <div style={{ display: 'flex', flexDirection: 'column', gap: '3px' }}>
                            <div style={{ fontSize: '11px', fontWeight: 'bold', color: '#38BDF8', letterSpacing: '0.5px' }}>
                              NUNC: {radicado}
                            </div>
                            <div style={{ fontSize: '11px', color: '#94A3B8', textTransform: 'capitalize' }}>
                              Delito: {delito.toLowerCase()}
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>

                {/* COLUMNA DERECHA: ACTIVIDADES POR REALIZAR */}
                <div style={{
                  display: 'flex',
                  flexDirection: 'column',
                  padding: '18px',
                  backgroundColor: '#0A1324',
                  overflow: 'hidden'
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
                    <div>
                      <h3 style={{ margin: 0, fontSize: '15px', color: '#34D399', display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <FaTasks /> Actividades por Realizar ({actividades.length})
                      </h3>
                      <p style={{ margin: '3px 0 0', fontSize: '11px', color: '#94A3B8' }}>
                        Haz clic en una actividad para abrir el panel de gestión y decisión
                      </p>
                    </div>

                    <div style={{ display: 'flex', gap: '6px', fontSize: '11px' }}>
                      <span style={{ backgroundColor: 'rgba(239, 68, 68, 0.15)', color: '#FCA5A5', padding: '3px 8px', borderRadius: '12px', fontWeight: 'bold' }}>
                        {pendientesCount} Pendientes
                      </span>
                      <span style={{ backgroundColor: 'rgba(16, 185, 129, 0.15)', color: '#6EE7B7', padding: '3px 8px', borderRadius: '12px', fontWeight: 'bold' }}>
                        {realizadasCount} Realizadas
                      </span>
                      {ignoradasCount > 0 && (
                        <span style={{ backgroundColor: 'rgba(148, 163, 184, 0.15)', color: '#CBD5E1', padding: '3px 8px', borderRadius: '12px', fontWeight: 'bold' }}>
                          {ignoradasCount} Ignoradas
                        </span>
                      )}
                    </div>
                  </div>

                  {/* Listado de Actividades / Bandeja Interactiva */}
                  <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '12px', overflowY: 'auto', paddingRight: '6px' }}>
                    {actividades.map((act) => {
                      const isDone = act.estado === 'REALIZADA';
                      const isIgnorada = act.estado === 'IGNORADA';
                      const isPending = act.estado === 'PENDIENTE';

                      return (
                        <div
                          key={act.id}
                          onClick={() => handleAbrirModalActividad(act)}
                          style={{
                            padding: '14px 16px',
                            borderRadius: '10px',
                            cursor: 'pointer',
                            backgroundColor: isPending ? '#0F172A' : '#091524',
                            border: isPending ? '1.5px solid #334155' : isDone ? '1.5px solid #10B981' : '1.5px solid #64748B',
                            display: 'flex',
                            flexDirection: 'column',
                            gap: '10px',
                            transition: 'all 0.2s ease',
                            boxShadow: '0 4px 6px rgba(0,0,0,0.2)'
                          }}
                          onMouseEnter={(e) => {
                            e.currentTarget.style.borderColor = '#34D399';
                            e.currentTarget.style.transform = 'translateY(-2px)';
                          }}
                          onMouseLeave={(e) => {
                            e.currentTarget.style.borderColor = isPending ? '#334155' : isDone ? '#10B981' : '#64748B';
                            e.currentTarget.style.transform = 'translateY(0)';
                          }}
                        >
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                              <span style={{
                                fontSize: '10px',
                                fontWeight: 'bold',
                                padding: '2px 8px',
                                borderRadius: '4px',
                                backgroundColor: act.urgencia === 'ALTA' ? 'rgba(239, 68, 68, 0.2)' : 'rgba(245, 158, 11, 0.2)',
                                color: act.urgencia === 'ALTA' ? '#FCA5A5' : '#FCD34D'
                              }}>
                                {act.urgencia} PRIORIDAD
                              </span>
                              {act.esDelegadaPorFiscal && (
                                <span style={{
                                  fontSize: '10px',
                                  fontWeight: 'bold',
                                  padding: '2px 8px',
                                  borderRadius: '4px',
                                  backgroundColor: 'rgba(56, 189, 248, 0.25)',
                                  color: '#38BDF8',
                                  border: '1px solid rgba(56, 189, 248, 0.4)',
                                  display: 'inline-flex',
                                  alignItems: 'center',
                                  gap: '4px'
                                }}>
                                  <FaShareSquare size={9} /> DELEGADA POR FISCAL
                                </span>
                              )}
                              <span style={{ fontSize: '11px', color: '#64748B', fontWeight: 'bold' }}>
                                {act.id}
                              </span>
                            </div>

                            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                              {isDone && (
                                <span style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '11px', color: '#34D399', fontWeight: 'bold' }}>
                                  <FaCheckCircle /> Realizada
                                </span>
                              )}
                              {isIgnorada && (
                                <span style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '11px', color: '#94A3B8', fontWeight: 'bold' }}>
                                  <FaBan /> Ignorada
                                </span>
                              )}
                              {isPending && (
                                <span style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '11px', color: '#F59E0B', fontWeight: 'bold' }}>
                                  <FaExclamationCircle /> Pendiente
                                </span>
                              )}
                            </div>
                          </div>

                          <div style={{ fontSize: '14px', fontWeight: 'bold', color: '#F8FAFC', lineHeight: '1.4' }}>
                            {act.titulo}
                          </div>

                          {/* Vinculación con Noticia Criminal o Despacho General */}
                          <div style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            backgroundColor: '#070E1A',
                            padding: '6px 10px',
                            borderRadius: '6px',
                            fontSize: '11px',
                            color: '#94A3B8'
                          }}>
                            <div>
                              <strong style={{ color: '#38BDF8' }}>Expediente / Caso:</strong>{' '}
                              <span>{act.delito} {act.radicado ? `(${act.radicado})` : ''}</span>
                            </div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '4px', color: '#34D399', fontWeight: 'bold' }}>
                              <span>Gestionar</span> <FaChevronRight size={10} />
                            </div>
                          </div>

                          {/* Si ya fue gestionada, muestra la opción tomada */}
                          {act.opcionSeleccionada && (
                            <div style={{
                              fontSize: '11px',
                              color: isIgnorada ? '#CBD5E1' : '#6EE7B7',
                              backgroundColor: isIgnorada ? 'rgba(100, 116, 139, 0.15)' : 'rgba(16, 185, 129, 0.12)',
                              padding: '6px 10px',
                              borderRadius: '6px',
                              borderLeft: `3px solid ${isIgnorada ? '#94A3B8' : '#10B981'}`
                            }}>
                              <strong>Decisión tomada:</strong> {act.opcionSeleccionada}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </div>

              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* MODAL ESPECÍFICO DE LA ACTIVIDAD: DETALLE, CASO VINCULADO, OPCIONES (INCLUYENDO IGNORAR) Y JUSTIFICACIÓN */}
      <AnimatePresence>
        {actividadModal && (
          <div
            style={{
              position: 'fixed',
              top: 0, left: 0, width: '100vw', height: '100vh',
              backgroundColor: 'rgba(2, 6, 23, 0.92)',
              zIndex: 100000,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: '20px',
              backdropFilter: 'blur(10px)',
              fontFamily: 'sans-serif'
            }}
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              style={{
                width: '100%',
                maxWidth: '750px',
                maxHeight: '90vh',
                backgroundColor: '#0B1528',
                border: '1.5px solid #34D399',
                borderRadius: '16px',
                display: 'flex',
                flexDirection: 'column',
                overflow: 'hidden',
                boxShadow: '0 25px 60px rgba(0,0,0,0.95)'
              }}
            >
              {/* Header Modal Actividad */}
              <div style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '16px 22px',
                backgroundColor: '#0D231E',
                borderBottom: '1px solid rgba(52, 211, 153, 0.3)'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <span style={{
                    backgroundColor: 'rgba(52, 211, 153, 0.2)',
                    color: '#34D399',
                    fontSize: '11px',
                    padding: '3px 10px',
                    borderRadius: '6px',
                    fontWeight: 'bold'
                  }}>
                    {actividadModal.id}
                  </span>
                  <h3 style={{ margin: 0, fontSize: '17px', color: '#ECFDF5', fontWeight: 'bold' }}>
                    {actividadModal.titulo}
                  </h3>
                </div>

                <button
                  onClick={() => setActividadModal(null)}
                  style={{
                    background: 'none',
                    border: 'none',
                    color: '#94A3B8',
                    fontSize: '20px',
                    cursor: 'pointer',
                    padding: '4px'
                  }}
                >
                  <FaTimes />
                </button>
              </div>

              {/* Contenido Scrolleable del Modal */}
              <div style={{ flex: 1, overflowY: 'auto', padding: '22px', display: 'flex', flexDirection: 'column', gap: '18px' }}>
                
                {actividadModal.esDelegadaPorFiscal && (
                  <div style={{
                    backgroundColor: 'rgba(14, 116, 144, 0.25)',
                    border: '1px solid #06B6D4',
                    borderRadius: '10px',
                    padding: '12px 16px',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '12px',
                    color: '#67E8F9',
                    fontSize: '13px'
                  }}>
                    <div style={{ backgroundColor: '#0891B2', color: 'white', padding: '6px', borderRadius: '50%', display: 'flex' }}>
                      <FaUserTie size={14} />
                    </div>
                    <div>
                      <strong>Actuación trasladada por el Fiscal Delegado:</strong> Esta gestión fue delegada directamente a tu despacho para que adoptes la decisión operativa pertinente.
                    </div>
                  </div>
                )}

                {/* 1. Descripción de la Actividad */}
                <div style={{
                  backgroundColor: '#070E1A',
                  border: '1px solid #1E293B',
                  borderRadius: '10px',
                  padding: '14px 16px',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '6px'
                }}>
                  <div style={{ fontSize: '11px', fontWeight: 'bold', color: '#64748B', display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <FaInfoCircle color="#38BDF8" /> DETALLE DE LA SITUACIÓN U OPERACIÓN:
                  </div>
                  <div style={{ fontSize: '13.5px', color: '#E2E8F0', lineHeight: '1.6' }}>
                    {actividadModal.descripcion}
                  </div>
                </div>

                {/* 2. Correspondencia con Noticia Criminal o Expediente */}
                <div style={{
                  backgroundColor: '#0A1B2A',
                  border: '1px solid #1E3A8A',
                  borderRadius: '10px',
                  padding: '14px 16px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  gap: '12px'
                }}>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                    <span style={{ fontSize: '11px', fontWeight: 'bold', color: '#60A5FA' }}>
                      VINCULACIÓN A NOTICIA CRIMINAL / EXPEDIENTE:
                    </span>
                    <span style={{ fontSize: '13px', color: '#F1F5F9', fontWeight: 'bold' }}>
                      Radicado: {actividadModal.radicado || 'General'} · Delito: {actividadModal.delito}
                    </span>
                  </div>

                  {actividadModal.casoNombre && (
                    <button
                      onClick={() => setPreviewCaso({ nombre: actividadModal.casoNombre })}
                      style={{
                        backgroundColor: '#1E3A8A',
                        color: '#BFDBFE',
                        border: '1px solid #3B82F6',
                        borderRadius: '8px',
                        padding: '6px 12px',
                        fontSize: '12px',
                        fontWeight: 'bold',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '6px'
                      }}
                    >
                      <FaEye /> Ver Carátula del Caso
                    </button>
                  )}
                </div>

                {/* 3. Opciones de Actuación / Decisión (Incluye siempre Ignorar) */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  <label style={{ fontSize: '13px', fontWeight: 'bold', color: '#34D399', display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <FaTasks /> Selecciona la opción de actuación a realizar:
                  </label>

                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {actividadModal.opciones.map((opcion, idx) => {
                      const isSelected = opcionTemporal === opcion;
                      const isIgnorarOption = opcion.toLowerCase().includes('ignorar');

                      return (
                        <div
                          key={idx}
                          onClick={() => setOpcionTemporal(opcion)}
                          style={{
                            padding: '12px 16px',
                            borderRadius: '10px',
                            cursor: 'pointer',
                            backgroundColor: isSelected 
                              ? (isIgnorarOption ? 'rgba(239, 68, 68, 0.2)' : 'rgba(52, 211, 153, 0.18)')
                              : '#070E1A',
                            border: isSelected
                              ? (isIgnorarOption ? '2px solid #EF4444' : '2px solid #34D399')
                              : '1px solid #1E293B',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '12px',
                            transition: 'all 0.15s ease'
                          }}
                        >
                          <div style={{
                            width: '20px',
                            height: '20px',
                            borderRadius: '50%',
                            border: isSelected 
                              ? (isIgnorarOption ? '6px solid #EF4444' : '6px solid #34D399')
                              : '2px solid #64748B',
                            backgroundColor: 'transparent',
                            flexShrink: 0
                          }} />

                          <div style={{ flex: 1, fontSize: '13px', color: isSelected ? '#FFFFFF' : '#CBD5E1', fontWeight: isSelected ? 'bold' : 'normal' }}>
                            {isIgnorarOption ? (
                              <span style={{ color: '#FCA5A5', display: 'flex', alignItems: 'center', gap: '6px' }}>
                                <FaBan size={12} /> {opcion}
                              </span>
                            ) : (
                              opcion
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>

                {/* 4. Campo de Justificación (se resalta cuando hay opción seleccionada) */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  <label style={{ fontSize: '13px', fontWeight: 'bold', color: '#FCD34D', display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <FaEdit /> Escribe la justificación y argumento de tu elección:
                  </label>
                  <p style={{ margin: 0, fontSize: '11px', color: '#94A3B8' }}>
                    Este texto se guardará en la trazabilidad del informe PDF para ser evaluado por los calificadores.
                  </p>

                  <textarea
                    rows={4}
                    placeholder="Escribe aquí los motivos, consideraciones técnico-jurídicas o razones operativas por las cuales adoptaste esta decisión..."
                    value={justificacionTemporal}
                    onChange={(e) => setJustificacionTemporal(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '12px 14px',
                      borderRadius: '10px',
                      backgroundColor: '#070E1A',
                      color: 'white',
                      border: '1px solid #334155',
                      fontSize: '13px',
                      resize: 'vertical',
                      outline: 'none',
                      lineHeight: '1.5'
                    }}
                  />
                </div>

                {mensajeExito && (
                  <div style={{
                    padding: '10px 14px',
                    borderRadius: '8px',
                    backgroundColor: 'rgba(16, 185, 129, 0.2)',
                    border: '1px solid #10B981',
                    color: '#6EE7B7',
                    fontSize: '13px',
                    fontWeight: 'bold',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px'
                  }}>
                    <FaCheck /> {mensajeExito}
                  </div>
                )}

              </div>

              {/* Footer Modal con Botones */}
              <div style={{
                padding: '14px 22px',
                backgroundColor: '#070E1A',
                borderTop: '1px solid #1E293B',
                display: 'flex',
                justifyContent: 'flex-end',
                alignItems: 'center',
                gap: '12px'
              }}>
                <button
                  onClick={() => setActividadModal(null)}
                  style={{
                    backgroundColor: 'transparent',
                    border: '1px solid #475569',
                    color: '#94A3B8',
                    padding: '10px 18px',
                    borderRadius: '8px',
                    fontSize: '13px',
                    fontWeight: 'bold',
                    cursor: 'pointer'
                  }}
                >
                  Cancelar
                </button>

                <button
                  onClick={handleGuardarGestionActividad}
                  style={{
                    backgroundColor: '#34D399',
                    color: '#064E3B',
                    border: 'none',
                    padding: '10px 22px',
                    borderRadius: '8px',
                    fontSize: '13px',
                    fontWeight: 'bold',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    boxShadow: '0 4px 12px rgba(52, 211, 153, 0.35)'
                  }}
                >
                  <FaSave /> Guardar y Registrar Decisión
                </button>
              </div>

            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* MODAL PREVIEW DE IMAGEN DE CASO */}
      <AnimatePresence>
        {previewCaso && (
          <div
            style={{
              position: 'fixed',
              top: 0, left: 0, width: '100vw', height: '100vh',
              backgroundColor: 'rgba(0,0,0,0.85)',
              zIndex: 1000000,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: '30px'
            }}
            onClick={() => setPreviewCaso(null)}
          >
            <div
              style={{
                width: '80vw', height: '85vh',
                backgroundColor: '#0F172A',
                borderRadius: '12px',
                border: '1px solid #34D399',
                display: 'flex',
                flexDirection: 'column',
                overflow: 'hidden'
              }}
              onClick={(e) => e.stopPropagation()}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 20px', backgroundColor: '#0D231E' }}>
                <div style={{ color: '#34D399', fontWeight: 'bold', fontSize: '15px' }}>
                  EXPEDIENTE: {previewCaso.nombre}
                </div>
                <button
                  onClick={() => setPreviewCaso(null)}
                  style={{ background: 'none', border: 'none', color: '#94A3B8', fontSize: '18px', cursor: 'pointer' }}
                >
                  <FaTimes />
                </button>
              </div>
              <div style={{ flex: 1, overflow: 'auto', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px', backgroundColor: '#020617' }}>
                <img
                  src={`http://localhost:8080/api/images/${encodeURIComponent(previewCaso.nombre)}?rol=ASISTENTE`}
                  alt="Caso Detalle"
                  style={{ maxHeight: '100%', maxWidth: '100%', objectFit: 'contain' }}
                />
              </div>
            </div>
          </div>
        )}
      </AnimatePresence>

    </div>
  );
}
