package com.example.desktop.controller;

import com.example.desktop.dao.CheckpointDAO;
import com.example.desktop.model.Checkpoint;
import com.example.desktop.util.AlertUtils;
import com.example.desktop.util.NavigationShell;
import com.example.desktop.util.TableEmptyState;
import com.example.desktop.util.SearchFieldIcon;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Controlador del módulo de gestión de Checkpoints.
 *
 * Estructura en {@code checkpoints.fxml}, estilo en {@code styles.css}.
 * Aquí solo vive el comportamiento: carga de datos, filtrado,
 * factories de celdas, interacciones de fila, shortcuts y CRUD.
 */
public class CheckpointController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(CheckpointController.class.getName());

    @FXML
    private TableView<Checkpoint> tablaCheckpoints;

    @FXML
    private TableColumn<Checkpoint, String> colHora;

    @FXML
    private TableColumn<Checkpoint, String> colNombre;

    @FXML
    private TableColumn<Checkpoint, Number> colLatitud;

    @FXML
    private TableColumn<Checkpoint, Number> colLongitud;

    @FXML
    private TableColumn<Checkpoint, String> colDescripcion;

    @FXML
    private TextField filterField;

    @FXML
    private Button clearButton;

    @FXML
    private Button editarButton;

    @FXML
    private Button eliminarButton;

    @FXML
    private Label counterLabel;

    private final CheckpointDAO checkpointDAO = new CheckpointDAO();
    private final ObservableList<Checkpoint> checkpointList = FXCollections.observableArrayList();
    private final FilteredList<Checkpoint> filteredList = new FilteredList<>(checkpointList, checkpoint -> true);
    private final SortedList<Checkpoint> sortedList = new SortedList<>(filteredList);

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        javafx.application.Platform.runLater(filterField::requestFocus);
        SearchFieldIcon.install(filterField);
        tablaCheckpoints.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaCheckpoints.setItems(sortedList);
        sortedList.comparatorProperty().bind(tablaCheckpoints.comparatorProperty());

        configureColumns();
        configureRowInteractions();

        filterField.textProperty()
                .addListener((obs, oldValue, newValue) -> {
                    filteredList.setPredicate(createFilterPredicate(newValue));
                    updateEmptyState();
                });
        clearButton.visibleProperty().bind(filterField.textProperty().isNotEmpty());

        editarButton.disableProperty()
                .bind(tablaCheckpoints.getSelectionModel().selectedItemProperty().isNull());
        eliminarButton.disableProperty()
                .bind(tablaCheckpoints.getSelectionModel().selectedItemProperty().isNull());

        checkpointList.addListener((ListChangeListener<Checkpoint>) change -> updateCounter());
        filteredList.addListener((ListChangeListener<Checkpoint>) change -> updateCounter());

        loadDataFromDatabase();
        updateCounter();

        tablaCheckpoints.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                configureShortcuts();
            }
        });
    }

    @FXML
    private void mostrarCrear() {
        abrirFormulario(null);
    }

    @FXML
    private void editarSeleccionado() {
        abrirFormulario(tablaCheckpoints.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void eliminarSeleccionado() {
        deleteSelected();
    }

    @FXML
    private void limpiarFiltro() {
        filterField.clear();
    }

    private void updateCounter() {
        updateEmptyState();
        if (counterLabel == null) {
            return;
        }
        int total = checkpointList.size();
        int visibles = filteredList.size();
        String unidad = total == 1 ? " checkpoint" : " checkpoints";
        counterLabel.setText(total == visibles ? total + unidad : "Mostrando " + visibles + " de " + total + unidad);
    }

    private void updateEmptyState() {
        boolean hayCheckpoints = !checkpointList.isEmpty();
        boolean filtroActivo = filterField != null && !filterField.getText().isBlank();
        TableEmptyState.update(tablaCheckpoints, hayCheckpoints, filtroActivo,
                "Todavía no hay Checkpoints", "Agrega un Checkpoint para definir los puntos de control.",
                "No hay Checkpoints que coincidan", "Prueba con otro nombre, horario o descripción.",
                "+ Nuevo Checkpoint", event -> mostrarCrear());
    }

    private static Predicate<Checkpoint> createFilterPredicate(String texto) {
        String filtro = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        if (filtro.isEmpty()) {
            return checkpoint -> true;
        }
        return checkpoint -> containsIgnoreCase(checkpoint.getNombre(), filtro)
                || containsIgnoreCase(checkpoint.getHora(), filtro)
                || containsIgnoreCase(checkpoint.getDescripcion(), filtro);
    }

    private static boolean containsIgnoreCase(String valor, String filtro) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(filtro);
    }

    private void configureShortcuts() {
        if (tablaCheckpoints.getScene() == null) {
            return;
        }
        tablaCheckpoints.getScene().getAccelerators().put(
                new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN),
                this::mostrarCrear);
        tablaCheckpoints.getScene().getAccelerators().put(
                new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN),
                this::editarSeleccionado);
        tablaCheckpoints.getScene().getAccelerators().put(
                new KeyCodeCombination(KeyCode.DELETE), this::deleteSelected);
    }

    private void editSelected() {
        Checkpoint selected = tablaCheckpoints.getSelectionModel().getSelectedItem();
        if (selected != null) {
            abrirFormulario(selected);
        }
    }

    private void deleteSelected() {
        Checkpoint selected = tablaCheckpoints.getSelectionModel().getSelectedItem();
        if (selected != null) {
            confirmAndDelete(selected);
        }
    }

    private void configureRowInteractions() {
        tablaCheckpoints.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, anterior, actual) -> {
                    System.out.println(
                            "[SELECCION] Anterior: "
                                    + (anterior == null ? "null" : anterior.getNombre())
                                    + " | Actual: "
                                    + (actual == null ? "null" : actual.getNombre()));
                });
        tablaCheckpoints.setRowFactory(tv -> {
            TableRow<Checkpoint> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();
            MenuItem editItem = new MenuItem("Editar");
            editItem.getStyleClass().add("action-edit-menu");
            editItem.setOnAction(event -> {
                Checkpoint item = row.getItem();
                if (item != null) {
                    abrirFormulario(item);
                }
            });
            MenuItem deleteItem = new MenuItem("Eliminar");
            deleteItem.getStyleClass().add("action-delete-menu");
            deleteItem.setOnAction(event -> {
                Checkpoint item = row.getItem();
                if (item != null) {
                    confirmAndDelete(item);
                }
            });
            contextMenu.getItems().addAll(editItem, deleteItem);
            row.setContextMenu(contextMenu);
            row.setOnMouseClicked(event -> {
                System.out.println(
                        "[CLIC] Cantidad: " + event.getClickCount()
                                + " | Fila vacía: " + row.isEmpty()
                                + " | Checkpoint: "
                                + (row.isEmpty() ? "ninguno" : row.getItem().getNombre())
                                + " | Seleccionado: "
                                + (tablaCheckpoints.getSelectionModel().getSelectedItem() == null
                                        ? "ninguno"
                                        : tablaCheckpoints.getSelectionModel()
                                                .getSelectedItem().getNombre()));

                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    abrirFormulario(row.getItem());
                }
                System.out.println(
                        "[ESTADO] Fila seleccionada: " + row.isSelected()
                                + " | Tabla enfocada: " + tablaCheckpoints.isFocused()
                                + " | Tabla seleccionada: "
                                + tablaCheckpoints.getSelectionModel().getSelectedItem().getNombre()
                                + " | Editar deshabilitado: " + editarButton.isDisabled()
                                + " | Eliminar deshabilitado: " + eliminarButton.isDisabled());
            });
            return row;
        });
    }

    private void configureColumns() {
        colHora.setCellValueFactory(new PropertyValueFactory<>("hora"));
        colHora.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                    getStyleClass().remove("hora-cell");
                } else {
                    setText(item);
                    if (!getStyleClass().contains("hora-cell")) {
                        getStyleClass().add("hora-cell");
                    }
                }
            }
        });
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colLatitud.setCellValueFactory(new PropertyValueFactory<>("latitud"));
        setNumericCellFactory(colLatitud);
        colLongitud.setCellValueFactory(new PropertyValueFactory<>("longitud"));
        setNumericCellFactory(colLongitud);
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
    }

    private static void setNumericCellFactory(TableColumn<Checkpoint, Number> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText("");
                    getStyleClass().remove("coordinate-cell");
                } else {
                    setText(formatCoordinate(value.doubleValue()));
                    if (!getStyleClass().contains("coordinate-cell")) {
                        getStyleClass().add("coordinate-cell");
                    }
                }
            }
        });
    }

    private static String formatCoordinate(double value) {
        if (value == 0.0) {
            return "0.0";
        }
        String raw = String.valueOf(value);
        if (!raw.contains(".")) {
            return raw;
        }
        String trimmed = raw.replaceAll("0+$", "").replaceAll("\\.$", "");
        return trimmed.isEmpty() ? "0.0" : trimmed;
    }

    private Window getWindow() {
        if (tablaCheckpoints.getScene() == null) {
            return null;
        }
        return tablaCheckpoints.getScene().getWindow();
    }

    private boolean loadDataFromDatabase() {
        checkpointList.clear();
        try {
            checkpointList.addAll(checkpointDAO.obtenerTodos());
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar los checkpoints", e);
            AlertUtils.showError(getWindow(), "Error de base de datos",
                    "No se pudieron cargar los checkpoints.\n" + e.getMessage());
            return false;
        }
    }

    private void abrirFormulario(Checkpoint checkpoint) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/fxml/checkpoint-form.fxml"));
            Parent root = loader.load();
            CheckpointFormController controller = loader.getController();
            controller.cargarCheckpoint(checkpoint);
            Stage stage = (Stage) tablaCheckpoints.getScene().getWindow();
            NavigationShell.setRoot(stage.getScene(), root);
            stage.setTitle(checkpoint == null ? "Nuevo checkpoint" : "Editar checkpoint");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al abrir el formulario de checkpoint", e);
            AlertUtils.showError(getWindow(), "Error",
                    "No se pudo abrir el formulario de checkpoint.");
        }
    }

    private void confirmAndDelete(Checkpoint item) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar eliminación");
        alert.setHeaderText("¿Eliminar Checkpoint?");
        alert.setContentText(
                "¿Está seguro de que desea eliminar '" + item.getNombre() + "'? Esta acción no se puede deshacer.");
        ButtonType confirmDelete = new ButtonType("Eliminar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(confirmDelete, cancel);
        AlertUtils.applyTheme(alert);
        Button btnEliminar = (Button) alert.getDialogPane().lookupButton(confirmDelete);
        if (btnEliminar != null) {
            btnEliminar.getStyleClass().add("btn-danger");
        }
        Button btnCancelar = (Button) alert.getDialogPane().lookupButton(cancel);
        if (btnCancelar != null) {
            btnCancelar.getStyleClass().add("btn-secondary");
        }
        Optional<ButtonType> result = AlertUtils.showAndWait(alert, getWindow());
        if (result.isPresent() && result.get() == confirmDelete) {
            try {
                if (checkpointDAO.eliminar(item.getId())) {
                    checkpointList.remove(item);
                } else {
                    AlertUtils.showError(getWindow(), "Error al eliminar",
                            "No se pudo eliminar el checkpoint " + "de la base de datos.");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error al eliminar checkpoint id=" + item.getId(), e);
                AlertUtils.showError(getWindow(), "Error al eliminar",
                        "No se pudo eliminar el checkpoint " + "de la base de datos.\n" + e.getMessage());
            }
        }
    }
}
