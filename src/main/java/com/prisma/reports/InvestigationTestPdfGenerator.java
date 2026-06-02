package com.prisma.reports;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public final class InvestigationTestPdfGenerator {

    private InvestigationTestPdfGenerator() {
    }

    public static void main(String[] args) throws Exception {
        Path pdf = generate();
        System.out.println(pdf.toAbsolutePath());
    }

    public static Path generate() throws IOException {
        Path downloads = Paths.get(System.getProperty("user.home"), "Downloads");
        Files.createDirectories(downloads);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path output = downloads.resolve("prisma-pdf-prueba-" + timestamp + ".pdf");

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            float margin = 50;
            float y = page.getMediaBox().getHeight() - margin;

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                y = writeLine(content, y, 16, true, "PRISMA DAE - Reporte de Investigación (Prueba)");
                y = writeLine(content, y, 11, false, "Integrantes: a, b, c");
                y = writeLine(content, y, 11, false, "Fecha de cierre: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                y = writeLine(content, y, 11, false, "Motivo de cierre: finalización manual");
                y = writeLine(content, y, 11, false, "Duración máxima configurada: 02:00:00");
                y = writeLine(content, y, 11, false, "");

                y = writeLine(content, y, 13, true, "Conexiones registradas");
                y = writeWrapped(content, y, 11, "1. Caso A <-> Caso B | Justificación: Relación de prueba entre noticias vinculadas.");
                y = writeWrapped(content, y, 11, "2. Caso B <-> Caso C | Justificación: Mismo contexto territorial y patrimonial.");
                y = writeLine(content, y, 11, false, "");

                y = writeLine(content, y, 13, true, "Grupos y justificaciones");
                y = writeWrapped(content, y, 11, "1. Grupo 1 (3 casos) [Finalizado]");
                y = writeWrapped(content, y, 11, "   Justificación: Los elementos evidencian un patrón común de estafa.");
                y = writeWrapped(content, y, 11, "   Decisión: 4. Priorizar.");
                y = writeWrapped(content, y, 11, "   Detalle decisión: Porque el impacto procesal y la urgencia son mayores que otros asuntos.");
                y = writeWrapped(content, y, 11, "   Casos: 050016000206202100084, 110016000023202100010, 110016000028202100031");
                y = writeLine(content, y, 11, false, "");

                y = writeLine(content, y, 13, true, "Casos aislados");
                List<String> isolated = List.of(
                        "050016000206202100084 - Justificación: Se mantiene aislado por falta de coincidencias directas.",
                        "110016000023202100010 - Justificación: Se requiere mayor actividad investigativa.",
                        "110016000028202100031 - Justificación: Aún no existen elementos de conexión suficientes."
                );
                int idx = 1;
                for (String line : isolated) {
                    y = writeWrapped(content, y, 11, idx + ". " + line);
                    idx++;
                }
                y = writeLine(content, y, 11, false, "");

                y = writeLine(content, y, 13, true, "Alertas distractivas");
                y = writeWrapped(content, y, 11, "1. 2026-05-29 13:29:02 | Imagen: correo-urgente.png | Estado: NOTIFICADA");
                y = writeWrapped(content, y, 11, "   Respuesta: Se dejó constancia y se continuó con la priorización del grupo.");
                y = writeWrapped(content, y, 11, "2. 2026-05-29 13:39:02 | Imagen: alerta-victima.png | Estado: LEIDA");
            }

            document.save(output.toFile());
        }

        return output;
    }

    private static float writeLine(PDPageContentStream content, float y, int fontSize, boolean bold, String text) throws IOException {
        if (text == null) {
            text = "";
        }
        if (y < 70) {
            return y;
        }
        content.beginText();
        content.setFont(bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, fontSize);
        content.newLineAtOffset(50, y);
        content.showText(text);
        content.endText();
        return y - (fontSize + 6);
    }

    private static float writeWrapped(PDPageContentStream content, float y, int fontSize, String text) throws IOException {
        if (text == null) {
            text = "";
        }
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (candidate.length() > 92) {
                y = writeLine(content, y, fontSize, false, line.toString());
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(candidate);
            }
        }
        if (line.length() > 0) {
            y = writeLine(content, y, fontSize, false, line.toString());
        }
        return y;
    }
}
