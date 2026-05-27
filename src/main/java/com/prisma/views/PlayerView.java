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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import com.prisma.data.CasoRepository;
import com.prisma.models.Caso;
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
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
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
    private final VBox casesModule;
    private final StackPane connectionDialogOverlay;
    private final StackPane pdfLoadingOverlay;
    private final StackPane isolatedNodesOverlay;
    private final Label connectionDialogTitle;
    private final Label connectionDialogSubtitle;
    private final ComboBox<String> connectionBasisBox;
    private final VBox connectionDetailContainer;
    private final TextField connectionDetailField;
    private final TextArea connectionReasonField;
    private final VBox isolatedNodesEntriesContainer;
    private final Map<CaseNode, TextArea> isolatedNodeReasonFields = new HashMap<>();
    private final VBox sidebar;
    private final ListView<String> connectionList;
    private final ListView<GroupCluster> groupList;
    private final VBox groupCardsContainer;
    private final ScrollPane groupScrollPane;
    private final ColorPicker groupColorPicker;
    private final TextArea groupReasonField;
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
        investigationSnapshotPath = Paths.get(System.getProperty("user.home"), "Documents", "PRISMA", "investigacion-" + investigationSessionId + ".json");

        timerLabel = new Label(formatDuration(INVESTIGATION_DURATION));
        timerLabel.getStyleClass().add("timer-pill");
        sessionLabel = new Label("Sesión: FISCAL");
        sessionLabel.getStyleClass().add("session-pill");

        finishInvestigationButton = new Button("Terminar investigación");
        finishInvestigationButton.getStyleClass().add("secondary-button");
        finishInvestigationButton.setOnAction(e -> finalizeInvestigation("finalización manual"));

        Button logoutButton = new Button("Cerrar sesión");
        logoutButton.getStyleClass().add("danger-button");
        logoutButton.setOnAction(e -> {
            LoginView loginView = new LoginView(stage);
            Scene scene = new Scene(loginView.getView(), 980, 680);
            Theme.apply(scene);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setFullScreen(true);
        });

        HBox topRight = new HBox(12, timerLabel, sessionLabel, finishInvestigationButton, logoutButton);
        topRight.getStyleClass().add("nav-actions");
        topRight.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(brandBlock, javafx.scene.layout.Priority.NEVER);
        HBox.setHgrow(topRight, javafx.scene.layout.Priority.NEVER);
        topBar.getChildren().addAll(brandBlock, navCenter, topRight);

        instructionLabel = new Label("Conecta esferas para crear hipótesis investigativas. Cada componente conectado se convierte en un grupo.");
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

        groupLayer = new Pane();
        groupLayer.setMouseTransparent(true);
        connectionLayer = new Pane();
        connectionLayer.setMouseTransparent(true);
        nodeLayer = new Pane();

        contentContainer = new Group();
        contentContainer.getChildren().addAll(groupLayer, connectionLayer, nodeLayer);

        board.getChildren().add(contentContainer);

        VBox boardWrapper = new VBox(12);
        boardWrapper.setPadding(new Insets(0, 16, 0, 0));
        Label boardTitle = new Label("Casos en movimiento");
        boardTitle.getStyleClass().add("section-title");
        statusLabel = new Label("Selecciona dos nodos para conectarlos.");
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
                    : "Modo selección activo: selecciona dos nodos para conectarlos.");
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

        groupList = new ListView<>();
        groupList.getStyleClass().add("group-list");
        groupList.setPrefHeight(180);
        groupList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> onGroupSelected(newValue));

        groupCardsContainer = new VBox(12);
        groupCardsContainer.setFillWidth(true);

        groupScrollPane = new ScrollPane(groupCardsContainer);
        groupScrollPane.getStyleClass().add("group-scroll");
        groupScrollPane.setFitToWidth(true);
        groupScrollPane.setPrefViewportHeight(360);

        ungroupedNodesLabel = new Label("Nodos sin grupo: 0");
        ungroupedNodesLabel.getStyleClass().add("app-subtitle");
        ungroupedNodesLabel.setWrapText(true);

        groupSummaryLabel = new Label("Selecciona un grupo para ver sus casos conectados.");
        groupSummaryLabel.getStyleClass().add("app-subtitle");
        groupSummaryLabel.setWrapText(true);

        groupColorPicker = new ColorPicker(Color.web("#38bdf8"));
        groupColorPicker.setVisible(false);
        groupColorPicker.setManaged(false);

        groupReasonField = new TextArea();
        groupReasonField.getStyleClass().add("text-area");
        groupReasonField.setPromptText("Justificación del grupo");
        groupReasonField.setPrefRowCount(4);

        Button refreshButton = new Button("Recalcular grupos");
        refreshButton.getStyleClass().add("secondary-button");
        refreshButton.setOnAction(e -> refreshGroups());

        VBox connectionsCard = new VBox(10, new Label("Conexiones (1)"), connectionList);
        connectionsCard.getStyleClass().add("panel-card");
        connectionsCard.setPadding(new Insets(14));
        VBox.setVgrow(connectionList, javafx.scene.layout.Priority.ALWAYS);

        VBox groupsCard = new VBox(10, new Label("Grupos detectados"), groupScrollPane, refreshButton);
        groupsCard.getStyleClass().add("panel-card");
        groupsCard.setPadding(new Insets(14));
        VBox.setVgrow(groupScrollPane, javafx.scene.layout.Priority.ALWAYS);

        VBox groupsHeader = new VBox(4, new Label("Grupos detectados"), ungroupedNodesLabel);
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

        // Ajustar tamaño del ImageView al tamaño del viewport para que ocupe todo el espacio
        caseImageScroll.viewportBoundsProperty().addListener((obs, oldB, newB) -> {
            if (newB == null) return;
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
        connectionBasisBox.getItems().addAll("Modalidad", "Modus operandi", "Patrón", "Criterio de Conexidad", "Otros");
        connectionBasisBox.setValue("Modalidad");
        connectionBasisBox.setMaxWidth(Double.MAX_VALUE);

        connectionDetailField = new TextField();
        connectionDetailField.getStyleClass().add("input-field");
        connectionDetailField.setPromptText("¿Cuál?");
        connectionDetailField.setMaxWidth(Double.MAX_VALUE);

        Label connectionDetailLabel = new Label("¿Cuál?");
        connectionDetailLabel.getStyleClass().add("muted-text");
        connectionDetailContainer = new VBox(6, connectionDetailLabel, connectionDetailField);
        connectionDetailContainer.setVisible(false);
        connectionDetailContainer.setManaged(false);

        connectionBasisBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            boolean requiresDetail = "Criterio de Conexidad".equals(newValue) || "Otros".equals(newValue);
            connectionDetailContainer.setVisible(requiresDetail);
            connectionDetailContainer.setManaged(requiresDetail);
            if (!requiresDetail) {
                connectionDetailField.clear();
            }
        });

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
                connectionDetailContainer,
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

        Label isolatedNodesTitle = new Label("Justificación de nodos aislados");
        isolatedNodesTitle.getStyleClass().add("section-title");

        Label isolatedNodesSubtitle = new Label("Antes de cerrar la investigación, explica por qué cada nodo quedó sin grupo.");
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

        moduleHost.getChildren().addAll(boardModule, connectionDialogOverlay, isolatedNodesOverlay, pdfLoadingOverlay);

        VBox content = new VBox(14, topBar, instructionLabel, moduleHost);
        view.setCenter(content);

        playTopBarEntrance();

        loadCasos();
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
        int index = 0;

        for (Caso caso : casos) {
            CaseNode node = new CaseNode(caso);
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
                statusLabel.setText("Seleccionado: " + node.getCaso().getNombre() + ". Elige otro nodo para asociarlo.");
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
            st.setToX(1.08);
            st.setToY(1.08);
            st.play();
        });

        node.setOnMouseExited(event -> {
            ScaleTransition st = new ScaleTransition(javafx.util.Duration.millis(120), node);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
    }

    private void showConnectionJustificationOverlay() {
        if (investigationFinished) {
            return;
        }
        if (selectedNode == null || pendingConnectionTarget == null) {
            return;
        }

        connectionDialogTitle.setText("Justificación: " + selectedNode.getCaso().getNombre() + " → " + pendingConnectionTarget.getCaso().getNombre());
        connectionBasisBox.setValue("Modalidad");
        connectionDetailField.clear();
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
        connectionDetailField.clear();
        connectionReasonField.clear();
        if (selectedNode != null) {
            selectedNode.setSelected(false);
        }
        if (pendingConnectionTarget != null) {
            pendingConnectionTarget.setSelected(false);
        }
        selectedNode = null;
        pendingConnectionTarget = null;
        statusLabel.setText("Selección cancelada.");
    }

    private void confirmConnectionFromOverlay() {
        if (investigationFinished) {
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
        boolean requiresDetail = "Criterio de Conexidad".equals(basis) || "Otros".equals(basis);
        String detail = connectionDetailField.getText().trim();
        if (requiresDetail && detail.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Indica ¿Cuál? para continuar.");
            return;
        }

        String connectionSummary = "Asociado por: " + basis;
        if (requiresDetail) {
            connectionSummary += " - " + detail;
        }
        connectionSummary += " | Justificación: " + trimmed;

        connections.add(new Connection(selectedNode, pendingConnectionTarget, connectionSummary));
        refreshConnections();
        refreshGroups();
        persistInvestigationSnapshot();
        statusLabel.setText("Conexión creada entre " + selectedNode.getCaso().getNombre() + " y " + pendingConnectionTarget.getCaso().getNombre() + ".");

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
                .map(connection -> connection.from.getCaso().getNombre() + " ↔ " + connection.to.getCaso().getNombre() + " | " + connection.reason)
                .collect(Collectors.toList()));

        connectionLayer.getChildren().setAll(connections.stream()
                .map(connection -> connection.line)
                .collect(Collectors.toList()));
        updateUngroupedNodesLabel();
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

        if (selectedGroupSignature != null && activeSignatures.stream().noneMatch(signature -> signature.equals(selectedGroupSignature))) {
            clearGroupSelection();
        }

        renderGroupCards();
        updateGroupOverlays();
        updateUngroupedNodesLabel();
    }

    private void updateUngroupedNodesLabel() {
        long ungroupedCount = nodes.stream()
                .filter(node -> !isGroupedNode(node))
                .count();
        ungroupedNodesLabel.setText("Nodos sin grupo: " + ungroupedCount);
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
        Label title = new Label(caso.getNombre());
        title.getStyleClass().add("section-title");

        Label meta = new Label(caso.getLugar() + " · " + caso.getFechaHechosFormateada());
        meta.getStyleClass().add("app-subtitle");

        Label summary = new Label(caso.getDescripcion());
        summary.getStyleClass().add("muted-text");
        summary.setWrapText(true);

        Button detailButton = new Button("Ver detalles");
        detailButton.getStyleClass().add("secondary-button");
        detailButton.setOnAction(e -> showCaseDetail(caso));

        VBox card = new VBox(10, title, meta, summary, detailButton);
        card.getStyleClass().add("panel-card");
        card.setPadding(new Insets(14));
        card.setOnMouseClicked(e -> showCaseDetail(caso));
        return card;
    }

    private String buildCaseDetails(Caso caso) {
        return "📋 " + caso.getNombre() + "\n\n"
                + "📍 Lugar: " + caso.getLugar() + "\n"
                + "📅 Fecha: " + caso.getFechaHechosFormateada() + "\n\n"
                + "📝 Descripción:\n" + caso.getDescripcion() + "\n\n"
                + "👥 Víctimas:\n" + String.join("\n", caso.getVictimas()) + "\n\n"
                + "⚖️ Victimarios:\n" + String.join("\n", caso.getVictimarios()) + "\n\n"
                + "⚔️ Delitos:\n" + String.join("\n", caso.getDelitos()) + "\n\n"
                + "🏛️ Actores Involucrados:\n" + String.join("\n", caso.getActoresInvolucrados()) + "\n";
    }

    private void showCaseDetail(Caso caso) {
        if (caso == null) return;
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
        if (rawPath == null || rawPath.isBlank()) return;

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
            moduleHost.getChildren().setAll(boardModule, connectionDialogOverlay, isolatedNodesOverlay, pdfLoadingOverlay);
        }
        setActiveTab(analyticalTab, casesTab);
        instructionLabel.setText("Conecta esferas para crear hipótesis investigativas. Cada componente conectado se convierte en un grupo.");
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
            moduleHost.getChildren().setAll(casesModule, connectionDialogOverlay, isolatedNodesOverlay, pdfLoadingOverlay);
        }
        setActiveTab(casesTab, analyticalTab);
        instructionLabel.setText("Revisa las tarjetas de cada caso y usa sus detalles para construir asociaciones más precisas.");
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

        String lowerQuery = normalized.toLowerCase();
        String suggestion = caseSearchSuggestions.getSelectionModel().getSelectedItem();
        if (suggestion == null || "Sin coincidencias".equalsIgnoreCase(suggestion) || !suggestion.toLowerCase().contains(lowerQuery)) {
            suggestion = caseSearchSuggestions.getItems().stream().findFirst().orElse(null);
        }
        final String resolvedSuggestion = suggestion;

        CaseNode target = nodes.stream()
            .filter(node -> buildSearchIndex(node.getCaso()).contains(lowerQuery) || node.getCaso().getNombre().equalsIgnoreCase(normalized))
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
        double viewportWidth = board.getWidth() > 0 ? board.getWidth() : board.getPrefWidth();
        double viewportHeight = board.getHeight() > 0 ? board.getHeight() : board.getPrefHeight();
        double targetX = (viewportWidth / 2.0) - (target.centerX() * boardZoom);
        double targetY = (viewportHeight / 2.0) - (target.centerY() * boardZoom);

        TranslateTransition transition = new TranslateTransition(javafx.util.Duration.millis(280), contentContainer);
        transition.setToX(targetX);
        transition.setToY(targetY);
        transition.play();
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

        TextArea reasonField = new TextArea(meta.reason);
        reasonField.getStyleClass().add("text-area");
        reasonField.setPromptText("Justificación general del grupo...");
        reasonField.setPrefRowCount(3);

        Button finalizeButton = new Button("Finalizar grupo");
        finalizeButton.getStyleClass().add("primary-button");
        finalizeButton.setOnAction(e -> saveGroupCard(cluster.signature, nameField, reasonField, meta, colorButton));

        Label countLabel = new Label(cluster.members.size() + " casos conectados");
        countLabel.getStyleClass().add("app-subtitle");

        VBox card = new VBox(8);
        card.getStyleClass().add("group-card");
        card.setPadding(new Insets(12));

        HBox headerRow = new HBox(10, colorButton, nameField);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(
                headerRow,
                reasonField,
                finalizeButton,
                countLabel
        );

        if (investigationFinished) {
            nameField.setDisable(true);
            reasonField.setDisable(true);
            colorButton.setDisable(true);
            finalizeButton.setDisable(true);
        }
        return card;
    }

    private void saveGroupCard(String signature, TextField nameField, TextArea reasonField, GroupMeta meta, Button colorButton) {
        if (investigationFinished) {
            return;
        }
        String reason = reasonField.getText().trim();
        String groupName = nameField.getText().trim();
        if (groupName.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "El grupo debe tener nombre.");
            return;
        }
        if (reason.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "El grupo debe tener justificación.");
            return;
        }

        meta.name = groupName;
        meta.reason = reason;
        metadataBySignature.put(signature, meta);
        colorButton.setStyle(buildSwatchStyle(meta.color));
        persistInvestigationSnapshot();
        statusLabel.setText("Grupo actualizado: " + meta.name + ".");
        renderGroupCards();
        updateGroupOverlays();
    }

    private void updateInvestigationTimer() {
        if (investigationFinished) {
            return;
        }

        Duration elapsed = Duration.ofMillis(System.currentTimeMillis() - investigationStartedAtMillis);
        Duration remaining = INVESTIGATION_DURATION.minus(elapsed);

        if (remaining.isNegative() || remaining.isZero()) {
            timerLabel.setText("00:00:00");
            finalizeInvestigation("tiempo agotado");
            return;
        }

        timerLabel.setText(formatDuration(remaining));
        if (remaining.toMinutes() <= 10) {
            if (!timerLabel.getStyleClass().contains("timer-pill-warning")) {
                timerLabel.getStyleClass().add("timer-pill-warning");
            }
        } else {
            timerLabel.getStyleClass().remove("timer-pill-warning");
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

        TextInputDialog investigatorDialog = new TextInputDialog();
        investigatorDialog.setTitle("Cerrar investigación");
        investigatorDialog.setHeaderText("Ingresa el nombre de quien realizó la investigación");
        investigatorDialog.setContentText("Nombre:");
        String investigatorName = investigatorDialog.showAndWait().map(String::trim).orElse("");
        if (investigatorName.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Debes ingresar un nombre para generar el PDF.");
            return;
        }

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
        });

        exportTask.setOnFailed(event -> {
            Throwable failure = exportTask.getException();
            statusLabel.setText("Investigación finalizada, pero hubo error al generar el PDF.");
            hidePdfLoadingOverlay();
            showAlert(Alert.AlertType.ERROR,
                    "Investigación finalizada por " + reason + ", pero no se pudo generar el PDF.\n"
                    + (failure == null ? "Error desconocido" : failure.getMessage()));
            investigationFinished = true;
            finishingInProgress = false;
        });

        Thread worker = new Thread(exportTask, "prisma-pdf-export");
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
            Label emptyLabel = new Label("No hay nodos aislados pendientes.");
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
            json.append("      \"detail\": \"").append(escapeJson(connection.reason)).append("\"\n");
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
            json.append("      \"reason\": \"").append(escapeJson(normalizeJustification(isolatedNodeJustifications.get(node.getCaso().getNombre())))).append("\"\n");
            json.append("    }");
            if (i < isolatedNodes.size() - 1) {
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

    private Path exportInvestigationPdf(String endReason, String investigatorName) throws IOException {
        Path downloads = Paths.get(System.getProperty("user.home"), "Downloads");
        Files.createDirectories(downloads);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path output = downloads.resolve("prisma-investigacion-" + timestamp + ".pdf");

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            float margin = 50;
            float y = page.getMediaBox().getHeight() - margin;

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                y = writePdfLine(content, page, y, 16, "PRISMA DAE - Reporte de Investigación", true);
                y = writePdfLine(content, page, y, 11, "Investigador: " + investigatorName, false);
                y = writePdfLine(content, page, y, 11, "Fecha de cierre: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), false);
                y = writePdfLine(content, page, y, 11, "Motivo de cierre: " + endReason, false);
                y = writePdfLine(content, page, y, 11, "Duración máxima configurada: " + formatDuration(INVESTIGATION_DURATION), false);
                y = writePdfLine(content, page, y, 11, "", false);

                y = writePdfLine(content, page, y, 13, "Conexiones registradas", true);
                if (connections.isEmpty()) {
                    y = writePdfWrappedLine(content, page, y, 11, "- No hay conexiones registradas.");
                } else {
                    int idx = 1;
                    for (Connection connection : connections) {
                        String line = idx + ". "
                                + connection.from.getCaso().getNombre()
                                + " <-> "
                                + connection.to.getCaso().getNombre()
                                + " | "
                                + connection.reason;
                        y = writePdfWrappedLine(content, page, y, 11, line);
                        idx++;
                    }
                }

                y = writePdfLine(content, page, y, 11, "", false);
                y = writePdfLine(content, page, y, 13, "Grupos y justificaciones", true);
                if (currentClusters.isEmpty()) {
                    y = writePdfWrappedLine(content, page, y, 11, "- No hay grupos detectados.");
                } else {
                    int idx = 1;
                    for (GroupCluster cluster : currentClusters) {
                        GroupMeta meta = cluster.meta;
                        y = writePdfWrappedLine(content, page, y, 11,
                                idx + ". " + meta.name + " (" + cluster.members.size() + " casos)");
                        y = writePdfWrappedLine(content, page, y, 11,
                                "   Justificación: " + (meta.reason == null ? "" : meta.reason));
                        y = writePdfWrappedLine(content, page, y, 11,
                                "   Casos: " + cluster.members.stream()
                                        .map(member -> member.getCaso().getNombre())
                                        .collect(Collectors.joining(", ")));
                        idx++;
                    }
                }

                y = writePdfLine(content, page, y, 11, "", false);
                y = writePdfLine(content, page, y, 13, "Casos aislados", true);
                List<CaseNode> isolatedNodes = nodes.stream()
                        .filter(node -> !isGroupedNode(node))
                        .collect(Collectors.toList());
                if (isolatedNodes.isEmpty()) {
                    y = writePdfWrappedLine(content, page, y, 11, "- No hay casos aislados.");
                } else {
                    int idx = 1;
                    for (CaseNode node : isolatedNodes) {
                    String reason = normalizeJustification(isolatedNodeJustifications.get(node.getCaso().getNombre()));
                        y = writePdfWrappedLine(content, page, y, 11,
                                idx + ". " + node.getCaso().getNombre() + " - Caso aislado");
                        y = writePdfWrappedLine(content, page, y, 11,
                                "   Justificación: " + reason);
                        idx++;
                    }
                }
            }

            document.save(output.toFile());
        }

        return output;
    }

    private float writePdfLine(PDPageContentStream content, PDPage page, float y, int fontSize, String text, boolean bold)
            throws IOException {
        float left = 52;
        if (y < 60) {
            return y;
        }
        content.beginText();
        content.setFont(bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, fontSize);
        content.newLineAtOffset(left, y);
        content.showText(text == null ? "" : text);
        content.endText();
        return y - (fontSize + 6);
    }

    private float writePdfWrappedLine(PDPageContentStream content, PDPage page, float y, int fontSize, String text)
            throws IOException {
        String value = text == null ? "" : text;
        int maxChars = 100;
        int cursor = 0;
        while (cursor < value.length()) {
            int end = Math.min(value.length(), cursor + maxChars);
            String chunk = value.substring(cursor, end);
            y = writePdfLine(content, page, y, fontSize, chunk, false);
            cursor = end;
        }
        if (value.isEmpty()) {
            y = writePdfLine(content, page, y, fontSize, "", false);
        }
        return y;
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
        for (GroupCluster cluster : currentClusters) {
            int size = cluster.members.size();
            if (size < 2) {
                continue;
            }

            double centroidX = 0.0;
            double centroidY = 0.0;
            for (CaseNode member : cluster.members) {
                centroidX += member.centerX();
                centroidY += member.centerY();
            }
            centroidX /= size;
            centroidY /= size;

            double radius = Math.max(68.0, 28.0 + size * 10.0);
            for (int i = 0; i < size; i++) {
                CaseNode member = cluster.members.get(i);
                double angle = (Math.PI * 2.0 * i) / size;
                double centerX = centroidX + Math.cos(angle) * radius;
                double centerY = centroidY + Math.sin(angle) * radius;
                double x = clamp(centerX - NODE_RADIUS, 0, safeWidth() - NODE_DIAMETER);
                double y = clamp(centerY - NODE_RADIUS, 0, safeHeight() - NODE_DIAMETER);
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
        return "-fx-background-color: " + toRgb(color) + "; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: rgba(103,232,249,0.40); -fx-border-width: 1;";
    }

    private String toRgb(Color color) {
        int red = (int) Math.round(color.getRed() * 255);
        int green = (int) Math.round(color.getGreen() * 255);
        int blue = (int) Math.round(color.getBlue() * 255);
        return "rgb(" + red + "," + green + "," + blue + ")";
    }

    private List<GroupCluster> detectClusters() {
        List<GroupCluster> clusters = new ArrayList<>();
        Set<CaseNode> visited = new HashSet<>();
        int sequence = 1;

        for (CaseNode node : nodes) {
            if (visited.contains(node)) {
                continue;
            }

            Set<CaseNode> component = new HashSet<>();
            collectGroup(node, component);
            visited.addAll(component);

            if (component.size() < 2) {
                continue;
            }

            List<CaseNode> members = component.stream()
                    .sorted(Comparator.comparing(caseNode -> caseNode.getCaso().getNombre()))
                    .collect(Collectors.toList());
            String signature = createSignature(members);
            String defaultGroupName = "Grupo " + sequence;
            GroupMeta meta = metadataBySignature.computeIfAbsent(signature, key -> new GroupMeta(
                    defaultGroupName,
                    defaultGroupColor(signature),
                    "Asociado por modalidad",
                    "Sin justificación registrada"
            ));

            clusters.add(new GroupCluster(signature, members, meta));
            sequence++;
        }

        return clusters;
    }

    private void collectGroup(CaseNode node, Set<CaseNode> group) {
        if (!group.add(node)) {
            return;
        }

        for (Connection connection : connections) {
            if (connection.from == node) {
                collectGroup(connection.to, group);
            } else if (connection.to == node) {
                collectGroup(connection.from, group);
            }
        }
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
            overlay.rectangle.setFill(Color.color(meta.color.getRed(), meta.color.getGreen(), meta.color.getBlue(), 0.08));
            overlay.rectangle.setStroke(Color.color(meta.color.getRed(), meta.color.getGreen(), meta.color.getBlue(), 0.92));
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
        alert.setTitle("PRISMA DAE");
        alert.showAndWait();
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
        private final Line line;

        private Connection(CaseNode from, CaseNode to, String reason) {
            this.from = from;
            this.to = to;
            this.reason = reason;
            this.line = new Line();
            this.line.setStroke(Color.web("#67e8f9", 0.78));
            this.line.setStrokeWidth(2.2);
            this.line.setMouseTransparent(true);
        }
    }

    private static final class GroupMeta {

        private String name;
        private Color color;
        private String mode;
        private String reason;

        private GroupMeta(String name, Color color, String mode, String reason) {
            this.name = name;
            this.color = color;
            this.mode = mode;
            this.reason = reason;
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

    private static final class GroupBounds {

        private final double minX;
        private final double minY;
        private final double width;
        private final double height;

        private GroupBounds(double minX, double minY, double width, double height) {
            this.minX = minX;
            this.minY = minY;
            this.width = width;
            this.height = height;
        }
    }

    private static final class GroupCluster {

        private final String signature;
        private final List<CaseNode> members;
        private final GroupMeta meta;

        private GroupCluster(String signature, List<CaseNode> members, GroupMeta meta) {
            this.signature = signature;
            this.members = members;
            this.meta = meta;
        }

        @Override
        public String toString() {
            return meta.name + " · " + members.size() + " casos";
        }
    }

    private static final class CaseNode extends StackPane {

        private final Caso caso;
        private double vx;
        private double vy;
        private boolean dragging;
        private double dragOffsetX;
        private double dragOffsetY;

        private CaseNode(Caso caso) {
            this.caso = caso;
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

            VBox content = new VBox(2, sphere, title);
            content.setAlignment(Pos.CENTER);
            getChildren().add(content);
        }

        private void setSelected(boolean selected) {
            if (selected) {
                if (!getStyleClass().contains("selected")) {
                    getStyleClass().add("selected");
                }
            } else {
                getStyleClass().remove("selected");
            }
        }

        private Caso getCaso() {
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
