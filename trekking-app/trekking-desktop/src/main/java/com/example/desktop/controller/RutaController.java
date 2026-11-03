package com.example.desktop.controller;

import com.example.desktop.dao.RutaDAO;
import com.example.desktop.dao.RutaCheckpointDAO;
import com.example.desktop.model.Ruta;
import com.example.desktop.util.NavigationShell;
import com.example.desktop.util.TableEmptyState;
import com.example.desktop.util.TableHeight;
import com.example.desktop.util.ComboBoxDisplayLabel;
import com.example.desktop.util.AlertUtils;
import com.example.desktop.util.SearchFieldIcon;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.ListCell;
import javafx.stage.Stage;

public class RutaController implements Initializable {

        private final RutaDAO rutaDAO = new RutaDAO();
        private final RutaCheckpointDAO rutaCheckpointDAO = new RutaCheckpointDAO();

        private ObservableList<Ruta> rutas;
        private FilteredList<Ruta> rutasFiltradas;

        private com.example.desktop.util.EstadoCell.Tono tonoDificultad(String dificultad) {
                return switch (dificultad) {
                        case "Baja" -> com.example.desktop.util.EstadoCell.Tono.EXITO;
                        case "Media" -> com.example.desktop.util.EstadoCell.Tono.ADVERTENCIA;
                        case "Alta" -> com.example.desktop.util.EstadoCell.Tono.PELIGRO;
                        default -> null;
                };
        }

        @FXML
        private TableView<Ruta> tablaRutas;

        @FXML
        private TableColumn<Ruta, String> colNombre;

        @FXML
        private TableColumn<Ruta, Double> colAltitudMaxima;

        @FXML
        private TableColumn<Ruta, String> colTipoTerreno;

        @FXML
        private TableColumn<Ruta, String> colDificultadTecnica;

        @FXML
        private TableColumn<Ruta, String> colDificultadFisica;
        @FXML private TableColumn<Ruta, String> colEstadoItinerario;
        @FXML private TableColumn<Ruta, Ruta> colAcciones;

        @FXML
        private TextField buscarField;

        @FXML
        private ComboBox<String> filtroTerrenoCombo;

        @FXML
        private ComboBox<String> filtroDificultadTecnicaCombo;

        @FXML
        private ComboBox<String> filtroDificultadFisicaCombo;

        @FXML
        private ComboBox<String> filtroEstadoCombo;


