package com.prisma.data;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.prisma.models.Caso;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public final class CasoRepository {
    private static final ObservableList<Caso> CASOS = FXCollections.observableArrayList();

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp"
    );

    static {
        loadFromCasosFolder();
        if (CASOS.isEmpty()) {
            seed();
        }
    }

    private CasoRepository() {
    }

    public static ObservableList<Caso> getCasos() {
        return CASOS;
    }

    public static void addCaso(Caso caso) {
        CASOS.add(caso);
    }

    /**
     * Escanea la carpeta casos/ buscando imágenes.
     * Por cada imagen encontrada, crea un Caso cuyo nombre es el nombre
     * del archivo sin extensión y cuya imagenPath es la ruta absoluta.
     */
    private static void loadFromCasosFolder() {
        Path casosDir = resolveCasosFolder();
        if (casosDir == null || !Files.isDirectory(casosDir)) {
            return;
        }

        List<Caso> found = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(casosDir)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry) && isImage(entry)) {
                    String fileName = entry.getFileName().toString();
                    String caseName = stripExtension(fileName);
                    Caso caso = new Caso(caseName, entry.toAbsolutePath().toString());
                    found.add(caso);
                }
            }
        } catch (IOException e) {
            System.err.println("PRISMA: No se pudo leer la carpeta casos/: " + e.getMessage());
        }

        found.sort(Comparator.comparing(Caso::getNombre, String.CASE_INSENSITIVE_ORDER));
        CASOS.addAll(found);
    }

    /**
     * Intenta resolver la carpeta casos/ relativa al directorio de trabajo.
     */
    private static Path resolveCasosFolder() {
        Path userDir = Paths.get(System.getProperty("user.dir"), "casos");
        if (Files.isDirectory(userDir)) {
            return userDir;
        }
        return null;
    }

    private static boolean isImage(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return IMAGE_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    private static String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    /** Datos de prueba de fallback cuando la carpeta casos/ está vacía o no existe. */
    private static void seed() {
        if (!CASOS.isEmpty()) {
            return;
        }

        CASOS.addAll(
                new Caso(
                        "Caso Aurora",
                        "Investigación inicial por alteración de evidencia en zona urbana.",
                        "Quito",
                        LocalDate.of(2026, 1, 12),
                        List.of("María López"),
                        List.of("Sujeto A"),
                        List.of("Fraude procesal"),
                        List.of("Fiscalía", "Policía Judicial")
                ),
                new Caso(
                        "Operación Prisma",
                        "Posible red de coacción y encubrimiento con múltiples testigos.",
                        "Guayaquil",
                        LocalDate.of(2026, 2, 4),
                        List.of("Carlos Mena", "Andrea Ruiz"),
                        List.of("Grupo desconocido"),
                        List.of("Coacción", "Encubrimiento"),
                        List.of("Fiscal", "Peritos", "Testigos")
                ),
                new Caso(
                        "Noche Cero",
                        "Hechos violentos vinculados a ingreso forzado y robo agravado.",
                        "Cuenca",
                        LocalDate.of(2026, 3, 18),
                        List.of("Luis Andrade"),
                        List.of("Dos implicados"),
                        List.of("Robo agravado", "Lesiones"),
                        List.of("Fiscalía", "Víctima", "Patrullaje")
                ),
                new Caso(
                        "Caso Vértice",
                        "Conjunto de movimientos financieros incompatibles con la actividad declarada.",
                        "Manta",
                        LocalDate.of(2026, 4, 2),
                        List.of("Entidad afectada"),
                        List.of("Administrador interno"),
                        List.of("Lavado de activos"),
                        List.of("Unidad de análisis", "Auditoría")
                ),
                new Caso(
                        "Caso Horizonte",
                        "Conflicto territorial con versiones cruzadas y cadenas de mando mixtas.",
                        "Loja",
                        LocalDate.of(2026, 4, 21),
                        List.of("Juana Torres"),
                        List.of("Sospechoso principal"),
                        List.of("Amenazas", "Asociación ilícita"),
                        List.of("Fiscal", "Investigadores", "Vecinos")
                )
        );
    }
}