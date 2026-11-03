package com.example.desktop.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Affine;

/** Loads the path data from one of the sidebar's SVG icon resources. */
public final class SidebarIcon {
    private static final String ICON_DIRECTORY = "/com/example/icons/";
    private static final Pattern PATH_DATA = Pattern.compile("<path\\b[^>]*\\bd=\"([^\"]+)\"");
    private static final Pattern VIEWBOX = Pattern.compile("viewBox=\"([^\"]+)\"");
    private static final double ICON_SIZE = 20.0;

    private SidebarIcon() { }

    public static StackPane load(String fileName) {
        String resource = ICON_DIRECTORY + fileName;
        try (InputStream input = SidebarIcon.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("No se encontró el icono del sidebar: " + resource);
            }

            String svg = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            Matcher viewBoxMatcher = VIEWBOX.matcher(svg);
            Matcher pathMatcher = PATH_DATA.matcher(svg);
            if (!viewBoxMatcher.find() || !pathMatcher.find()) {
                throw new IllegalStateException("El icono del sidebar no contiene un trazado: " + resource);
            }

            String[] bounds = viewBoxMatcher.group(1).trim().split("\\s+");
            if (bounds.length != 4) {
                throw new IllegalStateException("El icono del sidebar tiene un viewBox inválido: " + resource);
            }
            double minX = Double.parseDouble(bounds[0]);
            double minY = Double.parseDouble(bounds[1]);
            double viewWidth = Double.parseDouble(bounds[2]);
            double viewHeight = Double.parseDouble(bounds[3]);
            double scale = ICON_SIZE / Math.max(viewWidth, viewHeight);
            StackPane graphic = new StackPane();
            do {
                SVGPath icon = new SVGPath();
                icon.setContent(pathMatcher.group(1));
                icon.getTransforms().add(new Affine(scale, 0, -minX * scale,
                        0, scale, -minY * scale));
                icon.getStyleClass().add("sidebar-icon");
                icon.setManaged(false);
                graphic.getChildren().add(icon);
            } while (pathMatcher.find());
            graphic.setMinSize(ICON_SIZE, ICON_SIZE);
            graphic.setPrefSize(ICON_SIZE, ICON_SIZE);
            graphic.setMaxSize(ICON_SIZE, ICON_SIZE);
            graphic.getStyleClass().add("sidebar-icon-container");
            return graphic;
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer el icono del sidebar: " + resource, exception);
        }
    }
}
