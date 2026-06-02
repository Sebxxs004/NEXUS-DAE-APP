package com.prisma.views;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import com.prisma.ui.Theme;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

public class PlayerViewBrown {
    private static final String FONT = "'Segoe UI'";

    private final BorderPane view;
    private final Label timerLabel;
    private final HBox timerBadge;
    private final Timeline timerTimeline;
    private final PlayerView playerView;
    private final TextField caseSearchField;
    private final Stage stage;
    private final Pane edgeDecorLayer;
    private final Popup nodeTooltip;

    public PlayerViewBrown(Stage stage) {
        this.stage = stage;
        playerView = new PlayerView(stage);

        StackPane moduleHost = readField(playerView, "moduleHost", StackPane.class);
        Pane board = readField(playerView, "board", Pane.class);
        Pane nodeLayer = readField(playerView, "nodeLayer", Pane.class);
        javafx.scene.Group contentContainer = readField(playerView, "contentContainer", javafx.scene.Group.class);
        Pane groupLayer = readField(playerView, "groupLayer", Pane.class);
        VBox sidebar = readField(playerView, "sidebar", VBox.class);
        VBox boardModule = readField(playerView, "boardModule", VBox.class);
        VBox groupCardsContainer = readField(playerView, "groupCardsContainer", VBox.class);
        @SuppressWarnings("unchecked")
        ListView<String> connectionList = (ListView<String>) readField(playerView, "connectionList", ListView.class);
        Label totalNodesLabel = readField(playerView, "totalNodesLabel", Label.class);
        Label ungroupedNodesLabel = readField(playerView, "ungroupedNodesLabel", Label.class);
        Label statusLabel = readField(playerView, "statusLabel", Label.class);
        Button finishInvestigationButton = readField(playerView, "finishInvestigationButton", Button.class);
        @SuppressWarnings("unchecked")
        ListView<String> caseSearchSuggestions = (ListView<String>) readField(playerView, "caseSearchSuggestions", ListView.class);

        DistractionAlertManager.attach(stage);

        caseSearchField = readField(playerView, "caseSearchField", TextField.class);
        caseSearchField.setPromptText("Buscar caso o nodo...");
        caseSearchField.setPrefWidth(320);
        styleSearchField(caseSearchField, false);
        caseSearchField.focusedProperty().addListener((obs, oldFocus, focused) -> styleSearchField(caseSearchField, focused));
        caseSearchField.textProperty().addListener((obs, oldText, newText) -> {
            if (newText != null && !newText.isBlank()) {
                statusLabel.setText("Buscando: " + newText.trim());
            }
        });

        caseSearchSuggestions.setStyle(
            "-fx-background-color: #0d2459; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6;"
        );

        Label shield = new Label("⛨");
        shield.setStyle(
            "-fx-background-color: #c8a03b; " +
            "-fx-background-radius: 6 6 12 12; " +
            "-fx-padding: 6 10 6 10; " +
            "-fx-font-size: 16; " +
            "-fx-text-fill: #0a1a3a;"
        );

        Label title = new Label("TABLERO ANALÍTICO");
        title.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #d0e4ff; -fx-font-family: " + FONT + ";");

        Label subtitle = new Label("Fiscalía General de la Nación");
        subtitle.setStyle("-fx-font-size: 10; -fx-text-fill: #7ba3d8; -fx-font-family: " + FONT + ";");

        VBox brandBlock = new VBox(2, title, subtitle);
        brandBlock.setAlignment(Pos.CENTER_LEFT);

        Region separator = new Region();
        separator.setPrefWidth(1);
        separator.setMinHeight(32);
        separator.setMaxHeight(32);
        separator.setStyle("-fx-background-color: #1a3a7a;");

        Circle timerDot = new Circle(3.5, Color.web("#ff4444"));
        FadeTransition dotPulse = new FadeTransition(Duration.seconds(1), timerDot);
        dotPulse.setFromValue(1.0);
        dotPulse.setToValue(0.3);
        dotPulse.setAutoReverse(true);
        dotPulse.setCycleCount(Animation.INDEFINITE);
        dotPulse.play();

        timerLabel = new Label(InvestigationClock.formatRemaining());
        timerLabel.setStyle(
            "-fx-text-fill: #ffb0b0; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 13; " +
            "-fx-font-family: 'Consolas', 'Segoe UI', monospace;"
        );

        timerBadge = new HBox(8, timerDot, timerLabel);
        timerBadge.setAlignment(Pos.CENTER);
        timerBadge.setPadding(new Insets(5, 12, 5, 12));
        applyTimerStyle(false);

        Button volverButton = makeSecondaryButton("Volver");
        volverButton.setOnAction(e -> {
            AdminViewNew adminView = new AdminViewNew(stage);
            Scene scene = new Scene(adminView.getView(), 1500, 900);
            adminView.applyTheme(scene);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setFullScreen(true);
        });

        finishInvestigationButton.setText("Terminar");
        styleDangerButton(finishInvestigationButton);

        HBox topBar = new HBox(14, shield, brandBlock, separator, caseSearchField, timerBadge, volverButton, finishInvestigationButton);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 16, 10, 16));
        topBar.setStyle(
            "-fx-background-color: #0a1a3a; " +
            "-fx-border-color: transparent transparent #1a3a7a transparent; " +
            "-fx-border-width: 0 0 1 0;"
        );
        HBox.setHgrow(caseSearchField, Priority.ALWAYS);

        view = new BorderPane();
        view.setStyle("-fx-background-color: #08142e; -fx-font-family: " + FONT + ";");
        view.setTop(topBar);
        view.setCenter(moduleHost);

        sidebar.setPrefWidth(340);
        sidebar.setMinWidth(340);
        sidebar.setMaxWidth(340);
        sidebar.setStyle(
            "-fx-background-color: #0d1f45; " +
            "-fx-border-color: transparent transparent transparent #1a3a7a; " +
            "-fx-border-width: 0 0 0 1; " +
            "-fx-padding: 12;"
        );

        configureStatsRow(sidebar, totalNodesLabel, ungroupedNodesLabel);
        statusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #c8a03b; -fx-font-family: " + FONT + ";");

        styleSidebarTitles(sidebar);
        styleConnectionList(connectionList);
        connectionList.setPrefHeight(240);
        connectionList.setFixedCellSize(104);
        styleBoardModuleChrome(boardModule, statusLabel);

        groupCardsContainer.getChildren().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Node added : change.getAddedSubList()) {
                        styleGroupCard(added, groupCardsContainer.getChildren().indexOf(added));
                    }
                }
            }
        });

        nodeLayer.getChildren().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Node added : change.getAddedSubList()) {
                        styleCaseNode(added);
                    }
                }
            }
        });
        for (Node node : nodeLayer.getChildren()) {
            styleCaseNode(node);
        }

        edgeDecorLayer = new Pane();
        edgeDecorLayer.setMouseTransparent(true);
        int nodeIndex = contentContainer.getChildren().indexOf(nodeLayer);
        if (nodeIndex >= 0) {
            contentContainer.getChildren().add(nodeIndex, edgeDecorLayer);
        } else {
            contentContainer.getChildren().add(edgeDecorLayer);
        }

        nodeTooltip = buildNodeTooltip();
        nodeLayer.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (!(event.getTarget() instanceof Node target)) {
                return;
            }
            Node caseNode = findCaseNodeAncestor(target);
            if (caseNode == null) {
                return;
            }
            try {
                Field handModeField = findField(playerView.getClass(), "handMode");
                handModeField.setAccessible(true);
                if (handModeField.getBoolean(playerView)) {
                    return;
                }
            } catch (ReflectiveOperationException ignored) {
                return;
            }
            showNodeTooltip(caseNode, event.getScreenX(), event.getScreenY());
        });

        applyBoardSurface(board);

        timerTimeline = new Timeline(
            new KeyFrame(Duration.seconds(1), e -> refreshTimer()),
            new KeyFrame(Duration.millis(100), e -> {
                refreshCanvasStyles(nodeLayer, groupLayer, groupCardsContainer);
                refreshSidebarStats(totalNodesLabel, ungroupedNodesLabel);
            })
        );
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
        timerTimeline.play();
        refreshTimer();
        refreshSidebarStats(totalNodesLabel, ungroupedNodesLabel);
    }

    public void applyTheme(Scene scene) {
        Theme.apply(scene);
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

    private void applyBoardSurface(Pane board) {
        board.setStyle("-fx-background-color: #08142e;");
        board.getChildren().removeIf(child -> child instanceof ImageView);

        Image backgroundImage = new Image(getClass().getResourceAsStream("/styles/assets/tablero-analitico.png"));
        ImageView backgroundView = new ImageView(backgroundImage);
        backgroundView.setPreserveRatio(false);
        backgroundView.fitWidthProperty().bind(board.widthProperty());
        backgroundView.fitHeightProperty().bind(board.heightProperty());
        backgroundView.setMouseTransparent(true);
        backgroundView.setOpacity(0.38);

        Rectangle backgroundTint = new Rectangle();
        backgroundTint.widthProperty().bind(board.widthProperty());
        backgroundTint.heightProperty().bind(board.heightProperty());
        backgroundTint.setFill(Color.web("#08142e", 0.48));
        backgroundTint.setMouseTransparent(true);

        Pane gridLayer = new Pane();
        gridLayer.setMouseTransparent(true);
        Runnable redrawGrid = () -> {
            gridLayer.getChildren().clear();
            double width = Math.max(board.getWidth(), board.getPrefWidth());
            double height = Math.max(board.getHeight(), board.getPrefHeight());
            if (width <= 0 || height <= 0) {
                width = 1260;
                height = 720;
            }
            Color gridColor = Color.rgb(30, 77, 155, 0.12);
            for (double x = 0; x <= width; x += 40) {
                Line vertical = new Line(x, 0, x, height);
                vertical.setStroke(gridColor);
                vertical.setMouseTransparent(true);
                gridLayer.getChildren().add(vertical);
            }
            for (double y = 0; y <= height; y += 40) {
                Line horizontal = new Line(0, y, width, y);
                horizontal.setStroke(gridColor);
                horizontal.setMouseTransparent(true);
                gridLayer.getChildren().add(horizontal);
            }
        };
        board.widthProperty().addListener((obs, oldValue, newValue) -> redrawGrid.run());
        board.heightProperty().addListener((obs, oldValue, newValue) -> redrawGrid.run());
        redrawGrid.run();

        board.getChildren().add(0, backgroundView);
        board.getChildren().add(1, backgroundTint);
        board.getChildren().add(2, gridLayer);
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
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            return type.cast(field.get(target));
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("No se pudo leer el campo privado " + fieldName, ex);
        }
    }

    private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ex) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    private void refreshTimer() {
        timerLabel.setText(InvestigationClock.formatRemaining());
        applyTimerStyle(InvestigationClock.getRemaining().getSeconds() <= 600);
    }

    private void applyTimerStyle(boolean critical) {
        timerBadge.setStyle(
            "-fx-background-color: " + (critical ? "#7c1a1a" : "rgba(124,26,26,0.92)") + "; " +
            "-fx-border-color: #d04040; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 5 12 5 12;"
        );
    }

    private static void styleSearchField(TextField field, boolean focused) {
        field.setStyle(
            "-fx-background-color: #0d2459; " +
            "-fx-border-color: " + (focused ? "#3b7de0" : "#1a3a7a") + "; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-text-fill: #d0e4ff; " +
            "-fx-prompt-text-fill: #4a72a8; " +
            "-fx-font-size: 13; " +
            "-fx-font-family: " + FONT + ";"
        );
    }

    private static Button makeSecondaryButton(String text) {
        Button button = new Button(text);
        String normal =
            "-fx-background-color: #0d2459; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-text-fill: #d0e4ff; " +
            "-fx-font-size: 12; " +
            "-fx-font-family: " + FONT + "; " +
            "-fx-cursor: hand;";
        String hover =
            "-fx-background-color: #1a3a7a; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-text-fill: #d0e4ff; " +
            "-fx-font-size: 12; " +
            "-fx-font-family: " + FONT + "; " +
            "-fx-cursor: hand;";
        button.setStyle(normal);
        button.setOnMouseEntered(e -> button.setStyle(hover));
        button.setOnMouseExited(e -> button.setStyle(normal));
        return button;
    }

    private static void styleDangerButton(Button button) {
        String normal =
            "-fx-background-color: #3a1010; " +
            "-fx-border-color: #8b2020; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-text-fill: #ffb0b0; " +
            "-fx-font-size: 12; " +
            "-fx-font-family: " + FONT + "; " +
            "-fx-cursor: hand;";
        String hover =
            "-fx-background-color: #5a1a1a; " +
            "-fx-border-color: #8b2020; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-text-fill: #ffb0b0; " +
            "-fx-font-size: 12; " +
            "-fx-font-family: " + FONT + "; " +
            "-fx-cursor: hand;";
        button.setStyle(normal);
        button.setOnMouseEntered(e -> button.setStyle(hover));
        button.setOnMouseExited(e -> button.setStyle(normal));
    }

    private static void configureStatsRow(VBox sidebar, Label totalNodesLabel, Label ungroupedNodesLabel) {
        walkNodes(sidebar, node -> {
            if (!(node instanceof HBox statsRow)) {
                return;
            }
            boolean hasStats = statsRow.getChildren().contains(totalNodesLabel)
                    || statsRow.getChildren().contains(ungroupedNodesLabel);
            if (!hasStats) {
                return;
            }
            statsRow.setSpacing(10);
            statsRow.setAlignment(Pos.CENTER);
            for (Node child : statsRow.getChildren()) {
                if (child instanceof Label label) {
                    boolean warning = label == ungroupedNodesLabel;
                    HBox.setHgrow(label, Priority.ALWAYS);
                    label.setMinWidth(150);
                    label.setPrefWidth(150);
                    label.setMaxWidth(165);
                    label.setWrapText(true);
                    label.setAlignment(Pos.CENTER);
                    label.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
                }
            }
        });
    }

    private void refreshSidebarStats(Label totalNodesLabel, Label ungroupedNodesLabel) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> nodes = (List<Object>) readField(playerView, "nodes", List.class);
            @SuppressWarnings("unchecked")
            Map<Object, String> groupedNodeSignature =
                    (Map<Object, String>) readField(playerView, "groupedNodeSignature", Map.class);
            long totalCount = nodes.size();
            long ungroupedCount = nodes.stream()
                    .filter(node -> !groupedNodeSignature.containsKey(node))
                    .count();
            playerView.applySidebarStatChip(totalNodesLabel, totalCount, "Total de casos", false);
            playerView.applySidebarStatChip(ungroupedNodesLabel, ungroupedCount, "Sin grupo", true);
        } catch (RuntimeException ignored) {
            // keep labels updated by PlayerView
        }
    }

    private static void styleSidebarTitles(VBox sidebar) {
        walkNodes(sidebar, node -> {
            if (node instanceof Label label && label.getText() != null) {
                String text = label.getText();
                if (text.contains("Conexiones") || text.contains("Grupos") || text.contains("RESUMEN")
                        || text.equals("Conexiones y grupos")) {
                    label.setStyle(
                        "-fx-font-size: 11; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #c8a03b; " +
                        "-fx-font-family: " + FONT + ";"
                    );
                }
            }
            if (node instanceof VBox box && box.getStyleClass().contains("panel-card")) {
                box.setStyle(
                    "-fx-background-color: #0a1a3a; " +
                    "-fx-border-color: #1a3a7a; " +
                    "-fx-border-radius: 6; " +
                    "-fx-background-radius: 6; " +
                    "-fx-padding: 8;"
                );
            }
        });
    }

    private void styleConnectionList(ListView<String> connectionList) {
        connectionList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    return;
                }
                setText(null);

                VBox card = buildConnectionCard(item);
                HBox.setHgrow(card, Priority.ALWAYS);

                Button deleteButton = new Button("Eliminar");
                deleteButton.setStyle(
                    "-fx-background-color: #3a1010; " +
                    "-fx-border-color: #8b2020; " +
                    "-fx-border-radius: 5; " +
                    "-fx-background-radius: 5; " +
                    "-fx-border-width: 1; " +
                    "-fx-padding: 4 8 4 8; " +
                    "-fx-text-fill: #ffb0b0; " +
                    "-fx-font-size: 10; " +
                    "-fx-font-family: " + FONT + "; " +
                    "-fx-cursor: hand;"
                );
                deleteButton.setOnAction(event -> {
                    int index = getIndex();
                    if (index >= 0) {
                        playerView.removeConnectionAt(index);
                    }
                    event.consume();
                });

                HBox row = new HBox(8, card, deleteButton);
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
                setStyle("-fx-background-color: transparent; -fx-padding: 0 0 6 0;");
            }
        });
        connectionList.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
    }

    private static VBox buildConnectionCard(String item) {
        ConnectionDetails details = parseConnectionItem(item);

        Label pairLabel = new Label(details.pairText());
        pairLabel.setWrapText(true);
        pairLabel.setStyle(
            "-fx-font-size: 11; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #d0e4ff; " +
            "-fx-font-family: " + FONT + ";"
        );

        Label associatedLabel = new Label("Asociado por: " + details.associatedBy());
        associatedLabel.setWrapText(true);
        associatedLabel.setStyle(
            "-fx-font-size: 10; " +
            "-fx-text-fill: #7ba3d8; " +
            "-fx-font-family: " + FONT + ";"
        );

        Label justificationLabel = new Label("Justificación: " + details.justification());
        justificationLabel.setWrapText(true);
        justificationLabel.setStyle(
            "-fx-font-size: 10; " +
            "-fx-text-fill: #a8c8f0; " +
            "-fx-font-style: italic; " +
            "-fx-font-family: " + FONT + ";"
        );

        VBox card = new VBox(4, pairLabel, associatedLabel, justificationLabel);
        card.setStyle(
            "-fx-background-color: #0a1a3a; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-padding: 8 10 8 10;"
        );
        return card;
    }

    private static ConnectionDetails parseConnectionItem(String item) {
        String pairText = item;
        String associatedBy = "—";
        String justification = "—";

        int separator = item.indexOf(" | ");
        if (separator >= 0) {
            pairText = item.substring(0, separator).trim();
            String details = item.substring(separator + 3).trim();

            int justificationIndex = details.indexOf("Justificación:");
            if (justificationIndex >= 0) {
                justification = details.substring(justificationIndex + "Justificación:".length()).trim();
                details = details.substring(0, justificationIndex).trim();
            }

            if (details.startsWith("Asociado por:")) {
                associatedBy = details.substring("Asociado por:".length()).trim();
            } else if (!details.isBlank()) {
                associatedBy = details;
            }
        }

        if (associatedBy.isBlank()) {
            associatedBy = "—";
        }
        if (justification.isBlank()) {
            justification = "—";
        }

        return new ConnectionDetails(pairText, associatedBy, justification);
    }

    private record ConnectionDetails(String pairText, String associatedBy, String justification) {
    }

    private static void styleBoardModuleChrome(VBox boardModule, Label statusLabel) {
        boardModule.setStyle("-fx-background-color: transparent;");
        walkNodes(boardModule, node -> {
            if (node instanceof HBox toolbar && toolbar.getChildren().size() >= 2) {
                boolean hasZoom = toolbar.getChildren().stream()
                    .anyMatch(child -> child instanceof Button button && "-".equals(button.getText()));
                if (hasZoom) {
                    toolbar.setStyle(
                        "-fx-background-color: #0d2459; " +
                        "-fx-border-color: transparent transparent #1a3a7a transparent; " +
                        "-fx-border-width: 0 0 1 0; " +
                        "-fx-padding: 8 12 8 12;"
                    );
                }
            }
            if (node instanceof Label label && label == statusLabel) {
                label.setStyle("-fx-font-size: 12; -fx-text-fill: #c8a03b; -fx-font-family: " + FONT + ";");
            }
            if (node instanceof Button button) {
                String text = button.getText();
                if (text != null && text.startsWith("Modo:")) {
                    styleModeButton(button);
                } else if ("-".equals(text) || "+".equals(text)) {
                    styleZoomButton(button);
                } else if (text != null && text.endsWith("%")) {
                    button.setStyle(
                        "-fx-background-color: transparent; " +
                        "-fx-border-color: transparent; " +
                        "-fx-text-fill: #d0e4ff; " +
                        "-fx-font-size: 12; " +
                        "-fx-min-width: 36; " +
                        "-fx-font-family: " + FONT + ";"
                    );
                } else if ("Recalcular grupos".equals(text)) {
                    styleRecalcularButton(button);
                }
            }
            if (node instanceof Label label && "Casos en movimiento".equals(label.getText())) {
                label.setStyle(
                    "-fx-font-size: 11; " +
                    "-fx-font-weight: bold; " +
                    "-fx-text-fill: #7ba3d8; " +
                    "-fx-font-family: " + FONT + ";"
                );
            }
        });
    }

    private static void styleModeButton(Button button) {
        Runnable apply = () -> {
            boolean handMode = button.getText() != null && button.getText().contains("Mano");
            button.setStyle(
                "-fx-background-color: " + (handMode ? "transparent" : "#2563c8") + "; " +
                "-fx-border-color: #1a3a7a; " +
                "-fx-border-radius: 6; " +
                "-fx-background-radius: 6; " +
                "-fx-text-fill: " + (handMode ? "#7ba3d8" : "white") + "; " +
                "-fx-font-size: 12; " +
                "-fx-font-family: " + FONT + "; " +
                "-fx-cursor: hand;"
            );
        };
        apply.run();
        button.textProperty().addListener((obs, oldText, newText) -> apply.run());
        button.setOnMouseEntered(e -> {
            if (button.getText() != null && button.getText().contains("Mano")) {
                button.setStyle(
                    "-fx-background-color: #1a3a7a; " +
                    "-fx-border-color: #1a3a7a; " +
                    "-fx-border-radius: 6; " +
                    "-fx-background-radius: 6; " +
                    "-fx-text-fill: #d0e4ff; " +
                    "-fx-font-size: 12; " +
                    "-fx-font-family: " + FONT + "; " +
                    "-fx-cursor: hand;"
                );
            }
        });
        button.setOnMouseExited(e -> apply.run());
    }

    private static void styleZoomButton(Button button) {
        String normal =
            "-fx-background-color: #0d2459; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-text-fill: #d0e4ff; " +
            "-fx-font-size: 12; " +
            "-fx-min-width: 24; " +
            "-fx-min-height: 24; " +
            "-fx-font-family: " + FONT + "; " +
            "-fx-cursor: hand;";
        String hover =
            "-fx-background-color: #1a3a7a; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-text-fill: #d0e4ff; " +
            "-fx-font-size: 12; " +
            "-fx-min-width: 24; " +
            "-fx-min-height: 24; " +
            "-fx-font-family: " + FONT + "; " +
            "-fx-cursor: hand;";
        button.setStyle(normal);
        button.setOnMouseEntered(e -> button.setStyle(hover));
        button.setOnMouseExited(e -> button.setStyle(normal));
    }

    private static void styleRecalcularButton(Button button) {
        String normal =
            "-fx-background-color: #0a1a3a; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-text-fill: #7ba3d8; " +
            "-fx-font-size: 12; " +
            "-fx-max-width: infinity; " +
            "-fx-font-family: " + FONT + "; " +
            "-fx-cursor: hand;";
        String hover =
            "-fx-background-color: #1a3a7a; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-text-fill: #d0e4ff; " +
            "-fx-font-size: 12; " +
            "-fx-max-width: infinity; " +
            "-fx-font-family: " + FONT + "; " +
            "-fx-cursor: hand;";
        button.setStyle(normal);
        button.setOnMouseEntered(e -> button.setStyle(hover));
        button.setOnMouseExited(e -> button.setStyle(normal));
    }

    private static void styleGroupCard(Node node, int index) {
        if (!(node instanceof VBox card)) {
            return;
        }
        card.setStyle(
            "-fx-background-color: #0a1a3a; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 8 10 8 10;"
        );
        walkNodes(card, child -> {
            if (child instanceof TextField nameField) {
                nameField.setStyle(
                    "-fx-background-color: transparent; " +
                    "-fx-border-color: transparent; " +
                    "-fx-text-fill: #d0e4ff; " +
                    "-fx-font-size: 12; " +
                    "-fx-font-weight: bold; " +
                    "-fx-font-family: " + FONT + ";"
                );
            } else if (child instanceof Label label) {
                String text = label.getText();
                if (text != null && (text.contains("casos") || text.contains("justificación")
                        || text.contains("Números en tablero") || text.contains("N.º ")
                        || text.startsWith("Decisión:") || text.startsWith("Decisiones:")
                        || text.startsWith("Detalle:") || text.startsWith("Justificaciones:"))) {
                    label.setStyle("-fx-font-size: 10; -fx-text-fill: #7ba3d8; -fx-font-style: italic; -fx-font-family: " + FONT + ";");
                } else if (text != null && text.contains("→")) {
                    label.setStyle("-fx-font-size: 10; -fx-text-fill: #a8c8f0; -fx-font-family: " + FONT + ";");
                }
            } else if (child instanceof Button button && button.getText() != null
                    && button.getText().toLowerCase().contains("finalizar")) {
                styleFinalizeGroupButton(button);
            } else if (child instanceof Circle circle && circle.getRadius() <= 8) {
                circle.setFill(Color.web(index % 2 == 0 ? "#c8a03b" : "#3b7de0"));
                circle.setRadius(5);
            } else if (child instanceof javafx.scene.control.TextArea area) {
                area.setStyle(
                    "-fx-control-inner-background: transparent; " +
                    "-fx-background-color: transparent; " +
                    "-fx-text-fill: #7ba3d8; " +
                    "-fx-font-size: 10; " +
                    "-fx-font-style: italic; " +
                    "-fx-font-family: " + FONT + ";"
                );
            }
        });
    }

    private static void styleFinalizeGroupButton(Button button) {
        String normal =
            "-fx-background-color: #0a1d4a; " +
            "-fx-border-color: #2563c8; " +
            "-fx-border-radius: 5; " +
            "-fx-background-radius: 5; " +
            "-fx-text-fill: #a8c8f0; " +
            "-fx-font-size: 11; " +
            "-fx-font-family: " + FONT + "; " +
            "-fx-cursor: hand;";
        String hover =
            "-fx-background-color: #0d2e6e; " +
            "-fx-border-color: #2563c8; " +
            "-fx-border-radius: 5; " +
            "-fx-background-radius: 5; " +
            "-fx-text-fill: #a8c8f0; " +
            "-fx-font-size: 11; " +
            "-fx-font-family: " + FONT + "; " +
            "-fx-cursor: hand;";
        button.setStyle(normal);
        button.setOnMouseEntered(e -> button.setStyle(hover));
        button.setOnMouseExited(e -> button.setStyle(normal));
    }

    private static void styleCaseNode(Node node) {
        if (!(node instanceof StackPane caseNode)) {
            return;
        }

        caseNode.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-background-radius: 999;");
        caseNode.setPrefSize(72, 72);
        caseNode.setMinSize(72, 72);
        caseNode.setMaxSize(72, 72);

        boolean selected = caseNode.getStyleClass().contains("selected");
        try {
            Field selectedField = findField(caseNode.getClass(), "selected");
            selectedField.setAccessible(true);
            selected = selectedField.getBoolean(caseNode);
        } catch (ReflectiveOperationException ignored) {
            // keep style-class fallback
        }
        final boolean isSelected = selected;

        String displayNumber = "";
        try {
            displayNumber = readFieldStatic(caseNode, "displayNumber", String.class);
        } catch (RuntimeException ignored) {
            // keep empty fallback
        }
        final String nodeNumber = displayNumber;
        final String imageName = resolveCaseImageName(caseNode);

        walkNodes(caseNode, child -> {
            if (child instanceof VBox contentBox) {
                contentBox.setSpacing(0);
                contentBox.setAlignment(Pos.CENTER);
                contentBox.getChildren().removeIf(item ->
                    item instanceof Circle circle && circle.getRadius() < 29 && circle.getRadius() > 20
                );
                for (Node item : contentBox.getChildren()) {
                    if (item instanceof Circle circle) {
                        circle.setRadius(30);
                        circle.setFill(Color.web("#0d1f45"));
                        circle.setStroke(Color.web(isSelected ? "#c8a03b" : "#3b7de0"));
                        circle.setStrokeWidth(isSelected ? 2.5 : 1.5);
                    } else if (item instanceof Text text) {
                        text.setVisible(false);
                        text.setManaged(false);
                    }
                }
            } else if (child instanceof Label label && label.getStyleClass().contains("case-number-badge")) {
                label.setText(nodeNumber);
                label.setStyle(
                    "-fx-font-size: 11; " +
                    "-fx-font-weight: bold; " +
                    "-fx-text-fill: #f0c96e; " +
                    "-fx-background-color: transparent; " +
                    "-fx-border-color: transparent;"
                );
                StackPane.setAlignment(label, Pos.CENTER);
                StackPane.setMargin(label, new Insets(0, 0, 8, 0));
            }
        });

        if (!Boolean.TRUE.equals(caseNode.getProperties().get("prisma-brown-fixed"))) {
            ensureNodeDiscStack(caseNode, isSelected, nodeNumber, imageName);
            caseNode.getProperties().put("prisma-brown-fixed", true);
        } else {
            updateNodeDiscStack(caseNode, isSelected);
            updateNodeLabels(caseNode, nodeNumber, imageName);
        }
    }

    private static String resolveCaseImageName(Object caseNode) {
        try {
            Object caso = readFieldStatic(caseNode, "caso", Object.class);
            Method getImagenPath = caso.getClass().getMethod("getImagenPath");
            String imagenPath = (String) getImagenPath.invoke(caso);
            if (imagenPath != null && !imagenPath.isBlank()) {
                String fileName = java.nio.file.Paths.get(imagenPath).getFileName().toString();
                int dot = fileName.lastIndexOf('.');
                return dot > 0 ? fileName.substring(0, dot) : fileName;
            }
            Method getNombre = caso.getClass().getMethod("getNombre");
            String nombre = (String) getNombre.invoke(caso);
            if (nombre != null && !nombre.isBlank()) {
                return nombre;
            }
        } catch (ReflectiveOperationException ignored) {
            // fallback below
        }
        return "Sin imagen";
    }

    private static String formatImageLabel(String imageName) {
        if (imageName == null || imageName.isBlank()) {
            return "Sin imagen";
        }
        if (imageName.length() <= 18) {
            return imageName;
        }
        return imageName.substring(0, 16) + "…";
    }

    private static void ensureNodeDiscStack(StackPane caseNode, boolean isSelected, String displayNumber, String imageName) {
        Node contentNode = caseNode.getChildren().stream()
            .filter(child -> child instanceof VBox)
            .findFirst()
            .orElse(null);
        if (!(contentNode instanceof VBox contentBox)) {
            return;
        }

        Circle outer = new Circle(32);
        outer.setFill(Color.web("#0d1f45"));
        outer.setStroke(Color.web(isSelected ? "#c8a03b" : "#3b7de0"));
        outer.setStrokeWidth(isSelected ? 2.5 : 1.5);
        outer.setMouseTransparent(true);

        Circle inner = new Circle(26, Color.web("#1a3a7a"));
        inner.setMouseTransparent(true);

        Label idLabel = new Label(displayNumber);
        idLabel.setStyle(
            "-fx-font-size: 10; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #f0c96e; " +
            "-fx-font-family: " + FONT + ";"
        );
        idLabel.setMouseTransparent(true);
        idLabel.setUserData("node-id-label");

        Label caseLabel = new Label(formatImageLabel(imageName));
        caseLabel.setWrapText(true);
        caseLabel.setMaxWidth(58);
        caseLabel.setTextAlignment(TextAlignment.CENTER);
        caseLabel.setStyle(
            "-fx-font-size: 7; " +
            "-fx-text-fill: #d0e4ff; " +
            "-fx-text-alignment: center; " +
            "-fx-font-family: " + FONT + ";"
        );
        caseLabel.setMouseTransparent(true);
        caseLabel.setUserData("node-image-label");

        VBox labels = new VBox(2, idLabel, caseLabel);
        labels.setAlignment(Pos.CENTER);
        labels.setMaxWidth(58);
        labels.setMouseTransparent(true);

        StackPane disc = new StackPane(outer, inner, labels);
        disc.setMouseTransparent(true);
        disc.setMaxSize(72, 72);
        disc.setPrefSize(72, 72);

        int contentIndex = caseNode.getChildren().indexOf(contentBox);
        caseNode.getChildren().remove(contentBox);
        caseNode.getChildren().removeIf(child ->
            child instanceof Label label && label.getStyleClass().contains("case-number-badge")
        );
        if (contentIndex >= 0) {
            caseNode.getChildren().add(contentIndex, disc);
        } else {
            caseNode.getChildren().add(disc);
        }
    }

    private static void updateNodeDiscStack(StackPane caseNode, boolean isSelected) {
        for (Node child : caseNode.getChildren()) {
            if (!(child instanceof StackPane disc)) {
                continue;
            }
            for (Node discChild : disc.getChildren()) {
                if (discChild instanceof Circle circle && circle.getRadius() >= 30) {
                    circle.setStroke(Color.web(isSelected ? "#c8a03b" : "#3b7de0"));
                    circle.setStrokeWidth(isSelected ? 2.5 : 1.5);
                }
            }
        }
    }

    private static void updateNodeLabels(StackPane caseNode, String displayNumber, String imageName) {
        for (Node child : caseNode.getChildren()) {
            if (!(child instanceof StackPane disc)) {
                continue;
            }
            for (Node discChild : disc.getChildren()) {
                if (!(discChild instanceof VBox labels)) {
                    continue;
                }
                for (Node labelNode : labels.getChildren()) {
                    if (labelNode instanceof Label label) {
                        if ("node-id-label".equals(label.getUserData())) {
                            label.setText(displayNumber);
                        } else if ("node-image-label".equals(label.getUserData())) {
                            label.setText(formatImageLabel(imageName));
                        }
                    }
                }
            }
        }
    }

    private void refreshCanvasStyles(Pane nodeLayer, Pane groupLayer, VBox groupCardsContainer) {
        for (Node node : nodeLayer.getChildren()) {
            styleCaseNode(node);
        }

        @SuppressWarnings("unchecked")
        List<Object> connections = (List<Object>) readField(playerView, "connections", List.class);
        @SuppressWarnings("unchecked")
        Map<Object, String> groupedNodeSignature = (Map<Object, String>) readField(playerView, "groupedNodeSignature", Map.class);
        @SuppressWarnings("unchecked")
        List<Object> currentClusters = (List<Object>) readField(playerView, "currentClusters", List.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> overlayBySignature = (Map<String, Object>) readField(playerView, "overlayBySignature", Map.class);

        edgeDecorLayer.getChildren().clear();

        for (Object connectionObject : connections) {
            try {
                Object from = readField(connectionObject, "from", Object.class);
                Object to = readField(connectionObject, "to", Object.class);
                Line line = readField(connectionObject, "line", Line.class);
                if (line == null) {
                    continue;
                }
                boolean sameGroup = groupedNodeSignature.containsKey(from)
                        && groupedNodeSignature.containsKey(to)
                        && groupedNodeSignature.get(from).equals(groupedNodeSignature.get(to));
                if (sameGroup) {
                    line.setStroke(Color.web("#c8a03b"));
                    line.setStrokeWidth(1.5);
                    line.getStrokeDashArray().setAll(4.0, 2.0);
                } else {
                    line.setStroke(Color.web("#3b7de0"));
                    line.setStrokeWidth(1.2);
                    line.getStrokeDashArray().clear();
                }
                addEdgeDecoration(line, from, to, sameGroup);
            } catch (RuntimeException ignored) {
                // keep existing styling
            }
        }

        int groupIndex = 0;
        for (Object cluster : currentClusters) {
            try {
                String signature = readField(cluster, "signature", String.class);
                Object overlay = overlayBySignature.get(signature);
                if (overlay == null) {
                    groupIndex++;
                    continue;
                }
                Rectangle rectangle = readField(overlay, "rectangle", Rectangle.class);
                Text nameLabel = readField(overlay, "nameLabel", Text.class);
                boolean groupOne = groupIndex % 2 == 0;
                rectangle.setFill(groupOne ? Color.rgb(200, 160, 59, 0.08) : Color.rgb(37, 99, 200, 0.08));
                rectangle.setStroke(Color.web(groupOne ? "#c8a03b" : "#3b7de0"));
                rectangle.setStrokeWidth(1.5);
                rectangle.getStrokeDashArray().setAll(6.0, 3.0);
                rectangle.setArcWidth(12);
                rectangle.setArcHeight(12);
                nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
                nameLabel.setFill(Color.web(groupOne ? "#c8a03b" : "#7bc0ff"));
            } catch (RuntimeException ignored) {
                // keep overlay styling
            }
            groupIndex++;
        }

        String selectedGroupSignature = readField(playerView, "selectedGroupSignature", String.class);
        for (int i = 0; i < groupCardsContainer.getChildren().size(); i++) {
            Node cardNode = groupCardsContainer.getChildren().get(i);
            styleGroupCard(cardNode, i);
            if (selectedGroupSignature != null && i < currentClusters.size() && cardNode instanceof VBox box) {
                try {
                    Object cluster = currentClusters.get(i);
                    String signature = readField(cluster, "signature", String.class);
                    if (selectedGroupSignature.equals(signature)) {
                        box.setStyle(
                            "-fx-background-color: #0a1a3a; " +
                            "-fx-border-color: #c8a03b; " +
                            "-fx-border-radius: 8; " +
                            "-fx-background-radius: 8; " +
                            "-fx-padding: 8 10 8 10;"
                        );
                    }
                } catch (RuntimeException ignored) {
                    // keep card styling
                }
            }
        }
    }

    private void addEdgeDecoration(Line line, Object from, Object to, boolean sameGroup) {
        double startX = line.getStartX();
        double startY = line.getStartY();
        double endX = line.getEndX();
        double endY = line.getEndY();
        double midX = (startX + endX) / 2.0;
        double midY = (startY + endY) / 2.0;

        double angle = Math.atan2(endY - startY, endX - startX);
        double arrowSize = 8;
        Polygon arrow = new Polygon(
            0, 0,
            -arrowSize, arrowSize / 2.0,
            -arrowSize, -arrowSize / 2.0
        );
        arrow.setFill(Color.web(sameGroup ? "#c8a03b" : "#3b7de0"));
        arrow.setLayoutX(endX);
        arrow.setLayoutY(endY);
        arrow.setRotate(Math.toDegrees(angle));
        arrow.setMouseTransparent(true);
        edgeDecorLayer.getChildren().add(arrow);

        String fromShort = readNodeShortId(from);
        String toShort = readNodeShortId(to);
        Label edgeLabel = new Label(fromShort + " ↔ " + toShort);
        edgeLabel.setStyle(
            "-fx-text-fill: " + (sameGroup ? "#f0c96e" : "#a8c8f0") + "; " +
            "-fx-font-size: 9; " +
            "-fx-font-family: " + FONT + "; " +
            "-fx-background-color: #0d1f45; " +
            "-fx-border-color: " + (sameGroup ? "#c8a03b" : "#1a3a7a") + "; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-border-width: 0.6; " +
            "-fx-padding: 2 6 2 6;"
        );
        edgeLabel.setMouseTransparent(true);

        double dx = endX - startX;
        double dy = endY - startY;
        double length = Math.hypot(dx, dy);
        double offsetX = 0;
        double offsetY = -22;
        if (length > 0) {
            offsetX = (-dy / length) * 22;
            offsetY = (dx / length) * 22;
        }
        edgeLabel.setLayoutX(midX + offsetX - 24);
        edgeLabel.setLayoutY(midY + offsetY - 8);
        edgeDecorLayer.getChildren().add(edgeLabel);
    }

    private static String readNodeShortId(Object caseNode) {
        try {
            return readFieldStatic(caseNode, "displayNumber", String.class);
        } catch (RuntimeException ex) {
            return formatImageLabel(readNodeImageName(caseNode));
        }
    }

    private static String readNodeImageName(Object caseNode) {
        return resolveCaseImageName(caseNode);
    }

    private static String readNodeName(Object caseNode) {
        try {
            Object caso = readFieldStatic(caseNode, "caso", Object.class);
            Method getNombre = caso.getClass().getMethod("getNombre");
            return String.valueOf(getNombre.invoke(caso));
        } catch (ReflectiveOperationException ex) {
            try {
                return String.valueOf(readFieldStatic(caseNode, "displayNumber", String.class));
            } catch (RuntimeException ignored) {
                return "Nodo";
            }
        }
    }

    private static <T> T readFieldStatic(Object target, String fieldName, Class<T> type) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            return type.cast(field.get(target));
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("No se pudo leer el campo privado " + fieldName, ex);
        }
    }

    private Popup buildNodeTooltip() {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        return popup;
    }

    private void showNodeTooltip(Node caseNode, double screenX, double screenY) {
        String caseNumber;
        String groupName = "Sin grupo";
        try {
            caseNumber = readFieldStatic(caseNode, "displayNumber", String.class);
        } catch (RuntimeException ex) {
            caseNumber = "—";
        }

        @SuppressWarnings("unchecked")
        Map<Object, String> groupedNodeSignature = (Map<Object, String>) readField(playerView, "groupedNodeSignature", Map.class);
        @SuppressWarnings("unchecked")
        List<Object> currentClusters = (List<Object>) readField(playerView, "currentClusters", List.class);
        if (groupedNodeSignature.containsKey(caseNode)) {
            String signature = groupedNodeSignature.get(caseNode);
            for (Object cluster : currentClusters) {
                if (signature.equals(readField(cluster, "signature", String.class))) {
                    Object meta = readField(cluster, "meta", Object.class);
                    groupName = readField(meta, "name", String.class);
                    break;
                }
            }
        }

        Label idLine = new Label("Nodo: " + caseNumber);
        idLine.setStyle("-fx-font-size: 10; -fx-text-fill: #c8a03b; -fx-font-family: " + FONT + ";");

        Label caseLine = new Label(readNodeImageName(caseNode));
        caseLine.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: white; -fx-font-family: " + FONT + ";");

        Label groupLine = new Label("Grupo: " + groupName);
        groupLine.setStyle("-fx-font-size: 11; -fx-text-fill: #7ba3d8; -fx-font-family: " + FONT + ";");

        VBox content = new VBox(4, idLine, caseLine, groupLine);
        content.setPadding(new Insets(10, 12, 10, 12));
        content.setStyle(
            "-fx-background-color: #0d2459; " +
            "-fx-border-color: #3b7de0; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-border-width: 1;"
        );

        nodeTooltip.getContent().setAll(content);
        nodeTooltip.show(stage, screenX + 12, screenY + 12);

        Timeline hide = new Timeline(new KeyFrame(Duration.seconds(3.5), e -> nodeTooltip.hide()));
        hide.setCycleCount(1);
        hide.play();
    }

    private static Node findCaseNodeAncestor(Node node) {
        Node current = node;
        while (current != null) {
            if (current.getClass().getName().contains("CaseNode")) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private static void walkNodes(Node root, java.util.function.Consumer<Node> visitor) {
        visitor.accept(root);
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                walkNodes(child, visitor);
            }
        }
    }
}
