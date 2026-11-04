package com.example.desktop.controller;

import com.example.core.excepcion.ErrorCampo;
import com.example.core.itinerario.Checkpoint;
import com.example.core.itinerario.ValidadorCheckpoint;
import com.example.desktop.util.AlertUtils;
import com.example.desktop.util.FormValidation;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Window;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Formulario en-contenedor para crear o editar un Checkpoint del Itinerario pendiente de una Ruta. */
public class CheckpointFormController implements Initializable {

    private static final String DECIMAL_PATTERN = "-?\\d*([.,]\\d*)?";
    private static final String HORA_PATTERN = "\\d{0,2}:?\\d{0,2}";

    @FXML private Label tituloLabel;
    @FXML private Label subtituloLabel;
    @FXML private Label ayudaAltitud;
    @FXML private TextField txtNombre;
    @FXML private TextField txtHora;
    @FXML private TextField txtLatitud;
    @FXML private TextField txtLongitud;
    @FXML private TextField txtAltitud;
    @FXML private TextArea txtDescripcion;
    @FXML private Button guardarButton;

    private Checkpoint checkpointEnEdicion;
    private boolean modificandoCampos;
    private boolean formularioModificado;
    private double altitudMaxima;
    private Consumer<Checkpoint> aceptarCheckpoint;
    private Runnable cancelarItinerario;
    private BooleanSupplier confirmarSalidaItinerario;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        txtLatitud.setTextFormatter(createDecimalTextFormatter());
        txtLongitud.setTextFormatter(createDecimalTextFormatter());
        txtAltitud.setTextFormatter(createDecimalTextFormatter());
        txtHora.setTextFormatter(createHoraTextFormatter());

        FormValidation.watch(txtNombre, txtNombre.textProperty(), () -> errorDe("nombre"));
        FormValidation.watch(txtHora, txtHora.textProperty(), () -> errorDe("hora"));
        FormValidation.watch(txtLatitud, txtLatitud.textProperty(), () -> errorDe("latitud"));
        FormValidation.watch(txtLongitud, txtLongitud.textProperty(), () -> errorDe("longitud"));
        FormValidation.watch(txtAltitud, txtAltitud.textProperty(), () -> errorDe("altitud"));

        txtNombre.textProperty().addListener((obs, oldValue, newValue) -> marcarModificado());
        txtHora.textProperty().addListener((obs, oldValue, newValue) -> marcarModificado());
        txtLatitud.textProperty().addListener((obs, oldValue, newValue) -> marcarModificado());
        txtLongitud.textProperty().addListener((obs, oldValue, newValue) -> marcarModificado());
        txtAltitud.textProperty().addListener((obs, oldValue, newValue) -> marcarModificado());
        txtDescripcion.textProperty().addListener((obs, oldValue, newValue) -> marcarModificado());

