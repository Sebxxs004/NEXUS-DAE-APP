package com.prisma.views;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.prisma.data.CasoRepository;
import com.prisma.models.Caso;
import com.prisma.ui.Theme;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ScrollPane;
import javafx.scene.Cursor;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class CasesManagementBrownView {
    private static final String FONT = "'Segoe UI'";

    private final StackPane view;
    private final TilePane casesGrid;
    private final Label timerLabel;
    private final Timeline timerTimeline;
    private final StackPane modalOverlay;
    private final TextField searchField;
    private ImageView modalImageView;
    private Label modalTitleLabel;
    private Label modalHintLabel;
    private Button modalLocateButton;
    private Caso modalCurrentCase;
    private java.util.List<Caso> currentCases = new java.util.ArrayList<>();
    private StackPane modalImageViewport;
    private VBox modalCard;
    private double modalZoom = 1.0;
    private double modalBaseFitWidth = 900.0;
    private double modalBaseFitHeight = 620.0;
    private double dragAnchorX;
    private double dragAnchorY;
    private double dragStartTranslateX;
    private double dragStartTranslateY;

    // Batch grouping state
    private final java.util.Set<Caso> selectedCasesForBatch = new java.util.HashSet<>();
    private HBox batchButtonsContainer;
    private Button batchGroupNewButton;
    private Button batchGroupExistingButton;
    private StackPane batchJustificationOverlay;
    private StackPane addToGroupOverlay;

    // Performance caches
    private final Map<String, Boolean> fileExistsCache = new HashMap<>();
    private final Map<String, Image> imageCache = new HashMap<>();
    private final Stage stage;

    public CasesManagementBrownView(Stage stage) {
        this.stage = stage;
        Label shield = new Label("⚖");
        shield.setMinSize(28, 28);
        shield.setPrefSize(28, 28);
        shield.setMaxSize(28, 28);
        shield.setAlignment(Pos.CENTER);
        shield.setStyle(
            "-fx-background-color: #c8a03b; " +
            "-fx-background-radius: 4 4 14 14; " +
            "-fx-font-size: 13; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #0a1a3a; " +
            "-fx-font-family: " + FONT + ";"
        );

        Label title = new Label("Gestión de Casos");
        title.setStyle(
            "-fx-font-size: 13; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #d0e4ff; " +
            "-fx-font-family: " + FONT + ";"
        );

        Label topSubtitle = new Label("Fiscalía General de la Nación");
        topSubtitle.setStyle(
            "-fx-font-size: 10; " +
            "-fx-text-fill: #7ba3d8; " +
            "-fx-font-family: " + FONT + ";"
        );

        VBox titleBlock = new VBox(2, title, topSubtitle);
        titleBlock.setAlignment(Pos.CENTER_LEFT);

        Circle timerDot = new Circle(3, Color.web("#ff4444"));
        FadeTransition dotPulse = new FadeTransition(Duration.seconds(1), timerDot);
        dotPulse.setFromValue(1.0);
        dotPulse.setToValue(0.25);
        dotPulse.setAutoReverse(true);
        dotPulse.setCycleCount(Timeline.INDEFINITE);
        dotPulse.play();

        timerLabel = new Label(InvestigationClock.formatRemaining());
        timerLabel.setStyle(timerStyle(false));

        HBox timerBox = new HBox(8, timerDot, timerLabel);
        timerBox.setAlignment(Pos.CENTER);
        timerBox.setPadding(new Insets(4, 12, 4, 12));
        timerBox.setStyle(
            "-fx-background-color: #7c1a1a; " +
            "-fx-border-color: #d04040; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-border-width: 1;"
        );

        Button backButton = new Button("Volver atrás");
        String backNormal =
            "-fx-background-color: #0d2459; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 5 13 5 13; " +
            "-fx-text-fill: #d0e4ff; " +
            "-fx-font-size: 12; " +
            "-fx-font-family: " + FONT + "; " +
            "-fx-cursor: hand;";
        String backHover =
            "-fx-background-color: #1a3a7a; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 5 13 5 13; " +
            "-fx-text-fill: #d0e4ff; " +
            "-fx-font-size: 12; " +
            "-fx-font-family: " + FONT + "; " +
            "-fx-cursor: hand;";
        backButton.setStyle(backNormal);
        backButton.setOnMouseEntered(e -> backButton.setStyle(backHover));
        backButton.setOnMouseExited(e -> backButton.setStyle(backNormal));
        backButton.setOnAction(e -> {
            AdminViewNew adminViewNew = new AdminViewNew(stage);
            Scene scene = new Scene(adminViewNew.getView(), 1500, 900);
            Theme.apply(scene);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setFullScreen(true);
        });

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        HBox topRow = new HBox(12, shield, titleBlock, topSpacer, timerBox, backButton);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPadding(new Insets(10, 16, 10, 16));
        topRow.setStyle(
            "-fx-background-color: #0a1a3a; " +
            "-fx-border-color: transparent transparent #1a3a7a transparent; " +
            "-fx-border-width: 0 0 1 0;"
        );

        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-font-size: 15; -fx-text-fill: #7ba3d8; -fx-font-family: " + FONT + ";");

        searchField = new TextField();
        searchField.setPromptText("Buscar caso...");
        searchField.setPrefHeight(36);
        searchField.setStyle(
            "-fx-background-color: #08142e; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 7; " +
            "-fx-background-radius: 7; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 7 12 7 32; " +
            "-fx-text-fill: #d0e4ff; " +
            "-fx-prompt-text-fill: #3a5a8a; " +
            "-fx-font-size: 12; " +
            "-fx-font-family: " + FONT + ";"
        );
        searchField.focusedProperty().addListener((obs, wasFocused, isFocused) -> searchField.setStyle(
            "-fx-background-color: #08142e; " +
            "-fx-border-color: " + (isFocused ? "#3b7de0" : "#1a3a7a") + "; " +
            "-fx-border-radius: 7; " +
            "-fx-background-radius: 7; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 7 12 7 32; " +
            "-fx-text-fill: #d0e4ff; " +
            "-fx-prompt-text-fill: #3a5a8a; " +
            "-fx-font-size: 12; " +
            "-fx-font-family: " + FONT + ";"
        ));
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshGrid(stage));
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Label caseCountLabel = new Label("0 casos");
        caseCountLabel.setStyle(
            "-fx-font-size: 12; " +
            "-fx-text-fill: #7ba3d8; " +
            "-fx-font-family: " + FONT + ";"
        );
        searchField.getProperties().put("caseCountLabel", caseCountLabel);

        batchGroupNewButton = new Button("Crear nuevo grupo");
        batchGroupNewButton.setStyle(
            "-fx-background-color: #2563c8; " +
            "-fx-border-color: #3b7de0; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 6 12 6 12; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 11; " +
            "-fx-font-family: " + FONT + "; " +
            "-fx-cursor: hand;"
        );
        batchGroupNewButton.setOnAction(e -> showBatchJustificationOverlay());

        batchGroupExistingButton = new Button("Agregar a grupo");
        batchGroupExistingButton.setStyle(
            "-fx-background-color: #10b981; " +
            "-fx-border-color: #34d399; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 6 12 6 12; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 11; " +
            "-fx-font-family: " + FONT + "; " +
            "-fx-cursor: hand;"
        );
        batchGroupExistingButton.setOnAction(e -> showAddToGroupOverlay());

        batchButtonsContainer = new HBox(8, batchGroupNewButton, batchGroupExistingButton);
        batchButtonsContainer.setAlignment(Pos.CENTER_LEFT);
        batchButtonsContainer.setVisible(false);
        batchButtonsContainer.setManaged(false);

        HBox searchRow = new HBox(12, searchIcon, searchField, caseCountLabel, batchButtonsContainer);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.setPadding(new Insets(10, 16, 10, 16));
        searchRow.setStyle(
            "-fx-background-color: #0d2459; " +
            "-fx-border-color: transparent transparent #1a3a7a transparent; " +
            "-fx-border-width: 0 0 1 0;"
        );

        Label subtitle = new Label("Selecciona un caso para ver su imagen en detalle.");
        subtitle.setWrapText(true);
        subtitle.setStyle(
            "-fx-font-size: 11; " +
            "-fx-text-fill: #7ba3d8; " +
            "-fx-font-family: " + FONT + ";"
        );

        casesGrid = new TilePane();
        casesGrid.setHgap(14);
        casesGrid.setVgap(14);
        casesGrid.setTileAlignment(Pos.TOP_LEFT);
        casesGrid.setPrefColumns(3);
        casesGrid.setPrefTileWidth(290);
        casesGrid.setPrefTileHeight(210);
        casesGrid.setStyle("-fx-background-color: transparent;");

        ScrollPane gridScroll = new ScrollPane(casesGrid);
        gridScroll.setFitToWidth(true);
        gridScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        gridScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        gridScroll.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-background: transparent; " +
            "-fx-border-color: transparent;"
        );
        VBox.setVgrow(gridScroll, Priority.ALWAYS);

        BorderPane contentShell = new BorderPane();
        contentShell.setStyle("-fx-background-color: #08142e;");
        contentShell.setTop(topRow);
        contentShell.setCenter(new VBox(0, searchRow, subtitle, gridScroll));
        BorderPane.setMargin(subtitle, new Insets(8, 16, 4, 16));
        BorderPane.setMargin(gridScroll, new Insets(0, 16, 16, 16));
        VBox.setVgrow(gridScroll, Priority.ALWAYS);

        modalOverlay = buildModalOverlay(stage);
        modalOverlay.setVisible(false);
        modalOverlay.setManaged(false);

        batchJustificationOverlay = buildBatchJustificationOverlay(stage);
        batchJustificationOverlay.setVisible(false);
        batchJustificationOverlay.setManaged(false);

        addToGroupOverlay = buildAddToGroupOverlay(stage);
        addToGroupOverlay.setVisible(false);
        addToGroupOverlay.setManaged(false);

        view = new StackPane(contentShell, modalOverlay, batchJustificationOverlay, addToGroupOverlay);
        view.setStyle("-fx-background-color: #08142e;");

        refreshGrid(stage);
        CasoRepository.getCasos().addListener((ListChangeListener<Caso>) change -> refreshGrid(stage));

        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> refreshTimer()));
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
        timerTimeline.play();
    }

    public void applyTheme(Scene scene) {
        Theme.apply(scene);
    }

    public Parent getView() {
        return view;
    }

    private void refreshGrid(Stage stage) {
        String query = searchField == null ? "" : searchField.getText();
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();

        var filtered = CasoRepository.getCasos().stream()
                .sorted(Comparator.comparing(Caso::getNombre, String.CASE_INSENSITIVE_ORDER))
                .filter(caso -> normalizedQuery.isEmpty()
                        || containsIgnoreCase(caso.getNombre(), normalizedQuery)
                        || containsIgnoreCase(caso.getLugar(), normalizedQuery)
                        || containsIgnoreCase(caso.getDescripcion(), normalizedQuery))
                .toList();

        currentCases = filtered;

        Object countLabel = searchField.getProperties().get("caseCountLabel");
        if (countLabel instanceof Label label) {
            label.setText(filtered.size() + " casos");
        }

        // Deferred rendering: show UI shell immediately, populate cards in next frame
        casesGrid.getChildren().clear();
        final var snapshot = java.util.List.copyOf(filtered);
        javafx.application.Platform.runLater(() -> {
            casesGrid.getChildren().setAll(snapshot.stream()
                    .map(caso -> buildCaseCard(stage, caso))
                    .toList());
        });
    }

    private VBox buildCaseCard(Stage stage, Caso caso) {
        StackPane imageArea = new StackPane();
        imageArea.setMinHeight(140);
        imageArea.setPrefHeight(140);
        imageArea.setMaxHeight(140);
        imageArea.setStyle("-fx-background-color: #06101f; -fx-background-radius: 10 10 0 0;");

        boolean hasCaseImage = caso != null && caso.tieneImagen()
                && caso.getImagenPath() != null
                && cachedFileExists(caso.getImagenPath());

        if (hasCaseImage) {
            ImageView preview = new ImageView(loadCaseImage(caso));
            preview.setPreserveRatio(true);
            preview.fitWidthProperty().bind(imageArea.widthProperty().subtract(8));
            preview.fitHeightProperty().bind(imageArea.heightProperty().subtract(8));
            imageArea.getChildren().add(preview);
        } else {
            Label placeholderIcon = new Label("⬜");
            placeholderIcon.setStyle("-fx-font-size: 28; -fx-text-fill: #2a4a7a; -fx-font-family: " + FONT + ";");
            Label placeholderText = new Label("Sin imagen cargada");
            placeholderText.setStyle("-fx-font-size: 10; -fx-text-fill: #2a4a7a; -fx-font-family: " + FONT + ";");
            VBox placeholder = new VBox(4, placeholderIcon, placeholderText);
            placeholder.setAlignment(Pos.CENTER);
            imageArea.getChildren().add(placeholder);
        }

        int caseIndex = com.prisma.data.CasoRepository.getCasos().indexOf(caso) + 1;
        String formattedNum = String.format("%02d", caseIndex);

        Label numberBadge = new Label(formattedNum);
        numberBadge.setStyle(
            "-fx-background-color: #c8a03b; " +
            "-fx-text-fill: #0a1a3a; " +
            "-fx-font-size: 11; " +
            "-fx-font-weight: bold; " +
            "-fx-min-width: 24; " +
            "-fx-min-height: 24; " +
            "-fx-max-width: 24; " +
            "-fx-max-height: 24; " +
            "-fx-background-radius: 12; " +
            "-fx-alignment: center; " +
            "-fx-font-family: " + FONT + ";"
        );
        StackPane.setAlignment(numberBadge, Pos.TOP_LEFT);
        StackPane.setMargin(numberBadge, new Insets(8, 0, 0, 8));
        imageArea.getChildren().add(numberBadge);

        Label nameLabel = new Label(caso.getNombre());
        nameLabel.setMaxWidth(200);
        nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        nameLabel.setStyle(
            "-fx-font-size: 11; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #d0e4ff; " +
            "-fx-font-family: " + FONT + ";"
        );

        Button copyNameButton = new Button("Copiar");
        copyNameButton.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 5; " +
            "-fx-background-radius: 5; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 4 8 4 8; " +
            "-fx-text-fill: #7ba3d8; " +
            "-fx-font-size: 10; " +
            "-fx-font-family: " + FONT + "; " +
            "-fx-cursor: hand;"
        );
        copyNameButton.setOnAction(e -> copyCaseName(caso));

        Button detailsButton = new Button("Ver");
        detailsButton.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 5; " +
            "-fx-background-radius: 5; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 4 9 4 9; " +
            "-fx-text-fill: #7ba3d8; " +
            "-fx-font-size: 11; " +
            "-fx-font-family: " + FONT + "; " +
            "-fx-cursor: hand;"
        );
        detailsButton.setOnMouseEntered(e -> detailsButton.setStyle(
            "-fx-background-color: #2563c8; " +
            "-fx-border-color: #2563c8; " +
            "-fx-border-radius: 5; " +
            "-fx-background-radius: 5; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 4 9 4 9; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 11; " +
            "-fx-font-family: " + FONT + "; " +
            "-fx-cursor: hand;"
        ));
        detailsButton.setOnMouseExited(e -> detailsButton.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 5; " +
            "-fx-background-radius: 5; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 4 9 4 9; " +
            "-fx-text-fill: #7ba3d8; " +
            "-fx-font-size: 11; " +
            "-fx-font-family: " + FONT + "; " +
            "-fx-cursor: hand;"
        ));
        detailsButton.setOnAction(e -> openCaseModal(stage, caso));

        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.setPadding(new Insets(6, 11, 4, 11));
        
        boolean isGrouped = PlayerViewBrown.getInstance(stage).isCaseGrouped(caso);
        if (isGrouped) {
            Label statusBadge = new Label("🟢 Agrupado");
            statusBadge.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 11; -fx-font-family: " + FONT + ";");
            statusRow.getChildren().add(statusBadge);
        } else {
            javafx.scene.control.CheckBox selectBox = new javafx.scene.control.CheckBox();
            selectBox.setSelected(selectedCasesForBatch.contains(caso));
            selectBox.setStyle("-fx-cursor: hand;");

            Label statusBadge = new Label("⚪ No agrupado");
            statusBadge.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11; -fx-font-family: " + FONT + ";");

            selectBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    selectedCasesForBatch.add(caso);
                } else {
                    selectedCasesForBatch.remove(caso);
                }
                updateBatchButtonState();
            });

            statusRow.getChildren().addAll(selectBox, statusBadge);
        }

        HBox footer = new HBox(8, nameLabel, copyNameButton, detailsButton);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(9, 11, 9, 11));
        footer.setStyle(
            "-fx-border-color: #1a3a7a transparent transparent transparent; " +
            "-fx-border-width: 1 0 0 0;"
        );
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        VBox card = new VBox(imageArea, statusRow, footer);
        card.setPrefWidth(290);
        card.setMinWidth(290);
        card.setMaxWidth(290);
        card.setStyle(
            "-fx-background-color: #0f1e3d; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 10; " +
            "-fx-background-radius: 10; " +
            "-fx-border-width: 1;"
        );
        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: #152240; " +
            "-fx-border-color: #3b7de0; " +
            "-fx-border-radius: 10; " +
            "-fx-background-radius: 10; " +
            "-fx-border-width: 1;"
        ));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color: #0f1e3d; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 10; " +
            "-fx-background-radius: 10; " +
            "-fx-border-width: 1;"
        ));
        return card;
    }

    private StackPane buildModalOverlay(Stage stage) {
        StackPane backdrop = new StackPane();
        backdrop.setStyle("-fx-background-color: rgba(4, 9, 26, 0.82);");
        backdrop.setOnMouseClicked(e -> {
            if (e.getTarget() == backdrop) {
                hideModal();
            }
        });

        modalTitleLabel = new Label();
        modalTitleLabel.setStyle(
            "-fx-font-size: 14; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #f0c96e; " +
            "-fx-font-family: " + FONT + ";"
        );
        HBox.setHgrow(modalTitleLabel, Priority.ALWAYS);

        Label modalSubtitle = new Label();
        modalSubtitle.setStyle(
            "-fx-font-size: 11; " +
            "-fx-text-fill: #7ba3d8; " +
            "-fx-font-family: " + FONT + ";"
        );
        modalTitleLabel.textProperty().addListener((obs, oldText, newText) ->
            modalSubtitle.setText(newText == null ? "" : "Caso: " + newText)
        );

        String zoomBtnNormal =
            "-fx-background-color: #0d2459; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 5; " +
            "-fx-background-radius: 5; " +
            "-fx-border-width: 1; " +
            "-fx-min-width: 28; " +
            "-fx-min-height: 28; " +
            "-fx-pref-width: 28; " +
            "-fx-pref-height: 28; " +
            "-fx-padding: 0; " +
            "-fx-cursor: hand;";
        String zoomBtnHover =
            "-fx-background-color: #1a3a7a; " +
            "-fx-border-color: #3b7de0; " +
            "-fx-border-radius: 5; " +
            "-fx-background-radius: 5; " +
            "-fx-border-width: 1; " +
            "-fx-min-width: 28; " +
            "-fx-min-height: 28; " +
            "-fx-pref-width: 28; " +
            "-fx-pref-height: 28; " +
            "-fx-padding: 0; " +
            "-fx-cursor: hand;";
        String zoomIconStyle =
            "-fx-text-fill: #d0e4ff; " +
            "-fx-font-size: 18; " +
            "-fx-font-weight: bold; " +
            "-fx-font-family: 'Segoe UI', Arial, sans-serif;";

        Button zoomOut = createZoomControlButton("-", zoomIconStyle, zoomBtnNormal, zoomBtnHover);
        Button zoomIn = createZoomControlButton("+", zoomIconStyle, zoomBtnNormal, zoomBtnHover);
        String zoomResetIconStyle =
            "-fx-text-fill: #d0e4ff; " +
            "-fx-font-size: 11; " +
            "-fx-font-weight: bold; " +
            "-fx-font-family: 'Segoe UI', Arial, sans-serif;";
        Button zoomReset = createZoomControlButton("1:1", zoomResetIconStyle, zoomBtnNormal, zoomBtnHover);

        Label zoomPercentLabel = new Label("100%");
        zoomPercentLabel.setMinWidth(42);
        zoomPercentLabel.setAlignment(Pos.CENTER);
        zoomPercentLabel.setStyle(
            "-fx-font-size: 12; " +
            "-fx-text-fill: #d0e4ff; " +
            "-fx-font-family: " + FONT + ";"
        );

        HBox zoomControls = new HBox(8, zoomOut, zoomPercentLabel, zoomReset, zoomIn);
        zoomControls.setAlignment(Pos.CENTER_RIGHT);

        modalLocateButton = new Button("Ubicar caso");
        String locateNormal =
            "-fx-background-color: #0a1d4a; " +
            "-fx-border-color: #2563c8; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 5 12 5 12; " +
            "-fx-text-fill: #a8c8f0; " +
            "-fx-font-size: 12; " +
            "-fx-font-family: " + FONT + "; " +
            "-fx-cursor: hand;";
        String locateHover =
            "-fx-background-color: #0d2e6e; " +
            "-fx-border-color: #2563c8; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 5 12 5 12; " +
            "-fx-text-fill: #a8c8f0; " +
            "-fx-font-size: 12; " +
            "-fx-font-family: " + FONT + "; " +
            "-fx-cursor: hand;";
        modalLocateButton.setStyle(locateNormal);
        modalLocateButton.setOnMouseEntered(e -> modalLocateButton.setStyle(locateHover));
        modalLocateButton.setOnMouseExited(e -> modalLocateButton.setStyle(locateNormal));
        modalLocateButton.setOnAction(e -> openAnalyticalBoardForCurrentCase(stage));

        Button close = new Button("Cerrar");
        String closeNormal =
            "-fx-background-color: #3a1010; " +
            "-fx-border-color: #8b2020; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 5 12 5 12; " +
            "-fx-text-fill: #ffb0b0; " +
            "-fx-font-size: 12; " +
            "-fx-font-family: " + FONT + "; " +
            "-fx-cursor: hand;";
        String closeHover =
            "-fx-background-color: #5a1a1a; " +
            "-fx-border-color: #8b2020; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 5 12 5 12; " +
            "-fx-text-fill: #ffb0b0; " +
            "-fx-font-size: 12; " +
            "-fx-font-family: " + FONT + "; " +
            "-fx-cursor: hand;";
        close.setStyle(closeNormal);
        close.setOnMouseEntered(e -> close.setStyle(closeHover));
        close.setOnMouseExited(e -> close.setStyle(closeNormal));
        close.setOnAction(e -> hideModal());

        zoomOut.setOnAction(e -> applyModalZoom(0.88));
        zoomReset.setOnAction(e -> resetModalImageViewTransform());
        zoomIn.setOnAction(e -> applyModalZoom(1.12));

        HBox headerActions = new HBox(10, zoomControls, modalLocateButton, close);
        headerActions.setAlignment(Pos.CENTER_RIGHT);

        HBox modalHeader = new HBox(14, modalTitleLabel, headerActions);
        modalHeader.setAlignment(Pos.CENTER_LEFT);
        modalHeader.setPadding(new Insets(13, 18, 13, 18));
        modalHeader.setStyle(
            "-fx-background-color: #0a1a3a; " +
            "-fx-border-color: transparent transparent #1a3a7a transparent; " +
            "-fx-border-width: 0 0 1 0;"
        );

        modalHintLabel = new Label("Arrastra para mover · Rueda para hacer zoom");
        modalHintLabel.setStyle(
            "-fx-font-size: 11; " +
            "-fx-text-fill: #7ba3d8; " +
            "-fx-background-color: #0d1f45; " +
            "-fx-background-radius: 20; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 20; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 6 14 6 14; " +
            "-fx-font-family: " + FONT + ";"
        );
        StackPane.setAlignment(modalHintLabel, Pos.BOTTOM_CENTER);
        StackPane.setMargin(modalHintLabel, new Insets(0, 0, 12, 0));

        modalImageView = new ImageView();
        modalImageView.setPreserveRatio(true);
        modalImageView.setSmooth(true);
        modalImageView.setCache(true);

        modalImageViewport = new StackPane(modalImageView);
        modalImageViewport.setAlignment(Pos.CENTER);
        modalImageViewport.setMinSize(360, 280);
        modalImageViewport.setPrefSize(720, 480);
        modalImageViewport.setStyle("-fx-background-color: #04091a;");
        modalImageViewport.getProperties().put("zoomPercentLabel", zoomPercentLabel);
        Rectangle viewportClip = new Rectangle();
        viewportClip.widthProperty().bind(modalImageViewport.widthProperty());
        viewportClip.heightProperty().bind(modalImageViewport.heightProperty());
        modalImageViewport.setClip(viewportClip);

        modalImageViewport.addEventFilter(ScrollEvent.SCROLL, event -> {
            double factor = event.getDeltaY() > 0 ? 1.1 : 0.9;
            modalZoom = clamp(modalZoom * factor, 0.2, 5.0);
            applyZoomToModalImage();
            event.consume();
        });

        modalImageViewport.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (!event.isPrimaryButtonDown()) {
                return;
            }
            dragAnchorX = event.getSceneX();
            dragAnchorY = event.getSceneY();
            dragStartTranslateX = modalImageView.getTranslateX();
            dragStartTranslateY = modalImageView.getTranslateY();
            modalImageViewport.setCursor(Cursor.CLOSED_HAND);
            event.consume();
        });

        modalImageViewport.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!event.isPrimaryButtonDown()) {
                return;
            }
            modalImageView.setTranslateX(dragStartTranslateX + (event.getSceneX() - dragAnchorX));
            modalImageView.setTranslateY(dragStartTranslateY + (event.getSceneY() - dragAnchorY));
            event.consume();
        });

        modalImageViewport.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            modalImageViewport.setCursor(Cursor.OPEN_HAND);
        });

        modalImageViewport.setCursor(Cursor.OPEN_HAND);

        VBox emptyPlaceholder = new VBox(8);
        emptyPlaceholder.setAlignment(Pos.CENTER);
        emptyPlaceholder.setMouseTransparent(true);
        Label emptyIcon = new Label("🖼");
        emptyIcon.setStyle("-fx-font-size: 40; -fx-text-fill: #2a4a7a; -fx-font-family: " + FONT + ";");
        Label emptyText = new Label("Imagen del caso");
        emptyText.setStyle("-fx-font-size: 13; -fx-text-fill: #2a4a7a; -fx-font-family: " + FONT + ";");
        emptyPlaceholder.getChildren().addAll(emptyIcon, emptyText);

        modalHintLabel.setMouseTransparent(true);

        StackPane viewerStack = new StackPane(modalImageViewport, modalHintLabel, emptyPlaceholder);
        StackPane.setAlignment(emptyPlaceholder, Pos.CENTER);
        emptyPlaceholder.visibleProperty().bind(modalImageView.imageProperty().isNull());
        emptyPlaceholder.managedProperty().bind(modalImageView.imageProperty().isNull());

        modalCard = new VBox(modalHeader, viewerStack);
        modalCard.setMaxWidth(980);
        modalCard.setMaxHeight(820);
        modalCard.setStyle(
            "-fx-background-color: #0d1f45; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 12; " +
            "-fx-background-radius: 12; " +
            "-fx-border-width: 1;"
        );

        String arrowStyle = 
            "-fx-background-color: rgba(13, 36, 89, 0.7); " +
            "-fx-border-color: rgba(59, 125, 224, 0.5); " +
            "-fx-border-width: 1.5; " +
            "-fx-background-radius: 25; " +
            "-fx-border-radius: 25; " +
            "-fx-text-fill: #d0e4ff; " +
            "-fx-font-size: 28; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 0 0 5 0; " +
            "-fx-min-width: 50; " +
            "-fx-min-height: 50; " +
            "-fx-pref-width: 50; " +
            "-fx-pref-height: 50; " +
            "-fx-cursor: hand; " +
            "-fx-alignment: center;";
        String arrowHoverStyle = 
            "-fx-background-color: rgba(37, 99, 200, 0.9); " +
            "-fx-border-color: #3b7de0; " +
            "-fx-border-width: 1.5; " +
            "-fx-background-radius: 25; " +
            "-fx-border-radius: 25; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 28; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 0 0 5 0; " +
            "-fx-min-width: 50; " +
            "-fx-min-height: 50; " +
            "-fx-pref-width: 50; " +
            "-fx-pref-height: 50; " +
            "-fx-cursor: hand; " +
            "-fx-alignment: center;";

        Button prevButton = new Button("‹");
        prevButton.setStyle(arrowStyle);
        prevButton.setOnMouseEntered(e -> prevButton.setStyle(arrowHoverStyle));
        prevButton.setOnMouseExited(e -> prevButton.setStyle(arrowStyle));
        prevButton.setOnAction(e -> navigateCase(-1, stage));

        Button nextButton = new Button("›");
        nextButton.setStyle(arrowStyle);
        nextButton.setOnMouseEntered(e -> nextButton.setStyle(arrowHoverStyle));
        nextButton.setOnMouseExited(e -> nextButton.setStyle(arrowStyle));
        nextButton.setOnAction(e -> navigateCase(1, stage));

        StackPane.setAlignment(prevButton, Pos.CENTER_LEFT);
        StackPane.setAlignment(nextButton, Pos.CENTER_RIGHT);
        StackPane.setMargin(prevButton, new Insets(0, 0, 0, 40));
        StackPane.setMargin(nextButton, new Insets(0, 40, 0, 0));

        StackPane modalRoot = new StackPane(backdrop, modalCard, prevButton, nextButton);
        StackPane.setAlignment(modalCard, Pos.CENTER);
        return modalRoot;
    }

    private void navigateCase(int offset, Stage stage) {
        if (currentCases == null || currentCases.isEmpty() || modalCurrentCase == null) {
            return;
        }
        int index = currentCases.indexOf(modalCurrentCase);
        if (index == -1) {
            return;
        }
        int newIndex = index + offset;
        if (newIndex < 0) {
            newIndex = currentCases.size() - 1;
        } else if (newIndex >= currentCases.size()) {
            newIndex = 0;
        }
        Caso targetCase = currentCases.get(newIndex);
        openCaseModal(stage, targetCase);
    }

    private void openCaseModal(Stage stage, Caso caso) {
        modalCurrentCase = caso;
        int caseIndex = com.prisma.data.CasoRepository.getCasos().indexOf(caso) + 1;
        String formattedNum = String.format("%02d", caseIndex);
        modalTitleLabel.setText(formattedNum + " - " + caso.getNombre());
        modalHintLabel.setText("Arrastra para mover · Rueda para hacer zoom");
        modalHintLabel.setOpacity(1.0);
        modalHintLabel.setVisible(true);
        modalHintLabel.setManaged(true);

        Image image = loadCaseImage(caso);
        modalImageView.setImage(image);
        configureModalImageLayout(image, stage);

        FadeTransition hintFade = new FadeTransition(Duration.seconds(1), modalHintLabel);
        hintFade.setFromValue(1.0);
        hintFade.setToValue(0.0);
        hintFade.setDelay(Duration.seconds(3));
        hintFade.setOnFinished(e -> modalHintLabel.setVisible(false));
        hintFade.play();

        modalOverlay.setVisible(true);
        modalOverlay.setManaged(true);
        modalOverlay.toFront();
    }

    private void hideModal() {
        modalOverlay.setVisible(false);
        modalOverlay.setManaged(false);
        modalImageView.setImage(null);
        resetModalImageViewTransform();
        modalCurrentCase = null;
    }

    private void copyCaseName(Caso caso) {
        if (caso == null || caso.getNombre() == null || caso.getNombre().isBlank()) {
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(caso.getNombre());
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void openAnalyticalBoardForCurrentCase(Stage stage) {
        if (modalCurrentCase == null) {
            return;
        }
        PlayerViewBrown playerViewBrown = PlayerViewBrown.getInstance(stage);
        javafx.scene.Parent view = playerViewBrown.getView();
        if (view.getScene() != null) {
            view.getScene().setRoot(new javafx.scene.layout.Pane());
        }
        Scene scene = new Scene(view, 1500, 900);
        playerViewBrown.applyTheme(scene);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setFullScreen(true);
        playerViewBrown.focusCase(modalCurrentCase.getNombre());
    }

    private Image loadCaseImage(Caso caso) {
        if (caso != null && caso.tieneImagen()) {
            String imgPath = caso.getImagenPath();
            if (imgPath != null && cachedFileExists(imgPath)) {
                return imageCache.computeIfAbsent(imgPath, key -> {
                    String uri = Path.of(key).toUri().toString();
                    // Async background loading with thumbnail resolution for cards
                    return new Image(uri, 580, 280, true, true, true);
                });
            }
        }
        return imageCache.computeIfAbsent("__default__", key ->
                new Image(getClass().getResourceAsStream("/styles/assets/fondo-case.jpeg")));
    }

    /** Cached file-existence check to avoid repeated disk I/O per card render. */
    private boolean cachedFileExists(String path) {
        return fileExistsCache.computeIfAbsent(path, key -> {
            try {
                return Files.exists(Path.of(key));
            } catch (Exception e) {
                return false;
            }
        });
    }

    private void refreshTimer() {
        timerLabel.setText(InvestigationClock.formatRemaining());
        if (InvestigationClock.isCritical()) {
            timerLabel.setStyle(timerStyle(true));
        } else {
            timerLabel.setStyle(timerStyle(false));
        }
    }

    private String timerStyle(boolean critical) {
        return "-fx-font-size: 13; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #ffb0b0; " +
            "-fx-font-family: 'Consolas', 'Segoe UI', monospace;";
    }

    private void configureModalImageLayout(Image image, Stage stage) {
        Runnable apply = () -> {
            double w = image.getWidth();
            double h = image.getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }

            double maxW = Math.min(920, stage.getWidth() * 0.82);
            double maxH = Math.min(640, stage.getHeight() * 0.72);
            double minW = 360;
            double minH = 280;

            double scale = Math.min(1.0, Math.min(maxW / w, maxH / h));
            if (w * scale < minW || h * scale < minH) {
                scale = Math.max(scale, Math.min(minW / w, minH / h));
            }

            modalBaseFitWidth = w * scale;
            modalBaseFitHeight = h * scale;

            modalImageView.setFitWidth(modalBaseFitWidth);
            modalImageView.setFitHeight(modalBaseFitHeight);

            if (modalImageViewport != null) {
                modalImageViewport.setPrefSize(modalBaseFitWidth, modalBaseFitHeight);
                modalImageViewport.setMinSize(modalBaseFitWidth, modalBaseFitHeight);
                modalImageViewport.setMaxSize(modalBaseFitWidth, modalBaseFitHeight);
            }
            if (modalCard != null) {
                modalCard.setPrefWidth(modalBaseFitWidth);
            }

            resetModalImageViewTransform();
        };

        if (image.getWidth() > 0 && image.getProgress() >= 1) {
            apply.run();
            return;
        }

        ChangeListener<Number> loadListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                if (image.getWidth() > 0 && image.getProgress() >= 1) {
                    apply.run();
                    image.widthProperty().removeListener(this);
                }
            }
        };
        image.widthProperty().addListener(loadListener);
    }

    private void resetModalImageViewTransform() {
        modalZoom = 1.0;
        modalImageView.setTranslateX(0);
        modalImageView.setTranslateY(0);
        applyZoomToModalImage();
    }

    private void applyModalZoom(double factor) {
        modalZoom = clamp(modalZoom * factor, 0.2, 5.0);
        applyZoomToModalImage();
    }

    private static Button createZoomControlButton(
            String symbol,
            String iconStyle,
            String normalStyle,
            String hoverStyle) {
        Label icon = new Label(symbol);
        icon.setStyle(iconStyle);
        icon.setMouseTransparent(true);

        Button button = new Button();
        button.setGraphic(icon);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setStyle(normalStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(normalStyle));
        return button;
    }

    private void applyZoomToModalImage() {
        modalImageView.setFitWidth(modalBaseFitWidth);
        modalImageView.setFitHeight(modalBaseFitHeight);
        modalImageView.setScaleX(modalZoom);
        modalImageView.setScaleY(modalZoom);
        if (modalImageViewport != null) {
            Object zoomLabel = modalImageViewport.getProperties().get("zoomPercentLabel");
            if (zoomLabel instanceof Label label) {
                label.setText(Math.round(modalZoom * 100) + "%");
            }
        }
    }

    private void updateBatchButtonState() {
        int count = selectedCasesForBatch.size();
        boolean hasExistingGroups = !PlayerViewBrown.getInstance(stage).getCurrentClusters().isEmpty();

        batchGroupNewButton.setText("Crear nuevo grupo (" + count + ")");
        batchGroupNewButton.setVisible(count >= 2);
        batchGroupNewButton.setManaged(count >= 2);

        batchGroupExistingButton.setText("Agregar a grupo (" + count + ")");
        batchGroupExistingButton.setVisible(count >= 1 && hasExistingGroups);
        batchGroupExistingButton.setManaged(count >= 1 && hasExistingGroups);

        boolean showContainer = (count >= 2) || (count >= 1 && hasExistingGroups);
        batchButtonsContainer.setVisible(showContainer);
        batchButtonsContainer.setManaged(showContainer);
    }

    private void showBatchJustificationOverlay() {
        batchJustificationOverlay.setVisible(true);
        batchJustificationOverlay.setManaged(true);
        batchJustificationOverlay.toFront();
    }

    private void hideBatchJustificationOverlay() {
        batchJustificationOverlay.setVisible(false);
        batchJustificationOverlay.setManaged(false);
    }

    private StackPane buildBatchJustificationOverlay(Stage stage) {
        StackPane backdrop = new StackPane();
        backdrop.setStyle("-fx-background-color: rgba(4, 9, 26, 0.82);");

        VBox dialog = new VBox(14);
        dialog.setMaxWidth(460);
        dialog.setMaxHeight(Region.USE_PREF_SIZE);
        dialog.setPadding(new Insets(24));
        dialog.setStyle(
            "-fx-background-color: #0b1a3a; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 12; " +
            "-fx-background-radius: 12;"
        );

        Label titleLabel = new Label("Justificar Asociación Múltiple");
        titleLabel.setStyle(
            "-fx-font-size: 15; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #f0c96e; " +
            "-fx-font-family: " + FONT + ";"
        );

        Label subtitleLabel = new Label("Se creará una relación en cadena para los casos seleccionados.");
        subtitleLabel.setStyle(
            "-fx-font-size: 11; " +
            "-fx-text-fill: #7ba3d8; " +
            "-fx-font-family: " + FONT + ";"
        );

        Label basisLabel = new Label("Asociar por:");
        basisLabel.setStyle("-fx-text-fill: #d0e4ff; -fx-font-size: 11; -fx-font-family: " + FONT + ";");

        javafx.scene.control.ComboBox<String> basisBox = new javafx.scene.control.ComboBox<>();
        basisBox.getItems().addAll("Modalidad", "Modus operandi", "Patrón", "Criterio de Conexidad", "Fenomeno criminal", "Otros");
        basisBox.setValue("Modalidad");
        basisBox.setMaxWidth(Double.MAX_VALUE);
        basisBox.setStyle(
            "-fx-background-color: #08142e; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-text-fill: white;"
        );

        Label reasonLabel = new Label("Justificación:");
        reasonLabel.setStyle("-fx-text-fill: #d0e4ff; -fx-font-size: 11; -fx-font-family: " + FONT + ";");

        javafx.scene.control.TextArea reasonField = new javafx.scene.control.TextArea();
        reasonField.setPromptText("Escribe los detalles de la asociación...");
        reasonField.setPrefRowCount(3);
        reasonField.setWrapText(true);
        reasonField.setStyle(
            "-fx-control-inner-background: #08142e; " +
            "-fx-text-fill: white; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6;"
        );

        Button confirmButton = new Button("Confirmar");
        confirmButton.setStyle(
            "-fx-background-color: #22c55e; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-padding: 6 16 6 16; " +
            "-fx-cursor: hand;"
        );
        confirmButton.setOnAction(e -> {
            String reason = reasonField.getText().trim();
            if (reason.isEmpty()) {
                showAlert(javafx.scene.control.Alert.AlertType.WARNING, "Escribe una justificación.");
                return;
            }

            String basis = basisBox.getValue();
            String detail = "";

            java.util.List<Caso> list = new java.util.ArrayList<>(selectedCasesForBatch);
            // Connect them via PlayerView
            PlayerViewBrown.getInstance(stage).createBatchConnections(list, basis, detail, reason);

            selectedCasesForBatch.clear();
            updateBatchButtonState();
            hideBatchJustificationOverlay();
            
            // Redraw grid
            refreshGrid(stage);
            
            showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Asociación en lote creada con éxito en el tablero.");
        });

        Button cancelButton = new Button("Cancelar");
        cancelButton.setStyle(
            "-fx-background-color: #64748b; " +
            "-fx-text-fill: white; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-padding: 6 16 6 16; " +
            "-fx-cursor: hand;"
        );
        cancelButton.setOnAction(e -> {
            hideBatchJustificationOverlay();
        });

        HBox actions = new HBox(12, cancelButton, confirmButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        dialog.getChildren().addAll(
            titleLabel,
            subtitleLabel,
            basisLabel,
            basisBox,
            reasonLabel,
            reasonField,
            actions
        );

        backdrop.getChildren().add(dialog);
        return backdrop;
    }

    private void showAlert(javafx.scene.control.Alert.AlertType type, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(type);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && query != null && value.toLowerCase().contains(query);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private String boardStylesheet() {
        return java.util.Objects.requireNonNull(getClass().getResource("/styles/board-brown.css")).toExternalForm();
    }

    private void showAddToGroupOverlay() {
        Object comboObj = addToGroupOverlay.getProperties().get("groupComboBox");
        if (comboObj instanceof javafx.scene.control.ComboBox combo) {
            combo.getItems().setAll(PlayerViewBrown.getInstance(stage).getCurrentClusters());
            if (!combo.getItems().isEmpty()) {
                combo.setValue((PlayerView.GroupCluster) combo.getItems().get(0));
            }
        }
        Object reasonObj = addToGroupOverlay.getProperties().get("reasonField");
        if (reasonObj instanceof javafx.scene.control.TextArea reasonField) {
            reasonField.clear();
        }

        addToGroupOverlay.setVisible(true);
        addToGroupOverlay.setManaged(true);
        addToGroupOverlay.toFront();
    }

    private void hideAddToGroupOverlay() {
        addToGroupOverlay.setVisible(false);
        addToGroupOverlay.setManaged(false);
    }

    private StackPane buildAddToGroupOverlay(Stage stage) {
        StackPane backdrop = new StackPane();
        backdrop.setStyle("-fx-background-color: rgba(4, 9, 26, 0.82);");

        VBox dialog = new VBox(14);
        dialog.setMaxWidth(460);
        dialog.setMaxHeight(Region.USE_PREF_SIZE);
        dialog.setPadding(new Insets(24));
        dialog.setStyle(
            "-fx-background-color: #0b1a3a; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 12; " +
            "-fx-background-radius: 12;"
        );

        Label titleLabel = new Label("Agregar a Grupo Existente");
        titleLabel.setStyle(
            "-fx-font-size: 15; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #f0c96e; " +
            "-fx-font-family: " + FONT + ";"
        );

        Label subtitleLabel = new Label("Se asociarán los casos seleccionados al grupo elegido.");
        subtitleLabel.setStyle(
            "-fx-font-size: 11; " +
            "-fx-text-fill: #7ba3d8; " +
            "-fx-font-family: " + FONT + ";"
        );

        Label groupLabel = new Label("Seleccionar Grupo:");
        groupLabel.setStyle("-fx-text-fill: #d0e4ff; -fx-font-size: 11; -fx-font-family: " + FONT + ";");

        javafx.scene.control.ComboBox<PlayerView.GroupCluster> groupComboBox = new javafx.scene.control.ComboBox<>();
        groupComboBox.setMaxWidth(Double.MAX_VALUE);
        groupComboBox.setStyle(
            "-fx-background-color: #08142e; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-text-fill: white;"
        );

        Label basisLabel = new Label("Asociar por:");
        basisLabel.setStyle("-fx-text-fill: #d0e4ff; -fx-font-size: 11; -fx-font-family: " + FONT + ";");

        javafx.scene.control.ComboBox<String> basisBox = new javafx.scene.control.ComboBox<>();
        basisBox.getItems().addAll("Modalidad", "Modus operandi", "Patrón", "Criterio de Conexidad", "Fenomeno criminal", "Otros");
        basisBox.setValue("Modalidad");
        basisBox.setMaxWidth(Double.MAX_VALUE);
        basisBox.setStyle(
            "-fx-background-color: #08142e; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-text-fill: white;"
        );

        Label reasonLabel = new Label("Justificación:");
        reasonLabel.setStyle("-fx-text-fill: #d0e4ff; -fx-font-size: 11; -fx-font-family: " + FONT + ";");

        javafx.scene.control.TextArea reasonField = new javafx.scene.control.TextArea();
        reasonField.setPromptText("Escribe los detalles de la asociación...");
        reasonField.setPrefRowCount(3);
        reasonField.setWrapText(true);
        reasonField.setStyle(
            "-fx-control-inner-background: #08142e; " +
            "-fx-text-fill: white; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6;"
        );

        Button confirmButton = new Button("Confirmar");
        confirmButton.setStyle(
            "-fx-background-color: #22c55e; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-padding: 6 16 6 16; " +
            "-fx-cursor: hand;"
        );
        confirmButton.setOnAction(e -> {
            PlayerView.GroupCluster targetGroup = groupComboBox.getValue();
            if (targetGroup == null) {
                showAlert(javafx.scene.control.Alert.AlertType.WARNING, "Selecciona un grupo existente.");
                return;
            }

            String reason = reasonField.getText().trim();
            if (reason.isEmpty()) {
                showAlert(javafx.scene.control.Alert.AlertType.WARNING, "Escribe una justificación.");
                return;
            }

            String basis = basisBox.getValue();
            String detail = "";

            java.util.List<Caso> list = new java.util.ArrayList<>(selectedCasesForBatch);
            // Connect/Add them via PlayerView
            PlayerViewBrown.getInstance(stage).addCasesToGroup(list, targetGroup, basis, detail, reason);

            selectedCasesForBatch.clear();
            updateBatchButtonState();
            hideAddToGroupOverlay();

            // Redraw grid
            refreshGrid(stage);

            showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Casos agregados al grupo con éxito.");
        });

        Button cancelButton = new Button("Cancelar");
        cancelButton.setStyle(
            "-fx-background-color: #64748b; " +
            "-fx-text-fill: white; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-padding: 6 16 6 16; " +
            "-fx-cursor: hand;"
        );
        cancelButton.setOnAction(e -> {
            hideAddToGroupOverlay();
        });

        HBox actions = new HBox(12, cancelButton, confirmButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        dialog.getChildren().addAll(
            titleLabel,
            subtitleLabel,
            groupLabel,
            groupComboBox,
            basisLabel,
            basisBox,
            reasonLabel,
            reasonField,
            actions
        );

        backdrop.getChildren().add(dialog);

        backdrop.getProperties().put("groupComboBox", groupComboBox);
        backdrop.getProperties().put("reasonField", reasonField);

        return backdrop;
    }
}
