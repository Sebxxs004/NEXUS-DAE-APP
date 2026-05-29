package com.prisma.views;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public final class DistractionAlertManager {
    private static final java.time.Duration CHECK_INTERVAL = java.time.Duration.ofMinutes(10);
    private static final java.time.Duration IMAGE_WINDOW_DURATION = java.time.Duration.ofMinutes(3);
    private static final java.time.Duration READ_PENALTY = java.time.Duration.ofMinutes(2);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Object LOCK = new Object();

    private static final List<AlertRecord> ALERT_RECORDS = new ArrayList<>();
    private static final Set<String> USED_IMAGE_KEYS = new HashSet<>();
    private static final List<Path> AVAILABLE_IMAGES = new ArrayList<>();

    private static Stage ownerStage;
    private static Timeline monitorTimeline;
    private static boolean monitoringStarted;
    private static boolean alertActive;
    private static boolean exhausted;
    private static int probabilityTier;
    private static java.time.Instant nextEvaluationAt = java.time.Instant.now().plus(CHECK_INTERVAL);
    private static AlertRecord currentRecord;
    private static Stage notificationStage;
    private static Stage readingStage;
    private static Timeline readingCountdownTimeline;
    private static int readingRemainingSeconds;
    private static Path currentImagePath;
    private static boolean responseModeVisible;
    private static TextArea responseArea;
    private static Label countdownLabel;
    private static ScrollPane readingImageScroll;
    private static ImageView readingImageView;
    private static double readingZoom = 1.0;
    private static double readingBaseFitWidth = 1100.0;
    private static double readingBaseFitHeight = 720.0;
    private static double readingDragAnchorX;
    private static double readingDragAnchorY;
    private static double readingDragStartHValue;
    private static double readingDragStartVValue;

    static {
        loadAvailableImages();
    }

    private DistractionAlertManager() {
    }

    public static void attach(Stage stage) {
        synchronized (LOCK) {
            ownerStage = stage;
            if (!monitoringStarted) {
                startMonitoring();
            }
        }
    }

    public static void stopMonitoring() {
        synchronized (LOCK) {
            if (monitorTimeline != null) {
                monitorTimeline.stop();
            }
            if (readingCountdownTimeline != null) {
                readingCountdownTimeline.stop();
            }
            closeWindows();
            monitoringStarted = false;
            alertActive = false;
            currentRecord = null;
        }
    }

    public static void forceNextAlertTest() {
        synchronized (LOCK) {
            if (exhausted) {
                return;
            }
            probabilityTier = 2;
            nextEvaluationAt = java.time.Instant.now().minusSeconds(1);
        }
    }

    public static List<AlertRecord> getAlertRecords() {
        synchronized (LOCK) {
            return Collections.unmodifiableList(new ArrayList<>(ALERT_RECORDS));
        }
    }

    public static String buildAlertsJson() {
        synchronized (LOCK) {
            StringBuilder json = new StringBuilder();
            json.append("[");
            for (int i = 0; i < ALERT_RECORDS.size(); i++) {
                AlertRecord record = ALERT_RECORDS.get(i);
                json.append("{")
                        .append("\"timestamp\":\"").append(escapeJson(record.getTimestamp())).append("\",")
                        .append("\"image\":\"").append(escapeJson(record.getImageName())).append("\",")
                        .append("\"status\":\"").append(escapeJson(record.getStatus())).append("\",")
                        .append("\"response\":\"").append(escapeJson(record.getResponseText())).append("\"")
                        .append("}");
                if (i < ALERT_RECORDS.size() - 1) {
                    json.append(",");
                }
            }
            json.append("]");
            return json.toString();
        }
    }

    private static void startMonitoring() {
        monitoringStarted = true;
        monitorTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> checkForAlert()));
        monitorTimeline.setCycleCount(Timeline.INDEFINITE);
        monitorTimeline.play();
    }

    private static void checkForAlert() {
        synchronized (LOCK) {
            if (ownerStage == null || exhausted || alertActive) {
                return;
            }

            if (java.time.Instant.now().isBefore(nextEvaluationAt)) {
                return;
            }

            if (AVAILABLE_IMAGES.isEmpty()) {
                exhausted = true;
                stopTimelineOnly();
                return;
            }

            int probability = currentProbability();
            boolean trigger = probability >= 100 || ThreadLocalRandom.current().nextInt(100) < probability;
            if (trigger) {
                launchNotificationStage();
                resetCycle();
                return;
            }

            advanceProbabilityTier();
            nextEvaluationAt = java.time.Instant.now().plus(CHECK_INTERVAL);
        }
    }

    private static void launchNotificationStage() {
        currentImagePath = selectNextImage();
        if (currentImagePath == null) {
            exhausted = true;
            stopTimelineOnly();
            return;
        }

        alertActive = true;
        currentRecord = new AlertRecord(currentImagePath.getFileName().toString());
        currentRecord.status = "NOTIFICADA";
        ALERT_RECORDS.add(currentRecord);

        PlatformRunner.run(() -> {
            closeWindow(notificationStage);
            notificationStage = buildNotificationStage();
            notificationStage.show();
            notificationStage.toFront();
        });
    }

    private static Stage buildNotificationStage() {
        Stage stage = createBaseStage();
        StackPane root = new StackPane();

        ImageView background = new ImageView(loadImageFromResource("/styles/assets/correo-electronico.jpeg"));
        background.setPreserveRatio(false);
        background.fitWidthProperty().bind(stage.widthProperty());
        background.fitHeightProperty().bind(stage.heightProperty());
        background.setOpacity(0.88);
        background.setMouseTransparent(true);

        VBox card = new VBox(18);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(28));
        card.setMaxWidth(700);
        card.setStyle("-fx-background-color: rgba(5, 10, 20, 0.88); -fx-background-radius: 24; -fx-border-radius: 24; -fx-border-width: 1.5; -fx-border-color: rgba(248, 113, 113, 0.45);");

        Label title = new Label("HAS RECIBIDO UN NUEVOO CORREO");
        title.setWrapText(true);
        title.setStyle("-fx-text-fill: #fff7ed; -fx-font-size: 28; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");

        Label message = new Label("¿Deseas leerlo ahora?");
        message.setStyle("-fx-text-fill: #f8e4c6; -fx-font-size: 18; -fx-font-family: 'Segoe UI';");

        Label penaltyNotice = new Label("Si decides leerlo, se te restará tiempo.");
        penaltyNotice.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 16; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");

        Button readButton = new Button("Leer");
        styleAlertButton(readButton, true);
        readButton.setOnAction(e -> {
            markCurrentRecord("LEIDA", "");
            InvestigationClock.deduct(READ_PENALTY);
            closeWindow(notificationStage);
            notificationStage = null;
            launchReadingStage();
        });

        Button skipButton = new Button("Dejar pasar");
        styleAlertButton(skipButton, false);
        skipButton.setOnAction(e -> {
            markCurrentRecord("IGNORADA", "");
            closeWindow(notificationStage);
            notificationStage = null;
            finishCurrentAlert();
        });

        HBox actions = new HBox(12, readButton, skipButton);
        actions.setAlignment(Pos.CENTER);

        card.getChildren().addAll(title, message, penaltyNotice, actions);
        root.getChildren().addAll(background, card);
        StackPane.setAlignment(card, Pos.CENTER);

        stage.setScene(new Scene(root, 1280, 820));
        stage.setMaximized(true);
        stage.setFullScreen(true);
        return stage;
    }

    private static void launchReadingStage() {
        PlatformRunner.run(() -> {
            closeWindow(readingStage);
            readingStage = buildReadingStage();
            readingStage.show();
            readingStage.toFront();
        });
    }

    private static Stage buildReadingStage() {
        Stage stage = createBaseStage();
        readingRemainingSeconds = (int) IMAGE_WINDOW_DURATION.getSeconds();
        responseModeVisible = false;

        ImageView background = new ImageView(loadImageFromResource("/styles/assets/correo-electronico.jpeg"));
        background.setPreserveRatio(false);
        background.fitWidthProperty().bind(stage.widthProperty());
        background.fitHeightProperty().bind(stage.heightProperty());
        background.setMouseTransparent(true);

        readingImageView = new ImageView(loadImageFromPath(currentImagePath));
        readingImageView.setPreserveRatio(true);
        readingImageView.setSmooth(true);
        readingImageView.setCache(true);
        readingBaseFitWidth = 1100.0;
        readingBaseFitHeight = 760.0;
        readingZoom = 1.0;
        applyReadingZoom();

        StackPane imageHolder = new StackPane(readingImageView);
        imageHolder.setAlignment(Pos.CENTER);
        imageHolder.setMinSize(0, 0);
        imageHolder.setStyle("-fx-background-color: rgba(255,255,255,0.02);");

        readingImageScroll = new ScrollPane(imageHolder);
        readingImageScroll.setFitToWidth(false);
        readingImageScroll.setFitToHeight(false);
        readingImageScroll.setPannable(false);
        readingImageScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        readingImageScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        readingImageScroll.getStyleClass().add("case-modal-scroll");
        readingImageScroll.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isControlDown()) {
                double factor = event.getDeltaY() > 0 ? 1.12 : 0.9;
                readingZoom = clamp(readingZoom * factor, 0.35, 4.5);
                applyReadingZoom();
                event.consume();
            }
        });
        readingImageScroll.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            readingDragAnchorX = event.getX();
            readingDragAnchorY = event.getY();
            readingDragStartHValue = readingImageScroll.getHvalue();
            readingDragStartVValue = readingImageScroll.getVvalue();
        });
        readingImageScroll.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            double viewportWidth = Math.max(1.0, readingImageScroll.getViewportBounds().getWidth());
            double viewportHeight = Math.max(1.0, readingImageScroll.getViewportBounds().getHeight());
            double hDelta = (readingDragAnchorX - event.getX()) / viewportWidth;
            double vDelta = (readingDragAnchorY - event.getY()) / viewportHeight;
            readingImageScroll.setHvalue(clamp(readingDragStartHValue + hDelta, readingImageScroll.getHmin(), readingImageScroll.getHmax()));
            readingImageScroll.setVvalue(clamp(readingDragStartVValue + vDelta, readingImageScroll.getVmin(), readingImageScroll.getVmax()));
            event.consume();
        });

        VBox responseBox = new VBox(10);
        responseBox.setVisible(false);
        responseBox.setManaged(false);
        responseBox.setMaxWidth(760);
        responseBox.setStyle("-fx-background-color: rgba(8, 12, 24, 0.84); -fx-background-radius: 18; -fx-border-radius: 18; -fx-border-color: rgba(248, 113, 113, 0.3);");
        responseBox.setPadding(new Insets(16));

        responseArea = new TextArea();
        responseArea.setPromptText("Escribe aquí la respuesta del correo...");
        responseArea.setWrapText(true);
        responseArea.setPrefRowCount(6);
        responseArea.getStyleClass().add("text-area");

        Button saveResponse = new Button("Guardar respuesta");
        styleAlertButton(saveResponse, true);
        saveResponse.setOnAction(e -> {
            String text = responseArea.getText() == null ? "" : responseArea.getText().trim();
            if (text.isBlank()) {
                return;
            }
            markCurrentRecord("RESPONDIDA", text);
            closeWindow(readingStage);
            readingStage = null;
            finishCurrentAlert();
        });

        responseBox.getChildren().addAll(new Label("Respuesta"), responseArea, saveResponse);

        Button answerButton = new Button("Responder");
        styleAlertButton(answerButton, true);
        answerButton.setOnAction(e -> {
            responseModeVisible = true;
            responseBox.setVisible(true);
            responseBox.setManaged(true);
        });

        Button postponeButton = new Button("Posponer respuesta");
        styleAlertButton(postponeButton, false);
        postponeButton.setOnAction(e -> {
            markCurrentRecord(responseModeVisible ? "POSPUESTA" : "LEIDA_SIN_RESPUESTA", responseArea == null ? "" : responseArea.getText());
            closeWindow(readingStage);
            readingStage = null;
            finishCurrentAlert();
        });

        HBox actions = new HBox(12, answerButton, postponeButton);
        actions.setAlignment(Pos.CENTER);

        countdownLabel = new Label();
        countdownLabel.setStyle("-fx-text-fill: #fff7ed; -fx-font-size: 18; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
        updateCountdownLabel();

        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMaxHeight(Double.MAX_VALUE);
        card.setStyle("-fx-background-color: rgba(12, 10, 8, 0.86); -fx-background-radius: 24; -fx-border-radius: 24; -fx-border-color: rgba(245, 158, 11, 0.38); -fx-border-width: 1.5;");

        Label title = new Label("HAS RECIBIDO UN NUEVO CORREO");
        title.setStyle("-fx-text-fill: #fff7ed; -fx-font-size: 30; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");

        Label subtitle = new Label("Esta ventana se cerrará en: ");
        subtitle.setStyle("-fx-text-fill: #f8e4c6; -fx-font-size: 18; -fx-font-family: 'Segoe UI';");

        Label warning = new Label("Leer este correo descuenta tiempo del sistema.");
        warning.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 16; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");

        VBox.setVgrow(readingImageScroll, Priority.ALWAYS);
        readingImageScroll.setMaxWidth(Double.MAX_VALUE);
        readingImageScroll.setMaxHeight(Double.MAX_VALUE);
        readingImageScroll.setPrefViewportWidth(1220);
        readingImageScroll.setPrefViewportHeight(720);
        readingImageScroll.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 18; -fx-border-radius: 18; -fx-border-color: rgba(255,255,255,0.12);");

        card.getChildren().addAll(title, subtitle, warning, countdownLabel, readingImageScroll, responseBox, actions);

        StackPane root = new StackPane(background, card);
        root.setPadding(new Insets(0));
        StackPane.setAlignment(card, Pos.CENTER);

        stage.setScene(new Scene(root));
        stage.setMaximized(true);
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");
        stage.setOnCloseRequest(event -> event.consume());

        if (readingCountdownTimeline != null) {
            readingCountdownTimeline.stop();
        }
        readingCountdownTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> tickReadingCountdown()));
        readingCountdownTimeline.setCycleCount(Timeline.INDEFINITE);
        readingCountdownTimeline.play();

        return stage;
    }

    private static void applyReadingZoom() {
        if (readingImageView == null) {
            return;
        }
        readingImageView.setFitWidth(readingBaseFitWidth * readingZoom);
        readingImageView.setFitHeight(readingBaseFitHeight * readingZoom);
    }

    private static void tickReadingCountdown() {
        synchronized (LOCK) {
            if (readingStage == null) {
                return;
            }
            readingRemainingSeconds = Math.max(0, readingRemainingSeconds - 1);
            updateCountdownLabel();
            if (readingRemainingSeconds <= 0) {
                markCurrentRecord(responseModeVisible && responseArea != null && !responseArea.getText().isBlank()
                        ? "RESPONDIDA"
                        : "AUTO_CERRADA",
                        responseArea == null ? "" : responseArea.getText());
                closeWindow(readingStage);
                readingStage = null;
                finishCurrentAlert();
            }
        }
    }

    private static void finishCurrentAlert() {
        synchronized (LOCK) {
            closeWindow(notificationStage);
            closeWindow(readingStage);
            notificationStage = null;
            readingStage = null;
            alertActive = false;
            currentImagePath = null;
            currentRecord = null;
            responseArea = null;
            countdownLabel = null;
            responseModeVisible = false;
            readingRemainingSeconds = 0;
            resetCycle();
        }
    }

    private static void resetCycle() {
        probabilityTier = 0;
        nextEvaluationAt = java.time.Instant.now().plus(CHECK_INTERVAL);
    }

    private static void markCurrentRecord(String status, String responseText) {
        synchronized (LOCK) {
            if (currentRecord == null) {
                return;
            }
            currentRecord.status = status == null ? "" : status;
            currentRecord.responseText = responseText == null ? "" : responseText.trim();
            currentRecord.updatedAt = LocalDateTime.now().format(FORMATTER);
        }
    }

    private static int currentProbability() {
        return switch (probabilityTier) {
            case 0 -> 25;
            case 1 -> 50;
            default -> 100;
        };
    }

    private static void advanceProbabilityTier() {
        probabilityTier = Math.min(2, probabilityTier + 1);
    }

    private static void closeWindows() {
        closeWindow(notificationStage);
        closeWindow(readingStage);
        notificationStage = null;
        readingStage = null;
    }

    private static void stopTimelineOnly() {
        if (monitorTimeline != null) {
            monitorTimeline.stop();
        }
    }

    private static void closeWindow(Stage stage) {
        if (stage == null) {
            return;
        }
        try {
            stage.close();
        } catch (Exception ignored) {
        }
    }

    private static Stage createBaseStage() {
        Stage stage = new Stage(StageStyle.UNDECORATED);
        if (ownerStage != null) {
            stage.initOwner(ownerStage);
        }
        stage.initModality(Modality.NONE);
        stage.setAlwaysOnTop(true);
        stage.setFullScreenExitHint("");
        stage.setOnCloseRequest(event -> event.consume());
        return stage;
    }

    private static Path selectNextImage() {
        synchronized (LOCK) {
            List<Path> available = new ArrayList<>();
            for (Path path : AVAILABLE_IMAGES) {
                String key = path.toAbsolutePath().normalize().toString();
                if (!USED_IMAGE_KEYS.contains(key)) {
                    available.add(path);
                }
            }

            if (available.isEmpty()) {
                exhausted = true;
                stopTimelineOnly();
                return null;
            }

            List<Path> filtered = available.stream()
                    .filter(path -> !isBackgroundLikeImage(path))
                    .toList();
            if (!filtered.isEmpty()) {
                available = new ArrayList<>(filtered);
            }

            Path selected = available.get(ThreadLocalRandom.current().nextInt(available.size()));
            USED_IMAGE_KEYS.add(selected.toAbsolutePath().normalize().toString());

            if (USED_IMAGE_KEYS.size() >= AVAILABLE_IMAGES.size()) {
                exhausted = true;
                stopTimelineOnly();
            }

            return selected;
        }
    }

    private static Image loadImageFromResource(String resourcePath) {
        return new Image(Objects.requireNonNull(DistractionAlertManager.class.getResourceAsStream(resourcePath)));
    }

    private static Image loadImageFromPath(Path path) {
        return new Image(path.toUri().toString());
    }

    private static void loadAvailableImages() {
        AVAILABLE_IMAGES.clear();

        Path alertasDir = Paths.get(System.getProperty("user.dir"), "alertas");
        if (Files.exists(alertasDir) && Files.isDirectory(alertasDir)) {
            try {
                Files.list(alertasDir)
                        .filter(DistractionAlertManager::isSupportedImage)
                        .sorted()
                        .forEach(AVAILABLE_IMAGES::add);
            } catch (IOException ignored) {
            }
        }
    }

    private static boolean isBackgroundLikeImage(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        String value = path.getFileName().toString().toLowerCase();
        return value.contains("correo-electronico");
    }

    private static boolean isSupportedImage(Path path) {
        if (path == null) {
            return false;
        }
        String value = path.getFileName().toString().toLowerCase();
        return value.endsWith(".png") || value.endsWith(".jpg") || value.endsWith(".jpeg") || value.endsWith(".gif") || value.endsWith(".webp");
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void styleAlertButton(Button button, boolean primary) {
        String baseStyle = primary
                ? "-fx-background-color: linear-gradient(to bottom, #fbbf24, #d97706); -fx-text-fill: #1f1303; -fx-font-size: 15; -fx-font-weight: bold; -fx-background-radius: 18; -fx-border-radius: 18; -fx-padding: 12 22 12 22; -fx-cursor: hand;"
                : "-fx-background-color: linear-gradient(to bottom, rgba(255,255,255,0.18), rgba(255,255,255,0.08)); -fx-text-fill: #fff7ed; -fx-font-size: 15; -fx-font-weight: bold; -fx-background-radius: 18; -fx-border-radius: 18; -fx-border-color: rgba(255,255,255,0.18); -fx-padding: 12 22 12 22; -fx-cursor: hand;";
        button.setStyle(baseStyle);
        button.setMinWidth(175);
        button.setEffect(new DropShadow(18, primary ? Color.rgb(217, 119, 6, 0.55) : Color.rgb(15, 23, 42, 0.45)));

        button.setOnMouseEntered(event -> {
            button.setEffect(new DropShadow(26, primary ? Color.rgb(251, 191, 36, 0.78) : Color.rgb(56, 189, 248, 0.55)));
            ScaleTransition grow = new ScaleTransition(javafx.util.Duration.millis(130), button);
            grow.setToX(1.07);
            grow.setToY(1.07);
            grow.playFromStart();
        });
        button.setOnMouseExited(event -> {
            button.setEffect(new DropShadow(18, primary ? Color.rgb(217, 119, 6, 0.55) : Color.rgb(15, 23, 42, 0.45)));
            ScaleTransition shrink = new ScaleTransition(javafx.util.Duration.millis(130), button);
            shrink.setToX(1.0);
            shrink.setToY(1.0);
            shrink.playFromStart();
        });

        if (primary) {
            Timeline pulse = new Timeline(
                    new KeyFrame(javafx.util.Duration.ZERO, event -> button.setEffect(new DropShadow(18, Color.rgb(217, 119, 6, 0.55)))),
                    new KeyFrame(javafx.util.Duration.seconds(1.1), event -> button.setEffect(new DropShadow(30, Color.rgb(251, 191, 36, 0.8)))),
                    new KeyFrame(javafx.util.Duration.seconds(2.2), event -> button.setEffect(new DropShadow(18, Color.rgb(217, 119, 6, 0.55))))
            );
            pulse.setCycleCount(Timeline.INDEFINITE);
            pulse.play();
        }
    }

    private static void updateCountdownLabel() {
        if (countdownLabel == null) {
            return;
        }
        int minutes = Math.max(0, readingRemainingSeconds / 60);
        int seconds = Math.max(0, readingRemainingSeconds % 60);
        countdownLabel.setText(String.format("Esta ventana se cerrará en: %02d:%02d", minutes, seconds));
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    public static final class AlertRecord {
        private final String imageName;
        private final String createdAt;
        private String updatedAt;
        private String status;
        private String responseText = "";

        private AlertRecord(String imageName) {
            this.imageName = imageName;
            this.createdAt = LocalDateTime.now().format(FORMATTER);
            this.updatedAt = this.createdAt;
        }

        public String getImageName() {
            return imageName;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public String getStatus() {
            return status;
        }

        public String getResponseText() {
            return responseText;
        }

        public String getTimestamp() {
            return createdAt;
        }
    }

    private static final class PlatformRunner {
        private static void run(Runnable runnable) {
            javafx.application.Platform.runLater(runnable);
        }
    }
}