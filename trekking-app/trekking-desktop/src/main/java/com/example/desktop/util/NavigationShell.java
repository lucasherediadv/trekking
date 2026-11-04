package com.example.desktop.util;


import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import com.example.desktop.controller.ShellController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * Shell de navegación lateral de la aplicación.
 *
 * Estructura en {@code shell.fxml} vía {@link ShellController}.
 * Aquí solo queda la gestión de estado y navegación entre secciones.
 */
public final class NavigationShell {

    private static final String STATE_KEY =
            NavigationShell.class.getName() + ".state";
    private static final String STYLESHEET =
            "/com/example/fxml/styles.css";
    private static final String NAVIGATION_GUARD_KEY =
            NavigationShell.class.getName() + ".navigationGuard";

    private static final List<NavigationItem> ITEMS = List.of(
            new NavigationItem("Salidas", "trips", "salidas.fxml"),
            new NavigationItem("Rutas", "routes", "rutas.fxml"),
            new NavigationItem("Clientes", "clients", "lista-clientes.fxml"),
            new NavigationItem("Inventario", "inventory", "equipamiento.fxml"));

    private static final Map<String, Screen> SCREENS = Map.ofEntries(
            Map.entry("salidas", new Screen("Salidas", "trips", "salidas.fxml", List.of())),
            Map.entry("dashboard", new Screen("Salidas", "trips", "salidas.fxml", List.of())),
            Map.entry("rutas", new Screen("Rutas", "routes", "rutas.fxml", List.of("Salidas", "Rutas"))),
            Map.entry("clientes", new Screen("Clientes", "clients", "lista-clientes.fxml", List.of("Salidas", "Clientes"))),
            Map.entry("inventario", new Screen("Inventario", "inventory", "equipamiento.fxml", List.of("Salidas", "Inventario"))),
            Map.entry("salida-alta", new Screen("Alta de Salida", "trips", "salidas.fxml", List.of("Salidas", "Alta de Salida"))),
            Map.entry("salida-edicion", new Screen("Edición de Salida", "trips", "salidas.fxml", List.of("Salidas", "Editar salida"))),
            Map.entry("cliente-alta", new Screen("Alta de Cliente", "clients", "agregar-cliente.fxml", List.of("Clientes", "Nuevo cliente"))),
            Map.entry("cliente-edicion", new Screen("Edición de Cliente", "clients", "editar-cliente.fxml", List.of("Clientes", "Editar cliente"))),
            Map.entry("ruta-alta", new Screen("Alta de Ruta", "routes", "ruta-form.fxml", List.of("Rutas", "Nueva ruta"))),
            Map.entry("ruta-edicion", new Screen("Edición de Ruta", "routes", "ruta-form.fxml", List.of("Rutas", "Editar ruta"))),
            Map.entry("itinerario-ruta", new Screen("Itinerario", "routes", "itinerario-ruta.fxml", List.of("Salidas", "Rutas", "Itinerario"))),
            Map.entry("checkpoint-ruta-alta", new Screen("Nuevo Checkpoint", "routes", "checkpoint-form.fxml", List.of("Salidas", "Rutas", "Nuevo Checkpoint"))),
            Map.entry("checkpoint-ruta-edicion", new Screen("Editar Checkpoint", "routes", "checkpoint-form.fxml", List.of("Salidas", "Rutas", "Editar Checkpoint"))),
            Map.entry("inventario-alta", new Screen("Alta de Elemento", "inventory", "equipamiento-form.fxml", List.of("Inventario", "Nuevo elemento"))),
            Map.entry("inventario-edicion", new Screen("Edición de Elemento", "inventory", "equipamiento-form.fxml", List.of("Inventario", "Editar elemento"))));

    private NavigationShell() {
    }

    public static void install(Scene scene) {
        install(scene, "trips");
    }

    public static void install(Scene scene, String activeSection) {
        Parent currentRoot = scene.getRoot();
        if (currentRoot != null && currentRoot.getStyleClass().contains("app-shell")) {
            Object existing = scene.getProperties().get(STATE_KEY);
            if (existing instanceof ShellController controller) {
                controller.setActiveSection(activeSection);
            }
            return;
        }
        addStylesheet(scene);
        try {
            FXMLLoader loader = new FXMLLoader(
                    NavigationShell.class.getResource("/com/example/fxml/shell.fxml"));
            Parent shell = loader.load();
            ShellController controller = loader.getController();
            controller.setActiveSection(activeSection);
            controller.setContent(currentRoot);
            scene.getProperties().put(STATE_KEY, controller);
            scene.setRoot(shell);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("No se pudo instalar la navegación lateral.", e);
        }
    }

    public static void setRoot(Scene scene, Parent page) {
        Object value = scene.getProperties().get(STATE_KEY);
        if (!(value instanceof ShellController controller)) {
            throw new IllegalStateException(
                    "La navegación lateral no está instalada en esta ventana.");
        }
        if ("dashboard".equals(page.getId())) {
            navigate(scene, "trips");
        } else {
            controller.setContent(page);
        }
    }

