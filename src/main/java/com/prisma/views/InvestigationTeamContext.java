package com.prisma.views;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

public final class InvestigationTeamContext {
    private static String membersRaw;

    private InvestigationTeamContext() {
    }

    public static boolean ensureConfigured(Stage owner) {
        if (isConfigured()) {
            return true;
        }

        TextInputDialog dialog = new TextInputDialog();
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle("Integrantes del equipo");
        dialog.setHeaderText("Ingresa los integrantes separados por coma");
        dialog.setContentText("Formato: nombre1,nombre2,nombre3");

        while (true) {
            String input = dialog.showAndWait().map(String::trim).orElse("");
            if (input.isBlank()) {
                return false;
            }
            if (isValidMembersInput(input)) {
                membersRaw = normalizeMembers(input);
                return true;
            }
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Formato inválido");
            alert.setHeaderText("No se pudo validar la lista de integrantes");
                alert.setContentText("""
                    Usa nombres separados por coma. Ejemplos válidos:
                    nombre1,nombre2,nombre3
                    nombre1, nombre2, nombre3
                    """);
            if (owner != null) {
                alert.initOwner(owner);
            }
            alert.showAndWait();
        }
    }

    public static boolean isConfigured() {
        return membersRaw != null && !membersRaw.isBlank();
    }

    public static String getMembersDisplay() {
        return isConfigured() ? membersRaw : "No registrado";
    }

    private static boolean isValidMembersInput(String input) {
        if (input == null) {
            return false;
        }
        String trimmed = input.trim();
        if (trimmed.isBlank()) {
            return false;
        }
        List<String> names = Arrays.stream(trimmed.split(","))
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .collect(Collectors.toList());

        if (names.size() < 2) {
            return false;
        }

        return !trimmed.contains(",,");
    }

    private static String normalizeMembers(String input) {
        return Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .collect(Collectors.joining(", "));
    }
}
