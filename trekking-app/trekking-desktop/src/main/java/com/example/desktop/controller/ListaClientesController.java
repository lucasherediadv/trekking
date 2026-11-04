package com.example.desktop.controller;

import java.io.IOException;
import java.awt.Desktop;
import java.nio.file.Files;
import java.util.Optional;
import java.net.URL;
import java.util.ResourceBundle;

import com.example.core.cliente.Cliente;
import com.example.core.cliente.FichaMedica;
import com.example.core.excepcion.ExcepcionNegocio;
import com.example.desktop.Servicios;
import com.example.desktop.util.NavigationShell;
import com.example.desktop.util.TableEmptyState;
import com.example.desktop.util.TableHeight;
import com.example.desktop.util.ComboBoxDisplayLabel;
import com.example.desktop.util.AlertUtils;
import com.example.desktop.util.SearchFieldIcon;
import com.example.desktop.util.FechaFormato;

import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;

@SuppressWarnings("unused")
public class ListaClientesController implements Initializable {

    @FXML
    private TableView<Cliente> tablaClientes;

    @FXML
    private TextField buscarField;

    @FXML
    private ComboBox<String> estadoFiltroCombo;

    @FXML
    private TableColumn<Cliente, String> colNombre;

    @FXML
    private TableColumn<Cliente, String> colApellido;

    @FXML
    private TableColumn<Cliente, String> colEmail;

    @FXML
    private TableColumn<Cliente, String> colSexo;

    @FXML
    private TableColumn<Cliente, Boolean> colActivo;

    @FXML
    private TableColumn<Cliente, String> colDni;

    @FXML
    private TableColumn<Cliente, String> colTelefono;

    @FXML
    private TableColumn<Cliente, java.time.LocalDate> colFechaNacimiento;

    @FXML private TableColumn<Cliente, Cliente> colAcciones;


    private FilteredList<Cliente> clientesFiltrados;
    private ObservableList<Cliente> clientesOriginales;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        SearchFieldIcon.install(buscarField);
        javafx.application.Platform.runLater(buscarField::requestFocus);

        tablaClientes.getStyleClass().add("listing-table");

