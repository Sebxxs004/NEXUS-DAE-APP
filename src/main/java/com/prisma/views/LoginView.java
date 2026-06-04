package com.prisma.views;

import com.prisma.ui.Theme;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

public class LoginView {
    private final Stage stage;
    private final StackPane view;

    public LoginView(Stage stage) {
        this.stage = stage;
        this.view = new StackPane();
        this.view.setPrefSize(1500, 900);
        this.view.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        this.view.setStyle("-fx-background-color: #040814;");

        Image backgroundImage = new Image(getClass().getResourceAsStream("/styles/assets/fondo-login.png"));
        ImageView backgroundView = new ImageView(backgroundImage);
        backgroundView.setPreserveRatio(false);
        backgroundView.fitWidthProperty().bind(view.widthProperty());
        backgroundView.fitHeightProperty().bind(view.heightProperty());

        Rectangle horizontalOverlay = new Rectangle();
        horizontalOverlay.widthProperty().bind(view.widthProperty());
        horizontalOverlay.heightProperty().bind(view.heightProperty());
        horizontalOverlay.setFill(new LinearGradient(
                0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.rgb(4, 8, 20, 0.92)),
                new Stop(0.38, Color.rgb(4, 8, 20, 0.72)),
                new Stop(0.65, Color.rgb(4, 8, 20, 0.18)),
                new Stop(1.00, Color.TRANSPARENT)
        ));

