package com.prisma.controllers;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prisma.services.TeamTimerService;
import com.prisma.services.TeamTimerService.TeamTimerState;

@RestController
@RequestMapping("/api/equipos")
@CrossOrigin(origins = "http://localhost:5173")
public class TeamTimerController {

    private final TeamTimerService timerService;

    public TeamTimerController(TeamTimerService timerService) {
        this.timerService = timerService;
    }

    @GetMapping("/{equipoId}/timer")
    public ResponseEntity<Map<String, Object>> getTimer(@PathVariable String equipoId) {
        TeamTimerState state = timerService.getOrCreateTimer(equipoId);
        return ResponseEntity.ok(buildTimerResponse(state));
    }

    @PostMapping("/{equipoId}/timer/penalty")
    public ResponseEntity<Map<String, Object>> applyPenalty(
            @PathVariable String equipoId,
            @RequestBody Map<String, Object> payload
    ) {
        int minutes = 0;
        if (payload.get("minutes") instanceof Number) {
            minutes = ((Number) payload.get("minutes")).intValue();
        }
        String reason = (String) payload.getOrDefault("reason", "Penalización de tiempo");
        String byUser = (String) payload.getOrDefault("byUser", "Usuario");

        TeamTimerState state = timerService.applyPenalty(equipoId, minutes, reason, byUser);
        return ResponseEntity.ok(buildTimerResponse(state));
    }

    @PostMapping("/{equipoId}/timer/reset")
    public ResponseEntity<Map<String, Object>> resetTimer(@PathVariable String equipoId) {
        TeamTimerState state = timerService.resetTimer(equipoId);
        return ResponseEntity.ok(buildTimerResponse(state));
    }

    private Map<String, Object> buildTimerResponse(TeamTimerState state) {
        Map<String, Object> res = new HashMap<>();
        res.put("equipoId", state.getEquipoId());
        res.put("durationMs", state.getDurationMs());
        res.put("timeLeftMs", state.getTimeLeftMs());
        res.put("accumulatedPenaltyMs", state.getAccumulatedPenaltyMs());
        res.put("isFinished", state.getTimeLeftMs() <= 0);
        res.put("lastPenalty", state.getLastPenalty());
        res.put("serverTime", System.currentTimeMillis());
        return res;
    }
}
