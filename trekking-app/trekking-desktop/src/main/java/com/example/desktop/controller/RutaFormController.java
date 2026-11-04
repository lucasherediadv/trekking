package com.example.desktop.controller;

import com.example.core.excepcion.ErrorCampo;
import com.example.core.excepcion.ExcepcionNegocio;
import com.example.core.excepcion.ValidacionException;
import com.example.core.ruta.DatosRuta;
import com.example.core.ruta.NombreRuta;
import com.example.core.ruta.Ruta;
import com.example.core.ruta.ValidadorRuta;
import com.example.desktop.Servicios;
import com.example.desktop.util.FormValidation;
import com.example.desktop.util.ResponsiveFormLayout;
import com.example.desktop.util.NavigationShell;
import com.example.desktop.util.AlertUtils;
import com.example.desktop.util.ToastNotifications;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import javafx.css.PseudoClass;
import javafx.util.Duration;

import java.io.IOException;
import java.util.function.UnaryOperator;

public class RutaFormController {

        private Ruta rutaEnEdicion;

        @FXML
        private Label tituloForm;

        @FXML
        private Button guardarButton;

        @FXML
        private GridPane formGrid;

        @FXML
        private TextField nombreField;

        @FXML
        private TextField altitudMaximaField;

        @FXML
        private ComboBox<String> tipoTerrenoCombo;

        @FXML
        private ComboBox<String> dificultadTecnicaCombo;

        @FXML
        private ComboBox<String> dificultadFisicaCombo;

        @FXML
        public void initialize() {
                javafx.application.Platform.runLater(nombreField::requestFocus);

                configurarCampos();
                ResponsiveFormLayout.adapt(formGrid);

                tipoTerrenoCombo.getItems().addAll(ValidadorRuta.TIPOS_TERRENO);
                dificultadTecnicaCombo.getItems().addAll(ValidadorRuta.DIFICULTADES);
                dificultadFisicaCombo.getItems().addAll(ValidadorRuta.DIFICULTADES);

                configurarValidacionesEnTiempoReal();

                rutaEnEdicion = null;
        }

        public void setRutaEnEdicion(Ruta ruta) {
                this.rutaEnEdicion = ruta;
                if (ruta != null) {
                        tituloForm.setText("Editar ruta");
                        guardarButton.setText("Guardar cambios");
                        cargarRuta(ruta);
                } else {
                        tituloForm.setText("Nueva ruta");
                        guardarButton.setText("Crear ruta");
                }
        }

        @FXML
        private void volverListado() throws IOException {

                Stage stage = (Stage) nombreField.getScene().getWindow();

                FXMLLoader loader = new FXMLLoader(
                                getClass().getResource(
                                                "/com/example/fxml/rutas.fxml"));

                Parent root = loader.load();

                NavigationShell.setRoot(stage.getScene(), root);
        }

        @FXML
        private void guardarRuta() throws IOException {

                boolean valido = FormValidation.validateNow(nombreField);
                valido = FormValidation.validateNow(altitudMaximaField) && valido;
                valido = FormValidation.validateNow(tipoTerrenoCombo) && valido;
                valido = FormValidation.validateNow(dificultadTecnicaCombo) && valido;
                valido = FormValidation.validateNow(dificultadFisicaCombo) && valido;
                if (!valido) {
                        mostrarError("Revisa los campos",
                                        "Completa los datos requeridos y corrige los campos marcados.");
                        return;
                }

                DatosRuta datos = new DatosRuta(
                                NombreRuta.normalizar(nombreField.getText()),
                                ValidadorRuta.parsearAltitud(altitudMaximaField.getText()),
                                tipoTerrenoCombo.getValue(),
                                dificultadTecnicaCombo.getValue(),
                                dificultadFisicaCombo.getValue());

                Ruta ruta;
                try {
                        ruta = rutaEnEdicion == null
                                        ? Servicios.rutas().alta(datos)
                                        : Servicios.rutas().editar(rutaEnEdicion.id(), datos);
                } catch (ValidacionException e) {
                        marcarErrores(e);
                        mostrarError("No se pudo guardar", e.getMessage());
                        return;
                } catch (ExcepcionNegocio | IllegalStateException e) {
                        mostrarError("No se pudo guardar", e.getMessage());
                        return;
                }

                var ventana = nombreField.getScene().getWindow();
                if (rutaEnEdicion == null) {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/fxml/itinerario-ruta.fxml"));
                        Parent root = loader.load();
                        loader.<ItinerarioRutaController>getController().cargar(com.example.desktop.model.Ruta.desde(ruta));
                        NavigationShell.setRoot(nombreField.getScene(), root, "itinerario-ruta");
                        ToastNotifications.show(ventana, "Ruta guardada. Complete el Itinerario para usarla en una Salida.", ToastNotifications.Kind.SUCCESS);
                } else {
                        volverListado();
                        ToastNotifications.show(ventana, "Ruta actualizada", ToastNotifications.Kind.SUCCESS);
                }
        }

