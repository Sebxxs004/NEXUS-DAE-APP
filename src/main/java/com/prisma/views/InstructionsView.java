package com.prisma.views;

import java.util.ArrayList;
import java.util.List;

import com.prisma.ui.Theme;

import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

public class InstructionsView {
    private final Stage stage;
    private final StackPane view;

    private final List<HBox> navItems = new ArrayList<>();
    private final List<StackPane> navCircles = new ArrayList<>();
    private final List<Label> navNumbers = new ArrayList<>();
    private final List<Label> navTexts = new ArrayList<>();
    private final List<Rectangle> progressSegs = new ArrayList<>();

    private Label progressBadge;
    private ScrollPane contentScroll;
    private VBox contentVBox;

    private VBox sec1;
    private VBox sec2;
    private VBox sec3;
    private VBox sec4;

    private int currentSection = 1;

    public InstructionsView(Stage stage) {
        this.stage = stage;
        this.view = new StackPane();
        this.view.setPrefSize(1500, 900);
        build();
    }

    private void build() {
        Image backgroundImage = new Image(getClass().getResourceAsStream("/styles/assets/fondo-login.png"));
        ImageView backgroundView = new ImageView(backgroundImage);
        backgroundView.setPreserveRatio(false);
        backgroundView.fitWidthProperty().bind(view.widthProperty());
        backgroundView.fitHeightProperty().bind(view.heightProperty());

        Rectangle overlay = new Rectangle();
        overlay.widthProperty().bind(view.widthProperty());
        overlay.heightProperty().bind(view.heightProperty());
        overlay.setFill(Color.rgb(4, 8, 20, 0.96));

        BorderPane shell = new BorderPane();
        shell.setPadding(Insets.EMPTY);
        shell.setStyle("-fx-background-color: transparent;");

        shell.setTop(buildTopBar());
        shell.setLeft(buildSideNav());
        shell.setCenter(buildCenterContent());

        sec1 = buildSection1();
        sec2 = buildSection2();
        sec3 = buildSection3();
        sec4 = buildSection4();

        goToSection(1);

        view.getChildren().addAll(backgroundView, overlay, shell);
    }

    private HBox buildTopBar() {
        HBox topBar = new HBox(16);
        topBar.setMinHeight(56);
        topBar.setPrefHeight(56);
        topBar.setMaxHeight(56);
        topBar.setPadding(new Insets(0, 28, 0, 28));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: #0c1427; -fx-border-color: rgba(255,165,0,0.18); -fx-border-width: 0 0 1 0;");

        HBox backButton = new HBox();
        backButton.setAlignment(Pos.CENTER_LEFT);
        backButton.setPadding(new Insets(7, 14, 7, 14));
        backButton.setCursor(javafx.scene.Cursor.HAND);
        backButton.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-border-color: rgba(255,255,255,0.15); -fx-border-width: 1; -fx-background-radius: 8; -fx-border-radius: 8;");

        Label backLabel = new Label("← Volver");
        backLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
        backButton.getChildren().add(backLabel);
        backButton.setOnMouseEntered(e -> {
            backButton.setStyle("-fx-background-color: rgba(255,255,255,0.11); -fx-border-color: rgba(255,255,255,0.15); -fx-border-width: 1; -fx-background-radius: 8; -fx-border-radius: 8;");
            backLabel.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 13; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
        });
        backButton.setOnMouseExited(e -> {
            backButton.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-border-color: rgba(255,255,255,0.15); -fx-border-width: 1; -fx-background-radius: 8; -fx-border-radius: 8;");
            backLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
        });
        backButton.setOnMouseClicked(e -> goBackToLogin());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label title = new Label("Instrucciones del Simulador");
        title.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 15; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        progressBadge = new Label("Sección 1 / 4");
        progressBadge.setId("progressBadge");
        progressBadge.setStyle("-fx-background-color: rgba(255,165,0,0.10); -fx-border-color: rgba(255,165,0,0.25); -fx-border-width: 1; -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 5 14 5 14; -fx-text-fill: #e09d10; -fx-font-size: 12; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");

        topBar.getChildren().addAll(backButton, spacer, title, spacer2, progressBadge);
        return topBar;
    }

