package com.prisma.reports;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * Genera el reporte PDF final de investigación con estética PRISMA (navy / dorado).
 */
public final class InvestigationReportPdfExporter {

    private static final float MARGIN = 44f;
    private static final float PAGE_BOTTOM = 52f;
    private static final int WRAP_CHARS = 92;

    private InvestigationReportPdfExporter() {
    }

    public static void generate(Path output, ReportData data) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PdfCanvas canvas = new PdfCanvas(document);
            try {
                canvas.drawCoverHeader();
                canvas.drawMetaRow("Integrantes", data.investigatorName());
                canvas.drawMetaRow("Fecha de cierre", data.closedAt());
                canvas.drawMetaRow("Motivo de cierre", data.endReason());
                canvas.drawMetaRow("Duracion maxima", data.maxDuration());
                canvas.gap(10);

                canvas.drawSectionTitle("1. Conexiones entre casos");
                if (data.connections().isEmpty()) {
                    canvas.drawMuted("- No se registraron conexiones.");
                } else {
                    int index = 1;
                    for (ConnectionEntry connection : data.connections()) {
                        canvas.drawCardTitle(index + ". " + connection.caseFrom() + "  <->  " + connection.caseTo());
                        canvas.drawField("Tipo de asociacion", connection.associationType());
                        canvas.drawField("Justificacion", connection.justification());
                        canvas.gap(10);
                        index++;
                    }
                }

                canvas.drawSectionTitle("2. Grupos de casos");
                if (data.groups().isEmpty()) {
                    canvas.drawMuted("- No se detectaron grupos.");
                } else {
                    int index = 1;
                    for (GroupEntry group : data.groups()) {
                        String status = group.finalized() ? "Finalizado" : "En proceso";
                        canvas.drawCardTitle(index + ". " + group.name() + "  (" + group.memberCount() + " casos)  [" + status + "]");
                        canvas.drawField("Justificacion del grupo", group.groupJustification());
                        canvas.drawField("Casos incluidos", String.join(", ", group.memberNames()));

                        if (group.decisions().isEmpty()) {
                            canvas.drawMuted("   Sin decisiones registradas al finalizar el grupo.");
                        } else {
                            canvas.drawSubheading("Decisiones del fiscal");
                            for (DecisionEntry decision : group.decisions()) {
                                canvas.drawField("Decision", decision.title());
                                canvas.drawField("Justificacion de la decision", decision.justification());
                                canvas.gap(8);
                            }
                        }
                        canvas.gap(12);
                        index++;
                    }
                }

                canvas.drawSectionTitle("3. Casos aislados");
                if (data.isolatedCases().isEmpty()) {
                    canvas.drawMuted("- No quedaron casos aislados.");
                } else {
                    int index = 1;
                    for (IsolatedEntry isolated : data.isolatedCases()) {
                        canvas.drawCardTitle(index + ". " + isolated.caseName());
                        canvas.drawField("Justificacion", isolated.justification());
                        canvas.gap(10);
                        index++;
                    }
                }

                canvas.drawSectionTitle("4. Alertas distractivas (correos)");
                if (data.alerts().isEmpty()) {
                    canvas.drawMuted("- No se registraron alertas distractivas.");
                } else {
                    int index = 1;
                    for (AlertEntry alert : data.alerts()) {
                        canvas.drawCardTitle("Alerta " + index + " — " + alert.imageName());
                        canvas.drawField("Fecha", alert.timestamp());
                        canvas.drawField("Estado", alert.status());

                        if (alert.responded()) {
                            canvas.drawSubheading("Respuesta del fiscal al correo");
                            if (alert.imagePath() != null && Files.isRegularFile(alert.imagePath())) {
                                canvas.drawField("Imagen de la alerta", alert.imageName());
                                canvas.drawImage(alert.imagePath(), 460f, 300f);
                            } else {
                                canvas.drawMuted("   Imagen no disponible en disco: " + alert.imageName());
                            }
                            canvas.drawField("Texto de respuesta", alert.responseText());
                        } else {
                            canvas.drawMuted("   El fiscal no registro respuesta escrita a esta alerta.");
                        }
                        canvas.gap(14);
                        index++;
                    }
                }

                canvas.drawFooter("PRISMA DAE — Fiscalia General de la Nacion");
            } finally {
                canvas.close();
            }
            document.save(output.toFile());
        }
    }

    public record ReportData(
            String investigatorName,
            String closedAt,
            String endReason,
            String maxDuration,
            List<ConnectionEntry> connections,
            List<GroupEntry> groups,
            List<IsolatedEntry> isolatedCases,
            List<AlertEntry> alerts) {
    }

    public record ConnectionEntry(
            String caseFrom,
            String caseTo,
            String associationType,
            String justification) {
    }

    public record GroupEntry(
            String name,
            int memberCount,
            boolean finalized,
            String groupJustification,
            List<String> memberNames,
            List<DecisionEntry> decisions) {
    }

    public record DecisionEntry(String title, String justification) {
    }

    public record IsolatedEntry(String caseName, String justification) {
    }

    public record AlertEntry(
            String timestamp,
            String imageName,
            String status,
            String responseText,
            Path imagePath,
            boolean responded) {
    }

    public static List<DecisionEntry> parseDecisions(String decisionsSummary, String decisionsDetail) {
        List<DecisionEntry> parsed = new ArrayList<>();
        if (decisionsDetail != null && !decisionsDetail.isBlank()) {
            String[] blocks = decisionsDetail.split("\u2022");
            for (String block : blocks) {
                String trimmed = block.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                int justificationIndex = trimmed.indexOf("Justificacion:");
                if (justificationIndex < 0) {
                    justificationIndex = trimmed.indexOf("Justificación:");
                }
                String title = trimmed;
                String justification = "";
                if (justificationIndex >= 0) {
                    title = trimmed.substring(0, justificationIndex).trim();
                    justification = trimmed.substring(justificationIndex).replaceFirst("^Justificaci[oó]n:\\s*", "").trim();
                }
                if (!title.isEmpty()) {
                    parsed.add(new DecisionEntry(title, justification.isBlank() ? "Sin justificacion" : justification));
                }
            }
        }

        if (parsed.isEmpty() && decisionsSummary != null && !decisionsSummary.isBlank()) {
            String[] titles = decisionsSummary.split("\\|");
            for (String title : titles) {
                String clean = title.trim();
                if (!clean.isEmpty()) {
                    parsed.add(new DecisionEntry(clean, decisionsDetail == null ? "" : decisionsDetail.trim()));
                }
            }
        }
        return parsed;
    }

    public static ConnectionEntry parseConnection(String caseFrom, String caseTo, String reasonRaw) {
        String association = "No especificado";
        String justification = "Sin justificacion";
        if (reasonRaw != null && !reasonRaw.isBlank()) {
            String details = reasonRaw.trim();
            int justificationIndex = details.indexOf("Justificación:");
            if (justificationIndex < 0) {
                justificationIndex = details.indexOf("Justificacion:");
            }
            if (justificationIndex >= 0) {
                justification = details.substring(justificationIndex).replaceFirst("^Justificaci[oó]n:\\s*", "").trim();
                details = details.substring(0, justificationIndex).trim();
            }
            if (details.startsWith("Asociado por:")) {
                association = details.substring("Asociado por:".length()).trim();
            } else if (!details.isBlank()) {
                association = details;
            }
        }
        if (justification.isBlank()) {
            justification = "Sin justificacion";
        }
        if (association.isBlank()) {
            association = "No especificado";
        }
        return new ConnectionEntry(caseFrom, caseTo, association, justification);
    }

    public static Path resolveAlertImagePath(String imageName) {
        if (imageName == null || imageName.isBlank()) {
            return null;
        }
        Path path = Path.of(System.getProperty("user.dir"), "alertas", imageName);
        return Files.isRegularFile(path) ? path : null;
    }

    public static boolean alertWasAnswered(String status, String responseText) {
        boolean hasText = responseText != null && !responseText.isBlank();
        if (hasText) {
            return true;
        }
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toUpperCase();
        return "RESPONDIDA".equals(normalized) || "POSPUESTA".equals(normalized);
    }

    private static final class PdfCanvas {
        private static final float SECTION_BAR_HEIGHT = 24f;
        private static final float SECTION_GAP_AFTER = 18f;
        private static final float LINE_SPACING = 7f;

        private static final float[] COLOR_NAVY = {8f / 255f, 20f / 255f, 46f / 255f};
        private static final float[] COLOR_NAVY_PANEL = {13f / 255f, 31f / 255f, 89f / 255f};
        private static final float[] COLOR_GOLD = {200f / 255f, 160f / 255f, 59f / 255f};
        private static final float[] COLOR_BODY = {0f, 0f, 0f};
        private static final float[] COLOR_MUTED = {0.35f, 0.35f, 0.42f};
        private static final float[] COLOR_META_LABEL = {0.2f, 0.2f, 0.28f};

        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream content;
        private float y;
        private final float pageWidth;
        private final float contentWidth;

        private PdfCanvas(PDDocument document) throws IOException {
            this.document = document;
            this.page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            this.pageWidth = page.getMediaBox().getWidth();
            this.contentWidth = pageWidth - (MARGIN * 2);
            this.content = new PDPageContentStream(document, page);
            this.y = page.getMediaBox().getHeight() - MARGIN;
        }

        private void close() throws IOException {
            if (content != null) {
                content.close();
            }
        }

        private void newPageIfNeeded(float requiredHeight) throws IOException {
            if (y - requiredHeight >= PAGE_BOTTOM) {
                return;
            }
            content.close();
            page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        private void drawCoverHeader() throws IOException {
            newPageIfNeeded(70);
            fillRect(MARGIN, y - 52, contentWidth, 52, COLOR_NAVY);
            fillRect(MARGIN, y - 54, contentWidth, 2, COLOR_GOLD);
            writeText("PRISMA DAE", MARGIN + 14, y - 24, 20, true, COLOR_GOLD);
            writeText("Reporte de investigacion estructural", MARGIN + 14, y - 42, 11, false, COLOR_META_LABEL);
            y -= 72;
        }

        private void drawSectionTitle(String title) throws IOException {
            gap(14);
            newPageIfNeeded(SECTION_BAR_HEIGHT + SECTION_GAP_AFTER + 24);
            float barBottom = y - SECTION_BAR_HEIGHT;
            fillRect(MARGIN, barBottom, contentWidth, SECTION_BAR_HEIGHT, COLOR_NAVY_PANEL);
            writeText(toPdfText(title), MARGIN + 10, barBottom + 8, 12, true, COLOR_GOLD);
            y = barBottom - SECTION_GAP_AFTER;
        }

        private void drawCardTitle(String text) throws IOException {
            gap(6);
            newPageIfNeeded(20);
            writeWrapped(text, 11, true, COLOR_BODY, 0);
            gap(4);
        }

        private void drawSubheading(String text) throws IOException {
            gap(8);
            writeWrapped(text, 10, true, COLOR_GOLD, 0);
            gap(4);
        }

        private void drawMetaRow(String label, String value) throws IOException {
            gap(3);
            writeWrapped(label + ": " + safe(value), 10, false, COLOR_BODY, 0);
            gap(5);
        }

        private void drawField(String label, String value) throws IOException {
            writeWrapped(label + ": " + safe(value), 10, false, COLOR_BODY, 12);
            gap(6);
        }

        private void drawMuted(String text) throws IOException {
            writeWrapped(text, 10, false, COLOR_MUTED, 8);
        }

        private void drawFooter(String text) throws IOException {
            gap(12);
            newPageIfNeeded(20);
            writeWrapped(text, 9, false, COLOR_MUTED, 0);
        }

        private void drawImage(Path imagePath, float maxWidth, float maxHeight) throws IOException {
            PDImageXObject image = PDImageXObject.createFromFile(imagePath.toString(), document);
            float iw = image.getWidth();
            float ih = image.getHeight();
            if (iw <= 0 || ih <= 0) {
                return;
            }
            float scale = Math.min(maxWidth / iw, maxHeight / ih);
            float drawWidth = iw * scale;
            float drawHeight = ih * scale;
            newPageIfNeeded(drawHeight + 16);
            float x = MARGIN + 10;
            float imageY = y - drawHeight;
            fillRect(x - 2, imageY - 2, drawWidth + 4, drawHeight + 4, COLOR_NAVY_PANEL);
            content.drawImage(image, x, imageY, drawWidth, drawHeight);
            y = imageY - 14;
        }

        private void gap(float amount) {
            y -= amount;
        }

        private void fillRect(float x, float bottomY, float width, float height, float[] rgb) throws IOException {
            content.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
            content.addRect(x, bottomY, width, height);
            content.fill();
        }

        private void writeText(String text, float x, float baselineY, int fontSize, boolean bold, float[] rgb) throws IOException {
            content.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
            content.beginText();
            content.setFont(bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, fontSize);
            content.newLineAtOffset(x, baselineY);
            content.showText(toPdfText(text));
            content.endText();
        }

        private void writeWrapped(String text, int fontSize, boolean bold, float[] rgb, float indent) throws IOException {
            String value = safe(text);
            if (value.isEmpty()) {
                newPageIfNeeded(fontSize + 8);
                y -= fontSize + 4;
                return;
            }
            int cursor = 0;
            while (cursor < value.length()) {
                int end = Math.min(value.length(), cursor + WRAP_CHARS);
                if (end < value.length()) {
                    int lastSpace = value.lastIndexOf(' ', end);
                    if (lastSpace > cursor + 20) {
                        end = lastSpace;
                    }
                }
                String line = value.substring(cursor, end).trim();
                if (!line.isEmpty()) {
                    float lineHeight = fontSize + LINE_SPACING;
                    newPageIfNeeded(lineHeight + 4);
                    y -= 2;
                    writeText(line, MARGIN + indent, y, fontSize, bold, rgb);
                    y -= lineHeight;
                }
                cursor = end;
                while (cursor < value.length() && value.charAt(cursor) == ' ') {
                    cursor++;
                }
            }
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }

        private static String toPdfText(String input) {
            if (input == null) {
                return "";
            }
            return input
                    .replace('\u2022', '-')
                    .replace("\u2192", "->")
                    .replace("\u2194", "<->")
                    .replace("\u2212", "-")
                    .replace('á', 'a')
                    .replace('é', 'e')
                    .replace('í', 'i')
                    .replace('ó', 'o')
                    .replace('ú', 'u')
                    .replace('Á', 'A')
                    .replace('É', 'E')
                    .replace('Í', 'I')
                    .replace('Ó', 'O')
                    .replace('Ú', 'U')
                    .replace('ñ', 'n')
                    .replace('Ñ', 'N');
        }
    }
}
