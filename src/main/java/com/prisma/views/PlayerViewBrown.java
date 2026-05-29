package com.prisma.views;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;

import com.prisma.ui.Theme;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class PlayerViewBrown {
    private static final String BROWN_STYLESHEET;

    static {
        URL stylesheet = PlayerViewBrown.class.getResource("/styles/board-brown.css");
        BROWN_STYLESHEET = stylesheet == null ? null : stylesheet.toExternalForm();
    }

    private final StackPane view;
    private final Label timerLabel;
    private final Timeline timerTimeline;
    private final PlayerView playerView;
    private final TextField caseSearchField;

    public PlayerViewBrown(Stage stage) {
        playerView = new PlayerView(stage);

        StackPane moduleHost = readField(playerView, "moduleHost", StackPane.class);
        Pane board = readField(playerView, "board", Pane.class);
            DistractionAlertManager.attach(stage);
        caseSearchField = readField(playerView, "caseSearchField", TextField.class);
        @SuppressWarnings("unchecked")
        ListView<String> caseSearchSuggestions = (ListView<String>) readField(playerView, "caseSearchSuggestions", ListView.class);

        Image backgroundImage = new Image(getClass().getResourceAsStream("/styles/assets/tablero-analitico.jpeg"));
        ImageView backgroundView = new ImageView(backgroundImage);
        backgroundView.setPreserveRatio(false);
        backgroundView.fitWidthProperty().bind(stage.widthProperty());
        backgroundView.fitHeightProperty().bind(stage.heightProperty());
        backgroundView.setOpacity(0.28);

        Rectangle overlay = new Rectangle();
        overlay.widthProperty().bind(stage.widthProperty());
        overlay.heightProperty().bind(stage.heightProperty());
        overlay.setFill(Color.web("#1b1009", 0.42));
        overlay.setMouseTransparent(true);

        Label title = new Label("Bienvenido al TABLERO ANALITICO");
        title.setStyle(
            "-fx-font-size: 24; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #f8e4c6; " +
            "-fx-font-family: 'Segoe UI';"
        );

        Button searchButton = new Button("Buscar");
        searchButton.getStyleClass().add("secondary-button");

        searchButton.setOnAction(e -> invokeSearch(playerView, caseSearchField.getText()));

        caseSearchField.setPrefWidth(420);

        HBox searchRow = new HBox(10, caseSearchField, searchButton);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        VBox searchPanel = new VBox(6, searchRow, caseSearchSuggestions);
        searchPanel.setAlignment(Pos.CENTER_LEFT);
        searchPanel.setMaxWidth(520);

        timerLabel = new Label("TIEMPO " + InvestigationClock.formatRemaining());
        timerLabel.setStyle(
            "-fx-font-size: 17; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #fff7ed; " +
            "-fx-background-color: rgba(153, 27, 27, 0.94); " +
            "-fx-background-radius: 999; " +
            "-fx-border-color: rgba(254, 202, 202, 0.92); " +
            "-fx-border-radius: 999; " +
            "-fx-border-width: 2; " +
            "-fx-padding: 9 16 9 16; " +
            "-fx-font-family: 'Segoe UI'; " +
            "-fx-effect: dropshadow(gaussian, rgba(248, 113, 113, 0.8), 24, 0.28, 0, 0);"
        );

        Button backButton = new Button("Volver atrás");
        backButton.getStyleClass().add("danger-button");
        backButton.setOnAction(e -> {
            AdminViewNew adminViewNew = new AdminViewNew(stage);
            Scene scene = new Scene(adminViewNew.getView(), 1500, 900);
            Theme.apply(scene);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setFullScreen(true);
        });

        HBox topRow = new HBox(18, title, searchPanel, timerLabel, backButton);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPadding(new Insets(14, 18, 14, 18));
        topRow.setStyle(
            "-fx-background-color: rgba(32, 19, 11, 0.78); " +
            "-fx-background-radius: 18; " +
            "-fx-border-color: rgba(245, 158, 11, 0.24); " +
            "-fx-border-radius: 18;"
        );
        HBox.setHgrow(searchPanel, javafx.scene.layout.Priority.ALWAYS);

        BorderPane shell = new BorderPane();
        shell.setTop(topRow);
        shell.setCenter(moduleHost);
        shell.setPadding(new Insets(16));
        shell.setStyle("-fx-background-color: transparent;");

        view = new StackPane(backgroundView, overlay, shell);
        view.setStyle("-fx-background-color: transparent;");
        StackPane.setAlignment(shell, Pos.TOP_CENTER);

        applyBrownBoardSurface(board);

        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> refreshTimer()));
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
        timerTimeline.play();
    }

    public void applyTheme(Scene scene) {
        Theme.apply(scene);
        if (BROWN_STYLESHEET != null && !scene.getStylesheets().contains(BROWN_STYLESHEET)) {
            scene.getStylesheets().add(BROWN_STYLESHEET);
        }
    }

    public Parent getView() {
        return view;
    }

    public void focusCase(String query) {
        String value = query == null ? "" : query.trim();
        if (value.isEmpty()) {
            return;
        }
        caseSearchField.setText(value);
        invokeSearch(playerView, value);
    }

    private void applyBrownBoardSurface(Pane board) {
        board.setStyle("-fx-background-color: rgba(27, 15, 8, 0.58);");

        Image boardImage = new Image(getClass().getResourceAsStream("/styles/assets/tablero-analitico.jpeg"));
        ImageView boardBackgroundView = new ImageView(boardImage);
        boardBackgroundView.setPreserveRatio(false);
        boardBackgroundView.fitWidthProperty().bind(board.widthProperty());
        boardBackgroundView.fitHeightProperty().bind(board.heightProperty());
        boardBackgroundView.setOpacity(0.26);
        boardBackgroundView.setMouseTransparent(true);

        board.getChildren().add(0, boardBackgroundView);
    }

    private void invokeSearch(PlayerView playerView, String query) {
        try {
            Method method = PlayerView.class.getDeclaredMethod("searchCaseByQuery", String.class);
            method.setAccessible(true);
            method.invoke(playerView, query == null ? "" : query);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("No se pudo ejecutar la búsqueda del tablero.", ex);
        }
    }

    private static <T> T readField(Object target, String fieldName, Class<T> type) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return type.cast(field.get(target));
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("No se pudo leer el campo privado " + fieldName, ex);
        }
    }

    private void refreshTimer() {
        timerLabel.setText("TIEMPO " + InvestigationClock.formatRemaining());
        if (InvestigationClock.isCritical()) {
            if (!timerLabel.getStyleClass().contains("critical-timer-pill")) {
                timerLabel.getStyleClass().add("critical-timer-pill");
            }
            timerLabel.setStyle(
                "-fx-font-size: 17; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: #fff7ed; " +
                "-fx-background-color: linear-gradient(to right, rgba(153, 27, 27, 0.98), rgba(220, 38, 38, 0.92)); " +
                "-fx-background-radius: 999; " +
                "-fx-border-color: rgba(254, 226, 226, 0.98); " +
                "-fx-border-radius: 999; " +
                "-fx-border-width: 2; " +
                "-fx-padding: 9 16 9 16; " +
                "-fx-font-family: 'Segoe UI'; " +
                "-fx-effect: dropshadow(gaussian, rgba(248, 113, 113, 0.95), 28, 0.32, 0, 0);"
            );
        }
    }
}