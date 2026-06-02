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

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public final class DistractionAlertManager {
    private static final String UI_FONT = "'Segoe UI'";

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

    /** Lanza de inmediato la alerta de correo (para pruebas desde login u otras pantallas). */
    public static void triggerTestAlertNow() {
        synchronized (LOCK) {
            if (ownerStage == null || exhausted || alertActive) {
                return;
            }
            launchNotificationStage();
            resetCycle();
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
            markExhausted();
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

        ImageView background = sharedBackground(stage);
        Rectangle overlay = darkOverlay(stage);

        HBox cardHeader = new HBox(12);
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        cardHeader.setPadding(new Insets(16, 22, 16, 22));
        cardHeader.setStyle(
                "-fx-background-color: rgba(220,38,38,0.15); " +
                "-fx-border-color: transparent transparent rgba(248,113,113,0.18) transparent; " +
                "-fx-border-width: 0 0 1 0;");

        StackPane headerIcon = makeIconBox(
                FontAwesomeSolid.ENVELOPE,
                22,
                "#fca5a5",
                "rgba(220,38,38,0.20)",
                "rgba(248,113,113,0.30)");

        Label headerTitle = new Label("NUEVO CORREO INSTITUCIONAL");
        headerTitle.setStyle(
                "-fx-text-fill: #fff7ed; " +
                "-fx-font-size: 15; " +
                "-fx-font-weight: bold; " +
                "-fx-font-family: " + UI_FONT + ";");

        Label headerSubtitle = new Label("Sistema ARKIVA · Despacho Fiscal");
        headerSubtitle.setStyle(
                "-fx-text-fill: rgba(255,255,255,0.40); " +
                "-fx-font-size: 12; " +
                "-fx-font-family: " + UI_FONT + ";");

        VBox headerText = new VBox(2, headerTitle, headerSubtitle);
        headerText.setAlignment(Pos.CENTER_LEFT);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Circle pulseDot = new Circle(4, Color.web("#ef4444"));
        FadeTransition dotFade = new FadeTransition(javafx.util.Duration.seconds(1.2), pulseDot);
        dotFade.setFromValue(1.0);
        dotFade.setToValue(0.20);
        dotFade.setAutoReverse(true);
        dotFade.setCycleCount(Timeline.INDEFINITE);

        ScaleTransition dotScale = new ScaleTransition(javafx.util.Duration.seconds(1.2), pulseDot);
        dotScale.setFromX(1.0);
        dotScale.setFromY(1.0);
        dotScale.setToX(1.6);
        dotScale.setToY(1.6);
        dotScale.setAutoReverse(true);
        dotScale.setCycleCount(Timeline.INDEFINITE);

        ParallelTransition pulse = new ParallelTransition(dotFade, dotScale);
        pulse.play();

        cardHeader.getChildren().addAll(headerIcon, headerText, headerSpacer, pulseDot);

        Label introLabel = new Label("Ha llegado un nuevo correo a su bandeja.");
        introLabel.setWrapText(true);
        introLabel.setAlignment(Pos.CENTER);
        introLabel.setMaxWidth(Double.MAX_VALUE);
        introLabel.setStyle(
                "-fx-text-fill: #f1f5f9; " +
                "-fx-font-size: 15; " +
                "-fx-font-family: " + UI_FONT + ";");

        Label questionLabel = new Label("¿Desea leerlo ahora?");
        questionLabel.setAlignment(Pos.CENTER);
        questionLabel.setMaxWidth(Double.MAX_VALUE);
        questionLabel.setStyle(
                "-fx-text-fill: #f8fafc; " +
                "-fx-font-size: 18; " +
                "-fx-font-weight: bold; " +
                "-fx-font-family: " + UI_FONT + ";");

        FontIcon penaltyClockIcon = new FontIcon(FontAwesomeSolid.CLOCK);
        penaltyClockIcon.setIconSize(16);
        penaltyClockIcon.setIconColor(Color.web("#f59e0b"));

        Label penaltyPrefix = new Label("Leer este correo descontará ");
        penaltyPrefix.setStyle("-fx-text-fill: #fcd34d; -fx-font-size: 13; -fx-font-family: " + UI_FONT + ";");

        Label penaltyValue = new Label("2 minutos");
        penaltyValue.setStyle(
                "-fx-text-fill: #fbbf24; " +
                "-fx-font-size: 13; " +
                "-fx-font-weight: bold; " +
                "-fx-font-family: " + UI_FONT + ";");

        Label penaltySuffix = new Label(" del tiempo disponible.");
        penaltySuffix.setStyle("-fx-text-fill: #fcd34d; -fx-font-size: 13; -fx-font-family: " + UI_FONT + ";");

        HBox penaltyRow = new HBox(8, penaltyClockIcon, penaltyPrefix, penaltyValue, penaltySuffix);
        penaltyRow.setAlignment(Pos.CENTER_LEFT);
        penaltyRow.setPadding(new Insets(10, 16, 10, 16));
        penaltyRow.setStyle(
                "-fx-background-color: rgba(245,158,11,0.10); " +
                "-fx-border-color: rgba(245,158,11,0.25); " +
                "-fx-border-radius: 10; " +
                "-fx-background-radius: 10; " +
                "-fx-border-width: 1;");

        HBox readBtn = makeActionBtn(FontAwesomeSolid.ENVELOPE_OPEN, "Leer correo", true);
        readBtn.setOnMouseClicked(event -> {
            markCurrentRecord("LEIDA", "");
            InvestigationClock.deduct(READ_PENALTY);
            closeWindow(notificationStage);
            notificationStage = null;
            launchReadingStage();
        });

        HBox skipBtn = makeActionBtn(FontAwesomeSolid.TIMES, "Dejar pasar", false);
        skipBtn.setOnMouseClicked(event -> {
            markCurrentRecord("IGNORADA", "");
            closeWindow(notificationStage);
            notificationStage = null;
            finishCurrentAlert();
        });

        HBox buttonsRow = new HBox(12, readBtn, skipBtn);
        buttonsRow.setAlignment(Pos.CENTER);

        VBox cardBody = new VBox(14, introLabel, questionLabel, penaltyRow, buttonsRow);
        cardBody.setAlignment(Pos.CENTER);
        cardBody.setPadding(new Insets(24, 28, 20, 28));

        Label cardFooter = new Label(
                "Este aviso desaparecerá si no toma una decisión en los próximos 60 segundos.");
        cardFooter.setWrapText(true);
        cardFooter.setAlignment(Pos.CENTER);
        cardFooter.setMaxWidth(Double.MAX_VALUE);
        cardFooter.setPadding(new Insets(12, 22, 12, 22));
        cardFooter.setStyle(
                "-fx-text-fill: rgba(255,255,255,0.28); " +
                "-fx-font-size: 11; " +
                "-fx-font-family: " + UI_FONT + "; " +
                "-fx-background-color: rgba(255,255,255,0.02); " +
                "-fx-border-color: rgba(255,255,255,0.06) transparent transparent transparent; " +
                "-fx-border-width: 1 0 0 0;");

        VBox notifCard = new VBox(cardHeader, cardBody, cardFooter);
        notifCard.setSpacing(0);
        notifCard.setFillWidth(true);
        notifCard.setMaxWidth(560);
        notifCard.setPrefWidth(560);
        notifCard.setMaxHeight(Region.USE_PREF_SIZE);
        notifCard.setPrefHeight(Region.USE_COMPUTED_SIZE);
        notifCard.setStyle(
                "-fx-background-color: rgba(8,5,2,0.90); " +
                "-fx-background-radius: 20; " +
                "-fx-border-radius: 20; " +
                "-fx-border-color: rgba(248,113,113,0.30); " +
                "-fx-border-width: 1.5;");
        VBox.setVgrow(cardHeader, Priority.NEVER);
        VBox.setVgrow(cardBody, Priority.NEVER);
        VBox.setVgrow(cardFooter, Priority.NEVER);

        StackPane cardHost = new StackPane(notifCard);
        cardHost.setPickOnBounds(false);
        cardHost.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(notifCard, Pos.CENTER);

        root.getChildren().addAll(background, overlay, cardHost);
        StackPane.setAlignment(cardHost, Pos.CENTER);

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

        ImageView background = sharedBackground(stage);
        Rectangle overlay = darkOverlay(stage);

        HBox penaltyChip = makePill(
                FontAwesomeSolid.CLOCK,
                "−2 min descontados",
                "rgba(239,68,68,0.12)",
                "rgba(239,68,68,0.25)",
                "#fca5a5");

        Region topSpacerLeft = new Region();
        HBox.setHgrow(topSpacerLeft, Priority.ALWAYS);

        Label topTitle = new Label("HAS RECIBIDO UN NUEVO CORREO");
        topTitle.setStyle(
                "-fx-text-fill: #fff7ed; " +
                "-fx-font-size: 15; " +
                "-fx-font-weight: bold; " +
                "-fx-font-family: " + UI_FONT + ";");

        Region topSpacerRight = new Region();
        HBox.setHgrow(topSpacerRight, Priority.ALWAYS);

        FontIcon hourglassIcon = new FontIcon(FontAwesomeSolid.HOURGLASS_HALF);
        hourglassIcon.setIconSize(14);
        hourglassIcon.setIconColor(Color.web("#f59e0b"));

        Label countdownPrefix = new Label("Cierra en: ");
        countdownPrefix.setStyle("-fx-text-fill: #fcd34d; -fx-font-size: 12; -fx-font-family: " + UI_FONT + ";");

        countdownLabel = new Label();
        countdownLabel.setStyle(
                "-fx-text-fill: #fcd34d; " +
                "-fx-font-size: 13; " +
                "-fx-font-weight: bold; " +
                "-fx-font-family: " + UI_FONT + ";");
        updateCountdownLabel();

        HBox countdownBadge = new HBox(7, hourglassIcon, countdownPrefix, countdownLabel);
        countdownBadge.setAlignment(Pos.CENTER);
        countdownBadge.setPadding(new Insets(6, 14, 6, 14));
        countdownBadge.setStyle(
                "-fx-background-color: rgba(245,158,11,0.12); " +
                "-fx-border-color: rgba(245,158,11,0.28); " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; " +
                "-fx-border-width: 1;");

        HBox readingTopBar = new HBox(12, penaltyChip, topSpacerLeft, topTitle, topSpacerRight, countdownBadge);
        readingTopBar.setAlignment(Pos.CENTER_LEFT);
        readingTopBar.setPadding(new Insets(12, 20, 12, 20));
        readingTopBar.setStyle(
                "-fx-background-color: rgba(8,5,2,0.88); " +
                "-fx-border-color: transparent transparent rgba(245,158,11,0.18) transparent; " +
                "-fx-border-width: 0 0 1 0;");

        readingImageView = new ImageView(loadImageFromPath(currentImagePath));
        readingImageView.setPreserveRatio(true);
        readingImageView.setSmooth(true);
        readingImageView.setCache(true);
        readingBaseFitWidth = 1100.0;
        readingBaseFitHeight = 720.0;
        readingZoom = 1.0;
        applyReadingZoom();

        StackPane imageHolder = new StackPane(readingImageView);
        imageHolder.setAlignment(Pos.CENTER);
        imageHolder.setMinSize(0, 0);

        readingImageScroll = new ScrollPane(imageHolder);
        readingImageScroll.setFitToWidth(false);
        readingImageScroll.setFitToHeight(false);
        readingImageScroll.setPannable(false);
        readingImageScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        readingImageScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        readingImageScroll.setStyle(
                "-fx-background-color: rgba(255,255,255,0.04); " +
                "-fx-background-radius: 12; " +
                "-fx-border-radius: 12; " +
                "-fx-border-color: rgba(255,255,255,0.10); " +
                "-fx-border-width: 1;");
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

        VBox imageScrollArea = new VBox(readingImageScroll);
        imageScrollArea.setPadding(new Insets(12, 20, 8, 20));
        imageScrollArea.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(readingImageScroll, Priority.ALWAYS);

        Label responseTitle = new Label("RESPUESTA");
        responseTitle.setStyle(
                "-fx-text-fill: rgba(255,255,255,0.40); " +
                "-fx-font-size: 11; " +
                "-fx-font-weight: bold; " +
                "-fx-font-family: " + UI_FONT + ";");

        responseArea = new TextArea();
        responseArea.setPromptText("Escribe aquí la respuesta del correo...");
        responseArea.setWrapText(true);
        responseArea.setPrefRowCount(3);
        responseArea.setStyle(
                "-fx-control-inner-background: #f8fafc; " +
                "-fx-background-color: #f8fafc; " +
                "-fx-border-color: rgba(255,255,255,0.14); " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; " +
                "-fx-border-width: 1; " +
                "-fx-text-fill: #000000; " +
                "-fx-highlight-text-fill: #000000; " +
                "-fx-font-size: 13; " +
                "-fx-prompt-text-fill: #64748b; " +
                "-fx-font-family: " + UI_FONT + ";");

        HBox saveBtn = makeActionBtn(FontAwesomeSolid.SAVE, "Guardar respuesta", true);
        saveBtn.setOnMouseClicked(event -> {
            String text = responseArea.getText() == null ? "" : responseArea.getText().trim();
            if (text.isBlank()) {
                return;
            }
            markCurrentRecord("RESPONDIDA", text);
            closeWindow(readingStage);
            readingStage = null;
            finishCurrentAlert();
        });

        HBox postponeBtn = makeActionBtn(FontAwesomeSolid.CLOCK, "Posponer respuesta", false);
        postponeBtn.setOnMouseClicked(event -> {
            String text = responseArea.getText() == null ? "" : responseArea.getText().trim();
            markCurrentRecord(text.isBlank() ? "LEIDA_SIN_RESPUESTA" : "POSPUESTA", text);
            closeWindow(readingStage);
            readingStage = null;
            finishCurrentAlert();
        });

        HBox actionsRow = new HBox(10, saveBtn, postponeBtn);
        actionsRow.setAlignment(Pos.CENTER_LEFT);

        VBox responseBar = new VBox(10, responseTitle, responseArea, actionsRow);
        responseBar.setPadding(new Insets(12, 20, 14, 20));
        responseBar.setStyle(
                "-fx-background-color: rgba(8,5,2,0.88); " +
                "-fx-border-color: rgba(255,255,255,0.07) transparent transparent transparent; " +
                "-fx-border-width: 1 0 0 0;");

        BorderPane mainShell = new BorderPane();
        mainShell.setStyle("-fx-background-color: transparent;");
        mainShell.setTop(readingTopBar);
        mainShell.setCenter(imageScrollArea);
        mainShell.setBottom(responseBar);

        StackPane root = new StackPane(background, overlay, mainShell);

        stage.setScene(new Scene(root, 1280, 820));
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
                String responseText = responseArea == null ? "" : responseArea.getText().trim();
                markCurrentRecord(responseText.isBlank() ? "AUTO_CERRADA" : "RESPONDIDA", responseText);
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
            if (!exhausted) {
                resetCycle();
            }
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
        return 100;
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

        if (AVAILABLE_IMAGES.isEmpty()) {
            markExhausted();
        }
    }

    private static void markExhausted() {
        exhausted = true;
        stopTimelineOnly();
        nextEvaluationAt = java.time.Instant.MAX;
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
        countdownLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }

    private static ImageView sharedBackground(Stage stage) {
        ImageView background = new ImageView(loadImageFromResource("/styles/assets/correo-electronico.jpeg"));
        background.setPreserveRatio(false);
        background.fitWidthProperty().bind(stage.widthProperty());
        background.fitHeightProperty().bind(stage.heightProperty());
        background.setOpacity(0.45);
        background.setMouseTransparent(true);
        return background;
    }

    private static Rectangle darkOverlay(Stage stage) {
        Rectangle overlay = new Rectangle();
        overlay.setFill(Color.rgb(8, 5, 2, 0.58));
        overlay.widthProperty().bind(stage.widthProperty());
        overlay.heightProperty().bind(stage.heightProperty());
        overlay.setMouseTransparent(true);
        return overlay;
    }

    private static StackPane makeIconBox(
            FontAwesomeSolid icon,
            int size,
            String color,
            String bgRgba,
            String borderRgba) {
        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(size);
        fontIcon.setIconColor(Color.web(color));

        StackPane box = new StackPane(fontIcon);
        box.setMinSize(44, 44);
        box.setPrefSize(44, 44);
        box.setMaxSize(44, 44);
        box.setAlignment(Pos.CENTER);
        box.setStyle(
                "-fx-background-color: " + bgRgba + "; " +
                "-fx-background-radius: 10; " +
                "-fx-border-radius: 10; " +
                "-fx-border-color: " + borderRgba + "; " +
                "-fx-border-width: 1;");
        return box;
    }

    private static HBox makePill(
            FontAwesomeSolid icon,
            String text,
            String bgRgba,
            String borderRgba,
            String textColor) {
        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(13);
        fontIcon.setIconColor(Color.web(textColor));

        Label label = new Label(text);
        label.setStyle(
                "-fx-text-fill: " + textColor + "; " +
                "-fx-font-size: 12; " +
                "-fx-font-weight: bold; " +
                "-fx-font-family: " + UI_FONT + ";");

        HBox pill = new HBox(6, fontIcon, label);
        pill.setAlignment(Pos.CENTER);
        pill.setPadding(new Insets(5, 12, 5, 12));
        pill.setStyle(
                "-fx-background-color: " + bgRgba + "; " +
                "-fx-border-color: " + borderRgba + "; " +
                "-fx-border-radius: 6; " +
                "-fx-background-radius: 6; " +
                "-fx-border-width: 1;");
        return pill;
    }

    private static HBox makeActionBtn(FontAwesomeSolid icon, String label, boolean primary) {
        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(16);
        fontIcon.setIconColor(Color.web(primary ? "#0c0804" : "#e2e8f0"));

        Label textLabel = new Label(label);
        textLabel.setStyle(
                "-fx-text-fill: " + (primary ? "#0c0804" : "#e2e8f0") + "; " +
                "-fx-font-size: 14; " +
                "-fx-font-weight: bold; " +
                "-fx-font-family: " + UI_FONT + ";");

        HBox button = new HBox(8, fontIcon, textLabel);
        button.setAlignment(Pos.CENTER);
        button.setPadding(new Insets(13, 16, 13, 16));
        button.setMinWidth(200);
        button.setCursor(Cursor.HAND);
        button.setStyle(
                "-fx-background-color: " + (primary ? "#e09d10" : "rgba(255,255,255,0.08)") + "; " +
                "-fx-background-radius: 10; " +
                "-fx-border-radius: 10; " +
                (primary ? "" : "-fx-border-color: rgba(255,255,255,0.18); -fx-border-width: 1;"));

        button.setOnMouseEntered(event -> {
            ScaleTransition grow = new ScaleTransition(javafx.util.Duration.millis(130), button);
            grow.setToX(1.04);
            grow.setToY(1.04);
            grow.playFromStart();
        });
        button.setOnMouseExited(event -> {
            ScaleTransition shrink = new ScaleTransition(javafx.util.Duration.millis(130), button);
            shrink.setToX(1.0);
            shrink.setToY(1.0);
            shrink.playFromStart();
        });

        return button;
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