package com.prisma.views;

import com.prisma.ui.Theme;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginView {
    private BorderPane view;

    public LoginView(Stage stage) {
        view = new BorderPane();
        view.setStyle("-fx-background-color: #040814;");
        
        // ===== SIDEBAR IZQUIERDO =====
        VBox sidebar = new VBox(24);
        sidebar.setStyle("-fx-background-color: rgba(4, 8, 20, 0.95); -fx-padding: 48;");
        sidebar.setPrefWidth(420);
        sidebar.setMaxWidth(420);
        sidebar.setMinWidth(420);
        
        Label logo = new Label("PRISMA DAE");
        logo.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-font-family: 'Segoe UI';");
        
        Label title = new Label("SIMULADOR DE\nINVESTIGACIÓN\nESTRUCTURAL DEL\nDESPACHO FISCAL");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #ffffff; -fx-font-family: 'Segoe UI'; -fx-line-spacing: 4;");
        title.setWrapText(true);
        
        Label description = new Label("Acceso seguro para fiscales autorizados. Fortaleza la identificación de patrones criminales, la asociación de casos, la articulación del equipo y la formulación del plan de acción para casos complejos.");
        description.setStyle("-fx-font-size: 12; -fx-text-fill: #a1d8f4; -fx-font-family: 'Segoe UI'; -fx-wrap-text: true;");
        description.setWrapText(true);
        
        Separator sep1 = new Separator();
        sep1.setStyle("-fx-padding: 12;");
        
        Label emailLabel = new Label("SELECCIONAR ROL");
        emailLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-font-family: 'Segoe UI';");

        Button btnClose = new Button("Cerrar sistema");
        btnClose.setPrefHeight(44);
        btnClose.setPrefWidth(164);
        btnClose.setStyle(buttonStyle("#7f1d1d", "#f87171", "#ffffff"));
        btnClose.setTooltip(new Tooltip("Salir de la aplicación"));
        
        Button btnAdminNew = new Button("📁  PANEL NUEVO");
        btnAdminNew.setPrefWidth(320);
        btnAdminNew.setPrefHeight(48);
        btnAdminNew.setStyle(
            "-fx-font-size: 12; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 12; " +
            "-fx-background-color: #d97706; " +
            "-fx-text-fill: #041526; " +
            "-fx-border-color: #f59e0b; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-font-family: 'Segoe UI'; " +
            "-fx-cursor: hand;"
        );
        btnAdminNew.setOnMouseEntered(e -> btnAdminNew.setStyle(
            "-fx-font-size: 12; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 12; " +
            "-fx-background-color: #f59e0b; " +
            "-fx-text-fill: #041526; " +
            "-fx-border-color: #fef3c7; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-font-family: 'Segoe UI'; " +
            "-fx-cursor: hand;"
        ));
        btnAdminNew.setOnMouseExited(e -> btnAdminNew.setStyle(
            "-fx-font-size: 12; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 12; " +
            "-fx-background-color: #d97706; " +
            "-fx-text-fill: #041526; " +
            "-fx-border-color: #f59e0b; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-font-family: 'Segoe UI'; " +
            "-fx-cursor: hand;"
        ));
        btnAdminNew.setOnAction(e -> {
            AdminViewNew adminViewNew = new AdminViewNew(stage);
            Scene scene = new Scene(adminViewNew.getView(), 1500, 900);
            Theme.apply(scene);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setFullScreen(true);
        });

        Button btnAlertAdmin = new Button("⚠  PANEL ALERTAS");
        btnAlertAdmin.setPrefWidth(320);
        btnAlertAdmin.setPrefHeight(48);
        btnAlertAdmin.setStyle(
            "-fx-font-size: 12; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 12; " +
            "-fx-background-color: linear-gradient(to bottom, #ef4444, #991b1b); " +
            "-fx-text-fill: #fff7ed; " +
            "-fx-border-color: #fecaca; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-font-family: 'Segoe UI'; " +
            "-fx-cursor: hand;"
        );
        btnAlertAdmin.setOnMouseEntered(e -> btnAlertAdmin.setStyle(
            "-fx-font-size: 12; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 12; " +
            "-fx-background-color: linear-gradient(to bottom, #f87171, #dc2626); " +
            "-fx-text-fill: #fff7ed; " +
            "-fx-border-color: #fee2e2; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-font-family: 'Segoe UI'; " +
            "-fx-cursor: hand;"
        ));
        btnAlertAdmin.setOnMouseExited(e -> btnAlertAdmin.setStyle(
            "-fx-font-size: 12; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 12; " +
            "-fx-background-color: linear-gradient(to bottom, #ef4444, #991b1b); " +
            "-fx-text-fill: #fff7ed; " +
            "-fx-border-color: #fecaca; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-font-family: 'Segoe UI'; " +
            "-fx-cursor: hand;"
        ));
        btnAlertAdmin.setOnAction(e -> {
            AdminAlertView alertView = new AdminAlertView(stage);
            Scene scene = new Scene(alertView.getView(), 1500, 900);
            alertView.applyTheme(scene);
            Theme.apply(scene);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setFullScreen(true);
        });

        Button btnPlayer = new Button("🔍  TABLERO FISCAL");
        btnPlayer.setPrefWidth(320);
        btnPlayer.setPrefHeight(48);
        btnPlayer.setStyle(
            "-fx-font-size: 12; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 12; " +
            "-fx-background-color: #1e3a8a; " +
            "-fx-text-fill: #ffffff; " +
            "-fx-border-color: #38bdf8; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-font-family: 'Segoe UI'; " +
            "-fx-cursor: hand;"
        );
        btnPlayer.setOnMouseEntered(e -> btnPlayer.setStyle(
            "-fx-font-size: 12; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 12; " +
            "-fx-background-color: #38bdf8; " +
            "-fx-text-fill: #040814; " +
            "-fx-border-color: #67e8f9; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-font-family: 'Segoe UI'; " +
            "-fx-cursor: hand;"
        ));
        btnPlayer.setOnMouseExited(e -> btnPlayer.setStyle(
            "-fx-font-size: 12; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 12; " +
            "-fx-background-color: #1e3a8a; " +
            "-fx-text-fill: #ffffff; " +
            "-fx-border-color: #38bdf8; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-font-family: 'Segoe UI'; " +
            "-fx-cursor: hand;"
        ));
        btnPlayer.setOnAction(e -> {
            PlayerViewBrown playerView = new PlayerViewBrown(stage);
            Scene scene = new Scene(playerView.getView(), 1500, 900);
            playerView.applyTheme(scene);
            Theme.apply(scene);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setFullScreen(true);
        });

        btnClose.setOnAction(e -> stage.close());

        VBox buttonBox = new VBox(12, btnAdminNew, btnAlertAdmin, btnPlayer, btnClose);
        
        sidebar.getChildren().addAll(logo, title, description, sep1, emailLabel, buttonBox);
        VBox.setVgrow(sidebar, Priority.ALWAYS);
        
        // ===== IMAGEN DE FONDO (DERECHA) =====
        Image backgroundImage = new Image(getClass().getResourceAsStream("/styles/assets/fondo-login.png"));
        ImageView backgroundView = new ImageView(backgroundImage);
        backgroundView.setPreserveRatio(false);
        backgroundView.fitWidthProperty().bind(view.widthProperty().subtract(420));
        backgroundView.fitHeightProperty().bind(view.heightProperty());
        
        StackPane imageContainer = new StackPane(backgroundView);
        HBox.setHgrow(imageContainer, Priority.ALWAYS);
        
        // ===== CONTENEDOR PRINCIPAL (SIDEBAR + FONDO) =====
        HBox mainContainer = new HBox(0, sidebar, imageContainer);
        mainContainer.setStyle("-fx-background-color: #040814;");
        
        view.setCenter(mainContainer);
    }

    private String buttonStyle(String background, String border, String text) {
        return "-fx-font-size: 12; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 12; " +
                "-fx-background-color: " + background + "; " +
                "-fx-text-fill: " + text + "; " +
                "-fx-border-color: " + border + "; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 6; " +
                "-fx-background-radius: 6; " +
                "-fx-font-family: 'Segoe UI'; " +
                "-fx-cursor: hand;";
    }

    public BorderPane getView() {
        return view;
    }
}