    private VBox buildSideNav() {
        VBox sideNav = new VBox(6);
        sideNav.setPrefWidth(230);
        sideNav.setMinWidth(230);
        sideNav.setMaxWidth(230);
        sideNav.setPadding(new Insets(24, 14, 24, 14));
        sideNav.setStyle("-fx-background-color: #0c1427; -fx-border-color: rgba(255,255,255,0.06); -fx-border-width: 0 1 0 0;");

        sideNav.getChildren().addAll(
                makeNavItem(1, "Bienvenida"),
                makeNavItem(2, "¿Qué es NEXUS?"),
                makeNavItem(3, "Cada decisión importa"),
                makeNavItem(4, "Tu objetivo")
        );

        VBox sep = new VBox();
        sep.setMinHeight(1);
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: rgba(255,255,255,0.08);");
        VBox.setMargin(sep, new Insets(16, 0, 12, 0));

        Label progressLabel = new Label("PROGRESO");
        progressLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 11; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");

        HBox progressRow = new HBox(4);
        progressRow.setPadding(new Insets(0, 4, 0, 4));
        for (int i = 0; i < 4; i++) {
            StackPane slot = new StackPane();
            slot.setMinHeight(3);
            slot.setPrefHeight(3);
            slot.setMaxHeight(3);
            HBox.setHgrow(slot, Priority.ALWAYS);

            Rectangle seg = new Rectangle();
            seg.heightProperty().set(3);
            seg.widthProperty().bind(slot.widthProperty());
            seg.setArcWidth(2);
            seg.setArcHeight(2);
            seg.setFill(Color.rgb(255, 255, 255, 0.10));

            progressSegs.add(seg);
            slot.getChildren().add(seg);
            progressRow.getChildren().add(slot);
        }

        sideNav.getChildren().addAll(sep, progressLabel, progressRow);
        return sideNav;
    }