        @Override
        public void initialize(URL url, ResourceBundle rb) {
                javafx.application.Platform.runLater(buscarField::requestFocus);

                SearchFieldIcon.install(buscarField);

                tablaRutas.getStyleClass().add("listing-table");
                tablaRutas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

                colNombre.setCellValueFactory(
                                new PropertyValueFactory<>("nombre"));

                colNombre.setComparator(String.CASE_INSENSITIVE_ORDER);
                colNombre.setSortType(TableColumn.SortType.ASCENDING);

                colAltitudMaxima.setCellValueFactory(
                                new PropertyValueFactory<>("altitudMaxima"));

                colTipoTerreno.setCellValueFactory(
                                new PropertyValueFactory<>("tipoTerreno"));

                colDificultadTecnica.setCellValueFactory(
                                new PropertyValueFactory<>("dificultadTecnica"));

                colDificultadFisica.setCellValueFactory(
                                new PropertyValueFactory<>("dificultadFisica"));
                colDificultadTecnica.setCellFactory(column -> com.example.desktop.util.EstadoCell.create(this::tonoDificultad));
                colDificultadFisica.setCellFactory(column -> com.example.desktop.util.EstadoCell.create(this::tonoDificultad));
                colEstadoItinerario.setCellValueFactory(data -> {
                        int cantidad = rutaCheckpointDAO.obtenerPorRuta(data.getValue().getId()).size();
                        return new javafx.beans.property.SimpleStringProperty(cantidad == 0 ? "Borrador · 0" : cantidad + " Checkpoints · Completa");
                });
                colAcciones.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));

                colAcciones.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
                        @Override protected void updateItem(Ruta ruta, boolean empty) {
                                super.updateItem(ruta, empty);
                                if (empty || ruta == null) { com.example.desktop.util.RowActionCell.clear(this); return; }
                                boolean activa = rutaDAO.estaActiva(ruta.getId());
                                javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(3);
                                Button estado = com.example.desktop.util.RowActionButton.create(activa ? "Dar de baja" : "Reactivar",
                                        activa ? com.example.desktop.util.RowActionButton.Icon.DELETE : com.example.desktop.util.RowActionButton.Icon.RESTORE,
                                        e -> cambiarEstado(ruta));
                                if (activa) estado.getStyleClass().add("row-action-danger");
                                actions.getChildren().addAll(
                                        com.example.desktop.util.RowActionButton.create("Ver checkpoints", com.example.desktop.util.RowActionButton.Icon.MAP, e -> abrirItinerario(ruta)),
                                        com.example.desktop.util.RowActionButton.create("Editar ruta", com.example.desktop.util.RowActionButton.Icon.EDIT, e -> abrirEdicion(ruta)), estado);
                                com.example.desktop.util.RowActionCell.showCentered(this, actions);
                        }
                });

                configurarFiltros();
                cargarRutas();

                tablaRutas.getSortOrder().add(colNombre);
                tablaRutas.sort();

                actualizarEstadoTabla();
        }

        private void cargarRutas() {

                rutas = FXCollections.observableArrayList(switch (filtroEstadoCombo.getValue()) {
                        case "Inactivo" -> rutaDAO.obtenerTodasInactivas();
                        case "Activo" -> rutaDAO.obtenerTodas();
                        default -> rutaDAO.obtenerTodasIncluyendoInactivas();
                });

                rutasFiltradas = new FilteredList<>(rutas);

                tablaRutas.setItems(rutasFiltradas);

                aplicarFiltros();
        }

        private void abrirItinerario(Ruta ruta) {
                try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/fxml/itinerario-ruta.fxml"));
                        Parent root = loader.load();
                        loader.<ItinerarioRutaController>getController().cargar(ruta);
                        NavigationShell.setRoot(tablaRutas.getScene(), root, "itinerario-ruta");
                } catch (IOException ex) { mostrarAlerta("Error", "No se pudo abrir el Itinerario de la Ruta."); }
        }

        @FXML
        private void nuevaRuta() throws IOException {

                Stage stage = (Stage) tablaRutas.getScene().getWindow();

                FXMLLoader loader = new FXMLLoader(
                                getClass().getResource(
                                                "/com/example/fxml/ruta-form.fxml"));

                Parent root = loader.load();

                RutaFormController controller = loader.getController();

                controller.setRutaEnEdicion(null);

                NavigationShell.setRoot(stage.getScene(), root, "ruta-alta");
        }

        @FXML
        private void editarRuta() throws IOException {

                Ruta rutaSeleccionada = tablaRutas.getSelectionModel().getSelectedItem();

                if (rutaSeleccionada == null) {
                        mostrarAlerta(
                                        "Selecciona una ruta",
                                        "Selecciona una ruta para editar.");
                        return;
                }

                Stage stage = (Stage) tablaRutas.getScene().getWindow();

                FXMLLoader loader = new FXMLLoader(
                                getClass().getResource(
                                                "/com/example/fxml/ruta-form.fxml"));

                Parent root = loader.load();

                RutaFormController controller = loader.getController();

                controller.setRutaEnEdicion(rutaSeleccionada);


                NavigationShell.setRoot(stage.getScene(), root, "ruta-edicion");
        }

        private void abrirEdicion(Ruta rutaSeleccionada) {
                try {
                        Stage stage = (Stage) tablaRutas.getScene().getWindow();
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/fxml/ruta-form.fxml"));
                        Parent root = loader.load();
                        loader.<RutaFormController>getController().setRutaEnEdicion(rutaSeleccionada);
                        NavigationShell.setRoot(stage.getScene(), root, "ruta-edicion");
                } catch (IOException ex) { mostrarAlerta("Error", "No se pudo abrir el formulario de Ruta."); }
        }

        private void cambiarEstado(Ruta ruta) {
                boolean activa = rutaDAO.estaActiva(ruta.getId());
                Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
                confirmacion.setTitle(activa ? "Dar de baja Ruta" : "Reactivar Ruta");
                confirmacion.setHeaderText(null);
                confirmacion.setContentText("¿" + (activa ? "Dar de baja" : "Reactivar") + " la Ruta «" + ruta.getNombre() + "»?"
                                + (activa ? " Quedará excluida del uso activo." : ""));
                if (AlertUtils.showAndWait(confirmacion, tablaRutas).orElse(ButtonType.CANCEL) != ButtonType.OK) return;
                boolean actualizado = activa ? rutaDAO.eliminar(ruta.getId()) : rutaDAO.reactivar(ruta.getId());
                if (!actualizado) mostrarAlerta("Error", "No se pudo actualizar el estado de la Ruta.");
                cargarRutas(); tablaRutas.getSelectionModel().select(ruta);
        }

        @FXML
        private void eliminarRuta() {

                Ruta rutaSeleccionada = tablaRutas.getSelectionModel().getSelectedItem();

                if (rutaSeleccionada == null) {

                        mostrarAlerta(
                                        "Selecciona una ruta",
                                        "Selecciona una ruta para eliminar.");

                        return;
                }

                if (!rutaDAO.estaActiva(rutaSeleccionada.getId())) {
                        if (rutaDAO.reactivar(rutaSeleccionada.getId())) {
                                cargarRutas();
                                mostrarAlerta("Ruta reactivada", "La Ruta volvió a estar activa.");
                        } else {
                                mostrarAlerta("Error", "No se pudo reactivar la Ruta.");
                        }
                        return;
                }

                Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);

                confirmacion.setTitle("Confirmar eliminación");
                confirmacion.setHeaderText("¿Estás seguro?");

                confirmacion.setContentText(
                                "¿Eliminar la ruta \""
                                                + rutaSeleccionada.getNombre()
                                                + "\"?");

                if (AlertUtils.showAndWait(confirmacion, tablaRutas).orElse(null) == ButtonType.OK) {

                        if (rutaDAO.eliminar(rutaSeleccionada.getId())) {

                                cargarRutas();

                                mostrarAlerta(
                                                "Éxito",
                                                "Ruta eliminada correctamente.");

                        } else {

                                mostrarAlerta(
                                                "Error",
                                                "No se pudo eliminar la ruta.");
                        }
                }
        }

        private void configurarFiltros() {

                filtroTerrenoCombo.getItems().setAll(
                                "Todos", "Rocoso", "Boscoso", "Sendero", "Mixto");

                filtroDificultadTecnicaCombo.getItems().setAll(
                                "Todas", "Baja", "Media", "Alta");

                filtroDificultadFisicaCombo.getItems().setAll(
                                "Todas", "Baja", "Media", "Alta");

                filtroEstadoCombo.getItems().setAll("Activo", "Inactivo");

                ComboBoxDisplayLabel.prefixValue(filtroTerrenoCombo, "Terreno");
                ComboBoxDisplayLabel.prefixValue(filtroDificultadTecnicaCombo, "Dificultad técnica");
                ComboBoxDisplayLabel.prefixValue(filtroDificultadFisicaCombo, "Dificultad física");
                ComboBoxDisplayLabel.prefixValue(filtroEstadoCombo, "Estado");

                filtroTerrenoCombo.setValue("Todos");
                filtroDificultadTecnicaCombo.setValue("Todas");
                filtroDificultadFisicaCombo.setValue("Todas");
                filtroEstadoCombo.setValue("Activo");

                buscarField.textProperty().addListener(
                                (observable, anterior, actual) -> aplicarFiltros());

                filtroTerrenoCombo.valueProperty().addListener(
                                (observable, anterior, actual) -> aplicarFiltros());

                filtroDificultadTecnicaCombo.valueProperty().addListener(
                                (observable, anterior, actual) -> aplicarFiltros());

                filtroDificultadFisicaCombo.valueProperty().addListener(
                                (observable, anterior, actual) -> aplicarFiltros());

                filtroEstadoCombo.valueProperty().addListener(
                                (observable, anterior, actual) -> cargarRutas());
        }

        private void aplicarFiltros() {

                if (rutasFiltradas == null) {
                        return;
                }

                String texto = buscarField.getText()
                                .trim()
                                .toLowerCase();

                String terreno = filtroTerrenoCombo.getValue();

                String dificultadTecnica = filtroDificultadTecnicaCombo.getValue();

                String dificultadFisica = filtroDificultadFisicaCombo.getValue();

                rutasFiltradas.setPredicate(ruta -> {

                        boolean coincideNombre = texto.isEmpty()
                                        || ruta.getNombre()
                                                        .toLowerCase()
                                                        .contains(texto);

                        boolean coincideTerreno = terreno.equals("Todos")
                                        || ruta.getTipoTerreno()
                                                        .equals(terreno);

                        boolean coincideDificultadTecnica = dificultadTecnica.equals("Todas")
                                        || ruta.getDificultadTecnica()
                                                        .equals(dificultadTecnica);

                        boolean coincideDificultadFisica = dificultadFisica.equals("Todas")
                                        || ruta.getDificultadFisica()
                                                        .equals(dificultadFisica);

                        return coincideNombre
                                        && coincideTerreno
                                        && coincideDificultadTecnica
                                        && coincideDificultadFisica;
                });
                actualizarEstadoTabla();
        }

        private void actualizarEstadoTabla() {
                boolean filtrosActivos = !buscarField.getText().isBlank()
                                || !"Todos".equals(filtroTerrenoCombo.getValue())
                                || !"Todas".equals(filtroDificultadTecnicaCombo.getValue())
                                || !"Todas".equals(filtroDificultadFisicaCombo.getValue())
                                || "Inactivo".equals(filtroEstadoCombo.getValue());
                boolean hayRutas = rutas != null && !rutas.isEmpty();
                boolean mostrarInactivas = "Inactivo".equals(filtroEstadoCombo.getValue());
                TableEmptyState.update(tablaRutas, hayRutas, filtrosActivos,
                                mostrarInactivas ? "No hay rutas inactivas" : "Todavía no hay rutas activas",
                                mostrarInactivas ? "Las rutas dadas de baja aparecerán aquí."
                                                : "Crea una Ruta para empezar a planificar Salidas.",
                                "No hay rutas que coincidan", "Prueba con otra búsqueda o cambia los filtros.",
                                "MAP",
                                "+ Nueva Ruta", event -> {
                                        try {
                                                nuevaRuta();
                                        } catch (IOException e) {
                                                mostrarAlerta("Error", "No se pudo abrir el formulario de Ruta.");
                                        }
                                });
                TableHeight.update(tablaRutas,
                                tablaRutas.getItems() == null ? 0 : tablaRutas.getItems().size(), 430);
        }

        private void mostrarAlerta(
                        String titulo,
                        String mensaje) {

                Alert alerta = new Alert(Alert.AlertType.INFORMATION);

                alerta.setTitle(titulo);
                alerta.setHeaderText(null);
                alerta.setContentText(mensaje);
                AlertUtils.showAndWait(alerta, tablaRutas);
        }

        @FXML
        private void volverDashboard() throws IOException {

                FXMLLoader loader = new FXMLLoader(
                                getClass().getResource(
                                                "/com/example/fxml/dashboard.fxml"));

                Parent root = loader.load();

                NavigationShell.setRoot(tablaRutas.getScene(), root);
        }
}
