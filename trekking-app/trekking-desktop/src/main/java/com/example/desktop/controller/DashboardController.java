package com.example.desktop.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.example.desktop.service.DashboardService;
import com.example.desktop.util.TableEmptyState;
import com.example.desktop.util.NavigationShell;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Panel de Inicio: solo lectura + navegación.
 *
 * <p>Carga al entrar vía {@code initialize()}, sin auto-refresh.
 */
public class DashboardController {

    private final DashboardService dashboardService = new DashboardService();

    @FXML
    private Label kpiRutas;

    @FXML
    private Label kpiClientes;

    @FXML
    private Label kpiSalidas;

    @FXML
    private Label kpiEquipo;

    @FXML
    private TableView<DashboardService.ProximaSalida> tablaProximos;

    @FXML
    private TableColumn<DashboardService.ProximaSalida, LocalDate> colFecha;

    @FXML
    private TableColumn<DashboardService.ProximaSalida, String> colRuta;

    @FXML
    private TableColumn<DashboardService.ProximaSalida, LocalTime> colHorario;

    @FXML
    private TableColumn<DashboardService.ProximaSalida, Integer> colParticipantes;

    @FXML
    private Label errorLabel;

    @FXML
    private void initialize() {
        colFecha.setCellValueFactory(celda -> new SimpleObjectProperty<>(
                celda.getValue() == null ? null : celda.getValue().fecha()));
        colRuta.setCellValueFactory(celda -> new SimpleStringProperty(
                celda.getValue() == null ? "" : celda.getValue().nombreRuta()));
        colHorario.setCellValueFactory(celda -> new SimpleObjectProperty<>(
                celda.getValue() == null ? null : celda.getValue().horaInicio()));
        colHorario.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(LocalTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0
                        || getIndex() >= getTableView().getItems().size()) {
                    setText(null);
                    return;
                }
                DashboardService.ProximaSalida salida =
                        getTableView().getItems().get(getIndex());
                String inicio = salida.horaInicio() == null
                        ? "—"
                        : salida.horaInicio().toString();
                String fin = salida.horaFin() == null
                        ? ""
                        : "–" + salida.horaFin();
                setText(inicio + fin);
            }
        });
        colParticipantes.setCellValueFactory(celda ->
                new SimpleObjectProperty<>(celda.getValue() == null
                        ? null
                        : celda.getValue().participantes()));
        tablaProximos.setPlaceholder(
                TableEmptyState.create("No hay Salidas próximas",
                        "Las Salidas planificadas para los próximos 7 días aparecerán aquí.", null, null));
        cargar();
    }

    @FXML
    private void onVerTodasSalidas() {
        navegar("trips");
    }

    private void cargar() {
        try {
            DashboardService.Kpis kpis = dashboardService.obtenerKpis();
            kpiRutas.setText(String.valueOf(kpis.rutasActivas()));
            kpiClientes.setText(String.valueOf(kpis.clientesActivos()));
            kpiSalidas.setText(String.valueOf(kpis.salidasProximas()));
            kpiEquipo.setText(String.valueOf(kpis.equipamientoCritico()));

            List<DashboardService.ProximaSalida> proximos =
                    dashboardService.obtenerProximasSalidas();
            tablaProximos.setItems(
                    FXCollections.observableArrayList(proximos));

            mostrarError(null);
        } catch (RuntimeException e) {
            mostrarError(
                    "No se pudo cargar el panel: " + e.getMessage());
        }
    }

    private void mostrarError(String mensaje) {
        if (errorLabel == null) {
            return;
        }
        if (mensaje == null || mensaje.isBlank()) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        } else {
            errorLabel.setText(mensaje);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void navegar(String seccion) {
        if (tablaProximos.getScene() == null) {
            return;
        }
        NavigationShell.navigate(tablaProximos.getScene(), seccion);
    }
}
