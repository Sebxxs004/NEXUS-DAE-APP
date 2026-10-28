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
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import com.prisma.models.Caso;

@Service
public class CasoRepository {
    private final List<Caso> casosFiscal = new ArrayList<>();
    private final List<Caso> casosAsistente = new ArrayList<>();

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp"
    );

    @PostConstruct
    public void init() {
        reloadAll();
    }

    public synchronized void reloadAll() {
        casosFiscal.clear();
        casosAsistente.clear();

        loadFromFolder("casos", casosFiscal);
        loadFromFolder("casos-asistente", casosAsistente);

        if (casosFiscal.isEmpty()) {
            seed(casosFiscal);
        }
    }

    public List<Caso> getCasos() {
        return getCasosByRol("FISCAL");
    }

    public synchronized List<Caso> getCasosByRol(String rol) {
        if ("ASISTENTE".equalsIgnoreCase(rol)) {
            // Re-scan casos-asistente in case new images were added
            List<Caso> dynamicAsistente = new ArrayList<>();
            loadFromFolder("casos-asistente", dynamicAsistente);
            if (!dynamicAsistente.isEmpty()) {
                return dynamicAsistente;
            }
            // Fallback to fiscal cases if casos-asistente is still empty
            return new ArrayList<>(casosFiscal);
        }
        return new ArrayList<>(casosFiscal);
    }

    public Optional<Caso> findCasoForImage(String nombreCaso, String rol) {
        if ("ASISTENTE".equalsIgnoreCase(rol)) {
            List<Caso> asistenteList = new ArrayList<>();
            loadFromFolder("casos-asistente", asistenteList);
            Optional<Caso> foundInAsistente = asistenteList.stream()
                    .filter(c -> c.getNombre().equals(nombreCaso))
                    .findFirst();
            if (foundInAsistente.isPresent()) {
                return foundInAsistente;
            }
        }
        return casosFiscal.stream()
                .filter(c -> c.getNombre().equals(nombreCaso))
                .findFirst();
    }

    public void addCaso(Caso caso) {
        casosFiscal.add(caso);
    }

    private void loadFromFolder(String folderName, List<Caso> targetList) {
        Path dir = resolveFolder(folderName);
        if (dir == null || !Files.isDirectory(dir)) {
            return;
        }

        List<Caso> found = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry) && isImage(entry)) {
                    String fileName = entry.getFileName().toString();
                    String caseName = stripExtension(fileName);
                    Caso caso = new Caso(caseName, entry.toAbsolutePath().toString());
                    found.add(caso);
                }
            }
        } catch (IOException e) {
            System.err.println("NEXUS: No se pudo leer la carpeta " + folderName + "/: " + e.getMessage());
        }

        found.sort(Comparator.comparing(Caso::getNombre, String.CASE_INSENSITIVE_ORDER));
        targetList.addAll(found);
    }

    private Path resolveFolder(String folderName) {
        Path userDir = Paths.get(System.getProperty("user.dir"), folderName);
        if (Files.isDirectory(userDir)) {
            return userDir;
        }
        return null;
    }

    private boolean isImage(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return IMAGE_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    private String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    private void seed(List<Caso> target) {
        if (!target.isEmpty()) {
            return;
        }

        target.addAll(List.of(
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
                        "Operación NEXUS",
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
        ));
    }
}
