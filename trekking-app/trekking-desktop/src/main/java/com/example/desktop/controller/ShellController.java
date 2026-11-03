package com.example.desktop.controller;

import com.example.desktop.util.NavigationShell;
import com.example.desktop.util.SidebarIcon;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Contenido del shell de navegación lateral.
 *
 * Estructura en {@code shell.fxml}. Aquí solo vive el comportamiento:
 * sección activa y contenido central. La carga de páginas se delega
 * a {@link NavigationShell}.
 */
public class ShellController implements Initializable {

    @FXML
    private StackPane contentContainer;

    @FXML
    private Label sidebarBrand;

    @FXML
    private HBox breadcrumbBar;

    @FXML
    private VBox navigationBox;

    @FXML
    private VBox appSidebar;

    @FXML
    private Button btnToggleSidebar;

    @FXML
    private Button btnRutas;

    @FXML
    private Button btnInventario;

    @FXML
    private Button btnClientes;

    @FXML
    private Button btnSalidas;

    private String activeSection = "trips";
    private boolean sidebarCollapsed;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        sidebarBrand.setGraphic(SidebarIcon.load("MOUNTAIN-SNOW.svg"));
        sidebarBrand.setAccessibleText("TrekRisk");
        btnSalidas.setGraphic(SidebarIcon.load("SALIDAS.svg"));
        btnRutas.setGraphic(SidebarIcon.load("RUTAS.svg"));
        btnInventario.setGraphic(SidebarIcon.load("INVENTARIO.svg"));
        btnClientes.setGraphic(SidebarIcon.load("CLIENTES.svg"));
        updateSidebarState();
        refreshActiveStyles();
    }

    @FXML
    private void onToggleSidebar() {
        sidebarCollapsed = !sidebarCollapsed;
        updateSidebarState();
    }

    private void updateSidebarState() {
        if (appSidebar == null) return;

        double width = sidebarCollapsed ? 64.0 : 230.0;
        appSidebar.setMinWidth(width);
        appSidebar.setPrefWidth(width);
        appSidebar.setMaxWidth(width);
        appSidebar.getStyleClass().remove("app-sidebar-collapsed");
        if (sidebarCollapsed) appSidebar.getStyleClass().add("app-sidebar-collapsed");
        sidebarBrand.setText(sidebarCollapsed ? "" : "TrekRisk");

        setNavigationButtonState(btnSalidas, "Salidas");
        setNavigationButtonState(btnRutas, "Rutas");
        setNavigationButtonState(btnInventario, "Inventario");
        setNavigationButtonState(btnClientes, "Clientes");

        String toggleText = sidebarCollapsed ? "Expandir menú" : "Contraer menú";
        btnToggleSidebar.setGraphic(SidebarIcon.load(
                sidebarCollapsed ? "LEFT-PANE-OPEN.svg" : "LEFT-PANE-CLOSE.svg"));
        btnToggleSidebar.setTooltip(new Tooltip(toggleText));
        btnToggleSidebar.setAccessibleText(toggleText);
        btnToggleSidebar.setAccessibleHelp(toggleText);
    }

    private void setNavigationButtonState(Button button, String label) {
        if (button == null) return;
        button.setText(sidebarCollapsed ? "" : label);
        button.setTooltip(sidebarCollapsed ? new Tooltip(label) : null);
        button.setAccessibleText(label);
        button.setAccessibleHelp(label);
    }

    public void setActiveSection(String section) {
        this.activeSection = section;
        refreshActiveStyles();
    }

    public String getActiveSection() {
        return activeSection;
    }

    public void setContent(Parent page) {
        if (page != null) {
            contentContainer.getChildren().setAll(page);
            NavigationShell.renderBreadcrumbs(this, page.getId());
        } else {
            contentContainer.getChildren().clear();
            breadcrumbBar.getChildren().clear();
            breadcrumbBar.setManaged(false);
            breadcrumbBar.setVisible(false);
        }
    }

    public HBox getBreadcrumbBar() { return breadcrumbBar; }

    @FXML
    private void onSalidas() {
        navigate("trips");
    }

    @FXML
    private void onRutas() {
        navigate("routes");
    }

    @FXML
    private void onInventario() {
        navigate("inventory");
    }

    @FXML
    private void onClientes() {
        navigate("clients");
    }

    @FXML
    private void navigate(String section) {
        Scene scene = contentContainer.getScene();
        if (scene == null) {
            setActiveSection(section);
            return;
        }
        NavigationShell.navigate(scene, section);
    }

    private void refreshActiveStyles() {
        if (navigationBox == null) {
            return;
        }
        applyActive(btnSalidas, "trips");
        applyActive(btnRutas, "routes");
        applyActive(btnInventario, "inventory");
        applyActive(btnClientes, "clients");
    }

    private void applyActive(Button button, String section) {
        if (button == null) {
            return;
        }
        button.getStyleClass().remove("sidebar-button-active");
        if (section.equals(activeSection)
                && !button.getStyleClass().contains("sidebar-button-active")) {
            button.getStyleClass().add("sidebar-button-active");
        }
    }
}
