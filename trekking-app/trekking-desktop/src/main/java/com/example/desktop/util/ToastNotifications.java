package com.example.desktop.util;

import java.net.URL;

import javafx.animation.PauseTransition;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/** Avisos breves que no interrumpen la tarea en curso. */
public final class ToastNotifications {
    public enum Kind { SUCCESS, WARNING, ERROR }

    private ToastNotifications() { }

    public static void show(Window owner, String message, Kind kind) {
        if (owner == null || !owner.isShowing() || message == null || message.isBlank()) return;

        Label toast = new Label(message);
        toast.setWrapText(true);
        toast.setMaxWidth(360);
        toast.setAccessibleText(message);
        toast.getStyleClass().add("toast-notification");
        toast.getStyleClass().add(switch (kind) {
            case SUCCESS -> "toast-success";
            case WARNING -> "toast-warning";
            case ERROR -> "toast-error";
        });

        Popup popup = new Popup();
        popup.setAutoHide(false);
        popup.setHideOnEscape(false);
        popup.getContent().add(toast);
        Scene popupScene = popup.getScene();
        URL stylesheet = ToastNotifications.class.getResource("/com/example/fxml/styles.css");
        if (stylesheet != null) popupScene.getStylesheets().add(stylesheet.toExternalForm());
        toast.applyCss();
        toast.autosize();

        double x = owner.getX() + owner.getWidth() - toast.prefWidth(-1) - 24;
        double y = owner.getY() + owner.getHeight() - toast.prefHeight(-1) - 32;
        popup.show(owner, x, y);

        PauseTransition cierre = new PauseTransition(Duration.seconds(3.5));
        cierre.setOnFinished(event -> popup.hide());
        cierre.play();
    }
}