        tablaClientes.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY);

        colDni.setCellValueFactory(fila -> new javafx.beans.property.ReadOnlyObjectWrapper<>(fila.getValue().dni()));

        colNombre.setCellValueFactory(fila -> new javafx.beans.property.ReadOnlyObjectWrapper<>(fila.getValue().nombre()));

        colApellido.setCellValueFactory(fila -> new javafx.beans.property.ReadOnlyObjectWrapper<>(fila.getValue().apellido()));

        colEmail.setCellValueFactory(fila -> new javafx.beans.property.ReadOnlyObjectWrapper<>(fila.getValue().email()));

        colTelefono.setCellValueFactory(fila -> new javafx.beans.property.ReadOnlyObjectWrapper<>(fila.getValue().telefono()));

        colFechaNacimiento.setCellValueFactory(fila -> new javafx.beans.property.ReadOnlyObjectWrapper<>(fila.getValue().fechaNacimiento()));
        colFechaNacimiento.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override protected void updateItem(java.time.LocalDate fecha, boolean empty) {
                super.updateItem(fecha, empty);
                setText(empty || fecha == null ? null : FechaFormato.formatear(fecha));
            }
        });

        colSexo.setCellValueFactory(fila -> new javafx.beans.property.ReadOnlyObjectWrapper<>(fila.getValue().sexo()));

        colActivo.setCellValueFactory(fila -> new javafx.beans.property.ReadOnlyObjectWrapper<>(fila.getValue().activo()));
        colActivo.setCellFactory(column -> com.example.desktop.util.EstadoCell.create(
                activo -> activo ? com.example.desktop.util.EstadoCell.Tono.EXITO : com.example.desktop.util.EstadoCell.Tono.NEUTRO,
                activo -> activo ? "Activo" : "Inactivo"));
        colAcciones.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));

        colAcciones.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override protected void updateItem(Cliente cliente, boolean empty) {
                super.updateItem(cliente, empty);
                if (empty || cliente == null) { com.example.desktop.util.RowActionCell.clear(this); return; }
                javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(3);
                actions.getChildren().addAll(
                        com.example.desktop.util.RowActionButton.create("Editar cliente", com.example.desktop.util.RowActionButton.Icon.EDIT, e -> abrirEdicion(cliente)),
                        com.example.desktop.util.RowActionButton.create("Abrir ficha médica", com.example.desktop.util.RowActionButton.Icon.MEDICAL, e -> abrirFichaMedica(cliente)),
                        com.example.desktop.util.RowActionButton.create(cliente.activo() ? "Dar de baja" : "Reactivar",
                                cliente.activo() ? com.example.desktop.util.RowActionButton.Icon.DELETE : com.example.desktop.util.RowActionButton.Icon.RESTORE,
                                e -> cambiarEstado(cliente)));
                if (cliente.activo()) actions.getChildren().get(2).getStyleClass().add("row-action-danger");
                com.example.desktop.util.RowActionCell.showCentered(this, actions);
            }
        });

        buscarField.textProperty().addListener(
                (observable, anterior, actual) -> aplicarFiltros());

        estadoFiltroCombo.getItems().setAll("Activo", "Inactivo");
        ComboBoxDisplayLabel.prefixValue(estadoFiltroCombo, "Estado");
        estadoFiltroCombo.setValue("Activo");
        estadoFiltroCombo.valueProperty().addListener(
                (observable, anterior, actual) -> aplicarFiltros());

        cargarClientes();
    }

    private void cargarClientes() {

        ObservableList<Cliente> clientes = javafx.collections.FXCollections.observableArrayList(Servicios.clientes().listar());
        clientesOriginales = clientes;

        clientesFiltrados = new FilteredList<>(clientes);

        tablaClientes.setItems(clientesFiltrados);

        aplicarFiltros();
    }

    private void aplicarFiltros() {

        if (clientesFiltrados == null) {
            return;
        }

        String texto =
                buscarField.getText().trim().toLowerCase();

        boolean filtrarActivos = "Activo".equals(estadoFiltroCombo.getValue());

        clientesFiltrados.setPredicate(cliente -> {

            boolean coincideTexto =
                    texto.isEmpty()
                    || contiene(cliente.dni(), texto)
                    || contiene(cliente.nombre(), texto)
                    || contiene(cliente.apellido(), texto);

            return coincideTexto && cliente.activo() == filtrarActivos;
        });
        actualizarEstadoTabla();
    }

    private void actualizarEstadoTabla() {
        boolean filtrosActivos = !buscarField.getText().isBlank()
                || "Inactivo".equals(estadoFiltroCombo.getValue());
        boolean hayClientes = clientesOriginales != null && !clientesOriginales.isEmpty();
        TableEmptyState.update(tablaClientes, hayClientes, filtrosActivos,
                "Todavía no hay clientes", "Agrega un Cliente para empezar a planificar Salidas.",
                "No hay clientes que coincidan", "Prueba con otro DNI, nombre o apellido.",
                "GROUPOFF",
                "+ Nuevo cliente", event -> {
                    try {
                        irAgregarCliente(event);
                    } catch (IOException e) {
                        mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo abrir el alta de Cliente.");
                    }
                });
        TableHeight.update(tablaClientes,
                tablaClientes.getItems() == null ? 0 : tablaClientes.getItems().size(), 300);
    }

    private boolean contiene(String valor, String texto) {

        return valor != null
                && valor.toLowerCase().contains(texto);
    }

    @FXML
    private void irAgregarCliente(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                        "/com/example/fxml/agregar-cliente.fxml"));
        Parent root = loader.load();
        NavigationShell.setRoot(
                ((Node) event.getSource()).getScene(), root);
    }

    @FXML
    private void volverDashboard() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                        "/com/example/fxml/dashboard.fxml"));
        Parent root = loader.load();
        NavigationShell.setRoot(tablaClientes.getScene(), root);
    }

    @FXML
    private void editarCliente() {

        Cliente clienteSeleccionado =
                tablaClientes.getSelectionModel()
                        .getSelectedItem();

        if (clienteSeleccionado == null) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Cliente no seleccionado",
                    "Selecciona un cliente para editarlo.");

            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/fxml/editar-cliente.fxml"));
            loader.setController(new EditarClienteController(clienteSeleccionado));
            Parent root = loader.load();
            NavigationShell.setRoot(tablaClientes.getScene(), root, "cliente-edicion");

        } catch (IOException e) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "No se pudo abrir la edicion",
                    e.getMessage());
        }
    }

    private void abrirEdicion(Cliente clienteSeleccionado) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/fxml/editar-cliente.fxml"));
            loader.setController(new EditarClienteController(clienteSeleccionado));
            NavigationShell.setRoot(tablaClientes.getScene(), loader.load(), "cliente-edicion");
        } catch (IOException e) { mostrarAlerta(Alert.AlertType.ERROR, "No se pudo abrir la edición", e.getMessage()); }
    }

    @FXML
    private void abrirFichaMedica() {

        Cliente clienteSeleccionado =
                tablaClientes.getSelectionModel()
                        .getSelectedItem();

        if (clienteSeleccionado == null) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Cliente no seleccionado",
                    "Selecciona un cliente para abrir su ficha medica.");

            return;
        }

        try {

            Optional<FichaMedica> fichaMedica = Servicios.fichasMedicas().consultar(clienteSeleccionado.id());

            if (fichaMedica.isPresent()) {
                if (!Desktop.isDesktopSupported()
                        || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    throw new IOException("El sistema no tiene disponible un visor de PDF.");
                }
                java.nio.file.Path archivoTemporal =
                        Files.createTempFile("ficha-medica-", ".pdf");
                Files.write(archivoTemporal, fichaMedica.get().contenido());
                archivoTemporal.toFile().deleteOnExit();
                Desktop.getDesktop().open(archivoTemporal.toFile());
            } else {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Sin ficha médica",
                        "El cliente no tiene una ficha médica cargada.");
            }

        } catch (IOException | ExcepcionNegocio | IllegalStateException e) {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "No se pudo abrir la ficha",
                    e.getMessage());
        }
    }

    private void abrirFichaMedica(Cliente clienteSeleccionado) {
        tablaClientes.getSelectionModel().select(clienteSeleccionado);
        abrirFichaMedica();
    }

    @FXML
    private void eliminarCliente() {

        Cliente clienteSeleccionado =
                tablaClientes.getSelectionModel()
                        .getSelectedItem();

        if (clienteSeleccionado == null) {

            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Cliente no seleccionado",
                    "Selecciona un cliente para darlo de baja.");

            return;
        }

        Alert confirmacion =
                new Alert(Alert.AlertType.CONFIRMATION);

        confirmacion.setTitle("Dar de baja cliente");
        confirmacion.setHeaderText(null);

        confirmacion.setContentText(
                "Queres dar de baja a "
                + clienteSeleccionado.nombre()
                + " "
                + clienteSeleccionado.apellido()
                + "?");

        if (AlertUtils.showAndWait(confirmacion, tablaClientes).orElse(null)
                == javafx.scene.control.ButtonType.OK) {

            try {
                Servicios.clientes().darDeBaja(clienteSeleccionado.id());
            } catch (ExcepcionNegocio | IllegalStateException e) {
                mostrarAlerta(Alert.AlertType.ERROR, "No se pudo dar de baja", e.getMessage());
                return;
            }

            cargarClientes();

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Baja realizada",
                    "El cliente ya no figura como activo.");
        }
    }

    private void cambiarEstado(Cliente cliente) {
        String verbo = cliente.activo() ? "dar de baja" : "reactivar";
        Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION);
        confirmar.setTitle("Confirmar " + verbo);
        confirmar.setHeaderText(null);
        confirmar.setContentText("¿Quieres " + verbo + " a " + cliente.nombre() + " " + cliente.apellido()
                + (cliente.activo() ? "? Dejará de estar disponible para el uso activo." : "?"));
        if (AlertUtils.showAndWait(confirmar, tablaClientes).orElse(javafx.scene.control.ButtonType.CANCEL) != javafx.scene.control.ButtonType.OK) return;
        try {
            if (cliente.activo()) Servicios.clientes().darDeBaja(cliente.id());
            else Servicios.clientes().reactivar(cliente.id());
        } catch (ExcepcionNegocio | IllegalStateException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "No se pudo completar la acción", e.getMessage());
            return;
        }
        cargarClientes();
    }

    private void mostrarAlerta(
            Alert.AlertType tipo,
            String titulo,
            String mensaje) {

        Alert alerta = new Alert(tipo);

        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);

        AlertUtils.showAndWait(alerta, tablaClientes);
    }
}
