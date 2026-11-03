package com.example.desktop.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Affine;

/** Compact row-action buttons backed by the shared Material SVG resources. */
public final class RowActionButton {
    private static final String ICON_DIRECTORY = "/com/example/icons/";
    private static final Pattern PATH_DATA = Pattern.compile("<path\\b[^>]*\\bd=\"([^\"]+)\"");
    private static final double ICON_SIZE = 18.0;
    private static final double VIEWBOX_SIZE = 960.0;

    public enum Icon {
        EDIT("EDIT.svg"),
        DOWNLOAD("DOWNLOAD.svg"),
        MEDICAL("CLINICAL-NOTES.svg"),
        DELETE("DELETE.svg"),
        RESTORE("RESTORE.svg"),
        MAP("MAP.svg"),
        CHECK("CHECK-CIRCLE.svg"),
        CANCEL("CANCEL.svg"),
        ADD("ADD.svg");

        private final String fileName;
        private String path;

        Icon(String fileName) { this.fileName = fileName; }

        private String path() {
            if (path == null) path = loadPath(fileName);
            return path;
        }
    }

    private RowActionButton() { }

    public static Button create(String label, Icon icon, EventHandler<ActionEvent> action) {
        Button button = new Button();
        button.getStyleClass().add("row-action-button");
        button.setGraphic(graphic(icon));
        button.setTooltip(new Tooltip(label));
        button.setAccessibleText(label);
        button.setOnAction(action);
        return button;
    }

    /** Icon graphic for buttons that also carry a text label. */
    public static Node graphic(Icon icon) {
        SVGPath graphic = new SVGPath();
        graphic.setContent(icon.path());
        double scale = ICON_SIZE / VIEWBOX_SIZE;
        graphic.getTransforms().add(new Affine(scale, 0, 0, 0, scale, ICON_SIZE));
        graphic.getStyleClass().add("row-action-icon");
        graphic.setManaged(false);
        StackPane iconContainer = new StackPane(graphic);
        iconContainer.setMinSize(ICON_SIZE, ICON_SIZE);
        iconContainer.setPrefSize(ICON_SIZE, ICON_SIZE);
        iconContainer.setMaxSize(ICON_SIZE, ICON_SIZE);
        iconContainer.getStyleClass().add("row-action-icon-container");
        return iconContainer;
    }

    private static String loadPath(String fileName) {
        String resource = ICON_DIRECTORY + fileName;
        try (InputStream input = RowActionButton.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("No se encontró el icono de acción: " + resource);
            }
            String svg = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            Matcher matcher = PATH_DATA.matcher(svg);
            if (!matcher.find()) {
                throw new IllegalStateException("El icono de acción no contiene un trazado: " + resource);
            }
            return matcher.group(1);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer el icono de acción: " + resource, exception);
        }
    }
}
