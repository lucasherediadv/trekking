package com.example.desktop.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.net.URL;
import java.util.Optional;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Button;

/**
 * * Utilidades para mostrar alertas modales y aplicar el tema visual *
 * compartido de la aplicación.
 */
public final class AlertUtils {
    private AlertUtils() {
    }

    /**
     * * Aplica la hoja de estilos de la aplicación a un diálogo JavaFX. * * @param
     * dialog diálogo al que se aplicarán los estilos
     */
    public static void applyTheme(Dialog<?> dialog) {
        if (dialog == null) {
            return;
        }
        DialogPane pane = dialog.getDialogPane();
        if (pane == null) {
            return;
        }
        URL cssUrl = AlertUtils.class.getResource("/com/example/fxml/styles.css");
        if (cssUrl != null) {
            String cssPath = cssUrl.toExternalForm();
            if (!pane.getStylesheets().contains(cssPath)) {
                pane.getStylesheets().add(cssPath);
            }
        }
    }

    public static void showError(Window owner, String title, String message) {
        showAlert(Alert.AlertType.ERROR, owner, title, message);
    }

    public static void showWarning(Window owner, String title, String message) {
        showAlert(Alert.AlertType.WARNING, owner, title, message);
    }

    public static Optional<ButtonType> showAndWait(Alert alert, Node ownerNode) {
        Window owner = ownerNode == null || ownerNode.getScene() == null
                ? null
                : ownerNode.getScene().getWindow();
        return showAndWait(alert, owner);
    }

    public static Optional<ButtonType> showAndWait(Alert alert, Window owner) {
        applyTheme(alert);
        traducirBotonesEstandar(alert);
        if (owner != null) {
            alert.initOwner(owner);
            alert.initModality(Modality.WINDOW_MODAL);
            Scene ownerScene = owner.getScene();
            Node previousFocusOwner = ownerScene == null ? null : ownerScene.getFocusOwner();
            alert.setOnShown(event -> {
                alert.setX(owner.getX() + (owner.getWidth() - alert.getWidth()) / 2);
                alert.setY(owner.getY() + (owner.getHeight() - alert.getHeight()) / 2);
            });
            alert.setOnHidden(event -> {
                if (!owner.isShowing()) return;
                if (owner instanceof Stage stage) stage.toFront();
                if (previousFocusOwner != null && previousFocusOwner.getScene() == ownerScene) {
                    previousFocusOwner.requestFocus();
                }
            });
        }
        return alert.showAndWait();
    }

    private static void traducirBotonesEstandar(Alert alert) {
        for (ButtonType tipo : alert.getButtonTypes()) {
            String texto = tipo == ButtonType.OK || tipo == ButtonType.YES ? "Aceptar"
                    : tipo == ButtonType.CANCEL ? "Cancelar"
                    : tipo == ButtonType.NO ? "No"
                    : tipo == ButtonType.CLOSE ? "Cerrar" : null;
            if (texto == null) continue;
            javafx.scene.Node nodo = alert.getDialogPane().lookupButton(tipo);
            if (nodo instanceof Button boton) {
                boton.setText(texto);
                boton.setAccessibleText(texto);
            }
        }
    }

    private static void showAlert(Alert.AlertType type, Window owner, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyTheme(alert);
        showAndWait(alert, owner);
    }
}
