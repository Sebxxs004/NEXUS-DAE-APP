package com.prisma.controllers;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.prisma.data.CasoRepository;
import com.prisma.models.Caso;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class CasoController {
    
    private final CasoRepository casoRepository;

    public CasoController(CasoRepository casoRepository) {
        this.casoRepository = casoRepository;
    }

    @GetMapping("/casos")
    public List<Caso> getAllCasos(@RequestParam(value = "rol", defaultValue = "FISCAL") String rol) {
        return casoRepository.getCasosByRol(rol);
    }

    @GetMapping("/images/{nombreCaso}")
    public ResponseEntity<Resource> getImage(
            @PathVariable String nombreCaso,
            @RequestParam(value = "rol", defaultValue = "FISCAL") String rol
    ) {
        Optional<Caso> casoOpt = casoRepository.findCasoForImage(nombreCaso, rol);

        if (casoOpt.isPresent() && casoOpt.get().getImagenPath() != null) {
            try {
                Path path = Paths.get(casoOpt.get().getImagenPath());
                Resource resource = new UrlResource(path.toUri());
                if (resource.exists() || resource.isReadable()) {
                    String contentType = Files.probeContentType(path);
                    if (contentType == null) {
                        contentType = "application/octet-stream";
                    }
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(contentType))
                            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                            .body(resource);
                }
            } catch (IOException e) {
                // ignore and return 404
            }
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/alertas")
    public List<String> getAlertas(@RequestParam(value = "rol", defaultValue = "FISCAL") String rol) {
        List<String> files = listAlertFiles("ASISTENTE".equalsIgnoreCase(rol) ? "alertas-asistente" : "alertas");
        if (files.isEmpty() && "ASISTENTE".equalsIgnoreCase(rol)) {
            files = listAlertFiles("alertas");
        }
        return files;
    }

    @GetMapping("/alertas/image/{fileName}")
    public ResponseEntity<Resource> getAlertaImage(
            @PathVariable String fileName,
            @RequestParam(value = "rol", defaultValue = "FISCAL") String rol
    ) {
        Path folder = Paths.get(System.getProperty("user.dir"),
                "ASISTENTE".equalsIgnoreCase(rol) ? "alertas-asistente" : "alertas");
        Path target = folder.resolve(fileName).normalize();

        if (!Files.exists(target) && "ASISTENTE".equalsIgnoreCase(rol)) {
            target = Paths.get(System.getProperty("user.dir"), "alertas").resolve(fileName).normalize();
        }

        if (Files.isRegularFile(target)) {
            try {
                Resource resource = new UrlResource(target.toUri());
                String contentType = Files.probeContentType(target);
                if (contentType == null) {
                    contentType = "image/png";
                }
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } catch (IOException e) {
                // ignore
            }
        }
        return ResponseEntity.notFound().build();
    }

    private List<String> listAlertFiles(String folderName) {
        Path dir = Paths.get(System.getProperty("user.dir"), folderName);
        List<String> result = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return result;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                if (Files.isRegularFile(p)) {
                    String name = p.getFileName().toString();
                    String lower = name.toLowerCase();
                    if ((lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp"))
                            && !lower.contains("correo-electronico")) {
                        result.add(name);
                    }
                }
            }
        } catch (IOException ignored) {
        }
        result.sort(Comparator.naturalOrder());
        return result;
    }
}
