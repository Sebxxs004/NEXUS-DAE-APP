import React, { useState } from 'react';
import { useGlobal } from '../context/GlobalContext';
import { motion, AnimatePresence } from 'framer-motion';
import { FaChevronDown, FaChevronUp } from 'react-icons/fa';

const decisionOptionsFiscal = [
  { label: "1. Orden a la Policía Judicial.", question: "¿Qué actividad ordenará a la policía judicial?" },
  { label: "2. Orden de archivo.", question: "¿Cuál causal de archivo aplica?" },
  { label: "3. Solicitud de audiencia ante juez de control de garantías.", question: "¿Qué solicitud de garantías presentará?" },
  { label: "4. Priorizar investigación.", question: "¿Por qué debe priorizarse esta investigación?" },
  { label: "5. Caracterización de víctimas.", question: "¿Por qué debe caracterizarse a las víctimas?" },
  { label: "6. Fenómeno Criminal", question: "Justifique." },
  { label: "7. Otro.", question: "¿Cuál es la otra decisión y su fundamento?" }
];

const decisionOptionsAsistente = [
  { label: "1. Proyectar orden a la Policía Judicial.", question: "¿Qué elementos o actividades proyectará para revisión del Fiscal?" },
  { label: "2. Proyectar orden de archivo.", question: "¿Qué sustento fáctico y jurídico incluirá en el proyecto de archivo?" },
  { label: "3. Preparar solicitud de audiencia.", question: "¿Qué anexos y fundamentos preparará para la audiencia?" },
  { label: "4. Verificación y articulación de conexidad.", question: "¿Qué nexos o elementos comunes documentará en el expediente?" },
  { label: "5. Atención y contacto con víctimas/testigos.", question: "¿Qué gestión de citación o caracterización realizará?" },
  { label: "6. Informe de apoyo al Despacho.", question: "Justifique los hallazgos reportados." },
  { label: "7. Otro.", question: "¿Cuál es la otra actuación de asistente y su fundamento?" }
];

