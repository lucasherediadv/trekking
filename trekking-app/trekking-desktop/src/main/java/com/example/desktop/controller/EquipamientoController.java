package com.example.desktop.controller;

import com.example.core.excepcion.ExcepcionNegocio;
import com.example.core.inventario.ElementoInventario;
import com.example.desktop.Servicios;
import com.example.desktop.util.NavigationShell;
import com.example.desktop.util.TableEmptyState;
import com.example.desktop.util.TableHeight;
import com.example.desktop.util.ComboBoxDisplayLabel;
import com.example.desktop.util.AlertUtils;
import com.example.desktop.util.SearchFieldIcon;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import javafx.collections.transformation.FilteredList;

import java.io.IOException;
import java.util.Optional;

public class EquipamientoController {

    @FXML
    private TableView<ElementoInventario> tablaEquipamiento;

    @FXML
    private TableColumn<ElementoInventario, String> colNombre;

    @FXML
    private TableColumn<ElementoInventario, String> colCategoria;

    @FXML
    private TableColumn<ElementoInventario, Integer> colCantidad;

    @FXML
    private TableColumn<ElementoInventario, Integer> colReservada;

    @FXML
    private TableColumn<ElementoInventario, Integer> colDisponible;

    @FXML
    private TextField txtBuscar;

    @FXML
    private ComboBox<String> estadoFiltroCombo;

    @FXML
    private TableColumn<ElementoInventario, String> colEstado;
    @FXML private TableColumn<ElementoInventario, ElementoInventario> colAcciones;

    private ObservableList<ElementoInventario> equipamientoOriginal;
    private FilteredList<ElementoInventario> equipamientoFiltrado;

