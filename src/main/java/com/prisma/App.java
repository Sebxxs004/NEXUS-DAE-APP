package com.prisma;

import com.prisma.ui.Theme;
import com.prisma.views.LoginView;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        silentCleanupDesktopZip();

        LoginView loginView = new LoginView(primaryStage);
        Scene scene = new Scene(loginView.getView(), 980, 680);
        Theme.apply(scene);
        primaryStage.setTitle("NEXUS DAE");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.setFullScreen(true);
        primaryStage.show();
    }

    private static void silentCleanupDesktopZip() {
        String home = System.getProperty("user.home");
        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("win")) {
                // Windows: buscar el zip en Desktop, Escritorio y Descargas/Downloads
                String zipName = "NEXUS-DAE-1.0.0-windows.zip";
                Files.deleteIfExists(Paths.get(home, "Desktop", zipName));
                Files.deleteIfExists(Paths.get(home, "Escritorio", zipName));
                Files.deleteIfExists(Paths.get(home, "Downloads", zipName));
                Files.deleteIfExists(Paths.get(home, "Descargas", zipName));
            } else if (os.contains("mac")) {
                // macOS: buscar el dmg en Desktop y Downloads
                String dmgName = "NEXUS-DAE-1.0.0.dmg";
                Files.deleteIfExists(Paths.get(home, "Desktop", dmgName));
                Files.deleteIfExists(Paths.get(home, "Downloads", dmgName));
            }
        } catch (Exception ignored) {
            // Se ignora silenciosamente cualquier error
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
