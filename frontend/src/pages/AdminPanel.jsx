import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { FaUsersCog, FaPlus, FaTrash, FaUserPlus, FaSignOutAlt, FaUserTie, FaUserCheck, FaLayerGroup } from 'react-icons/fa';
import { useGlobal } from '../context/GlobalContext';

export default function AdminPanel() {
  const navigate = useNavigate();
  const { currentUser, logoutUser } = useGlobal();

  const [equipos, setEquipos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Form state for new Equipo
  const [teamName, setTeamName] = useState('');
  const [teamDesc, setTeamDesc] = useState('');

  // Form state for adding a Usuario to an Equipo
  const [selectedEquipoId, setSelectedEquipoId] = useState('');
  const [nombreCompleto, setNombreCompleto] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [rol, setRol] = useState('FISCAL');

  const fetchEquipos = async () => {
    setLoading(true);
    try {
      const res = await axios.get('http://localhost:8080/api/admin/equipos');
      setEquipos(res.data);
      if (res.data.length > 0 && !selectedEquipoId) {
        setSelectedEquipoId(res.data[0].id);
      }
    } catch (err) {
      setErrorMsg('No se pudo cargar la lista de equipos desde el servidor.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEquipos();
  }, []);

  const showTemporarySuccess = (msg) => {
    setSuccessMsg(msg);
    setErrorMsg('');
    setTimeout(() => setSuccessMsg(''), 3500);
  };

  const handleCreateEquipo = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    if (!teamName.trim()) return;

    try {
      const res = await axios.post('http://localhost:8080/api/admin/equipos', {
        nombre: teamName.trim(),
        descripcion: teamDesc.trim()
      });
      setTeamName('');
      setTeamDesc('');
      await fetchEquipos();
      setSelectedEquipoId(res.data.id);
      showTemporarySuccess(`Equipo "${res.data.nombre}" creado exitosamente.`);
    } catch (err) {
      setErrorMsg(err.response?.data?.message || 'Error al crear el equipo.');
    }
  };

  const handleDeleteEquipo = async (equipoId, nombre) => {
    if (!window.confirm(`¿Eliminar el equipo "${nombre}" y todos sus usuarios asociados?`)) return;
    setErrorMsg('');
    try {
      await axios.delete(`http://localhost:8080/api/admin/equipos/${equipoId}`);
      if (selectedEquipoId === equipoId) {
        setSelectedEquipoId('');
      }
      await fetchEquipos();
      showTemporarySuccess(`Equipo "${nombre}" eliminado.`);
    } catch (err) {
      setErrorMsg('No se pudo eliminar el equipo.');
    }
  };

  const handleAddUsuario = async (e) => {
    e.preventDefault();
    setErrorMsg('');

    if (!selectedEquipoId) {
      setErrorMsg('Primero selecciona o crea un equipo.');
      return;
    }

    try {
      const res = await axios.post(`http://localhost:8080/api/admin/equipos/${selectedEquipoId}/usuarios`, {
        nombreCompleto: nombreCompleto.trim(),
        username: username.trim(),
        password: password,
        rol: rol
      });
      setNombreCompleto('');
      setUsername('');
      setPassword('');
      await fetchEquipos();
      showTemporarySuccess(`Usuario "${res.data.username}" (${res.data.rol}) agregado al equipo.`);
    } catch (err) {
      setErrorMsg(err.response?.data?.message || 'Error al crear el usuario.');
    }
  };

  const handleDeleteUsuario = async (usuarioId, uname) => {
    setErrorMsg('');
    try {
      await axios.delete(`http://localhost:8080/api/admin/usuarios/${usuarioId}`);
      await fetchEquipos();
      showTemporarySuccess(`Usuario "${uname}" eliminado.`);
    } catch (err) {
      setErrorMsg('No se pudo eliminar el usuario.');
    }
  };

  const handleLogout = () => {
    logoutUser();
    navigate('/');
  };

  return (
    <div style={{
      minHeight: '100vh',
      width: '100vw',
      backgroundColor: '#0A1128',
      backgroundImage: "linear-gradient(rgba(10, 17, 40, 0.92), rgba(10, 17, 40, 0.96)), url('/assets/fondo-admin.jpeg')",
      backgroundSize: 'cover',
      backgroundPosition: 'center',
      color: 'white',
      display: 'flex',
      flexDirection: 'column',
      fontFamily: 'sans-serif'
    }}>
      {/* Top Navbar */}
      <header style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: '16px 32px',
        backgroundColor: '#111C38',
        borderBottom: '1px solid #1E3A8A'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div style={{ backgroundColor: '#F59E0B', color: 'black', padding: '10px', borderRadius: '8px', display: 'flex' }}>
            <FaUsersCog size={22} />
          </div>
          <div>
            <h1 style={{ margin: 0, fontSize: '20px', fontWeight: 'bold' }}>PANEL DE ADMINISTRACIÓN · NEXUS DAE</h1>
            <p style={{ margin: 0, fontSize: '12px', color: '#94A3B8' }}>
              Gestión de Equipos Mixtos, Fiscales Delegados y Asistentes Judiciales
            </p>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          {currentUser && (
            <span style={{ fontSize: '13px', color: '#CBD5E1' }}>
              Sesión: <strong>{currentUser.nombreCompleto}</strong> (<span style={{ color: '#FDE047' }}>{currentUser.rol}</span>)
            </span>
          )}
          <button
            onClick={handleLogout}
            style={{
              backgroundColor: 'rgba(239, 68, 68, 0.15)',
              color: '#FCA5A5',
              border: '1px solid #EF4444',
              padding: '8px 16px',
              borderRadius: '8px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              fontWeight: 'bold'
            }}
          >
            <FaSignOutAlt /> Cerrar Sesión
          </button>
        </div>
      </header>

      {/* Main Content */}
      <div style={{ padding: '28px 32px', maxWidth: '1400px', width: '100%', margin: '0 auto', flex: 1 }}>
        {errorMsg && (
          <div style={{ backgroundColor: 'rgba(239, 68, 68, 0.18)', border: '1px solid #EF4444', color: '#FCA5A5', padding: '12px 16px', borderRadius: '8px', marginBottom: '20px' }}>
            {errorMsg}
          </div>
        )}

        {successMsg && (
          <div style={{ backgroundColor: 'rgba(16, 185, 129, 0.18)', border: '1px solid #10B981', color: '#6EE7B7', padding: '12px 16px', borderRadius: '8px', marginBottom: '20px' }}>
            {successMsg}
          </div>
        )}

        <div style={{ display: 'grid', gridTemplateColumns: '380px 1fr', gap: '28px', alignItems: 'start' }}>
          
          {/* Left Column: Forms */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
            
            {/* Card 1: Create Equipo */}
            <div style={{ backgroundColor: '#16234D', border: '1px solid #1E3A8A', borderRadius: '12px', padding: '22px' }}>
              <h3 style={{ marginTop: 0, marginBottom: '14px', display: 'flex', alignItems: 'center', gap: '10px', color: '#FDE047', fontSize: '16px' }}>
                <FaLayerGroup /> 1. Crear Nuevo Equipo / Despacho
              </h3>
              <form onSubmit={handleCreateEquipo} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <input
                  type="text"
                  placeholder="Nombre del Equipo (ej. Despacho 02 - Mixto)"
                  value={teamName}
                  onChange={(e) => setTeamName(e.target.value)}
                  style={{ padding: '10px 12px', borderRadius: '8px', border: '1px solid #334155', backgroundColor: '#0F172A', color: 'white' }}
                  required
                />
                <input
                  type="text"
                  placeholder="Descripción opcional"
                  value={teamDesc}
                  onChange={(e) => setTeamDesc(e.target.value)}
                  style={{ padding: '10px 12px', borderRadius: '8px', border: '1px solid #334155', backgroundColor: '#0F172A', color: 'white' }}
                />
                <button
                  type="submit"
                  style={{
                    backgroundColor: '#FDE047',
                    color: 'black',
                    fontWeight: 'bold',
                    border: 'none',
                    padding: '11px',
                    borderRadius: '8px',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '8px'
                  }}
                >
                  <FaPlus /> Crear Equipo
                </button>
              </form>
            </div>

            {/* Card 2: Add Member (Fiscal or Asistente) to Equipo */}
            <div style={{ backgroundColor: '#16234D', border: '1px solid #1E3A8A', borderRadius: '12px', padding: '22px' }}>
              <h3 style={{ marginTop: 0, marginBottom: '14px', display: 'flex', alignItems: 'center', gap: '10px', color: '#38BDF8', fontSize: '16px' }}>
                <FaUserPlus /> 2. Agregar Integrante a un Equipo
              </h3>
              <form onSubmit={handleAddUsuario} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <div>
                  <label style={{ fontSize: '12px', color: '#94A3B8', display: 'block', marginBottom: '4px' }}>Equipo asignado:</label>
                  <select
                    value={selectedEquipoId}
                    onChange={(e) => setSelectedEquipoId(e.target.value)}
                    style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid #334155', backgroundColor: '#0F172A', color: 'white' }}
                    required
                  >
                    <option value="" disabled>Seleccione un equipo...</option>
                    {equipos.map(eq => (
                      <option key={eq.id} value={eq.id}>
                        {eq.nombre} ({eq.totalFiscales}F / {eq.totalAsistentes}A)
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label style={{ fontSize: '12px', color: '#94A3B8', display: 'block', marginBottom: '4px' }}>Rol del Usuario:</label>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                    <button
                      type="button"
                      onClick={() => setRol('FISCAL')}
                      style={{
                        padding: '10px',
                        borderRadius: '8px',
                        border: rol === 'FISCAL' ? '2px solid #38BDF8' : '1px solid #334155',
                        backgroundColor: rol === 'FISCAL' ? 'rgba(56, 189, 248, 0.2)' : '#0F172A',
                        color: rol === 'FISCAL' ? '#38BDF8' : '#94A3B8',
                        fontWeight: 'bold',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: '6px'
                      }}
                    >
                      <FaUserTie /> FISCAL
                    </button>
                    <button
                      type="button"
                      onClick={() => setRol('ASISTENTE')}
                      style={{
                        padding: '10px',
                        borderRadius: '8px',
                        border: rol === 'ASISTENTE' ? '2px solid #34D399' : '1px solid #334155',
                        backgroundColor: rol === 'ASISTENTE' ? 'rgba(52, 211, 153, 0.2)' : '#0F172A',
                        color: rol === 'ASISTENTE' ? '#34D399' : '#94A3B8',
                        fontWeight: 'bold',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: '6px'
                      }}
                    >
                      <FaUserCheck /> ASISTENTE
                    </button>
                  </div>
                </div>

                <input
                  type="text"
                  placeholder="Nombre completo del integrante"
                  value={nombreCompleto}
                  onChange={(e) => setNombreCompleto(e.target.value)}
                  style={{ padding: '10px 12px', borderRadius: '8px', border: '1px solid #334155', backgroundColor: '#0F172A', color: 'white' }}
                  required
                />

                <input
                  type="text"
                  placeholder="Usuario de acceso (ej. fiscal3, asistente2)"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  style={{ padding: '10px 12px', borderRadius: '8px', border: '1px solid #334155', backgroundColor: '#0F172A', color: 'white' }}
                  required
                />

                <input
                  type="text"
                  placeholder="Contraseña de acceso"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  style={{ padding: '10px 12px', borderRadius: '8px', border: '1px solid #334155', backgroundColor: '#0F172A', color: 'white' }}
                  required
                />

                <button
                  type="submit"
                  style={{
                    backgroundColor: '#2563EB',
                    color: 'white',
                    fontWeight: 'bold',
                    border: 'none',
                    padding: '11px',
                    borderRadius: '8px',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '8px'
                  }}
                >
                  <FaUserPlus /> Registrar Usuario en Equipo
                </button>
              </form>
            </div>

          </div>

          {/* Right Column: Teams and Members Overview */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
            <h2 style={{ margin: 0, fontSize: '18px', color: 'white' }}>
              Equipos Conformados ({equipos.length})
            </h2>

            {loading ? (
              <div style={{ color: '#94A3B8' }}>Cargando equipos...</div>
            ) : equipos.length === 0 ? (
              <div style={{ backgroundColor: '#16234D', padding: '30px', borderRadius: '12px', textAlign: 'center', color: '#94A3B8' }}>
                No hay equipos registrados. Crea el primer equipo en el panel izquierdo.
              </div>
            ) : (
              equipos.map(equipo => (
                <div
                  key={equipo.id}
                  style={{
                    backgroundColor: '#16234D',
                    border: selectedEquipoId === equipo.id ? '2px solid #38BDF8' : '1px solid #1E3A8A',
                    borderRadius: '12px',
                    padding: '20px'
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '14px' }}>
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        <h3 style={{ margin: 0, fontSize: '18px', color: '#FDE047' }}>{equipo.nombre}</h3>
                        <span style={{ backgroundColor: 'rgba(56, 189, 248, 0.15)', color: '#38BDF8', border: '1px solid rgba(56, 189, 248, 0.4)', padding: '3px 10px', borderRadius: '12px', fontSize: '12px', fontWeight: 'bold' }}>
                          {equipo.totalFiscales} {equipo.totalFiscales === 1 ? 'Fiscal' : 'Fiscales'}
                        </span>
                        <span style={{ backgroundColor: 'rgba(52, 211, 153, 0.15)', color: '#34D399', border: '1px solid rgba(52, 211, 153, 0.4)', padding: '3px 10px', borderRadius: '12px', fontSize: '12px', fontWeight: 'bold' }}>
                          {equipo.totalAsistentes} {equipo.totalAsistentes === 1 ? 'Asistente' : 'Asistentes'}
                        </span>
                      </div>
                      {equipo.descripcion && (
                        <p style={{ margin: '6px 0 0', fontSize: '13px', color: '#94A3B8' }}>{equipo.descripcion}</p>
                      )}
                    </div>

                    <div style={{ display: 'flex', gap: '10px' }}>
                      <button
                        onClick={() => setSelectedEquipoId(equipo.id)}
                        style={{
                          backgroundColor: selectedEquipoId === equipo.id ? '#38BDF8' : 'rgba(255,255,255,0.08)',
                          color: selectedEquipoId === equipo.id ? 'black' : 'white',
                          border: 'none',
                          padding: '6px 12px',
                          borderRadius: '6px',
                          fontSize: '12px',
                          fontWeight: 'bold',
                          cursor: 'pointer'
                        }}
                      >
                        {selectedEquipoId === equipo.id ? 'Seleccionado' : 'Seleccionar'}
                      </button>
                      <button
                        onClick={() => handleDeleteEquipo(equipo.id, equipo.nombre)}
                        style={{
                          backgroundColor: 'rgba(239, 68, 68, 0.15)',
                          color: '#EF4444',
                          border: '1px solid rgba(239, 68, 68, 0.4)',
                          padding: '6px 10px',
                          borderRadius: '6px',
                          cursor: 'pointer'
                        }}
                        title="Eliminar equipo"
                      >
                        <FaTrash />
                      </button>
                    </div>
                  </div>

                  {/* Members Table */}
                  {(!equipo.integrantes || equipo.integrantes.length === 0) ? (
                    <div style={{ padding: '12px', backgroundColor: '#0F172A', borderRadius: '8px', color: '#64748B', fontSize: '13px' }}>
                      Sin integrantes aún. Usa el formulario de la izquierda para agregar Fiscales o Asistentes.
                    </div>
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                      {equipo.integrantes.map(member => {
                        const isFiscal = member.rol === 'FISCAL';
                        return (
                          <div
                            key={member.id}
                            style={{
                              display: 'flex',
                              justifyContent: 'space-between',
                              alignItems: 'center',
                              backgroundColor: '#0F172A',
                              padding: '10px 14px',
                              borderRadius: '8px',
                              borderLeft: `4px solid ${isFiscal ? '#38BDF8' : '#34D399'}`
                            }}
                          >
                            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                              <span style={{
                                backgroundColor: isFiscal ? 'rgba(56, 189, 248, 0.15)' : 'rgba(52, 211, 153, 0.15)',
                                color: isFiscal ? '#38BDF8' : '#34D399',
                                padding: '4px 8px',
                                borderRadius: '6px',
                                fontSize: '11px',
                                fontWeight: 'bold',
                                minWidth: '85px',
                                textAlign: 'center'
                              }}>
                                {member.rol}
                              </span>
                              <div>
                                <div style={{ fontWeight: 'bold', fontSize: '14px' }}>{member.nombreCompleto}</div>
                                <div style={{ fontSize: '12px', color: '#94A3B8' }}>
                                  Usuario: <code style={{ color: '#FDE047' }}>{member.username}</code> · Clave: <code style={{ color: '#CBD5E1' }}>{member.password}</code>
                                </div>
                              </div>
                            </div>

                            <button
                              onClick={() => handleDeleteUsuario(member.id, member.username)}
                              style={{
                                background: 'transparent',
                                border: 'none',
                                color: '#EF4444',
                                cursor: 'pointer',
                                padding: '6px'
                              }}
                              title="Eliminar integrante"
                            >
                              <FaTrash />
                            </button>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              ))
            )}
          </div>

        </div>
      </div>
    </div>
  );
}
