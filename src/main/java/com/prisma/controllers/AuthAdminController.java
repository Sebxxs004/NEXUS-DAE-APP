package com.prisma.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prisma.data.UsuarioEquipoRepository;
import com.prisma.models.Equipo;
import com.prisma.models.RolUsuario;
import com.prisma.models.Usuario;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthAdminController {

    private final UsuarioEquipoRepository repository;

    public AuthAdminController(UsuarioEquipoRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        Optional<Usuario> userOpt = repository.authenticate(username, password);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Usuario o contraseña incorrectos."));
        }

        Usuario user = userOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("nombreCompleto", user.getNombreCompleto());
        response.put("username", user.getUsername());
        response.put("rol", user.getRol().name());

        if (user.getEquipoId() != null) {
            Optional<Equipo> equipoOpt = repository.getEquipoById(user.getEquipoId());
            if (equipoOpt.isPresent()) {
                response.put("equipo", buildEquipoDto(equipoOpt.get()));
            }
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/equipos")
    public List<Map<String, Object>> getAllEquipos() {
        return repository.getEquipos().stream()
                .map(this::buildEquipoDto)
                .collect(Collectors.toList());
    }

    @PostMapping("/admin/equipos")
    public ResponseEntity<?> createEquipo(@RequestBody Map<String, String> payload) {
        String nombre = payload.get("nombre");
        String descripcion = payload.getOrDefault("descripcion", "");

        if (nombre == null || nombre.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "El nombre del equipo es obligatorio."));
        }

        Equipo equipo = repository.createEquipo(nombre, descripcion);
        return ResponseEntity.status(HttpStatus.CREATED).body(buildEquipoDto(equipo));
    }

    @DeleteMapping("/admin/equipos/{equipoId}")
    public ResponseEntity<?> deleteEquipo(@PathVariable String equipoId) {
        boolean deleted = repository.deleteEquipo(equipoId);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("message", "Equipo eliminado correctamente."));
    }

    @PostMapping("/admin/equipos/{equipoId}/usuarios")
    public ResponseEntity<?> addUsuarioToEquipo(
            @PathVariable String equipoId,
            @RequestBody Map<String, String> payload
    ) {
        Optional<Equipo> equipoOpt = repository.getEquipoById(equipoId);
        if (equipoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "El equipo especificado no existe."));
        }

        String nombreCompleto = payload.get("nombreCompleto");
        String username = payload.get("username");
        String password = payload.get("password");
        String rolStr = payload.get("rol");

        if (nombreCompleto == null || nombreCompleto.trim().isEmpty()
                || username == null || username.trim().isEmpty()
                || password == null || password.trim().isEmpty()
                || rolStr == null || rolStr.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Todos los campos del usuario son obligatorios."));
        }

        if (repository.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "El nombre de usuario '" + username + "' ya está en uso."));
        }

        RolUsuario rol;
        try {
            rol = RolUsuario.valueOf(rolStr.trim().toUpperCase());
            if (rol == RolUsuario.ADMIN) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Solo se pueden asignar roles FISCAL o ASISTENTE dentro de un equipo."));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Rol inválido. Use FISCAL o ASISTENTE."));
        }

        Usuario nuevo = repository.createUsuario(nombreCompleto, username, password, rol, equipoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(buildUsuarioDto(nuevo));
    }

    @DeleteMapping("/admin/usuarios/{usuarioId}")
    public ResponseEntity<?> deleteUsuario(@PathVariable String usuarioId) {
        boolean deleted = repository.deleteUsuario(usuarioId);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("message", "Usuario eliminado correctamente."));
    }

    private Map<String, Object> buildEquipoDto(Equipo equipo) {
        List<Map<String, Object>> integrantes = repository.getUsuariosByEquipo(equipo.getId())
                .stream()
                .map(this::buildUsuarioDto)
                .collect(Collectors.toList());

        long countFiscales = integrantes.stream()
                .filter(u -> "FISCAL".equals(u.get("rol")))
                .count();
        long countAsistentes = integrantes.stream()
                .filter(u -> "ASISTENTE".equals(u.get("rol")))
                .count();

        Map<String, Object> map = new HashMap<>();
        map.put("id", equipo.getId());
        map.put("nombre", equipo.getNombre());
        map.put("descripcion", equipo.getDescripcion());
        map.put("integrantes", integrantes);
        map.put("totalFiscales", countFiscales);
        map.put("totalAsistentes", countAsistentes);
        return map;
    }

    private Map<String, Object> buildUsuarioDto(Usuario u) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", u.getId());
        map.put("nombreCompleto", u.getNombreCompleto());
        map.put("username", u.getUsername());
        map.put("password", u.getPassword());
        map.put("rol", u.getRol().name());
        map.put("equipoId", u.getEquipoId());
        return map;
    }
}
