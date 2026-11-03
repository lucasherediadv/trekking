package com.example.desktop.controller;

import com.example.desktop.dao.RutaCheckpointDAO;
import com.example.desktop.model.Checkpoint;
import com.example.desktop.model.Ruta;
import com.example.desktop.util.AlertUtils;
import com.example.desktop.util.NavigationShell;
import com.example.desktop.util.RowActionButton;
import com.example.desktop.util.SidebarIcon;
import com.example.desktop.util.ToastNotifications;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Edición pendiente y guardado atómico del Itinerario de una Ruta. */
public class ItinerarioRutaController {

    @FXML private ScrollPane root;
    @FXML private Label titulo;
    @FXML private Label resumen;
    @FXML private Label estado;
    @FXML private Label cantidad;
    @FXML private ListView<Checkpoint> lista;
    @FXML private Button agregarButton;

    private final RutaCheckpointDAO dao = new RutaCheckpointDAO();
    private final List<Checkpoint> puntos = new ArrayList<>();
    private Ruta ruta;
    private Scene scene;
    private boolean cambios;

    @FXML
    private void initialize() {
        agregarButton.setGraphic(SidebarIcon.load("ADD.svg"));
        agregarButton.setAccessibleText("Agregar checkpoint");
    }

    public void cargar(Ruta ruta) {
        this.ruta = ruta;
        titulo.setText("Itinerario · " + ruta.getNombre());
        resumen.setText(String.format(
                "Altitud máxima %.0f m · %s · Dificultad %s / %s",
                ruta.getAltitudMaxima(), ruta.getTipoTerreno(),
                ruta.getDificultadTecnica(), ruta.getDificultadFisica()));
        puntos.clear();
        puntos.addAll(dao.obtenerPorRuta(ruta.getId()));
        ordenarPorHora();
        configurarFilas();
        refrescar();
        Platform.runLater(() -> {
            scene = root.getScene();
            if (scene != null) {
                NavigationShell.setNavigationGuard(scene, this::confirmarSalida);
            }
        });
    }