    private HBox makeNavItem(int n, String label) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10, 12, 10, 12));
        item.setCursor(javafx.scene.Cursor.HAND);
        item.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-border-width: 1; -fx-background-radius: 8; -fx-border-radius: 8;");

        StackPane numberCircle = new StackPane();
        numberCircle.setPrefSize(22, 22);
        numberCircle.setMinSize(22, 22);
        numberCircle.setMaxSize(22, 22);
        numberCircle.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 11;");

        Label number = new Label(String.valueOf(n));
        number.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
        numberCircle.getChildren().add(number);

        Label text = new Label(label);
        text.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13; -fx-font-family: 'Segoe UI';");

        item.getChildren().addAll(numberCircle, text);
        item.setOnMouseClicked(e -> goToSection(n));

        navItems.add(item);
        navCircles.add(numberCircle);
        navNumbers.add(number);
        navTexts.add(text);
        return item;
    }

    private ScrollPane buildCenterContent() {
        contentVBox = new VBox(20);
        contentVBox.setPadding(new Insets(32, 40, 32, 40));
        contentVBox.setMaxWidth(860);

        StackPane contentWrapper = new StackPane(contentVBox);
        contentWrapper.setAlignment(Pos.TOP_CENTER);
        contentWrapper.setStyle("-fx-background-color: #070c1a;");

        contentScroll = new ScrollPane(contentWrapper);
        contentScroll.setFitToWidth(true);
        contentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        contentScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        contentScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return contentScroll;
    }

    private VBox buildSection1() {
        VBox section = new VBox(18);
        section.getChildren().add(sectionHeader("⚖️", "Bienvenido al Despacho", "Dispone de tres horas para revisar su despacho y tomar las primeras decisiones. Su despacho es mixto, conoce de diferentes delitos y temáticas."));
        section.getChildren().add(infoBox("Acaba de llegar a su oficina. El reloj institucional marca el inicio de su jornada y la acumulación de trabajo ya es evidente."));

        GridPane alertGrid = new GridPane();
        alertGrid.setHgap(12);
        alertGrid.setVgap(12);
        ColumnConstraints colA = new ColumnConstraints();
        colA.setPercentWidth(50);
        ColumnConstraints colB = new ColumnConstraints();
        colB.setPercentWidth(50);
        alertGrid.getColumnConstraints().addAll(colA, colB);
        alertGrid.add(alertCard("📂", "Noticias criminales", "Más de 50 noticias criminales pendientes de revisión."), 0, 0);
        alertGrid.add(alertCard("📧", "Correo institucional", "Solicitudes y comunicaciones esperando respuesta."), 1, 0);
        alertGrid.add(alertCard("👤", "Víctima en sala", "Una víctima está esperando ser atendida personalmente."), 0, 1);
        alertGrid.add(alertCard("🔍", "Reporte de investigador", "Un investigador reporta un posible caso de criminalidad organizada."), 1, 1);

        HBox contextBox = new HBox();
        contextBox.setPadding(new Insets(16));
        contextBox.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-border-color: rgba(255,255,255,0.07); -fx-border-width: 1; -fx-background-radius: 10; -fx-border-radius: 10;");
        Label context = new Label("Su equipo es limitado. No cuenta con asistente, pero tiene asignado un judicante que podrá apoyarlo. El tiempo también es limitado. Y cada decisión tendrá consecuencias.");
        context.setWrapText(true);
        context.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 14; -fx-font-family: 'Segoe UI'; -fx-line-spacing: 4;");
        contextBox.getChildren().add(context);

        section.getChildren().addAll(alertGrid, contextBox, ctaRow(
                "¿Listo para continuar?",
                "Conozca qué es NEXUS y cómo funciona.",
                "Siguiente →",
                () -> goToSection(2),
                "-fx-background-color: rgba(224,157,16,0.06); -fx-border-color: rgba(224,157,16,0.18); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12;"
        ));

        return section;
    }

    private VBox buildSection2() {
        VBox section = new VBox(18);
        section.getChildren().add(sectionHeader("🧩", "¿Qué es NEXUS?", "Una actividad de simulación interactiva diseñada para fiscales."));
        section.getChildren().add(infoBox("NEXUS es una actividad de simulación interactiva en la que usted asume el rol de Fiscal Delegado al frente de un despacho con una carga real de trabajo."));

        Label during = new Label("DURANTE EL JUEGO DEBERÁ:");
        during.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11; -fx-font-weight: bold; -fx-font-family: 'Segoe UI'; -fx-letter-spacing: 2px;");

        VBox features = new VBox(8,
                featureItem("Priorizar investigaciones."),
                featureItem("Analizar noticias criminales."),
                featureItem("Determinar qué asuntos son de su competencia."),
                featureItem("Impulsar actuaciones investigativas."),
                featureItem("Coordinar actividades con Policía Judicial."),
                featureItem("Atender víctimas y peticionarios."),
                featureItem("Gestionar términos y requerimientos."),
                featureItem("Tomar decisiones estratégicas", "bajo presión.")
        );

        section.getChildren().addAll(during, features, ctaRow(
                "Explore el impacto de sus decisiones",
                "Cada caso puede escalar de formas inesperadas.",
                "Siguiente →",
                () -> goToSection(3),
                "-fx-background-color: rgba(224,157,16,0.06); -fx-border-color: rgba(224,157,16,0.18); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12;"
        ));

        return section;
    }

    private VBox buildSection3() {
        VBox section = new VBox(18);
        section.getChildren().add(sectionHeader("⚡", "Cada decisión cambia la historia", "Los casos evolucionan según sus acciones."));
        section.getChildren().add(infoBox("En un despacho fiscal no siempre es evidente qué caso requiere atención inmediata. Un asunto aparentemente menor puede convertirse en:"));

        VBox storyCard = new VBox(12);
        storyCard.setPadding(new Insets(18));
        storyCard.setStyle("-fx-background-color: rgba(14,30,74,0.40); -fx-border-color: rgba(59,130,246,0.18); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12;");

        GridPane chainGrid = new GridPane();
        chainGrid.setHgap(8);
        chainGrid.setVgap(8);
        ColumnConstraints colA = new ColumnConstraints();
        colA.setPercentWidth(50);
        ColumnConstraints colB = new ColumnConstraints();
        colB.setPercentWidth(50);
        chainGrid.getColumnConstraints().addAll(colA, colB);
        chainGrid.add(chainItem("Una red de estafa"), 0, 0);
        chainGrid.add(chainItem("Un caso de corrupción"), 1, 0);
        chainGrid.add(chainItem("Una estructura criminal organizada"), 0, 1);
        chainGrid.add(chainItem("Una investigación de alto impacto regional"), 1, 1);

        Label decisions = new Label("POR ELLO, USTED DEBERÁ DECIDIR:");
        decisions.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");

        FlowPane tags = new FlowPane();
        tags.setHgap(8);
        tags.setVgap(8);
        tags.getChildren().addAll(
                tag("⚖️ Qué investigar", "gold"),
                tag("📊 Qué priorizar", "blue"),
                tag("🤝 Qué delegar", "green"),
                tag("📤 Qué remitir", "red"),
                tag("🔗 Qué asociar con otros casos.", "blue"),
                tag("❌ Y qué asuntos no corresponden a su competencia.", "gold")
        );

        storyCard.getChildren().addAll(chainGrid);
        section.getChildren().addAll(storyCard, decisions, tags, ctaRow(
                "Un paso más",
                "Revise el objetivo final del simulador.",
                "Siguiente →",
                () -> goToSection(4),
                "-fx-background-color: rgba(224,157,16,0.06); -fx-border-color: rgba(224,157,16,0.18); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12;"
        ));

        return section;
    }

    private VBox buildSection4() {
        VBox section = new VBox(18);
        section.getChildren().add(sectionHeader("🎯", "Objetivo de NEXUS", "Lo que se evalúa en la simulación."));
        section.getChildren().add(infoBox("Lograr un equilibrio sostenible entre: legalidad, eficiencia y atención real a víctimas."));

        GridPane objectives = new GridPane();
        objectives.setHgap(10);
        objectives.setVgap(10);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(33.33);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(33.33);
        ColumnConstraints c3 = new ColumnConstraints();
        c3.setPercentWidth(33.33);
        objectives.getColumnConstraints().addAll(c1, c2, c3);
        objectives.add(objCard("⚖️", "Legalidad"), 0, 0);
        objectives.add(objCard("⚡", "Eficiencia"), 1, 0);
        objectives.add(objCard("🎯", "Priorización"), 2, 0);
        objectives.add(objCard("👤", "Atención a víctimas"), 0, 1);
        objectives.add(objCard("🔧", "Gestión de recursos"), 1, 1);
        objectives.add(objCard("📈", "Resultados investigativos"), 2, 1);

        Label quote = new Label("Porque en un despacho fiscal real, resolver un caso no solo significa tomar la decisión correcta. También significa tomarla en el momento adecuado.");
        quote.setWrapText(true);
        quote.setTextAlignment(TextAlignment.CENTER);
        quote.setStyle("-fx-background-color: rgba(224,157,16,0.07); -fx-border-color: rgba(224,157,16,0.20); -fx-border-width: 1; -fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 16 18 16 18; -fx-text-fill: #fcd34d; -fx-font-size: 14; -fx-font-family: 'Segoe UI'; -fx-font-style: italic;");

        section.getChildren().addAll(objectives, quote, ctaRow(
                "¡Todo listo para comenzar!",
                "Ingrese al despacho y tome su primer turno.",
                "🚀 Ir a NEXUS",
                this::launchNexus,
                "-fx-background-color: rgba(198,130,10,0.10); -fx-border-color: rgba(224,157,16,0.30); -fx-border-width: 1; -fx-background-radius: 12; -fx-border-radius: 12;"
        ));

        return section;
    }

    private HBox sectionHeader(String icon, String title, String desc) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 0, 14, 0));
        row.setStyle("-fx-border-color: rgba(255,255,255,0.07); -fx-border-width: 0 0 1 0;");

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(44, 44);
        iconBox.setMinSize(44, 44);
        iconBox.setMaxSize(44, 44);
        iconBox.setStyle("-fx-background-color: rgba(224,157,16,0.12); -fx-border-color: rgba(224,157,16,0.25); -fx-border-width: 1; -fx-background-radius: 10; -fx-border-radius: 10;");

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 22; -fx-font-family: 'Segoe UI';");
        iconBox.getChildren().add(iconLabel);

        VBox textBox = new VBox(2);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 22; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
        Label descLabel = new Label(desc);
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13; -fx-font-family: 'Segoe UI';");
        textBox.getChildren().addAll(titleLabel, descLabel);

        row.getChildren().addAll(iconBox, textBox);
        return row;
    }

    private Label infoBox(String text) {
        Label box = new Label(text);
        box.setWrapText(true);
        box.setStyle("-fx-background-color: rgba(59,130,246,0.07); -fx-border-color: rgba(59,130,246,0.18); -fx-border-width: 1; -fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 14 16 14 16; -fx-text-fill: #93c5fd; -fx-font-size: 13; -fx-font-family: 'Segoe UI'; -fx-line-spacing: 4;");
        return box;
    }

    private HBox alertCard(String icon, String title, String body) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-border-color: rgba(224,157,16,0.28) rgba(255,255,255,0.10) rgba(255,255,255,0.10) rgba(255,255,255,0.10); -fx-border-width: 1 1 1 3; -fx-background-radius: 10; -fx-border-radius: 10;");

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 22; -fx-font-family: 'Segoe UI';");

        VBox texts = new VBox(3);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 14; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
        Label bodyLabel = new Label(body);
        bodyLabel.setWrapText(true);
        bodyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13; -fx-font-family: 'Segoe UI';");
        texts.getChildren().addAll(titleLabel, bodyLabel);
        HBox.setHgrow(texts, Priority.ALWAYS);

        card.getChildren().addAll(iconLabel, texts);
        return card;
    }

    private HBox featureItem(String text) {
        return featureItem(text, "");
    }

    private HBox featureItem(String bold, String rest) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(12, 14, 12, 14));
        row.setStyle("-fx-background-color: rgba(255,255,255,0.02); -fx-border-color: rgba(255,255,255,0.06); -fx-border-width: 1; -fx-background-radius: 8; -fx-border-radius: 8;");

        VBox dotWrap = new VBox();
        dotWrap.setPadding(new Insets(5, 0, 0, 0));
        Circle dot = new Circle(4, Color.web("#e09d10"));
        dotWrap.getChildren().add(dot);

        String content = (bold == null ? "" : bold.trim());
        if (rest != null && !rest.isBlank()) {
            content = content + " " + rest.trim();
        }
        Label text = new Label(content);
        text.setWrapText(true);
        text.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13; -fx-font-family: 'Segoe UI';");
        HBox.setHgrow(text, Priority.ALWAYS);

        row.getChildren().addAll(dotWrap, text);
        return row;
    }

    private Label tag(String text, String type) {
        Label label = new Label(text);
        String colorStyle;
        if ("gold".equals(type)) {
            colorStyle = "-fx-background-color: rgba(224,157,16,0.10); -fx-border-color: rgba(224,157,16,0.20); -fx-text-fill: #e09d10;";
        } else if ("blue".equals(type)) {
            colorStyle = "-fx-background-color: rgba(59,130,246,0.10); -fx-border-color: rgba(59,130,246,0.20); -fx-text-fill: #60a5fa;";
        } else if ("green".equals(type)) {
            colorStyle = "-fx-background-color: rgba(34,197,94,0.10); -fx-border-color: rgba(34,197,94,0.20); -fx-text-fill: #4ade80;";
        } else if ("red".equals(type)) {
            colorStyle = "-fx-background-color: rgba(239,68,68,0.10); -fx-border-color: rgba(239,68,68,0.20); -fx-text-fill: #f87171;";
        } else {
            colorStyle = "-fx-background-color: rgba(255,255,255,0.10); -fx-border-color: rgba(255,255,255,0.20); -fx-text-fill: #cbd5e1;";
        }
        label.setStyle(colorStyle + " -fx-border-width: 1; -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 5 12 5 12; -fx-font-size: 12; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
        return label;
    }

    private HBox ctaRow(String title, String sub, String btnLabel, Runnable action, String rowStyle) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(18, 20, 18, 20));
        row.setStyle(rowStyle);

        VBox textBox = new VBox(3);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 15; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
        Label subLabel = new Label(sub);
        subLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13; -fx-font-family: 'Segoe UI';");
        textBox.getChildren().addAll(titleLabel, subLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actionButton = new HBox();
        actionButton.setAlignment(Pos.CENTER);
        actionButton.setCursor(javafx.scene.Cursor.HAND);
        actionButton.setPadding(new Insets(12, 24, 12, 24));
        actionButton.setStyle("-fx-background-color: #e09d10; -fx-background-radius: 10;");

        Label buttonText = new Label(btnLabel);
        buttonText.setStyle("-fx-text-fill: #0c1427; -fx-font-size: 14; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
        actionButton.getChildren().add(buttonText);

        actionButton.setOnMouseClicked(e -> action.run());
        actionButton.setOnMouseEntered(e -> {
            ScaleTransition grow = new ScaleTransition(Duration.millis(120), actionButton);
            grow.setToX(1.04);
            grow.setToY(1.04);
            grow.playFromStart();
        });
        actionButton.setOnMouseExited(e -> {
            ScaleTransition shrink = new ScaleTransition(Duration.millis(120), actionButton);
            shrink.setToX(1.0);
            shrink.setToY(1.0);
            shrink.playFromStart();
        });

        row.getChildren().addAll(textBox, spacer, actionButton);
        return row;
    }

    private VBox objCard(String icon, String label) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(14, 12, 14, 12));
        card.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-border-color: rgba(255,255,255,0.07); -fx-border-width: 1; -fx-background-radius: 10; -fx-border-radius: 10;");

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 24; -fx-font-family: 'Segoe UI';");

        Label text = new Label(label);
        text.setWrapText(true);
        text.setTextAlignment(TextAlignment.CENTER);
        text.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");

        card.getChildren().addAll(iconLabel, text);
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: rgba(224,157,16,0.07); -fx-border-color: rgba(224,157,16,0.20); -fx-border-width: 1; -fx-background-radius: 10; -fx-border-radius: 10;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-border-color: rgba(255,255,255,0.07); -fx-border-width: 1; -fx-background-radius: 10; -fx-border-radius: 10;"));
        return card;
    }

    private HBox chainItem(String text) {
        HBox row = new HBox(4);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 10, 8, 10));
        row.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 6;");

        Label arrow = new Label("→ ");
        arrow.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 12; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");

        Label body = new Label(text);
        body.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12; -fx-font-family: 'Segoe UI';");

        row.getChildren().addAll(arrow, body);
        return row;
    }

    private void goToSection(int n) {
        if (n < 1 || n > 4) {
            return;
        }
        if (n == currentSection && !contentVBox.getChildren().isEmpty()) {
            return;
        }
        currentSection = n;
        showSection(n);
        updateNav(n);
        progressBadge.setText("Sección " + n + " / 4");
        contentScroll.setVvalue(0);
    }

    private void showSection(int n) {
        contentVBox.getChildren().clear();
        VBox section = switch (n) {
            case 1 -> sec1;
            case 2 -> sec2;
            case 3 -> sec3;
            case 4 -> sec4;
            default -> sec1;
        };
        contentVBox.getChildren().add(section);
    }

    private void updateNav(int active) {
        for (int i = 0; i < navItems.size(); i++) {
            int section = i + 1;
            HBox item = navItems.get(i);
            StackPane circle = navCircles.get(i);
            Label number = navNumbers.get(i);
            Label text = navTexts.get(i);

            if (section < active) {
                item.setStyle("-fx-background-color: rgba(34,197,94,0.07); -fx-border-color: rgba(34,197,94,0.15); -fx-border-width: 1; -fx-background-radius: 8; -fx-border-radius: 8;");
                circle.setStyle("-fx-background-color: rgba(34,197,94,0.20); -fx-background-radius: 11;");
                number.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 12; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
                text.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 13; -fx-font-family: 'Segoe UI';");
            } else if (section == active) {
                item.setStyle("-fx-background-color: rgba(224,157,16,0.12); -fx-border-color: rgba(224,157,16,0.30); -fx-border-width: 1; -fx-background-radius: 8; -fx-border-radius: 8;");
                circle.setStyle("-fx-background-color: #e09d10; -fx-background-radius: 11;");
                number.setStyle("-fx-text-fill: #0c1427; -fx-font-size: 12; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
                text.setStyle("-fx-text-fill: #e09d10; -fx-font-size: 13; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
            } else {
                item.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-border-width: 1; -fx-background-radius: 8; -fx-border-radius: 8;");
                circle.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 11;");
                number.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
                text.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13; -fx-font-family: 'Segoe UI';");
            }
        }

        for (int i = 0; i < progressSegs.size(); i++) {
            int section = i + 1;
            if (section < active) {
                progressSegs.get(i).setFill(Color.web("#22c55e"));
            } else if (section == active) {
                progressSegs.get(i).setFill(Color.web("#e09d10"));
            } else {
                progressSegs.get(i).setFill(Color.rgb(255, 255, 255, 0.10));
            }
        }
    }

    private void launchNexus() {
        if (!InvestigationTeamContext.ensureConfigured(stage)) {
            return;
        }
        Scene scene = new Scene(new AdminViewNew(stage).getView(), 1500, 900);
        Theme.apply(scene);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setFullScreen(true);
    }

    private void goBackToLogin() {
        LoginView loginView = new LoginView(stage);
        Scene scene = new Scene(loginView.getView(), 1500, 900);
        Theme.apply(scene);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setFullScreen(true);
    }

    public Parent getView() {
        return view;
    }

    public void applyTheme(Scene scene) {
        Theme.apply(scene);
    }
}
