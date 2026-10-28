package com.prisma.services;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class TeamTimerService {

    // Duración oficial de la simulación para equipos mixtos: 2 horas (120 min = 7,200,000 ms)
    public static final long DURACION_TOTAL_MS = 2 * 60 * 60 * 1000L;

    public static class TeamTimerState {
        private final String equipoId;
        private long startedAt;
        private final long durationMs;
        private long accumulatedPenaltyMs;
        private Map<String, Object> lastPenalty;

        public TeamTimerState(String equipoId) {
            this.equipoId = equipoId;
            this.startedAt = System.currentTimeMillis();
            this.durationMs = DURACION_TOTAL_MS;
            this.accumulatedPenaltyMs = 0L;
            this.lastPenalty = null;
        }

        public synchronized long getTimeLeftMs() {
            long now = System.currentTimeMillis();
            long elapsed = now - startedAt;
            long remaining = durationMs - elapsed - accumulatedPenaltyMs;
            return Math.max(0, remaining);
        }

        public synchronized void applyPenalty(int minutes, String reason, String byUser) {
            long penaltyMs = minutes * 60 * 1000L;
            this.accumulatedPenaltyMs += penaltyMs;

            Map<String, Object> pen = new ConcurrentHashMap<>();
            pen.put("id", UUID.randomUUID().toString());
            pen.put("minutes", minutes);
            pen.put("reason", reason != null ? reason : "Penalización de tiempo");
            pen.put("byUser", byUser != null ? byUser : "Usuario");
            pen.put("timestamp", System.currentTimeMillis());
            this.lastPenalty = pen;
        }

        public synchronized void reset() {
            this.startedAt = System.currentTimeMillis();
            this.accumulatedPenaltyMs = 0L;
            this.lastPenalty = null;
        }

        public String getEquipoId() {
            return equipoId;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public synchronized long getAccumulatedPenaltyMs() {
            return accumulatedPenaltyMs;
        }

        public synchronized Map<String, Object> getLastPenalty() {
            return lastPenalty;
        }
    }

    private final ConcurrentHashMap<String, TeamTimerState> timers = new ConcurrentHashMap<>();

    public TeamTimerState getOrCreateTimer(String equipoId) {
        String key = (equipoId == null || equipoId.trim().isEmpty()) ? "general" : equipoId.trim();
        return timers.computeIfAbsent(key, TeamTimerState::new);
    }

    public TeamTimerState applyPenalty(String equipoId, int minutes, String reason, String byUser) {
        TeamTimerState timer = getOrCreateTimer(equipoId);
        timer.applyPenalty(minutes, reason, byUser);
        return timer;
    }

    public TeamTimerState resetTimer(String equipoId) {
        TeamTimerState timer = getOrCreateTimer(equipoId);
        timer.reset();
        return timer;
    }
}
