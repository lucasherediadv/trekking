package com.example.desktop.controller;

import com.example.desktop.dao.RutaDAO;
import com.example.desktop.model.Ruta;
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

        private final RutaDAO rutaDAO = new RutaDAO();

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

                tipoTerrenoCombo.getItems().addAll(
                                "Rocoso",
                                "Boscoso",
                                "Sendero",
                                "Mixto");

                dificultadTecnicaCombo.getItems().addAll(
                                "Baja",
                                "Media",
                                "Alta");

                dificultadFisicaCombo.getItems().addAll(
                                "Baja",
                                "Media",
                                "Alta");

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

                FormValidation.validateNow(nombreField);
                FormValidation.validateNow(altitudMaximaField);
                FormValidation.validateNow(tipoTerrenoCombo);
                FormValidation.validateNow(dificultadTecnicaCombo);
                FormValidation.validateNow(dificultadFisicaCombo);

                String nombre = Ruta.normalizarNombre(
                                nombreField.getText());

                if (!validarNombre(nombre)) {
                        return;
                }

                if (!validarNombreDuplicado(nombre)) {
                        return;
                }

                if (tipoTerrenoCombo.getValue() == null) {
                        mostrarError(
                                        "Tipo de terreno",
                                        "Selecciona un tipo de terreno.");
                        return;
                }

                if (dificultadTecnicaCombo.getValue() == null) {
                        mostrarError(
                                        "Dificultad técnica",
                                        "Selecciona una dificultad técnica.");
                        return;
                }

                if (dificultadFisicaCombo.getValue() == null) {
                        mostrarError(
                                        "Dificultad física",
                                        "Selecciona una dificultad física.");
                        return;
                }

                Double altitudMaxima = obtenerAltitud();

                if (altitudMaxima == null) {
                        return;
                }

                String tipoTerreno = tipoTerrenoCombo.getValue();
                String dificultadTecnica = dificultadTecnicaCombo.getValue();
                String dificultadFisica = dificultadFisicaCombo.getValue();

                Ruta ruta = rutaEnEdicion == null
                                ? new Ruta(
                                                nombre,
                                                altitudMaxima,
                                                tipoTerreno,
                                                dificultadTecnica,
                                                dificultadFisica)
                                : new Ruta(
                                                rutaEnEdicion.getId(),
                                                nombre,
                                                altitudMaxima,
                                                tipoTerreno,
                                                dificultadTecnica,
                                                dificultadFisica);

                int rutaId;

                if (rutaEnEdicion == null) {

                        rutaId = rutaDAO.insertar(ruta);

                        if (rutaId == -1) {
                                mostrarError(
                                                "Error",
                                                "No se pudo guardar la ruta.");
                                return;
                        }

                } else {

                        boolean rutaActualizada = rutaDAO.actualizar(ruta);

                        if (!rutaActualizada) {
                                mostrarError(
                                                "Error",
                                                "No se pudo actualizar la ruta.");
                                return;
                        }

                        rutaId = ruta.getId();
                }

                var ventana = nombreField.getScene().getWindow();
                if (rutaEnEdicion == null) {
                        ruta.setId(rutaId);
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/fxml/itinerario-ruta.fxml"));
                        Parent root = loader.load();
                        loader.<ItinerarioRutaController>getController().cargar(ruta);
                        NavigationShell.setRoot(nombreField.getScene(), root, "itinerario-ruta");
                        ToastNotifications.show(ventana, "Ruta guardada. Complete el Itinerario para usarla en una Salida.", ToastNotifications.Kind.SUCCESS);
                } else {
                        volverListado();
                        ToastNotifications.show(ventana, "Ruta actualizada", ToastNotifications.Kind.SUCCESS);
                }
        }

        private boolean validarNombre(String nombre) {

                if (nombre.isBlank()) {

                        mostrarError(
                                        "Nombre de ruta",
                                        "El nombre de la ruta es obligatorio.");

                        nombreField.requestFocus();

                        return false;
                }

                if (!Ruta.nombreValido(nombre)) {

                        mostrarError(
                                        "Nombre de ruta",
                                        "El nombre solamente puede contener letras, "
                                                        + "números y espacios.");

                        nombreField.requestFocus();

                        return false;
                }

                return true;
        }

        private boolean validarNombreDuplicado(String nombre) {

                int idExcluido = rutaEnEdicion == null
                                ? -1
                                : rutaEnEdicion.getId();

                if (rutaDAO.existeNombre(nombre, idExcluido)) {

                        FormValidation.showError(
                                        nombreField,
                                        "Ya existe una ruta con ese nombre.");
                        mostrarError(
                                        "Nombre duplicado",
                                        "Ya existe una ruta con ese nombre.");

                        nombreField.requestFocus();

                        return false;
                }

                return true;
        }

        private Double obtenerAltitud() {

                String texto = altitudMaximaField.getText().trim();

                if (texto.isEmpty()) {

                        mostrarError(
                                        "Altitud máxima",
                                        "La altitud máxima es obligatoria.");

                        altitudMaximaField.requestFocus();

                        return null;
                }

                try {

                        double valor = Double.parseDouble(
                                        texto.replace(',', '.'));

                        if (!Double.isFinite(valor)) {

                                mostrarError(
                                                "Altitud máxima",
                                                "Ingresa un valor numérico válido.");

                                altitudMaximaField.requestFocus();

                                return null;
                        }

                        if (valor < 0) {

                                mostrarError(
                                                "Altitud máxima",
                                                "La altitud no puede ser negativa.");

                                altitudMaximaField.requestFocus();

                                return null;
                        }

                        return valor;

                } catch (NumberFormatException e) {

                        mostrarError(
                                        "Altitud máxima",
                                        "Ingresa un valor numérico válido.");

                        altitudMaximaField.requestFocus();

                        return null;
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
                FormValidation.watch(nombreField, nombreField.textProperty(), () -> {
                        String nombre = Ruta.formatearNombre(nombreField.getText());
                        if (nombre.isBlank()) {
                                return "El nombre de la ruta es obligatorio.";
                        }
                        if (!Ruta.nombreValido(nombre)) {
                                return "El nombre solo puede contener letras, numeros y espacios.";
                        }
                        return null;
                });
                FormValidation.watch(
                                altitudMaximaField,
                                altitudMaximaField.textProperty(),
                                this::validarAltitudEnTiempoReal);
                FormValidation.watch(tipoTerrenoCombo, tipoTerrenoCombo.valueProperty(),
                                () -> tipoTerrenoCombo.getValue() == null
                                                ? "Selecciona un tipo de terreno."
                                                : null);
                FormValidation.watch(
                                dificultadTecnicaCombo,
                                dificultadTecnicaCombo.valueProperty(),
                                () -> dificultadTecnicaCombo.getValue() == null
                                                ? "Selecciona una dificultad tecnica."
                                                : null);
                FormValidation.watch(
                                dificultadFisicaCombo,
                                dificultadFisicaCombo.valueProperty(),
                                () -> dificultadFisicaCombo.getValue() == null
                                                ? "Selecciona una dificultad fisica."
                                                : null);
        }

        private String validarAltitudEnTiempoReal() {
                String texto = altitudMaximaField.getText().trim();
                if (texto.isEmpty()) {
                        return "La altitud maxima es obligatoria.";
                }
                try {
                        double valor = Double.parseDouble(texto.replace(',', '.'));
                        if (!Double.isFinite(valor) || valor < 0) {
                                return "La altitud debe ser un numero valido mayor o igual a cero.";
                        }
                        return null;
                } catch (NumberFormatException e) {
                        return "Ingresa un valor numerico valido.";
                }
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
                                ruta.getNombre());

                altitudMaximaField.setText(
                                formatearNumero(
                                                ruta.getAltitudMaxima()));

                tipoTerrenoCombo.setValue(
                                ruta.getTipoTerreno());

                dificultadTecnicaCombo.setValue(
                                ruta.getDificultadTecnica());

                dificultadFisicaCombo.setValue(
                                ruta.getDificultadFisica());
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
