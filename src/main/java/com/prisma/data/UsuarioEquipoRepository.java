package com.prisma.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.prisma.models.Equipo;
import com.prisma.models.RolUsuario;
import com.prisma.models.Usuario;

@Repository
public class UsuarioEquipoRepository {

    private static final String STORAGE_FILE = "nexus-users-teams.json";
    private final ObjectMapper objectMapper;
    private StorageData data;

    public static class StorageData {
        private List<Equipo> equipos = new ArrayList<>();
        private List<Usuario> usuarios = new ArrayList<>();

        public List<Equipo> getEquipos() {
            return equipos;
        }

        public void setEquipos(List<Equipo> equipos) {
            this.equipos = equipos;
        }

        public List<Usuario> getUsuarios() {
            return usuarios;
        }

        public void setUsuarios(List<Usuario> usuarios) {
            this.usuarios = usuarios;
        }
    }

    public UsuarioEquipoRepository() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        loadOrSeedData();
    }

    private synchronized void loadOrSeedData() {
        File file = new File(STORAGE_FILE);
        if (file.exists()) {
            try {
                this.data = objectMapper.readValue(file, StorageData.class);
                if (this.data.getEquipos() == null) {
                    this.data.setEquipos(new ArrayList<>());
                }
                if (this.data.getUsuarios() == null) {
                    this.data.setUsuarios(new ArrayList<>());
                }
                ensureAdminExists();
                return;
            } catch (IOException e) {
                System.err.println("No se pudo leer " + STORAGE_FILE + ", inicializando datos por defecto: " + e.getMessage());
            }
        }

        this.data = new StorageData();
        seedDefaultData();
        saveData();
    }

    private void ensureAdminExists() {
        boolean hasAdmin = this.data.getUsuarios().stream()
                .anyMatch(u -> u.getRol() == RolUsuario.ADMIN);
        if (!hasAdmin) {
            this.data.getUsuarios().add(new Usuario(
                    UUID.randomUUID().toString(),
                    "Administrador NEXUS",
                    "admin",
                    "admin123",
                    RolUsuario.ADMIN,
                    null
            ));
            saveData();
        }
    }

    private void seedDefaultData() {
        // Usuario Admin por defecto
        this.data.getUsuarios().add(new Usuario(
                UUID.randomUUID().toString(),
                "Administrador NEXUS",
                "admin",
                "admin123",
                RolUsuario.ADMIN,
                null
        ));

        // Equipo de demostración mixto (Fiscales + Asistentes)
        String equipoId = UUID.randomUUID().toString();
        Equipo equipoDemo = new Equipo(
                equipoId,
                "Despacho 01 - Equipo Mixto",
                "Equipo conformado por fiscales delegados y asistentes judiciales"
        );
        this.data.getEquipos().add(equipoDemo);

        this.data.getUsuarios().add(new Usuario(
                UUID.randomUUID().toString(),
                "Dra. Laura Gómez",
                "fiscal1",
                "1234",
                RolUsuario.FISCAL,
                equipoId
        ));

        this.data.getUsuarios().add(new Usuario(
                UUID.randomUUID().toString(),
                "Dr. Andrés Restrepo",
                "fiscal2",
                "1234",
                RolUsuario.FISCAL,
                equipoId
        ));

        this.data.getUsuarios().add(new Usuario(
                UUID.randomUUID().toString(),
                "Camilo Vargas",
                "asistente1",
                "1234",
                RolUsuario.ASISTENTE,
                equipoId
        ));
    }

    private synchronized void saveData() {
        try {
            objectMapper.writeValue(new File(STORAGE_FILE), this.data);
        } catch (IOException e) {
            System.err.println("Error guardando " + STORAGE_FILE + ": " + e.getMessage());
        }
    }

    public synchronized Optional<Usuario> authenticate(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }
        return this.data.getUsuarios().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username.trim()) && u.getPassword().equals(password))
                .findFirst();
    }

    public synchronized List<Equipo> getEquipos() {
        return new ArrayList<>(this.data.getEquipos());
    }

    public synchronized Optional<Equipo> getEquipoById(String equipoId) {
        if (equipoId == null) {
            return Optional.empty();
        }
        return this.data.getEquipos().stream()
                .filter(e -> e.getId().equals(equipoId))
                .findFirst();
    }

    public synchronized List<Usuario> getUsuariosByEquipo(String equipoId) {
        if (equipoId == null) {
            return List.of();
        }
        return this.data.getUsuarios().stream()
                .filter(u -> equipoId.equals(u.getEquipoId()))
                .collect(Collectors.toList());
    }

    public synchronized Equipo createEquipo(String nombre, String descripcion) {
        Equipo nuevo = new Equipo(
                UUID.randomUUID().toString(),
                nombre.trim(),
                descripcion != null ? descripcion.trim() : ""
        );
        this.data.getEquipos().add(nuevo);
        saveData();
        return nuevo;
    }

    public synchronized boolean deleteEquipo(String equipoId) {
        boolean removed = this.data.getEquipos().removeIf(e -> e.getId().equals(equipoId));
        if (removed) {
            this.data.getUsuarios().removeIf(u -> equipoId.equals(u.getEquipoId()));
            saveData();
        }
        return removed;
    }

    public synchronized boolean existsByUsername(String username) {
        return this.data.getUsuarios().stream()
                .anyMatch(u -> u.getUsername().equalsIgnoreCase(username.trim()));
    }

    public synchronized Usuario createUsuario(String nombreCompleto, String username, String password, RolUsuario rol, String equipoId) {
        Usuario nuevo = new Usuario(
                UUID.randomUUID().toString(),
                nombreCompleto.trim(),
                username.trim(),
                password,
                rol,
                equipoId
        );
        this.data.getUsuarios().add(nuevo);
        saveData();
        return nuevo;
    }

    public synchronized boolean deleteUsuario(String usuarioId) {
        boolean removed = this.data.getUsuarios().removeIf(u -> u.getId().equals(usuarioId) && u.getRol() != RolUsuario.ADMIN);
        if (removed) {
            saveData();
        }
        return removed;
    }
}
