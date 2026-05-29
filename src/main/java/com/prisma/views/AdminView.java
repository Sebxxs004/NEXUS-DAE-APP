package com.prisma.views;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import com.prisma.data.CasoRepository;
import com.prisma.models.Caso;

import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AdminView {
    private final Stage stage;
    private BorderPane view;
    private VBox casosCardsContainer;
    private TextArea detalleArea;

    public AdminView(Stage stage) {
        this.stage = stage;
        view = new BorderPane();
        view.getStyleClass().add("app-shell");
        view.setPadding(new Insets(26));

        VBox mainCard = new VBox(18);
        mainCard.getStyleClass().add("panel-card");
        mainCard.setPadding(new Insets(24));

        Label title = new Label("Panel Admin");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("Consulta los casos cargados, genera el PDF simulado y cierra la sesión cuando termines.");
        subtitle.getStyleClass().add("app-subtitle");
        subtitle.setWrapText(true);

        Button btnSimulatePdf = new Button("Generar PDF simulado");
        btnSimulatePdf.getStyleClass().add("secondary-button");

        Button btnLogout = new Button("Cerrar sesión");
        btnLogout.getStyleClass().add("danger-button");

        HBox actions = new HBox(12, btnSimulatePdf, btnLogout);

        Label listTitle = new Label("Casos cargados");
        listTitle.getStyleClass().add("section-title");

        casosCardsContainer = new VBox(12);
        casosCardsContainer.getStyleClass().add("case-cards-container");
        casosCardsContainer.setPadding(new Insets(4));

        ScrollPane casosScrollPane = new ScrollPane(casosCardsContainer);
        casosScrollPane.getStyleClass().add("group-scroll");
        casosScrollPane.setFitToWidth(true);
        casosScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        casosScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        casosScrollPane.setPrefViewportHeight(280);

        VBox detallePanel = new VBox(14);
        detallePanel.getStyleClass().add("panel-card");
        detallePanel.setPadding(new Insets(16));
        detallePanel.setStyle("-fx-border-width: 1; -fx-border-color: rgba(56, 189, 248, 0.3);");

        Label detalleTitle = new Label("Detalles del caso");
        detalleTitle.getStyleClass().add("section-title");

        detalleArea = new TextArea();
        detalleArea.getStyleClass().add("text-area");
        detalleArea.setEditable(false);
        detalleArea.setWrapText(true);
        detalleArea.setPrefRowCount(12);
        VBox.setVgrow(detalleArea, Priority.ALWAYS);

        detallePanel.getChildren().addAll(detalleTitle, detalleArea);

        refreshList();
        CasoRepository.getCasos().addListener((ListChangeListener<Caso>) change -> refreshList());
        refreshCaseCards();

        btnSimulatePdf.setOnAction(e -> {
            try {
                Path out = generateSimulatedPdf();
                showAlert(Alert.AlertType.INFORMATION, "PDF simulado generado en: " + out.toString());
            } catch (IOException ex) {
                showAlert(Alert.AlertType.ERROR, "Error generando PDF: " + ex.getMessage());
            }
        });

        btnLogout.setOnAction(e -> {
            DistractionAlertManager.stopMonitoring();
            LoginView loginView = new LoginView(stage);
            javafx.scene.Scene scene = new javafx.scene.Scene(loginView.getView(), 980, 680);
            com.prisma.ui.Theme.apply(scene);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setFullScreen(true);
        });

        mainCard.getChildren().addAll(title, subtitle, actions);

        VBox rightCard = new VBox(14, listTitle, casosScrollPane);
        rightCard.getStyleClass().add("sidebar-card");
        rightCard.setPadding(new Insets(22));
        rightCard.setPrefWidth(420);
        VBox.setVgrow(rightCard, Priority.SOMETIMES);

        VBox rightContainer = new VBox(14, rightCard, detallePanel);
        VBox.setVgrow(detallePanel, Priority.ALWAYS);

        view.setCenter(mainCard);
        view.setRight(rightContainer);
    }

    private void refreshList() {
        refreshCaseCards();
    }

    private void refreshCaseCards() {
        casosCardsContainer.getChildren().setAll(CasoRepository.getCasos().stream()
                .map(this::buildCaseCard)
                .toList());
    }

    private Path generateSimulatedPdf() throws IOException {
        Path downloads = Paths.get(System.getProperty("user.home"), "Downloads");
        Files.createDirectories(downloads);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path output = downloads.resolve("prisma-simulado-" + timestamp + ".pdf");

        // Try to locate latest investigation snapshot JSON in Documents/PRISMA
        Path snapshotsDir = Paths.get(System.getProperty("user.home"), "Documents", "PRISMA");
        Path latestSnapshot = null;
        if (Files.exists(snapshotsDir) && Files.isDirectory(snapshotsDir)) {
            latestSnapshot = Files.list(snapshotsDir)
                    .filter(p -> p.getFileName().toString().startsWith("investigacion-") && p.getFileName().toString().endsWith(".json"))
                    .max(Comparator.comparingLong(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toMillis();
                        } catch (IOException ex) {
                            return 0L;
                        }
                    })).orElse(null);
        }

        // Parsed data holders
        List<String[]> parsedConnections = new ArrayList<>(); // {from,to,detail}
        List<String[]> parsedGroups = new ArrayList<>(); // {name,reason,membersCSV}
        List<String[]> parsedIsolated = new ArrayList<>(); // {name,reason}
        List<String[]> parsedAlerts = new ArrayList<>(); // {timestamp,image,status,response}

        if (latestSnapshot != null) {
            String json = Files.readString(latestSnapshot);

            // connections
            Pattern pConn = Pattern.compile("\\\"from\\\"\\s*:\\s*\\\"(.*?)\\\".*?\\\"to\\\"\\s*:\\s*\\\"(.*?)\\\".*?\\\"detail\\\"\\s*:\\s*\\\"(.*?)\\\"", Pattern.DOTALL);
            Matcher mConn = pConn.matcher(json);
            while (mConn.find()) {
                parsedConnections.add(new String[]{unescapeJson(mConn.group(1)), unescapeJson(mConn.group(2)), unescapeJson(mConn.group(3))});
            }

            // groups
            Pattern pGroup = Pattern.compile("\\\"name\\\"\\s*:\\s*\\\"(.*?)\\\".*?\\\"reason\\\"\\s*:\\s*\\\"(.*?)\\\".*?\\\"members\\\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
            Matcher mGroup = pGroup.matcher(json);
            while (mGroup.find()) {
                String name = unescapeJson(mGroup.group(1));
                String reason = unescapeJson(mGroup.group(2));
                String membersRaw = mGroup.group(3);
                // extract quoted members
                Pattern pMember = Pattern.compile("\"(.*?)\"");
                Matcher mMember = pMember.matcher(membersRaw);
                List<String> members = new ArrayList<>();
                while (mMember.find()) {
                    members.add(unescapeJson(mMember.group(1)));
                }
                String membersCsv = String.join(", ", members);
                parsedGroups.add(new String[]{name, reason, membersCsv});
            }

            // isolated nodes
            Pattern pIso = Pattern.compile("\\\"isolatedNodes\\\"\\s*:\\s*\\[([\\s\\S]*?)\\]", Pattern.DOTALL);
            Matcher mIsoBlock = pIso.matcher(json);
            if (mIsoBlock.find()) {
                String block = mIsoBlock.group(1);
                Pattern pIsoItem = Pattern.compile("\\\"name\\\"\\s*:\\s*\\\"(.*?)\\\".*?\\\"reason\\\"\\s*:\\s*\\\"(.*?)\\\"", Pattern.DOTALL);
                Matcher mIsoItem = pIsoItem.matcher(block);
                while (mIsoItem.find()) {
                    parsedIsolated.add(new String[]{unescapeJson(mIsoItem.group(1)), unescapeJson(mIsoItem.group(2))});
                }
            }

            // alerts
            Pattern pAlerts = Pattern.compile("\\\"alerts\\\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
            Matcher mAlerts = pAlerts.matcher(json);
            if (mAlerts.find()) {
                String alertsBlock = mAlerts.group(1);
                Pattern pAlertItem = Pattern.compile("\\{\\s*\\\"timestamp\\\"\\s*:\\s*\\\"(.*?)\\\"\\s*,\\s*\\\"image\\\"\\s*:\\s*\\\"(.*?)\\\"\\s*,\\s*\\\"status\\\"\\s*:\\s*\\\"(.*?)\\\"\\s*,\\s*\\\"response\\\"\\s*:\\s*\\\"(.*?)\\\"\\s*\\}", Pattern.DOTALL);
                Matcher mAlertItem = pAlertItem.matcher(alertsBlock);
                while (mAlertItem.find()) {
                    parsedAlerts.add(new String[]{
                        unescapeJson(mAlertItem.group(1)),
                        unescapeJson(mAlertItem.group(2)),
                        unescapeJson(mAlertItem.group(3)),
                        unescapeJson(mAlertItem.group(4))
                    });
                }
            }
        }

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            float margin = 50;
            float y = page.getMediaBox().getHeight() - margin;

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                y = writePdfLine(content, page, y, 16, "PRISMA DAE - Reporte de Investigación (Simulado)", true);
                y = writePdfLine(content, page, y, 11, "Generado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), false);
                y = writePdfLine(content, page, y, 11, "Fuente: " + (latestSnapshot == null ? "Catalogo de casos" : latestSnapshot.getFileName().toString()), false);
                y = writePdfLine(content, page, y, 11, "", false);

                y = writePdfLine(content, page, y, 13, "Conexiones registradas", true);
                if (parsedConnections.isEmpty()) {
                    y = writePdfWrappedLine(content, page, y, 11, "- No hay conexiones registradas (último snapshot no disponible o vacío).");
                } else {
                    int idx = 1;
                    for (String[] conn : parsedConnections) {
                        String line = idx + ". " + conn[0] + " <-> " + conn[1] + " | " + conn[2];
                        y = writePdfWrappedLine(content, page, y, 11, line);
                        idx++;
                    }
                }

                y = writePdfLine(content, page, y, 11, "", false);
                y = writePdfLine(content, page, y, 13, "Grupos y justificaciones", true);
                if (parsedGroups.isEmpty()) {
                    y = writePdfWrappedLine(content, page, y, 11, "- No hay grupos detectados (último snapshot no disponible o vacío).\n\n");
                } else {
                    int idx = 1;
                    for (String[] grp : parsedGroups) {
                        y = writePdfWrappedLine(content, page, y, 11, idx + ". " + grp[0] + " (miembros: " + grp[2] + ")");
                        y = writePdfWrappedLine(content, page, y, 11, "   Justificación: " + grp[1]);
                        idx++;
                    }
                }

                y = writePdfLine(content, page, y, 11, "", false);
                y = writePdfLine(content, page, y, 13, "Casos aislados", true);
                if (parsedIsolated.isEmpty()) {
                    y = writePdfWrappedLine(content, page, y, 11, "- No hay casos aislados (último snapshot no disponible o vacío).\n\n");
                } else {
                    int idx = 1;
                    for (String[] iso : parsedIsolated) {
                        y = writePdfWrappedLine(content, page, y, 11, idx + ". " + iso[0] + " - Justificación: " + iso[1]);
                        idx++;
                    }
                }

                y = writePdfLine(content, page, y, 11, "", false);
                y = writePdfLine(content, page, y, 13, "Alertas distractivas", true);
                if (parsedAlerts.isEmpty()) {
                    y = writePdfWrappedLine(content, page, y, 11, "- No hay alertas distractivas registradas.");
                } else {
                    int idx = 1;
                    for (String[] alert : parsedAlerts) {
                        y = writePdfWrappedLine(content, page, y, 11, idx + ". " + alert[0] + " | Imagen: " + alert[1] + " | Estado: " + alert[2]);
                        if (!alert[3].isBlank()) {
                            y = writePdfWrappedLine(content, page, y, 11, "   Respuesta: " + alert[3]);
                        }
                        idx++;
                    }
                }
            }

            document.save(output.toFile());
        }

        return output;
    }

    // Reuse small helpers from PlayerView: simple PDF helpers
    private float writePdfLine(PDPageContentStream content, PDPage page, float y, int fontSize, String text, boolean bold)
            throws IOException {
        float left = 52;
        if (y < 60) {
            return y;
        }
        content.beginText();
        content.setFont(bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, fontSize);
        content.newLineAtOffset(left, y);
        content.showText(text == null ? "" : text);
        content.endText();
        return y - (fontSize + 6);
    }

    private float writePdfWrappedLine(PDPageContentStream content, PDPage page, float y, int fontSize, String text)
            throws IOException {
        String value = text == null ? "" : text;
        int maxChars = 100;
        int cursor = 0;
        while (cursor < value.length()) {
            int end = Math.min(value.length(), cursor + maxChars);
            String chunk = value.substring(cursor, end);
            y = writePdfLine(content, page, y, fontSize, chunk, false);
            cursor = end;
        }
        if (value.isEmpty()) {
            y = writePdfLine(content, page, y, fontSize, "", false);
        }
        return y;
    }

    private String unescapeJson(String s) {
        if (s == null) return "";
        return s.replaceAll("\\\\", "\\")
                .replaceAll("\\\"", "\"")
                .replaceAll("\\n", "\n")
                .replaceAll("\\r", "");
    }

    private VBox buildCaseCard(Caso caso) {
        Label cardTitle = new Label(caso.getNombre());
        cardTitle.getStyleClass().add("section-title");

        Label cardMeta = new Label(caso.getLugar() + " · " + caso.getFechaHechosFormateada());
        cardMeta.getStyleClass().add("app-subtitle");

        Label cardSummary = new Label(caso.getDescripcion());
        cardSummary.getStyleClass().add("muted-text");
        cardSummary.setWrapText(true);

        Button detailButton = new Button("Ver detalles");
        detailButton.getStyleClass().add("secondary-button");

        VBox card = new VBox(10, cardTitle, cardMeta, cardSummary, detailButton);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(14));
        return card;
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("PRISMA DAE");
        alert.showAndWait();
    }

    public BorderPane getView() {
        return view;
    }
}
