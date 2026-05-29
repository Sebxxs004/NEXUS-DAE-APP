package com.prisma.views;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import com.prisma.data.CasoRepository;
import com.prisma.models.Caso;
import com.prisma.ui.Theme;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class CasesManagementBrownView {
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
    private ScrollPane modalImageScroll;
    private double modalZoom = 1.0;
    private double modalBaseFitWidth = 900.0;
    private double modalBaseFitHeight = 620.0;
    private double dragAnchorX;
    private double dragAnchorY;
    private double dragStartHValue;
    private double dragStartVValue;

    public CasesManagementBrownView(Stage stage) {
        Image backgroundImage = new Image(getClass().getResourceAsStream("/styles/assets/gestion-casos.jpeg"));
        ImageView backgroundView = new ImageView(backgroundImage);
        backgroundView.setPreserveRatio(false);
        backgroundView.fitWidthProperty().bind(stage.widthProperty());
        backgroundView.fitHeightProperty().bind(stage.heightProperty());
        backgroundView.setOpacity(0.28);

        StackPane outerShell = new StackPane(backgroundView);

        BorderPane contentShell = new BorderPane();
        contentShell.setStyle("-fx-background-color: transparent;");
        contentShell.setPadding(new Insets(16));

        Label title = new Label("Gestión de Casos");
        title.getStyleClass().add("app-title");

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

        timerLabel = new Label("TIEMPO " + InvestigationClock.formatRemaining());
        timerLabel.setStyle(timerStyle(false));

        HBox topRow = new HBox(18, title, timerLabel, backButton);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPadding(new Insets(14, 18, 14, 18));
        topRow.setStyle(
            "-fx-background-color: rgba(33, 19, 12, 0.78); " +
            "-fx-background-radius: 18; " +
            "-fx-border-color: rgba(245, 158, 11, 0.24); " +
            "-fx-border-radius: 18;"
        );
        HBox.setHgrow(title, Priority.ALWAYS);

        Label subtitle = new Label("Selecciona un caso para ver su imagen en un modal con zoom.");
        subtitle.getStyleClass().add("app-subtitle");
        subtitle.setWrapText(true);

        searchField = new TextField();
        searchField.setPromptText("Buscar por nombre, lugar o descripción");
        searchField.setPrefHeight(42);
        searchField.setMaxWidth(Double.MAX_VALUE);
        searchField.setStyle(
            "-fx-background-color: rgba(33, 19, 12, 0.92); " +
            "-fx-text-fill: #fff7ed; " +
            "-fx-prompt-text-fill: #c8a17b; " +
            "-fx-background-radius: 14; " +
            "-fx-border-color: rgba(245, 158, 11, 0.22); " +
            "-fx-border-radius: 14; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 10 14 10 14; " +
            "-fx-font-size: 14; " +
            "-fx-font-family: 'Segoe UI';"
        );
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshGrid(stage));

        casesGrid = new TilePane();
        casesGrid.getStyleClass().add("cases-grid");
        casesGrid.setHgap(16);
        casesGrid.setVgap(16);
        casesGrid.setTileAlignment(Pos.TOP_LEFT);
        casesGrid.setPrefColumns(3);
        casesGrid.setPrefTileWidth(290);
        casesGrid.setPrefTileHeight(210);

        ScrollPane gridScroll = new ScrollPane(casesGrid);
        gridScroll.getStyleClass().add("group-scroll");
        gridScroll.setFitToWidth(true);
        gridScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        gridScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(gridScroll, Priority.ALWAYS);

        VBox centerContent = new VBox(12, topRow, subtitle, searchField, gridScroll);
        centerContent.setPadding(new Insets(0, 0, 14, 0));
        VBox.setVgrow(gridScroll, Priority.ALWAYS);

        contentShell.setCenter(centerContent);

        modalOverlay = buildModalOverlay(stage);
        modalOverlay.setVisible(false);
        modalOverlay.setManaged(false);

        outerShell.getChildren().addAll(contentShell, modalOverlay);
        view = new StackPane(outerShell);
        view.setStyle("-fx-background-color: transparent;");

        refreshGrid(stage);
        CasoRepository.getCasos().addListener((ListChangeListener<Caso>) change -> refreshGrid(stage));

        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> refreshTimer()));
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
        timerTimeline.play();
    }

    public void applyTheme(Scene scene) {
        Theme.apply(scene);
        if (!scene.getStylesheets().contains(boardStylesheet())) {
            scene.getStylesheets().add(boardStylesheet());
        }
    }

    public Parent getView() {
        return view;
    }

    private void refreshGrid(Stage stage) {
        String query = searchField == null ? "" : searchField.getText();
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();

        casesGrid.getChildren().setAll(CasoRepository.getCasos().stream()
                .sorted(Comparator.comparing(Caso::getNombre, String.CASE_INSENSITIVE_ORDER))
            .filter(caso -> normalizedQuery.isEmpty()
                || containsIgnoreCase(caso.getNombre(), normalizedQuery)
                || containsIgnoreCase(caso.getLugar(), normalizedQuery)
                || containsIgnoreCase(caso.getDescripcion(), normalizedQuery))
                .map(caso -> buildCaseCard(stage, caso))
                .toList());
    }

    private VBox buildCaseCard(Stage stage, Caso caso) {
        Label nameLabel = new Label(caso.getNombre());
        nameLabel.getStyleClass().add("section-title");

        Button copyNameButton = new Button("Copiar");
        copyNameButton.getStyleClass().add("secondary-button");
        copyNameButton.setOnAction(e -> copyCaseName(caso));

        HBox titleRow = new HBox(10, nameLabel, copyNameButton);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label metaLabel = new Label(caso.getLugar() + " · " + caso.getFechaHechosFormateada());
        metaLabel.getStyleClass().add("app-subtitle");

        Label summaryLabel = new Label(caso.getDescripcion());
        summaryLabel.getStyleClass().add("muted-text");
        summaryLabel.setWrapText(true);

        Button detailsButton = new Button("Ver detalles");
        detailsButton.getStyleClass().add("secondary-button");
        detailsButton.setOnAction(e -> openCaseModal(stage, caso));

        VBox card = new VBox(10, titleRow, metaLabel, summaryLabel, detailsButton);
        card.getStyleClass().add("case-card-brown");
        card.setPadding(new Insets(16));
        card.setPrefWidth(290);
        return card;
    }

    private StackPane buildModalOverlay(Stage stage) {
        Image modalBackgroundImage = new Image(getClass().getResourceAsStream("/styles/assets/fondo-case.jpeg"));
        ImageView modalBackgroundView = new ImageView(modalBackgroundImage);
        modalBackgroundView.setPreserveRatio(false);
        modalBackgroundView.fitWidthProperty().bind(stage.widthProperty());
        modalBackgroundView.fitHeightProperty().bind(stage.heightProperty());
        modalBackgroundView.setOpacity(0.72);
        modalBackgroundView.setMouseTransparent(true);

        StackPane backdrop = new StackPane(modalBackgroundView);
        backdrop.setStyle("-fx-background-color: rgba(10, 6, 4, 0.28);");
        backdrop.setOnMouseClicked(e -> {
            if (e.getTarget() == backdrop) {
                hideModal();
            }
        });

        modalTitleLabel = new Label();
        modalTitleLabel.getStyleClass().add("app-title");

        modalHintLabel = new Label("Ctrl + rueda para zoom. Arrastra para mover la imagen.");
        modalHintLabel.getStyleClass().add("app-subtitle");

        modalLocateButton = new Button("Ubicar caso");
        modalLocateButton.getStyleClass().add("secondary-button");
        modalLocateButton.setOnAction(e -> openAnalyticalBoardForCurrentCase(stage));

        modalImageView = new ImageView();
        modalImageView.setPreserveRatio(true);
        modalImageView.setSmooth(true);
        modalImageView.setCache(true);

        StackPane imageHolder = new StackPane(modalImageView);
        imageHolder.getStyleClass().add("case-modal-viewer");
        imageHolder.setMinSize(720, 440);

        modalImageScroll = new ScrollPane(imageHolder);
        modalImageScroll.getStyleClass().add("case-modal-scroll");
        modalImageScroll.setPannable(false);
        modalImageScroll.setFitToWidth(false);
        modalImageScroll.setFitToHeight(false);
        modalImageScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        modalImageScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        modalImageScroll.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isControlDown()) {
                double factor = event.getDeltaY() > 0 ? 1.12 : 0.9;
                modalZoom = clamp(modalZoom * factor, 0.35, 4.5);
                applyZoomToModalImage();
                event.consume();
            }
        });

        modalImageScroll.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            dragAnchorX = event.getX();
            dragAnchorY = event.getY();
            dragStartHValue = modalImageScroll.getHvalue();
            dragStartVValue = modalImageScroll.getVvalue();
        });

        modalImageScroll.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            double hRange = Math.max(0.0001, modalImageScroll.getHmax() - modalImageScroll.getHmin());
            double vRange = Math.max(0.0001, modalImageScroll.getVmax() - modalImageScroll.getVmin());
            double hDelta = (dragAnchorX - event.getX()) / Math.max(1, modalImageScroll.getViewportBounds().getWidth());
            double vDelta = (dragAnchorY - event.getY()) / Math.max(1, modalImageScroll.getViewportBounds().getHeight());
            modalImageScroll.setHvalue(clamp(dragStartHValue + (hDelta / hRange), modalImageScroll.getHmin(), modalImageScroll.getHmax()));
            modalImageScroll.setVvalue(clamp(dragStartVValue + (vDelta / vRange), modalImageScroll.getVmin(), modalImageScroll.getVmax()));
            event.consume();
        });

        Button zoomOut = new Button("-");
        zoomOut.getStyleClass().add("secondary-button");
        zoomOut.setOnAction(e -> applyModalZoom(0.88));

        Button zoomReset = new Button("100%");
        zoomReset.getStyleClass().add("secondary-button");
        zoomReset.setOnAction(e -> applyModalZoom(1.0));

        Button zoomIn = new Button("+");
        zoomIn.getStyleClass().add("secondary-button");
        zoomIn.setOnAction(e -> applyModalZoom(1.12));

        Button close = new Button("Cerrar");
        close.getStyleClass().add("danger-button");
        close.setOnAction(e -> hideModal());

        HBox toolbar = new HBox(10, zoomOut, zoomReset, zoomIn, close);
        toolbar.setAlignment(Pos.CENTER_RIGHT);

        VBox modalCard = new VBox(12, modalTitleLabel, modalHintLabel, modalImageScroll, modalLocateButton, toolbar);
        modalCard.getStyleClass().add("case-modal-card");
        modalCard.setPadding(new Insets(18));
        modalCard.setMaxWidth(980);
        modalCard.setMaxHeight(760);
        modalCard.setStyle(modalCard.getStyle() + " -fx-background-color: rgba(24, 15, 10, 0.72);");

        StackPane modalRoot = new StackPane(backdrop, modalCard);
        StackPane.setAlignment(modalCard, Pos.CENTER);
        return modalRoot;
    }

    private void openCaseModal(Stage stage, Caso caso) {
        modalCurrentCase = caso;
        modalTitleLabel.setText(caso.getNombre());
        modalHintLabel.setText(caso.getLugar() + " · " + caso.getFechaHechosFormateada());

        Image image = loadCaseImage(caso);
        modalImageView.setImage(image);
        modalBaseFitWidth = Math.min(900, stage.getWidth() * 0.72);
        modalBaseFitHeight = Math.min(620, stage.getHeight() * 0.62);
        modalZoom = 1.0;
        applyZoomToModalImage();
        if (modalImageScroll != null) {
            modalImageScroll.setHvalue(0);
            modalImageScroll.setVvalue(0);
        }

        modalOverlay.setVisible(true);
        modalOverlay.setManaged(true);
        modalOverlay.toFront();
    }

    private void hideModal() {
        modalOverlay.setVisible(false);
        modalOverlay.setManaged(false);
        modalImageView.setImage(null);
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
        PlayerViewBrown playerViewBrown = new PlayerViewBrown(stage);
        Scene scene = new Scene(playerViewBrown.getView(), 1500, 900);
        playerViewBrown.applyTheme(scene);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setFullScreen(true);
        playerViewBrown.focusCase(modalCurrentCase.getNombre());
    }

    private Image loadCaseImage(Caso caso) {
        if (caso != null && caso.tieneImagen()) {
            Path path = Path.of(caso.getImagenPath());
            if (Files.exists(path)) {
                return new Image(path.toUri().toString());
            }
        }
        return new Image(getClass().getResourceAsStream("/styles/assets/fondo-case.jpeg"));
    }

    private void refreshTimer() {
        timerLabel.setText("TIEMPO " + InvestigationClock.formatRemaining());
        if (InvestigationClock.isCritical()) {
            timerLabel.setStyle(timerStyle(true));
        } else {
            timerLabel.setStyle(timerStyle(false));
        }
    }

    private String timerStyle(boolean critical) {
        if (critical) {
            return "-fx-font-size: 17; " +
                   "-fx-font-weight: bold; " +
                   "-fx-text-fill: #fff7ed; " +
                   "-fx-background-color: linear-gradient(to right, rgba(153, 27, 27, 0.98), rgba(220, 38, 38, 0.92)); " +
                   "-fx-background-radius: 999; " +
                   "-fx-border-color: rgba(254, 226, 226, 0.98); " +
                   "-fx-border-radius: 999; " +
                   "-fx-border-width: 2; " +
                   "-fx-padding: 9 16 9 16; " +
                   "-fx-font-family: 'Segoe UI'; " +
                   "-fx-effect: dropshadow(gaussian, rgba(248, 113, 113, 0.95), 28, 0.32, 0, 0);";
        }
        return "-fx-font-size: 17; " +
               "-fx-font-weight: bold; " +
               "-fx-text-fill: #fff7ed; " +
               "-fx-background-color: rgba(153, 27, 27, 0.94); " +
               "-fx-background-radius: 999; " +
               "-fx-border-color: rgba(254, 202, 202, 0.92); " +
               "-fx-border-radius: 999; " +
               "-fx-border-width: 2; " +
               "-fx-padding: 9 16 9 16; " +
               "-fx-font-family: 'Segoe UI'; " +
               "-fx-effect: dropshadow(gaussian, rgba(248, 113, 113, 0.8), 24, 0.28, 0, 0);";
    }

    private void applyModalZoom(double factor) {
        modalZoom = clamp(modalZoom * factor, 0.35, 4.5);
        applyZoomToModalImage();
    }

    private void applyZoomToModalImage() {
        modalImageView.setFitWidth(modalBaseFitWidth * modalZoom);
        modalImageView.setFitHeight(modalBaseFitHeight * modalZoom);
        modalImageView.setScaleX(1.0);
        modalImageView.setScaleY(1.0);
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
}