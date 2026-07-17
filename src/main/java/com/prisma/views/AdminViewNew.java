package com.prisma.views;

import com.prisma.data.CasoRepository;
import com.prisma.ui.Theme;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

public class AdminViewNew {
    private static final String FONT = "'Segoe UI'";

    private final StackPane view;
    private final Stage stage;
    private final Label timerLabel;
    private final HBox timerBadge;
    private final Timeline timerTimeline;
    private MediaView mediaView;
    private BorderPane shellContainer;

    public static boolean onboardingMode = false;
    private StackPane onboardingOverlay;
    private Pane onboardingFloatLayer;
    private VBox onboardingDialog;
    private Label onboardingMessageLabel;
    private Rectangle onboardingDimLayer;
    private Rectangle onboardingSpotlight;
    private Rectangle onboardingFocusRing;
    private HBox rightButton;

    public AdminViewNew(Stage stage) {
        this.stage = stage;

        view = new StackPane();
        view.setPrefSize(1500, 900);
        view.setMinSize(1500, 900);
        view.setStyle("-fx-background-color: #0a0e1c; -fx-font-family: " + FONT + ";");
        DistractionAlertManager.attach(stage);

        Image backgroundImage = new Image(getClass().getResourceAsStream("/styles/assets/fondo-admin.jpeg"));
        ImageView backgroundView = new ImageView(backgroundImage);
        backgroundView.setPreserveRatio(false);
        backgroundView.fitWidthProperty().bind(view.widthProperty());
        backgroundView.fitHeightProperty().bind(view.heightProperty());
        backgroundView.setMouseTransparent(true);
        ColorAdjust colorAdjust = new ColorAdjust();
        colorAdjust.setBrightness(-0.22);
        backgroundView.setEffect(colorAdjust);

        Rectangle radialOverlay = new Rectangle();
        radialOverlay.widthProperty().bind(view.widthProperty());
        radialOverlay.heightProperty().bind(view.heightProperty());
        radialOverlay.setMouseTransparent(true);
        radialOverlay.setFill(new RadialGradient(
            0, 0, 0.5, 0.5, 0.6, true, CycleMethod.NO_CYCLE,
            new Stop(0.30, Color.color(0, 0, 0, 0)),
            new Stop(1.00, Color.color(10.0 / 255.0, 14.0 / 255.0, 28.0 / 255.0, 0.55))
        ));

        Rectangle topGradient = new Rectangle();
        topGradient.widthProperty().bind(view.widthProperty());
        topGradient.setHeight(90);
        topGradient.setMouseTransparent(true);
        topGradient.setFill(new LinearGradient(
            0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.color(8.0 / 255.0, 12.0 / 255.0, 24.0 / 255.0, 0.85)),
            new Stop(1.0, Color.color(0, 0, 0, 0))
        ));
        StackPane.setAlignment(topGradient, Pos.TOP_LEFT);

