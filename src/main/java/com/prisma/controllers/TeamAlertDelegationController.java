package com.prisma.controllers;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prisma.services.TeamAlertDelegationService;
import com.prisma.services.TeamAlertDelegationService.DelegatedAlert;

@RestController
@RequestMapping("/api/equipos")
@CrossOrigin(origins = "http://localhost:5173")
public class TeamAlertDelegationController {

    private final TeamAlertDelegationService alertService;

    public TeamAlertDelegationController(TeamAlertDelegationService alertService) {
        this.alertService = alertService;
    }

    @PostMapping("/{equipoId}/alertas/delegar")
    public ResponseEntity<DelegatedAlert> delegarAlerta(
            @PathVariable String equipoId,
            @RequestBody Map<String, Object> payload
    ) {
        DelegatedAlert alert = alertService.delegarAlerta(equipoId, payload);
        return ResponseEntity.ok(alert);
    }

    @GetMapping("/{equipoId}/alertas/delegadas")
    public ResponseEntity<List<DelegatedAlert>> getAlertasDelegadas(@PathVariable String equipoId) {
        List<DelegatedAlert> alerts = alertService.getAlertasDelegadas(equipoId);
        return ResponseEntity.ok(alerts);
    }

    @PostMapping("/{equipoId}/alertas/resolver")
    public ResponseEntity<?> resolverAlerta(
            @PathVariable String equipoId,
            @RequestBody Map<String, Object> payload
    ) {
        DelegatedAlert alert = alertService.resolverAlerta(equipoId, payload);
        if (alert == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(alert);
    }
}