        Platform.runLater(txtNombre::requestFocus);
    }

    /** Abre el formulario como parte de la edición pendiente del Itinerario. */
    public void cargarCheckpointParaItinerario(
            Checkpoint checkpoint,
            double maximoRuta,
            Consumer<Checkpoint> aceptar,
            Runnable cancelar,
            BooleanSupplier confirmarSalida) {
        checkpointEnEdicion = checkpoint;
        altitudMaxima = maximoRuta;
        aceptarCheckpoint = aceptar;
        cancelarItinerario = cancelar;
        confirmarSalidaItinerario = confirmarSalida;
        ayudaAltitud.setText("Entre 0 y " + maximoRuta + " m, según la altitud máxima de la Ruta.");
        configurarTitulo(checkpoint != null);
        cargarCampos(checkpoint);
    }

    private void configurarTitulo(boolean edicion) {
        tituloLabel.setText(edicion ? "Editar Checkpoint" : "Nuevo Checkpoint");
        subtituloLabel.setText(edicion
                ? "Actualiza los datos de este punto de control de la Ruta."
                : "Completa los datos del punto de control de la Ruta.");
        guardarButton.setText(edicion ? "Guardar cambios" : "Agregar checkpoint");
    }

    private void cargarCampos(Checkpoint checkpoint) {
        modificandoCampos = true;
        txtNombre.setText(checkpoint == null ? "" : checkpoint.nombre());
        txtHora.setText(checkpoint == null ? "" : checkpoint.hora());
        txtLatitud.setText(checkpoint == null ? "" : String.valueOf(checkpoint.latitud()));
        txtLongitud.setText(checkpoint == null ? "" : String.valueOf(checkpoint.longitud()));
        txtAltitud.setText(checkpoint == null ? "0" : String.valueOf(checkpoint.altitud()));
        txtDescripcion.setText(checkpoint == null ? "" : checkpoint.descripcion());
        modificandoCampos = false;
        formularioModificado = false;
        Platform.runLater(txtNombre::requestFocus);
    }

    private void marcarModificado() {
        if (!modificandoCampos) formularioModificado = true;
    }

    @FXML
    private void guardarCheckpoint() {
        guardarEnEdicionPendiente();
    }

    private boolean guardarEnEdicionPendiente() {
        if (!validateInput()) return false;
        if (aceptarCheckpoint != null) aceptarCheckpoint.accept(crearCheckpoint());
        return true;
    }

    private Checkpoint crearCheckpoint() {
        return new Checkpoint(
                checkpointEnEdicion == null ? 0L : checkpointEnEdicion.id(),
                txtNombre.getText().trim(),
                txtHora.getText().trim(),
                ValidadorCheckpoint.parsearDecimal(txtLatitud.getText()),
                ValidadorCheckpoint.parsearDecimal(txtLongitud.getText()),
                ValidadorCheckpoint.parsearDecimal(txtAltitud.getText()),
                txtDescripcion.getText().trim());
    }

    @FXML
    private void cancelar() {
        if (cancelarItinerario != null) cancelarItinerario.run();
    }

    /** Protege la navegación de la shell ante datos de formulario pendientes. */
    public boolean confirmarNavegacion() {
        if (!formularioModificado) {
            return confirmarSalidaItinerario == null || confirmarSalidaItinerario.getAsBoolean();
        }

        ButtonType guardar = new ButtonType("Guardar checkpoint");
        ButtonType descartar = new ButtonType("Descartar");
        ButtonType seguir = new ButtonType("Seguir editando", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION,
                "Hay cambios sin confirmar en el Checkpoint.", guardar, descartar, seguir);
        alerta.setTitle("Cambios sin guardar");
        alerta.setHeaderText("¿Qué quieres hacer?");
        ButtonType respuesta = AlertUtils.showAndWait(alerta, getWindow()).orElse(seguir);
        if (respuesta == seguir) return false;

        if (respuesta == guardar) {
            if (!guardarEnEdicionPendiente()) return false;
        } else if (cancelarItinerario != null) {
            cancelarItinerario.run();
        }
        return confirmarSalidaItinerario == null || confirmarSalidaItinerario.getAsBoolean();
    }

    public boolean validateInput() {
        boolean valido = true;
        valido = FormValidation.validateNow(txtNombre) && valido;
        valido = FormValidation.validateNow(txtHora) && valido;
        valido = FormValidation.validateNow(txtLatitud) && valido;
        valido = FormValidation.validateNow(txtLongitud) && valido;
        valido = FormValidation.validateNow(txtAltitud) && valido;
        return valido;
    }

    /** Validación en vivo con el validador de core: error del campo indicado, o null si es válido. */
    private String errorDe(String campo) {
        return ValidadorCheckpoint.validar(txtNombre.getText(), txtHora.getText(), txtLatitud.getText(),
                        txtLongitud.getText(), txtAltitud.getText(), altitudMaxima).stream()
                .filter(e -> e.campo().equals(campo)).map(ErrorCampo::mensaje).findFirst().orElse(null);
    }

    private TextFormatter<String> createDecimalTextFormatter() {
        return new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            return newText.isEmpty() || newText.matches(DECIMAL_PATTERN) ? change : null;
        });
    }

    private TextFormatter<String> createHoraTextFormatter() {
        return new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            return newText.isEmpty() || newText.matches(HORA_PATTERN) ? change : null;
        });
    }

    private Window getWindow() {
        return txtNombre.getScene() == null ? null : txtNombre.getScene().getWindow();
    }
}
