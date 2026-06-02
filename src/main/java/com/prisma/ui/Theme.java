package com.prisma.ui;

import javafx.scene.Scene;

public final class Theme {
    private static final String STYLESHEET = Theme.class.getResource("/styles/nexus.css").toExternalForm();

    private Theme() {
    }

    public static void apply(Scene scene) {
        if (!scene.getStylesheets().contains(STYLESHEET)) {
            scene.getStylesheets().add(STYLESHEET);
        }
    }
}