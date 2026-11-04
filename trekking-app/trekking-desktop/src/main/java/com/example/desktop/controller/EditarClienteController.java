package com.example.desktop.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import com.example.core.cliente.Cliente;
import com.example.core.cliente.DatosCliente;
import com.example.core.excepcion.ErrorCampo;
import com.example.core.excepcion.ExcepcionNegocio;
import com.example.core.excepcion.ValidacionException;
import com.example.desktop.Servicios;
import com.example.desktop.dao.FichaMedicaDAO;
import com.example.desktop.util.FormValidation;
import com.example.desktop.util.ResponsiveFormLayout;
import com.example.desktop.util.NavigationShell;
import com.example.desktop.util.AlertUtils;
import com.example.desktop.util.FechaFormato;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class EditarClienteController {

    private static final long TAMANO_MAXIMO_FICHA = 2L * 1024 * 1024;

    @FXML
    private TextField dniField;

    @FXML
    private TextField nombreField;

    @FXML
    private TextField apellidoField;

    @FXML
    private DatePicker fechaNacimientoPicker;

    @FXML
    private TextField emailField;

    @FXML
    private TextField telefonoField;

    @FXML
    private ComboBox<String> sexoComboBox;

    @FXML
    private CheckBox activoCheckBox;

    @FXML
    private TextField contactoNombreField;

    @FXML
    private TextField contactoTelefonoField;

    @FXML
    private TextField contactoRelacionField;

    @FXML
    private CheckBox autorizacionMenoresCheckBox;

    @FXML
    private TextField tutorNombreField;

    @FXML
    private TextField tutorApellidoField;

    @FXML
    private TextField tutorDniField;

    @FXML
    private TextField tutorTelefonoField;

    @FXML
    private Label tutorNombreLabel;

    @FXML
    private Label tutorApellidoLabel;

    @FXML
    private Label tutorDniLabel;

    @FXML
    private Label tutorTelefonoLabel;

    @FXML
    private Button guardarButton;

    @FXML private GridPane datosPersonalesGrid;
    @FXML private GridPane contactoGrid;
    @FXML private VBox tutorNombreGroup;
    @FXML private VBox tutorApellidoGroup;
    @FXML private VBox tutorDniGroup;
    @FXML private VBox tutorTelefonoGroup;

    @FXML
    private Label fichaMedicaLabel;

    @FXML
    private Button quitarFichaButton;

    private final Cliente cliente;
    private final FichaMedicaDAO fichaMedicaDAO = new FichaMedicaDAO();
    private File fichaMedicaSeleccionada;
    private boolean eliminarFichaMedica;

    public EditarClienteController(Cliente cliente) {
        this.cliente = cliente;
    }

    @FXML
    private void initialize() {
        guardarButton.setText("Guardar cambios");
        ResponsiveFormLayout.adapt(datosPersonalesGrid);
        ResponsiveFormLayout.adapt(contactoGrid);
        FechaFormato.configurar(fechaNacimientoPicker);
        sexoComboBox.getItems().addAll(
                "Masculino",
                "Femenino",
                "No binario",
                "Otro",
                "Prefiero no decir");
        javafx.application.Platform.runLater(nombreField::requestFocus);

        dniField.setText(textoSeguro(cliente.dni()));
        nombreField.setText(textoSeguro(cliente.nombre()));
        apellidoField.setText(textoSeguro(cliente.apellido()));
        fechaNacimientoPicker.setValue(cliente.fechaNacimiento());
        emailField.setText(textoSeguro(cliente.email()));
        telefonoField.setText(textoSeguro(cliente.telefono()));
        sexoComboBox.setValue(cliente.sexo());
        contactoNombreField.setText(
                textoSeguro(cliente.contactoEmergenciaNombre()));
        contactoTelefonoField.setText(
                textoSeguro(cliente.contactoEmergenciaTelefono()));
        contactoRelacionField.setText(
                textoSeguro(cliente.contactoEmergenciaRelacion()));
        autorizacionMenoresCheckBox.setSelected(
                cliente.autorizacionMenores());
        tutorNombreField.setText(textoSeguro(cliente.tutorNombre()));
        tutorApellidoField.setText(textoSeguro(cliente.tutorApellido()));
        tutorDniField.setText(textoSeguro(cliente.tutorDni()));
        tutorTelefonoField.setText(textoSeguro(cliente.tutorTelefono()));
        actualizarCamposTutor();
        activoCheckBox.setSelected(cliente.activo());

        com.example.desktop.model.FichaMedica fichaMedica = fichaMedicaDAO.obtener(cliente.id());

        boolean existeFichaEnDisco = Files.isRegularFile(
                Path.of("fichas_medicas", cliente.dni() + ".pdf"));

        boolean existeFicha = fichaMedica != null || existeFichaEnDisco;

        if (fichaMedica != null) {
            fichaMedicaLabel.setText(fichaMedica.getNombreArchivo());
        } else if (existeFichaEnDisco) {
            fichaMedicaLabel.setText(cliente.dni() + ".pdf");
        } else {
            fichaMedicaLabel.setText("Sin ficha medica");
        }

        quitarFichaButton.setVisible(existeFicha);
        quitarFichaButton.setManaged(existeFicha);

        configurarValidacionesEnTiempoReal();
    }

    private void configurarValidacionesEnTiempoReal() {
        FormValidation.watch(dniField, dniField.textProperty(), () -> errorDe("dni"));
        FormValidation.watch(nombreField, nombreField.textProperty(), () -> errorDe("nombre"));
        FormValidation.watch(apellidoField, apellidoField.textProperty(), () -> errorDe("apellido"));
        FormValidation.watch(fechaNacimientoPicker,
                fechaNacimientoPicker.getEditor().textProperty(),
                this::validarFechaNacimientoEnTiempoReal);
        FormValidation.watch(emailField, emailField.textProperty(), () -> errorDe("email"));
        FormValidation.watch(telefonoField, telefonoField.textProperty(), () -> errorDe("telefono"));
        FormValidation.watch(sexoComboBox, sexoComboBox.valueProperty(), () -> errorDe("sexo"));
        FormValidation.watch(contactoNombreField, contactoNombreField.textProperty(),
                () -> errorDe("contactoEmergenciaNombre"));
        FormValidation.watch(contactoTelefonoField, contactoTelefonoField.textProperty(),
                () -> errorDe("contactoEmergenciaTelefono"));
        FormValidation.watch(contactoRelacionField, contactoRelacionField.textProperty(),
                () -> errorDe("contactoEmergenciaRelacion"));
        FormValidation.watch(autorizacionMenoresCheckBox, autorizacionMenoresCheckBox.selectedProperty(),
                () -> errorDe("autorizacionMenores"));
        FormValidation.watch(tutorNombreField, tutorNombreField.textProperty(), () -> errorDe("tutorNombre"));
        FormValidation.watch(tutorApellidoField, tutorApellidoField.textProperty(), () -> errorDe("tutorApellido"));
        FormValidation.watch(tutorDniField, tutorDniField.textProperty(), () -> errorDe("tutorDni"));
        FormValidation.watch(tutorTelefonoField, tutorTelefonoField.textProperty(), () -> errorDe("tutorTelefono"));

        contactoNombreField.textProperty().addListener(
                (observable, anterior, actual) -> validarContactoVinculado());
        contactoTelefonoField.textProperty().addListener(
                (observable, anterior, actual) -> validarContactoVinculado());
        contactoRelacionField.textProperty().addListener(
                (observable, anterior, actual) -> validarContactoVinculado());
        fechaNacimientoPicker.valueProperty().addListener(
                (observable, anterior, actual) -> {
                    FormValidation.validateNow(fechaNacimientoPicker);
                    validarTutorVinculado();
                });
        autorizacionMenoresCheckBox.selectedProperty().addListener(
                (observable, anterior, actual) -> validarTutorVinculado());
    }

    private void validarContactoVinculado() {
        FormValidation.validateNow(contactoNombreField);
        FormValidation.validateNow(contactoTelefonoField);
        FormValidation.validateNow(contactoRelacionField);
    }

    private void validarTutorVinculado() {
        FormValidation.validateNow(autorizacionMenoresCheckBox);
        FormValidation.validateNow(tutorNombreField);
        FormValidation.validateNow(tutorApellidoField);
        FormValidation.validateNow(tutorDniField);
        FormValidation.validateNow(tutorTelefonoField);
    }

    private String validarFechaNacimientoEnTiempoReal() {
        String texto = fechaNacimientoPicker.getEditor().getText().trim();
        if (texto.isEmpty()) {
            return "La fecha de nacimiento es obligatoria.";
        }
        try {
            fechaNacimientoPicker.getConverter().fromString(texto);
            return errorDe("fechaNacimiento");
        } catch (DateTimeParseException e) {
            return "Ingresa una fecha de nacimiento valida.";
        }
    }

    private DatosCliente leerDatos() {
        return new DatosCliente(textoSeguro(dniField.getText()).trim(), textoSeguro(nombreField.getText()).trim(),
                textoSeguro(apellidoField.getText()).trim(), fechaNacimientoPicker.getValue(),
                textoSeguro(emailField.getText()).trim(), textoSeguro(telefonoField.getText()).trim(),
                sexoComboBox.getValue(), textoSeguro(contactoNombreField.getText()).trim(),
                textoSeguro(contactoTelefonoField.getText()).trim(),
                textoSeguro(contactoRelacionField.getText()).trim(), autorizacionMenoresCheckBox.isSelected(),
                textoSeguro(tutorNombreField.getText()).trim(), textoSeguro(tutorApellidoField.getText()).trim(),
                textoSeguro(tutorDniField.getText()).trim(), textoSeguro(tutorTelefonoField.getText()).trim());
    }

    /** Mensaje de core para un campo; la pantalla solo lo muestra. */
    private String errorDe(String campo) {
        return Servicios.clientes().validar(leerDatos()).stream()
                .filter(e -> e.campo().equals(campo))
                .map(ErrorCampo::mensaje)
                .findFirst().orElse(null);
    }

    private void marcarErrores(ValidacionException e) {
        for (ErrorCampo error : e.errores()) {
            Control control = switch (error.campo()) {
                case "dni" -> dniField;
                case "nombre" -> nombreField;
                case "apellido" -> apellidoField;
                case "fechaNacimiento" -> fechaNacimientoPicker;
                case "email" -> emailField;
                case "telefono" -> telefonoField;
                case "sexo" -> sexoComboBox;
                case "contactoEmergenciaNombre" -> contactoNombreField;
                case "contactoEmergenciaTelefono" -> contactoTelefonoField;
                case "contactoEmergenciaRelacion" -> contactoRelacionField;
                case "autorizacionMenores" -> autorizacionMenoresCheckBox;
                case "tutorNombre" -> tutorNombreField;
                case "tutorApellido" -> tutorApellidoField;
                case "tutorDni" -> tutorDniField;
                case "tutorTelefono" -> tutorTelefonoField;
                default -> null;
            };
            if (control != null) FormValidation.showError(control, error.mensaje());
        }
    }

    @FXML
    private void cambiarAutorizacionMenores() {
        actualizarCamposTutor();
    }

    private void actualizarCamposTutor() {
        boolean mostrar = autorizacionMenoresCheckBox.isSelected();
        VBox[] groups = {tutorNombreGroup, tutorApellidoGroup, tutorDniGroup, tutorTelefonoGroup};
        for (VBox group : groups) {
            group.setVisible(mostrar);
            group.setManaged(mostrar);
        }
        Label[] labels = {
                tutorNombreLabel,
                tutorApellidoLabel,
                tutorDniLabel,
                tutorTelefonoLabel
        };
        TextField[] fields = {
                tutorNombreField,
                tutorApellidoField,
                tutorDniField,
                tutorTelefonoField
        };
        for (Label label : labels) {
            label.setVisible(mostrar);
            label.setManaged(mostrar);
        }
        for (TextField field : fields) {
            field.setVisible(mostrar);
            field.setManaged(mostrar);
        }
    }

    @FXML
    private void seleccionarFichaMedica() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Seleccionar ficha medica");
        selector.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Archivos PDF", "*.pdf", "*.PDF"));
        Window ventana = fichaMedicaLabel.getScene().getWindow();
        File archivo = selector.showOpenDialog(ventana);
        if (archivo == null) {
            return;
        }
        if (archivo.length() > TAMANO_MAXIMO_FICHA
                || !archivo.getName().toLowerCase().endsWith(".pdf")) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Ficha no valida",
                    "Elegi un PDF de hasta 2 MB.");
            return;
        }
        fichaMedicaSeleccionada = archivo;
        eliminarFichaMedica = false;
        fichaMedicaLabel.setText(archivo.getName());
    }

    @FXML
    private void borrarFichaMedica() {
        fichaMedicaSeleccionada = null;
        eliminarFichaMedica = true;
        fichaMedicaLabel.setText("Se eliminara al guardar");
    }

    @FXML
    private void cancelarEdicion() {
        try {
            mostrarListaClientes();
        } catch (IOException e) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "No se pudo abrir la lista",
                    e.getMessage());
        }
    }

    @FXML
    private void guardarCambios() {
        validarFormularioVisualmente();
        DatosCliente datos = leerDatos();
        if (!Servicios.clientes().validar(datos).isEmpty()) {
            mostrarAlerta(Alert.AlertType.ERROR, "Revisa los datos",
                    "Corrige los campos marcados antes de guardar.");
            return;
        }
        String dni = datos.dni();

        byte[] fichaMedica = null;
        String nombreFichaMedica = null;
        if (fichaMedicaSeleccionada != null) {
            try {
                fichaMedica = Files.readAllBytes(
                        fichaMedicaSeleccionada.toPath());
            } catch (IOException e) {
                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "No se pudo leer el archivo",
                        e.getMessage());
                return;
            }
            if (!esPdfValido(fichaMedica)) {
                mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Ficha no valida",
                        "Elegi un PDF de hasta 2 MB.");
                return;
            }
            nombreFichaMedica = fichaMedicaSeleccionada.getName();
        }

        try {
            Servicios.clientes().editar(cliente.id(), datos);
            if (activoCheckBox.isSelected() != cliente.activo()) {
                if (activoCheckBox.isSelected()) Servicios.clientes().reactivar(cliente.id());
                else Servicios.clientes().darDeBaja(cliente.id());
            }
        } catch (ValidacionException e) {
            marcarErrores(e);
            mostrarAlerta(Alert.AlertType.ERROR, "No se pudo guardar", e.getMessage());
            return;
        } catch (ExcepcionNegocio | IllegalStateException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "No se pudo guardar", e.getMessage());
            return;
        }

        try {
            if (fichaMedicaSeleccionada != null) {
                fichaMedicaDAO.guardar(cliente.id(), fichaMedica, nombreFichaMedica);
            } else if (eliminarFichaMedica) {
                fichaMedicaDAO.borrar(cliente.id());
            }
        } catch (IllegalStateException e) {
            mostrarAlerta(Alert.AlertType.WARNING, "Ficha no actualizada",
                    "Los datos se guardaron, pero no se pudo actualizar la ficha medica: " + e.getMessage());
        }

        try {
            if (fichaMedicaSeleccionada != null) {
                FichaMedica.guardarEnCarpeta(dni, fichaMedica);
                if (!cliente.dni().equals(dni)) {
                    FichaMedica.borrarDeCarpeta(cliente.dni());
                }
            } else if (eliminarFichaMedica) {
                FichaMedica.borrarDeCarpeta(cliente.dni());
            } else if (!cliente.dni().equals(dni)) {
                FichaMedica.renombrarEnCarpeta(cliente.dni(), dni);
            }
        } catch (IOException e) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Ficha no actualizada en carpeta",
                    "Los datos se guardaron en la base, pero la copia de la ficha no pudo actualizarse: "
                            + e.getMessage());
        }

        try {
            mostrarListaClientes();
            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Cliente actualizado",
                    "Los cambios se guardaron.");
        } catch (IOException e) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "No se pudo volver a la lista",
                    e.getMessage());
        }
    }

    private void mostrarListaClientes() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                        "/com/example/fxml/lista-clientes.fxml"));
        Parent root = loader.load();
        NavigationShell.setRoot(fichaMedicaLabel.getScene(), root);
    }

    private boolean esPdfValido(byte[] contenido) {
        return contenido.length <= TAMANO_MAXIMO_FICHA
                && contenido.length >= 5
                && contenido[0] == '%'
                && contenido[1] == 'P'
                && contenido[2] == 'D'
                && contenido[3] == 'F'
                && contenido[4] == '-';
    }

    private void validarFormularioVisualmente() {
        FormValidation.validateNow(dniField);
        FormValidation.validateNow(nombreField);
        FormValidation.validateNow(apellidoField);
        FormValidation.validateNow(fechaNacimientoPicker);
        FormValidation.validateNow(emailField);
        FormValidation.validateNow(telefonoField);
        FormValidation.validateNow(sexoComboBox);
        validarContactoVinculado();
        validarTutorVinculado();
    }

    private void mostrarAlerta(
            Alert.AlertType tipo,
            String titulo,
            String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        AlertUtils.showAndWait(alerta, dniField);
    }

    private String textoSeguro(String valor) {
        return valor == null ? "" : valor;
    }
}
