import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { FaFolderOpen, FaSearch, FaCopy, FaEye, FaChevronLeft, FaChevronRight, FaPlus, FaLink } from 'react-icons/fa';
import { useTimer } from '../utils/useTimer';
import { useGlobal } from '../context/GlobalContext';
import GroupSidebar from '../components/GroupSidebar';

export default function CaseSelection() {
  const { casos, loading, error, groups, addGroup, addCasesToGroup, getCaseGroup, currentUser } = useGlobal();
  const isAsistente = currentUser?.rol === 'ASISTENTE';
  const activeRol = isAsistente ? 'ASISTENTE' : 'FISCAL';
  const homeRoute = isAsistente ? '/inicio-asistente' : '/inicio';
  
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedDelito, setSelectedDelito] = useState('');
  
  // Selection and Grouping State
  const [selectedForGroup, setSelectedForGroup] = useState([]);
  const [modalType, setModalType] = useState(null); // 'new' or 'existing'
  
  // Form state for Modals
  const [groupName, setGroupName] = useState('');
  const [assocType, setAssocType] = useState('MODALIDAD');
  const [justification, setJustification] = useState('');
  const [selectedGroupId, setSelectedGroupId] = useState('');

  // Details Modal state
  const [selectedCaseIndex, setSelectedCaseIndex] = useState(null);
  const [zoomLevel, setZoomLevel] = useState(100);

  const navigate = useNavigate();
  const { timeLeft, formatTime } = useTimer();

  const delitos = [...new Set(casos.map(c => c.delitos && c.delitos.length > 0 ? c.delitos[0] : '').filter(Boolean))];

  const filteredCasos = casos.filter(c => {
    const term = searchTerm.toLowerCase();
    
    // Si contiene números, busca por radicado/nombre. Si son letras, busca por delito.
    const isNumberSearch = /\d/.test(term); 
    
    let matchesSearch = false;
    if (term === '') {
      matchesSearch = true;
    } else if (isNumberSearch) {
      matchesSearch = (c.radicado && c.radicado.includes(term)) || (c.nombre && c.nombre.includes(term));
    } else {
      matchesSearch = c.delitos && c.delitos.some(d => d.toLowerCase().includes(term));
    }

    const crimeType = c.delitos && c.delitos.length > 0 ? c.delitos[0] : '';
    const matchesDelito = selectedDelito === '' || crimeType === selectedDelito;
    
    return matchesSearch && matchesDelito;
  });

  const toggleCaseSelection = (e, casoNombre) => {
    e.stopPropagation();
    setSelectedForGroup(prev => 
      prev.includes(casoNombre) 
        ? prev.filter(n => n !== casoNombre) 
        : [...prev, casoNombre]
    );
  };

  const handleCreateGroup = () => {
    if (!justification.trim()) {
      alert("Por favor escriba una justificación.");
      return;
    }
    const finalGroupName = groupName.trim() || `Grupo ${groups.length + 1}`;
    const newGroup = {
      id: `G${groups.length + 1}`,
      name: finalGroupName,
      cases: [...selectedForGroup],
      type: assocType,
      justification
    };
    addGroup(newGroup);
    setSelectedForGroup([]);
    setModalType(null);
    setAssocType('MODALIDAD');
    setJustification('');
    setGroupName('');
  };

  const handleAddToGroup = () => {
    if (!selectedGroupId) {
      alert("Seleccione un grupo.");
      return;
    }
    if (!justification.trim()) {
      alert("Por favor escriba una justificación.");
      return;
    }
    addCasesToGroup(selectedGroupId, selectedForGroup);
    setSelectedForGroup([]);
    setModalType(null);
    setSelectedGroupId('');
    setAssocType('MODALIDAD');
    setJustification('');
    setGroupName('');
  };

  const handleOpenModal = (index) => {
    setSelectedCaseIndex(index);
    setZoomLevel(100);
  };

  if (loading) return <div style={{ color: 'white', padding: '20px' }}>Cargando casos...</div>;
  if (error) return <div style={{ color: 'red', padding: '20px' }}>Error: {error}</div>;

  return (
    <div style={{ backgroundColor: isAsistente ? '#061D1A' : '#0A1128', minHeight: '100vh', width: '100vw', padding: '15px', color: 'white', display: 'flex', flexDirection: 'column', fontFamily: 'sans-serif' }}>
      
      {/* Top Header */}
      <header style={{ 
        display: 'flex', justifyContent: 'space-between', alignItems: 'center', 
        backgroundColor: isAsistente ? '#0D332E' : '#16234D', padding: '10px 20px', borderRadius: '8px', marginBottom: '10px',
        border: isAsistente ? '1px solid #10B981' : 'none'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
          <div style={{ backgroundColor: isAsistente ? '#34D399' : '#FDE047', padding: '8px', borderRadius: '8px', color: 'black', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <FaFolderOpen size={20} />
          </div>
          <div>
            <h2 style={{ margin: 0, fontSize: '18px', fontWeight: 'bold' }}>
              Gestión de Casos · {isAsistente ? 'Asistente de Fiscal' : 'Fiscal Delegado'}
            </h2>
            <p style={{ margin: 0, fontSize: '12px', opacity: 0.7 }}>
              Fiscalía General de la Nación {currentUser?.equipo ? `· ${currentUser.equipo.nombre}` : ''}
            </p>
          </div>
        </div>
        
        <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
          <div className="timer-clock-badge" style={{ backgroundColor: '#EF4444', padding: '8px 15px', borderRadius: '6px', fontWeight: 'bold', fontSize: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <div style={{ width: '8px', height: '8px', backgroundColor: 'white', borderRadius: '50%' }}></div>
            {formatTime(timeLeft)}
          </div>
          <button 
            onClick={() => navigate(homeRoute)}
            style={{ backgroundColor: isAsistente ? '#059669' : '#1E3A8A', color: 'white', border: 'none', padding: '8px 15px', borderRadius: '6px', cursor: 'pointer', fontWeight: 'bold' }}
          >
            Volver atrás
          </button>
        </div>
      </header>

      <div style={{ display: 'flex', flex: 1, overflow: 'hidden', gap: '20px' }}>
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
          {/* Search Bar & Filter */}
          <div style={{ display: 'flex', gap: '10px', marginBottom: '10px' }}>
        <div style={{ flex: 1, backgroundColor: '#16234D', borderRadius: '8px', padding: '10px 20px', display: 'flex', alignItems: 'center', gap: '10px' }}>
          <FaSearch color="#60A5FA" />
          <input 
            type="text" 
            placeholder="Buscar caso por número o delito..." 
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{ background: 'transparent', border: 'none', color: 'white', outline: 'none', flex: 1, fontSize: '14px' }}
          />
        </div>
        
        <select 
          value={selectedDelito} 
          onChange={(e) => setSelectedDelito(e.target.value)}
          style={{ backgroundColor: '#16234D', color: 'white', border: 'none', borderRadius: '8px', padding: '10px 20px', outline: 'none', cursor: 'pointer' }}
        >
          <option value="">Todos los delitos</option>
          {delitos.map(delito => (
            <option key={delito} value={delito}>{delito.charAt(0).toUpperCase() + delito.slice(1)}</option>
          ))}
        </select>

        <div style={{ backgroundColor: '#16234D', borderRadius: '8px', padding: '10px 20px', display: 'flex', alignItems: 'center', color: '#60A5FA', fontSize: '14px' }}>
          {filteredCasos.length} casos
        </div>
      </div>

      {/* Floating Action Bar for Grouping */}
      <AnimatePresence>
        {selectedForGroup.length > 0 && (
          <motion.div 
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            style={{ 
              backgroundColor: '#1E3A8A', 
              padding: '10px 20px', 
              borderRadius: '8px', 
              marginBottom: '15px', 
              display: 'flex', 
              justifyContent: 'space-between', 
              alignItems: 'center',
              border: '1px solid #3B82F6',
              boxShadow: '0 4px 6px rgba(0,0,0,0.3)'
            }}
          >
            <div style={{ fontWeight: 'bold' }}>{selectedForGroup.length} casos seleccionados para agrupar</div>
            <div style={{ display: 'flex', gap: '10px' }}>
              <button 
                onClick={() => setModalType('new')}
                style={{ backgroundColor: '#FDE047', color: 'black', border: 'none', padding: '8px 15px', borderRadius: '6px', fontWeight: 'bold', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '5px' }}
              >
                <FaPlus size={12} /> Crear Nuevo Grupo
              </button>
              {groups.length > 0 && (
                <button 
                  onClick={() => { setModalType('existing'); setSelectedGroupId(groups[0].id); }}
                  style={{ backgroundColor: 'transparent', color: 'white', border: '1px solid white', padding: '8px 15px', borderRadius: '6px', fontWeight: 'bold', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '5px' }}
                >
                  <FaLink size={12} /> Agregar a Existente
                </button>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <p style={{ fontSize: '14px', marginBottom: '15px', opacity: 0.9 }}>Selecciona un caso para ver su imagen en detalle, o marca su casilla para agruparlo.</p>

      {/* Cases Grid */}
      <div style={{ 
        flex: 1,
        minHeight: 0,
        display: 'grid', 
        gridTemplateColumns: 'repeat(5, 1fr)', 
        gap: '15px', 
        overflowY: 'auto',
        paddingRight: '5px',
        paddingBottom: '20px'
      }}>
        {filteredCasos.map((caso, index) => {
          const group = getCaseGroup(caso.nombre);
          const isSelected = selectedForGroup.includes(caso.nombre);

          return (
            <motion.div 
              key={caso.nombre}
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: (index % 10) * 0.05 }}
              style={{ 
                backgroundColor: isSelected ? '#1E3A8A' : '#1E293B', 
                borderRadius: '8px', 
                border: `1px solid ${isSelected ? '#3B82F6' : '#334155'}`,
                padding: '10px',
                display: 'flex',
                flexDirection: 'column',
                cursor: 'pointer',
                position: 'relative'
              }}
              onClick={() => handleOpenModal(index)}
              whileHover={{ borderColor: '#60A5FA' }}
            >
              <div style={{ position: 'relative', height: '120px', backgroundColor: 'white', borderRadius: '4px', marginBottom: '10px', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden' }}>
                <div style={{ position: 'absolute', top: '-10px', left: '-10px', backgroundColor: '#FDE047', color: 'black', width: '30px', height: '30px', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 'bold', fontSize: '12px', zIndex: 1 }}>
                  {(index + 1).toString().padStart(2, '0')}
                </div>
                
                {/* Group Ribbon */}
                {group && (
                  <div style={{ position: 'absolute', top: '10px', right: '-25px', backgroundColor: '#EF4444', color: 'white', fontSize: '10px', fontWeight: 'bold', padding: '2px 30px', transform: 'rotate(45deg)', zIndex: 1, boxShadow: '0 2px 4px rgba(0,0,0,0.3)' }}>
                    {group.name}
                  </div>
                )}

                <img 
                  src={`http://localhost:8080/api/images/${encodeURIComponent(caso.nombre)}?rol=${activeRol}`} 
                  alt="Case Preview" 
                  style={{ width: '100%', height: '100%', objectFit: 'cover', opacity: group ? 0.8 : 1 }}
                  onError={(e) => { e.target.style.display = 'none'; e.target.nextSibling.style.display = 'flex'; }}
                />
                <div style={{ display: 'none', width: '100%', height: '100%', backgroundColor: '#F1F5F9', alignItems: 'center', justifyContent: 'center', color: '#94A3B8', fontSize: '12px' }}>
                  Imagen no disponible
                </div>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '5px', marginBottom: '8px', fontSize: '12px', color: group ? '#EF4444' : '#94A3B8', fontWeight: group ? 'bold' : 'normal' }}>
                {!group ? (
                  <>
                    <input 
                      type="checkbox" 
                      style={{ accentColor: '#1E3A8A', width: '14px', height: '14px', cursor: 'pointer' }} 
                      checked={isSelected}
                      onClick={(e) => toggleCaseSelection(e, caso.nombre)} 
                    /> 
                    No agrupado
                  </>
                ) : (
                  <>
                    <div style={{ width: '8px', height: '8px', backgroundColor: '#EF4444', borderRadius: '50%' }}></div>
                    Agrupado
                  </>
                )}
              </div>

              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 'auto' }}>
                <span style={{ fontSize: '11px', whiteSpace: 'nowrap' }}>
                  {caso.nombre}
                </span>
                <div style={{ display: 'flex', gap: '5px' }}>
                  <button style={{ backgroundColor: 'transparent', border: '1px solid #334155', color: '#60A5FA', borderRadius: '4px', padding: '4px 6px', cursor: 'pointer' }} onClick={(e) => e.stopPropagation()}><FaCopy size={10} /></button>
                  <button style={{ backgroundColor: 'transparent', border: '1px solid #334155', color: '#60A5FA', borderRadius: '4px', padding: '4px 6px', cursor: 'pointer' }} onClick={(e) => e.stopPropagation()}><FaEye size={10} /></button>
                </div>
              </div>
            </motion.div>
          );
        })}
        </div>
        
        {/* Main Content Closing Divs */}
        </div>
        
        {groups.length > 0 && (
          <div style={{ width: '350px', height: '100%', overflow: 'hidden', borderRadius: '8px', border: '1px solid #1E3A8A' }}>
            <GroupSidebar />
          </div>
        )}
      </div>

      {/* Modals for Grouping */}
      <AnimatePresence>
        {modalType && (
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            style={{
              position: 'fixed', top: 0, left: 0, width: '100vw', height: '100vh',
              backgroundColor: 'rgba(0,0,0,0.7)', zIndex: 10000,
              display: 'flex', alignItems: 'center', justifyContent: 'center'
            }}
          >
            <motion.div 
              initial={{ scale: 0.95 }} animate={{ scale: 1 }} exit={{ scale: 0.95 }}
              style={{
                backgroundColor: '#1E3A8A', width: '500px', borderRadius: '12px', padding: '25px',
                border: '1px solid #3B82F6', boxShadow: '0 10px 25px rgba(0,0,0,0.5)',
                color: 'white', fontFamily: 'sans-serif'
              }}
            >
              <h2 style={{ color: '#FDE047', marginTop: 0, marginBottom: '10px', fontSize: '20px' }}>
                {modalType === 'new' ? 'Justificar Asociación Múltiple' : 'Agregar a Grupo Existente'}
              </h2>
              <p style={{ fontSize: '13px', opacity: 0.9, marginBottom: '20px' }}>
                {modalType === 'new' 
                  ? 'Se creará una relación en cadena para los casos seleccionados.' 
                  : 'Se asociarán los casos seleccionados al grupo elegido.'}
              </p>

              {modalType === 'new' && (
                <div style={{ marginBottom: '15px' }}>
                  <label style={{ display: 'block', marginBottom: '5px', fontSize: '14px', fontWeight: 'bold' }}>Nombre del Grupo (Opcional):</label>
                  <input 
                    type="text"
                    placeholder={`Ej. Grupo ${groups.length + 1}`}
                    value={groupName}
                    onChange={(e) => setGroupName(e.target.value)}
                    style={{ width: '100%', padding: '10px', backgroundColor: '#0F172A', color: 'white', border: '1px solid #334155', borderRadius: '6px' }}
                  />
                </div>
              )}

              {modalType === 'existing' && (
                <div style={{ marginBottom: '15px' }}>
                  <label style={{ display: 'block', fontSize: '13px', marginBottom: '5px', fontWeight: 'bold' }}>Seleccionar Grupo:</label>
                  <select 
                    value={selectedGroupId} 
                    onChange={(e) => setSelectedGroupId(e.target.value)}
                    style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid #3B82F6', backgroundColor: '#0F172A', color: 'white', outline: 'none' }}
                  >
                    <option value="" disabled>Seleccione un grupo...</option>
                    {groups.map(g => (
                      <option key={g.id} value={g.id}>{g.name} - {g.cases.length} casos</option>
                    ))}
                  </select>
                </div>
              )}

              <div style={{ marginBottom: '15px' }}>
                <label style={{ display: 'block', fontSize: '13px', marginBottom: '5px', fontWeight: 'bold' }}>Asociar por:</label>
                <select 
                  value={assocType} 
                  onChange={(e) => setAssocType(e.target.value)}
                  style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid #3B82F6', backgroundColor: '#0F172A', color: 'white', outline: 'none' }}
                >
                  <option value="MODALIDAD">MODALIDAD</option>
                  <option value="MODUS OPERANDI">MODUS OPERANDI</option>
                  <option value="PATRON">PATRON</option>
                  <option value="CRITERIO DE CONEXIDAD">CRITERIO DE CONEXIDAD</option>
                  <option value="FENOMENO CRIMINAL">FENOMENO CRIMINAL</option>
                  <option value="OTROS">OTROS</option>
                </select>
              </div>

              <div style={{ marginBottom: '25px' }}>
                <label style={{ display: 'block', fontSize: '13px', marginBottom: '5px', fontWeight: 'bold' }}>Justificación:</label>
                <textarea 
                  value={justification}
                  onChange={(e) => setJustification(e.target.value)}
                  placeholder="Escribe los detalles de la asociación..."
                  style={{ width: '100%', height: '100px', padding: '10px', borderRadius: '6px', border: '1px solid #3B82F6', backgroundColor: '#0F172A', color: 'white', outline: 'none', resize: 'none' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '15px' }}>
                <button 
                  onClick={() => setModalType(null)}
                  style={{ backgroundColor: '#E2E8F0', color: '#475569', border: 'none', padding: '10px 20px', borderRadius: '6px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  Cancelar
                </button>
                <button 
                  onClick={modalType === 'new' ? handleCreateGroup : handleAddToGroup}
                  style={{ backgroundColor: '#86EFAC', color: '#166534', border: 'none', padding: '10px 20px', borderRadius: '6px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  Confirmar
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Case Details Modal (Image Viewer) */}
      <AnimatePresence>
        {selectedCaseIndex !== null && !modalType && (
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            style={{
              position: 'fixed', top: 0, left: 0, width: '100vw', height: '100vh',
              backgroundColor: 'rgba(0,0,0,0.85)', zIndex: 9999,
              display: 'flex', alignItems: 'center', justifyContent: 'center', backdropFilter: 'blur(5px)'
            }}
            onClick={() => setSelectedCaseIndex(null)}
          >
            <div 
              style={{
                width: '80vw', height: '85vh', backgroundColor: '#0F172A', borderRadius: '12px',
                border: '1px solid #1E3A8A', display: 'flex', flexDirection: 'column', overflow: 'hidden', position: 'relative'
              }}
              onClick={(e) => e.stopPropagation()}
            >
              {/* Modal Header */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '15px 20px', backgroundColor: '#1E3A8A' }}>
                <h3 style={{ margin: 0, fontSize: '16px', color: '#FDE047', display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <div style={{ backgroundColor: '#FDE047', color: 'black', borderRadius: '50%', width: '24px', height: '24px', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '12px', fontWeight: 'bold' }}>
                    {(selectedCaseIndex + 1).toString().padStart(2, '0')}
                  </div>
                  {filteredCasos[selectedCaseIndex].nombre}
                </h3>
                
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <button onClick={() => setZoomLevel(z => Math.max(z - 20, 20))} style={{ backgroundColor: 'transparent', color: 'white', border: '1px solid rgba(255,255,255,0.3)', padding: '5px 10px', borderRadius: '4px', cursor: 'pointer' }}>-</button>
                  <span style={{ fontSize: '14px' }}>{zoomLevel}%</span>
                  <button onClick={() => setZoomLevel(100)} style={{ backgroundColor: 'transparent', color: 'white', border: '1px solid rgba(255,255,255,0.3)', padding: '5px 10px', borderRadius: '4px', cursor: 'pointer' }}>1:1</button>
                  <button onClick={() => setZoomLevel(z => Math.min(z + 20, 200))} style={{ backgroundColor: 'transparent', color: 'white', border: '1px solid rgba(255,255,255,0.3)', padding: '5px 10px', borderRadius: '4px', cursor: 'pointer' }}>+</button>
                  
                  <button onClick={() => navigate(`/board/${encodeURIComponent(filteredCasos[selectedCaseIndex].nombre)}`)} style={{ backgroundColor: 'transparent', color: 'white', border: '1px solid white', padding: '6px 15px', borderRadius: '4px', cursor: 'pointer', marginLeft: '10px' }}>Ubicar caso</button>
                  <button onClick={() => setSelectedCaseIndex(null)} style={{ backgroundColor: 'transparent', color: '#EF4444', border: '1px solid #EF4444', padding: '6px 15px', borderRadius: '4px', cursor: 'pointer', marginLeft: '10px' }}>Cerrar</button>
                </div>
              </div>

              {/* Modal Body with Image */}
              <div style={{ flex: 1, overflow: 'auto', display: 'flex', backgroundColor: '#F8FAFC', padding: '20px' }}>
                 <div style={{ margin: 'auto', display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100%', width: '100%' }}>
                   <img 
                     src={`http://localhost:8080/api/images/${encodeURIComponent(filteredCasos[selectedCaseIndex].nombre)}?rol=${activeRol}`} 
                     alt="Case Full" 
                     style={{ 
                       width: zoomLevel === 100 ? '100%' : `${zoomLevel}%`, 
                       height: zoomLevel === 100 ? '100%' : 'auto', 
                       maxHeight: zoomLevel === 100 ? '100%' : 'none',
                       objectFit: zoomLevel === 100 ? 'contain' : 'scale-down',
                       transition: 'all 0.2s', 
                       boxShadow: '0 10px 25px rgba(0,0,0,0.5)',
                       aspectRatio: '1536/1024'
                     }}
                   />
                 </div>
              </div>

              {/* Floating Arrows */}
              <button 
                onClick={(e) => { e.stopPropagation(); if (selectedCaseIndex > 0) { setSelectedCaseIndex(selectedCaseIndex - 1); setZoomLevel(100); } }}
                disabled={selectedCaseIndex === 0}
                style={{
                  position: 'absolute', top: '50%', left: '20px', transform: 'translateY(-50%)',
                  backgroundColor: '#1E3A8A', color: 'white', border: 'none', borderRadius: '50%', width: '40px', height: '40px',
                  display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: selectedCaseIndex === 0 ? 'not-allowed' : 'pointer',
                  opacity: selectedCaseIndex === 0 ? 0.3 : 1, boxShadow: '0 4px 6px rgba(0,0,0,0.3)'
                }}
              >
                <FaChevronLeft />
              </button>
              
              <button 
                onClick={(e) => { e.stopPropagation(); if (selectedCaseIndex < filteredCasos.length - 1) { setSelectedCaseIndex(selectedCaseIndex + 1); setZoomLevel(100); } }}
                disabled={selectedCaseIndex === filteredCasos.length - 1}
                style={{
                  position: 'absolute', top: '50%', right: '20px', transform: 'translateY(-50%)',
                  backgroundColor: '#1E3A8A', color: 'white', border: 'none', borderRadius: '50%', width: '40px', height: '40px',
                  display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: selectedCaseIndex === filteredCasos.length - 1 ? 'not-allowed' : 'pointer',
                  opacity: selectedCaseIndex === filteredCasos.length - 1 ? 0.3 : 1, boxShadow: '0 4px 6px rgba(0,0,0,0.3)'
                }}
              >
                <FaChevronRight />
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