    @FXML
    public void initialize() {
        javafx.application.Platform.runLater(txtBuscar::requestFocus);

        SearchFieldIcon.install(txtBuscar);

        tablaEquipamiento.getStyleClass().add("listing-table");
        tablaEquipamiento.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colNombre.setCellValueFactory(
                data -> new ReadOnlyObjectWrapper<>(data.getValue().nombre()));

        colCategoria.setCellValueFactory(
                data -> new ReadOnlyObjectWrapper<>(data.getValue().categoria()));

        colCantidad.setCellValueFactory(
                data -> new ReadOnlyObjectWrapper<>(data.getValue().cantidad()));

        colReservada.setCellValueFactory(
                data -> new ReadOnlyObjectWrapper<>(data.getValue().cantidadReservada()));

        colDisponible.setCellValueFactory(
                data -> new ReadOnlyObjectWrapper<>(data.getValue().cantidadDisponible()));

        colEstado.setCellValueFactory(
                data -> new ReadOnlyObjectWrapper<>(data.getValue().estado().etiqueta()));
        colEstado.setCellFactory(column -> com.example.desktop.util.EstadoCell.create(estado -> switch (estado) {
            case "Disponible" -> com.example.desktop.util.EstadoCell.Tono.EXITO;
            case "En Inspección", "En Reparación" -> com.example.desktop.util.EstadoCell.Tono.ADVERTENCIA;
            case "Fuera de Servicio", "Extraviado" -> com.example.desktop.util.EstadoCell.Tono.PELIGRO;
            default -> null;
        }));
        colAcciones.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));

        colAcciones.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override protected void updateItem(ElementoInventario item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { com.example.desktop.util.RowActionCell.clear(this); return; }
                javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(3);
                if ("Inactivo".equals(estadoFiltroCombo.getValue())) {
                    actions.getChildren().add(com.example.desktop.util.RowActionButton.create(
                            "Reactivar elemento", com.example.desktop.util.RowActionButton.Icon.RESTORE,
                            e -> confirmarReactivacion(item)));
                } else {
                    javafx.scene.control.Button baja = com.example.desktop.util.RowActionButton.create(
                            "Dar de baja", com.example.desktop.util.RowActionButton.Icon.DELETE,
                            e -> confirmarBaja(item));
                    baja.getStyleClass().add("row-action-danger");
                    actions.getChildren().addAll(
                            com.example.desktop.util.RowActionButton.create("Editar elemento",
                                    com.example.desktop.util.RowActionButton.Icon.EDIT, e -> abrirFormulario(item)), baja);
                }
                com.example.desktop.util.RowActionCell.showCentered(this, actions);
            }
        });

        estadoFiltroCombo.getItems().setAll("Activo", "Inactivo");
        ComboBoxDisplayLabel.prefixValue(estadoFiltroCombo, "Estado");
        estadoFiltroCombo.setValue("Activo");
        estadoFiltroCombo.valueProperty().addListener((observable, anterior, actual) -> cargarEquipamiento());
        txtBuscar.textProperty().addListener((observable, anterior, actual) -> aplicarFiltro());

        cargarEquipamiento();
    }

    private void cargarEquipamiento() {

        try {
            equipamientoOriginal = FXCollections.observableArrayList(
                    "Inactivo".equals(estadoFiltroCombo.getValue())
                            ? Servicios.inventario().listarDadosDeBaja()
                            : Servicios.inventario().listarActivos());
        } catch (RuntimeException e) {
            equipamientoOriginal = FXCollections.observableArrayList();
            mostrarAlerta(Alert.AlertType.ERROR, "Inventario", "No se pudo cargar el Inventario: " + e.getMessage());
        }
        equipamientoFiltrado = new FilteredList<>(equipamientoOriginal, item -> true);
        tablaEquipamiento.setItems(equipamientoFiltrado);
        aplicarFiltro();
    }

    private void aplicarFiltro() {
        if (equipamientoFiltrado == null) return;
        String consulta = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        equipamientoFiltrado.setPredicate(item -> consulta.isEmpty()
                || item.nombre().toLowerCase().contains(consulta)
                || item.categoria().toLowerCase().contains(consulta)
                || item.estado().etiqueta().toLowerCase().contains(consulta));
        boolean hayEquipamiento = equipamientoOriginal != null && !equipamientoOriginal.isEmpty();
        boolean filtroActivo = !consulta.isEmpty();
        TableEmptyState.update(tablaEquipamiento, hayEquipamiento, filtroActivo,
                "Inactivo".equals(estadoFiltroCombo.getValue())
                        ? "No hay elementos dados de baja" : "No hay elementos activos",
                "Inactivo".equals(estadoFiltroCombo.getValue())
                        ? "Los elementos dados de baja aparecerán aquí." : "Agrega elementos para administrar el Inventario.",
                "No hay elementos que coincidan", "Prueba con otro nombre, categoría o estado.",
                "INVENTORY",
                "+ Nuevo elemento", event -> nuevoEquipamiento());
        TableHeight.update(tablaEquipamiento, equipamientoFiltrado.size(), 300);
    }

    @FXML
    private void nuevoEquipamiento() {

        abrirFormulario(null);
    }

    @FXML
    private void editarEquipamiento() {

        ElementoInventario seleccionado =
                tablaEquipamiento.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Editar equipamiento",
                    "Seleccione un equipo para editar.");
            return;
        }

        abrirFormulario(seleccionado);
    }

    @FXML
    private void eliminarEquipamiento() {

        ElementoInventario seleccionado =
                tablaEquipamiento.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Eliminar equipamiento",
                    "Seleccione un equipo para eliminar.");
            return;
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION);

        int reservada = seleccionado.cantidadReservada();
        confirmacion.setTitle("Dar de baja elemento de inventario");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText(reservada > 0
                ? "Este elemento tiene " + reservada + " unidad(es) reservada(s). "
                        + "Las Salidas planificadas mantienen esa reserva. ¿Desea darlo de baja?"
                : "¿Está seguro de dar de baja el elemento seleccionado?");

        Optional<ButtonType> resultado =
                AlertUtils.showAndWait(confirmacion, tablaEquipamiento);

        if (resultado.isPresent()
                && resultado.get() == ButtonType.OK) {

            if (!darDeBaja(seleccionado)) return;

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Inventario",
                    "Elemento dado de baja correctamente.");
        }
    }

    private void confirmarBaja(ElementoInventario seleccionado) {
        int reservada = seleccionado.cantidadReservada();
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Dar de baja elemento de inventario");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Dar de baja «" + seleccionado.nombre() + "»? Se excluirá del uso activo."
                + (reservada > 0 ? " Las " + reservada + " unidades reservadas por Salidas planificadas se conservan." : ""));
        if (AlertUtils.showAndWait(confirmacion, tablaEquipamiento).orElse(ButtonType.CANCEL) == ButtonType.OK) {
            darDeBaja(seleccionado);
        }
    }

    private boolean darDeBaja(ElementoInventario elemento) {
        try {
            Servicios.inventario().darDeBaja(elemento.id());
            return true;
        } catch (ExcepcionNegocio | IllegalStateException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Dar de baja elemento", e.getMessage());
            return false;
        } finally {
            cargarEquipamiento();
        }
    }

    private void confirmarReactivacion(ElementoInventario seleccionado) {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Reactivar «" + seleccionado.nombre() + "»? Volverá al uso activo.",
                ButtonType.CANCEL, ButtonType.OK);
        confirmacion.setTitle("Reactivar elemento");
        confirmacion.setHeaderText(null);
        if (AlertUtils.showAndWait(confirmacion, tablaEquipamiento).orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                Servicios.inventario().reactivar(seleccionado.id());
            } catch (ExcepcionNegocio | IllegalStateException e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Reactivar elemento", e.getMessage());
                cargarEquipamiento();
                return;
            }
            cargarEquipamiento();
            mostrarAlerta(Alert.AlertType.INFORMATION, "Inventario",
                    "Elemento reactivado correctamente.");
        }
    }

    @FXML
    private void volverDashboard() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/fxml/dashboard.fxml"));

            Parent root = loader.load();

            Stage stage = (Stage) tablaEquipamiento
                    .getScene()
                    .getWindow();

            NavigationShell.setRoot(stage.getScene(), root);
            stage.setTitle("Trekking");

        } catch (IOException e) {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo volver al dashboard.");
        }
    }

    private void abrirFormulario(ElementoInventario equipamiento) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/fxml/equipamiento-form.fxml"));

            Parent root = loader.load();

            EquipamientoFormController controller =
                    loader.getController();

            controller.cargarEquipamiento(equipamiento);

            Stage stage = (Stage) tablaEquipamiento
                    .getScene()
                    .getWindow();

            NavigationShell.setRoot(stage.getScene(), root, equipamiento == null ? "inventario-alta" : "inventario-edicion");
            stage.setTitle(
                    equipamiento == null
                            ? "Nuevo equipamiento"
                            : "Editar equipamiento");

        } catch (IOException e) {

            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    "No se pudo abrir el formulario de equipamiento.");
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
        AlertUtils.showAndWait(alerta, tablaEquipamiento);
    }
}