    public static void setRoot(Scene scene, Parent page, String screenId) {
        page.setId(screenId);
        setRoot(scene, page);
    }

    public static void renderBreadcrumbs(ShellController controller, String screenId) {
        HBox bar = controller.getBreadcrumbBar();
        bar.getChildren().clear();
        Screen screen = SCREENS.get(screenId);
        boolean visible = screen != null && !screen.crumbs().isEmpty();
        bar.setVisible(visible);
        bar.setManaged(visible);
        if (!visible) return;
        for (int i = 0; i < screen.crumbs().size(); i++) {
            if (i > 0) {
                Label chevron = new Label("›");
                chevron.getStyleClass().add("breadcrumb-chevron");
                bar.getChildren().add(chevron);
            }
            String crumb = screen.crumbs().get(i);
            boolean current = i == screen.crumbs().size() - 1;
            if (current) {
                Label label = new Label(crumb);
                label.getStyleClass().add("breadcrumb-current");
                bar.getChildren().add(label);
            } else {
                Button link = new Button(crumb);
                link.getStyleClass().add("breadcrumb-link");
                int destination = i;
                link.setOnAction(event -> navigateToCrumb(controller, screen, destination));
                bar.getChildren().add(link);
            }
        }
    }

    private static void navigateToCrumb(ShellController controller, Screen current, int crumbIndex) {
        Scene scene = controller.getBreadcrumbBar().getScene();
        Object guard = scene.getProperties().get(NAVIGATION_GUARD_KEY);
        if (guard instanceof BooleanSupplier canLeave && !canLeave.getAsBoolean()) return;
        String crumb = current.crumbs().get(crumbIndex);
        String targetId;
        if (crumb.equals("Clientes")) targetId = "clientes";
        else if (crumb.equals("Rutas")) targetId = "rutas";
        else if (crumb.equals("Inventario")) targetId = "inventario";
        else if (crumbIndex == 0) targetId = "salidas";
        else if (crumbIndex == 1) targetId = current.section().equals("routes") ? "rutas"
                : current.section().equals("clients") ? "clientes" : "inventario";
        else targetId = crumb.equals("Listado") ? (current.section().equals("routes") ? "rutas"
                : current.section().equals("clients") ? "clientes" : "inventario") : "salidas";
        Screen target = SCREENS.get(targetId);
        loadScreen(scene, controller, target);
    }

    public static void navigate(Scene scene, String section) {
        NavigationItem target = ITEMS.stream()
                .filter(item -> item.section().equals(section))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sección desconocida: " + section));
        Object value = scene.getProperties().get(STATE_KEY);
        ShellController controller = value instanceof ShellController sc ? sc : null;
        if (controller == null) {
            install(scene, section);
            value = scene.getProperties().get(STATE_KEY);
            if (!(value instanceof ShellController sc2)) {
                throw new IllegalStateException(
                        "La navegación lateral no está instalada en esta ventana.");
            }
            controller = sc2;
        }
        Object guard = scene.getProperties().get(NAVIGATION_GUARD_KEY);
        if (guard instanceof BooleanSupplier canLeave && !canLeave.getAsBoolean()) return;
        Screen screen = SCREENS.get(switch (target.section()) {
            case "trips" -> "salidas";
            case "routes" -> "rutas";
            case "clients" -> "clientes";
            default -> "inventario";
        });
        loadScreen(scene, controller, screen);
    }

    private static void loadScreen(Scene scene, ShellController controller, Screen target) {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationShell.class.getResource("/com/example/fxml/" + target.fxml()));
            Parent page = loader.load();
            page.setId(screenId(target));
            controller.setActiveSection(target.section());
            controller.setContent(page);
        } catch (IOException | RuntimeException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("No se pudo abrir la pantalla");
            alert.setHeaderText(null);
            alert.setContentText("No se pudo abrir " + target.label() + ": " + e.getMessage());
            AlertUtils.showAndWait(alert, scene.getWindow());
        }
    }

    private static String screenId(Screen screen) {
        return SCREENS.entrySet().stream().filter(entry -> entry.getValue() == screen)
                .map(Map.Entry::getKey).findFirst().orElse("unknown");
    }

    /** Registers a page-specific check before replacing the central shell content. */
    public static void setNavigationGuard(Scene scene, BooleanSupplier guard) {
        if (guard == null) scene.getProperties().remove(NAVIGATION_GUARD_KEY);
        else scene.getProperties().put(NAVIGATION_GUARD_KEY, guard);
    }

    private static void addStylesheet(Scene scene) {
        URL stylesheet = NavigationShell.class.getResource(STYLESHEET);
        if (stylesheet != null
                && !scene.getStylesheets().contains(stylesheet.toExternalForm())) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
    }

    private record NavigationItem(String label, String section, String fxml) {
    }

    private record Screen(String label, String section, String fxml, List<String> crumbs) { }
}
