package com.example.desktop.controller;

import com.example.core.excepcion.ErrorCampo;
import com.example.core.excepcion.ExcepcionNegocio;
import com.example.core.excepcion.ValidacionException;
import com.example.core.inventario.CasosDeUsoInventario;
import com.example.core.inventario.DatosElemento;
import com.example.core.inventario.ElementoInventario;
import com.example.core.inventario.EstadoElemento;
import com.example.core.inventario.ValidadorElemento;
import com.example.desktop.Servicios;
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

    private ElementoInventario equipamientoEditando;

    @FXML
    public void initialize() {
        javafx.application.Platform.runLater(txtNombre::requestFocus);
        ResponsiveFormLayout.adapt(formGrid);

        cmbCategoria.getItems().addAll(ValidadorElemento.CATEGORIAS);
        for (EstadoElemento estado : EstadoElemento.values()) {
            cmbEstado.getItems().add(estado.etiqueta());
        }

        // Los mensajes vienen del validador de core; la pantalla solo los muestra junto a cada campo.
        FormValidation.watch(txtNombre, txtNombre.textProperty(), () -> errorDe("nombre"));
        FormValidation.watch(cmbCategoria, cmbCategoria.valueProperty(), () -> errorDe("categoria"));
        FormValidation.watch(txtCantidad, txtCantidad.textProperty(), () -> errorDe("cantidad"));
        FormValidation.watch(cmbEstado, cmbEstado.valueProperty(), () -> errorDe("estado"));
    }

    private EstadoElemento estadoSeleccionado() {
        return EstadoElemento.desdeEtiqueta(cmbEstado.getValue()).orElse(null);
    }

    private String errorDe(String campo) {
        return ValidadorElemento.validar(textoSeguro(txtNombre.getText()), cmbCategoria.getValue(),
                        txtCantidad.getText(), estadoSeleccionado()).stream()
                .filter(e -> e.campo().equals(campo))
                .map(ErrorCampo::mensaje)
                .findFirst().orElse(null);
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

        DatosElemento datos = new DatosElemento(textoSeguro(txtNombre.getText()).trim(), cmbCategoria.getValue(),
                Integer.parseInt(txtCantidad.getText().trim()), estadoSeleccionado());

        CasosDeUsoInventario inventario = Servicios.inventario();
        try {
            if (equipamientoEditando == null) {
                inventario.alta(datos);
            } else {
                inventario.editar(equipamientoEditando.id(), datos);
            }
        } catch (ValidacionException e) {
            marcarErrores(e);
            mostrarAlerta(Alert.AlertType.ERROR, "No se pudo guardar", e.getMessage());
            return;
        } catch (ExcepcionNegocio | IllegalStateException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "No se pudo guardar", e.getMessage());
            return;
        }

        var ventana = txtNombre.getScene().getWindow();
        volverListado();
        ToastNotifications.show(ventana,
                equipamientoEditando == null ? "Elemento agregado al Inventario" : "Elemento actualizado",
                ToastNotifications.Kind.SUCCESS);
    }

    private void marcarErrores(ValidacionException e) {
        for (ErrorCampo error : e.errores()) {
            switch (error.campo()) {
                case "nombre" -> FormValidation.showError(txtNombre, error.mensaje());
                case "categoria" -> FormValidation.showError(cmbCategoria, error.mensaje());
                case "cantidad" -> FormValidation.showError(txtCantidad, error.mensaje());
                case "estado" -> FormValidation.showError(cmbEstado, error.mensaje());
                default -> { }
            }
        }
    }

    public void cargarEquipamiento(ElementoInventario equipamiento) {

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

        txtNombre.setText(equipamiento.nombre());

        cmbCategoria.setValue(
                equipamiento.categoria());

        txtCantidad.setText(
                String.valueOf(equipamiento.cantidad()));

        cmbEstado.setValue(
                equipamiento.estado().etiqueta());
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

    private String textoSeguro(String valor) {
        return valor == null ? "" : valor;
    }
}