        Rectangle bottomGradient = new Rectangle();
        bottomGradient.widthProperty().bind(view.widthProperty());
        bottomGradient.setHeight(200);
        bottomGradient.setMouseTransparent(true);
        bottomGradient.setFill(new LinearGradient(
            0, 1, 0, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.color(8.0 / 255.0, 12.0 / 255.0, 24.0 / 255.0, 0.93)),
            new Stop(1.0, Color.color(0, 0, 0, 0))
        ));
        StackPane.setAlignment(bottomGradient, Pos.BOTTOM_LEFT);

        timerLabel = new Label("01:59:53");
        timerLabel.setStyle(
            "-fx-text-fill: #ffffff; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 16; " +
            "-fx-font-family: 'Consolas', 'Segoe UI', monospace;"
        );

        Circle timerDot = new Circle(3.5, Color.web("#ffffff"));
        FadeTransition dotPulse = new FadeTransition(Duration.seconds(1), timerDot);
        dotPulse.setFromValue(1.0);
        dotPulse.setToValue(0.3);
        dotPulse.setAutoReverse(true);
        dotPulse.setCycleCount(Animation.INDEFINITE);
        dotPulse.play();

        Label timerPrefix = new Label("TIEMPO");
        timerPrefix.setStyle(
            "-fx-text-fill: rgba(255,255,255,0.75); " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 11; " +
            "-fx-letter-spacing: 1px; " +
            "-fx-font-family: " + FONT + ";"
        );

        timerBadge = new HBox(8, timerDot, timerPrefix, timerLabel);
        timerBadge.setAlignment(Pos.CENTER);
        timerBadge.setPadding(new Insets(8, 18, 8, 18));
        applyNormalTimerStyle();

        HBox topBar = buildTopBar();
        VBox centerArea = buildCenterArea();
        HBox actionBar = buildActionBar();

        BorderPane shell = new BorderPane();
        shell.setStyle("-fx-background-color: transparent;");
        shell.setTop(topBar);
        shell.setCenter(centerArea);
        shell.setBottom(actionBar);
        this.shellContainer = shell;

        mediaView = new MediaView();
        mediaView.setPreserveRatio(false);
        mediaView.fitWidthProperty().bind(view.widthProperty());
        mediaView.fitHeightProperty().bind(view.heightProperty());
        mediaView.setVisible(false);
        mediaView.setMouseTransparent(true);

        view.getChildren().addAll(
            backgroundView,
            mediaView,
            radialOverlay,
            topGradient,
            bottomGradient,
            shell
        );

        boolean isFirstTime = !java.nio.file.Files.exists(
            java.nio.file.Path.of(System.getProperty("user.home"), "Documents", "NEXUS", "active-session-snapshot.json")
        );

        if (isFirstTime) {
            // Cinema spotlight effect pointing to the bottom-left action button
            Rectangle spotlight = new Rectangle();
            spotlight.widthProperty().bind(view.widthProperty());
            spotlight.heightProperty().bind(view.heightProperty());
            spotlight.setMouseTransparent(true); // Allow clicks to pass through to the button
            spotlight.setFill(new RadialGradient(
                0, 0, 0.16, 0.88, 0.28, true, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.TRANSPARENT),
                new Stop(0.42, Color.color(3.0 / 255.0, 7.0 / 255.0, 18.0 / 255.0, 0.38)),
                new Stop(0.80, Color.color(3.0 / 255.0, 7.0 / 255.0, 18.0 / 255.0, 0.88)),
                new Stop(1.00, Color.color(3.0 / 255.0, 7.0 / 255.0, 18.0 / 255.0, 0.94))
            ));
            
            // Subtle breathing animation for the spotlight to feel alive
            FadeTransition spotlightPulse = new FadeTransition(Duration.seconds(2.5), spotlight);
            spotlightPulse.setFromValue(0.92);
            spotlightPulse.setToValue(1.0);
            spotlightPulse.setAutoReverse(true);
            spotlightPulse.setCycleCount(Animation.INDEFINITE);
            spotlightPulse.play();
            
            view.getChildren().add(spotlight);
        }

        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> refreshTimer()));
        timerTimeline.setCycleCount(Animation.INDEFINITE);
        timerTimeline.play();
        refreshTimer();

        if (onboardingMode) {
            onboardingOverlay = buildOnboardingOverlay();
            view.getChildren().add(onboardingOverlay);
            javafx.application.Platform.runLater(this::startOnboardingFlow);
        }
    }

    public StackPane getView() {
        return view;
    }

    public void applyTheme(Scene scene) {
        Theme.apply(scene);
    }

    private void goBackToLogin() {
        timerTimeline.stop();
        DistractionAlertManager.stopMonitoring();
        PlayerViewBrown.clearActiveInstance();
        Scene scene = new Scene(new LoginView(stage).getView(), 1500, 900);
        Theme.apply(scene);

                javafx.scene.Scene currentScene = stage.getScene();
        if (currentScene != null) {
            javafx.scene.Parent viewRoot = scene.getRoot();
            scene.setRoot(new javafx.scene.layout.Region()); // Detach from dummy scene
            currentScene.setRoot(viewRoot);
        } else {
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setFullScreen(true);
        }
}

    private void openCaseManagement() {
        CasesManagementBrownView casesView = new CasesManagementBrownView(stage);
        Scene scene = new Scene(casesView.getView(), 1500, 900);
        casesView.applyTheme(scene);

                javafx.scene.Scene currentScene = stage.getScene();
        if (currentScene != null) {
            javafx.scene.Parent viewRoot = scene.getRoot();
            scene.setRoot(new javafx.scene.layout.Region()); // Detach from dummy scene
            currentScene.setRoot(viewRoot);
        } else {
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setFullScreen(true);
        }
}

    private void openAnalyticsBoard() {
        PlayerViewBrown.clearActiveInstance();
        PlayerViewBrown playerView = PlayerViewBrown.getInstance(stage);
        javafx.scene.Parent view = playerView.getView();
        if (view.getScene() != null) {
            view.getScene().setRoot(new javafx.scene.layout.Pane());
        }
        Scene scene = new Scene(view, 1500, 900);
        playerView.applyTheme(scene);

                javafx.scene.Scene currentScene = stage.getScene();
        if (currentScene != null) {
            javafx.scene.Parent viewRoot = scene.getRoot();
            scene.setRoot(new javafx.scene.layout.Region()); // Detach from dummy scene
            currentScene.setRoot(viewRoot);
        } else {
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setFullScreen(true);
        }
if (PlayerViewBrown.onboardingMode) {
            playerView.startOnboardingFlowExternal();
        }
    }

    private void playTransitionAndGo(String videoFilename, Runnable action) {
        // Disable UI interaction
        view.setMouseTransparent(true);

        // Hide UI elements smoothly
        FadeTransition hideUi = new FadeTransition(Duration.millis(300), shellContainer);
        hideUi.setToValue(0.0);
        hideUi.play();

        try {
            String videoUrl = getClass().getResource("/styles/assets/videos/" + videoFilename).toExternalForm();
            Media media = new Media(videoUrl);
            MediaPlayer player = new MediaPlayer(media);
            mediaView.setMediaPlayer(player);
            mediaView.setVisible(true);
            
            player.setOnEndOfMedia(() -> {
                action.run();
                view.setMouseTransparent(false);
            });
            player.play();
        } catch (Exception e) {
            System.err.println("Error playing video transition: " + e.getMessage());
            action.run();
        }
    }

    private HBox buildTopBar() {
        FontIcon backIcon = new FontIcon(FontAwesomeSolid.ARROW_LEFT);
        backIcon.setIconSize(15);
        backIcon.setIconColor(Color.web("#e2e8f0"));

        Label backText = new Label("Volver al login");
        backText.setStyle(
            "-fx-text-fill: #e2e8f0; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 13; " +
            "-fx-font-family: " + FONT + ";"
        );

        HBox backButton = new HBox(7, backIcon, backText);
        backButton.setAlignment(Pos.CENTER_LEFT);
        backButton.setPadding(new Insets(7, 14, 7, 14));
        backButton.setStyle(
            "-fx-background-color: rgba(255,255,255,0.10); " +
            "-fx-background-radius: 8; " +
            "-fx-border-color: rgba(255,255,255,0.18); " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1; " +
            "-fx-cursor: hand;"
        );
        backButton.setOnMouseEntered(e -> backButton.setStyle(
            "-fx-background-color: rgba(255,255,255,0.17); " +
            "-fx-background-radius: 8; " +
            "-fx-border-color: rgba(255,255,255,0.18); " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1; " +
            "-fx-cursor: hand;"
        ));
        backButton.setOnMouseExited(e -> backButton.setStyle(
            "-fx-background-color: rgba(255,255,255,0.10); " +
            "-fx-background-radius: 8; " +
            "-fx-border-color: rgba(255,255,255,0.18); " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1; " +
            "-fx-cursor: hand;"
        ));
        backButton.setOnMouseClicked(e -> goBackToLogin());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(12, backButton, spacer, timerBadge);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(14, 20, 14, 20));
        topBar.setMinHeight(56);
        topBar.setPrefHeight(56);
        topBar.setMaxHeight(56);
        topBar.setStyle("-fx-background-color: transparent;");
        return topBar;
    }

    private VBox buildCenterArea() {
        Label welcomeTitle = new Label("Bienvenido a tu Despacho");
        welcomeTitle.setStyle(
            "-fx-text-fill: #f8fafc; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 28; " +
            "-fx-font-family: " + FONT + ";"
        );
        DropShadow welcomeShadow = new DropShadow();
        welcomeShadow.setRadius(14);
        welcomeShadow.setColor(Color.color(0, 0, 0, 0.70));
        welcomeTitle.setEffect(welcomeShadow);
        VBox.setMargin(welcomeTitle, new Insets(0, 0, 18, 0));

        Image logoImage = new Image(getClass().getResourceAsStream("/styles/assets/NEXUS-DAE.png"));
        ImageView logoView = new ImageView(logoImage);
        logoView.setFitWidth(160);
        logoView.setFitHeight(160);
        logoView.setPreserveRatio(true);
        DropShadow logoShadow = new DropShadow();
        logoShadow.setRadius(18);
        logoShadow.setColor(Color.color(1.0, 220.0 / 255.0, 80.0 / 255.0, 0.22));
        logoView.setEffect(logoShadow);

        Label nexusLabel = new Label("NEXUS");
        nexusLabel.setStyle(
            "-fx-text-fill: #f8fafc; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 22; " +
            "-fx-letter-spacing: 5px; " +
            "-fx-font-family: " + FONT + ";"
        );

        Label daeLabel = new Label("DIRECCIÓN DE ALTOS ESTUDIOS");
        daeLabel.setStyle(
            "-fx-text-fill: rgba(255,255,255,0.38); " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 11; " +
            "-fx-letter-spacing: 2px; " +
            "-fx-font-family: " + FONT + ";"
        );

        VBox logoBlock = new VBox(6, logoView, nexusLabel, daeLabel);
        logoBlock.setAlignment(Pos.CENTER);
        VBox.setMargin(logoBlock, new Insets(0, 0, 14, 0));

        HBox statusBar = new HBox(10,
            makeStatusPill(FontAwesomeSolid.CLOCK, "Jornada: ", InvestigationClock.getDuration().toHours() + " horas"),
            makeStatusPill(FontAwesomeSolid.COPY, "Casos activos: ", String.valueOf(CasoRepository.getCasos().size())),
            makeStatusPill(FontAwesomeSolid.USERS, "Equipo: ", formatTeamLabel())
        );
        statusBar.setAlignment(Pos.CENTER);
        VBox.setMargin(statusBar, new Insets(14, 0, 0, 0));

        VBox centerArea = new VBox(0, welcomeTitle, logoBlock, statusBar);
        centerArea.setAlignment(Pos.CENTER);
        centerArea.setPadding(new Insets(0, 20, 0, 20));
        centerArea.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        centerArea.setStyle("-fx-background-color: transparent;");
        return centerArea;
    }

    private HBox buildActionBar() {
        boolean isFirstTime = !java.nio.file.Files.exists(
            java.nio.file.Path.of(System.getProperty("user.home"), "Documents", "NEXUS", "active-session-snapshot.json")
        );

        HBox leftButton = makeActionBtn(
            FontAwesomeSolid.FOLDER_OPEN,
            "Procesos del despacho",
            "Noticias criminales y expedientes",
            false,
            () -> playTransitionAndGo("VIDEO2.mp4", this::openCaseManagement)
        );

        javafx.scene.Node leftButtonContainer = leftButton;

        if (isFirstTime) {
            // Apply a highlighted focus border, glow and warning colors
            leftButton.setStyle(
                "-fx-background-color: #f59e0b; " +
                "-fx-background-radius: 11; " +
                "-fx-border-color: #ffffff; " +
                "-fx-border-width: 2.5; " +
                "-fx-border-radius: 11; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(245,158,11,0.8), 20, 0.6, 0, 0); " +
                "-fx-cursor: hand;"
            );

            // Create a floating, blinking label OUTSIDE and ABOVE the button
            Label startHint = new Label("👇 ¡Empiece por aquí! 👇");
            startHint.setStyle(
                "-fx-text-fill: #fcd34d; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 13; " +
                "-fx-font-family: " + FONT + "; " +
                "-fx-background-color: rgba(245,158,11,0.15); " +
                "-fx-border-color: #f59e0b; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 6; " +
                "-fx-background-radius: 6; " +
                "-fx-padding: 4 8 4 8;"
            );

            // Soft floating animation for the hint
            ScaleTransition hintPulse = new ScaleTransition(Duration.seconds(0.8), startHint);
            hintPulse.setFromX(1.0);
            hintPulse.setFromY(1.0);
            hintPulse.setToX(1.06);
            hintPulse.setToY(1.06);
            hintPulse.setAutoReverse(true);
            hintPulse.setCycleCount(Animation.INDEFINITE);
            hintPulse.play();

            VBox wrappedLeft = new VBox(8, startHint, leftButton);
            wrappedLeft.setAlignment(Pos.BOTTOM_CENTER);
            
            // Offset the margin to ensure the floating label doesn't push the button out of alignment
            VBox.setMargin(startHint, new Insets(0, 0, 2, 0));
            leftButtonContainer = wrappedLeft;
        }

        rightButton = makeActionBtn(
            FontAwesomeSolid.CHART_BAR,
            "Toma de decisiones",
            "Patrones y conexiones",
            true,
            () -> playTransitionAndGo("VIDEO1.mp4", this::openAnalyticsBoard)
        );

        if (isFirstTime) {
            rightButton.setDisable(true);
            rightButton.setOpacity(0.5);
            rightButton.setStyle(
                "-fx-background-color: #4b5563; " +
                "-fx-background-radius: 11; " +
                "-fx-cursor: not-allowed;"
            );
            rightButton.setOnMouseClicked(null);
            rightButton.setOnMouseEntered(null);
            rightButton.setOnMouseExited(null);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actionBar = new HBox(leftButtonContainer, spacer, rightButton);
        actionBar.setAlignment(Pos.BOTTOM_CENTER);
        actionBar.setPadding(new Insets(0, 40, 32, 40));
        actionBar.setStyle("-fx-background-color: transparent;");
        return actionBar;
    }

    private HBox makeStatusPill(FontAwesomeSolid iconCode, String prefixText, String valueText) {
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(13);
        icon.setIconColor(Color.web("#e09d10"));

        Label prefix = new Label(prefixText);
        prefix.setStyle(
            "-fx-text-fill: rgba(255,255,255,0.50); " +
            "-fx-font-size: 12; " +
            "-fx-font-family: " + FONT + ";"
        );

        Label value = new Label(valueText);
        value.setStyle(
            "-fx-text-fill: #f1f5f9; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 12; " +
            "-fx-font-family: " + FONT + ";"
        );

        HBox pill = new HBox(6, icon, prefix, value);
        pill.setAlignment(Pos.CENTER);
        pill.setPadding(new Insets(5, 13, 5, 13));
        pill.setStyle(
            "-fx-background-color: rgba(255,255,255,0.07); " +
            "-fx-background-radius: 999; " +
            "-fx-border-color: rgba(255,255,255,0.12); " +
            "-fx-border-radius: 999; " +
            "-fx-border-width: 1;"
        );
        return pill;
    }

    private HBox makeActionBtn(
            FontAwesomeSolid icon,
            String mainText,
            String subText,
            boolean iconRight,
            Runnable action) {
        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(20);
        fontIcon.setIconColor(Color.web("#0c1220"));

        Label mainLabel = new Label(mainText);
        mainLabel.setStyle(
            "-fx-text-fill: #0c1220; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 14; " +
            "-fx-font-family: " + FONT + ";"
        );

        Label subLabel = new Label(subText);
        subLabel.setStyle(
            "-fx-text-fill: rgba(12,18,32,0.60); " +
            "-fx-font-size: 11; " +
            "-fx-font-family: " + FONT + ";"
        );

        VBox textBlock = new VBox(2, mainLabel, subLabel);
        textBlock.setAlignment(iconRight ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        HBox button;
        if (iconRight) {
            button = new HBox(12, textBlock, fontIcon);
            button.setAlignment(Pos.CENTER_RIGHT);
        } else {
            button = new HBox(12, fontIcon, textBlock);
            button.setAlignment(Pos.CENTER_LEFT);
        }

        button.setPadding(new Insets(15, 26, 15, 26));
        button.setMinWidth(220);
        button.setStyle(
            "-fx-background-color: #e09d10; " +
            "-fx-background-radius: 11; " +
            "-fx-cursor: hand;"
        );

        button.setOnMouseEntered(e -> {
            ScaleTransition grow = new ScaleTransition(Duration.millis(130), button);
            grow.setToX(1.03);
            grow.setToY(1.03);
            grow.playFromStart();
        });
        button.setOnMouseExited(e -> {
            ScaleTransition shrink = new ScaleTransition(Duration.millis(130), button);
            shrink.setToX(1.0);
            shrink.setToY(1.0);
            shrink.playFromStart();
        });
        button.setOnMouseClicked(e -> action.run());

        return button;
    }

    private void refreshTimer() {
        long remainingSeconds = InvestigationClock.getRemaining().getSeconds();
        timerLabel.setText(InvestigationClock.formatRemaining());
        if (remainingSeconds <= 600) {
            applyCriticalTimerStyle();
        } else {
            applyNormalTimerStyle();
        }
    }

    private void applyNormalTimerStyle() {
        timerBadge.setStyle(
            "-fx-background-color: rgba(220,38,38,0.85); " +
            "-fx-background-radius: 999; " +
            "-fx-border-color: rgba(255,100,100,0.50); " +
            "-fx-border-radius: 999; " +
            "-fx-border-width: 1.5;"
        );
    }

    private void applyCriticalTimerStyle() {
        timerBadge.setStyle(
            "-fx-background-color: rgba(185,28,28,0.95); " +
            "-fx-background-radius: 999; " +
            "-fx-border-color: rgba(255,100,100,0.50); " +
            "-fx-border-radius: 999; " +
            "-fx-border-width: 1.5;"
        );
    }

    private static String formatTeamLabel() {
        if (!InvestigationTeamContext.isConfigured()) {
            return "1 judicante";
        }
        int count = InvestigationTeamContext.getMembersDisplay().split(",").length;
        return "1 judicante";
    }

    private StackPane buildOnboardingOverlay() {
        onboardingDimLayer = new Rectangle();
        onboardingDimLayer.setVisible(false);
        onboardingDimLayer.setMouseTransparent(true);

        onboardingSpotlight = new Rectangle();
        onboardingSpotlight.setVisible(false);
        onboardingSpotlight.setMouseTransparent(true);

        onboardingFocusRing = new Rectangle();
        onboardingFocusRing.setFill(Color.TRANSPARENT);
        onboardingFocusRing.setStroke(Color.web("#fcd34d"));
        onboardingFocusRing.setStrokeWidth(2.5);
        onboardingFocusRing.setStrokeType(StrokeType.INSIDE);
        onboardingFocusRing.setArcWidth(12);
        onboardingFocusRing.setArcHeight(12);
        onboardingFocusRing.setVisible(false);
        onboardingFocusRing.setMouseTransparent(true);

        onboardingMessageLabel = new Label();
        onboardingMessageLabel.setWrapText(true);
        onboardingMessageLabel.setMaxWidth(380);
        onboardingMessageLabel.setAlignment(Pos.CENTER);
        onboardingMessageLabel.setStyle(
            "-fx-text-fill: #e8f0ff; " +
            "-fx-font-size: 14; " +
            "-fx-font-family: " + FONT + ";"
        );

        Label onboardingTitle = new Label("Guía del Despacho");
        onboardingTitle.setStyle(
            "-fx-text-fill: #f0c96e; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 16; " +
            "-fx-font-family: " + FONT + ";"
        );

        Button nextButton = new Button("Siguiente");
        nextButton.setStyle(
            "-fx-background-color: #2563c8; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-padding: 8 22 8 22; " +
            "-fx-cursor: hand; " +
            "-fx-font-family: " + FONT + ";"
        );
        nextButton.setOnAction(e -> {
            onboardingMode = false;
            PlayerViewBrown.onboardingMode = true;
            playTransitionAndGo("VIDEO1.mp4", this::openAnalyticsBoard);
        });

        HBox actions = new HBox(nextButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        onboardingDialog = new VBox(12, onboardingTitle, onboardingMessageLabel, actions);
        onboardingDialog.setPrefWidth(400);
        onboardingDialog.setMaxWidth(400);
        onboardingDialog.setMaxHeight(Region.USE_PREF_SIZE);
        onboardingDialog.setPadding(new Insets(20));
        onboardingDialog.setStyle(
            "-fx-background-color: #0b1a3a; " +
            "-fx-border-color: #3b7de0; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 12; " +
            "-fx-background-radius: 12; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.55), 18, 0.3, 0, 8);"
        );
        onboardingDialog.setPickOnBounds(true);

        Pane clickBlocker = new Pane();
        clickBlocker.setStyle("-fx-background-color: transparent;");
        clickBlocker.setPickOnBounds(true);
        clickBlocker.setOnMouseClicked(ev -> ev.consume());

        onboardingFloatLayer = new Pane();
        onboardingFloatLayer.setPickOnBounds(false);
        onboardingFloatLayer.getChildren().addAll(onboardingFocusRing, onboardingDialog);

        StackPane overlay = new StackPane(onboardingDimLayer, onboardingSpotlight, clickBlocker, onboardingFloatLayer);
        overlay.setPickOnBounds(false);
        StackPane.setAlignment(onboardingFloatLayer, Pos.TOP_LEFT);

        onboardingDimLayer.widthProperty().bind(overlay.widthProperty());
        onboardingDimLayer.heightProperty().bind(overlay.heightProperty());
        onboardingSpotlight.widthProperty().bind(overlay.widthProperty());
        onboardingSpotlight.heightProperty().bind(overlay.heightProperty());
        clickBlocker.prefWidthProperty().bind(overlay.widthProperty());
        clickBlocker.prefHeightProperty().bind(overlay.heightProperty());
        clickBlocker.minWidthProperty().bind(overlay.widthProperty());
        clickBlocker.minHeightProperty().bind(overlay.heightProperty());
        clickBlocker.maxWidthProperty().bind(overlay.widthProperty());
        clickBlocker.maxHeightProperty().bind(overlay.heightProperty());
        onboardingFloatLayer.prefWidthProperty().bind(overlay.widthProperty());
        onboardingFloatLayer.prefHeightProperty().bind(overlay.heightProperty());
        onboardingFloatLayer.minWidthProperty().bind(overlay.widthProperty());
        onboardingFloatLayer.minHeightProperty().bind(overlay.heightProperty());
        onboardingFloatLayer.maxWidthProperty().bind(overlay.widthProperty());
        onboardingFloatLayer.maxHeightProperty().bind(overlay.heightProperty());

        FadeTransition spotlightPulse = new FadeTransition(Duration.seconds(2.2), onboardingSpotlight);
        spotlightPulse.setFromValue(0.90);
        spotlightPulse.setToValue(1.0);
        spotlightPulse.setAutoReverse(true);
        spotlightPulse.setCycleCount(Animation.INDEFINITE);
        spotlightPulse.play();

        FadeTransition ringPulse = new FadeTransition(Duration.seconds(1.1), onboardingFocusRing);
        ringPulse.setFromValue(0.55);
        ringPulse.setToValue(1.0);
        ringPulse.setAutoReverse(true);
        ringPulse.setCycleCount(Animation.INDEFINITE);
        ringPulse.play();

        return overlay;
    }

    private void startOnboardingFlow() {
        onboardingMessageLabel.setText("Deberá dirigirse a la toma de decisiones");

        rightButton.setDisable(false);
        rightButton.setOpacity(1.0);
        rightButton.setScaleX(1.0);
        rightButton.setScaleY(1.0);
        rightButton.setStyle(
            "-fx-background-color: #e09d10; " +
            "-fx-background-radius: 11; " +
            "-fx-cursor: hand;"
        );
        rightButton.setOnMouseClicked(e -> playTransitionAndGo("VIDEO1.mp4", this::openAnalyticsBoard));
        rightButton.setOnMouseEntered(e -> {
            ScaleTransition grow = new ScaleTransition(Duration.millis(130), rightButton);
            grow.setToX(1.03);
            grow.setToY(1.03);
            grow.playFromStart();
        });
        rightButton.setOnMouseExited(e -> {
            ScaleTransition shrink = new ScaleTransition(Duration.millis(130), rightButton);
            shrink.setToX(1.0);
            shrink.setToY(1.0);
            shrink.playFromStart();
        });

        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
        delay.setOnFinished(ev -> javafx.application.Platform.runLater(() -> javafx.application.Platform.runLater(() -> {
            view.applyCss();
            view.layout();
            positionSpotlightOn(rightButton, 0.14);
            positionFocusRingOn(rightButton);
            positionOnboardingDialogBottom();
        })));
        delay.play();
    }

    private void positionSpotlightOn(Node target, double minRadius) {
        if (target == null) {
            onboardingSpotlight.setVisible(false);
            onboardingDimLayer.setVisible(true);
            onboardingDimLayer.setFill(Color.color(0, 0, 0, 0.82));
            return;
        }
        positionSpotlightOnBounds(getVisualSceneBounds(target), minRadius);
    }

    private void positionSpotlightOnBounds(Bounds targetBounds, double minRadius) {
        if (targetBounds == null || onboardingOverlay.getWidth() <= 0 || onboardingOverlay.getHeight() <= 0) {
            onboardingSpotlight.setVisible(false);
            onboardingDimLayer.setVisible(true);
            onboardingDimLayer.setFill(Color.color(0, 0, 0, 0.82));
            return;
        }

        Bounds overlayBounds = onboardingOverlay.localToScene(onboardingOverlay.getBoundsInLocal());
        double overlayW = overlayBounds.getWidth();
        double overlayH = overlayBounds.getHeight();
        if (overlayW <= 0 || overlayH <= 0) {
            return;
        }

        double centerX = ((targetBounds.getMinX() + targetBounds.getMaxX()) / 2.0 - overlayBounds.getMinX()) / overlayW;
        double centerY = ((targetBounds.getMinY() + targetBounds.getMaxY()) / 2.0 - overlayBounds.getMinY()) / overlayH;
        double targetSize = Math.max(targetBounds.getWidth(), targetBounds.getHeight());
        double radius = Math.max(minRadius, Math.min(0.34, (targetSize * 3.4) / Math.min(overlayW, overlayH)));

        onboardingDimLayer.setVisible(false);
        onboardingSpotlight.setVisible(true);
        onboardingSpotlight.setFill(new RadialGradient(
                0, 0, centerX, centerY, radius, true, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.TRANSPARENT),
                new Stop(0.38, Color.TRANSPARENT),
                new Stop(0.62, Color.color(3.0 / 255.0, 7.0 / 255.0, 18.0 / 255.0, 0.45)),
                new Stop(0.85, Color.color(3.0 / 255.0, 7.0 / 255.0, 18.0 / 255.0, 0.90)),
                new Stop(1.00, Color.color(3.0 / 255.0, 7.0 / 255.0, 18.0 / 255.0, 0.96))
        ));
    }

    private void positionFocusRingOn(Node target) {
        if (target == null) {
            onboardingFocusRing.setVisible(false);
            return;
        }
        positionFocusRingOnBounds(getVisualSceneBounds(target));
    }

    private void positionFocusRingOnBounds(Bounds sceneBounds) {
        if (sceneBounds == null || onboardingFloatLayer == null) {
            onboardingFocusRing.setVisible(false);
            return;
        }

        Point2D topLeft = onboardingFloatLayer.sceneToLocal(sceneBounds.getMinX(), sceneBounds.getMinY());
        Point2D bottomRight = onboardingFloatLayer.sceneToLocal(sceneBounds.getMaxX(), sceneBounds.getMaxY());

        double minX = Math.min(topLeft.getX(), bottomRight.getX());
        double minY = Math.min(topLeft.getY(), bottomRight.getY());
        double maxX = Math.max(topLeft.getX(), bottomRight.getX());
        double maxY = Math.max(topLeft.getY(), bottomRight.getY());

        onboardingFocusRing.setLayoutX(minX);
        onboardingFocusRing.setLayoutY(minY);
        onboardingFocusRing.setWidth(Math.max(1, maxX - minX));
        onboardingFocusRing.setHeight(Math.max(1, maxY - minY));
        onboardingFocusRing.setVisible(true);
    }

    private Bounds getVisualSceneBounds(Node node) {
        if (node == null || !node.isVisible()) {
            return null;
        }

        Bounds local = node.getLayoutBounds();
        if (local.getWidth() <= 0 || local.getHeight() <= 0) {
            local = node.getBoundsInLocal();
        }

        Point2D topLeft = node.localToScene(local.getMinX(), local.getMinY());
        Point2D topRight = node.localToScene(local.getMaxX(), local.getMinY());
        Point2D bottomLeft = node.localToScene(local.getMinX(), local.getMaxY());
        Point2D bottomRight = node.localToScene(local.getMaxX(), local.getMaxY());

        double minX = Math.min(Math.min(topLeft.getX(), topRight.getX()), Math.min(bottomLeft.getX(), bottomRight.getX()));
        double minY = Math.min(Math.min(topLeft.getY(), topRight.getY()), Math.min(bottomLeft.getY(), bottomRight.getY()));
        double maxX = Math.max(Math.max(topLeft.getX(), topRight.getX()), Math.max(bottomLeft.getX(), bottomRight.getX()));
        double maxY = Math.max(Math.max(topLeft.getY(), topRight.getY()), Math.max(bottomLeft.getY(), bottomRight.getY()));
        return new javafx.geometry.BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

    private void positionOnboardingDialogBottom() {
        onboardingDialog.applyCss();
        onboardingDialog.layout();
        double layerW = onboardingFloatLayer.getWidth();
        double layerH = onboardingFloatLayer.getHeight();
        if (layerW <= 0 || layerH <= 0) {
            return;
        }
        double dialogW = onboardingDialog.getWidth();
        double dialogH = onboardingDialog.getHeight();
        double bottomMargin = 140;
        onboardingDialog.setLayoutX(Math.max(24, (layerW - dialogW) / 2.0));
        onboardingDialog.setLayoutY(Math.max(24, layerH - dialogH - bottomMargin));
    }
}
