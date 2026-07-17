package com.prisma.views;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.prisma.data.CasoRepository;
import com.prisma.models.Caso;
import com.prisma.ui.Theme;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ListChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;

public class CasesManagementBrownView {
    private static final String FONT = "'Segoe UI'";
    private static final Path ONBOARDING_MARKER = Path.of(
            System.getProperty("user.home"), "Documents", "NEXUS", "cases-management-onboarding-seen.flag");
    private static boolean onboardingCompleted = false;

    private final StackPane view;
    private final TilePane casesGrid;
    private ScrollPane gridScroll;
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

    // First-time onboarding
    private StackPane onboardingOverlay;
    private Pane onboardingFloatLayer;
    private VBox onboardingDialog;
    private Label onboardingMessageLabel;
    private Rectangle onboardingDimLayer;
    private Rectangle onboardingSpotlight;
    private Rectangle onboardingFocusRing;
    private boolean onboardingPendingStart;
    private boolean onboardingInProgress;
    private boolean captureOnboardingTargets;
    private Node onboardingVerTarget;
    private Node onboardingCheckboxTarget;
    private Node onboardingSecondCheckboxTarget;
    private Node onboardingHighlightedTarget;
    private Caso onboardingDemoCaseFirst;
    private Caso onboardingDemoCaseSecond;
    private int onboardingStep;