        private void marcarErrores(ValidacionException e) {
                for (ErrorCampo error : e.errores()) {
                        switch (error.campo()) {
                                case "nombre" -> FormValidation.showError(nombreField, error.mensaje());
                                case "altitudMaxima" -> FormValidation.showError(altitudMaximaField, error.mensaje());
                                case "tipoTerreno" -> FormValidation.showError(tipoTerrenoCombo, error.mensaje());
                                case "dificultadTecnica" -> FormValidation.showError(dificultadTecnicaCombo, error.mensaje());
                                case "dificultadFisica" -> FormValidation.showError(dificultadFisicaCombo, error.mensaje());
                                default -> { }
                        }
                }
        }

        private void marcarError(
                        TextField campo,
                        String mensaje) {

                campo.pseudoClassStateChanged(
                                PseudoClass.getPseudoClass("error"),
                                true);

                Tooltip tooltip = new Tooltip(mensaje);
                tooltip.setShowDelay(Duration.millis(200));
                tooltip.setShowDuration(Duration.seconds(5));
                tooltip.setHideDelay(Duration.millis(100));

                campo.setTooltip(tooltip);
        }

        private void limpiarError(TextField campo) {
                campo.pseudoClassStateChanged(
                                PseudoClass.getPseudoClass("error"),
                                false);
                FormValidation.clearError(campo);
        }

        private void configurarCampos() {
                UnaryOperator<TextFormatter.Change> filtroNombre = change -> {
                        String nuevoTexto = change.getControlNewText();

                        if (nuevoTexto.length() > 100) {
                                marcarError(
                                                nombreField,
                                                "El nombre no puede superar los 100 caracteres.");
                                return null;
                        }

                        if (nuevoTexto.matches("[\\p{L}\\p{N} ]*")) {
                                limpiarError(nombreField);
                                return change;
                        }

                        marcarError(
                                        nombreField,
                                        "El nombre solamente puede contener letras, números y espacios.");
                        return null;
                };

                nombreField.setTextFormatter(
                                new TextFormatter<>(filtroNombre));

                nombreField.setTooltip(
                                new Tooltip(
                                                "Ingresa el nombre de la ruta."));

                configurarCampoNumerico(
                                altitudMaximaField,
                                "Ejemplo: 1250");
        }

        private void configurarValidacionesEnTiempoReal() {
                // Los mensajes vienen del validador de core; la pantalla solo los muestra junto a cada campo.
                FormValidation.watch(nombreField, nombreField.textProperty(), () -> errorDe("nombre"));
                FormValidation.watch(altitudMaximaField, altitudMaximaField.textProperty(), () -> errorDe("altitudMaxima"));
                FormValidation.watch(tipoTerrenoCombo, tipoTerrenoCombo.valueProperty(), () -> errorDe("tipoTerreno"));
                FormValidation.watch(dificultadTecnicaCombo, dificultadTecnicaCombo.valueProperty(),
                                () -> errorDe("dificultadTecnica"));
                FormValidation.watch(dificultadFisicaCombo, dificultadFisicaCombo.valueProperty(),
                                () -> errorDe("dificultadFisica"));
        }

        private String errorDe(String campo) {
                return ValidadorRuta.validar(nombreField.getText(), altitudMaximaField.getText(),
                                                tipoTerrenoCombo.getValue(), dificultadTecnicaCombo.getValue(),
                                                dificultadFisicaCombo.getValue()).stream()
                                .filter(e -> e.campo().equals(campo))
                                .map(ErrorCampo::mensaje)
                                .findFirst().orElse(null);
        }

        private void configurarCampoNumerico(
                        TextField campo,
                        String textoAyuda) {

                UnaryOperator<TextFormatter.Change> filtro = change -> {
                        String nuevoTexto = change.getControlNewText();

                        if (nuevoTexto.isEmpty()) {
                                limpiarError(campo);
                                return change;
                        }

                        if (nuevoTexto.matches(
                                        "-?\\d*[\\.,]?\\d*")) {
                                limpiarError(campo);
                                return change;
                        }

                        marcarError(
                                        campo,
                                        "Ingrese solamente un valor numérico válido.");

                        return null;
                };

                campo.setTextFormatter(
                                new TextFormatter<>(filtro));

                campo.setTooltip(
                                new Tooltip(textoAyuda));
        }

        private void cargarRuta(Ruta ruta) {

                nombreField.setText(
                                ruta.nombre());

                altitudMaximaField.setText(
                                formatearNumero(
                                                ruta.altitudMaxima()));

                tipoTerrenoCombo.setValue(
                                ruta.tipoTerreno());

                dificultadTecnicaCombo.setValue(
                                ruta.dificultadTecnica());

                dificultadFisicaCombo.setValue(
                                ruta.dificultadFisica());
        }

        private String formatearNumero(double valor) {
                return String.valueOf(valor);
        }

        private void mostrarError(
                        String titulo,
                        String mensaje) {

                Alert alerta = new Alert(Alert.AlertType.ERROR);

                alerta.setTitle(titulo);
                alerta.setHeaderText(null);
                alerta.setContentText(mensaje);
                AlertUtils.showAndWait(alerta, nombreField);
        }

}
