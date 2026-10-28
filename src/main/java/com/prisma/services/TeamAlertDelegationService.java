package com.prisma.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class TeamAlertDelegationService {

    public static class DelegatedAlert {
        private String id;
        private String equipoId;
        private String titulo;
        private String subtitulo;
        private String dialogo;
        private String delito;
        private String radicado;
        private String casoNombre;
        private String delegadoPor;
        private List<String> opciones;
        private String estado; // PENDIENTE | REALIZADA | IGNORADA
        private String opcionSeleccionada;
        private String justificacion;
        private String resueltoPor;
        private long timestamp;
        private long fechaResolucion;

        public DelegatedAlert() {
            this.id = UUID.randomUUID().toString();
            this.estado = "PENDIENTE";
            this.timestamp = System.currentTimeMillis();
            this.opciones = new ArrayList<>();
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getEquipoId() { return equipoId; }
        public void setEquipoId(String equipoId) { this.equipoId = equipoId; }

        public String getTitulo() { return titulo; }
        public void setTitulo(String titulo) { this.titulo = titulo; }

        public String getSubtitulo() { return subtitulo; }
        public void setSubtitulo(String subtitulo) { this.subtitulo = subtitulo; }

        public String getDialogo() { return dialogo; }
        public void setDialogo(String dialogo) { this.dialogo = dialogo; }

        public String getDelito() { return delito; }
        public void setDelito(String delito) { this.delito = delito; }

        public String getRadicado() { return radicado; }
        public void setRadicado(String radicado) { this.radicado = radicado; }

        public String getCasoNombre() { return casoNombre; }
        public void setCasoNombre(String casoNombre) { this.casoNombre = casoNombre; }

        public String getDelegadoPor() { return delegadoPor; }
        public void setDelegadoPor(String delegadoPor) { this.delegadoPor = delegadoPor; }

        public List<String> getOpciones() { return opciones; }
        public void setOpciones(List<String> opciones) { this.opciones = opciones; }

        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }

        public String getOpcionSeleccionada() { return opcionSeleccionada; }
        public void setOpcionSeleccionada(String opcionSeleccionada) { this.opcionSeleccionada = opcionSeleccionada; }

        public String getJustificacion() { return justificacion; }
        public void setJustificacion(String justificacion) { this.justificacion = justificacion; }

        public String getResueltoPor() { return resueltoPor; }
        public void setResueltoPor(String resueltoPor) { this.resueltoPor = resueltoPor; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public long getFechaResolucion() { return fechaResolucion; }
        public void setFechaResolucion(long fechaResolucion) { this.fechaResolucion = fechaResolucion; }
    }

    // Mapa de alertas delegadas por equipo (equipoId -> List<DelegatedAlert>)
    private final ConcurrentHashMap<String, List<DelegatedAlert>> teamAlerts = new ConcurrentHashMap<>();

    public DelegatedAlert delegarAlerta(String equipoId, Map<String, Object> payload) {
        String key = (equipoId == null || equipoId.trim().isEmpty()) ? "general" : equipoId.trim();

        DelegatedAlert alert = new DelegatedAlert();
        if (payload.get("id") != null) {
            alert.setId(payload.get("id").toString());
        }
        alert.setEquipoId(key);
        alert.setTitulo((String) payload.getOrDefault("titulo", "Actuación delegada por el Fiscal"));
        alert.setSubtitulo((String) payload.getOrDefault("subtitulo", "Traslado de trámite de despacho"));
        alert.setDialogo((String) payload.getOrDefault("dialogo", ""));
        alert.setDelito((String) payload.getOrDefault("delito", "GENERAL"));
        alert.setRadicado((String) payload.getOrDefault("radicado", "Sin radicado"));
        alert.setCasoNombre((String) payload.get("casoNombre"));
        alert.setDelegadoPor((String) payload.getOrDefault("delegadoPor", "Fiscal Delegado"));

        if (payload.get("opciones") instanceof List<?>) {
            List<?> rawList = (List<?>) payload.get("opciones");
            List<String> opts = new ArrayList<>();
            for (Object item : rawList) {
                if (item != null) opts.add(item.toString());
            }
            alert.setOpciones(opts);
        }

        // Si no trae opciones, agregamos opciones estándar operativas incluyendo Ignorar
        if (alert.getOpciones().isEmpty()) {
            alert.getOpciones().add("Ignorar o descartar esta solicitud / actividad");
            alert.getOpciones().add("Proyectar minuta de respuesta formal y remitir al Fiscal");
            alert.getOpciones().add("Verificar folios y solicitar ampliación técnica a Policía Judicial");
            alert.getOpciones().add("Archivar preventivamente la constancia en el expediente");
        }

        teamAlerts.computeIfAbsent(key, k -> new ArrayList<>());
        synchronized (teamAlerts.get(key)) {
            // Si ya existe una con el mismo ID, actualizarla; de lo contrario, agregar
            List<DelegatedAlert> list = teamAlerts.get(key);
            list.removeIf(a -> a.getId().equals(alert.getId()));
            list.add(0, alert); // Al inicio para que sea la primera
        }

        return alert;
    }

    public List<DelegatedAlert> getAlertasDelegadas(String equipoId) {
        String key = (equipoId == null || equipoId.trim().isEmpty()) ? "general" : equipoId.trim();
        List<DelegatedAlert> list = teamAlerts.getOrDefault(key, new ArrayList<>());
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }

    public DelegatedAlert resolverAlerta(String equipoId, Map<String, Object> payload) {
        String key = (equipoId == null || equipoId.trim().isEmpty()) ? "general" : equipoId.trim();
        String alertaId = (String) payload.get("alertaId");
        if (alertaId == null) return null;

        List<DelegatedAlert> list = teamAlerts.getOrDefault(key, new ArrayList<>());
        synchronized (list) {
            for (DelegatedAlert a : list) {
                if (a.getId().equals(alertaId)) {
                    a.setEstado((String) payload.getOrDefault("estado", "REALIZADA"));
                    a.setOpcionSeleccionada((String) payload.get("opcionSeleccionada"));
                    a.setJustificacion((String) payload.get("justificacion"));
                    a.setResueltoPor((String) payload.getOrDefault("resueltoPor", "Asistente de Fiscal"));
                    a.setFechaResolucion(System.currentTimeMillis());
                    return a;
                }
            }
        }
        return null;
    }
}
