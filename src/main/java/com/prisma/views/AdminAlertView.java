package com.prisma.views;

import com.prisma.ui.Theme;

import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

public class AdminAlertView {
    private final StackPane view;
    private final Label timerLabel;
    private final Timeline timerTimeline;

    public AdminAlertView(Stage stage) {
        view = new StackPane();
        view.setStyle("-fx-background-color: #040814;");
        DistractionAlertManager.attach(stage);

        Image backgroundImage = new Image(getClass().getResourceAsStream("/styles/assets/fondo-admin.jpeg"));
        ImageView backgroundView = new ImageView(backgroundImage);
        backgroundView.setPreserveRatio(false);
        backgroundView.fitWidthProperty().bind(view.widthProperty());
        backgroundView.fitHeightProperty().bind(view.heightProperty());
        backgroundView.setMouseTransparent(true);

        Label title = new Label("PANEL DE PRUEBA DE ALERTA");
        title.setStyle("-fx-font-size: 34; -fx-font-weight: bold; -fx-text-fill: #f8fafc; -fx-font-family: 'Segoe UI'; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.75), 18, 0.25, 0, 4);");

        Label subtitle = new Label("Desde aquí puedes forzar la próxima alerta distractiva sin abrir el panel nuevo.");
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(720);
        subtitle.setStyle("-fx-font-size: 18; -fx-text-fill: #fef3c7; -fx-font-family: 'Segoe UI';");

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

        Button btnBack = new Button("↩  Volver al login");
        styleButton(btnBack, false);
        btnBack.setOnAction(e -> {
            DistractionAlertManager.stopMonitoring();
            LoginView loginView = new LoginView(stage);
            Scene scene = new Scene(loginView.getView(), 980, 680);
            Theme.apply(scene);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setFullScreen(true);
        });

        HBox actions = new HBox(14, btnBack);
        actions.setAlignment(Pos.CENTER);

        VBox content = new VBox(18, title, subtitle, timerLabel, actions);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(28));
        content.setMaxWidth(860);
        content.setStyle("-fx-background-color: rgba(5, 10, 20, 0.74); -fx-background-radius: 26; -fx-border-radius: 26; -fx-border-color: rgba(251, 191, 36, 0.26); -fx-border-width: 1.5;");

        BorderPane foreground = new BorderPane();
        foreground.setPickOnBounds(false);
        foreground.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        HBox topBar = new HBox(timerLabel);
        topBar.setAlignment(Pos.TOP_RIGHT);
        topBar.setPadding(new Insets(20, 24, 0, 0));
        foreground.setTop(topBar);
        foreground.setCenter(content);

        view.getChildren().addAll(backgroundView, foreground);

        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> refreshTimer()));
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
        timerTimeline.play();
    }

    private void refreshTimer() {
        timerLabel.setText("TIEMPO " + InvestigationClock.formatRemaining());
        if (InvestigationClock.isCritical()) {
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

    private void styleButton(Button button, boolean primary) {
        String style = primary
                ? "-fx-font-size: 16; -fx-font-weight: bold; -fx-background-color: linear-gradient(to bottom, #ef4444, #991b1b); -fx-text-fill: #fff7ed; -fx-border-color: #fecaca; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12; -fx-font-family: 'Segoe UI'; -fx-cursor: hand;"
                : "-fx-font-size: 16; -fx-font-weight: bold; -fx-background-color: linear-gradient(to bottom, rgba(255,255,255,0.18), rgba(255,255,255,0.08)); -fx-text-fill: #fff7ed; -fx-border-color: rgba(255,255,255,0.18); -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12; -fx-font-family: 'Segoe UI'; -fx-cursor: hand;";
        button.setStyle(style);
        button.setPrefWidth(primary ? 250 : 210);
        button.setPrefHeight(58);
        button.setEffect(new DropShadow(18, primary ? Color.rgb(127, 29, 29, 0.7) : Color.rgb(15, 23, 42, 0.45)));
        button.setOnMouseEntered(e -> {
            button.setEffect(new DropShadow(26, primary ? Color.rgb(239, 68, 68, 0.8) : Color.rgb(56, 189, 248, 0.55)));
            ScaleTransition grow = new ScaleTransition(Duration.millis(120), button);
            grow.setToX(1.07);
            grow.setToY(1.07);
            grow.playFromStart();
        });
        button.setOnMouseExited(e -> {
            button.setEffect(new DropShadow(18, primary ? Color.rgb(127, 29, 29, 0.7) : Color.rgb(15, 23, 42, 0.45)));
            ScaleTransition shrink = new ScaleTransition(Duration.millis(120), button);
            shrink.setToX(1.0);
            shrink.setToY(1.0);
            shrink.playFromStart();
        });
    }

    public StackPane getView() {
        return view;
    }

    public void applyTheme(Scene scene) {
        Theme.apply(scene);
    }
}