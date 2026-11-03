package com.example.desktop.controller;

import com.example.desktop.dao.CheckpointDAO;
import com.example.desktop.model.Checkpoint;
import com.example.desktop.util.AlertUtils;
import com.example.desktop.util.FormValidation;
import com.example.desktop.util.NavigationShell;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Formulario en-contenedor para crear o editar un Checkpoint. */
public class CheckpointFormController implements Initializable {

    private static final Logger LOGGER =
            Logger.getLogger(CheckpointFormController.class.getName());
    private static final double LAT_MIN = -90.0;
    private static final double LAT_MAX = 90.0;
    private static final double LON_MIN = -180.0;
    private static final double LON_MAX = 180.0;
    private static final DateTimeFormatter HORA_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
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

    private final CheckpointDAO checkpointDAO = new CheckpointDAO();
    private Checkpoint checkpointEnEdicion;
    private boolean modoItinerario;
    private boolean modificandoCampos;
    private boolean formularioModificado;
    private double altitudMaxima = Double.POSITIVE_INFINITY;
    private Consumer<Checkpoint> aceptarCheckpoint;
    private Runnable cancelarItinerario;
    private BooleanSupplier confirmarSalidaItinerario;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        txtLatitud.setTextFormatter(createDecimalTextFormatter());
        txtLongitud.setTextFormatter(createDecimalTextFormatter());
        txtAltitud.setTextFormatter(createDecimalTextFormatter());
        txtHora.setTextFormatter(createHoraTextFormatter());

        FormValidation.watch(txtNombre, txtNombre.textProperty(), this::validarNombre);
        FormValidation.watch(txtHora, txtHora.textProperty(), this::validarHora);
        FormValidation.watch(txtLatitud, txtLatitud.textProperty(), this::validarLatitud);
        FormValidation.watch(txtLongitud, txtLongitud.textProperty(), this::validarLongitud);
        FormValidation.watch(txtAltitud, txtAltitud.textProperty(), this::validarAltitud);

        txtNombre.textProperty().addListener((obs, oldValue, newValue) -> marcarModificado());
        txtHora.textProperty().addListener((obs, oldValue, newValue) -> marcarModificado());
        txtLatitud.textProperty().addListener((obs, oldValue, newValue) -> marcarModificado());
        txtLongitud.textProperty().addListener((obs, oldValue, newValue) -> marcarModificado());
        txtAltitud.textProperty().addListener((obs, oldValue, newValue) -> marcarModificado());
        txtDescripcion.textProperty().addListener((obs, oldValue, newValue) -> marcarModificado());