    // Performance caches
    private final Map<String, Boolean> fileExistsCache = new HashMap<>();
    private final Map<String, Image> imageCache = new HashMap<>();
    private final Map<String, Image> fullImageCache = new HashMap<>();
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
            "-fx-background-color: #0A1128; " +
            "-fx-border-color: transparent transparent #1a3a7a transparent; " +
            "-fx-border-width: 0 0 1 0;"
        );

        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-font-size: 15; -fx-text-fill: #7ba3d8; -fx-font-family: " + FONT + ";");

        searchField = new TextField();
        searchField.setPromptText("Buscar por número de caso o nombre...");
        searchField.setPrefHeight(40);
        searchField.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 7; " +
            "-fx-background-radius: 7; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 7 12 7 32; " +
            "-fx-text-fill: #000000; " +
            "-fx-prompt-text-fill: #64748B; " +
            "-fx-font-size: 14; " +
            "-fx-font-family: " + FONT + ";"
        );
        searchField.focusedProperty().addListener((obs, wasFocused, isFocused) -> searchField.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-border-color: " + (isFocused ? "#F1C40F" : "#1a3a7a") + "; " +
            "-fx-border-radius: 7; " +
            "-fx-background-radius: 7; " +
            "-fx-border-width: " + (isFocused ? "2" : "1") + "; " +
            "-fx-padding: 7 12 7 32; " +
            "-fx-text-fill: #000000; " +
            "-fx-prompt-text-fill: #64748B; " +
            "-fx-font-size: 14; " +
            "-fx-font-family: " + FONT + ";"
        ));
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshGrid(stage));
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Label caseCountLabel = new Label("0 casos");
        caseCountLabel.setStyle(
            "-fx-font-size: 14; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-font-family: " + FONT + ";"
        );
        searchField.getProperties().put("caseCountLabel", caseCountLabel);

        batchGroupNewButton = new Button("AGRUPAR SELECCIONADAS");
        batchGroupNewButton.setStyle(
            "-fx-background-color: #64748B; " +
            "-fx-background-radius: 6; " +
            "-fx-padding: 10 20 10 20; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 14; " +
            "-fx-font-family: " + FONT + "; " +
            "-fx-cursor: hand;"
        );
        batchGroupNewButton.setOnAction(e -> showBatchJustificationOverlay());

        batchGroupExistingButton = new Button("DESHACER SELECCIÓN");
        batchGroupExistingButton.setStyle(
            "-fx-background-color: #E00A1A; " +
            "-fx-background-radius: 6; " +
            "-fx-padding: 10 20 10 20; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 14; " +
            "-fx-font-family: " + FONT + "; " +
            "-fx-cursor: hand;"
        );
        batchGroupExistingButton.setOnAction(e -> {
            selectedCasesForBatch.clear();
            refreshGrid(stage);
            updateBatchButtonState();
        });

        Label actionTitle = new Label("ACCIONES DE AGRUPACIÓN");
        actionTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 20 6 20; -fx-font-size: 14; -fx-font-family: " + FONT + ";");
        VBox actionTitleBox = new VBox(actionTitle);
        actionTitleBox.setAlignment(Pos.CENTER);
        actionTitleBox.setStyle("-fx-background-color: #E00A1A; -fx-background-radius: 8 8 0 0;");

        HBox actionButtonsBox = new HBox(12, batchGroupNewButton, batchGroupExistingButton);
        actionButtonsBox.setAlignment(Pos.CENTER);
        actionButtonsBox.setPadding(new Insets(16));
        actionButtonsBox.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 8 8;");

        VBox bottomActionBarBox = new VBox(actionTitleBox, actionButtonsBox);
        bottomActionBarBox.setMaxWidth(500);

        batchButtonsContainer = new HBox(bottomActionBarBox);
        batchButtonsContainer.setAlignment(Pos.CENTER);
        batchButtonsContainer.setPadding(new Insets(10));
        batchButtonsContainer.setVisible(false);
        batchButtonsContainer.setManaged(false);

        HBox searchRow = new HBox(12, searchIcon, searchField, caseCountLabel);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.setPadding(new Insets(10, 16, 10, 16));
        searchRow.setStyle("-fx-background-color: #0A1128;");

        Label subtitle = new Label("Selecciona las carpetas que deseas agrupar o ver detalles");
        subtitle.setWrapText(true);
        subtitle.setStyle(
            "-fx-font-size: 14; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-font-family: " + FONT + ";"
        );

        casesGrid = new TilePane();
        casesGrid.setHgap(14);
        casesGrid.setVgap(14);
        casesGrid.setTileAlignment(Pos.TOP_LEFT);
        casesGrid.setPrefColumns(3);
        casesGrid.setPrefTileWidth(290);
        casesGrid.setPrefTileHeight(250);
        casesGrid.setStyle("-fx-background-color: transparent;");

        gridScroll = new ScrollPane(casesGrid);
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
        contentShell.setStyle("-fx-background-color: #0A1128;");
        contentShell.setTop(new VBox(0, topRow, searchRow));
        contentShell.setBottom(batchButtonsContainer);
        contentShell.setCenter(new VBox(0, subtitle, gridScroll));
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

        onboardingOverlay = buildOnboardingOverlay();
        onboardingOverlay.setVisible(false);
        onboardingOverlay.setManaged(false);

        view = new StackPane(contentShell, modalOverlay, batchJustificationOverlay, addToGroupOverlay, onboardingOverlay);
        view.setStyle("-fx-background-color: #08142e;");

        onboardingPendingStart = shouldShowOnboarding();
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
        captureOnboardingTargets = onboardingPendingStart || onboardingInProgress;
        if (!onboardingInProgress) {
            onboardingVerTarget = null;
            onboardingCheckboxTarget = null;
            onboardingSecondCheckboxTarget = null;
            onboardingDemoCaseFirst = null;
            onboardingDemoCaseSecond = null;
        }
        final var snapshot = java.util.List.copyOf(filtered);
        final boolean startOnboarding = onboardingPendingStart && !onboardingInProgress;
        final boolean resumeOnboarding = onboardingInProgress;
        javafx.application.Platform.runLater(() -> {
            casesGrid.getChildren().setAll(snapshot.stream()
                    .map(caso -> buildCaseCard(stage, caso))
                    .toList());
            if (startOnboarding) {
                javafx.application.Platform.runLater(this::beginOnboardingIfReady);
            } else if (resumeOnboarding) {
                javafx.application.Platform.runLater(this::resolveOnboardingTargets);
            }
        });
    }

    private void rebuildGridDuringOnboarding() {
        casesGrid.getChildren().setAll(currentCases.stream()
                .map(caso -> buildCaseCard(stage, caso))
                .toList());
        resolveOnboardingTargets();
        updateBatchButtonState();
    }

    private StackPane buildCaseCard(Stage stage, Caso caso) {
        boolean isSelected = selectedCasesForBatch.contains(caso);
        PlayerView.GroupCluster groupCluster = PlayerViewBrown.getInstance(stage).findGroupForCase(caso);
        boolean isGrouped = groupCluster != null;

        // Colors
        String tabColor = isGrouped ? colorToRgb(groupCluster.getColor()) : "#084C8C";
        
        // Folder Tab
        Region folderTab = new Region();
        folderTab.setPrefHeight(20);
        folderTab.setMaxWidth(120);
        folderTab.setStyle("-fx-background-color: " + tabColor + "; -fx-background-radius: 12 12 0 0;");
        
        // Number badge on tab
        String formattedNum = String.format("%02d", com.prisma.data.CasoRepository.getCasos().indexOf(caso) + 1);
        Label numberBadge = new Label(formattedNum);
        numberBadge.setStyle(
            "-fx-background-color: #F1C40F; " +
            "-fx-text-fill: #000000; " +
            "-fx-font-size: 13; " +
            "-fx-font-weight: bold; " +
            "-fx-min-width: 32; " +
            "-fx-min-height: 32; " +
            "-fx-max-width: 32; " +
            "-fx-max-height: 32; " +
            "-fx-background-radius: 16; " +
            "-fx-alignment: center; " +
            "-fx-font-family: " + FONT + ";"
        );
        
        StackPane tabContainer = new StackPane(folderTab, numberBadge);
        StackPane.setAlignment(folderTab, Pos.BOTTOM_LEFT);
        StackPane.setAlignment(numberBadge, Pos.CENTER_RIGHT);
        tabContainer.setAlignment(Pos.BOTTOM_LEFT);
        tabContainer.setPadding(new Insets(0, 10, 0, 0));
        
        // Image Area (White Background)
        StackPane imageArea = new StackPane();
        imageArea.setMinHeight(140);
        imageArea.setPrefHeight(140);
        imageArea.setMaxHeight(140);
        imageArea.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8; -fx-border-color: #E2E8F0; -fx-border-radius: 8;");
        imageArea.setPadding(new Insets(4));

        boolean hasCaseImage = caso != null && caso.tieneImagen() && caso.getImagenPath() != null && cachedFileExists(caso.getImagenPath());
        if (hasCaseImage) {
            ImageView preview = new ImageView(loadCaseImage(caso));
            preview.setPreserveRatio(true);
            preview.fitWidthProperty().bind(imageArea.widthProperty().subtract(16));
            preview.fitHeightProperty().bind(imageArea.heightProperty().subtract(16));
            imageArea.getChildren().add(preview);
        } else {
            FontIcon placeholderIcon = new FontIcon(FontAwesomeSolid.FILE_ALT);
            placeholderIcon.setIconSize(64);
            placeholderIcon.setIconColor(Color.web("#64748B"));
            imageArea.getChildren().add(placeholderIcon);
        }
        
        // Checkbox Overlay
        javafx.scene.control.CheckBox selectBox = new javafx.scene.control.CheckBox();
        selectBox.setSelected(isSelected);
        selectBox.setStyle(
            "-fx-cursor: hand; " +
            "-fx-scale-x: 1.5; " +
            "-fx-scale-y: 1.5;"
        );
        selectBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                selectedCasesForBatch.add(caso);
            } else {
                selectedCasesForBatch.remove(caso);
            }
            refreshGrid(stage);
            updateBatchButtonState();
        });
        
        StackPane checkOverlay = new StackPane(selectBox);
        checkOverlay.setAlignment(Pos.BOTTOM_RIGHT);
        checkOverlay.setPadding(new Insets(0, 10, -15, 0)); // Pull it down over the edge
        checkOverlay.setPickOnBounds(false);

        StackPane imageWithCheck = new StackPane(imageArea, checkOverlay);
        imageWithCheck.setPadding(new Insets(10, 10, 20, 10)); // Extra bottom padding for checkbox overhang

        Label nameLabel = new Label(caso.getNombre());
        nameLabel.setMaxWidth(260);
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        nameLabel.setStyle(
            "-fx-font-size: 16; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #000000; " +
            "-fx-font-family: " + FONT + ";"
        );
        
        Label statusLabel = new Label(isGrouped ? "Carpeta Agrupada" : (isSelected ? "Carpeta Seleccionada" : "Carpeta Individual"));
        statusLabel.setStyle(
            "-fx-font-size: 12; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #64748B; " +
            "-fx-font-family: " + FONT + ";"
        );
        
        VBox titleBox = new VBox(2, nameLabel, statusLabel);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(0, 10, 10, 10));
        titleBox.setStyle("-fx-background-color: #FFFFFF;");

        // Footer Actions
        FontIcon copyIcon = new FontIcon(FontAwesomeSolid.COPY);
        copyIcon.setIconColor(Color.WHITE);
        copyIcon.setIconSize(16);
        Button copyNameButton = new Button("Copiar", copyIcon);
        copyNameButton.setContentDisplay(ContentDisplay.TOP);
        copyNameButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        copyNameButton.setOnAction(e -> copyCaseName(caso));

        FontIcon viewIcon = new FontIcon(FontAwesomeSolid.EYE);
        viewIcon.setIconColor(Color.WHITE);
        viewIcon.setIconSize(16);
        Button detailsButton = new Button("Ver Detalle", viewIcon);
        detailsButton.setContentDisplay(ContentDisplay.TOP);
        detailsButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        detailsButton.setOnAction(e -> openCaseModal(stage, caso));

        HBox footer = new HBox(20, copyNameButton, detailsButton);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(10));
        footer.setStyle("-fx-background-color: #084C8C; -fx-background-radius: 0 0 8 8;");
        
        // If grouped, show group name instead of standard footer or alongside it? 
        // Mockup keeps footer. Let's add group badge on top of image.
        if (isGrouped) {
            Label groupBadge = new Label(groupCluster.getName());
            groupBadge.setStyle("-fx-background-color: " + colorToRgb(groupCluster.getColor()) + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 8 4 8; -fx-background-radius: 4;");
            StackPane.setAlignment(groupBadge, Pos.TOP_LEFT);
            imageArea.getChildren().add(groupBadge);
        }

        VBox cardBody = new VBox(imageWithCheck, titleBox, footer);
        cardBody.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 0 8 8 8;");
        
        VBox fullCard = new VBox(tabContainer, cardBody);
        fullCard.setPrefWidth(290);
        fullCard.setMinWidth(290);
        fullCard.setMaxWidth(290);
        
        String borderStyle = isSelected ? "-fx-border-color: #F1C40F; -fx-border-width: 4; -fx-border-radius: 8;" : "-fx-border-color: transparent; -fx-border-width: 4;";
        fullCard.setStyle(borderStyle);
        fullCard.setOnMouseClicked(e -> {
            // Toggle selection if clicking the card body (not buttons)
            if (!(e.getTarget() instanceof Button) && !(e.getTarget() instanceof FontIcon)) {
                selectBox.setSelected(!selectBox.isSelected());
            }
        });

        StackPane cardWrapper = new StackPane(fullCard);
        cardWrapper.setPrefWidth(290);
        cardWrapper.setMinWidth(290);
        cardWrapper.setMaxWidth(290);
        cardWrapper.getProperties().put("caso", caso);
        return cardWrapper;
    }

    private static String buildDefaultCaseCardStyle(boolean hover) {
        if (hover) {
            return "-fx-background-color: #152240; " +
                "-fx-border-color: #3b7de0; " +
                "-fx-border-radius: 10; " +
                "-fx-background-radius: 10; " +
                "-fx-border-width: 1;";
        }
        return "-fx-background-color: #0f1e3d; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 10; " +
            "-fx-background-radius: 10; " +
            "-fx-border-width: 1;";
    }

    private static String buildGroupedCaseCardStyle(Color groupColor, boolean hover) {
        String borderColor = colorToRgba(groupColor, hover ? 0.95 : 0.80);
        String backgroundColor = colorToRgba(groupColor, hover ? 0.22 : 0.14);
        return "-fx-background-color: " + backgroundColor + "; " +
            "-fx-border-color: " + borderColor + "; " +
            "-fx-border-radius: 10; " +
            "-fx-background-radius: 10; " +
            "-fx-border-width: 2;";
    }

    private static String colorToRgb(Color color) {
        int red = (int) Math.round(color.getRed() * 255);
        int green = (int) Math.round(color.getGreen() * 255);
        int blue = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02x%02x%02x", red, green, blue);
    }

    private static String colorToRgba(Color color, double alpha) {
        int red = (int) Math.round(color.getRed() * 255);
        int green = (int) Math.round(color.getGreen() * 255);
        int blue = (int) Math.round(color.getBlue() * 255);
        return String.format("rgba(%d, %d, %d, %.2f)", red, green, blue, alpha);
    }

    private static String contrastingTextColor(Color background) {
        double luminance = 0.299 * background.getRed()
            + 0.587 * background.getGreen()
            + 0.114 * background.getBlue();
        return luminance > 0.62 ? "#0a1a3a" : "#ffffff";
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

        Image image = loadCaseImageFull(caso);
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

    private Image loadCaseImageFull(Caso caso) {
        if (caso != null && caso.tieneImagen()) {
            String imgPath = caso.getImagenPath();
            if (imgPath != null && cachedFileExists(imgPath)) {
                return fullImageCache.computeIfAbsent(imgPath, key -> {
                    String uri = Path.of(key).toUri().toString();
                    // Async background loading at FULL resolution (no width/height limit)
                    return new Image(uri, 0, 0, true, true, true);
                });
            }
        }
        return fullImageCache.computeIfAbsent("__default__", key ->
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

        if (count > 0) {
            batchGroupNewButton.setStyle(
                "-fx-background-color: #F1C40F; " +
                "-fx-background-radius: 6; " +
                "-fx-padding: 10 20 10 20; " +
                "-fx-text-fill: black; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 14; " +
                "-fx-font-family: " + FONT + "; " +
                "-fx-cursor: hand;"
            );
            batchGroupNewButton.setDisable(false);
            batchGroupExistingButton.setDisable(false);
            batchButtonsContainer.setVisible(true);
            batchButtonsContainer.setManaged(true);
        } else {
            batchGroupNewButton.setStyle(
                "-fx-background-color: #64748B; " +
                "-fx-background-radius: 6; " +
                "-fx-padding: 10 20 10 20; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 14; " +
                "-fx-font-family: " + FONT + ";"
            );
            batchGroupNewButton.setDisable(true);
            batchGroupExistingButton.setDisable(true);
            batchButtonsContainer.setVisible(false);
            batchButtonsContainer.setManaged(false);
        }
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

        VBox dialog = new VBox(18);
        dialog.setMaxWidth(600);
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
            "-fx-font-size: 22; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #f0c96e; " +
            "-fx-font-family: " + FONT + ";"
        );

        Label subtitleLabel = new Label("Se creará una relación en cadena para los casos seleccionados.");
        subtitleLabel.setStyle(
            "-fx-font-size: 14; " +
            "-fx-text-fill: #7ba3d8; " +
            "-fx-font-family: " + FONT + ";"
        );

        Label basisLabel = new Label("Asociar por:");
        basisLabel.setStyle("-fx-text-fill: #d0e4ff; -fx-font-size: 14; -fx-font-weight: bold; -fx-font-family: " + FONT + ";");

        javafx.scene.control.ComboBox<String> basisBox = new javafx.scene.control.ComboBox<>();
        basisBox.getItems().addAll("Modalidad", "Modus operandi", "Patrón", "Criterio de Conexidad", "Fenomeno criminal", "Otros");
        basisBox.setValue("Modalidad");
        basisBox.setMaxWidth(Double.MAX_VALUE);
        basisBox.setStyle(
            "-fx-background-color: #08142e; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-font-size: 14; " +
            "-fx-text-fill: white;"
        );

        Label reasonLabel = new Label("Justificación:");
        reasonLabel.setStyle("-fx-text-fill: #d0e4ff; -fx-font-size: 14; -fx-font-weight: bold; -fx-font-family: " + FONT + ";");

        javafx.scene.control.TextArea reasonField = new javafx.scene.control.TextArea();
        reasonField.setPromptText("Escribe los detalles de la asociación...");
        reasonField.setPrefRowCount(3);
        reasonField.setWrapText(true);
        reasonField.setStyle(
            "-fx-control-inner-background: #08142e; " +
            "-fx-text-fill: white; " +
            "-fx-border-color: #1a3a7a; " +
            "-fx-border-radius: 6; " +
            "-fx-font-size: 14; " +
            "-fx-background-radius: 6;"
        );

        Button confirmButton = new Button("Confirmar");
        confirmButton.setStyle(
            "-fx-background-color: #22c55e; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 14; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-padding: 10 20 10 20; " +
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
            "-fx-font-weight: bold; " +
            "-fx-font-size: 14; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6; " +
            "-fx-padding: 10 20 10 20; " +
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
        backdrop.getProperties().put("justificationDialog", dialog);
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

    private StackPane buildOnboardingOverlay() {
        onboardingDimLayer = new Rectangle();
        onboardingDimLayer.setFill(Color.color(0, 0, 0, 0.82));

        onboardingSpotlight = new Rectangle();
        onboardingSpotlight.setVisible(false);
        onboardingSpotlight.setMouseTransparent(true);

        onboardingFocusRing = new Rectangle();
        onboardingFocusRing.setFill(Color.TRANSPARENT);
        onboardingFocusRing.setStroke(Color.web("#fcd34d"));
        onboardingFocusRing.setStrokeWidth(2.5);
        onboardingFocusRing.setArcWidth(12);
        onboardingFocusRing.setArcHeight(12);
        onboardingFocusRing.setVisible(false);
        onboardingFocusRing.setMouseTransparent(true);
        onboardingFocusRing.setEffect(new javafx.scene.effect.DropShadow(12, Color.web("#f59e0b")));

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
        nextButton.setOnAction(e -> advanceOnboarding());

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
        clickBlocker.setOnMouseClicked(e -> e.consume());

        onboardingFloatLayer = new Pane();
        onboardingFloatLayer.setPickOnBounds(false);
        onboardingFloatLayer.getChildren().addAll(onboardingFocusRing, onboardingDialog);

        StackPane overlay = new StackPane(onboardingDimLayer, onboardingSpotlight, clickBlocker, onboardingFloatLayer);
        overlay.setPickOnBounds(false);

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

    private boolean shouldShowOnboarding() {
        if (onboardingCompleted) {
            return false;
        }
        if (Files.exists(ONBOARDING_MARKER)) {
            onboardingCompleted = true;
            return false;
        }
        Path snapshot = Path.of(
                System.getProperty("user.home"), "Documents", "NEXUS", "active-session-snapshot.json");
        return !Files.exists(snapshot);
    }

    private void beginOnboardingIfReady() {
        if (!onboardingPendingStart) {
            return;
        }
        if (casesGrid.getChildren().isEmpty()) {
            onboardingPendingStart = false;
            return;
        }
        if (onboardingVerTarget == null || onboardingCheckboxTarget == null || onboardingDemoCaseSecond == null) {
            onboardingPendingStart = false;
            return;
        }

        onboardingInProgress = true;
        onboardingOverlay.setVisible(true);
        onboardingOverlay.setManaged(true);
        ensureOnboardingOverlayOnTop();
        showOnboardingStep(1);
    }

    private void advanceOnboarding() {
        showOnboardingStep(onboardingStep + 1);
    }

    private void showOnboardingStep(int step) {
        clearOnboardingHighlight();
        onboardingStep = step;
        ensureOnboardingOverlayOnTop();

        switch (step) {
            case 1 -> showOnboardingIntroStep();
            case 2 -> showOnboardingVerStep();
            case 3 -> showOnboardingCaseModalStep();
            case 4 -> showOnboardingCheckboxStep();
            case 5 -> showOnboardingTwoSelectedStep();
            case 6 -> showOnboardingCreateGroupStep();
            case 7 -> showOnboardingJustificationStep();
            case 8 -> showOnboardingGroupedResultStep();
            case 9 -> transitionToAdminViewOnboarding();
            default -> cleanupOnboardingDemoAndFinish();
        }
    }

    private void transitionToAdminViewOnboarding() {
        onboardingInProgress = false;
        AdminViewNew.onboardingMode = true;
        AdminViewNew adminViewNew = new AdminViewNew(stage);
        Scene scene = new Scene(adminViewNew.getView(), 1500, 900);
        adminViewNew.applyTheme(scene);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setFullScreen(true);
    }

    private void showOnboardingIntroStep() {
        hideModal();
        hideBatchJustificationOverlay();
        onboardingMessageLabel.setText(
                "Empiece leyendo sus casos, para conocer qué tiene en el Despacho");
        onboardingSpotlight.setVisible(false);
        onboardingFocusRing.setVisible(false);
        onboardingDimLayer.setVisible(true);
        onboardingDimLayer.setFill(Color.color(0, 0, 0, 0.82));
        javafx.application.Platform.runLater(this::positionOnboardingDialogCentered);
    }

    private void showOnboardingVerStep() {
        hideModal();
        hideBatchJustificationOverlay();
        onboardingMessageLabel.setText("Presionando aquí podrá ver los detalles del caso");
        scrollNodeIntoView(onboardingVerTarget);
        javafx.application.Platform.runLater(() -> {
            positionSpotlightOn(onboardingVerTarget, 0.10);
            positionFocusRingOn(onboardingVerTarget);
            positionOnboardingDialogBottom();
            applyOnboardingHighlight(onboardingVerTarget);
        });
    }

    private void showOnboardingCaseModalStep() {
        hideBatchJustificationOverlay();
        onboardingMessageLabel.setText("Aquí podrá leer todo lo relevante al caso");
        if (onboardingDemoCaseFirst != null) {
            openCaseModal(stage, onboardingDemoCaseFirst);
        }
        javafx.application.Platform.runLater(() -> {
            ensureOnboardingOverlayOnTop();
            view.applyCss();
            view.layout();
            positionSpotlightOn(modalCard, 0.34);
            positionFocusRingOn(modalCard);
            positionOnboardingDialogBottom();
        });
    }

    private void showOnboardingCheckboxStep() {
        hideModal();
        hideBatchJustificationOverlay();
        selectedCasesForBatch.clear();
        updateBatchButtonState();
        onboardingMessageLabel.setText(
                "Seleccionando los recuadros podrá ir asociando y agrupando los casos");
        scrollNodeIntoView(onboardingCheckboxTarget);
        javafx.application.Platform.runLater(() -> {
            positionSpotlightOn(onboardingCheckboxTarget, 0.09);
            positionFocusRingOn(onboardingCheckboxTarget);
            positionOnboardingDialogBottom();
            applyOnboardingHighlight(onboardingCheckboxTarget);
        });
    }

    private void showOnboardingTwoSelectedStep() {
        hideModal();
        hideBatchJustificationOverlay();
        onboardingMessageLabel.setText("Podrá seleccionar de esta manera para armar los grupos");
        selectedCasesForBatch.clear();
        if (onboardingDemoCaseFirst != null) {
            selectedCasesForBatch.add(onboardingDemoCaseFirst);
        }
        if (onboardingDemoCaseSecond != null) {
            selectedCasesForBatch.add(onboardingDemoCaseSecond);
        }
        rebuildGridDuringOnboarding();
        javafx.application.Platform.runLater(() -> {
            Node firstCheckbox = onboardingCheckboxTarget;
            Node secondCheckbox = onboardingSecondCheckboxTarget;
            scrollNodeIntoView(firstCheckbox);
            positionSpotlightOnNodes(0.14, firstCheckbox, secondCheckbox);
            positionFocusRingOnNodes(firstCheckbox, secondCheckbox);
            positionOnboardingDialogBottom();
            applyOnboardingHighlight(firstCheckbox);
            applyOnboardingHighlight(secondCheckbox);
        });
    }

    private void showOnboardingCreateGroupStep() {
        hideModal();
        hideBatchJustificationOverlay();
        updateBatchButtonState();
        onboardingMessageLabel.setText(
                "Le deberá dar click luego de seleccionar los casos que desea agrupar");
        javafx.application.Platform.runLater(() -> {
            scrollNodeIntoView(batchGroupNewButton);
            positionSpotlightOn(batchGroupNewButton, 0.08);
            positionFocusRingOn(batchGroupNewButton);
            positionOnboardingDialogBottom();
            applyOnboardingHighlight(batchGroupNewButton);
        });
    }

    private void showOnboardingJustificationStep() {
        hideModal();
        showBatchJustificationOverlay();
        onboardingMessageLabel.setText(
                "Aquí deberá justificar las conexiones que acaba de confirmar");
        javafx.application.Platform.runLater(() -> {
            ensureOnboardingOverlayOnTop();
            view.applyCss();
            view.layout();
            Object dialogObj = batchJustificationOverlay.getProperties().get("justificationDialog");
            Node dialog = dialogObj instanceof Node node ? node : batchJustificationOverlay;
            positionSpotlightOn(dialog, 0.36);
            positionFocusRingOn(dialog);
            positionOnboardingDialogBottom();
        });
    }

    private void showOnboardingGroupedResultStep() {
        hideBatchJustificationOverlay();
        createOnboardingDemoGroup();
        rebuildGridDuringOnboarding();
        onboardingMessageLabel.setText("Y así es como formará y asociará los grupos");
        javafx.application.Platform.runLater(() -> {
            Node card1 = findCardWrapperForCase(onboardingDemoCaseFirst);
            Node card2 = findCardWrapperForCase(onboardingDemoCaseSecond);
            if (card1 != null) {
                scrollNodeIntoView(card1);
            }
            positionSpotlightOnNodes(0.22, card1, card2);
            positionFocusRingOnNodes(card1, card2);
            positionOnboardingDialogBottom();
        });
    }

    private void createOnboardingDemoGroup() {
        if (onboardingDemoCaseFirst == null || onboardingDemoCaseSecond == null) {
            return;
        }
        java.util.List<Caso> demoCases = java.util.List.of(onboardingDemoCaseFirst, onboardingDemoCaseSecond);
        PlayerViewBrown.getInstance(stage).removeConnectionsForCases(demoCases);
        PlayerViewBrown.getInstance(stage).createBatchConnections(
                demoCases,
                "Modalidad",
                "",
                "Demostración guiada del despacho");
    }

    private void cleanupOnboardingDemoAndFinish() {
        hideModal();
        hideBatchJustificationOverlay();
        if (onboardingDemoCaseFirst != null && onboardingDemoCaseSecond != null) {
            PlayerViewBrown.getInstance(stage).removeConnectionsForCases(
                    java.util.List.of(onboardingDemoCaseFirst, onboardingDemoCaseSecond));
        }
        selectedCasesForBatch.clear();
        onboardingInProgress = false;
        onboardingDemoCaseFirst = null;
        onboardingDemoCaseSecond = null;
        onboardingSecondCheckboxTarget = null;
        refreshGrid(stage);
        updateBatchButtonState();
        finishOnboarding();
    }

    private void ensureOnboardingOverlayOnTop() {
        if (onboardingOverlay != null && onboardingOverlay.isVisible()) {
            onboardingOverlay.toFront();
        }
    }

    private void resolveOnboardingTargets() {
        onboardingVerTarget = findOnboardingNode("onboardingVer");
        onboardingCheckboxTarget = findOnboardingNode("onboardingCheckbox");
        onboardingSecondCheckboxTarget = findOnboardingNode("onboardingCheckbox2");
    }

    private Node findOnboardingNode(String propertyKey) {
        for (Node card : casesGrid.getChildren()) {
            Node found = findNodeWithProperty(card, propertyKey);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private Node findNodeWithProperty(Node root, String propertyKey) {
        if (Boolean.TRUE.equals(root.getProperties().get(propertyKey))) {
            return root;
        }
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Node found = findNodeWithProperty(child, propertyKey);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private StackPane findCardWrapperForCase(Caso caso) {
        if (caso == null) {
            return null;
        }
        for (Node node : casesGrid.getChildren()) {
            if (node instanceof StackPane wrapper
                    && caso.equals(wrapper.getProperties().get("caso"))) {
                return wrapper;
            }
        }
        return null;
    }

    private void positionSpotlightOn(Node target, double minRadius) {
        if (target == null) {
            onboardingSpotlight.setVisible(false);
            onboardingDimLayer.setVisible(true);
            onboardingDimLayer.setFill(Color.color(0, 0, 0, 0.82));
            return;
        }
        positionSpotlightOnBounds(target.localToScene(target.getBoundsInLocal()), minRadius);
    }

    private void positionSpotlightOnNodes(double minRadius, Node... targets) {
        Bounds bounds = unionSceneBounds(targets);
        if (bounds == null) {
            onboardingSpotlight.setVisible(false);
            onboardingDimLayer.setVisible(true);
            onboardingDimLayer.setFill(Color.color(0, 0, 0, 0.82));
            return;
        }
        positionSpotlightOnBounds(bounds, minRadius);
    }

    private void positionSpotlightOnBounds(Bounds targetBounds, double minRadius) {
        if (targetBounds == null || onboardingOverlay.getWidth() <= 0 || onboardingOverlay.getHeight() <= 0) {
            onboardingSpotlight.setVisible(false);
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
        positionFocusRingOnBounds(target.localToScene(target.getBoundsInLocal()), 8);
    }

    private void positionFocusRingOnNodes(Node... targets) {
        Bounds bounds = unionSceneBounds(targets);
        if (bounds == null) {
            onboardingFocusRing.setVisible(false);
            return;
        }
        positionFocusRingOnBounds(bounds, 10);
    }

    private void positionFocusRingOnBounds(Bounds targetBounds, double padding) {
        if (targetBounds == null || onboardingOverlay.getWidth() <= 0 || onboardingOverlay.getHeight() <= 0) {
            onboardingFocusRing.setVisible(false);
            return;
        }

        Bounds overlayBounds = onboardingOverlay.localToScene(onboardingOverlay.getBoundsInLocal());
        onboardingFocusRing.setLayoutX(targetBounds.getMinX() - overlayBounds.getMinX() - padding);
        onboardingFocusRing.setLayoutY(targetBounds.getMinY() - overlayBounds.getMinY() - padding);
        onboardingFocusRing.setWidth(Math.max(24, targetBounds.getWidth() + padding * 2));
        onboardingFocusRing.setHeight(Math.max(24, targetBounds.getHeight() + padding * 2));
        onboardingFocusRing.setVisible(true);
    }

    private Bounds unionSceneBounds(Node... nodes) {
        if (nodes == null || nodes.length == 0) {
            return null;
        }
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;
        boolean found = false;
        for (Node node : nodes) {
            if (node == null) {
                continue;
            }
            Bounds bounds = node.localToScene(node.getBoundsInLocal());
            minX = Math.min(minX, bounds.getMinX());
            minY = Math.min(minY, bounds.getMinY());
            maxX = Math.max(maxX, bounds.getMaxX());
            maxY = Math.max(maxY, bounds.getMaxY());
            found = true;
        }
        if (!found) {
            return null;
        }
        return new javafx.geometry.BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

    private void positionOnboardingDialogCentered() {
        onboardingDialog.applyCss();
        onboardingDialog.layout();

        double layerW = onboardingFloatLayer.getWidth();
        double layerH = onboardingFloatLayer.getHeight();
        if (layerW <= 0 || layerH <= 0) {
            return;
        }

        double dialogW = onboardingDialog.getWidth();
        double dialogH = onboardingDialog.getHeight();
        onboardingDialog.setLayoutX((layerW - dialogW) / 2.0);
        onboardingDialog.setLayoutY((layerH - dialogH) / 2.0);
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
        double bottomMargin = 32;
        onboardingDialog.setLayoutX(Math.max(24, (layerW - dialogW) / 2.0));
        onboardingDialog.setLayoutY(Math.max(24, layerH - dialogH - bottomMargin));
    }

    private void applyOnboardingHighlight(Node target) {
        if (target == null) {
            return;
        }
        if (target instanceof Button button) {
            if (!button.getProperties().containsKey("onboardingOriginalStyle")) {
                button.getProperties().put("onboardingOriginalStyle", button.getStyle());
            }
            button.setDisable(true);
            button.setStyle(
                "-fx-background-color: #2563c8; " +
                "-fx-border-color: #fcd34d; " +
                "-fx-border-radius: 5; " +
                "-fx-background-radius: 5; " +
                "-fx-border-width: 2; " +
                "-fx-padding: 4 9 4 9; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 11; " +
                "-fx-effect: dropshadow(gaussian, #fcd34d, 18, 0.7, 0, 0); " +
                "-fx-font-family: " + FONT + "; " +
                "-fx-cursor: default;"
            );
            if (onboardingHighlightedTarget == null) {
                onboardingHighlightedTarget = button;
            } else if (onboardingHighlightedTarget instanceof javafx.scene.control.CheckBox) {
                // Keep checkbox as primary tracked target when both are highlighted.
            } else if (onboardingHighlightedTarget != button) {
                button.getProperties().put("onboardingSecondaryHighlight", Boolean.TRUE);
            }
        } else if (target instanceof javafx.scene.control.CheckBox box) {
            if (!box.getProperties().containsKey("onboardingOriginalStyle")) {
                box.getProperties().put("onboardingOriginalStyle", box.getStyle());
            }
            box.setDisable(true);
            box.setStyle(
                "-fx-cursor: default; " +
                "-fx-effect: dropshadow(gaussian, #fcd34d, 16, 0.8, 0, 0);"
            );
            if (onboardingHighlightedTarget == null) {
                onboardingHighlightedTarget = box;
            } else if (onboardingHighlightedTarget != box) {
                box.getProperties().put("onboardingSecondaryHighlight", Boolean.TRUE);
            }
        }
    }

    private void clearOnboardingHighlight() {
        onboardingFocusRing.setVisible(false);
        clearHighlightOnNode(onboardingVerTarget);
        clearHighlightOnNode(onboardingCheckboxTarget);
        clearHighlightOnNode(onboardingSecondCheckboxTarget);
        clearHighlightOnNode(batchGroupNewButton);
        onboardingHighlightedTarget = null;
    }

    private void clearHighlightOnNode(Node target) {
        if (target == null) {
            return;
        }
        if (target instanceof Button button) {
            button.setDisable(false);
            Object original = button.getProperties().remove("onboardingOriginalStyle");
            if (original instanceof String style) {
                button.setStyle(style);
            }
            button.getProperties().remove("onboardingSecondaryHighlight");
        } else if (target instanceof javafx.scene.control.CheckBox box) {
            box.setDisable(false);
            Object original = box.getProperties().remove("onboardingOriginalStyle");
            if (original instanceof String style) {
                box.setStyle(style);
            } else {
                box.setStyle("-fx-cursor: hand;");
            }
            box.setEffect(null);
            box.getProperties().remove("onboardingSecondaryHighlight");
        }
    }

    private void scrollNodeIntoView(Node target) {
        if (gridScroll == null || target == null || casesGrid.getHeight() <= 0) {
            return;
        }

        Bounds targetBounds = target.localToScene(target.getBoundsInLocal());
        Bounds viewportBounds = gridScroll.localToScene(gridScroll.getViewportBounds());
        double contentHeight = casesGrid.getHeight();
        if (contentHeight <= 0) {
            return;
        }

        double delta = 0;
        if (targetBounds.getMaxY() > viewportBounds.getMaxY() - 24) {
            delta = targetBounds.getMaxY() - viewportBounds.getMaxY() + 48;
        } else if (targetBounds.getMinY() < viewportBounds.getMinY() + 24) {
            delta = targetBounds.getMinY() - viewportBounds.getMinY() - 48;
        }
        if (delta != 0) {
            gridScroll.setVvalue(clamp(gridScroll.getVvalue() + delta / contentHeight, 0, 1));
        }
    }

    private void finishOnboarding() {
        clearOnboardingHighlight();
        onboardingFocusRing.setVisible(false);
        hideModal();
        hideBatchJustificationOverlay();
        onboardingOverlay.setVisible(false);
        onboardingOverlay.setManaged(false);
        onboardingPendingStart = false;
        onboardingInProgress = false;
        onboardingCompleted = true;
        try {
            Files.createDirectories(ONBOARDING_MARKER.getParent());
            Files.writeString(ONBOARDING_MARKER, "seen");
        } catch (Exception ignored) {
            // Non-critical marker; onboarding still completes in memory.
        }
    }
}
