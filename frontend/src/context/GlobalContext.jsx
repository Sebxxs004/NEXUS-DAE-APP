import React, { createContext, useContext, useState, useEffect } from 'react';
import axios from 'axios';

const GlobalContext = createContext();

export function GlobalProvider({ children }) {
  const [casos, setCasos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Usuario autenticado y su equipo ({ id, nombreCompleto, username, rol, equipo })
  const [currentUser, setCurrentUser] = useState(() => {
    try {
      const saved = localStorage.getItem('nexus_current_user');
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });
  
  // Array of groups: { id, name, cases: [], type, justification }
  const [groups, setGroups] = useState([]);

  // Registro de alertas de interrupción atendidas/ignoradas en la sesión
  const [alertRecords, setAlertRecords] = useState([]);

  const addAlertRecord = (record) => {
    setAlertRecords(prev => {
      if (record.id) {
        const existingIdx = prev.findIndex(r => r.id === record.id);
        if (existingIdx !== -1) {
          const updated = [...prev];
          updated[existingIdx] = {
            ...updated[existingIdx],
            ...record,
            timestamp: new Date().toLocaleString()
          };
          return updated;
        }
      }
      return [...prev, {
        ...record,
        timestamp: new Date().toLocaleString()
      }];
    });
  };

  useEffect(() => {
    const fetchCasos = async () => {
      setLoading(true);
      try {
        const rolParam = currentUser?.rol === 'ASISTENTE' ? 'ASISTENTE' : 'FISCAL';
        const response = await axios.get(`http://localhost:8080/api/casos?rol=${rolParam}`);
        const uniqueData = Array.from(new Map(response.data.map(item => [item.nombre, item])).values());
        setCasos(uniqueData);
        setError(null);
      } catch (err) {
        console.error("Error al cargar casos:", err);
        setError("No se pudo conectar con el servidor.");
        
        // Mock data to show the layout even if backend fails
        setCasos(Array.from({ length: 20 }, (_, i) => ({
          nombre: `1100160000992023${i}000`,
          radicado: `1100160000992023${i}000`,
          delitos: [i % 2 === 0 ? 'hurto calificado' : 'extorsion'],
          descripcion: 'Descripción mock'
        })));
      } finally {
        setLoading(false);
      }
    };

    fetchCasos();
  }, [currentUser?.rol]);

  const loginUser = (userData) => {
    setCurrentUser(userData);
    localStorage.setItem('nexus_current_user', JSON.stringify(userData));
    if (userData?.equipo?.integrantes?.length > 0) {
      const names = userData.equipo.integrantes.map(i => `${i.nombreCompleto} (${i.rol})`).join(', ');
      localStorage.setItem('nexus_user', names);
    } else if (userData?.nombreCompleto) {
      localStorage.setItem('nexus_user', `${userData.nombreCompleto} (${userData.rol})`);
    }
  };

  const logoutUser = () => {
    setCurrentUser(null);
    localStorage.removeItem('nexus_current_user');
    localStorage.removeItem('nexus_user');
  };

  const addGroup = (newGroup) => {
    setGroups(prev => [...prev, newGroup]);
  };

  const addCasesToGroup = (groupId, casesToAdd) => {
    setGroups(prev => prev.map(g => {
      if (g.id === groupId) {
        return { ...g, cases: [...new Set([...g.cases, ...casesToAdd])] };
      }
      return g;
    }));
  };

  const getCaseGroup = (casoNombre) => {
    return groups.find(g => g.cases.includes(casoNombre));
  };

  const updateGroupDecision = (groupId, decisionData) => {
    setGroups(prev => prev.map(g => g.id === groupId ? { ...g, decision: decisionData } : g));
  };

  return (
    <GlobalContext.Provider value={{
      casos, loading, error,
      currentUser, loginUser, logoutUser,
      groups, addGroup, addCasesToGroup, getCaseGroup, updateGroupDecision,
      alertRecords, addAlertRecord
    }}>
      {children}
    </GlobalContext.Provider>
  );
}

export function useGlobal() {
  return useContext(GlobalContext);
}
