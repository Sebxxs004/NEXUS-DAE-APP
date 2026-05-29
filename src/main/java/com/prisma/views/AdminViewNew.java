package com.prisma.views;

import javafx.animation.ScaleTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import com.prisma.ui.Theme;

public class AdminViewNew {
    private final StackPane view;
    private final Label timerLabel;
    private final Timeline timerTimeline;

    public AdminViewNew(Stage stage) {
        view = new StackPane();
        view.setStyle("-fx-background-color: #040814;");
        DistractionAlertManager.attach(stage);

        Image backgroundImage = new Image(getClass().getResourceAsStream("/styles/assets/fondo-admin.jpeg"));
        ImageView backgroundView = new ImageView(backgroundImage);
        backgroundView.setPreserveRatio(false);
        backgroundView.fitWidthProperty().bind(view.widthProperty());
        backgroundView.fitHeightProperty().bind(view.heightProperty());
        backgroundView.setMouseTransparent(true);

        Label welcomeLabel = new Label("BIENVENIDO A TU DESPACHO");
        welcomeLabel.setStyle(
            "-fx-font-size: 34; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #f8fafc; " +
            "-fx-font-family: 'Segoe UI'; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.75), 18, 0.25, 0, 4);"
        );

        Image logoImage = new Image(getClass().getResourceAsStream("/styles/assets/PRISMA-DAE.png"));
        ImageView logoView = new ImageView(logoImage);
        logoView.setPreserveRatio(true);
        logoView.setFitWidth(360);
        logoView.setSmooth(true);

        timerLabel = new Label("TIEMPO " + InvestigationClock.formatRemaining());
        timerLabel.setStyle(
            "-fx-font-size: 18; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #fff7ed; " +
            "-fx-background-color: rgba(153, 27, 27, 0.94); " +
            "-fx-background-radius: 999; " +
            "-fx-border-color: rgba(254, 202, 202, 0.92); " +
            "-fx-border-radius: 999; " +
            "-fx-border-width: 2; " +
            "-fx-padding: 10 18 10 18; " +
            "-fx-font-family: 'Segoe UI'; " +
            "-fx-effect: dropshadow(gaussian, rgba(248, 113, 113, 0.8), 24, 0.28, 0, 0);"
        );

        VBox centerContent = new VBox(16, welcomeLabel, logoView);
        centerContent.setAlignment(Pos.CENTER);
        centerContent.setMouseTransparent(true);
        BorderPane foreground = new BorderPane();
        foreground.setPickOnBounds(false);
        foreground.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Button btnBackLogin = new Button("↩  Volver al login");
        styleAnimatedButton(btnBackLogin, false);
        btnBackLogin.setOnAction(e -> {
            DistractionAlertManager.stopMonitoring();
            LoginView loginView = new LoginView(stage);
            Scene scene = new Scene(loginView.getView(), 980, 680);
            Theme.apply(scene);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setFullScreen(true);
        });

        BorderPane topBar = new BorderPane();
        topBar.setPadding(new Insets(20, 24, 0, 0));
        topBar.setLeft(btnBackLogin);
        topBar.setRight(timerLabel);

        foreground.setTop(topBar);
        foreground.setCenter(centerContent);

        Button btnCases = new Button("📁  Gestión de casos");
        btnCases.setPrefWidth(340);
        btnCases.setPrefHeight(64);
        styleAnimatedButton(btnCases, true);
        btnCases.setOnAction(e -> {
            CasesManagementBrownView casesView = new CasesManagementBrownView(stage);
            Scene scene = new Scene(casesView.getView(), 1500, 900);
            casesView.applyTheme(scene);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setFullScreen(true);
        });

        Button btnBoard = new Button("📊  Tablero analítico");
        btnBoard.setPrefWidth(340);
        btnBoard.setPrefHeight(64);
        styleAnimatedButton(btnBoard, true);
        btnBoard.setOnAction(e -> {
            PlayerViewBrown playerView = new PlayerViewBrown(stage);
            Scene scene = new Scene(playerView.getView(), 1500, 900);
            playerView.applyTheme(scene);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setFullScreen(true);
        });

        HBox leftAction = new HBox(btnCases);
        leftAction.setAlignment(Pos.BOTTOM_LEFT);
        leftAction.setPadding(new Insets(0, 0, 42, 42));
        leftAction.setMaxWidth(Region.USE_PREF_SIZE);

        HBox rightAction = new HBox(12, btnBoard);
        rightAction.setAlignment(Pos.BOTTOM_RIGHT);
        rightAction.setPadding(new Insets(0, 42, 42, 0));
        rightAction.setMaxWidth(Double.MAX_VALUE);

        BorderPane bottomBar = new BorderPane();
        bottomBar.setPickOnBounds(false);
        bottomBar.setPadding(new Insets(0, 42, 42, 42));
        bottomBar.setLeft(leftAction);
        bottomBar.setRight(rightAction);

        foreground.setBottom(bottomBar);

        view.getChildren().addAll(backgroundView, foreground);

        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> refreshTimer()));
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
        timerTimeline.play();
    }

    private void refreshTimer() {
        timerLabel.setText("TIEMPO " + InvestigationClock.formatRemaining());
        if (InvestigationClock.isCritical()) {
            if (!timerLabel.getStyleClass().contains("critical-timer-pill")) {
                timerLabel.getStyleClass().add("critical-timer-pill");
            }
            timerLabel.setStyle(
                "-fx-font-size: 18; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: #fff7ed; " +
                "-fx-background-color: linear-gradient(to right, rgba(153, 27, 27, 0.98), rgba(220, 38, 38, 0.92)); " +
                "-fx-background-radius: 999; " +
                "-fx-border-color: rgba(254, 226, 226, 0.98); " +
                "-fx-border-radius: 999; " +
                "-fx-border-width: 2; " +
                "-fx-padding: 10 18 10 18; " +
                "-fx-font-family: 'Segoe UI'; " +
                "-fx-effect: dropshadow(gaussian, rgba(248, 113, 113, 0.95), 28, 0.32, 0, 0);"
            );
        }
    }

    public StackPane getView() {
        return view;
    }

    public void applyTheme(Scene scene) {
        Theme.apply(scene);
    }

    private void styleAnimatedButton(Button button, boolean primary) {
        String style = primary
            ? "-fx-font-size: 16; -fx-font-weight: bold; -fx-background-color: linear-gradient(to bottom, #facc15, #d97706); -fx-text-fill: #1f1300; -fx-border-color: #fde68a; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10; -fx-font-family: 'Segoe UI'; -fx-cursor: hand;"
            : "-fx-font-size: 14; -fx-font-weight: bold; -fx-background-color: linear-gradient(to bottom, rgba(255,255,255,0.18), rgba(255,255,255,0.08)); -fx-text-fill: #fff7ed; -fx-border-color: rgba(255,255,255,0.18); -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10; -fx-font-family: 'Segoe UI'; -fx-cursor: hand;";
        button.setStyle(style);
        button.setPrefHeight(64);
        button.setEffect(new DropShadow(20, primary ? Color.rgb(0, 0, 0, 0.45) : Color.rgb(15, 23, 42, 0.38)));
        button.setOnMouseEntered(e -> {
            button.setEffect(new DropShadow(28, primary ? Color.rgb(251, 191, 36, 0.58) : Color.rgb(56, 189, 248, 0.50)));
            ScaleTransition grow = new ScaleTransition(Duration.millis(125), button);
            grow.setToX(1.06);
            grow.setToY(1.06);
            grow.playFromStart();
        });
        button.setOnMouseExited(e -> {
            button.setEffect(new DropShadow(20, primary ? Color.rgb(0, 0, 0, 0.45) : Color.rgb(15, 23, 42, 0.38)));
            ScaleTransition shrink = new ScaleTransition(Duration.millis(125), button);
            shrink.setToX(1.0);
            shrink.setToY(1.0);
            shrink.playFromStart();
        });
    }
}