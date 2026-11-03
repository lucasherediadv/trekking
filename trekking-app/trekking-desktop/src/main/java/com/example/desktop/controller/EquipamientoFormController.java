package com.example.desktop.controller;

import com.example.desktop.dao.EquipamientoDAO;
import com.example.desktop.model.Equipamiento;
import com.example.desktop.util.FormValidation;
import com.example.desktop.util.ResponsiveFormLayout;
import com.example.desktop.util.ToastNotifications;
import com.example.desktop.util.NavigationShell;
import com.example.desktop.util.AlertUtils;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class EquipamientoFormController {

    @FXML
    private TextField txtNombre;

    @FXML
    private ComboBox<String> cmbCategoria;

    @FXML
    private TextField txtCantidad;

    @FXML
    private ComboBox<String> cmbEstado;

    @FXML
    private Label tituloLabel;

    @FXML
    private javafx.scene.control.Button guardarButton;

    @FXML
    private GridPane formGrid;

    private Equipamiento equipamientoEditando;

    @FXML
    public void initialize() {
        javafx.application.Platform.runLater(txtNombre::requestFocus);
        ResponsiveFormLayout.adapt(formGrid);

        cmbCategoria.getItems().addAll(
                "Campamento",
                "Navegación",
                "Comunicación",
                "Seguridad",
                "Escalada",
                "Vestimenta",
                "Hidratación",
                "Primeros Auxilios",
                "Herramientas",
                "Transporte"
        );

        cmbEstado.getItems().addAll(
                "Disponible",
                "Asignado",
                "En Reparación",
                "En Inspección",
                "Fuera de Servicio",
                "Extraviado"
        );

        FormValidation.watch(txtNombre, txtNombre.textProperty(),
                () -> textoSeguro(txtNombre.getText()).trim().isEmpty()
                        ? "El nombre es obligatorio."
                        : null);
        FormValidation.watch(cmbCategoria, cmbCategoria.valueProperty(),
                () -> cmbCategoria.getValue() == null
                        ? "Seleccione una categoría."
                        : null);
        FormValidation.watch(txtCantidad, txtCantidad.textProperty(),
                this::validarCantidad);
        FormValidation.watch(cmbEstado, cmbEstado.valueProperty(),
                () -> cmbEstado.getValue() == null
                        ? "Seleccione un estado."
                        : null);
    }

    @FXML
    private void guardarEquipamiento() {

        boolean valido = FormValidation.validateNow(txtNombre);
        valido = FormValidation.validateNow(cmbCategoria) && valido;
        valido = FormValidation.validateNow(txtCantidad) && valido;
        valido = FormValidation.validateNow(cmbEstado) && valido;
        if (!valido) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Revisa los campos",
                    "Completa los datos requeridos y corrige los campos marcados.");
            return;
        }

        String nombre = textoSeguro(txtNombre.getText()).trim();
        String categoria = cmbCategoria.getValue();
        String cantidadTexto = txtCantidad.getText().trim();
        String estado = cmbEstado.getValue();

        if (nombre.isEmpty()
                || categoria == null
                || cantidadTexto.isEmpty()
                || estado == null) {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Datos incompletos",
                    "Todos los campos son obligatorios.");

            return;
        }

        int cantidad = Integer.parseInt(cantidadTexto);

        try {
            if (equipamientoEditando == null) {
                EquipamientoDAO.insertar(new Equipamiento(
                        0, nombre, categoria, cantidad, estado));
            } else {
                EquipamientoDAO.actualizar(new Equipamiento(
                        equipamientoEditando.getId(), nombre, categoria, cantidad, estado));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "No se pudo guardar", e.getMessage());
            return;
        }

        var ventana = txtNombre.getScene().getWindow();
        volverListado();
        ToastNotifications.show(ventana,
                equipamientoEditando == null ? "Elemento agregado al Inventario" : "Elemento actualizado",
                ToastNotifications.Kind.SUCCESS);
    }

    public void cargarEquipamiento(Equipamiento equipamiento) {

        equipamientoEditando = equipamiento;

        if (equipamiento == null) {
            tituloLabel.setText("Nuevo elemento");
            guardarButton.setText("Crear elemento");
        } else {
            tituloLabel.setText("Editar elemento");
            guardarButton.setText("Guardar cambios");
        }

        if (equipamiento == null) {
            return;
        }

        txtNombre.setText(equipamiento.getNombre());

        cmbCategoria.setValue(
                equipamiento.getCategoria());

        txtCantidad.setText(
                String.valueOf(equipamiento.getCantidad()));

        cmbEstado.setValue(
                equipamiento.getEstado());
    }

    @FXML
    private void cancelar() {
        volverListado();
    }

    private void volverListado() {

        try {

            javafx.fxml.FXMLLoader loader =
                    new javafx.fxml.FXMLLoader(
                            getClass().getResource(
                                    "/com/example/fxml/equipamiento.fxml"));

            javafx.scene.Parent root = loader.load();

            Stage stage =
                    (Stage) txtNombre.getScene().getWindow();

            NavigationShell.setRoot(stage.getScene(), root);

            stage.setTitle("Equipamiento");

        } catch (java.io.IOException e) {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo volver al listado de equipamiento.");
        }
    }

    private void mostrarAlerta(
            Alert.AlertType tipo,
            String titulo,
            String mensaje) {

        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        AlertUtils.showAndWait(alerta, txtNombre);
    }

    private String validarCantidad() {
        String cantidad = textoSeguro(txtCantidad.getText()).trim();
        if (cantidad.isEmpty()) {
            return "La cantidad es obligatoria.";
        }
        try {
            if (Integer.parseInt(cantidad) < 0) {
                return "La cantidad total debe ser mayor o igual a cero.";
            }
            return null;
        } catch (NumberFormatException e) {
            return "La cantidad debe ser un número entero mayor o igual a cero.";
        }
    }

    private String textoSeguro(String valor) {
        return valor == null ? "" : valor;
    }
}