        Rectangle bottomFade = new Rectangle();
        bottomFade.widthProperty().bind(view.widthProperty());
        bottomFade.setHeight(180);
        bottomFade.setFill(new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.TRANSPARENT),
                new Stop(1.0, Color.rgb(4, 8, 20, 0.85))
        ));
        StackPane.setAlignment(bottomFade, Pos.BOTTOM_LEFT);

        VBox leftPanel = new VBox();
        leftPanel.setSpacing(0);
        leftPanel.setMaxWidth(400);
        leftPanel.setPadding(new Insets(40, 44, 40, 44));
        leftPanel.setStyle("-fx-background-color: transparent;");
        StackPane.setAlignment(leftPanel, Pos.CENTER_LEFT);

        HBox logoRow = new HBox(10);
        logoRow.setAlignment(Pos.CENTER_LEFT);

        StackPane fgnBox = new StackPane();
        fgnBox.setPrefSize(36, 36);
        fgnBox.setMinSize(36, 36);
        fgnBox.setMaxSize(36, 36);
        fgnBox.setStyle("-fx-background-color: #c6820a; -fx-background-radius: 6;");
        Label fgnLabel = new Label("FGN");
        fgnLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 11; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
        fgnBox.getChildren().add(fgnLabel);

        VBox logoTextBox = new VBox();
        logoTextBox.setSpacing(1);
        Label nexusLabel = new Label("NEXUS DAE");
        nexusLabel.setStyle("-fx-text-fill: #e09d10; -fx-font-size: 13; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
        Label fgnSubtitle = new Label("FISCALÍA GENERAL DE LA NACIÓN");
        fgnSubtitle.setStyle("-fx-text-fill: rgba(255,255,255,0.45); -fx-font-size: 10; -fx-font-family: 'Segoe UI';");
        logoTextBox.getChildren().addAll(nexusLabel, fgnSubtitle);
        logoRow.getChildren().addAll(fgnBox, logoTextBox);
        VBox.setMargin(logoRow, new Insets(0, 0, 28, 0));

        HBox liveBadge = new HBox(6);
        liveBadge.setAlignment(Pos.CENTER_LEFT);
        liveBadge.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-border-color: rgba(255,255,255,0.10); -fx-border-width: 1; -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 4 11 4 11;");
        Circle liveDot = new Circle(3, Color.web("#22c55e"));
        FadeTransition dotPulse = new FadeTransition(Duration.seconds(2), liveDot);
        dotPulse.setFromValue(1.0);
        dotPulse.setToValue(0.3);
        dotPulse.setCycleCount(FadeTransition.INDEFINITE);
        dotPulse.setAutoReverse(true);
        dotPulse.play();
        Label liveLabel = new Label("Sistema activo");
        liveLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.50); -fx-font-size: 11; -fx-font-family: 'Segoe UI';");
        liveBadge.getChildren().addAll(liveDot, liveLabel);
        VBox.setMargin(liveBadge, new Insets(0, 0, 16, 0));

        VBox mainTitleBox = new VBox(0);
        Label t1 = new Label("Simulador de");
        Label t2 = new Label("Despacho");
        Label t3 = new Label("Fiscal");
        t1.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 34; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
        t2.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 34; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
        t3.setStyle("-fx-text-fill: #e09d10; -fx-font-size: 34; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
        mainTitleBox.getChildren().addAll(t1, t2, t3);
        VBox.setMargin(mainTitleBox, new Insets(0, 0, 8, 0));

        Label subtitle = new Label("Acceso para fiscales autorizados. Asuma el rol de Fiscal Delegado y tome decisiones reales.");
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(320);
        subtitle.setStyle("-fx-text-fill: rgba(255,255,255,0.50); -fx-font-size: 13; -fx-font-family: 'Segoe UI';");
        VBox.setMargin(subtitle, new Insets(0, 0, 36, 0));

        VBox buttonGroup = new VBox(12);
        buttonGroup.setMaxWidth(300);

        HBox nexusButton = buildPrimaryAction();
        HBox instructionsButton = buildGhostAction();
        HBox exitButton = buildExitAction();
        buttonGroup.getChildren().addAll(nexusButton, instructionsButton, exitButton);

        leftPanel.getChildren().addAll(logoRow, liveBadge, mainTitleBox, subtitle, buttonGroup);

        Label footerNote = new Label("Colombia · Sistema institucional\nFiscalía General de la Nación");
        footerNote.setStyle("-fx-text-fill: rgba(255,255,255,0.28); -fx-font-size: 11; -fx-font-family: 'Segoe UI';");
        footerNote.setTextAlignment(TextAlignment.RIGHT);
        footerNote.setAlignment(Pos.CENTER_RIGHT);
        StackPane.setAlignment(footerNote, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(footerNote, new Insets(0, 22, 18, 0));

        view.getChildren().addAll(backgroundView, horizontalOverlay, bottomFade, leftPanel, footerNote);
    }

    private HBox buildPrimaryAction() {
        HBox button = new HBox(12);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setPadding(new Insets(14, 20, 14, 20));
        button.setCursor(Cursor.HAND);
        button.setStyle("-fx-background-color: #e09d10; -fx-background-radius: 10;");

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(30, 30);
        iconBox.setMinSize(30, 30);
        iconBox.setMaxSize(30, 30);
        iconBox.setStyle("-fx-background-color: rgba(0,0,0,0.18); -fx-background-radius: 7;");
        Label icon = new Label("⚖️");
        icon.setStyle("-fx-font-size: 16; -fx-font-family: 'Segoe UI';");
        iconBox.getChildren().add(icon);

        VBox textBox = new VBox(1);
        Label title = new Label("Ingresar a NEXUS");
        title.setStyle("-fx-text-fill: #0c1220; -fx-font-size: 14; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
        Label subtitle = new Label("Iniciar despacho fiscal");
        subtitle.setStyle("-fx-text-fill: #0c1220; -fx-font-size: 11; -fx-font-family: 'Segoe UI'; -fx-opacity: 0.65;");
        textBox.getChildren().addAll(title, subtitle);

        button.getChildren().addAll(iconBox, textBox);

        button.setOnMouseEntered(e -> {
            TranslateTransition tt = new TranslateTransition(Duration.millis(140), button);
            tt.setToX(3);
            tt.play();
        });
        button.setOnMouseExited(e -> {
            TranslateTransition tt = new TranslateTransition(Duration.millis(140), button);
            tt.setToX(0);
            tt.play();
        });
        button.setOnMouseClicked(e -> openNexus());
        return button;
    }

    private HBox buildGhostAction() {
        HBox button = new HBox(12);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setPadding(new Insets(14, 20, 14, 20));
        button.setCursor(Cursor.HAND);
        button.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-border-color: rgba(255,255,255,0.18); -fx-border-width: 1; -fx-background-radius: 10; -fx-border-radius: 10;");

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(30, 30);
        iconBox.setMinSize(30, 30);
        iconBox.setMaxSize(30, 30);
        iconBox.setStyle("-fx-background-color: rgba(255,255,255,0.10); -fx-background-radius: 7;");
        Label icon = new Label("📋");
        icon.setStyle("-fx-font-size: 16; -fx-font-family: 'Segoe UI';");
        iconBox.getChildren().add(icon);

        VBox textBox = new VBox(1);
        Label title = new Label("Leer instrucciones");
        title.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 14; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
        Label subtitle = new Label("Cómo funciona el simulador");
        subtitle.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 11; -fx-font-family: 'Segoe UI'; -fx-opacity: 0.65;");
        textBox.getChildren().addAll(title, subtitle);

        button.getChildren().addAll(iconBox, textBox);

        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: rgba(255,255,255,0.13); -fx-border-color: rgba(255,255,255,0.18); -fx-border-width: 1; -fx-background-radius: 10; -fx-border-radius: 10;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-border-color: rgba(255,255,255,0.18); -fx-border-width: 1; -fx-background-radius: 10; -fx-border-radius: 10;"));
        button.setOnMouseClicked(e -> openInstructions());
        return button;
    }



    private HBox buildExitAction() {
        HBox button = new HBox(12);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setPadding(new Insets(14, 20, 14, 20));
        button.setCursor(Cursor.HAND);
        button.setStyle(
            "-fx-background-color: rgba(127,29,29,0.22); " +
            "-fx-border-color: rgba(248,113,113,0.35); " +
            "-fx-border-width: 1; " +
            "-fx-background-radius: 10; " +
            "-fx-border-radius: 10;"
        );

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(30, 30);
        iconBox.setMinSize(30, 30);
        iconBox.setMaxSize(30, 30);
        iconBox.setStyle("-fx-background-color: rgba(0,0,0,0.18); -fx-background-radius: 7;");
        Label icon = new Label("⏻");
        icon.setStyle("-fx-font-size: 16; -fx-text-fill: #fecaca; -fx-font-family: 'Segoe UI';");
        iconBox.getChildren().add(icon);

        VBox textBox = new VBox(1);
        Label title = new Label("Salir del sistema");
        title.setStyle("-fx-text-fill: #fecaca; -fx-font-size: 14; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
        Label subtitle = new Label("Cerrar la aplicación");
        subtitle.setStyle("-fx-text-fill: #fecaca; -fx-font-size: 11; -fx-font-family: 'Segoe UI'; -fx-opacity: 0.75;");
        textBox.getChildren().addAll(title, subtitle);

        button.getChildren().addAll(iconBox, textBox);

        button.setOnMouseEntered(e -> button.setStyle(
            "-fx-background-color: rgba(153,27,27,0.38); " +
            "-fx-border-color: rgba(248,113,113,0.55); " +
            "-fx-border-width: 1; " +
            "-fx-background-radius: 10; " +
            "-fx-border-radius: 10;"
        ));
        button.setOnMouseExited(e -> button.setStyle(
            "-fx-background-color: rgba(127,29,29,0.22); " +
            "-fx-border-color: rgba(248,113,113,0.35); " +
            "-fx-border-width: 1; " +
            "-fx-background-radius: 10; " +
            "-fx-border-radius: 10;"
        ));
        button.setOnMouseClicked(e -> Platform.exit());
        return button;
    }

    private void openNexus() {
        AdminViewNew adminViewNew = new AdminViewNew(stage);
        Scene scene = new Scene(adminViewNew.getView(), 1500, 900);
        Theme.apply(scene);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setFullScreen(true);
    }

    private void openInstructions() {
        InstructionsView instructionsView = new InstructionsView(stage);
        Scene scene = new Scene(instructionsView.getView(), 1500, 900);
        Theme.apply(scene);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setFullScreen(true);
    }

    public StackPane getView() {
        return view;
    }

    public void applyTheme(Scene scene) {
        Theme.apply(scene);
    }

    public void goBackToLogin() {
        LoginView loginView = new LoginView(stage);
        Scene scene = new Scene(loginView.getView(), 1500, 900);
        applyTheme(scene);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setFullScreen(true);
    }
}