export default function GroupSidebar() {
  const { casos, groups, updateGroupDecision, currentUser } = useGlobal();
  const isAsistente = currentUser?.rol === 'ASISTENTE';
  const decisionOptions = isAsistente ? decisionOptionsAsistente : decisionOptionsFiscal;
  const [activeDecisionGroupId, setActiveDecisionGroupId] = useState(null);
  
  // State for the modal form
  const [selectedOptions, setSelectedOptions] = useState({});
  const [justifications, setJustifications] = useState({});

  // State for accordion (collapsed groups)
  const [collapsedGroups, setCollapsedGroups] = useState({});

  const totalCases = casos.length;
  const groupedCasesCount = new Set(groups.flatMap(g => g.cases)).size;
  const ungroupedCases = totalCases - groupedCasesCount;

  const handleOpenDecision = (groupId) => {
    setActiveDecisionGroupId(groupId);
    setSelectedOptions({});
    setJustifications({});
  };

  const handleToggleOption = (option) => {
    setSelectedOptions(prev => ({ ...prev, [option]: !prev[option] }));
  };

  const handleJustificationChange = (option, value) => {
    setJustifications(prev => ({ ...prev, [option]: value }));
  };

  const handleSubmitDecision = () => {
    const selectedKeys = Object.keys(selectedOptions).filter(k => selectedOptions[k]);
    if (selectedKeys.length === 0) {
      alert("Debes seleccionar al menos una opción.");
      return;
    }
    
    // Validar que todas las opciones marcadas tengan justificación
    const missingJustification = selectedKeys.some(k => !justifications[k] || !justifications[k].trim());
    if (missingJustification) {
      alert("Por favor, escribe una justificación o respuesta para todas las opciones que marcaste.");
      return;
    }
    
    // Save decision
    updateGroupDecision(activeDecisionGroupId, {
      options: selectedOptions,
      justifications
    });
    
    setActiveDecisionGroupId(null);
  };

  const toggleGroupCollapse = (groupId) => {
    setCollapsedGroups(prev => ({ ...prev, [groupId]: !prev[groupId] }));
  };

  return (
    <div style={{
      width: '350px',
      height: '100%',
      backgroundColor: '#0F172A',
      borderLeft: '1px solid #1E3A8A',
      display: 'flex',
      flexDirection: 'column',
      padding: '15px',
      color: 'white',
      fontFamily: 'sans-serif',
      position: 'relative'
    }}>
      
      {/* Top Half: Conexiones y grupos section */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0, marginBottom: '15px' }}>
        <h3 style={{ color: '#FDE047', fontSize: '14px', marginBottom: '10px', marginTop: 0 }}>Conexiones y grupos</h3>
        <div style={{ 
          backgroundColor: '#1E293B', 
          border: '1px solid #1E3A8A', 
          borderRadius: '8px', 
          padding: '10px',
          flex: 1,
          overflowY: 'auto'
        }}>
          <div style={{ color: '#FDE047', fontSize: '12px', fontWeight: 'bold', marginBottom: '10px' }}>Conexiones ({groups.length})</div>
          {groups.map((g, idx) => (
            <div key={idx} style={{ marginBottom: '15px', fontSize: '12px' }}>
              <div style={{ fontWeight: 'bold', marginBottom: '4px' }}>
                {g.cases.map(c => c.split('_')[0]).join(', ')}
              </div>
              <div style={{ opacity: 0.8 }}>Asociado por: {g.type}</div>
              <div style={{ opacity: 0.8 }}>Justificación: {g.justification}</div>
            </div>
          ))}
          {groups.length === 0 && (
            <div style={{ opacity: 0.5, fontSize: '12px' }}>No hay grupos creados.</div>
          )}
        </div>
      </div>

      {/* Bottom Half: Grupos detectados section */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0 }}>
        <h3 style={{ color: '#FDE047', fontSize: '14px', marginBottom: '10px' }}>Grupos detectados</h3>
        
        {/* Stats */}
        <div style={{ display: 'flex', gap: '10px', marginBottom: '15px' }}>
          <div style={{ flex: 1, backgroundColor: '#1E293B', border: '1px solid #1E3A8A', borderRadius: '8px', padding: '15px 10px', textAlign: 'center' }}>
            <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#FDE047' }}>{totalCases}</div>
            <div style={{ fontSize: '11px', opacity: 0.8 }}>Total de casos</div>
          </div>
          <div style={{ flex: 1, backgroundColor: '#1E293B', border: '1px solid #FDE047', borderRadius: '8px', padding: '15px 10px', textAlign: 'center' }}>
            <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#FDE047' }}>{ungroupedCases}</div>
            <div style={{ fontSize: '11px', opacity: 0.8 }}>Sin grupo</div>
          </div>
        </div>

        {/* Group Cards (Scrollable) */}
        <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '10px', paddingRight: '5px' }}>
          {groups.map((g, idx) => {
            const isCollapsed = collapsedGroups[g.id];
            return (
              <div key={idx} style={{ backgroundColor: '#1E293B', border: '1px solid #1E3A8A', borderRadius: '8px', padding: '15px', display: 'flex', flexDirection: 'column' }}>
                <div 
                  style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', cursor: 'pointer' }}
                  onClick={() => toggleGroupCollapse(g.id)}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <div style={{ width: '16px', height: '16px', backgroundColor: g.decision ? '#10B981' : '#EF4444', borderRadius: '4px' }}></div>
                    <div style={{ fontWeight: 'bold', fontSize: '14px' }}>{g.name}</div>
                  </div>
                  <div style={{ color: '#60A5FA' }}>
                    {isCollapsed ? <FaChevronDown size={12} /> : <FaChevronUp size={12} />}
                  </div>
                </div>
                
                <AnimatePresence>
                  {!isCollapsed && (
                    <motion.div 
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: 'auto', opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      style={{ overflow: 'hidden', paddingTop: '10px' }}
                    >
                      <div style={{ fontSize: '11px', opacity: 0.8, marginBottom: '5px' }}>
                        Números en tablero: {g.cases.length > 0 ? `N.º ${g.cases.map(c => c.substring(c.length - 2)).join(' - N.º ')}` : ''}
                      </div>
                      <div style={{ fontSize: '10px', opacity: 0.6, marginBottom: '15px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {g.cases.map(c => `N.º ${c.substring(c.length - 2)} - ${c}`).join('\n')}
                      </div>
                      
                      {g.decision ? (
                        <div style={{ width: '100%', padding: '10px', textAlign: 'center', color: '#10B981', fontWeight: 'bold', border: '1px solid #10B981', borderRadius: '20px', fontSize: '12px' }}>
                          Decisión tomada
                        </div>
                      ) : (
                        <button 
                          onClick={(e) => { e.stopPropagation(); handleOpenDecision(g.id); }}
                          style={{ 
                            width: '100%', 
                            backgroundColor: '#0EA5E9', 
                            color: 'white', 
                            border: 'none', 
                            padding: '10px', 
                            borderRadius: '20px', 
                            fontWeight: 'bold',
                            cursor: 'pointer'
                          }}
                        >
                          ¿Qué va a decidir ahora?
                        </button>
                      )}
                      
                      <div style={{ fontSize: '10px', textAlign: 'center', marginTop: '8px', opacity: 0.8 }}>
                        {g.cases.length} casos conectados
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            );
          })}
        </div>
        
        <button style={{ 
          width: '100%', 
          backgroundColor: 'transparent', 
          color: 'white', 
          border: '1px solid rgba(255,255,255,0.2)', 
          padding: '12px', 
          borderRadius: '8px', 
          cursor: 'pointer',
          marginTop: '15px'
        }}>
          Recalcular grupos
        </button>
      </div>

      {/* Decision Modal */}
      <AnimatePresence>
        {activeDecisionGroupId && (
          <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.7)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
            <motion.div 
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              style={{ backgroundColor: '#16234D', padding: '30px', borderRadius: '12px', width: '500px', border: '2px solid #3B82F6', boxShadow: '0 0 30px rgba(59, 130, 246, 0.5)', color: 'white' }}
            >
              <h2 style={{ color: '#FDE047', marginTop: 0, marginBottom: '10px' }}>
                ¿Qué vas a decidir ahora?
              </h2>
              <p style={{ fontSize: '14px', marginBottom: '20px', opacity: 0.9 }}>
                Selecciona una o más opciones y justifica cada decisión:
              </p>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '15px', maxHeight: '400px', overflowY: 'auto', paddingRight: '10px' }}>
                {decisionOptions.map((opt, i) => (
                  <div key={i} style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    <label style={{ display: 'flex', alignItems: 'center', gap: '10px', cursor: 'pointer', fontWeight: 'bold', fontSize: '14px' }}>
                      <input 
                        type="checkbox" 
                        checked={!!selectedOptions[opt.label]} 
                        onChange={() => handleToggleOption(opt.label)}
                        style={{ width: '18px', height: '18px', cursor: 'pointer' }}
                      />
                      {opt.label}
                    </label>
                    
                    {selectedOptions[opt.label] && (
                      <motion.div 
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: 'auto', opacity: 1 }}
                        style={{ paddingLeft: '28px', overflow: 'hidden' }}
                      >
                        <div style={{ fontSize: '12px', marginBottom: '5px', color: '#93C5FD' }}>{opt.question}</div>
                        <textarea 
                          placeholder="Escribe tu respuesta aquí..."
                          value={justifications[opt.label] || ''}
                          onChange={(e) => handleJustificationChange(opt.label, e.target.value)}
                          style={{ 
                            width: '100%', padding: '10px', borderRadius: '6px', 
                            backgroundColor: '#0F172A', color: 'white', 
                            border: '1px solid #334155', minHeight: '60px', resize: 'vertical', fontSize: '12px' 
                          }}
                        />
                      </motion.div>
                    )}
                  </div>
                ))}
              </div>

              <div style={{ display: 'flex', gap: '15px', marginTop: '30px' }}>
                <button 
                  onClick={handleSubmitDecision}
                  style={{ backgroundColor: '#0EA5E9', color: 'white', border: 'none', padding: '10px 20px', borderRadius: '20px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  Aceptar
                </button>
                <button 
                  onClick={() => setActiveDecisionGroupId(null)}
                  style={{ backgroundColor: 'transparent', color: '#60A5FA', border: 'none', padding: '10px 20px', borderRadius: '20px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  Cancelar
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

    </div>
  );
}
