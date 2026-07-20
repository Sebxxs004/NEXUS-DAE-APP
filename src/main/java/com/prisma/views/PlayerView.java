package com.prisma.views;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.prisma.data.CasoRepository;
import com.prisma.models.Caso;
import com.prisma.reports.InvestigationReportPdfExporter;
import com.prisma.reports.InvestigationReportPdfExporter.AlertEntry;
import com.prisma.reports.InvestigationReportPdfExporter.ConnectionEntry;
import com.prisma.reports.InvestigationReportPdfExporter.DecisionEntry;
import com.prisma.reports.InvestigationReportPdfExporter.GroupEntry;
import com.prisma.reports.InvestigationReportPdfExporter.IsolatedEntry;
import com.prisma.reports.InvestigationReportPdfExporter.ReportData;
import com.prisma.ui.Theme;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class PlayerView {

    private static final double NODE_DIAMETER = 72;
    private static final double NODE_RADIUS = NODE_DIAMETER / 2.0;
    private static final double TARGET_SPEED = 0.45;
    private static final double REPULSION_DISTANCE = 120.0;
    private static final double REPULSION_STRENGTH = 650.0;
    private static final double GROUP_PADDING = 30.0;
    private static final double WORLD_WIDTH = 2400.0;
    private static final double WORLD_HEIGHT = 1600.0;
    private static final double MIN_BOARD_ZOOM = 0.55;
    private static final double MAX_BOARD_ZOOM = 1.8;
    private static final double BOARD_ZOOM_STEP = 0.12;
    private static final Duration INVESTIGATION_DURATION = Duration.ofHours(2);

    private final BorderPane view;
    private final Pane board;
    private final Group contentContainer;
    private final Pane groupLayer;
    private final Pane connectionLayer;
    private final Pane nodeLayer;

    private final HBox topBar;
    private final HBox modeTabs;
    private final Button analyticalTab;
    private final Button casesTab;
    private final Label timerLabel;
    private final Label sessionLabel;
    private final Label instructionLabel;
    private final Button finishInvestigationButton;
    private final StackPane moduleHost;
    private final VBox boardModule;
    private final VBox boardWrapper;
    private final VBox casesModule;
    private final StackPane connectionDialogOverlay;
    private final StackPane pdfLoadingOverlay;
    private final StackPane isolatedNodesOverlay;
    private final StackPane decisionOverlay;
    private final VBox decisionOptionsContainer;
    private final VBox decisionJustificationsContainer;
    private final ScrollPane decisionJustificationsScroll;
    private final Map<String, CheckBox> decisionOptionCheckboxes = new LinkedHashMap<>();
    private final Map<String, TextArea> decisionJustificationFields = new HashMap<>();
    private final Label decisionOverlayTitle;
    private String pendingDecisionGroupSignature;
    private GroupMeta pendingDecisionMeta;
    private String pendingDecisionGroupName;
    private String pendingDecisionReason;
    private Button pendingDecisionColorButton;
    private final Label connectionDialogTitle;
    private final Label connectionDialogSubtitle;
    private final ComboBox<String> connectionBasisBox;
    private final TextArea connectionReasonField;
    private final VBox isolatedNodesEntriesContainer;
    private final Map<CaseNode, TextArea> isolatedNodeReasonFields = new HashMap<>();
    private final VBox sidebar;
    private final ListView<String> connectionList;
    private final Label connectionsSectionTitle;
    private final ListView<GroupCluster> groupList;
    private final VBox groupCardsContainer;
    private final ScrollPane groupScrollPane;
    private final ColorPicker groupColorPicker;
    private final TextArea groupReasonField;
    private final Label totalNodesLabel;
    private final Label ungroupedNodesLabel;
    private final Label groupSummaryLabel;
    private final Label statusLabel;

    private final List<CaseNode> nodes = new ArrayList<>();
    private final List<Connection> connections = new ArrayList<>();
    private final Map<String, GroupMeta> metadataBySignature = new HashMap<>();
    private final Map<String, GroupOverlay> overlayBySignature = new HashMap<>();
    private final Map<CaseNode, String> groupedNodeSignature = new HashMap<>();
    private final Map<String, String> isolatedNodeJustifications = new HashMap<>();

    private List<GroupCluster> currentClusters = List.of();
    private CaseNode selectedNode;
    private CaseNode pendingConnectionTarget;
    private List<CaseNode> pendingBatchNodes;
    private String selectedGroupSignature;
    private Stage stage;
    private final VBox casesCardsContainer;
    private final TextArea caseDetailArea;
    private final ImageView caseDetailImageView;
    private final ScrollPane caseImageScroll;
    private final TextField caseSearchField;
    private final ListView<String> caseSearchSuggestions;
    private double caseImageScale = 1.0;
    private static final double INITIAL_IMAGE_SCALE = 0.93;
    private static final double MIN_IMAGE_SCALE = 0.25;
    private static final double MAX_IMAGE_SCALE = 4.0;
    private double boardZoom = 1.0;
    private boolean handMode = false;
    private boolean panning = false;
    private double panStartX;
    private double panStartY;
    private double contentTranslateXStart;
    private double contentTranslateYStart;
    private final long investigationStartedAtMillis;
    private final String investigationSessionId;
    private final Path investigationSnapshotPath;
    private boolean investigationFinished = false;
    private boolean finishingInProgress = false;
    private boolean boardModuleShownOnce = false;
    private boolean casesModuleShownOnce = false;
    private String pendingFinalizationReason;

    public PlayerView(Stage stage) {
        this.stage = stage;
        view = new BorderPane();
        view.getStyleClass().add("app-shell");
        view.setPadding(new Insets(14, 14, 14, 14));

        topBar = new HBox(18);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label appMark = new Label("MODO INVESTIGADOR");
        appMark.getStyleClass().add("top-brand");

        Label appSubMark = new Label("Fiscal / Tablero analítico");
        appSubMark.getStyleClass().add("top-brand-subtitle");

        VBox brandBlock = new VBox(2, appMark, appSubMark);
        brandBlock.getStyleClass().add("nav-brand-block");
        brandBlock.setAlignment(Pos.CENTER_LEFT);

        modeTabs = new HBox(10);
        modeTabs.getStyleClass().add("mode-tabs");
        analyticalTab = new Button("Tablero Analítico");
        analyticalTab.getStyleClass().addAll("mode-tab", "mode-tab-active");
        casesTab = new Button("Gestión de Casos");
        casesTab.getStyleClass().add("mode-tab");
        modeTabs.getChildren().addAll(analyticalTab, casesTab);
        analyticalTab.setOnAction(e -> showBoardModule());
        casesTab.setOnAction(e -> showCasesModule());

        caseSearchField = new TextField();
        caseSearchField.setPromptText("Buscar caso...");
        caseSearchField.getStyleClass().addAll("input-field", "case-search-field");
        caseSearchField.setPrefWidth(330);

        Button caseSearchButton = new Button("Buscar");
        caseSearchButton.getStyleClass().add("secondary-button");

        caseSearchSuggestions = new ListView<>();
        caseSearchSuggestions.getStyleClass().add("case-search-results");
        caseSearchSuggestions.setItems(FXCollections.observableArrayList());
        caseSearchSuggestions.setPrefHeight(108);
        caseSearchSuggestions.setMaxHeight(108);
        caseSearchSuggestions.setVisible(false);
        caseSearchSuggestions.setManaged(false);

        HBox searchRow = new HBox(10, caseSearchField, caseSearchButton);
        searchRow.setAlignment(Pos.CENTER);

        VBox searchPanel = new VBox(6, searchRow, caseSearchSuggestions);
        searchPanel.setAlignment(Pos.CENTER);
        searchPanel.setMaxWidth(460);

        caseSearchField.textProperty().addListener((obs, oldValue, newValue) -> updateCaseSearchSuggestions(newValue));
        caseSearchField.setOnAction(e -> searchCaseByQuery(caseSearchField.getText()));
        caseSearchButton.setOnAction(e -> searchCaseByQuery(caseSearchField.getText()));
        caseSearchSuggestions.setOnMouseClicked(e -> {
            String selected = caseSearchSuggestions.getSelectionModel().getSelectedItem();
            if (selected != null) {
                searchCaseByQuery(selected);
            }
        });

        VBox navCenter = new VBox(8, modeTabs, searchPanel);
        navCenter.getStyleClass().add("nav-center-shell");
        navCenter.setAlignment(Pos.CENTER);
        HBox.setHgrow(navCenter, javafx.scene.layout.Priority.ALWAYS);

        investigationStartedAtMillis = System.currentTimeMillis();
        investigationSessionId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        investigationSnapshotPath = Paths.get(System.getProperty("user.home"), "Documents", "NEXUS",
                "active-session-snapshot.json");

        timerLabel = new Label("TIEMPO " + formatDuration(INVESTIGATION_DURATION));
        timerLabel.getStyleClass().add("timer-pill");
        sessionLabel = new Label("Sesión: FISCAL");
        sessionLabel.getStyleClass().add("session-pill");

        finishInvestigationButton = new Button("Terminar investigación");
        finishInvestigationButton.getStyleClass().add("secondary-button");
        finishInvestigationButton.setOnAction(e -> finalizeInvestigation("finalización manual"));

        Button logoutButton = new Button("Cerrar sesión");
        logoutButton.getStyleClass().add("danger-button");
        logoutButton.setOnAction(e -> {
            DistractionAlertManager.stopMonitoring();
            PlayerViewBrown.clearActiveInstance();
            LoginView loginView = new LoginView(stage);
            Scene scene = com.prisma.ui.ResponsiveUtils.createResponsiveScene(loginView.getView(), 1500, 900);
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
});

        HBox topRight = new HBox(12, timerLabel, sessionLabel, finishInvestigationButton, logoutButton);
        topRight.getStyleClass().add("nav-actions");
        topRight.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(brandBlock, javafx.scene.layout.Priority.NEVER);
        HBox.setHgrow(topRight, javafx.scene.layout.Priority.NEVER);
        topBar.getChildren().addAll(brandBlock, navCenter, topRight);

        instructionLabel = new Label(
                "Conecta esferas para crear hipótesis investigativas. Cada componente conectado se convierte en un grupo.");
        instructionLabel.getStyleClass().add("instruction-strip");
        instructionLabel.setMaxWidth(Double.MAX_VALUE);

        moduleHost = new StackPane();
        moduleHost.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        board = new Pane();
        board.getStyleClass().add("board-surface");
        board.setPrefSize(1260, 720);
        board.setMinSize(900, 560);
        board.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        board.setOnScroll(this::handleBoardZoom);
        board.setOnMousePressed(this::handleBoardMousePressed);
        board.setOnMouseDragged(this::handleBoardMouseDragged);
        board.setOnMouseReleased(this::handleBoardMouseReleased);
        Rectangle boardClip = new Rectangle();
        boardClip.widthProperty().bind(board.widthProperty());
        boardClip.heightProperty().bind(board.heightProperty());
        board.setClip(boardClip);

        Image boardBackgroundImage = new Image(getClass().getResourceAsStream("/styles/assets/tablero-analitico.jpeg"));
        ImageView boardBackgroundView = new ImageView(boardBackgroundImage);
        boardBackgroundView.setPreserveRatio(false);
        boardBackgroundView.fitWidthProperty().bind(board.widthProperty());
        boardBackgroundView.fitHeightProperty().bind(board.heightProperty());
        boardBackgroundView.setMouseTransparent(true);

        groupLayer = new Pane();
        groupLayer.setMouseTransparent(true);
        connectionLayer = new Pane();
        connectionLayer.setMouseTransparent(true);
        nodeLayer = new Pane();

        contentContainer = new Group();
        contentContainer.getChildren().addAll(groupLayer, connectionLayer, nodeLayer);

        board.getChildren().addAll(boardBackgroundView, contentContainer);

        boardWrapper = new VBox(12);
        boardWrapper.setPadding(new Insets(0, 16, 0, 0));
        Label boardTitle = new Label("Casos en movimiento");
        boardTitle.getStyleClass().add("section-title");
        statusLabel = new Label("Selecciona dos casos para conectarlos.");
        statusLabel.getStyleClass().add("app-subtitle");
        statusLabel.setWrapText(true);
        HBox boardHeader = new HBox(12, boardTitle, statusLabel);
        boardHeader.setAlignment(Pos.CENTER_LEFT);
        boardHeader.getStyleClass().add("board-header");

        Button zoomOutButton = new Button("-");
        zoomOutButton.getStyleClass().add("secondary-button");
        zoomOutButton.setOnAction(e -> applyBoardZoom(boardZoom - BOARD_ZOOM_STEP));

        Button zoomResetButton = new Button("100%");
        zoomResetButton.getStyleClass().add("secondary-button");
        zoomResetButton.setOnAction(e -> applyBoardZoom(1.0));

        Button zoomInButton = new Button("+");
        zoomInButton.getStyleClass().add("secondary-button");
        zoomInButton.setOnAction(e -> applyBoardZoom(boardZoom + BOARD_ZOOM_STEP));

        Button interactionModeButton = new Button("Modo: Selección");
        interactionModeButton.getStyleClass().add("secondary-button");
        interactionModeButton.setOnAction(e -> {
            handMode = !handMode;
            if (!handMode) {
                panning = false;
            }
            board.setCursor(handMode ? javafx.scene.Cursor.OPEN_HAND : javafx.scene.Cursor.DEFAULT);
            interactionModeButton.setText(handMode ? "Modo: Mano" : "Modo: Selección");
            statusLabel.setText(handMode
                    ? "Modo mano activo: arrastra para mover el tablero."
                    : "Modo selección activo: selecciona dos casos para conectarlos.");
        });

        HBox zoomControls = new HBox(8, interactionModeButton, zoomOutButton, zoomResetButton, zoomInButton);
        zoomControls.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(boardHeader, javafx.scene.layout.Priority.ALWAYS);
        HBox boardToolbar = new HBox(12, boardHeader, zoomControls);
        boardToolbar.setAlignment(Pos.CENTER_LEFT);
        boardWrapper.getChildren().addAll(boardToolbar, board);

        sidebar = new VBox(14);
        sidebar.getStyleClass().add("sidebar-card");
        sidebar.setPrefWidth(380);
        sidebar.setPadding(new Insets(16));

        Label sidebarTitle = new Label("Conexiones y grupos");
        sidebarTitle.getStyleClass().add("section-title");

        connectionList = new ListView<>();
        connectionList.getStyleClass().add("connection-list");
        connectionList.setPrefHeight(190);
        installConnectionListCellFactory();

        groupList = new ListView<>();
        groupList.getStyleClass().add("group-list");
        groupList.setPrefHeight(180);
        groupList.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> onGroupSelected(newValue));

        groupCardsContainer = new VBox(12);
        groupCardsContainer.setFillWidth(true);

        groupScrollPane = new ScrollPane(groupCardsContainer);
        groupScrollPane.getStyleClass().add("group-scroll");
        groupScrollPane.setFitToWidth(true);
        groupScrollPane.setPrefViewportHeight(360);

        totalNodesLabel = new Label("Casos totales: 0");
        totalNodesLabel.getStyleClass().add("node-stat-pill");
        totalNodesLabel.getStyleClass().add("node-stat-pill-total");

        ungroupedNodesLabel = new Label("Casos sin grupo: 0");
        ungroupedNodesLabel.getStyleClass().add("node-stat-pill");
        ungroupedNodesLabel.getStyleClass().add("node-stat-pill-alert");
        ungroupedNodesLabel.setWrapText(true);

        groupSummaryLabel = new Label("Selecciona un grupo para ver sus casos conectados.");
        groupSummaryLabel.getStyleClass().add("app-subtitle");
        groupSummaryLabel.setWrapText(true);

        groupColorPicker = new ColorPicker(Color.web("#38bdf8"));
        groupColorPicker.setVisible(false);
        groupColorPicker.setManaged(false);

        groupReasonField = new TextArea();
        groupReasonField.getStyleClass().add("text-area");
        groupReasonField.setPromptText("Escribe aquí tu justificación...");
        groupReasonField.setPrefRowCount(4);

        Button refreshButton = new Button("Recalcular grupos");
        refreshButton.getStyleClass().add("secondary-button");
        refreshButton.setOnAction(e -> refreshGroups());

        connectionsSectionTitle = new Label("Conexiones (0)");
        connectionsSectionTitle.getStyleClass().add("section-title");

        VBox connectionsCard = new VBox(10, connectionsSectionTitle, connectionList);
        connectionsCard.getStyleClass().add("panel-card");
        connectionsCard.setPadding(new Insets(14));
        VBox.setVgrow(connectionList, javafx.scene.layout.Priority.ALWAYS);

        VBox groupsCard = new VBox(10, new Label("Grupos detectados"), groupScrollPane, refreshButton);
        groupsCard.getStyleClass().add("panel-card");
        groupsCard.setPadding(new Insets(14));
        VBox.setVgrow(groupScrollPane, javafx.scene.layout.Priority.ALWAYS);

        HBox statsRow = new HBox(10, totalNodesLabel, ungroupedNodesLabel);
        statsRow.setFillHeight(true);
        VBox groupsHeader = new VBox(8, new Label("Grupos detectados"), statsRow);
        groupsHeader.getChildren().get(0).getStyleClass().add("section-title");

        groupsCard.getChildren().setAll(groupsHeader, groupScrollPane, refreshButton);

        sidebar.getChildren().addAll(sidebarTitle, connectionsCard, groupsCard);
        VBox.setVgrow(connectionsCard, javafx.scene.layout.Priority.ALWAYS);
        VBox.setVgrow(groupsCard, javafx.scene.layout.Priority.ALWAYS);

        boardModule = new VBox(14, new HBox(14, boardWrapper, sidebar));
        HBox.setHgrow(boardWrapper, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(sidebar, javafx.scene.layout.Priority.NEVER);

        casesCardsContainer = new VBox(12);
        casesCardsContainer.getStyleClass().add("case-cards-container");
        casesCardsContainer.setPadding(new Insets(4));

        ScrollPane casesScroll = new ScrollPane(casesCardsContainer);
        casesScroll.getStyleClass().add("group-scroll");
        casesScroll.setFitToWidth(true);
        casesScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        casesScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        caseDetailImageView = new ImageView();
        caseDetailImageView.setFitWidth(260);
        caseDetailImageView.setFitHeight(180);
        caseDetailImageView.setPreserveRatio(false);
        caseDetailImageView.setSmooth(true);
        caseDetailImageView.setVisible(false);

        caseDetailArea = new TextArea();
        caseDetailArea.getStyleClass().add("text-area");
        caseDetailArea.setEditable(false);
        caseDetailArea.setWrapText(true);
        caseDetailArea.setPrefRowCount(12);

        caseImageScroll = new ScrollPane(new Group(caseDetailImageView));
        caseImageScroll.getStyleClass().add("image-scroll");
        caseImageScroll.setPannable(true);
        caseImageScroll.setFitToWidth(true);
        caseImageScroll.setFitToHeight(true);
        caseImageScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        caseImageScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        caseImageScroll.setVisible(false);
        caseImageScroll.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox.setVgrow(caseImageScroll, javafx.scene.layout.Priority.ALWAYS);

        caseImageScroll.addEventFilter(ScrollEvent.SCROLL, ev -> {
            if (ev.isControlDown()) {
                double delta = ev.getDeltaY() > 0 ? 1.1 : 0.9;
                caseImageScale = clamp(caseImageScale * delta, MIN_IMAGE_SCALE, MAX_IMAGE_SCALE);
                caseDetailImageView.setScaleX(caseImageScale);
                caseDetailImageView.setScaleY(caseImageScale);
                ev.consume();
            }
        });

        caseImageScroll.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                caseImageScale = INITIAL_IMAGE_SCALE;
                caseDetailImageView.setScaleX(1.0);
                caseDetailImageView.setScaleY(1.0);
                caseImageScroll.setHvalue(0);
                caseImageScroll.setVvalue(0);
            }
        });

        // Add zoom buttons for image detail
        Button zoomOutBtn = new Button("-");
        zoomOutBtn.getStyleClass().add("secondary-button");
        zoomOutBtn.setOnAction(e -> {
            caseImageScale = clamp(caseImageScale * 0.92, MIN_IMAGE_SCALE, MAX_IMAGE_SCALE);
            caseDetailImageView.setScaleX(caseImageScale);
            caseDetailImageView.setScaleY(caseImageScale);
        });

        Button zoomInBtn = new Button("+");
        zoomInBtn.getStyleClass().add("secondary-button");
        zoomInBtn.setOnAction(e -> {
            caseImageScale = clamp(caseImageScale * 1.08, MIN_IMAGE_SCALE, MAX_IMAGE_SCALE);
            caseDetailImageView.setScaleX(caseImageScale);
            caseDetailImageView.setScaleY(caseImageScale);
        });

        HBox imageControls = new HBox(8, zoomOutBtn, zoomInBtn);
        imageControls.setAlignment(Pos.CENTER_RIGHT);
        VBox.setVgrow(imageControls, javafx.scene.layout.Priority.NEVER);

        // Ajustar tamaño del ImageView al tamaño del viewport para que ocupe todo el
        // espacio
        caseImageScroll.viewportBoundsProperty().addListener((obs, oldB, newB) -> {
            if (newB == null)
                return;
            try {
                caseDetailImageView.setFitWidth(newB.getWidth());
                caseDetailImageView.setFitHeight(newB.getHeight());
            } catch (Exception ex) {
                // ignore
            }
        });

        VBox caseDetailCard = new VBox(12,
                new Label("Detalle del caso"),
                imageControls,
                caseImageScroll);
        caseDetailCard.getStyleClass().add("panel-card");
        caseDetailCard.setPadding(new Insets(16));

        VBox casesHeader = new VBox(8,
                new Label("Gestión de Casos"),
                new Label("Selecciona una tarjeta para leer los detalles completos."));
        casesHeader.getChildren().get(0).getStyleClass().add("section-title");
        casesHeader.getChildren().get(1).getStyleClass().add("app-subtitle");

        VBox casesListCard = new VBox(12, new Label("Casos cargados"), casesScroll);
        casesListCard.getStyleClass().add("sidebar-card");
        casesListCard.setPadding(new Insets(16));
        VBox.setVgrow(casesScroll, javafx.scene.layout.Priority.ALWAYS);

        casesModule = new VBox(16, casesHeader, new HBox(16, casesListCard, caseDetailCard));
        HBox.setHgrow(casesListCard, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(caseDetailCard, javafx.scene.layout.Priority.ALWAYS);

        connectionDialogTitle = new Label("Justificación de asociación");
        connectionDialogTitle.getStyleClass().add("section-title");

        connectionDialogSubtitle = new Label("Asociado por");
        connectionDialogSubtitle.getStyleClass().add("app-subtitle");

        connectionBasisBox = new ComboBox<>();
        connectionBasisBox.getItems().addAll("Modalidad", "Modus operandi", "Patrón", "Criterio de Conexidad",
                "Fenomeno criminal", "Otros");
        connectionBasisBox.setValue("Modalidad");
        connectionBasisBox.setMaxWidth(Double.MAX_VALUE);

        connectionReasonField = new TextArea();
        connectionReasonField.getStyleClass().add("text-area");
        connectionReasonField.setPromptText("Explica por qué relacionas estos casos");
        connectionReasonField.setWrapText(true);
        connectionReasonField.setPrefRowCount(3);
        connectionReasonField.setMaxHeight(120);

        Button confirmConnectionButton = new Button("Confirmar conexión");
        confirmConnectionButton.getStyleClass().add("primary-button");
        confirmConnectionButton.setOnAction(e -> confirmConnectionFromOverlay());

        Button cancelConnectionButton = new Button("Cancelar");
        cancelConnectionButton.getStyleClass().add("ghost-button");
        cancelConnectionButton.setOnAction(e -> hideConnectionJustificationOverlay());

        VBox connectionCard = new VBox(12,
                connectionDialogTitle,
                connectionDialogSubtitle,
                connectionBasisBox,
                connectionReasonField,
                new HBox(10, confirmConnectionButton, cancelConnectionButton));
        connectionCard.getStyleClass().add("panel-card");
        connectionCard.setPadding(new Insets(18));
        connectionCard.setMaxWidth(520);
        connectionCard.setMaxHeight(Region.USE_PREF_SIZE);
        connectionCard.setMinHeight(Region.USE_PREF_SIZE);
        connectionCard.setPrefHeight(Region.USE_COMPUTED_SIZE);

        connectionDialogOverlay = new StackPane(connectionCard);
        connectionDialogOverlay.setVisible(false);
        connectionDialogOverlay.setManaged(false);
        connectionDialogOverlay.setStyle("-fx-background-color: rgba(3, 8, 20, 0.72);");
        connectionDialogOverlay.setAlignment(Pos.CENTER);
        StackPane.setAlignment(connectionCard, Pos.CENTER);

        ProgressIndicator generatingIndicator = new ProgressIndicator();
        Label generatingLabel = new Label("Generando reporte PDF, por favor espera...");
        generatingLabel.getStyleClass().add("section-title");
        VBox generatingCard = new VBox(14, generatingIndicator, generatingLabel);
        generatingCard.setAlignment(Pos.CENTER);
        generatingCard.getStyleClass().add("panel-card");
        generatingCard.setPadding(new Insets(22));
        generatingCard.setMaxWidth(460);

        pdfLoadingOverlay = new StackPane(generatingCard);
        pdfLoadingOverlay.setVisible(false);
        pdfLoadingOverlay.setManaged(false);
        pdfLoadingOverlay.setStyle("-fx-background-color: rgba(2, 6, 18, 0.78);");
        pdfLoadingOverlay.setAlignment(Pos.CENTER);
        StackPane.setAlignment(generatingCard, Pos.CENTER);

        Label isolatedNodesTitle = new Label("Justificación de casos aislados");
        isolatedNodesTitle.getStyleClass().add("section-title");

        Label isolatedNodesSubtitle = new Label(
                "Antes de cerrar la investigación, explica por qué cada caso quedó sin grupo.");
        isolatedNodesSubtitle.getStyleClass().add("app-subtitle");
        isolatedNodesSubtitle.setWrapText(true);

        isolatedNodesEntriesContainer = new VBox(12);
        isolatedNodesEntriesContainer.setFillWidth(true);

        ScrollPane isolatedNodesScroll = new ScrollPane(isolatedNodesEntriesContainer);
        isolatedNodesScroll.getStyleClass().add("group-scroll");
        isolatedNodesScroll.setFitToWidth(true);
        isolatedNodesScroll.setPrefViewportHeight(320);

        Button confirmIsolatedNodesButton = new Button("Guardar y finalizar");
        confirmIsolatedNodesButton.getStyleClass().add("primary-button");
        confirmIsolatedNodesButton.setOnAction(e -> confirmIsolatedNodesAndFinalize());

        Button cancelIsolatedNodesButton = new Button("Cancelar");
        cancelIsolatedNodesButton.getStyleClass().add("ghost-button");
        cancelIsolatedNodesButton.setOnAction(e -> hideIsolatedNodesOverlay());

        VBox isolatedNodesCard = new VBox(12,
                isolatedNodesTitle,
                isolatedNodesSubtitle,
                isolatedNodesScroll,
                new HBox(10, confirmIsolatedNodesButton, cancelIsolatedNodesButton));
        isolatedNodesCard.getStyleClass().add("panel-card");
        isolatedNodesCard.setPadding(new Insets(18));
        isolatedNodesCard.setMaxWidth(640);

        isolatedNodesOverlay = new StackPane(isolatedNodesCard);
        isolatedNodesOverlay.setVisible(false);
        isolatedNodesOverlay.setManaged(false);
        isolatedNodesOverlay.setStyle("-fx-background-color: rgba(3, 8, 20, 0.74);");
        isolatedNodesOverlay.setAlignment(Pos.CENTER);
        StackPane.setAlignment(isolatedNodesCard, Pos.CENTER);

        // ── Decision overlay modal ──
        decisionOverlayTitle = new Label("¿Qué vas a decidir ahora?");
        decisionOverlayTitle.getStyleClass().add("section-title");

        Label decisionSubtitle = new Label("Selecciona una o más opciones y justifica cada decisión:");
        decisionSubtitle.getStyleClass().add("app-subtitle");
        decisionSubtitle.setWrapText(true);

        decisionOptionsContainer = new VBox(8);
        decisionOptionsContainer.setFillWidth(true);

        for (DecisionOptionDef option : DECISION_OPTIONS) {
            CheckBox optionCheck = new CheckBox(option.label());
            optionCheck.setWrapText(true);
            optionCheck.setMaxWidth(Double.MAX_VALUE);
            optionCheck.setStyle(
                    "-fx-font-size: 13px; " +
                            "-fx-text-fill: #dbeafe; " +
                            "-fx-padding: 4 0 4 0;");
            optionCheck.selectedProperty()
                    .addListener((obs, wasSelected, isSelected) -> toggleDecisionJustificationRow(option, isSelected));
            decisionOptionCheckboxes.put(option.id(), optionCheck);
            decisionOptionsContainer.getChildren().add(optionCheck);
        }

        decisionJustificationsContainer = new VBox(12);
        decisionJustificationsContainer.setFillWidth(true);

        decisionJustificationsScroll = new ScrollPane(decisionJustificationsContainer);
        decisionJustificationsScroll.getStyleClass().add("group-scroll");
        decisionJustificationsScroll.setFitToWidth(true);
        decisionJustificationsScroll.setPrefViewportHeight(200);
        decisionJustificationsScroll.setVisible(false);
        decisionJustificationsScroll.setManaged(false);

        Button confirmDecisionButton = new Button("Aceptar");
        confirmDecisionButton.getStyleClass().add("primary-button");
        confirmDecisionButton.setOnAction(e -> confirmDecisionFromOverlay());

        Button cancelDecisionButton = new Button("Cancelar");
        cancelDecisionButton.getStyleClass().add("ghost-button");
        cancelDecisionButton.setOnAction(e -> hideDecisionOverlay());

        HBox decisionButtons = new HBox(10, confirmDecisionButton, cancelDecisionButton);
        decisionButtons.setAlignment(Pos.CENTER_LEFT);

        VBox decisionCard = new VBox(12,
                decisionOverlayTitle,
                decisionSubtitle,
                decisionOptionsContainer,
                decisionJustificationsScroll,
                decisionButtons);
        decisionCard.getStyleClass().add("panel-card");
        decisionCard.setPadding(new Insets(18));
        decisionCard.setMaxWidth(620);
        decisionCard.setMaxHeight(Region.USE_PREF_SIZE);
        decisionCard.setMinHeight(Region.USE_PREF_SIZE);
        decisionCard.setPrefHeight(Region.USE_COMPUTED_SIZE);

        decisionOverlay = new StackPane(decisionCard);
        decisionOverlay.setVisible(false);
        decisionOverlay.setManaged(false);
        decisionOverlay.setStyle("-fx-background-color: rgba(3, 8, 20, 0.76);");
        decisionOverlay.setAlignment(Pos.CENTER);
        StackPane.setAlignment(decisionCard, Pos.CENTER);

        moduleHost.getChildren().addAll(boardModule, connectionDialogOverlay, isolatedNodesOverlay, decisionOverlay,
                pdfLoadingOverlay);

        VBox content = new VBox(14, topBar, instructionLabel, moduleHost);
        view.setCenter(content);

        playTopBarEntrance();

        loadCasos();
        restoreInvestigationFromSnapshot();
        refreshConnections();
        refreshGroups();
        refreshCasesModule();
        showBoardModule();

        AnimationTimer physicsTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateInvestigationTimer();
                if (investigationFinished) {
                    return;
                }
                stepPhysics();
                updateConnections();
                updateGroupOverlays();
            }
        };
        physicsTimer.start();
    }

    private void loadCasos() {
        List<Caso> casos = CasoRepository.getCasos();
        double width = safeWidth();
        double height = safeHeight();
        double margin = NODE_RADIUS + 20;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int index = 1;

        for (Caso caso : casos) {
            CaseNode node = new CaseNode(caso, String.format("%02d", index));
            double startX = random.nextDouble(margin, Math.max(margin + 1, width - margin));
            double startY = random.nextDouble(margin, Math.max(margin + 1, height - margin));
            node.setBoardPosition(startX, startY);
            setRandomVelocity(node);
            registerNode(node);
            nodes.add(node);
            nodeLayer.getChildren().add(node);
            playNodeSpawnAnimation(node, index);
            index++;
        }

        updateCaseSearchSuggestions(caseSearchField.getText());
    }

    private void registerNode(CaseNode node) {
        node.setOnMouseClicked(event -> {
            if (investigationFinished) {
                return;
            }
            if (handMode) {
                return;
            }
            if (selectedNode == null) {
                selectedNode = node;
                node.setSelected(true);
                statusLabel
                        .setText("Seleccionado: " + node.getCaso().getNombre() + ". Elige otro caso para asociarlo.");
                return;
            }

            if (selectedNode == node) {
                node.setSelected(false);
                selectedNode = null;
                statusLabel.setText("Selección cancelada.");
                return;
            }

            pendingConnectionTarget = node;
            pendingConnectionTarget.setSelected(true);
            showConnectionJustificationOverlay();
        });

        node.setOnMouseEntered(event -> {
            if (investigationFinished) {
                return;
            }
            ScaleTransition st = new ScaleTransition(javafx.util.Duration.millis(120), node);
            double targetScale = node.isSelected() ? 1.18 : 1.08;
            st.setToX(targetScale);
            st.setToY(targetScale);
            st.play();
        });

        node.setOnMouseExited(event -> {
            ScaleTransition st = new ScaleTransition(javafx.util.Duration.millis(120), node);
            double targetScale = node.isSelected() ? 1.18 : 1.0;
            st.setToX(targetScale);
            st.setToY(targetScale);
            st.play();
        });
    }

    private void showConnectionJustificationOverlay() {
        if (investigationFinished) {
            return;
        }

        boolean isBatch = pendingBatchNodes != null && pendingBatchNodes.size() >= 2;
        if (!isBatch && (selectedNode == null || pendingConnectionTarget == null)) {
            return;
        }

        if (isBatch) {
            connectionDialogTitle
                    .setText("Justificación de asociación múltiple (" + pendingBatchNodes.size() + " casos)");
        } else {
            connectionDialogTitle.setText("Justificación: " + selectedNode.getCaso().getNombre() + " → "
                    + pendingConnectionTarget.getCaso().getNombre());
        }

        connectionBasisBox.setValue("Modalidad");
        connectionReasonField.clear();
        connectionDialogOverlay.setVisible(true);
        connectionDialogOverlay.setManaged(true);
        connectionDialogOverlay.toFront();
        connectionReasonField.requestFocus();
    }

    private void hideConnectionJustificationOverlay() {
        connectionDialogOverlay.setVisible(false);
        connectionDialogOverlay.setManaged(false);
        connectionBasisBox.setValue("Modalidad");
        connectionReasonField.clear();
        if (selectedNode != null) {
            selectedNode.setSelected(false);
        }
        if (pendingConnectionTarget != null) {
            pendingConnectionTarget.setSelected(false);
        }
        if (pendingBatchNodes != null) {
            for (CaseNode node : pendingBatchNodes) {
                node.setSelected(false);
            }
        }
        selectedNode = null;
        pendingConnectionTarget = null;
        pendingBatchNodes = null;
        statusLabel.setText("Selección cancelada.");
    }

    private void confirmConnectionFromOverlay() {
        if (investigationFinished) {
            hideConnectionJustificationOverlay();
            return;
        }

        if (pendingBatchNodes != null && !pendingBatchNodes.isEmpty()) {
            String trimmed = connectionReasonField.getText().trim();
            if (trimmed.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Escribe una justificación para continuar.");
                return;
            }

            String basis = connectionBasisBox.getValue();
            String connectionSummary = "Asociado por: " + basis + " | Justificación: " + trimmed;

            // Create connections in a consecutive chain
            for (int i = 0; i < pendingBatchNodes.size() - 1; i++) {
                CaseNode from = pendingBatchNodes.get(i);
                CaseNode to = pendingBatchNodes.get(i + 1);

                boolean alreadyConnected = connections.stream()
                        .anyMatch(conn -> (conn.from == from && conn.to == to) || (conn.from == to && conn.to == from));
                if (!alreadyConnected) {
                    connections.add(new Connection(from, to, connectionSummary, "Grupo Legado"));
                }
            }

            refreshConnections();
            refreshGroups();
            persistInvestigationSnapshot();

            statusLabel.setText("Conexiones en lote creadas para " + pendingBatchNodes.size() + " casos.");

            for (CaseNode node : pendingBatchNodes) {
                node.setSelected(false);
            }
            pendingBatchNodes = null;
            hideConnectionJustificationOverlay();
            return;
        }

        if (selectedNode == null || pendingConnectionTarget == null) {
            hideConnectionJustificationOverlay();
            return;
        }

        String trimmed = connectionReasonField.getText().trim();
        if (trimmed.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Escribe una justificación para continuar.");
            return;
        }

        String basis = connectionBasisBox.getValue();
        String connectionSummary = "Asociado por: " + basis + " | Justificación: " + trimmed;

        connections.add(new Connection(selectedNode, pendingConnectionTarget, connectionSummary, "Grupo Legado"));
        refreshConnections();
        refreshGroups();
        persistInvestigationSnapshot();
        statusLabel.setText("Conexión creada entre " + selectedNode.getCaso().getNombre() + " y "
                + pendingConnectionTarget.getCaso().getNombre() + ".");

        selectedNode.setSelected(false);
        pendingConnectionTarget.setSelected(false);
        selectedNode = null;
        pendingConnectionTarget = null;
        hideConnectionJustificationOverlay();
    }

    private void stepPhysics() {
        double width = safeWidth();
        double height = safeHeight();
        List<CaseNode> freeNodes = nodes.stream()
                .filter(node -> !isGroupedNode(node))
                .collect(Collectors.toList());

        for (int i = 0; i < freeNodes.size(); i++) {
            CaseNode a = freeNodes.get(i);
            if (a.dragging) {
                continue;
            }

            for (int j = i + 1; j < freeNodes.size(); j++) {
                CaseNode b = freeNodes.get(j);
                double dx = a.centerX() - b.centerX();
                double dy = a.centerY() - b.centerY();
                double distanceSq = Math.max(dx * dx + dy * dy, 80.0);
                double distance = Math.sqrt(distanceSq);
                if (distance < REPULSION_DISTANCE) {
                    double force = REPULSION_STRENGTH / distanceSq;
                    double fx = force * dx / distance;
                    double fy = force * dy / distance;
                    a.vx += fx * 0.12;
                    a.vy += fy * 0.12;
                    b.vx -= fx * 0.12;
                    b.vy -= fy * 0.12;
                }
            }

            normalizeVelocity(a);

            double nextX = a.getLayoutX() + a.vx;
            double nextY = a.getLayoutY() + a.vy;

            if (nextX <= 0 || nextX + NODE_DIAMETER >= width) {
                a.vx = -a.vx;
                nextX = clamp(nextX, 0, width - NODE_DIAMETER);
            }
            if (nextY <= 0 || nextY + NODE_DIAMETER >= height) {
                a.vy = -a.vy;
                nextY = clamp(nextY, 0, height - NODE_DIAMETER);
            }

            Point2D resolved = resolveGroupFrameCollision(a, nextX, nextY);
            nextX = resolved.getX();
            nextY = resolved.getY();

            a.setBoardPosition(nextX, nextY);
            normalizeVelocity(a);
        }
    }

    private void normalizeVelocity(CaseNode node) {
        double speed = Math.hypot(node.vx, node.vy);
        if (speed < 0.001) {
            setRandomVelocity(node);
            return;
        }

        node.vx = node.vx / speed * TARGET_SPEED;
        node.vy = node.vy / speed * TARGET_SPEED;
    }

    private void setRandomVelocity(CaseNode node) {
        double angle = ThreadLocalRandom.current().nextDouble(0.0, Math.PI * 2.0);
        node.vx = Math.cos(angle) * TARGET_SPEED;
        node.vy = Math.sin(angle) * TARGET_SPEED;
    }

    private void updateConnections() {
        for (Connection connection : connections) {
            connection.line.setStartX(connection.from.centerX());
            connection.line.setStartY(connection.from.centerY());
            connection.line.setEndX(connection.to.centerX());
            connection.line.setEndY(connection.to.centerY());
        }
    }

    private void refreshConnections() {
        connectionList.getItems().setAll(connections.stream()
                .map(connection -> connection.from.getCaso().getNombre() + " ↔ " + connection.to.getCaso().getNombre()
                        + " | " + connection.reason)
                .collect(Collectors.toList()));

        connectionLayer.getChildren().setAll(connections.stream()
                .map(connection -> connection.line)
                .collect(Collectors.toList()));

        if (connectionsSectionTitle != null) {
            connectionsSectionTitle.setText("Conexiones (" + connections.size() + ")");
        }
        updateUngroupedNodesLabel();
    }

    private void installConnectionListCellFactory() {
        connectionList.setCellFactory(listView -> new ListCell<>() {
            private final Label summaryLabel = new Label();
            private final Button deleteButton = new Button("Eliminar");
            private final HBox row = new HBox(10, summaryLabel, deleteButton);

            {
                summaryLabel.setWrapText(true);
                summaryLabel.setMaxWidth(Double.MAX_VALUE);
                summaryLabel.getStyleClass().add("app-subtitle");
                HBox.setHgrow(summaryLabel, Priority.ALWAYS);
                row.setAlignment(Pos.CENTER_LEFT);

                deleteButton.getStyleClass().add("danger-button");
                deleteButton.setMinWidth(Region.USE_PREF_SIZE);
                deleteButton.setOnAction(event -> {
                    int index = getIndex();
                    if (index >= 0) {
                        removeConnectionAt(index);
                    }
                    event.consume();
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                summaryLabel.setText(item);
                deleteButton.setDisable(investigationFinished);
                setText(null);
                setGraphic(row);
            }
        });
    }

    public void removeConnectionAt(int index) {
        if (investigationFinished) {
            showAlert(Alert.AlertType.INFORMATION, "La investigación ya finalizó; no puedes eliminar conexiones.");
            return;
        }
        if (index < 0 || index >= connections.size()) {
            return;
        }

        Connection removed = connections.remove(index);
        refreshConnections();
        refreshGroups();
        restoreMovementForUngroupedNodes();
        persistInvestigationSnapshot();

        statusLabel.setText("Conexión eliminada entre "
                + removed.from.getCaso().getNombre()
                + " y "
                + removed.to.getCaso().getNombre()
                + ".");
    }

    private void restoreMovementForUngroupedNodes() {
        for (CaseNode node : nodes) {
            if (!isGroupedNode(node) && !node.dragging) {
                double speed = Math.hypot(node.vx, node.vy);
                if (speed < TARGET_SPEED * 0.35) {
                    setRandomVelocity(node);
                }
            }
        }
    }

    private void refreshGroups() {
        currentClusters = detectClusters();
        rebuildGroupedNodesMap();
        arrangeGroupedNodes();

        Set<String> activeSignatures = currentClusters.stream()
                .map(cluster -> cluster.signature)
                .collect(Collectors.toSet());

        overlayBySignature.entrySet().removeIf(entry -> {
            boolean remove = !activeSignatures.contains(entry.getKey());
            if (remove) {
                groupLayer.getChildren().removeAll(entry.getValue().rectangle, entry.getValue().nameLabel);
            }
            return remove;
        });

        groupList.getItems().setAll(currentClusters);

        if (selectedGroupSignature != null
                && activeSignatures.stream().noneMatch(signature -> signature.equals(selectedGroupSignature))) {
            clearGroupSelection();
        }

        renderGroupCards();
        updateGroupOverlays();
        updateUngroupedNodesLabel();
    }

    private void updateUngroupedNodesLabel() {
        long totalCount = nodes.size();
        long ungroupedCount = nodes.stream()
                .filter(node -> !isGroupedNode(node))
                .count();
        applySidebarStatChip(totalNodesLabel, totalCount, "Total de casos", false);
        applySidebarStatChip(ungroupedNodesLabel, ungroupedCount, "Sin grupo", true);
    }

    public void applySidebarStatChip(Label host, long value, String caption, boolean warning) {
        Label numberLabel = new Label(String.valueOf(value));
        numberLabel.setStyle(
                "-fx-font-size: 22; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: " + (warning ? "#c8a03b" : "#d0e4ff") + "; " +
                        "-fx-font-family: 'Segoe UI', sans-serif;");

        Label captionLabel = new Label(caption);
        captionLabel.setWrapText(true);
        captionLabel.setStyle(
                "-fx-font-size: 10; " +
                        "-fx-text-fill: #7ba3d8; " +
                        "-fx-font-family: 'Segoe UI', sans-serif;");

        VBox chipContent = new VBox(2, numberLabel, captionLabel);
        chipContent.setAlignment(Pos.CENTER);

        host.setGraphic(chipContent);
        host.setText(null);
        host.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        host.setAlignment(Pos.CENTER);
        host.setStyle(
                "-fx-background-color: #0a1a3a; " +
                        "-fx-border-color: " + (warning ? "#c8a03b" : "#1a3a7a") + "; " +
                        "-fx-border-radius: 6; " +
                        "-fx-background-radius: 6; " +
                        "-fx-padding: 8 10 8 10; " +
                        "-fx-alignment: center;");
    }

    private void refreshCasesModule() {
        if (casesCardsContainer == null) {
            return;
        }

        casesCardsContainer.getChildren().setAll(CasoRepository.getCasos().stream()
                .map(this::buildCaseCard)
                .collect(Collectors.toList()));

        if (!CasoRepository.getCasos().isEmpty() && caseDetailArea.getText().isBlank()) {
            showCaseDetail(CasoRepository.getCasos().get(0));
        }
    }

    private VBox buildCaseCard(Caso caso) {
        int caseIndex = CasoRepository.getCasos().indexOf(caso) + 1;
        String formattedNum = String.format("%02d", caseIndex);
        Label title = new Label(caso.getNombre());
        title.getStyleClass().add("section-title");

        Label badge = new Label(formattedNum);
        badge.setStyle(
                "-fx-background-color: #c8a03b; " +
                        "-fx-text-fill: #0a1a3a; " +
                        "-fx-font-size: 11; " +
                        "-fx-font-weight: bold; " +
                        "-fx-min-width: 22; " +
                        "-fx-min-height: 22; " +
                        "-fx-max-width: 22; " +
                        "-fx-max-height: 22; " +
                        "-fx-background-radius: 11; " +
                        "-fx-alignment: center; " +
                        "-fx-font-family: 'Segoe UI', sans-serif;");

        HBox headerRow = new HBox(8, badge, title);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label meta = new Label(caso.getLugar() + " · " + caso.getFechaHechosFormateada());
        meta.getStyleClass().add("app-subtitle");

        Label summary = new Label(caso.getDescripcion());
        summary.getStyleClass().add("muted-text");
        summary.setWrapText(true);

        Button detailButton = new Button("Ver detalles");
        detailButton.getStyleClass().add("secondary-button");
        detailButton.setOnAction(e -> showCaseDetail(caso));

        VBox card = new VBox(10, headerRow, meta, summary, detailButton);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(14));
        card.setOnMouseClicked(e -> showCaseDetail(caso));
        return card;
    }

    private String buildCaseDetails(Caso caso) {
        int caseIndex = CasoRepository.getCasos().indexOf(caso) + 1;
        String formattedNum = String.format("%02d", caseIndex);
        return "📋 [" + formattedNum + "] " + caso.getNombre() + "\n\n"
                + "📍 Lugar: " + caso.getLugar() + "\n"
                + "📅 Fecha: " + caso.getFechaHechosFormateada() + "\n\n"
                + "📝 Descripción:\n" + caso.getDescripcion() + "\n\n"
                + "👥 Víctimas:\n" + String.join("\n", caso.getVictimas()) + "\n\n"
                + "⚖️ Victimarios:\n" + String.join("\n", caso.getVictimarios()) + "\n\n"
                + "⚔️ Delitos:\n" + String.join("\n", caso.getDelitos()) + "\n\n"
                + "🏛️ Actores Involucrados:\n" + String.join("\n", caso.getActoresInvolucrados()) + "\n";
    }

    private void showCaseDetail(Caso caso) {
        if (caso == null)
            return;
        // hide textual area and show image-only view
        caseDetailArea.setVisible(false);
        caseDetailArea.setManaged(false);
        caseDetailImageView.setImage(null);
        caseDetailImageView.setVisible(false);
        caseImageScroll.setVisible(false);

        if (!caso.tieneImagen()) {
            return;
        }

        String rawPath = caso.getImagenPath();
        if (rawPath == null || rawPath.isBlank())
            return;

        try {
            Path candidate = Paths.get(rawPath);
            if (!candidate.isAbsolute() || !Files.exists(candidate)) {
                Path alt = Paths.get("casos").resolve(rawPath);
                if (Files.exists(alt)) {
                    candidate = alt;
                }
            }

            if (Files.exists(candidate)) {
                Image img = new Image(candidate.toUri().toString(), false);
                caseDetailImageView.setImage(img);
                caseDetailImageView.setVisible(true);
                caseImageScale = INITIAL_IMAGE_SCALE;
                caseDetailImageView.setScaleX(caseImageScale);
                caseDetailImageView.setScaleY(caseImageScale);
                caseImageScroll.setVisible(true);
                caseImageScroll.setHvalue(0);
                caseImageScroll.setVvalue(0);
                return;
            }

            // Try classpath resource under /casos/
            String resourcePath = "/casos/" + rawPath;
            try (var is = getClass().getResourceAsStream(resourcePath)) {
                if (is != null) {
                    Image img = new Image(is);
                    caseDetailImageView.setImage(img);
                    caseDetailImageView.setVisible(true);
                    caseImageScale = INITIAL_IMAGE_SCALE;
                    caseDetailImageView.setScaleX(caseImageScale);
                    caseDetailImageView.setScaleY(caseImageScale);
                    caseImageScroll.setVisible(true);
                    caseImageScroll.setHvalue(0);
                    caseImageScroll.setVvalue(0);
                    return;
                }
            } catch (Exception ex) {
                // fall through
            }
        } catch (Exception ex) {
            // ignore and leave image hidden
        }
    }

    private void showBoardModule() {
        if (!moduleHost.getChildren().contains(boardModule)) {
            moduleHost.getChildren().setAll(boardModule, connectionDialogOverlay, isolatedNodesOverlay,
                    pdfLoadingOverlay);
        }
        setActiveTab(analyticalTab, casesTab);
        instructionLabel.setText(
                "Conecta esferas para crear hipótesis investigativas. Cada componente conectado se convierte en un grupo.");
        board.setVisible(true);
        board.setManaged(true);
        if (!boardModuleShownOnce) {
            animateModuleEntrance(boardModule);
            boardModuleShownOnce = true;
        } else {
            animateModuleSwitch(boardModule);
        }
    }

    private void handleBoardZoom(ScrollEvent event) {
        if (investigationFinished) {
            return;
        }
        if (event.isControlDown()) {
            double direction = event.getDeltaY() > 0 ? BOARD_ZOOM_STEP : -BOARD_ZOOM_STEP;
            applyBoardZoom(boardZoom + direction);
            event.consume();
        }
    }

    private void applyBoardZoom(double requestedZoom) {
        boardZoom = clamp(requestedZoom, MIN_BOARD_ZOOM, MAX_BOARD_ZOOM);
        contentContainer.setScaleX(boardZoom);
        contentContainer.setScaleY(boardZoom);
    }

    private void handleBoardMousePressed(javafx.scene.input.MouseEvent event) {
        if (investigationFinished) {
            return;
        }
        if (!handMode) {
            return;
        }
        if (event.isPrimaryButtonDown()) {
            panning = true;
            panStartX = event.getX();
            panStartY = event.getY();
            contentTranslateXStart = contentContainer.getTranslateX();
            contentTranslateYStart = contentContainer.getTranslateY();
            board.setCursor(javafx.scene.Cursor.CLOSED_HAND);
            event.consume();
        }
    }

    private void handleBoardMouseDragged(javafx.scene.input.MouseEvent event) {
        if (investigationFinished) {
            return;
        }
        if (panning) {
            double deltaX = event.getX() - panStartX;
            double deltaY = event.getY() - panStartY;
            contentContainer.setTranslateX(contentTranslateXStart + deltaX);
            contentContainer.setTranslateY(contentTranslateYStart + deltaY);
            event.consume();
        }
    }

    private void handleBoardMouseReleased(javafx.scene.input.MouseEvent event) {
        if (investigationFinished) {
            return;
        }
        if (panning) {
            panning = false;
            board.setCursor(handMode ? javafx.scene.Cursor.OPEN_HAND : javafx.scene.Cursor.DEFAULT);
            event.consume();
        }
    }

    private void showCasesModule() {
        refreshCasesModule();
        if (!moduleHost.getChildren().contains(casesModule)) {
            moduleHost.getChildren().setAll(casesModule, connectionDialogOverlay, isolatedNodesOverlay,
                    pdfLoadingOverlay);
        }
        setActiveTab(casesTab, analyticalTab);
        instructionLabel.setText(
                "Revisa las tarjetas de cada caso y usa sus detalles para construir asociaciones más precisas.");
        board.setVisible(false);
        board.setManaged(false);
        if (!casesModuleShownOnce) {
            animateModuleEntrance(casesModule);
            casesModuleShownOnce = true;
        } else {
            animateModuleSwitch(casesModule);
        }
    }

    private void playTopBarEntrance() {
        topBar.setOpacity(0.0);
        topBar.setTranslateY(-10.0);

        FadeTransition fade = new FadeTransition(javafx.util.Duration.millis(520), topBar);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        TranslateTransition slide = new TranslateTransition(javafx.util.Duration.millis(520), topBar);
        slide.setFromY(-10.0);
        slide.setToY(0.0);

        fade.play();
        slide.play();
    }

    private void animateModuleEntrance(VBox module) {
        module.setOpacity(0.0);
        FadeTransition ft = new FadeTransition(javafx.util.Duration.millis(420), module);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    private void animateModuleSwitch(VBox module) {
        module.setOpacity(0.0);
        FadeTransition ft = new FadeTransition(javafx.util.Duration.millis(240), module);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    private void playNodeSpawnAnimation(CaseNode node, int index) {
        node.setOpacity(0.0);
        node.setScaleX(0.88);
        node.setScaleY(0.88);

        FadeTransition fade = new FadeTransition(javafx.util.Duration.millis(320), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setDelay(javafx.util.Duration.millis(24L * index));

        ScaleTransition scale = new ScaleTransition(javafx.util.Duration.millis(320), node);
        scale.setFromX(0.88);
        scale.setFromY(0.88);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setDelay(javafx.util.Duration.millis(24L * index));

        fade.play();
        scale.play();
    }

    private void setActiveTab(Button active, Button inactive) {
        active.getStyleClass().remove("mode-tab");
        if (!active.getStyleClass().contains("mode-tab-active")) {
            active.getStyleClass().add("mode-tab-active");
        }

        inactive.getStyleClass().remove("mode-tab-active");
        if (!inactive.getStyleClass().contains("mode-tab")) {
            inactive.getStyleClass().add("mode-tab");
        }
    }

    private void updateCaseSearchSuggestions(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase();
        if (normalized.isBlank()) {
            caseSearchSuggestions.getItems().clear();
            caseSearchSuggestions.setVisible(false);
            caseSearchSuggestions.setManaged(false);
            return;
        }

        List<String> matches = nodes.stream()
                .map(node -> node.getCaso())
                .distinct()
                .filter(caso -> buildSearchIndex(caso).contains(normalized))
                .map(Caso::getNombre)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .limit(6)
                .collect(Collectors.toList());

        if (matches.isEmpty()) {
            caseSearchSuggestions.getItems().setAll("Sin coincidencias");
        } else {
            caseSearchSuggestions.getItems().setAll(matches);
        }
        caseSearchSuggestions.setVisible(true);
        caseSearchSuggestions.setManaged(true);
    }

    private void searchCaseByQuery(String query) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.isBlank()) {
            statusLabel.setText("Escribe el nombre de un caso para buscarlo.");
            return;
        }

        // Support for batch relations when query contains commas
        if (normalized.contains(",")) {
            String[] parts = normalized.split(",");
            List<CaseNode> targets = new ArrayList<>();
            for (String part : parts) {
                String cleanPart = part.trim();
                if (cleanPart.isEmpty()) {
                    continue;
                }
                String lowerPart = cleanPart.toLowerCase();
                CaseNode found = nodes.stream()
                        .filter(node -> node.getCaso().getNombre().equalsIgnoreCase(cleanPart)
                                || buildSearchIndex(node.getCaso()).contains(lowerPart))
                        .findFirst()
                        .orElse(null);
                if (found == null) {
                    showAlert(Alert.AlertType.WARNING, "No se encontró el caso: " + cleanPart);
                    return;
                }
                if (!targets.contains(found)) {
                    targets.add(found);
                }
            }

            if (targets.size() < 2) {
                showAlert(Alert.AlertType.WARNING,
                        "Debes especificar al menos 2 casos diferentes para crear una relación.");
                return;
            }

            // Success batch match
            pendingBatchNodes = targets;
            for (CaseNode node : targets) {
                node.setSelected(true);
            }

            centerBoardOnNode(targets.get(0));
            statusLabel.setText(
                    "Casos para asociar: " + targets.size() + " casos seleccionados. Registra su justificación.");

            caseSearchSuggestions.getSelectionModel().clearSelection();
            caseSearchSuggestions.setVisible(false);
            caseSearchSuggestions.setManaged(false);

            showConnectionJustificationOverlay();
            return;
        }

        String lowerQuery = normalized.toLowerCase();
        String suggestion = caseSearchSuggestions.getSelectionModel().getSelectedItem();
        if (suggestion == null || "Sin coincidencias".equalsIgnoreCase(suggestion)
                || !suggestion.toLowerCase().contains(lowerQuery)) {
            suggestion = caseSearchSuggestions.getItems().stream().findFirst().orElse(null);
        }
        final String resolvedSuggestion = suggestion;

        CaseNode target = nodes.stream()
                .filter(node -> buildSearchIndex(node.getCaso()).contains(lowerQuery)
                        || node.getCaso().getNombre().equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(null);

        if (target == null && resolvedSuggestion != null && !"Sin coincidencias".equalsIgnoreCase(resolvedSuggestion)) {
            target = nodes.stream()
                    .filter(node -> node.getCaso().getNombre().equalsIgnoreCase(resolvedSuggestion))
                    .findFirst()
                    .orElse(null);
        }

        if (target == null) {
            target = nodes.stream()
                    .filter(node -> buildSearchIndex(node.getCaso()).contains(lowerQuery))
                    .findFirst()
                    .orElse(null);
        }

        if (target == null) {
            statusLabel.setText("No se encontró el caso: " + normalized);
            caseSearchSuggestions.getSelectionModel().clearSelection();
            return;
        }

        caseSearchField.setText(target.getCaso().getNombre());
        caseSearchSuggestions.getSelectionModel().clearSelection();
        caseSearchSuggestions.setVisible(false);
        caseSearchSuggestions.setManaged(false);
        focusCaseNode(target);
    }

    private void focusCaseNode(CaseNode target) {
        if (target == null || investigationFinished) {
            return;
        }

        showBoardModule();
        hideConnectionJustificationOverlay();
        target.setSelected(true);
        target.toFront();
        selectedNode = target;
        pendingConnectionTarget = null;
        statusLabel.setText("Caso encontrado: " + target.getCaso().getNombre() + ".");
        centerBoardOnNode(target);
    }

    private String buildSearchIndex(Caso caso) {
        if (caso == null) {
            return "";
        }
        return String.join(" | ",
                safeLower(caso.getNombre()),
                safeLower(caso.getDescripcion()),
                safeLower(caso.getLugar()),
                safeLower(String.join(" ", caso.getVictimas())),
                safeLower(String.join(" ", caso.getVictimarios())),
                safeLower(String.join(" ", caso.getDelitos())),
                safeLower(String.join(" ", caso.getActoresInvolucrados())),
                safeLower(caso.getImagenPath()));
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private void centerBoardOnNode(CaseNode target) {
        centerBoardOnPoint(target.centerX(), target.centerY(), true);
    }

    public void centerBoardOnCluster(GroupCluster cluster) {
        if (cluster == null || cluster.members.isEmpty()) {
            return;
        }
        showBoardModule();
        double cx = 0.0;
        double cy = 0.0;
        for (CaseNode member : cluster.members) {
            cx += member.centerX();
            cy += member.centerY();
        }
        cx /= cluster.members.size();
        cy /= cluster.members.size();
        centerBoardOnPoint(cx, cy, false);
    }

    public Node getGroupOverlayNode(GroupCluster cluster) {
        if (cluster == null) {
            return null;
        }
        GroupOverlay overlay = overlayBySignature.get(cluster.signature);
        return overlay != null ? overlay.rectangle : null;
    }

    private void centerBoardOnPoint(double centerX, double centerY, boolean animate) {
        double viewportWidth = board.getWidth() > 0 ? board.getWidth() : board.getPrefWidth();
        double viewportHeight = board.getHeight() > 0 ? board.getHeight() : board.getPrefHeight();
        double targetX = (viewportWidth / 2.0) - (centerX * boardZoom);
        double targetY = (viewportHeight / 2.0) - (centerY * boardZoom);

        if (animate) {
            TranslateTransition transition = new TranslateTransition(javafx.util.Duration.millis(280), contentContainer);
            transition.setToX(targetX);
            transition.setToY(targetY);
            transition.play();
        } else {
            contentContainer.setTranslateX(targetX);
            contentContainer.setTranslateY(targetY);
        }
    }

    private void renderGroupCards() {
        groupCardsContainer.getChildren().setAll(currentClusters.stream()
                .map(this::buildGroupCard)
                .collect(Collectors.toList()));
    }

    private VBox buildGroupCard(GroupCluster cluster) {
        GroupMeta meta = cluster.meta;

        TextField nameField = new TextField(meta.name);
        nameField.getStyleClass().add("input-field");
        nameField.setPromptText("Nombre del grupo");
        nameField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (investigationFinished) {
                return;
            }
            String liveName = newValue == null ? "" : newValue.trim();
            meta.name = liveName.isBlank() ? oldValue : liveName;
            updateGroupOverlays();
            groupList.refresh();
        });

        Button colorButton = new Button();
        colorButton.getStyleClass().add("group-swatch-button");
        colorButton.setMinSize(30, 30);
        colorButton.setPrefSize(30, 30);
        colorButton.setMaxSize(30, 30);
        colorButton.setStyle(buildSwatchStyle(meta.color));
        colorButton.setCursor(javafx.scene.Cursor.HAND);
        colorButton.setOnAction(e -> {
            ColorPicker picker = new ColorPicker(meta.color);
            picker.setOnAction(ce -> {
                Color selectedColor = picker.getValue();
                meta.color = selectedColor;
                colorButton.setStyle(buildSwatchStyle(selectedColor));
                updateGroupOverlays();
            });
            picker.show();
        });

        Label countLabel = new Label(cluster.members.size() + " casos conectados");
        countLabel.getStyleClass().add("app-subtitle");

        String memberNumbers = cluster.members.stream()
                .map(node -> "N.º " + node.getDisplayNumber())
                .collect(Collectors.joining(" · "));
        Label membersIndexLabel = new Label("Números en tablero: " + memberNumbers);
        membersIndexLabel.getStyleClass().add("app-subtitle");
        membersIndexLabel.setWrapText(true);

        Label membersNamesLabel = new Label(buildGroupMembersSummary(cluster));
        membersNamesLabel.getStyleClass().add("muted-text");
        membersNamesLabel.setWrapText(true);

        VBox card = new VBox(8);
        card.getStyleClass().add("group-card");
        card.setPadding(new Insets(12));

        HBox headerRow = new HBox(10, colorButton, nameField);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        if (meta.finalized) {
            Label finalizedLabel = new Label("✔ Grupo finalizado");
            finalizedLabel.getStyleClass().add("app-subtitle");
            finalizedLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
            VBox decisionInfo = new VBox(4);
            if (meta.mode != null && !meta.mode.isBlank()) {
                Label decisionLabel = new Label("Decisiones: " + meta.mode);
                decisionLabel.getStyleClass().add("app-subtitle");
                decisionLabel.setWrapText(true);
                decisionInfo.getChildren().add(decisionLabel);
            }
            if (meta.decisionDetail != null && !meta.decisionDetail.isBlank()) {
                Label detailLabel = new Label("Justificaciones:\n" + meta.decisionDetail);
                detailLabel.getStyleClass().add("app-subtitle");
                detailLabel.setWrapText(true);
                decisionInfo.getChildren().add(detailLabel);
            }
            card.getChildren().addAll(
                    headerRow,
                    membersIndexLabel,
                    membersNamesLabel,
                    finalizedLabel,
                    decisionInfo,
                    countLabel);
            nameField.setDisable(true);
            colorButton.setDisable(true);
        } else {
            Button finalizeButton = new Button("¿Que va a decidir ahora?");
            finalizeButton.getStyleClass().add("primary-button");
            finalizeButton
                    .setOnAction(e -> saveGroupCard(cluster.signature, nameField, meta, colorButton));
            card.getChildren().addAll(
                    headerRow,
                    membersIndexLabel,
                    membersNamesLabel,
                    finalizeButton,
                    countLabel);
            if (investigationFinished) {
                nameField.setDisable(true);
                colorButton.setDisable(true);
                finalizeButton.setDisable(true);
            }
        }
        return card;
    }

    private String buildGroupMembersSummary(GroupCluster cluster) {
        return cluster.members.stream()
                .map(node -> "N.º " + node.getDisplayNumber() + " → " + shortenCaseName(node.getCaso().getNombre(), 22))
                .collect(Collectors.joining("\n"));
    }

    private String shortenCaseName(String name, int maxLength) {
        if (name == null || name.isBlank()) {
            return "Sin nombre";
        }
        String trimmed = name.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private void saveGroupCard(String signature, TextField nameField, GroupMeta meta,
            Button colorButton) {
        if (investigationFinished) {
            return;
        }
        String groupName = nameField.getText().trim();
        if (groupName.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "El grupo debe tener nombre.");
            return;
        }

        // Store pending state and show decision modal
        pendingDecisionGroupSignature = signature;
        pendingDecisionMeta = meta;
        pendingDecisionGroupName = groupName;
        pendingDecisionReason = "";
        pendingDecisionColorButton = colorButton;
        showDecisionOverlay();
    }

    private void showDecisionOverlay() {
        decisionOptionCheckboxes.values().forEach(check -> check.setSelected(false));
        decisionJustificationsContainer.getChildren().clear();
        decisionJustificationFields.clear();
        decisionJustificationsScroll.setVisible(false);
        decisionJustificationsScroll.setManaged(false);
        decisionOverlay.setVisible(true);
        decisionOverlay.setManaged(true);
        decisionOverlay.toFront();
    }

    private void hideDecisionOverlay() {
        decisionOverlay.setVisible(false);
        decisionOverlay.setManaged(false);
        decisionOptionCheckboxes.values().forEach(check -> check.setSelected(false));
        decisionJustificationsContainer.getChildren().clear();
        decisionJustificationFields.clear();
        pendingDecisionGroupSignature = null;
        pendingDecisionMeta = null;
        pendingDecisionGroupName = null;
        pendingDecisionReason = null;
        pendingDecisionColorButton = null;
    }

    private void toggleDecisionJustificationRow(DecisionOptionDef option, boolean selected) {
        if (selected) {
            if (decisionJustificationFields.containsKey(option.id())) {
                return;
            }
            Label promptLabel = new Label(option.justificationPrompt());
            promptLabel.getStyleClass().add("app-subtitle");
            promptLabel.setWrapText(true);
            promptLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #67e8f9;");

            TextArea justificationField = new TextArea();
            justificationField.getStyleClass().add("text-area");
            justificationField.setPromptText("Escribe la justificación de esta decisión, puede ser un solo párrafo...");
            justificationField.setPrefRowCount(2);
            justificationField.setWrapText(true);

            VBox row = new VBox(6, promptLabel, justificationField);
            row.getProperties().put("decisionId", option.id());
            decisionJustificationFields.put(option.id(), justificationField);
            decisionJustificationsContainer.getChildren().add(row);
        } else {
            decisionJustificationsContainer.getChildren()
                    .removeIf(node -> option.id().equals(node.getProperties().get("decisionId")));
            decisionJustificationFields.remove(option.id());
        }

        boolean hasSelections = !decisionJustificationFields.isEmpty();
        decisionJustificationsScroll.setVisible(hasSelections);
        decisionJustificationsScroll.setManaged(hasSelections);
    }

    private void confirmDecisionFromOverlay() {
        StringBuilder selectedDecisions = new StringBuilder();
        StringBuilder justifications = new StringBuilder();
        List<String> missingJustifications = new ArrayList<>();

        for (DecisionOptionDef option : DECISION_OPTIONS) {
            CheckBox checkBox = decisionOptionCheckboxes.get(option.id());
            if (checkBox == null || !checkBox.isSelected()) {
                continue;
            }

            TextArea field = decisionJustificationFields.get(option.id());
            String justification = field == null ? "" : field.getText().trim();
            if (justification.isBlank()) {
                missingJustifications.add(option.label());
                continue;
            }

            if (selectedDecisions.length() > 0) {
                selectedDecisions.append(" | ");
            }
            selectedDecisions.append(option.label());
            justifications.append("• ").append(option.label()).append('\n');
            justifications.append("  Justificación: ").append(justification).append("\n\n");
        }

        if (selectedDecisions.length() == 0) {
            showAlert(Alert.AlertType.WARNING, "Selecciona al menos una opción antes de continuar.");
            return;
        }
        if (!missingJustifications.isEmpty()) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Completa la justificación de cada opción seleccionada:\n"
                            + String.join("\n", missingJustifications));
            return;
        }

        // Apply decision to pending group
        if (pendingDecisionMeta != null && pendingDecisionGroupSignature != null) {
            pendingDecisionMeta.name = pendingDecisionGroupName;
            pendingDecisionMeta.reason = pendingDecisionReason;
            pendingDecisionMeta.mode = selectedDecisions.toString();
            pendingDecisionMeta.decisionDetail = justifications.toString().trim();
            pendingDecisionMeta.finalized = true;
            metadataBySignature.put(pendingDecisionGroupSignature, pendingDecisionMeta);
            if (pendingDecisionColorButton != null) {
                pendingDecisionColorButton.setStyle(buildSwatchStyle(pendingDecisionMeta.color));
            }
            persistInvestigationSnapshot();
            statusLabel.setText("Grupo finalizado: " + pendingDecisionMeta.name + ".");
            renderGroupCards();
            updateGroupOverlays();
        }
        hideDecisionOverlay();
    }

    private void updateInvestigationTimer() {
        if (investigationFinished) {
            return;
        }

        Duration elapsed = Duration.ofMillis(System.currentTimeMillis() - investigationStartedAtMillis);
        Duration remaining = INVESTIGATION_DURATION.minus(elapsed);

        if (remaining.isNegative() || remaining.isZero()) {
            timerLabel.setText("TIEMPO 00:00:00");
            timerLabel.setScaleX(1.34);
            timerLabel.setScaleY(1.34);
            finalizeInvestigation("tiempo agotado");
            return;
        }

        timerLabel.setText("TIEMPO " + formatDuration(remaining));
        if (remaining.toMinutes() <= 2) {
            timerLabel.setScaleX(1.28);
            timerLabel.setScaleY(1.28);
            if (!timerLabel.getStyleClass().contains("timer-pill-critical")) {
                timerLabel.getStyleClass().add("timer-pill-critical");
            }
        } else if (remaining.toMinutes() <= 5) {
            timerLabel.setScaleX(1.18);
            timerLabel.setScaleY(1.18);
            timerLabel.getStyleClass().remove("timer-pill-critical");
            if (!timerLabel.getStyleClass().contains("timer-pill-warning")) {
                timerLabel.getStyleClass().add("timer-pill-warning");
            }
        } else if (remaining.toMinutes() <= 10) {
            timerLabel.setScaleX(1.1);
            timerLabel.setScaleY(1.1);
            if (!timerLabel.getStyleClass().contains("timer-pill-warning")) {
                timerLabel.getStyleClass().add("timer-pill-warning");
            }
        } else {
            timerLabel.setScaleX(1.0);
            timerLabel.setScaleY(1.0);
            timerLabel.getStyleClass().remove("timer-pill-warning");
            timerLabel.getStyleClass().remove("timer-pill-critical");
        }
    }

    private String formatDuration(Duration duration) {
        long totalSeconds = Math.max(0, duration.getSeconds());
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void finalizeInvestigation(String reason) {
        if (investigationFinished || finishingInProgress) {
            return;
        }

        long isolatedCount = countUngroupedNodes();
        if (isolatedCount > 0 && isolatedNodeJustificationsAreMissing()) {
            pendingFinalizationReason = reason;
            showIsolatedNodesOverlay();
            return;
        }

        beginFinalization(reason);
    }

    private void beginFinalization(String reason) {
        if (investigationFinished || finishingInProgress) {
            return;
        }

        if (!InvestigationTeamContext.ensureConfigured(stage)) {
            showAlert(Alert.AlertType.WARNING, "Debes registrar los integrantes para generar el PDF.");
            return;
        }

        DistractionAlertManager.stopMonitoring();

        String investigatorName = InvestigationTeamContext.getMembersDisplay();

        finishingInProgress = true;
        disableInvestigationPanel();
        showPdfLoadingOverlay();

        Task<Path> exportTask = new Task<>() {
            @Override
            protected Path call() throws Exception {
                persistInvestigationSnapshot();
                return exportInvestigationPdf(reason, investigatorName);
            }
        };

        exportTask.setOnSucceeded(event -> {
            Path pdfPath = exportTask.getValue();
            statusLabel.setText("Investigación finalizada. PDF generado: " + pdfPath.getFileName());
            hidePdfLoadingOverlay();
            showAlert(Alert.AlertType.INFORMATION,
                    "Investigación finalizada por " + reason + ".\nPDF generado en:\n" + pdfPath.toAbsolutePath());
            investigationFinished = true;
            finishingInProgress = false;

            deleteGameDataAndExit();
        });

        exportTask.setOnFailed(event -> {
            Throwable failure = exportTask.getException();
            statusLabel.setText("Investigación finalizada, pero hubo error al generar el PDF.");
            hidePdfLoadingOverlay();

            Path backupTxt = exportBackupTextFile(reason, investigatorName);
            String backupMsg = "";
            if (backupTxt != null) {
                backupMsg = "\n\nSe ha generado un archivo de respaldo de texto plano con tu trabajo en:\n"
                        + backupTxt.toAbsolutePath();
            } else {
                backupMsg = "\n\n(No se pudo crear el archivo de respaldo de texto plano en Descargas).";
            }

            showAlert(Alert.AlertType.ERROR,
                    "Investigación finalizada por " + reason + ", pero no se pudo generar el PDF.\n"
                            + (failure == null ? "Error desconocido" : failure.getMessage())
                            + backupMsg);

            investigationFinished = true;
            finishingInProgress = false;

            if (backupTxt != null) {
                deleteGameDataAndExit();
            }
        });

        Thread worker = new Thread(exportTask, "NEXUS-pdf-export");
        worker.setDaemon(true);
        worker.start();
    }

    private void showIsolatedNodesOverlay() {
        rebuildIsolatedNodesOverlay();
        isolatedNodesOverlay.setVisible(true);
        isolatedNodesOverlay.setManaged(true);
        isolatedNodesOverlay.toFront();
    }

    private void hideIsolatedNodesOverlay() {
        isolatedNodesOverlay.setVisible(false);
        isolatedNodesOverlay.setManaged(false);
        pendingFinalizationReason = null;
    }

    private void rebuildIsolatedNodesOverlay() {
        isolatedNodeReasonFields.clear();
        isolatedNodesEntriesContainer.getChildren().clear();

        List<CaseNode> isolatedNodes = nodes.stream()
                .filter(node -> !isGroupedNode(node))
                .collect(Collectors.toList());

        if (isolatedNodes.isEmpty()) {
            Label emptyLabel = new Label("No hay casos aislados pendientes.");
            emptyLabel.getStyleClass().add("app-subtitle");
            isolatedNodesEntriesContainer.getChildren().add(emptyLabel);
            return;
        }

        for (CaseNode node : isolatedNodes) {
            Label nodeTitle = new Label(node.getCaso().getNombre());
            nodeTitle.getStyleClass().add("section-title");

            Label nodeHint = new Label("Explica por qué este caso se mantiene como caso aislado.");
            nodeHint.getStyleClass().add("app-subtitle");
            nodeHint.setWrapText(true);

            TextArea reasonField = new TextArea();
            reasonField.getStyleClass().add("text-area");
            reasonField.setPromptText("Justificación del caso aislado...");
            reasonField.setPrefRowCount(3);
            reasonField.setWrapText(true);
            isolatedNodeReasonFields.put(node, reasonField);

            VBox row = new VBox(8, nodeTitle, nodeHint, reasonField);
            row.getStyleClass().add("panel-card");
            row.setPadding(new Insets(12));
            isolatedNodesEntriesContainer.getChildren().add(row);
        }
    }

    private void confirmIsolatedNodesAndFinalize() {
        if (investigationFinished || finishingInProgress) {
            return;
        }

        Map<String, String> justifications = new HashMap<>();
        for (Map.Entry<CaseNode, TextArea> entry : isolatedNodeReasonFields.entrySet()) {
            String reason = entry.getValue().getText() == null ? "" : entry.getValue().getText().trim();
            justifications.put(entry.getKey().getCaso().getNombre(), reason);
        }

        isolatedNodeJustifications.clear();
        isolatedNodeJustifications.putAll(justifications);
        hideIsolatedNodesOverlay();
        beginFinalization(pendingFinalizationReason == null ? "finalización manual" : pendingFinalizationReason);
    }

    private boolean isolatedNodeJustificationsAreMissing() {
        long isolatedCount = countUngroupedNodes();
        return isolatedCount > 0 && isolatedNodeJustifications.size() != isolatedCount;
    }

    private long countUngroupedNodes() {
        return nodes.stream()
                .filter(node -> !isGroupedNode(node))
                .count();
    }

    private void showPdfLoadingOverlay() {
        Platform.runLater(() -> {
            pdfLoadingOverlay.setVisible(true);
            pdfLoadingOverlay.setManaged(true);
            pdfLoadingOverlay.toFront();
        });
    }

    private void hidePdfLoadingOverlay() {
        Platform.runLater(() -> {
            pdfLoadingOverlay.setVisible(false);
            pdfLoadingOverlay.setManaged(false);
        });
    }

    private void persistInvestigationSnapshot() {
        try {
            Files.createDirectories(investigationSnapshotPath.getParent());
            Files.writeString(investigationSnapshotPath, buildInvestigationJson(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            statusLabel.setText("No se pudo persistir el estado de investigación.");
        }
    }

    private String buildInvestigationJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"sessionId\": \"").append(escapeJson(investigationSessionId)).append("\",\n");
        json.append("  \"generatedAt\": \"")
                .append(escapeJson(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))
                .append("\",\n");
        json.append("  \"connections\": [\n");

        for (int i = 0; i < connections.size(); i++) {
            Connection connection = connections.get(i);
            json.append("    {\n");
            json.append("      \"from\": \"").append(escapeJson(connection.from.getCaso().getNombre())).append("\",\n");
            json.append("      \"to\": \"").append(escapeJson(connection.to.getCaso().getNombre())).append("\",\n");
            json.append("      \"detail\": \"").append(escapeJson(connection.reason)).append("\",\n");
            json.append("      \"groupId\": \"").append(escapeJson(connection.groupId != null ? connection.groupId : "Grupo Legado")).append("\"\n");
            json.append("    }");
            if (i < connections.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ],\n");
        json.append("  \"groups\": [\n");

        for (int i = 0; i < currentClusters.size(); i++) {
            GroupCluster cluster = currentClusters.get(i);
            GroupMeta meta = cluster.meta;
            json.append("    {\n");
            json.append("      \"name\": \"").append(escapeJson(meta.name)).append("\",\n");
            json.append("      \"reason\": \"").append(escapeJson(meta.reason)).append("\",\n");
            json.append("      \"finalized\": ").append(meta.finalized).append(",\n");
            json.append("      \"decision\": \"").append(escapeJson(meta.mode != null ? meta.mode : ""))
                    .append("\",\n");
            json.append("      \"decisionDetail\": \"")
                    .append(escapeJson(meta.decisionDetail != null ? meta.decisionDetail : "")).append("\",\n");
            json.append("      \"members\": [");
            for (int j = 0; j < cluster.members.size(); j++) {
                CaseNode member = cluster.members.get(j);
                json.append("\"").append(escapeJson(member.getCaso().getNombre())).append("\"");
                if (j < cluster.members.size() - 1) {
                    json.append(", ");
                }
            }
            json.append("]\n");
            json.append("    }");
            if (i < currentClusters.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ],\n");
        json.append("  \"isolatedNodes\": [\n");

        List<CaseNode> isolatedNodes = nodes.stream()
                .filter(node -> !isGroupedNode(node))
                .collect(Collectors.toList());
        for (int i = 0; i < isolatedNodes.size(); i++) {
            CaseNode node = isolatedNodes.get(i);
            json.append("    {\n");
            json.append("      \"name\": \"").append(escapeJson(node.getCaso().getNombre())).append("\",\n");
            json.append("      \"reason\": \"")
                    .append(escapeJson(
                            normalizeJustification(isolatedNodeJustifications.get(node.getCaso().getNombre()))))
                    .append("\"\n");
            json.append("    }");
            if (i < isolatedNodes.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ],\n");
        json.append("  \"alerts\": [\n");

        List<DistractionAlertManager.AlertRecord> alerts = DistractionAlertManager.getAlertRecords();
        for (int i = 0; i < alerts.size(); i++) {
            DistractionAlertManager.AlertRecord alert = alerts.get(i);
            json.append("    {\n");
            json.append("      \"timestamp\": \"").append(escapeJson(alert.getTimestamp())).append("\",\n");
            json.append("      \"image\": \"").append(escapeJson(alert.getImageName())).append("\",\n");
            json.append("      \"status\": \"").append(escapeJson(alert.getStatus())).append("\",\n");
            json.append("      \"response\": \"").append(escapeJson(alert.getResponseText())).append("\"\n");
            json.append("    }");
            if (i < alerts.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}\n");
        return json.toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private void disableInvestigationPanel() {
        board.setDisable(true);
        sidebar.setDisable(true);
        analyticalTab.setDisable(true);
        casesTab.setDisable(true);
        finishInvestigationButton.setDisable(true);
        handMode = false;
        panning = false;
        board.setCursor(javafx.scene.Cursor.DEFAULT);
        hideConnectionJustificationOverlay();
    }

    private Path exportBackupTextFile(String endReason, String investigatorName) {
        try {
            Path downloads = Paths.get(System.getProperty("user.home"), "Downloads");
            Files.createDirectories(downloads);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Path backupFile = downloads.resolve("nexus-investigacion-backup-" + timestamp + ".txt");

            ReportData reportData = buildInvestigationReportData(endReason, investigatorName);

            StringBuilder content = new StringBuilder();
            content.append("==================================================\n");
            content.append("NEXUS DAE - RESPALDO DE INVESTIGACIÓN (TEXTO PLANO)\n");
            content.append("==================================================\n\n");
            content.append("Integrantes: ").append(reportData.investigatorName()).append("\n");
            content.append("Fecha de cierre: ").append(reportData.closedAt()).append("\n");
            content.append("Motivo de cierre: ").append(reportData.endReason()).append("\n");
            content.append("Duración máxima: ").append(reportData.maxDuration()).append("\n\n");

            content.append("--------------------------------------------------\n");
            content.append("1. CONEXIONES ENTRE CASOS\n");
            content.append("--------------------------------------------------\n");
            if (reportData.connections().isEmpty()) {
                content.append("- No se registraron conexiones.\n");
            } else {
                int index = 1;
                for (var connection : reportData.connections()) {
                    content.append(index).append(". ").append(connection.caseFrom()).append(" <-> ")
                            .append(connection.caseTo()).append("\n");
                    content.append("   Tipo de asociación: ").append(connection.associationType()).append("\n");
                    content.append("   Justificación: ").append(connection.justification()).append("\n\n");
                    index++;
                }
            }

            content.append("--------------------------------------------------\n");
            content.append("2. GRUPOS DE CASOS\n");
            content.append("--------------------------------------------------\n");
            if (reportData.groups().isEmpty()) {
                content.append("- No se detectaron grupos.\n");
            } else {
                int index = 1;
                for (var group : reportData.groups()) {
                    String status = group.finalized() ? "Finalizado" : "En proceso";
                    content.append(index).append(". ").append(group.name()).append(" (").append(group.memberCount())
                            .append(" casos) [").append(status).append("]\n");
                    content.append("   Justificación del grupo: ").append(group.groupJustification()).append("\n");
                    content.append("   Casos incluidos: ").append(String.join(", ", group.memberNames())).append("\n");
                    if (!group.decisions().isEmpty()) {
                        content.append("   Decisiones del fiscal:\n");
                        for (var decision : group.decisions()) {
                            content.append("     - Decisión: ").append(decision.title()).append("\n");
                            content.append("       Justificación: ").append(decision.justification()).append("\n");
                        }
                    }
                    content.append("\n");
                    index++;
                }
            }

            content.append("--------------------------------------------------\n");
            content.append("3. CASOS AISLADOS\n");
            content.append("--------------------------------------------------\n");
            if (reportData.isolatedCases().isEmpty()) {
                content.append("- No quedaron casos aislados.\n");
            } else {
                int index = 1;
                for (var isolated : reportData.isolatedCases()) {
                    content.append(index).append(". ").append(isolated.caseName()).append("\n");
                    content.append("   Justificación: ").append(isolated.justification()).append("\n\n");
                    index++;
                }
            }

            Files.writeString(backupFile, content.toString(), java.nio.charset.StandardCharsets.UTF_8);
            return backupFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Path exportInvestigationPdf(String endReason, String investigatorName) throws IOException {
        Path downloads = Paths.get(System.getProperty("user.home"), "Downloads");
        Files.createDirectories(downloads);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path output = downloads.resolve("nexus-investigacion-" + timestamp + ".pdf");

        ReportData reportData = buildInvestigationReportData(endReason, investigatorName);
        InvestigationReportPdfExporter.generate(output, reportData);
        return output;
    }

    private ReportData buildInvestigationReportData(String endReason, String investigatorName) {
        List<ConnectionEntry> connectionEntries = connections.stream()
                .map(connection -> InvestigationReportPdfExporter.parseConnection(
                        connection.from.getCaso().getNombre(),
                        connection.to.getCaso().getNombre(),
                        connection.reason))
                .toList();

        List<GroupEntry> groupEntries = currentClusters.stream()
                .map(cluster -> {
                    GroupMeta meta = cluster.meta;
                    List<DecisionEntry> decisions = InvestigationReportPdfExporter.parseDecisions(
                            meta.mode,
                            meta.decisionDetail);
                    List<String> memberNames = cluster.members.stream()
                            .map(member -> member.getCaso().getNombre())
                            .toList();
                    return new GroupEntry(
                            meta.name == null ? "Grupo sin nombre" : meta.name,
                            cluster.members.size(),
                            meta.finalized,
                            normalizeJustification(meta.reason),
                            memberNames,
                            decisions);
                })
                .toList();

        List<IsolatedEntry> isolatedEntries = nodes.stream()
                .filter(node -> !isGroupedNode(node))
                .map(node -> new IsolatedEntry(
                        node.getCaso().getNombre(),
                        normalizeJustification(isolatedNodeJustifications.get(node.getCaso().getNombre()))))
                .toList();

        List<AlertEntry> alertEntries = DistractionAlertManager.getAlertRecords().stream()
                .map(alert -> {
                    boolean responded = InvestigationReportPdfExporter.alertWasAnswered(
                            alert.getStatus(),
                            alert.getResponseText());
                    return new AlertEntry(
                            alert.getTimestamp(),
                            alert.getImageName(),
                            normalizeJustification(alert.getStatus()),
                            alert.getResponseText() == null ? "" : alert.getResponseText().trim(),
                            InvestigationReportPdfExporter.resolveAlertImagePath(alert.getImageName()),
                            responded);
                })
                .toList();

        return new ReportData(
                investigatorName,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                endReason,
                formatDuration(INVESTIGATION_DURATION),
                connectionEntries,
                groupEntries,
                isolatedEntries,
                alertEntries);
    }

    private String normalizeJustification(String justification) {
        String value = justification == null ? "" : justification.trim();
        return value.isBlank() ? "Sin justificación" : value;
    }

    private boolean isGroupedNode(CaseNode node) {
        return groupedNodeSignature.containsKey(node);
    }

    private void rebuildGroupedNodesMap() {
        groupedNodeSignature.clear();
        for (GroupCluster cluster : currentClusters) {
            for (CaseNode member : cluster.members) {
                groupedNodeSignature.put(member, cluster.signature);
                member.dragging = false;
                member.vx = 0;
                member.vy = 0;
            }
        }
    }

    private void arrangeGroupedNodes() {
        int clusterCount = currentClusters.size();
        double[] centroidsX = new double[clusterCount];
        double[] centroidsY = new double[clusterCount];
        double[] halfWidths = new double[clusterCount];
        double[] halfHeights = new double[clusterCount];

        // Phase 1: Compute initial centroids and estimated bounding box dimensions for
        // each group
        for (int c = 0; c < clusterCount; c++) {
            GroupCluster cluster = currentClusters.get(c);
            int size = cluster.members.size();
            if (size < 2) {
                continue;
            }
            double cx = 0.0;
            double cy = 0.0;
            for (CaseNode member : cluster.members) {
                cx += member.centerX();
                cy += member.centerY();
            }
            centroidsX[c] = cx / size;
            centroidsY[c] = cy / size;

            // The nodes are placed in a circle of radius R around the centroid.
            // Radius R is Math.max(68.0, 28.0 + size * 10.0).
            double r = Math.max(68.0, 28.0 + size * 10.0);
            // Bounding box extends to R + NODE_RADIUS + GROUP_PADDING on each side.
            double extent = r + NODE_RADIUS + GROUP_PADDING + 15.0; // Added safety offset
            halfWidths[c] = extent;
            halfHeights[c] = extent;
        }

        // Phase 2: Iteratively push apart overlapping bounding boxes (AABB separation)
        for (int iteration = 0; iteration < 50; iteration++) {
            boolean moved = false;
            for (int i = 0; i < clusterCount; i++) {
                if (currentClusters.get(i).members.size() < 2)
                    continue;
                for (int j = i + 1; j < clusterCount; j++) {
                    if (currentClusters.get(j).members.size() < 2)
                        continue;

                    double dx = centroidsX[j] - centroidsX[i];
                    double dy = centroidsY[j] - centroidsY[i];
                    double minDistanceX = halfWidths[i] + halfWidths[j];
                    double minDistanceY = halfHeights[i] + halfHeights[j];

                    double overlapX = minDistanceX - Math.abs(dx);
                    double overlapY = minDistanceY - Math.abs(dy);

                    // If overlapping in both dimensions, we have a collision
                    if (overlapX > 0 && overlapY > 0) {
                        moved = true;
                        // Push along the axis of minimum penetration
                        if (overlapX < overlapY) {
                            double push = overlapX / 2.0;
                            if (dx >= 0) {
                                centroidsX[i] -= push;
                                centroidsX[j] += push;
                            } else {
                                centroidsX[i] += push;
                                centroidsX[j] -= push;
                            }
                        } else {
                            double push = overlapY / 2.0;
                            if (dy >= 0) {
                                centroidsY[i] -= push;
                                centroidsY[j] += push;
                            } else {
                                centroidsY[i] += push;
                                centroidsY[j] -= push;
                            }
                        }
                    }
                }
            }
            if (!moved)
                break;
        }

        // Clamp centroids to the board's safe area to keep groups completely visible
        double boardW = safeWidth();
        double boardH = safeHeight();
        for (int c = 0; c < clusterCount; c++) {
            if (currentClusters.get(c).members.size() < 2)
                continue;
            centroidsX[c] = clamp(centroidsX[c], halfWidths[c], boardW - halfWidths[c]);
            centroidsY[c] = clamp(centroidsY[c], halfHeights[c], boardH - halfHeights[c]);
        }

        // Phase 3: Position the member nodes around their group's resolved centroid
        for (int c = 0; c < clusterCount; c++) {
            GroupCluster cluster = currentClusters.get(c);
            int size = cluster.members.size();
            if (size < 2) {
                continue;
            }
            double radius = Math.max(68.0, 28.0 + size * 10.0);
            for (int i = 0; i < size; i++) {
                CaseNode member = cluster.members.get(i);
                double angle = (Math.PI * 2.0 * i) / size;
                double centerX = centroidsX[c] + Math.cos(angle) * radius;
                double centerY = centroidsY[c] + Math.sin(angle) * radius;
                double x = clamp(centerX - NODE_RADIUS, 0, boardW - NODE_DIAMETER);
                double y = clamp(centerY - NODE_RADIUS, 0, boardH - NODE_DIAMETER);
                member.setBoardPosition(x, y);
                member.vx = 0;
                member.vy = 0;
            }
        }
    }

    private Point2D resolveGroupFrameCollision(CaseNode node, double nextX, double nextY) {
        double epsilon = 0.2;
        double resolvedX = nextX;
        double resolvedY = nextY;

        for (GroupCluster cluster : currentClusters) {
            if (cluster.members.contains(node)) {
                continue;
            }

            GroupBounds bounds = computeBounds(cluster.members);
            double left = bounds.minX - GROUP_PADDING;
            double top = bounds.minY - GROUP_PADDING;
            double right = left + bounds.width + GROUP_PADDING * 2.0;
            double bottom = top + bounds.height + GROUP_PADDING * 2.0;

            double nodeLeft = resolvedX;
            double nodeTop = resolvedY;
            double nodeRight = resolvedX + NODE_DIAMETER;
            double nodeBottom = resolvedY + NODE_DIAMETER;

            boolean intersects = nodeRight > left && nodeLeft < right && nodeBottom > top && nodeTop < bottom;
            if (!intersects) {
                continue;
            }

            double overlapLeft = nodeRight - left;
            double overlapRight = right - nodeLeft;
            double overlapTop = nodeBottom - top;
            double overlapBottom = bottom - nodeTop;

            double minOverlap = Math.min(Math.min(overlapLeft, overlapRight), Math.min(overlapTop, overlapBottom));
            if (minOverlap == overlapLeft) {
                resolvedX = left - NODE_DIAMETER - epsilon;
                node.vx = -Math.abs(node.vx);
            } else if (minOverlap == overlapRight) {
                resolvedX = right + epsilon;
                node.vx = Math.abs(node.vx);
            } else if (minOverlap == overlapTop) {
                resolvedY = top - NODE_DIAMETER - epsilon;
                node.vy = -Math.abs(node.vy);
            } else {
                resolvedY = bottom + epsilon;
                node.vy = Math.abs(node.vy);
            }
        }

        resolvedX = clamp(resolvedX, 0, safeWidth() - NODE_DIAMETER);
        resolvedY = clamp(resolvedY, 0, safeHeight() - NODE_DIAMETER);
        return new Point2D(resolvedX, resolvedY);
    }

    private String buildSwatchStyle(Color color) {
        return "-fx-background-color: " + toRgb(color)
                + "; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: rgba(103,232,249,0.40); -fx-border-width: 1;";
    }

    private String toRgb(Color color) {
        int red = (int) Math.round(color.getRed() * 255);
        int green = (int) Math.round(color.getGreen() * 255);
        int blue = (int) Math.round(color.getBlue() * 255);
        return "rgb(" + red + "," + green + "," + blue + ")";
    }

    private int getMaxSequence() {
        int maxSequence = 0;
        for (GroupMeta m : metadataBySignature.values()) {
            if (m.name != null && m.name.startsWith("Grupo ")) {
                try {
                    int num = Integer.parseInt(m.name.substring(6).trim());
                    if (num > maxSequence) maxSequence = num;
                } catch (Exception e) {}
            }
        }
        return maxSequence;
    }

    private List<GroupCluster> detectClusters() {
        List<GroupCluster> clusters = new ArrayList<>();
        
        java.util.Map<String, java.util.List<Connection>> connsByGroup = connections.stream()
            .filter(c -> c.groupId != null && !c.groupId.isEmpty())
            .collect(Collectors.groupingBy(c -> c.groupId));

        for (java.util.Map.Entry<String, java.util.List<Connection>> entry : connsByGroup.entrySet()) {
            String groupName = entry.getKey();
            Set<CaseNode> component = new HashSet<>();
            for (Connection c : entry.getValue()) {
                component.add(c.from);
                component.add(c.to);
            }
            
            if (component.size() < 2) continue;
            
            List<CaseNode> members = component.stream()
                    .sorted(Comparator.comparing(caseNode -> caseNode.getCaso().getNombre()))
                    .collect(Collectors.toList());
                    
            String signature = groupName;
            GroupMeta meta = metadataBySignature.get(signature);
            if (meta == null) {
                String colorHex = "#67e8f9";
                if (groupName != null && !groupName.isBlank()) {
                    String[] colors = {"#ef4444", "#3b82f6", "#10b981", "#f59e0b", "#8b5cf6", "#ec4899", "#14b8a6", "#f97316"};
                    int idx = Math.abs(groupName.hashCode()) % colors.length;
                    colorHex = colors[idx];
                }
                
                meta = new GroupMeta(
                        groupName,
                        Color.web(colorHex),
                        "Asociado por modalidad",
                        "Sin justificación registrada",
                        "",
                        false);
                metadataBySignature.put(signature, meta);
            }
            clusters.add(new GroupCluster(signature, members, meta));
        }

        Set<String> activeSignatures = clusters.stream()
                .map(c -> c.signature)
                .collect(Collectors.toSet());
        metadataBySignature.keySet().retainAll(activeSignatures);

        return clusters;
    }

    private GroupMeta findBestMatchingMeta(List<CaseNode> newMembers) {
        Set<String> newNames = newMembers.stream()
                .map(n -> n.getCaso().getNombre())
                .collect(Collectors.toSet());

        String bestSignature = null;
        int bestOverlap = 0;

        for (Map.Entry<String, GroupMeta> entry : metadataBySignature.entrySet()) {
            Set<String> oldNames = new HashSet<>(List.of(entry.getKey().split("\\|")));
            int overlap = 0;
            for (String name : oldNames) {
                if (newNames.contains(name)) {
                    overlap++;
                }
            }
            // Must share at least 2 members (a real group) and be the best match
            if (overlap >= 2 && overlap > bestOverlap) {
                bestOverlap = overlap;
                bestSignature = entry.getKey();
            }
        }

        if (bestSignature != null) {
            GroupMeta inherited = metadataBySignature.remove(bestSignature);
            // Also migrate or cleanup the overlay to avoid visual stacking
            GroupOverlay oldOverlay = overlayBySignature.remove(bestSignature);
            if (oldOverlay != null) {
                groupLayer.getChildren().removeAll(oldOverlay.rectangle, oldOverlay.nameLabel);
            }
            return inherited;
        }
        return null;
    }


    private void updateGroupOverlays() {
        // Limpiar overlays obsoletos
        Set<String> validSignatures = currentClusters.stream()
                .map(c -> c.signature)
                .collect(Collectors.toSet());

        overlayBySignature.entrySet().removeIf(entry -> {
            if (!validSignatures.contains(entry.getKey())) {
                groupLayer.getChildren().removeAll(entry.getValue().rectangle, entry.getValue().nameLabel);
                return true;
            }
            return false;
        });

        // Actualizar overlays válidos
        for (GroupCluster cluster : currentClusters) {
            GroupOverlay overlay = overlayBySignature.computeIfAbsent(cluster.signature, key -> {
                Rectangle rectangle = new Rectangle();
                rectangle.setMouseTransparent(true);
                rectangle.setArcWidth(26);
                rectangle.setArcHeight(26);
                Text nameLabel = new Text();
                nameLabel.setMouseTransparent(true);
                nameLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
                nameLabel.setFill(Color.web("#38bdf8"));
                groupLayer.getChildren().addAll(rectangle, nameLabel);
                return new GroupOverlay(rectangle, nameLabel);
            });

            GroupMeta meta = cluster.meta;
            GroupBounds bounds = computeBounds(cluster.members);
            overlay.rectangle.setX(bounds.minX - GROUP_PADDING);
            overlay.rectangle.setY(bounds.minY - GROUP_PADDING);
            overlay.rectangle.setWidth(bounds.width + GROUP_PADDING * 2.0);
            overlay.rectangle.setHeight(bounds.height + GROUP_PADDING * 2.0);
            overlay.rectangle
                    .setFill(Color.color(meta.color.getRed(), meta.color.getGreen(), meta.color.getBlue(), 0.08));
            overlay.rectangle
                    .setStroke(Color.color(meta.color.getRed(), meta.color.getGreen(), meta.color.getBlue(), 0.92));
            overlay.rectangle.setStrokeWidth(3.2);

            overlay.nameLabel.setText(meta.name);
            overlay.nameLabel.setX(bounds.minX - GROUP_PADDING + 12);
            overlay.nameLabel.setY(bounds.minY - GROUP_PADDING + 26);
        }
    }

    private GroupBounds computeBounds(List<CaseNode> members) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        for (CaseNode member : members) {
            minX = Math.min(minX, member.getLayoutX());
            minY = Math.min(minY, member.getLayoutY());
            maxX = Math.max(maxX, member.getLayoutX() + NODE_DIAMETER);
            maxY = Math.max(maxY, member.getLayoutY() + NODE_DIAMETER);
        }

        return new GroupBounds(minX, minY, maxX - minX, maxY - minY);
    }

    private String createSignature(List<CaseNode> members) {
        return members.stream()
                .map(member -> member.getCaso().getNombre())
                .sorted()
                .collect(Collectors.joining("|"));
    }

    private Color defaultGroupColor(String signature) {
        int hash = Math.abs(signature.hashCode());
        double hue = hash % 360;
        return Color.hsb(hue, 0.72, 0.95);
    }

    private void onGroupSelected(GroupCluster cluster) {
        if (cluster == null) {
            selectedGroupSignature = null;
            groupSummaryLabel.setText("Selecciona un grupo para ver sus casos conectados.");
            groupReasonField.clear();
            groupColorPicker.setValue(Color.web("#38bdf8"));
            return;
        }

        selectedGroupSignature = cluster.signature;
        GroupMeta meta = cluster.meta;
        groupSummaryLabel.setText("Casos del grupo: " + cluster.members.stream()
                .map(node -> node.getCaso().getNombre())
                .collect(Collectors.joining(", ")));
        groupReasonField.setText(meta.reason);
        groupColorPicker.setValue(meta.color);
    }

    private void clearGroupSelection() {
        selectedGroupSignature = null;
        groupList.getSelectionModel().clearSelection();
        groupSummaryLabel.setText("Selecciona un grupo para ver sus casos conectados.");
        groupReasonField.clear();
        groupColorPicker.setValue(Color.web("#38bdf8"));
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("NEXUS DAE");
        alert.showAndWait();
    }

    private void deleteGameDataAndExit() {
        try {
            Files.deleteIfExists(investigationSnapshotPath);
            Path casosDir = Paths.get("casos");
            if (Files.exists(casosDir)) {
                Files.walk(casosDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
            }
            Path alertasDir = Paths.get("alertas");
            if (Files.exists(alertasDir)) {
                Files.walk(alertasDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
            }

            // Eliminar el instalador del escritorio según el sistema operativo
            String home = System.getProperty("user.home");
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                String zipName = "NEXUS-DAE-1.0.0-windows.zip";
                Files.deleteIfExists(Paths.get(home, "Desktop", zipName));
                Files.deleteIfExists(Paths.get(home, "Escritorio", zipName));
                Files.deleteIfExists(Paths.get(home, "Downloads", zipName));
                Files.deleteIfExists(Paths.get(home, "Descargas", zipName));
            } else if (os.contains("mac")) {
                Files.deleteIfExists(Paths.get(home, "Desktop", "NEXUS-DAE-1.0.0.dmg"));
                Files.deleteIfExists(Paths.get(home, "Downloads", "NEXUS-DAE-1.0.0.dmg"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Platform.exit();
        System.exit(0);
    }

    private HBox buildActionChip(String labelText) {
        Label checkbox = new Label("☐");
        checkbox.getStyleClass().add("muted-text");
        Label label = new Label(labelText);
        label.getStyleClass().add("action-chip");
        HBox chip = new HBox(10, checkbox, label);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().add("action-chip");
        return chip;
    }

    private double safeWidth() {
        return WORLD_WIDTH;
    }

    private double safeHeight() {
        return WORLD_HEIGHT;
    }

    private double clamp(double value, double min, double max) {
        if (max <= min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    public BorderPane getView() {
        return view;
    }

    private static final class Connection {

        private final CaseNode from;
        private final CaseNode to;
        private final String reason;
        private final String groupId;
        private final Line line;

        private Connection(CaseNode from, CaseNode to, String reason, String groupId) {
            this.from = from;
            this.to = to;
            this.reason = reason;
            this.groupId = groupId;
            this.line = new Line();
            
            String colorHex = "#67e8f9"; // default
            if (groupId != null && !groupId.isBlank()) {
                String[] colors = {"#ef4444", "#3b82f6", "#10b981", "#f59e0b", "#8b5cf6", "#ec4899", "#14b8a6", "#f97316"};
                int idx = Math.abs(groupId.hashCode()) % colors.length;
                colorHex = colors[idx];
            }
            this.line.setStroke(Color.web(colorHex, 0.78));
            this.line.setStrokeWidth(2.2);
            this.line.setMouseTransparent(true);
        }
    }

    private record DecisionOptionDef(String id, String label, String justificationPrompt) {
    }

    private static final List<DecisionOptionDef> DECISION_OPTIONS = List.of(
            new DecisionOptionDef(
                    "police",
                    "1. Orden a la Policía Judicial.",
                    "¿Qué actividad ordenará la Policía Judicial?"),
            new DecisionOptionDef(
                    "archive",
                    "2. Orden de archivo.",
                    "¿Cuál causal de archivo aplica?"),
            new DecisionOptionDef(
                    "hearing",
                    "3. Solicitud de audiencia ante juez de control de garantías.",
                    "¿Qué solicitud de garantías presentará?"),
            new DecisionOptionDef(
                    "prioritize",
                    "4. Priorizar investigación.",
                    "¿Por qué debe priorizarse esta investigación?"),
            new DecisionOptionDef(
                    "victims",
                    "5. Caracterización de víctimas.",
                    "¿Por qué debe caracterizarse a las víctimas?"),
            new DecisionOptionDef(
                    "fenomeno",
                    "6. Fenómeno Criminal",
                    "Justifique"),
            new DecisionOptionDef(
                    "other",
                    "7. Otro.",
                    "¿Cuál es la otra decisión y su fundamento?"));

    public static final class GroupMeta {

        public String name;
        public Color color;
        private String mode;
        public String reason;
        private String decisionDetail;
        private boolean finalized;

        private GroupMeta(String name, Color color, String mode, String reason, String decisionDetail,
                boolean finalized) {
            this.name = name;
            this.color = color;
            this.mode = mode;
            this.reason = reason;
            this.decisionDetail = decisionDetail;
            this.finalized = finalized;
        }
    }

    private static final class GroupOverlay {

        private final Rectangle rectangle;
        private final Text nameLabel;

        private GroupOverlay(Rectangle rectangle, Text nameLabel) {
            this.rectangle = rectangle;
            this.nameLabel = nameLabel;
        }
    }

    static final class GroupCluster {

        final String signature;
        final List<CaseNode> members;
        final GroupMeta meta;

        GroupCluster(String signature, List<CaseNode> members, GroupMeta meta) {
            this.signature = signature;
            this.members = members;
            this.meta = meta;
        }

        @Override
        public String toString() {
            return meta.name + " · " + members.size() + " casos";
        }

        public String getName() {
            return meta.name;
        }

        public Color getColor() {
            return meta.color;
        }
    }

    
    public List<GroupCluster> findGroupsForCase(Caso caso) {
        if (caso == null) {
            return java.util.Collections.emptyList();
        }
        CaseNode targetNode = null;
        for (CaseNode node : nodes) {
            if (node.getCaso().getNombre().equalsIgnoreCase(caso.getNombre())) {
                targetNode = node;
                break;
            }
        }
        if (targetNode == null) return java.util.Collections.emptyList();
        
        List<GroupCluster> result = new ArrayList<>();
        for (GroupCluster cluster : currentClusters) {
            if (cluster.members.contains(targetNode)) {
                result.add(cluster);
            }
        }
        return result;
    }

    public GroupCluster findGroupForCase(Caso caso) {
        if (caso == null) {
            return null;
        }
        for (CaseNode node : nodes) {
            if (!node.getCaso().getNombre().equalsIgnoreCase(caso.getNombre())) {
                continue;
            }
            if (!isGroupedNode(node)) {
                return null;
            }
            String signature = groupedNodeSignature.get(node);
            for (GroupCluster cluster : currentClusters) {
                if (cluster.signature.equals(signature)) {
                    return cluster;
                }
            }
            return null;
        }
        return null;
    }

    public boolean isCaseGrouped(Caso caso) {
        if (caso == null) {
            return false;
        }
        for (CaseNode node : nodes) {
            if (node.getCaso().getNombre().equalsIgnoreCase(caso.getNombre())) {
                return isGroupedNode(node);
            }
        }
        return false;
    }

    public List<GroupCluster> getCurrentClusters() {
        return currentClusters;
    }

    public void addCasesToGroup(List<Caso> casesToAdd, GroupCluster targetGroup, String basis, String detail,
            String reason) {
        if (casesToAdd == null || casesToAdd.isEmpty() || targetGroup == null || investigationFinished) {
            return;
        }

        List<CaseNode> targetNodes = new ArrayList<>();
        for (Caso c : casesToAdd) {
            for (CaseNode node : nodes) {
                if (node.getCaso().getNombre().equalsIgnoreCase(c.getNombre())) {
                    targetNodes.add(node);
                    break;
                }
            }
        }

        if (targetNodes.isEmpty()) {
            return;
        }

        CaseNode targetMember = targetGroup.members.get(0);

        String connectionSummary = "Asociado por: " + basis;
        if (detail != null && !detail.isBlank()) {
            connectionSummary += " - " + detail;
        }
        connectionSummary += " | Justificación: " + reason;

        // Connect the selected cases consecutively
        for (int i = 0; i < targetNodes.size() - 1; i++) {
            CaseNode from = targetNodes.get(i);
            CaseNode to = targetNodes.get(i + 1);
            boolean alreadyConnected = connections.stream()
                    .anyMatch(conn -> (conn.from == from && conn.to == to) || (conn.from == to && conn.to == from));
            if (!alreadyConnected) {
                connections.add(new Connection(from, to, connectionSummary, "Grupo Legado"));
            }
        }

        // Connect the first of the selected cases to the target group member
        CaseNode firstNewNode = targetNodes.get(0);
        
        String targetGroupSignature = null;
        for (GroupCluster cluster : currentClusters) {
            if (cluster.members.contains(targetMember)) {
                targetGroupSignature = cluster.signature;
                break;
            }
        }
        if (targetGroupSignature == null) {
            targetGroupSignature = "Grupo Legado";
        }
        String finalTargetGroupSignature = targetGroupSignature;
        
        boolean alreadyConnected = connections.stream()
                .anyMatch(conn -> ((conn.from == firstNewNode && conn.to == targetMember)
                        || (conn.from == targetMember && conn.to == firstNewNode)) && finalTargetGroupSignature.equals(conn.groupId));
        if (!alreadyConnected) {
            connections.add(new Connection(firstNewNode, targetMember, connectionSummary, finalTargetGroupSignature));
        }

        refreshConnections();
        refreshGroups();
        persistInvestigationSnapshot();
    }

    public void createBatchConnections(List<Caso> casos, String basis, String detail, String reason, String customGroupName) {
        if (casos == null || casos.size() < 2 || investigationFinished) {
            return;
        }

        List<CaseNode> targetNodes = new ArrayList<>();
        for (Caso c : casos) {
            for (CaseNode node : nodes) {
                if (node.getCaso().getNombre().equalsIgnoreCase(c.getNombre())) {
                    targetNodes.add(node);
                    break;
                }
            }
        }

        if (targetNodes.size() < 2) {
            return;
        }

        String connectionSummary = "Asociado por: " + basis;
        if (detail != null && !detail.isBlank()) {
            connectionSummary += " - " + detail;
        }
        connectionSummary += " | Justificación: " + reason;

        // Connect consecutively
        for (int i = 0; i < targetNodes.size() - 1; i++) {
            CaseNode from = targetNodes.get(i);
            CaseNode to = targetNodes.get(i + 1);

            String actualGroupId = customGroupName != null && !customGroupName.isBlank() ? customGroupName : "Grupo " + (getMaxSequence() + 1);
            boolean alreadyConnected = connections.stream()
                    .anyMatch(conn -> ((conn.from == from && conn.to == to) || (conn.from == to && conn.to == from)) && actualGroupId.equals(conn.groupId));
            if (!alreadyConnected) {
                connections.add(new Connection(from, to, connectionSummary, actualGroupId));
            }
        }

        refreshConnections();

        if (customGroupName != null && !customGroupName.isBlank()) {
            List<GroupCluster> clusters = detectClusters();
            for (GroupCluster gc : clusters) {
                if (gc.members.contains(targetNodes.get(0))) {
                    GroupMeta oldMeta = gc.meta;
                    if (oldMeta != null) {
                        GroupMeta newMeta = new GroupMeta(
                                customGroupName, oldMeta.color, oldMeta.mode, oldMeta.reason,
                                oldMeta.decisionDetail, oldMeta.finalized
                        );
                        metadataBySignature.put(gc.signature, newMeta);
                    }
                    break;
                }
            }
        }

        refreshGroups();
        persistInvestigationSnapshot();
    }

    public void removeConnectionsForCases(List<Caso> casos) {
        if (casos == null || casos.isEmpty() || investigationFinished) {
            return;
        }

        java.util.Set<String> names = casos.stream()
                .filter(c -> c != null && c.getNombre() != null)
                .map(c -> c.getNombre().toLowerCase())
                .collect(java.util.stream.Collectors.toSet());
        if (names.isEmpty()) {
            return;
        }

        boolean removed = connections.removeIf(conn -> {
            String from = conn.from.getCaso().getNombre().toLowerCase();
            String to = conn.to.getCaso().getNombre().toLowerCase();
            return names.contains(from) || names.contains(to);
        });
        if (!removed) {
            return;
        }

        refreshConnections();
        refreshGroups();
        restoreMovementForUngroupedNodes();
        persistInvestigationSnapshot();
    }

    private void restoreInvestigationFromSnapshot() {
        if (!Files.exists(investigationSnapshotPath)) {
            return;
        }

        try {
            String json = Files.readString(investigationSnapshotPath, StandardCharsets.UTF_8);

            // --- Restore connections ---
            int connIndex = json.indexOf("\"connections\":");
            if (connIndex >= 0) {
                int connArrayStart = json.indexOf('[', connIndex);
                int connArrayEnd = findMatchingBracket(json, connArrayStart);
                if (connArrayStart >= 0 && connArrayEnd > connArrayStart) {
                    String connSection = json.substring(connArrayStart, connArrayEnd + 1);
                    int objStart = 0;
                    while (true) {
                        objStart = connSection.indexOf('{', objStart);
                        if (objStart < 0)
                            break;
                        int objEnd = connSection.indexOf('}', objStart);
                        if (objEnd < 0)
                            break;
                        String obj = connSection.substring(objStart, objEnd + 1);
                        String from = extractJsonValue(obj, "from");
                        String to = extractJsonValue(obj, "to");
                        String detail = extractJsonValue(obj, "detail");
                        String rawGroupId = extractJsonValue(obj, "groupId");
                        String finalGroupId = rawGroupId.isEmpty() ? "Grupo Legado" : rawGroupId;
                        if (!from.isEmpty() && !to.isEmpty()) {
                            CaseNode nodeFrom = findNodeByName(from);
                            CaseNode nodeTo = findNodeByName(to);
                            if (nodeFrom != null && nodeTo != null) {
                                boolean alreadyConnected = connections.stream()
                                        .anyMatch(c -> ((c.from == nodeFrom && c.to == nodeTo) || (c.from == nodeTo && c.to == nodeFrom)) && finalGroupId.equals(c.groupId));
                                if (!alreadyConnected) {
                                    connections.add(new Connection(nodeFrom, nodeTo, detail, finalGroupId));
                                }
                            }
                        }
                        objStart = objEnd + 1;
                    }
                }
            }

            // Rebuild clusters so we can populate metadataBySignature
            currentClusters = detectClusters();
            rebuildGroupedNodesMap();

            // --- Restore groups metadata ---
            int groupsIndex = json.indexOf("\"groups\":");
            if (groupsIndex >= 0) {
                int groupsArrayStart = json.indexOf('[', groupsIndex);
                int groupsArrayEnd = findMatchingBracket(json, groupsArrayStart);
                if (groupsArrayStart >= 0 && groupsArrayEnd > groupsArrayStart) {
                    String groupsSection = json.substring(groupsArrayStart, groupsArrayEnd + 1);
                    int objStart = 0;
                    while (true) {
                        objStart = groupsSection.indexOf('{', objStart);
                        if (objStart < 0)
                            break;
                        int objEnd = groupsSection.indexOf('}', objStart);
                        if (objEnd < 0)
                            break;
                        String obj = groupsSection.substring(objStart, objEnd + 1);
                        String name = extractJsonValue(obj, "name");
                        String reason = extractJsonValue(obj, "reason");
                        boolean finalized = "true".equalsIgnoreCase(extractJsonValue(obj, "finalized"));
                        String decision = extractJsonValue(obj, "decision");
                        String decisionDetail = extractJsonValue(obj, "decisionDetail");

                        int membersStart = obj.indexOf("\"members\":");
                        if (membersStart >= 0) {
                            int mArrStart = obj.indexOf('[', membersStart);
                            int mArrEnd = obj.indexOf(']', mArrStart);
                            if (mArrStart >= 0 && mArrEnd > mArrStart) {
                                String membersList = obj.substring(mArrStart + 1, mArrEnd);
                                List<String> memberNames = new ArrayList<>();
                                for (String member : membersList.split(",")) {
                                    String clean = member.replace("\"", "").trim();
                                    if (!clean.isEmpty()) {
                                        memberNames.add(clean);
                                    }
                                }
                                memberNames.sort(String.CASE_INSENSITIVE_ORDER);
                                String signature = String.join("|", memberNames);

                                GroupMeta meta = metadataBySignature.get(signature);
                                if (meta == null) {
                                    meta = new GroupMeta(name, defaultGroupColor(signature), decision, reason,
                                            decisionDetail, finalized);
                                    metadataBySignature.put(signature, meta);
                                } else {
                                    meta.name = name;
                                    meta.reason = reason;
                                    meta.finalized = finalized;
                                    meta.mode = decision;
                                    meta.decisionDetail = decisionDetail;
                                }
                            }
                        }
                        objStart = objEnd + 1;
                    }
                }
            }

            // --- Restore isolated node justifications ---
            int isolatedIndex = json.indexOf("\"isolatedNodes\":");
            if (isolatedIndex >= 0) {
                int isoArrayStart = json.indexOf('[', isolatedIndex);
                int isoArrayEnd = findMatchingBracket(json, isoArrayStart);
                if (isoArrayStart >= 0 && isoArrayEnd > isoArrayStart) {
                    String isoSection = json.substring(isoArrayStart, isoArrayEnd + 1);
                    int objStart = 0;
                    while (true) {
                        objStart = isoSection.indexOf('{', objStart);
                        if (objStart < 0)
                            break;
                        int objEnd = isoSection.indexOf('}', objStart);
                        if (objEnd < 0)
                            break;
                        String obj = isoSection.substring(objStart, objEnd + 1);
                        String isoName = extractJsonValue(obj, "name");
                        String isoReason = extractJsonValue(obj, "reason");
                        if (!isoName.isEmpty() && !isoReason.isEmpty() && !"Sin justificación".equals(isoReason)) {
                            isolatedNodeJustifications.put(isoName, isoReason);
                        }
                        objStart = objEnd + 1;
                    }
                }
            }

            // --- Restore alert records ---
            int alertsIndex = json.indexOf("\"alerts\":");
            if (alertsIndex >= 0) {
                int alertArrayStart = json.indexOf('[', alertsIndex);
                int alertArrayEnd = findMatchingBracket(json, alertArrayStart);
                if (alertArrayStart >= 0 && alertArrayEnd > alertArrayStart) {
                    String alertsSection = json.substring(alertArrayStart, alertArrayEnd + 1);
                    int objStart = 0;
                    while (true) {
                        objStart = alertsSection.indexOf('{', objStart);
                        if (objStart < 0)
                            break;
                        int objEnd = alertsSection.indexOf('}', objStart);
                        if (objEnd < 0)
                            break;
                        String obj = alertsSection.substring(objStart, objEnd + 1);
                        String timestamp = extractJsonValue(obj, "timestamp");
                        String image = extractJsonValue(obj, "image");
                        String status = extractJsonValue(obj, "status");
                        String response = extractJsonValue(obj, "response");
                        if (!image.isEmpty()) {
                            boolean exists = DistractionAlertManager.getAlertRecords().stream()
                                    .anyMatch(r -> r.getImageName().equalsIgnoreCase(image));
                            if (!exists) {
                                DistractionAlertManager.addOfflineAlertRecord(timestamp, image, status, response);
                            }
                        }
                        objStart = objEnd + 1;
                    }
                }
            }

            statusLabel.setText("Progreso restaurado desde la última sesión local.");

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("No se pudo cargar el progreso anterior.");
        }
    }

    private int findMatchingBracket(String json, int openIndex) {
        if (openIndex < 0 || openIndex >= json.length()) {
            return -1;
        }
        char open = json.charAt(openIndex);
        char close = open == '[' ? ']' : '}';
        int depth = 0;
        boolean inString = false;
        for (int i = openIndex; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && inString) {
                i++; // skip escaped char
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString)
                continue;
            if (c == open)
                depth++;
            else if (c == close) {
                depth--;
                if (depth == 0)
                    return i;
            }
        }
        return -1;
    }

    private String extractJsonValue(String block, String key) {
        int keyIndex = block.indexOf("\"" + key + "\":");
        if (keyIndex < 0) {
            return "";
        }
        int valStart = keyIndex + key.length() + 3;
        while (valStart < block.length() && Character.isWhitespace(block.charAt(valStart))) {
            valStart++;
        }
        if (valStart >= block.length()) {
            return "";
        }

        if (block.charAt(valStart) == '"') {
            int valEnd = valStart + 1;
            while (valEnd < block.length()) {
                if (block.charAt(valEnd) == '"' && block.charAt(valEnd - 1) != '\\') {
                    break;
                }
                valEnd++;
            }
            if (valEnd < block.length()) {
                return block.substring(valStart + 1, valEnd)
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")
                        .replace("\\n", "\n")
                        .replace("\\r", "\r");
            }
        } else {
            int valEnd = valStart;
            while (valEnd < block.length() && !Character.isWhitespace(block.charAt(valEnd))
                    && block.charAt(valEnd) != ',' && block.charAt(valEnd) != '}' && block.charAt(valEnd) != '\n') {
                valEnd++;
            }
            return block.substring(valStart, valEnd).trim();
        }
        return "";
    }

    private CaseNode findNodeByName(String name) {
        if (name == null)
            return null;
        for (CaseNode node : nodes) {
            if (node.getCaso().getNombre().equalsIgnoreCase(name)) {
                return node;
            }
        }
        return null;
    }

    public static final class CaseNode extends StackPane {

        private final Caso caso;
        private final String displayNumber;
        private final Label numberBadge;
        private double vx;
        private double vy;
        private boolean dragging;
        private double dragOffsetX;
        private double dragOffsetY;
        private boolean selected;

        private String getDisplayNumber() {
            return displayNumber;
        }

        private CaseNode(Caso caso, String displayNumber) {
            this.caso = caso;
            this.displayNumber = displayNumber;
            setPrefSize(NODE_DIAMETER, NODE_DIAMETER);
            setMinSize(NODE_DIAMETER, NODE_DIAMETER);
            setMaxSize(NODE_DIAMETER, NODE_DIAMETER);
            getStyleClass().add("case-node");
            setAlignment(Pos.CENTER);

            javafx.scene.shape.Circle sphere = new javafx.scene.shape.Circle(NODE_RADIUS);
            sphere.getStyleClass().add("case-sphere");

            Text title = new Text(caso.getNombre());
            title.setFill(Color.WHITE);
            title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            title.setWrappingWidth(NODE_DIAMETER - 12);
            title.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

            numberBadge = new Label(displayNumber);
            numberBadge.getStyleClass().add("case-number-badge");
            StackPane.setAlignment(numberBadge, Pos.TOP_LEFT);
            StackPane.setMargin(numberBadge, new Insets(6, 0, 0, 6));

            VBox content = new VBox(2, sphere, title);
            content.setAlignment(Pos.CENTER);
            getChildren().addAll(content, numberBadge);
        }

        private void setSelected(boolean selected) {
            this.selected = selected;
            if (selected) {
                if (!getStyleClass().contains("selected")) {
                    getStyleClass().add("selected");
                }
                setScaleX(1.18);
                setScaleY(1.18);
                toFront();
            } else {
                getStyleClass().remove("selected");
                setScaleX(1.0);
                setScaleY(1.0);
            }
        }

        private boolean isSelected() {
            return selected;
        }

        public Caso getCaso() {
            return caso;
        }

        private void setBoardPosition(double x, double y) {
            setLayoutX(x);
            setLayoutY(y);
        }

        private double centerX() {
            return getLayoutX() + NODE_RADIUS;
        }

        private double centerY() {
            return getLayoutY() + NODE_RADIUS;
        }
    }
}