    private void configurarFilas() {
        lista.setPlaceholder(new Label("Esta Ruta todavía no tiene Checkpoints."));
        lista.setCellFactory(view -> new ListCell<>() {
            private final Label hora = new Label();
            private final Label nombre = new Label();
            private final Label detalle = new Label();
            private final VBox datos = new VBox(4, nombre, detalle);
            private final Region separador = new Region();
            private final Button editar = RowActionButton.create(
                    "Editar Checkpoint", RowActionButton.Icon.EDIT,
                    event -> ItinerarioRutaController.this.editar(getItem()));
            private final Button quitar = RowActionButton.create(
                    "Quitar Checkpoint", RowActionButton.Icon.DELETE,
                    event -> ItinerarioRutaController.this.quitar(getItem()));
            private final HBox acciones = new HBox(4, editar, quitar);
            private final HBox fila = new HBox(14, hora, datos, separador, acciones);

            {
                hora.getStyleClass().add("itinerary-time");
                nombre.getStyleClass().add("itinerary-checkpoint-name");
                detalle.getStyleClass().add("itinerary-checkpoint-detail");
                nombre.setWrapText(true);
                detalle.setWrapText(true);
                quitar.getStyleClass().add("row-action-danger");
                fila.setAlignment(javafx.geometry.Pos.TOP_LEFT);
                acciones.getStyleClass().add("row-actions");
                acciones.setOpacity(0);
                acciones.setMouseTransparent(true);
                hoverProperty().addListener((obs, wasHovered, isHovered) -> actualizarVisibilidadAcciones());
                selectedProperty().addListener((obs, wasSelected, isSelected) -> actualizarVisibilidadAcciones());
                editar.focusedProperty().addListener((obs, wasFocused, isFocused) -> actualizarVisibilidadAcciones());
                quitar.focusedProperty().addListener((obs, wasFocused, isFocused) -> actualizarVisibilidadAcciones());
                HBox.setHgrow(datos, Priority.NEVER);
                HBox.setHgrow(separador, Priority.ALWAYS);
            }

            private void actualizarVisibilidadAcciones() {
                boolean visible = isHover() || isSelected() || editar.isFocused() || quitar.isFocused();
                acciones.setOpacity(visible ? 1.0 : 0.0);
                acciones.setMouseTransparent(!visible);
            }

            @Override
            protected void updateItem(Checkpoint checkpoint, boolean empty) {
                super.updateItem(checkpoint, empty);
                if (empty || checkpoint == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                hora.setText(checkpoint.getHora());
                nombre.setText(checkpoint.getNombre());
                detalle.setText(String.format("%.0f m · %.5f, %.5f",
                        checkpoint.getAltitud(), checkpoint.getLatitud(), checkpoint.getLongitud()));
                setGraphic(fila);
                setText(null);
            }
        });
    }

    private void refrescar() {
        ordenarPorHora();
        lista.setItems(FXCollections.observableArrayList(puntos));
        cantidad.setText("(" + puntos.size() + ")");
        estado.setText(puntos.isEmpty()
                ? "Borrador · agrega dos o más Checkpoints para usar esta Ruta en una Salida."
                : "La Ruta puede guardarse sin Checkpoints o con al menos dos.");
        lista.setPrefHeight(puntos.isEmpty()
                ? 92
                : Math.min(420, Math.max(82, puntos.size() * 82.0)));
    }

    private void ordenarPorHora() {
        // List.sort es estable: conserva el orden explícito entre horas iguales.
        puntos.sort(Comparator.comparing(Checkpoint::getHora));
    }

    @FXML
    private void agregar() {
        editar(null);
    }

    private void editar(Checkpoint original) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/fxml/checkpoint-form.fxml"));
            Parent formulario = loader.load();
            CheckpointFormController controller = loader.getController();
            controller.cargarCheckpointParaItinerario(
                    original,
                    ruta.getAltitudMaxima(),
                    actualizado -> guardarCheckpointPendiente(original, actualizado),
                    this::volverAlItinerario,
                    this::confirmarSalida);
            scene = lista.getScene();
            NavigationShell.setNavigationGuard(scene, controller::confirmarNavegacion);
            NavigationShell.setRoot(scene, formulario,
                    original == null ? "checkpoint-ruta-alta" : "checkpoint-ruta-edicion");
        } catch (IOException | RuntimeException ex) {
            alerta("No se pudo abrir el formulario", "No se pudo abrir el formulario del Checkpoint.");
        }
    }

    private void guardarCheckpointPendiente(Checkpoint original, Checkpoint actualizado) {
        if (original == null) {
            puntos.add(actualizado);
        } else {
            for (int i = 0; i < puntos.size(); i++) {
                if (puntos.get(i) == original) {
                    puntos.set(i, actualizado);
                    break;
                }
            }
        }
        cambios = true;
        volverAlItinerario();
        refrescar();
    }

    private void volverAlItinerario() {
        if (scene == null) return;
        NavigationShell.setNavigationGuard(scene, this::confirmarSalida);
        NavigationShell.setRoot(scene, root, "itinerario-ruta");
    }

    private void quitar(Checkpoint checkpoint) {
        if (checkpoint != null) {
            puntos.remove(checkpoint);
            cambios = true;
            refrescar();
        }
    }

    @FXML
    private void guardar() {
        if (puntos.size() == 1) {
            alerta("Itinerario incompleto",
                    "El Itinerario necesita inicio y fin. Agrega otro Checkpoint o quita el único punto.");
            return;
        }
        if (dao.reemplazarPorRuta(ruta.getId(), List.copyOf(puntos))) {
            cambios = false;
            refrescar();
            ToastNotifications.show(scene.getWindow(), "Itinerario guardado",
                    ToastNotifications.Kind.SUCCESS);
        } else {
            alerta("No se pudo guardar",
                    "El Itinerario anterior se conserva. Verifica la altitud máxima y vuelve a intentar.");
        }
    }

    private boolean confirmarSalida() {
        if (cambios) {
            ButtonType guardar = new ButtonType("Guardar");
            ButtonType descartar = new ButtonType("Descartar");
            ButtonType seguir = new ButtonType("Seguir editando", ButtonBar.ButtonData.CANCEL_CLOSE);
            Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                    "Hay cambios pendientes en el Itinerario.", guardar, descartar, seguir);
            confirmacion.setTitle("Cambios sin guardar");
            confirmacion.setHeaderText("¿Qué quieres hacer?");
            ButtonType respuesta = AlertUtils.showAndWait(confirmacion, scene.getWindow()).orElse(seguir);
            if (respuesta == seguir) return false;
            if (respuesta == guardar) {
                guardar();
                if (cambios) return false;
            }
        }
        NavigationShell.setNavigationGuard(scene, null);
        return true;
    }

    private void alerta(String titulo, String texto) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION, texto);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        AlertUtils.showAndWait(alerta, lista);
    }
}