        Platform.runLater(txtNombre::requestFocus);
    }

    /** Mantiene el modo de edición directa usado por el módulo de Checkpoints. */
    public void cargarCheckpoint(Checkpoint checkpoint) {
        checkpointEnEdicion = checkpoint;
        modoItinerario = false;
        configurarTitulo(checkpoint != null);
        cargarCampos(checkpoint);
    }

    /** Abre el formulario como parte de la edición pendiente del Itinerario. */
    public void cargarCheckpointParaItinerario(
            Checkpoint checkpoint,
            double maximoRuta,
            Consumer<Checkpoint> aceptar,
            Runnable cancelar,
            BooleanSupplier confirmarSalida) {
        checkpointEnEdicion = checkpoint;
        modoItinerario = true;
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
        txtNombre.setText(checkpoint == null ? "" : checkpoint.getNombre());
        txtHora.setText(checkpoint == null ? "" : checkpoint.getHora());
        txtLatitud.setText(checkpoint == null ? "" : String.valueOf(checkpoint.getLatitud()));
        txtLongitud.setText(checkpoint == null ? "" : String.valueOf(checkpoint.getLongitud()));
        txtAltitud.setText(checkpoint == null ? "0" : String.valueOf(checkpoint.getAltitud()));
        txtDescripcion.setText(checkpoint == null ? "" : checkpoint.getDescripcion());
        modificandoCampos = false;
        formularioModificado = false;
        Platform.runLater(txtNombre::requestFocus);
    }

    private void marcarModificado() {
        if (!modificandoCampos) formularioModificado = true;
    }

    @FXML
    private void guardarCheckpoint() {
        if (modoItinerario) {
            guardarEnEdicionPendiente();
            return;
        }
        if (!validateInput()) return;

        Checkpoint resultado = crearCheckpoint();
        try {
            if (checkpointEnEdicion == null) {
                long id = checkpointDAO.insertar(resultado);
                if (id == -1) {
                    AlertUtils.showError(getWindow(), "Error al guardar",
                            "No se pudo confirmar el guardado del Checkpoint.");
                    return;
                }
            } else if (checkpointDAO.actualizar(resultado) == 0) {
                AlertUtils.showError(getWindow(), "Error al guardar",
                        "No se pudo actualizar el Checkpoint.");
                return;
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al guardar el Checkpoint", ex);
            AlertUtils.showError(getWindow(), "Error al guardar",
                    "No se pudo guardar el Checkpoint en la base de datos.\n" + ex.getMessage());
            return;
        }

        mostrarConfirmacionExito();
        volverListado();
    }

    private boolean guardarEnEdicionPendiente() {
        if (!validateInput()) return false;
        if (aceptarCheckpoint != null) aceptarCheckpoint.accept(crearCheckpoint());
        return true;
    }

    private Checkpoint crearCheckpoint() {
        return new Checkpoint(
                checkpointEnEdicion == null ? 0L : checkpointEnEdicion.getId(),
                txtNombre.getText().trim(),
                txtHora.getText().trim(),
                parseDecimal(txtLatitud.getText()),
                parseDecimal(txtLongitud.getText()),
                parseDecimal(txtAltitud.getText()),
                txtDescripcion.getText().trim());
    }

    @FXML
    private void cancelar() {
        if (modoItinerario) {
            if (cancelarItinerario != null) cancelarItinerario.run();
        } else {
            volverListado();
        }
    }

    /** Protege la navegación de la shell ante datos de formulario pendientes. */
    public boolean confirmarNavegacion() {
        if (!modoItinerario) return true;
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

    private void volverListado() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/fxml/checkpoints.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) txtNombre.getScene().getWindow();
            NavigationShell.setNavigationGuard(stage.getScene(), null);
            NavigationShell.setRoot(stage.getScene(), root);
            stage.setTitle("Checkpoints");
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error al volver al listado de Checkpoints", ex);
            AlertUtils.showError(getWindow(), "Error",
                    "No se pudo volver al listado de Checkpoints.");
        }
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

    private String validarNombre() {
        return txtNombre.getText().isBlank() ? "Ingresa el nombre del Checkpoint." : null;
    }

    private String validarHora() {
        String hora = txtHora.getText().trim();
        if (!hora.matches("(?:[01]\\d|2[0-3]):[0-5]\\d")) {
            return "Usa el formato de 24 horas HH:mm, por ejemplo 14:30.";
        }
        try {
            LocalTime.parse(hora, HORA_FORMATTER);
            return null;
        } catch (DateTimeParseException ex) {
            return "Usa el formato de 24 horas HH:mm, por ejemplo 14:30.";
        }
    }

    private String validarLatitud() {
        return validarRango(txtLatitud.getText(), LAT_MIN, LAT_MAX,
                "La latitud debe estar entre -90 y 90.");
    }

    private String validarLongitud() {
        return validarRango(txtLongitud.getText(), LON_MIN, LON_MAX,
                "La longitud debe estar entre -180 y 180.");
    }

    private String validarAltitud() {
        String error = validarRango(txtAltitud.getText(), 0, altitudMaxima,
                modoItinerario
                        ? "La altitud debe estar entre 0 y " + altitudMaxima + " m."
                        : "La altitud debe ser mayor o igual a 0 m.");
        return error;
    }

    private String validarRango(String texto, double minimo, double maximo, String mensaje) {
        if (texto == null || texto.isBlank()) return "Este campo es obligatorio.";
        try {
            double numero = parseDecimal(texto);
            return Double.isFinite(numero) && numero >= minimo && numero <= maximo
                    ? null : mensaje;
        } catch (NumberFormatException ex) {
            return "Ingresa un número decimal válido.";
        }
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

    private void mostrarConfirmacionExito() {
        boolean esAlta = checkpointEnEdicion == null;
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(esAlta ? "Checkpoint creado" : "Checkpoint actualizado");
        alerta.setHeaderText(null);
        alerta.setContentText(esAlta
                ? "El Checkpoint se creó correctamente."
                : "El Checkpoint se actualizó correctamente.");
        AlertUtils.showAndWait(alerta, getWindow());
    }

    private Window getWindow() {
        return txtNombre.getScene() == null ? null : txtNombre.getScene().getWindow();
    }

    private static double parseDecimal(String raw) {
        return Double.parseDouble(raw.trim().replace(',', '.'));
    }
}
