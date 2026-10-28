import React, { useState, useMemo, useCallback, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import ReactFlow, { Background, Controls, Handle, Position } from 'reactflow';
import 'reactflow/dist/style.css';
import { FaShieldAlt, FaSearch } from 'react-icons/fa';
import { useGlobal } from '../context/GlobalContext';
import { useTimer } from '../utils/useTimer';
import GroupSidebar from '../components/GroupSidebar';
import { generatePDFReport } from '../utils/PDFGenerator';
import { motion, AnimatePresence } from 'framer-motion';

// Custom Node Component
const CaseNode = ({ data }) => {
  return (
    <div style={{
      width: '60px', height: '60px',
      backgroundColor: '#1E3A8A',
      borderRadius: '50%',
      border: `2px solid ${data.isGrouped ? '#38BDF8' : '#64748B'}`,
      display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
      color: 'white',
      boxShadow: data.isGrouped ? '0 0 15px rgba(56, 189, 248, 0.5)' : '0 4px 10px rgba(0,0,0,0.5)',
      fontSize: '10px',
      textAlign: 'center',
      padding: '5px',
      animation: `float ${3 + Math.random() * 2}s ease-in-out infinite`,
      animationDelay: `${Math.random() * 2}s`
    }}>
      <style>
        {`
          @keyframes float {
            0% { transform: translateY(0px); }
            50% { transform: translateY(-10px); }
            100% { transform: translateY(0px); }
          }
        `}
      </style>
      <Handle type="target" position={Position.Top} style={{ opacity: 0 }} />
      <div style={{ fontWeight: 'bold', fontSize: '12px' }}>
        {data.label}
      </div>
      <div style={{ fontSize: '7px', opacity: 0.8, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '50px' }}>
        {data.sublabel}
      </div>
      <Handle type="source" position={Position.Bottom} style={{ opacity: 0 }} />
    </div>
  );
};

const nodeTypes = { caseNode: CaseNode };

export default function Board() {
  const navigate = useNavigate();
  const { casos, groups, currentUser, alertRecords } = useGlobal();
  const isAsistente = currentUser?.rol === 'ASISTENTE';
  const homeRoute = isAsistente ? '/inicio-asistente' : '/inicio';
  const { timeLeft, formatTime, isFinished } = useTimer();
  const [searchTerm, setSearchTerm] = useState('');
  
  // Modals state
  const [showTimeoutModal, setShowTimeoutModal] = useState(false);
  const [showWarningModal, setShowWarningModal] = useState(false);
  const [showNamesModal, setShowNamesModal] = useState(false);
  const [memberNames, setMemberNames] = useState(() => {
    const teamMembers = currentUser?.equipo?.integrantes;
    if (teamMembers && teamMembers.length > 0) {
      return teamMembers.map(m => `${m.nombreCompleto} (${m.rol})`);
    }
    return ['', '', ''];
  });
  const [simulationEnded, setSimulationEnded] = useState(false);

  // Listen to timer finished event
  useEffect(() => {
    const handleTimerFinished = () => {
      if (!simulationEnded) {
        setShowTimeoutModal(true);
      }
    };
    
    window.addEventListener('nexus_timer_finished', handleTimerFinished);
    
    // If it's already finished according to the hook but the event fired before this mounted
    if (isFinished && !simulationEnded) {
      setShowTimeoutModal(true);
    }
    
    return () => window.removeEventListener('nexus_timer_finished', handleTimerFinished);
  }, [isFinished, simulationEnded]);

  const handleFinishRequest = () => {
    const totalCases = casos.length;
    const groupedCasesCount = new Set(groups.flatMap(g => g.cases)).size;
    const hasUngroupedCases = totalCases > groupedCasesCount;

    if (hasUngroupedCases) {
      setShowWarningModal(true);
    } else {
      setShowNamesModal(true);
    }
  };

  const executeFinish = () => {
    setShowWarningModal(false);
    setShowTimeoutModal(false);
    setShowNamesModal(false);
    setSimulationEnded(true);
    generatePDFReport(casos, groups, memberNames, currentUser, alertRecords);
  };

  // Generate nodes and edges
  const { initialNodes, initialEdges } = useMemo(() => {
    const nodes = [];
    const edges = [];

    // Simple grid layout for ungrouped, clustered for grouped
    let ungroupedIndex = 0;
    const groupCenters = groups.map((_, i) => ({
      x: 300 + (i % 3) * 300,
      y: 200 + Math.floor(i / 3) * 300
    }));

    casos.forEach((caso, i) => {
      // Is it grouped?
      const groupIndex = groups.findIndex(g => g.cases.includes(caso.nombre));
      
      let x, y;
      if (groupIndex !== -1) {
        // Position around the group center
        const center = groupCenters[groupIndex];
        const casesInGroup = groups[groupIndex].cases;
        const indexInGroup = casesInGroup.indexOf(caso.nombre);
        const angle = (indexInGroup / casesInGroup.length) * Math.PI * 2;
        const radius = 100;
        x = center.x + Math.cos(angle) * radius;
        y = center.y + Math.sin(angle) * radius;

        // Connect to the first case in the group to form a star/cluster topology
        if (indexInGroup > 0) {
          edges.push({
            id: `e-${casesInGroup[0]}-${caso.nombre}`,
            source: casesInGroup[0],
            target: caso.nombre,
            type: 'straight',
            style: { stroke: '#38BDF8', strokeWidth: 2, opacity: 0.6 }
          });
        }
      } else {
        // Scatter ungrouped with an organic, slightly random layout
        const col = ungroupedIndex % 7;
        const row = Math.floor(ungroupedIndex / 7);
        x = 100 + col * 140 + (Math.random() - 0.5) * 80;
        y = 100 + row * 140 + (Math.random() - 0.5) * 80;
        ungroupedIndex++;
      }

      nodes.push({
        id: caso.nombre,
        type: 'caseNode',
        position: { x, y },
        data: { 
          label: (i + 1).toString().padStart(2, '0'), 
          sublabel: caso.nombre,
          isGrouped: groupIndex !== -1
        }
      });
    });

    return { initialNodes: nodes, initialEdges: edges };
  }, [casos, groups]);

  const [nodes, setNodes] = useState(initialNodes);
  const [edges, setEdges] = useState(initialEdges);

  // Filter nodes based on search
  useEffect(() => {
    if (!searchTerm) {
      setNodes(initialNodes);
      return;
    }
    const term = searchTerm.toLowerCase();
    const filtered = initialNodes.map(n => ({
      ...n,
      style: {
        ...n.style,
        opacity: n.id.toLowerCase().includes(term) || n.data.label.includes(term) ? 1 : 0.2
      }
    }));
    setNodes(filtered);
  }, [searchTerm, initialNodes]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', width: '100vw', backgroundColor: isAsistente ? '#061D1A' : '#0A1128', fontFamily: 'sans-serif' }}>
      
      {/* Top Header */}
      <header style={{ 
        display: 'flex', justifyContent: 'space-between', alignItems: 'center', 
        backgroundColor: isAsistente ? '#0D332E' : '#16234D', padding: '10px 20px', borderBottom: `1px solid ${isAsistente ? '#10B981' : '#1E3A8A'}`, zIndex: 10
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
          <div style={{ backgroundColor: isAsistente ? '#34D399' : '#FDE047', padding: '8px', borderRadius: '8px', color: 'black', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <FaShieldAlt size={20} />
          </div>
          <div>
            <h2 style={{ margin: 0, fontSize: '18px', fontWeight: 'bold', color: 'white' }}>
              TABLERO ANALÍTICO · {isAsistente ? 'ASISTENTE' : 'FISCAL'}
            </h2>
            <p style={{ margin: 0, fontSize: '12px', opacity: 0.7, color: 'white' }}>
              Fiscalía General de la Nación {currentUser?.equipo ? `· ${currentUser.equipo.nombre}` : ''}
            </p>
          </div>
        </div>

        {/* Center Search */}
        <div style={{ flex: 1, maxWidth: '600px', backgroundColor: isAsistente ? '#064E3B' : '#1E3A8A', borderRadius: '8px', padding: '10px 20px', display: 'flex', alignItems: 'center', gap: '10px' }}>
          <FaSearch color={isAsistente ? '#34D399' : '#60A5FA'} />
          <input 
            type="text" 
            placeholder="Buscar caso o nodo..." 
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{ background: 'transparent', border: 'none', color: 'white', outline: 'none', flex: 1, fontSize: '14px' }}
          />
        </div>
        
        <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
          <div className="timer-clock-badge" style={{ backgroundColor: '#EF4444', color: 'white', padding: '8px 15px', borderRadius: '6px', fontWeight: 'bold', fontSize: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <div style={{ width: '8px', height: '8px', backgroundColor: 'white', borderRadius: '50%' }}></div>
            {formatTime(timeLeft)}
          </div>
          <button 
            onClick={() => navigate(homeRoute)}
            disabled={simulationEnded}
            style={{ backgroundColor: simulationEnded ? '#334155' : (isAsistente ? '#059669' : '#2563EB'), color: simulationEnded ? '#94A3B8' : 'white', border: 'none', padding: '8px 20px', borderRadius: '6px', cursor: simulationEnded ? 'not-allowed' : 'pointer', fontWeight: 'bold' }}
          >
            Volver
          </button>
          <button 
            onClick={handleFinishRequest}
            disabled={simulationEnded}
            style={{ backgroundColor: simulationEnded ? '#7F1D1D' : 'transparent', color: simulationEnded ? '#FECACA' : '#EF4444', border: `1px solid ${simulationEnded ? '#7F1D1D' : '#EF4444'}`, padding: '8px 20px', borderRadius: '6px', cursor: simulationEnded ? 'not-allowed' : 'pointer', fontWeight: 'bold' }}
          >
            {simulationEnded ? 'Terminado' : 'Terminar'}
          </button>
        </div>
      </header>

      {/* Main Body */}
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        
        {/* Graph Area */}
        <div style={{ flex: 1, position: 'relative', opacity: simulationEnded ? 0.3 : 1, pointerEvents: simulationEnded ? 'none' : 'auto', transition: 'opacity 0.5s' }}>
          {/* Background Image Setup */}
          <div style={{ 
            position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, 
            backgroundImage: isAsistente
              ? "url('/assets/tablero-asistente.jpg'), url('/assets/tablero-analitico.png')"
              : "url('/assets/tablero-analitico.png')", 
            backgroundSize: 'cover', 
            backgroundPosition: 'center', 
            opacity: 0.5, 
            zIndex: 0 
          }} />
          
          <div style={{ position: 'absolute', top: '10px', left: '10px', backgroundColor: '#1E3A8A', color: 'white', padding: '8px 15px', borderRadius: '8px', fontSize: '12px', zIndex: 10, display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span style={{ fontWeight: 'bold' }}>Casos en movimiento</span>
            <span style={{ opacity: 0.8 }}>Progreso restaurado desde la última sesión local.</span>
          </div>

          <ReactFlow 
            nodes={nodes} 
            edges={edges} 
            nodeTypes={nodeTypes}
            fitView
            style={{ zIndex: 5 }}
            proOptions={{ hideAttribution: true }} // hide reactflow logo
          >
            <Background color="#334155" gap={16} />
            <Controls style={{ left: 10, bottom: 10 }} />
          </ReactFlow>
        </div>

        {/* Right Sidebar */}
        <div style={{ opacity: simulationEnded ? 0.3 : 1, pointerEvents: simulationEnded ? 'none' : 'auto', transition: 'opacity 0.5s', display: 'flex', height: '100%' }}>
          <GroupSidebar />
        </div>

      </div>

      {/* Warning Modal */}
      <AnimatePresence>
        {showWarningModal && (
          <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.8)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 9999 }}>
            <motion.div 
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              style={{ backgroundColor: '#1E293B', padding: '30px', borderRadius: '12px', width: '450px', border: '2px solid #F59E0B', color: 'white', textAlign: 'center' }}
            >
              <h2 style={{ color: '#F59E0B', marginTop: 0 }}>¡Advertencia!</h2>
              <p style={{ fontSize: '16px', margin: '20px 0' }}>
                Hay casos que no están agrupados. ¿Deseas de todas formas terminar la simulación y generar el informe?
              </p>
              <div style={{ display: 'flex', justifyContent: 'center', gap: '20px', marginTop: '30px' }}>
                <button 
                  onClick={() => { setShowWarningModal(false); setShowNamesModal(true); }}
                  style={{ backgroundColor: '#F59E0B', color: 'black', border: 'none', padding: '10px 30px', borderRadius: '8px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  Sí, Terminar
                </button>
                <button 
                  onClick={() => setShowWarningModal(false)}
                  style={{ backgroundColor: 'transparent', color: '#F59E0B', border: '1px solid #F59E0B', padding: '10px 30px', borderRadius: '8px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  No, Volver
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Names Modal */}
      <AnimatePresence>
        {showNamesModal && (
          <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.8)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 9999 }}>
            <motion.div 
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              style={{ backgroundColor: '#1E293B', padding: '30px', borderRadius: '12px', width: '450px', border: '2px solid #3B82F6', color: 'white', textAlign: 'center' }}
            >
              <h2 style={{ color: '#60A5FA', marginTop: 0 }}>Integrantes del Equipo</h2>
              <p style={{ fontSize: '14px', margin: '10px 0 20px', opacity: 0.8 }}>
                Confirma los integrantes del equipo para generar el informe PDF.
              </p>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '15px', marginBottom: '25px' }}>
                {memberNames.map((name, i) => (
                  <input 
                    key={i}
                    type="text"
                    placeholder={`Nombre del integrante ${i + 1}`}
                    value={name}
                    onChange={(e) => {
                      const newNames = [...memberNames];
                      newNames[i] = e.target.value;
                      setMemberNames(newNames);
                    }}
                    style={{ padding: '10px', borderRadius: '6px', border: '1px solid #334155', backgroundColor: '#0F172A', color: 'white', width: '100%' }}
                  />
                ))}
              </div>
              <div style={{ display: 'flex', justifyContent: 'center', gap: '20px' }}>
                <button 
                  onClick={executeFinish}
                  disabled={memberNames.some(name => name.trim() === '')}
                  style={{ backgroundColor: '#3B82F6', color: 'white', border: 'none', padding: '10px 30px', borderRadius: '8px', fontWeight: 'bold', cursor: memberNames.some(name => name.trim() === '') ? 'not-allowed' : 'pointer', opacity: memberNames.some(name => name.trim() === '') ? 0.5 : 1 }}
                >
                  Generar PDF
                </button>
                <button 
                  onClick={() => setShowNamesModal(false)}
                  style={{ backgroundColor: 'transparent', color: '#60A5FA', border: '1px solid #3B82F6', padding: '10px 30px', borderRadius: '8px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  Cancelar
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Timeout Modal */}
      <AnimatePresence>
        {showTimeoutModal && (
          <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.9)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 9999, backdropFilter: 'blur(5px)' }}>
            <motion.div 
              initial={{ y: -50, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              style={{ backgroundColor: '#1E293B', padding: '40px', borderRadius: '12px', width: '500px', border: '2px solid #EF4444', color: 'white', textAlign: 'center' }}
            >
              <h1 style={{ color: '#EF4444', marginTop: 0, fontSize: '24px' }}>¡Tiempo Agotado!</h1>
              <p style={{ fontSize: '18px', margin: '20px 0' }}>
                Se ha acabado el tiempo de la simulación. 
                <br /><br />
                Por favor dale clic al botón para finalizar y descargar el informe de tus decisiones.
              </p>
              <button 
                onClick={() => { setShowTimeoutModal(false); setShowNamesModal(true); }}
                style={{ backgroundColor: '#EF4444', color: 'white', border: 'none', padding: '15px 40px', borderRadius: '8px', fontWeight: 'bold', fontSize: '16px', cursor: 'pointer', marginTop: '20px' }}
              >
                Finalizar
              </button>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

    </div>
  );
}
