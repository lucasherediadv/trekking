package com.example.desktop.controller;

import com.example.desktop.dao.RutaDAO;
import com.example.desktop.Servicios;
import com.example.desktop.dao.SalidaDAO;
import com.example.desktop.service.FichaOperativaPDFService;
import com.example.desktop.model.Ruta;
import com.example.desktop.model.Salida;
import com.example.core.itinerario.Checkpoint;
import com.example.desktop.model.Equipamiento;
import com.example.desktop.model.AsignacionEquipamiento;
import com.example.desktop.dao.EquipamientoDAO;
import com.example.core.cliente.Cliente;
import com.example.desktop.model.AsignacionParticipante;
import com.example.desktop.util.NavigationShell;
import com.example.desktop.util.SearchFieldIcon;
import com.example.desktop.util.TableEmptyState;
import com.example.desktop.util.TableHeight;
import com.example.desktop.util.ComboBoxDisplayLabel;
import com.example.desktop.util.AlertUtils;
import com.example.desktop.util.FechaFormato;
import com.example.desktop.util.FormValidation;
import com.example.desktop.util.RowActionButton;
import com.example.desktop.util.ToastNotifications;
import java.sql.SQLException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Locale;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.Node;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

public class SalidasController {
    private final SalidaDAO salidaDAO = new SalidaDAO();
    private final FichaOperativaPDFService fichaPDFService = new FichaOperativaPDFService();
    private final RutaDAO rutaDAO = new RutaDAO();
    @FXML private TableView<Salida> tablaSalidas;
    @FXML private TableColumn<Salida, LocalDate> colFecha;
    @FXML private TableColumn<Salida, String> colRuta;
    @FXML private TableColumn<Salida, String> colEstado;
    @FXML private TableColumn<Salida, String> colParticipantes;
    @FXML private TableColumn<Salida, String> colEquipamiento;
    @FXML private TableColumn<Salida, Salida> colAcciones;
    private Label kpiProximas, kpiRutas, kpiClientes, kpiInventario;
    @FXML private Button tarjetaProximas, tarjetaRutas, tarjetaClientes, tarjetaInventario;
    @FXML private TextField buscarField;
    @FXML private ComboBox<String> estadoFiltro;
    @FXML private Button btnRealizada, btnCancelar;
    private Map<Integer, SalidaDAO.ResumenOperativo> resumenes = Map.of();
    private List<Salida> salidasOriginales = List.of();

    @FXML private void initialize() {
        javafx.application.Platform.runLater(buscarField::requestFocus);
        configurarTarjetasIndicadores();
        SearchFieldIcon.install(buscarField);
        tablaSalidas.getStyleClass().add("listing-table");
        tablaSalidas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colFecha.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().fecha()));
        colFecha.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(LocalDate fecha, boolean empty) {
                super.updateItem(fecha, empty);
                setText(empty || fecha == null ? null : FechaFormato.formatear(fecha));
            }
        });
        colRuta.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().rutaNombre()));
        colEstado.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().estado()));
        colEstado.setCellFactory(column -> com.example.desktop.util.EstadoCell.<Salida, String>create(estado -> switch (estado) {
            case "planificada" -> com.example.desktop.util.EstadoCell.Tono.INFORMACION;
            case "realizada" -> com.example.desktop.util.EstadoCell.Tono.EXITO;
            case "cancelada" -> com.example.desktop.util.EstadoCell.Tono.PELIGRO;
            default -> null;
        }, estado -> estado.isEmpty() ? estado : Character.toUpperCase(estado.charAt(0)) + estado.substring(1)));
        colParticipantes.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(resumenParticipantes(data.getValue())));
        colEquipamiento.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(resumenEquipamiento(data.getValue())));
        colAcciones.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        colAcciones.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(Salida salida, boolean empty) {
                super.updateItem(salida, empty);
                if (empty || salida == null) { com.example.desktop.util.RowActionCell.clear(this); return; }
                javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(3);
                actions.getChildren().addAll(
                        com.example.desktop.util.RowActionButton.create("Editar salida", com.example.desktop.util.RowActionButton.Icon.EDIT, e -> { tablaSalidas.getSelectionModel().select(salida); editarSalida(); }),
                        com.example.desktop.util.RowActionButton.create("Descargar ficha operativa PDF", com.example.desktop.util.RowActionButton.Icon.DOWNLOAD, e -> { tablaSalidas.getSelectionModel().select(salida); generarFichaPDF(); }));
                if ("planificada".equals(salida.estado())) {
                    javafx.scene.control.Button cancelar = com.example.desktop.util.RowActionButton.create("Cancelar salida", com.example.desktop.util.RowActionButton.Icon.CANCEL, e -> { tablaSalidas.getSelectionModel().select(salida); cancelarSalida(); });
                    cancelar.getStyleClass().add("row-action-danger");
                    actions.getChildren().addAll(
                            com.example.desktop.util.RowActionButton.create("Marcar realizada", com.example.desktop.util.RowActionButton.Icon.CHECK, e -> { tablaSalidas.getSelectionModel().select(salida); marcarRealizada(); }), cancelar);
                }
                com.example.desktop.util.RowActionCell.showCentered(this, actions);
            }
        });
        estadoFiltro.setItems(FXCollections.observableArrayList(
                "Todos", "Planificada", "Realizada", "Cancelada"));
        ComboBoxDisplayLabel.prefixValue(estadoFiltro, "Estado");
        estadoFiltro.setValue("Todos");
        actualizarIndicadores(); actualizarTabla();
        tablaSalidas.getSelectionModel().selectedItemProperty().addListener((obs, anterior, seleccionada) -> actualizarAcciones(seleccionada));
        actualizarAcciones(null);
        buscarField.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltros());
        estadoFiltro.valueProperty().addListener((obs, oldValue, newValue) -> aplicarFiltros());
        tablaSalidas.setRowFactory(table -> new TableRow<>() {
            @Override protected void updateItem(Salida salida, boolean empty) {
                super.updateItem(salida, empty); getStyleClass().remove("salida-pendiente-vencida");
                if (!empty && salida != null && "planificada".equals(salida.estado()) && salida.fecha().isBefore(LocalDate.now()))
                    getStyleClass().add("salida-pendiente-vencida");
            }
        });
    }

    private void configurarTarjetasIndicadores() {
        kpiProximas = new Label("0");
        kpiRutas = new Label("0");
        kpiClientes = new Label("0");
        kpiInventario = new Label("0");
        configurarTarjeta(tarjetaProximas, "SALIDAS.svg", "Salidas próximas", kpiProximas,
                "Planificadas; hoy y próximos 7 días (inclusive)");
        configurarTarjeta(tarjetaRutas, "RUTAS.svg", "Rutas activas", kpiRutas,
                "En estado activo");
        configurarTarjeta(tarjetaClientes, "CLIENTES.svg", "Clientes activos", kpiClientes,
                "En estado activo");
        configurarTarjeta(tarjetaInventario, null, "Inventario crítico", kpiInventario,
                "Disponible ≤ 2 o en reparación, fuera de servicio o extraviado");
    }

    private void configurarTarjeta(Button tarjeta, String archivoIcono, String titulo,
            Label valor, String descripcion) {
        Node icono;
        if (archivoIcono == null) {
            Label alerta = new Label("⚠");
            alerta.getStyleClass().add("kpi-action-alert-icon");
            icono = alerta;
        } else {
            icono = com.example.desktop.util.SidebarIcon.load(archivoIcono);
        }
        icono.getStyleClass().add("kpi-action-icon");
        icono.setMouseTransparent(true);
        Label encabezado = new Label(titulo);
        encabezado.getStyleClass().add("kpi-action-title");
        HBox filaEncabezado = new HBox(8, icono, encabezado);
        filaEncabezado.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        valor.getStyleClass().setAll("kpi-action-value");
        Label detalle = new Label(descripcion);
        detalle.getStyleClass().add("kpi-action-description");
        detalle.setWrapText(true);
        VBox contenido = new VBox(7, filaEncabezado, valor, detalle);
        contenido.setFillWidth(true);
        tarjeta.setGraphic(contenido);
        tarjeta.setMaxWidth(Double.MAX_VALUE);
        tarjeta.setPrefHeight(154);
    }

    @FXML private void irASalidas() { navegarAIndicador("trips"); }
    @FXML private void irARutas() { navegarAIndicador("routes"); }
    @FXML private void irAClientes() { navegarAIndicador("clients"); }
    @FXML private void irAInventario() { navegarAIndicador("inventory"); }

    private void navegarAIndicador(String seccion) {
        Scene escena = tarjetaProximas.getScene();
        if (escena != null) NavigationShell.navigate(escena, seccion);
    }

    @FXML private void iniciarWizard() { new WizardSalida().mostrar(); }
    @FXML private void editarSalida() {
        Salida seleccionada = tablaSalidas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) { aviso(Alert.AlertType.INFORMATION, "Selecciona una Salida", "Selecciona la Salida que quieres editar."); return; }
        new EditorSalida(seleccionada).mostrar();
    }
    @FXML private void marcarRealizada() { cerrar("realizada"); }
    @FXML private void cancelarSalida() { cerrar("cancelada"); }

    @FXML private void generarFichaPDF() {
        Salida salida = tablaSalidas.getSelectionModel().getSelectedItem();
        if (salida == null) {
            aviso(Alert.AlertType.INFORMATION, "Selecciona una Salida", "Selecciona la Salida cuya ficha quieres generar.");
            return;
        }
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar ficha operativa");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));
        selector.setInitialFileName(fichaPDFService.nombreArchivo(salida));
        java.io.File archivo = selector.showSaveDialog(tablaSalidas.getScene().getWindow());
        if (archivo == null) return;
        try {
            fichaPDFService.exportar(salida.id(), archivo.toPath());
            ToastNotifications.show(tablaSalidas.getScene().getWindow(), "Ficha operativa guardada", ToastNotifications.Kind.SUCCESS);
        } catch (SQLException | IOException e) {
            aviso(Alert.AlertType.ERROR, "No se pudo generar la ficha", e.getMessage());
        }
    }

    private void cerrar(String estado) {
        Salida salida = tablaSalidas.getSelectionModel().getSelectedItem();
        if (salida == null) { aviso(Alert.AlertType.INFORMATION, "Selecciona una Salida", "Selecciona una Salida planificada para continuar."); return; }
        if (!"planificada".equals(salida.estado())) {
            aviso(Alert.AlertType.INFORMATION, "Salida ya cerrada", "Solo se pueden cerrar Salidas planificadas."); return;
        }
        String accion = "realizada".equals(estado) ? "marcar como realizada" : "cancelar";
        Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Quieres " + accion + " la Salida " + salida.id() + "? Su reserva de stock se liberará y el estado no se podrá revertir desde el flujo normal.",
                ButtonType.CANCEL, ButtonType.OK);
        confirmar.setTitle("Cerrar Salida"); confirmar.setHeaderText("Confirmar cierre");
        if (AlertUtils.showAndWait(confirmar, tablaSalidas).orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try { salidaDAO.cambiarEstado(salida.id(), estado); actualizarIndicadores(); actualizarTabla(); }
        catch (SQLException e) { aviso(Alert.AlertType.ERROR, "No se pudo actualizar la Salida", e.getMessage()); }
    }

    private void actualizarAcciones(Salida seleccionada) {
        boolean planificada = seleccionada != null && "planificada".equals(seleccionada.estado());
        if (btnRealizada != null) btnRealizada.setDisable(!planificada);
        if (btnCancelar != null) btnCancelar.setDisable(!planificada);
    }

    private String resumenParticipantes(Salida salida) {
        SalidaDAO.ResumenOperativo resumen = resumenes.get(salida.id());
        return resumen == null ? "—" : textoResumen(resumen.participantes());
    }

    private String resumenEquipamiento(Salida salida) {
        SalidaDAO.ResumenOperativo resumen = resumenes.get(salida.id());
        return resumen == null ? "—" : textoResumen(resumen.equipamiento());
    }

    private String textoResumen(String texto) {
        return texto == null || texto.isBlank() ? "—" : texto;
    }

    private void actualizarTabla() {
        try {
            resumenes = salidaDAO.obtenerResumenesOperativos();
            salidasOriginales = salidaDAO.obtenerTodas();
            FilteredList<Salida> filas = new FilteredList<>(FXCollections.observableArrayList(salidasOriginales), s -> true);
            tablaSalidas.setItems(filas); aplicarFiltros();
        } catch (RuntimeException e) { aviso(Alert.AlertType.ERROR, "No se pudieron cargar las Salidas", e.getMessage()); }
    }
    private void aplicarFiltros() {
        if (!(tablaSalidas.getItems() instanceof FilteredList<?> raw)) return;
        @SuppressWarnings("unchecked") FilteredList<Salida> filas = (FilteredList<Salida>) raw;
        String consulta = buscarField == null || buscarField.getText() == null ? "" : buscarField.getText().trim().toLowerCase(Locale.ROOT);
        String estadoSeleccionado = estadoFiltro == null ? "Todos" : estadoFiltro.getValue();
        String estado = switch (estadoSeleccionado == null ? "Todos" : estadoSeleccionado) {
            case "Planificada" -> "planificada";
            case "Realizada" -> "realizada";
            case "Cancelada" -> "cancelada";
            default -> null;
        };
        filas.setPredicate(s -> (consulta.isEmpty() || s.rutaNombre().toLowerCase(Locale.ROOT).contains(consulta)
                        || contieneCliente(s, consulta))
                && (estado == null || s.estado().equals(estado)));
        boolean filtrosActivos = !consulta.isEmpty() || estado != null;
        boolean haySalidas = !salidasOriginales.isEmpty();
        TableEmptyState.update(tablaSalidas, haySalidas, filtrosActivos,
                "Todavía no hay Salidas", "Planifica una Salida para comenzar a organizar una actividad.",
                "No hay Salidas que coincidan", "Prueba con otra Ruta, Cliente o estado.",
                "EVENTBUSY",
                "+ Nueva Salida", event -> iniciarWizard());
        TableHeight.update(tablaSalidas, filas.size(), 500);
    }

    private boolean contieneCliente(Salida salida, String consulta) {
        SalidaDAO.ResumenOperativo resumen = resumenes.get(salida.id());
        return resumen != null && resumen.participantes() != null
                && resumen.participantes().toLowerCase(Locale.ROOT).contains(consulta);
    }
    private void actualizarIndicadores() {
        try {
            var indicadores = salidaDAO.obtenerIndicadores();
            kpiProximas.setText(String.valueOf(indicadores.get("proximas"))); kpiRutas.setText(String.valueOf(indicadores.get("rutas")));
            kpiClientes.setText(String.valueOf(indicadores.get("clientes"))); kpiInventario.setText(String.valueOf(indicadores.get("criticos")));
            tarjetaProximas.setAccessibleText("Salidas próximas: " + kpiProximas.getText()
                    + ". Planificadas; hoy y próximos 7 días inclusive. Activar para abrir Salidas.");
            tarjetaRutas.setAccessibleText("Rutas activas: " + kpiRutas.getText()
                    + ". En estado activo. Activar para abrir Rutas.");
            tarjetaClientes.setAccessibleText("Clientes activos: " + kpiClientes.getText()
                    + ". En estado activo. Activar para abrir Clientes.");
            tarjetaInventario.setAccessibleText("Inventario crítico: " + kpiInventario.getText()
                    + ". Disponible ≤ 2 o en reparación, fuera de servicio o extraviado. Activar para abrir Inventario.");
        } catch (RuntimeException e) { aviso(Alert.AlertType.ERROR, "No se pudieron cargar los indicadores", e.getMessage()); }
    }
    private void aviso(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo); alert.setTitle(titulo); alert.setHeaderText(null);
        alert.setContentText(mensaje == null ? "Ocurrió un error inesperado." : mensaje); AlertUtils.showAndWait(alert, tablaSalidas);
    }

    private final class EditorSalida {
        private final Salida original;
        private final Scene escenaAplicacion = tablaSalidas.getScene();
        private static final double ALTO_FILA_LISTA = 40;
        private final DatePicker fecha = new DatePicker();
        private final ComboBox<Ruta> rutas = new ComboBox<>();
        private final ListView<Checkpoint> puntos = new ListView<>();
        private final List<Checkpoint> checkpoints = new ArrayList<>();
        private final Label detalleRuta = new Label();
        private boolean actualizarDesdeRuta;
        private final List<Checkpoint> checkpointsGuardarEnRuta = new ArrayList<>();
        private final List<AsignacionEquipamiento> equipamiento = new ArrayList<>();
        private final List<AsignacionParticipante> participantes = new ArrayList<>();
        private final java.util.Set<Integer> refrescarEquipamiento = new java.util.HashSet<>();
        private final java.util.Set<Integer> refrescarParticipantes = new java.util.HashSet<>();
        private final ComboBox<Equipamiento> selectorEquipo = new ComboBox<>();
        private final TextField cantidadEquipo = new TextField("1");
        private final ListView<AsignacionEquipamiento> listaEquipo = new ListView<>();
        private final ComboBox<Cliente> selectorCliente = new ComboBox<>();
        private final ListView<AsignacionParticipante> listaParticipantes = new ListView<>();
        private final java.util.Set<Integer> equiposActivos = new java.util.HashSet<>();
        private boolean cambiosRealizados;

        EditorSalida(Salida original) { this.original = original; }

        void mostrar() {
            try {
                checkpoints.addAll(salidaDAO.obtenerCheckpoints(original.id()));
                equipamiento.addAll(salidaDAO.obtenerEquipamiento(original.id()));
                participantes.addAll(salidaDAO.obtenerParticipantes(original.id()));
                rutas.setItems(FXCollections.observableArrayList(rutaDAO.obtenerTodasIncluyendoInactivas()));
            } catch (SQLException e) {
                aviso(Alert.AlertType.ERROR, "No se pudo abrir la Salida", e.getMessage()); return;
            }
            rutas.setConverter(new StringConverter<>() { public String toString(Ruta r) { return r == null ? "" : r.getNombre(); } public Ruta fromString(String s) { return null; } });
            FechaFormato.configurar(fecha);
            fecha.setValue(original.fecha());
            fecha.valueProperty().addListener((o, anterior, nueva) -> {
                if (!java.util.Objects.equals(anterior, nueva)) cambiosRealizados = true;
            });
            rutas.getItems().stream().filter(r -> r.getId() == original.rutaId()).findFirst().ifPresent(rutas::setValue);
            rutas.valueProperty().addListener((o, a, b) -> {
                if (b != null && b.getId() != original.rutaId() && !rutaDAO.estaActiva(b.getId())) {
                    rutas.setValue(a); aviso(Alert.AlertType.WARNING, "Ruta inactiva", "No se puede cambiar a una Ruta inactiva."); return;
                }
                if (b != null && a != null && b.getId() != a.getId()) {
                    cambiosRealizados = true;
                    actualizarDesdeRuta = false;
                    checkpointsGuardarEnRuta.clear();
                    checkpoints.clear(); checkpoints.addAll(Servicios.itinerarios().obtener(b.getId()).checkpoints()); actualizarPuntos();
                }
                actualizarDetalle();
            });
            detalleRuta.setWrapText(true);
            detalleRuta.getStyleClass().add("salida-ruta-resumen");
            configurarListas();
            configurarAsignaciones();
            actualizarDetalle(); actualizarPuntos();

            VBox campoFecha = campo("Fecha de la Salida *", fecha);
            VBox campoRuta = campo("Ruta *", rutas);
            FormValidation.watch(fecha, fecha.valueProperty(),
                    () -> fecha.getValue() == null ? "La fecha de la Salida es obligatoria." : null);
            FormValidation.watch(rutas, rutas.valueProperty(),
                    () -> rutas.getValue() == null ? "Selecciona una Ruta." : null);
            fecha.setPromptText("Selecciona una fecha");
            rutas.setPromptText("Seleccione una Ruta");

            Button agregarCheckpoint = botonConIcono("Agregar checkpoint", RowActionButton.Icon.ADD, () -> editarCheckpoint(null));
            Button refrescar = botonConIcono("Actualizar desde Ruta", RowActionButton.Icon.RESTORE, this::actualizarCopiaDesdeRuta);
            Label ayudaCheckpoints = new Label("Es la copia operativa de la Salida y admite cualquier cantidad de checkpoints, incluso ninguno.");
            ayudaCheckpoints.getStyleClass().add("field-help");
            ayudaCheckpoints.setWrapText(true);

            VBox seccionDatos = seccion(false, encabezadoSeccion("Datos de la Salida"), campoFecha);
            VBox seccionRuta = seccion(false, encabezadoSeccion("Ruta e itinerario", agregarCheckpoint, refrescar),
                    campoRuta, detalleRuta, ayudaCheckpoints, puntos);
            VBox seccionEquipo = seccion(false, encabezadoSeccion("Equipamiento"),
                    filaAgregado(campo("Elemento", selectorEquipo), campo("Cantidad", cantidadEquipo),
                            botonAgregar(this::agregarEquipo)),
                    listaEquipo);
            VBox seccionParticipantes = seccion(true, encabezadoSeccion("Participantes"),
                    filaAgregado(campo("Cliente", selectorCliente), botonAgregar(this::agregarParticipante)),
                    listaParticipantes);

            Button guardar = new Button("Guardar cambios");
            guardar.getStyleClass().add("primary-button");
            guardar.setOnAction(e -> guardar());
            Button cancelar = new Button("Cancelar");
            cancelar.getStyleClass().add("secondary-button");
            cancelar.setOnAction(e -> cancelarEdicion());
            VBox pie = new VBox();
            pie.getStyleClass().add("crud-actions");

            VBox tarjeta = new VBox(seccionDatos, seccionRuta, seccionEquipo, seccionParticipantes, pie);
            tarjeta.getStyleClass().add("form-card");
            tarjeta.widthProperty().addListener((o, a, ancho) -> disponerAcciones(pie, ancho.doubleValue() < 480, guardar, cancelar));
            disponerAcciones(pie, false, guardar, cancelar);

            Label titulo = new Label("Editar salida");
            titulo.getStyleClass().add("dialog-header-title");
            Label subtitulo = new Label("Modifica la fecha, la Ruta, el equipamiento y los Participantes de la Salida.");
            subtitulo.getStyleClass().add("dialog-header-subtitle");
            subtitulo.setWrapText(true);
            VBox contenido = new VBox(titulo, subtitulo, tarjeta);
            contenido.getStyleClass().add("form-content");
            VBox pagina = new VBox(contenido);
            pagina.setAlignment(Pos.TOP_CENTER);
            pagina.getStyleClass().add("form-page");
            ScrollPane scroll = new ScrollPane(pagina);
            scroll.setFitToWidth(true);
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scroll.getStyleClass().add("pantalla-scroll");
            NavigationShell.setNavigationGuard(escenaAplicacion, this::confirmarCambioDeSeccion);
            NavigationShell.setRoot(escenaAplicacion, scroll, "salida-edicion");
            javafx.application.Platform.runLater(fecha::requestFocus);
        }

        private VBox campo(String etiqueta, Control control) {
            Label label = new Label(etiqueta);
            label.getStyleClass().add("form-label");
            label.setLabelFor(control);
            control.setMaxWidth(Double.MAX_VALUE);
            VBox campo = new VBox(label, control);
            campo.getStyleClass().add("form-field");
            return campo;
        }

        private VBox seccion(boolean ultima, Node... hijos) {
            VBox seccion = new VBox(hijos);
            seccion.getStyleClass().add("form-section");
            if (ultima) seccion.getStyleClass().add("form-section-last");
            return seccion;
        }

        private HBox encabezadoSeccion(String titulo, Node... acciones) {
            Label label = new Label(titulo);
            label.getStyleClass().add("form-section-title");
            label.setMinWidth(Region.USE_PREF_SIZE);
            HBox encabezado = new HBox(12, label);
            encabezado.setAlignment(Pos.CENTER_LEFT);
            if (acciones.length > 0) {
                FlowPane botones = new FlowPane(8, 8, acciones);
                botones.setAlignment(Pos.CENTER_RIGHT);
                HBox.setHgrow(botones, Priority.ALWAYS);
                encabezado.getChildren().add(botones);
            }
            return encabezado;
        }

        private Button botonConIcono(String texto, RowActionButton.Icon icono, Runnable accion) {
            Button boton = new Button(texto, RowActionButton.graphic(icono));
            boton.getStyleClass().add("secondary-button");
            boton.setGraphicTextGap(8);
            boton.setOnAction(e -> accion.run());
            return boton;
        }

        private Button botonAgregar(Runnable accion) {
            Button boton = new Button("Agregar");
            boton.getStyleClass().add("secondary-button");
            boton.setOnAction(e -> accion.run());
            return boton;
        }

        /** Campos y botón de agregado en una fila que se apila en un ancho estrecho. */
        private GridPane filaAgregado(Node... nodos) {
            GridPane fila = new GridPane();
            fila.getStyleClass().add("form-grid");
            ColumnConstraints principal = new ColumnConstraints();
            principal.setHgrow(Priority.ALWAYS);
            fila.getColumnConstraints().add(principal);
            boolean[] apilada = {false};
            disponerFila(fila, false, nodos);
            fila.widthProperty().addListener((o, a, ancho) -> {
                boolean estrecha = ancho.doubleValue() < 600;
                if (estrecha != apilada[0]) { apilada[0] = estrecha; disponerFila(fila, estrecha, nodos); }
            });
            return fila;
        }

        private void disponerFila(GridPane fila, boolean apilada, Node... nodos) {
            fila.getChildren().clear();
            fila.setHgap(apilada ? 0 : 16);
            fila.setVgap(12);
            for (int i = 0; i < nodos.length; i++) {
                Node nodo = nodos[i];
                GridPane.setValignment(nodo, VPos.BOTTOM);
                fila.add(nodo, apilada ? 0 : i, apilada ? i : 0);
            }
            if (nodos[nodos.length - 1] instanceof Button boton) {
                boton.setMaxWidth(apilada ? Double.MAX_VALUE : Region.USE_COMPUTED_SIZE);
            }
        }

        private void disponerAcciones(VBox pie, boolean estrecho, Button guardar, Button cancelar) {
            Pane contenedor = estrecho ? new VBox(8) : new HBox(8);
            if (contenedor instanceof HBox caja) caja.setAlignment(Pos.CENTER_RIGHT);
            for (Button boton : List.of(guardar, cancelar)) boton.setMaxWidth(estrecho ? Double.MAX_VALUE : Region.USE_COMPUTED_SIZE);
            contenedor.getChildren().setAll(guardar, cancelar);
            pie.getChildren().setAll(contenedor);
        }

        private void configurarListas() {
            for (ListView<?> lista : List.of(puntos, listaEquipo, listaParticipantes)) {
                lista.getStyleClass().add("salida-lista");
                lista.setFixedCellSize(ALTO_FILA_LISTA);
                lista.setFocusTraversable(false);
            }
            puntos.setPlaceholder(new Label("Sin checkpoints"));
            listaEquipo.setPlaceholder(new Label("Sin equipamiento asignado"));
            listaParticipantes.setPlaceholder(new Label("Sin participantes asignados"));
            puntos.setCellFactory(v -> new ListCell<>() { @Override protected void updateItem(Checkpoint cp, boolean empty) {
                super.updateItem(cp, empty);
                setText(null);
                if (empty || cp == null) { setGraphic(null); return; }
                Button editar = RowActionButton.create("Editar checkpoint " + cp.nombre(), RowActionButton.Icon.EDIT, e -> editarCheckpoint(cp));
                Button quitar = RowActionButton.create("Quitar checkpoint " + cp.nombre(), RowActionButton.Icon.DELETE, e -> quitarCheckpoint(cp));
                quitar.getStyleClass().add("row-action-danger");
                setGraphic(filaLista(List.of(etiquetaFila(cp.hora(), 56), etiquetaFila(cp.nombre(), -1), etiquetaFila(cp.altitud() + " m", 80)), editar, quitar));
            }});
            listaEquipo.setCellFactory(v -> new ListCell<>() { @Override protected void updateItem(AsignacionEquipamiento a, boolean empty) {
                super.updateItem(a, empty);
                setText(null);
                if (empty || a == null) { setGraphic(null); return; }
                Button quitar = RowActionButton.create("Quitar equipamiento " + a.equipamiento().getNombre(), RowActionButton.Icon.DELETE, e -> quitarEquipo(a));
                quitar.getStyleClass().add("row-action-danger");
                setGraphic(filaLista(List.of(etiquetaFila(a.equipamiento().getNombre(), -1), etiquetaFila("Cantidad: " + a.cantidad(), 110)), quitar));
            }});
            listaParticipantes.setCellFactory(v -> new ListCell<>() { @Override protected void updateItem(AsignacionParticipante a, boolean empty) {
                super.updateItem(a, empty);
                setText(null);
                if (empty || a == null) { setGraphic(null); return; }
                Cliente c = a.cliente();
                Button quitar = RowActionButton.create("Quitar participante " + c.apellido() + ", " + c.nombre(), RowActionButton.Icon.DELETE, e -> quitarParticipante(a));
                quitar.getStyleClass().add("row-action-danger");
                setGraphic(filaLista(List.of(etiquetaFila(c.apellido() + ", " + c.nombre(), -1), etiquetaFila("DNI " + c.dni(), 110)), quitar));
            }});
        }

        /** Etiqueta de una fila de lista; con ancho negativo ocupa el espacio sobrante. */
        private Label etiquetaFila(String texto, double ancho) {
            Label label = new Label(texto);
            label.setMinWidth(0);
            label.setTextOverrun(OverrunStyle.ELLIPSIS);
            if (ancho < 0) { label.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(label, Priority.ALWAYS); }
            else { label.setMinWidth(ancho); label.setPrefWidth(ancho); }
            return label;
        }

        private HBox filaLista(List<Label> datos, Button... acciones) {
            HBox fila = new HBox(8);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.getChildren().addAll(datos);
            HBox botones = new HBox(3, acciones);
            botones.setMinWidth(Region.USE_PREF_SIZE);
            fila.getChildren().add(botones);
            return fila;
        }

        private void ajustarAltura(ListView<?> lista, int filas) {
            lista.setPrefHeight(Math.max(1, filas) * ALTO_FILA_LISTA + 2);
        }

        private void quitarCheckpoint(Checkpoint cp) {
            cambiosRealizados = true;
            checkpoints.remove(cp);
            checkpointsGuardarEnRuta.remove(cp);
            actualizarPuntos();
        }

        private void actualizarCopiaDesdeRuta() {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Se reemplazarán los datos copiados de Ruta y todos sus Checkpoints. Se perderán las modificaciones locales de Checkpoints. Equipamiento y Participantes se conservarán.", ButtonType.CANCEL, ButtonType.OK);
            confirm.setTitle("Actualizar desde ruta"); confirm.setHeaderText("¿Reemplazar la copia operativa?");
            if (AlertUtils.showAndWait(confirm, tablaSalidas).orElse(ButtonType.CANCEL) == ButtonType.OK && rutas.getValue() != null) {
                cambiosRealizados = true;
                actualizarDesdeRuta = true;
                checkpointsGuardarEnRuta.clear();
                checkpoints.clear(); checkpoints.addAll(Servicios.itinerarios().obtener(rutas.getValue().getId()).checkpoints()); actualizarPuntos();
            }
        }

        private void configurarAsignaciones() {
            var equipos = new ArrayList<Equipamiento>(EquipamientoDAO.obtenerTodosParaEdicion(original.id()));
            equipos.forEach(e->equiposActivos.add(e.getId()));
            EquipamientoDAO.obtenerDadosDeBaja().forEach(e -> { if (equipamiento.stream().anyMatch(a -> a.equipamiento().getId()==e.getId())) equipos.add(e); });
            equipamiento.forEach(a -> { if(equipos.stream().noneMatch(e->e.getId()==a.equipamiento().getId())) equipos.add(a.equipamiento()); });
            selectorEquipo.setItems(FXCollections.observableArrayList(equipos));
            selectorEquipo.setConverter(new StringConverter<>() { public String toString(Equipamiento e){return e==null?"":e.getNombre()+" · disponible: "+e.getCantidadDisponible();} public Equipamiento fromString(String s){return null;} });
            selectorCliente.setConverter(new StringConverter<>() { public String toString(Cliente c){return c==null?"":c.apellido()+", "+c.nombre()+" · DNI "+c.dni();} public Cliente fromString(String s){return null;} });
            var todos = com.example.desktop.Servicios.clientes().listar();
            var clientes = new ArrayList<Cliente>();
            todos.stream().filter(Cliente::activo).forEach(clientes::add);
            participantes.forEach(a -> { if(clientes.stream().noneMatch(c->c.id()==a.cliente().id())) clientes.add(a.cliente()); });
            selectorCliente.setItems(FXCollections.observableArrayList(clientes));
            renderAsignaciones();
        }

        private void renderAsignaciones(){
            listaEquipo.setItems(FXCollections.observableArrayList(equipamiento));
            listaParticipantes.setItems(FXCollections.observableArrayList(participantes));
            ajustarAltura(listaEquipo, equipamiento.size());
            ajustarAltura(listaParticipantes, participantes.size());
        }
        private void agregarEquipo() {
            try {
                Equipamiento item = selectorEquipo.getValue();
                int cantidad = Integer.parseInt(cantidadEquipo.getText().trim());
                if (item == null || cantidad <= 0) throw new IllegalArgumentException();
                int indice = -1;
                for (int i = 0; i < equipamiento.size(); i++) {
                    if (equipamiento.get(i).equipamiento().getId() == item.getId()) indice = i;
                }
                if (indice >= 0) {
                    var asignacion = equipamiento.get(indice);
                    equipamiento.set(indice, new AsignacionEquipamiento(
                            asignacion.equipamiento(), asignacion.cantidad() + cantidad, false));
                } else {
                    equipamiento.add(new AsignacionEquipamiento(item, cantidad, false));
                }
                cambiosRealizados = true;
                renderAsignaciones();
            } catch (RuntimeException e) {
                aviso(Alert.AlertType.WARNING, "Cantidad inválida", "Selecciona un elemento y una cantidad positiva.");
            }
        }

        private void quitarEquipo(AsignacionEquipamiento asignacion) {
            cambiosRealizados = true;
            equipamiento.remove(asignacion);
            refrescarEquipamiento.add(asignacion.equipamiento().getId());
            if (!equiposActivos.contains(asignacion.equipamiento().getId())) {
                selectorEquipo.getItems().removeIf(item -> item.getId() == asignacion.equipamiento().getId());
            }
            renderAsignaciones();
        }

        private void agregarParticipante() {
            Cliente cliente = selectorCliente.getValue();
            if (cliente == null) return;
            if (participantes.stream().anyMatch(a -> a.cliente().id() == cliente.id())) {
                aviso(Alert.AlertType.INFORMATION, "Cliente ya asociado", "Ese Cliente ya está asociado a esta Salida.");
                return;
            }
            participantes.add(new AsignacionParticipante(cliente, false));
            cambiosRealizados = true;
            renderAsignaciones();
        }

        private void quitarParticipante(AsignacionParticipante asignacion) {
            cambiosRealizados = true;
            participantes.remove(asignacion);
            refrescarParticipantes.add(asignacion.cliente().id());
            if (!asignacion.cliente().activo()) {
                selectorCliente.getItems().removeIf(cliente -> cliente.id() == asignacion.cliente().id());
            }
            renderAsignaciones();
        }

        private void actualizarPuntos() {
            puntos.setItems(FXCollections.observableArrayList(checkpoints));
            ajustarAltura(puntos, checkpoints.size());
        }

        private void actualizarDetalle() {
            Ruta r = rutas.getValue();
            if (r != null) detalleRuta.setText("Altitud máxima " + r.getAltitudMaxima() + " m · Terreno " + r.getTipoTerreno()
                    + " · Dificultad técnica " + r.getDificultadTecnica() + " · Dificultad física " + r.getDificultadFisica());
        }

        private void editarCheckpoint(Checkpoint existente) {
            Ruta ruta = rutas.getValue(); if (ruta == null) return;
            TextField nombre = new TextField(existente == null ? "" : existente.nombre());
            TextField hora = new TextField(existente == null ? "" : existente.hora());
            TextField latitud = new TextField(existente == null ? "" : String.valueOf(existente.latitud()));
            TextField longitud = new TextField(existente == null ? "" : String.valueOf(existente.longitud()));
            TextField altitud = new TextField(existente == null ? "" : String.valueOf(existente.altitud()));
            TextField descripcion = new TextField(existente == null ? "" : existente.descripcion());
            CheckBox guardarRuta = new CheckBox("Guardar también en la Ruta");
            boolean activa = rutaDAO.estaActiva(ruta.getId());
            int cantidadPlantilla = Servicios.itinerarios().obtener(ruta.getId()).cantidad();
            boolean permiteGuardar = cantidadPlantilla + 1 >= 2;
            guardarRuta.setVisible(existente == null && activa && permiteGuardar);
            guardarRuta.setManaged(existente == null && activa && permiteGuardar);
            if (!activa) guardarRuta.setTooltip(new javafx.scene.control.Tooltip("La Ruta está inactiva."));
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.initOwner(escenaAplicacion.getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle(existente == null ? "Agregar checkpoint" : "Editar checkpoint");
            AlertUtils.applyTheme(dialog);
            dialog.getDialogPane().getStyleClass().add("dialog-form-card");
            dialog.getDialogPane().setPrefWidth(560);
            ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
            ButtonType guardar = new ButtonType(
                    existente == null ? "Agregar checkpoint" : "Guardar cambios",
                    ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(cancelar, guardar);
            GridPane grid = new GridPane(); grid.setHgap(8); grid.setVgap(8);
            grid.getStyleClass().add("form-grid");
            String[] labels = {"Nombre *", "Hora (HH:mm) *", "Latitud *", "Longitud *", "Altitud (m) *", "Descripción"};
            TextField[] fields = {nombre, hora, latitud, longitud, altitud, descripcion};
            for (int i = 0; i < fields.length; i++) { grid.add(new Label(labels[i]), 0, i); grid.add(fields[i], 1, i); }
            grid.add(guardarRuta, 1, fields.length); dialog.getDialogPane().setContent(grid);
            if (dialog.showAndWait().orElse(cancelar) != guardar) return;
            try {
                double altitudMaxima = rutas.getValue().getId() == original.rutaId() && !actualizarDesdeRuta
                        ? original.rutaAltitudMaxima() : ruta.getAltitudMaxima();
                Checkpoint cp = new Checkpoint(0L, nombre.getText().trim(), hora.getText().trim(),
                        Double.parseDouble(latitud.getText().trim().replace(',', '.')), Double.parseDouble(longitud.getText().trim().replace(',', '.')),
                        Double.parseDouble(altitud.getText().trim().replace(',', '.')), descripcion.getText().trim());
                if (cp.nombre().isBlank() || !cp.hora().matches("(?:[01]\\d|2[0-3]):[0-5]\\d") || cp.latitud() < -90 || cp.latitud() > 90
                        || cp.longitud() < -180 || cp.longitud() > 180 || cp.altitud() < 0 || cp.altitud() > altitudMaxima)
                    throw new IllegalArgumentException("Verifica los campos; la altitud debe estar entre cero y " + altitudMaxima + " m.");
                if (existente == null) {
                    checkpoints.add(cp);
                } else {
                    int index = checkpoints.indexOf(existente);
                    checkpoints.set(index, cp);
                    int indexRuta = checkpointsGuardarEnRuta.indexOf(existente);
                    if (indexRuta >= 0) checkpointsGuardarEnRuta.set(indexRuta, cp);
                }
                if (guardarRuta.isSelected()) checkpointsGuardarEnRuta.add(cp);
                cambiosRealizados = true;
                actualizarPuntos();
            } catch (RuntimeException ex) { aviso(Alert.AlertType.WARNING, "Checkpoint inválido", ex.getMessage() == null ? "Completa correctamente todos los campos." : ex.getMessage()); }
        }

        private void guardar() {
            boolean fechaValida = FormValidation.validateNow(fecha);
            boolean rutaValida = FormValidation.validateNow(rutas);
            if (!fechaValida || !rutaValida) {
                (fechaValida ? rutas : fecha).requestFocus();
                return;
            }
            try {
                List<Checkpoint> copia = List.copyOf(checkpoints);
                salidaDAO.actualizar(original.id(), fecha.getValue(), rutas.getValue().getId(), copia, List.copyOf(checkpointsGuardarEnRuta), actualizarDesdeRuta, List.copyOf(equipamiento), List.copyOf(participantes), java.util.Set.copyOf(refrescarEquipamiento), java.util.Set.copyOf(refrescarParticipantes));
                NavigationShell.setNavigationGuard(escenaAplicacion, null);
                volverAlListado();
                ToastNotifications.show(escenaAplicacion.getWindow(), "Salida actualizada", ToastNotifications.Kind.SUCCESS);
            } catch (SQLException | RuntimeException e) { aviso(Alert.AlertType.ERROR, "No se pudo actualizar la Salida", e.getMessage()); }
        }

        private boolean confirmarCambioDeSeccion() {
            if (!cambiosRealizados) {
                NavigationShell.setNavigationGuard(escenaAplicacion, null);
                return true;
            }
            boolean descartar = confirmarDescarte("Cambiar de sección");
            if (descartar) NavigationShell.setNavigationGuard(escenaAplicacion, null);
            return descartar;
        }

        private boolean confirmarDescarte(String contexto) {
            Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION,
                    "Hay cambios sin guardar en la Salida. ¿Quieres descartarlos?",
                    ButtonType.CANCEL, ButtonType.OK);
            confirmar.setTitle("Descartar cambios de Salida");
            confirmar.setHeaderText(contexto);
            return AlertUtils.showAndWait(confirmar, tablaSalidas).orElse(ButtonType.CANCEL) == ButtonType.OK;
        }

        private void cancelarEdicion() {
            if (cambiosRealizados && !confirmarDescarte("Cancelar edición")) return;
            NavigationShell.setNavigationGuard(escenaAplicacion, null);
            volverAlListado();
        }

        private void volverAlListado() {
            try {
                javafx.scene.Parent pagina = new javafx.fxml.FXMLLoader(
                        getClass().getResource("/com/example/fxml/salidas.fxml")).load();
                NavigationShell.setRoot(escenaAplicacion, pagina);
            } catch (IOException ex) {
                aviso(Alert.AlertType.ERROR, "No se pudo volver a Salidas", ex.getMessage());
            }
        }
    }

    private final class WizardSalida {
        private final Scene escenaAplicacion = tablaSalidas.getScene();
        private final List<AsignacionEquipamiento> asignaciones = new ArrayList<>();
        private final List<AsignacionParticipante> participantes = new ArrayList<>();
        private final VBox content = new VBox(14);
        private final Label heading = new Label();
        private final Label progress = new Label();
        private final javafx.scene.layout.HBox stepper = new javafx.scene.layout.HBox(0);
        private final DatePicker fecha = new DatePicker();
        private final ComboBox<Ruta> rutas = new ComboBox<>();
        private final javafx.scene.control.ToggleGroup modoRuta = new javafx.scene.control.ToggleGroup();
        private final RadioButton usarRuta = new RadioButton("Utilizar una Ruta existente");
        private final RadioButton crearRuta = new RadioButton("Crear una Ruta nueva");
        private final TextField nombre = new TextField();
        private final TextField altitud = new TextField();
        private final ComboBox<String> terreno = opciones("Rocoso", "Boscoso", "Sendero", "Mixto");
        private final ComboBox<String> tecnica = opciones("Baja", "Media", "Alta");
        private final ComboBox<String> fisica = opciones("Baja", "Media", "Alta");
        private final Label detalle = new Label();
        private final ComboBox<Equipamiento> inventario = new ComboBox<>();
        private final TextField cantidadAsignada = new TextField("1");
        private final ListView<AsignacionEquipamiento> listaAsignaciones = new ListView<>();
        private final ComboBox<Cliente> clientes = new ComboBox<>();
        private final TextField buscarCliente = new TextField();
        private final ListView<AsignacionParticipante> listaParticipantes = new ListView<>();
        private List<Cliente> todosLosClientes = List.of();
        private final Button anterior = new Button("Anterior"), siguiente = new Button("Siguiente"), cancelar = new Button("Cancelar");
        private int paso;
        private List<Ruta> rutasActivas;

        WizardSalida() {
            FechaFormato.configurar(fecha);
            usarRuta.setToggleGroup(modoRuta); crearRuta.setToggleGroup(modoRuta); usarRuta.setSelected(true);
            rutas.setConverter(new StringConverter<>() { public String toString(Ruta r) { return r == null ? "" : r.getNombre(); } public Ruta fromString(String s) { return null; } });
            fecha.valueProperty().addListener((o, a, b) -> renderStepper());
            rutas.valueProperty().addListener((o, a, b) -> renderStepper());
            nombre.textProperty().addListener((o, a, b) -> renderStepper());
            altitud.textProperty().addListener((o, a, b) -> renderStepper());
            terreno.valueProperty().addListener((o, a, b) -> renderStepper());
            tecnica.valueProperty().addListener((o, a, b) -> renderStepper());
            fisica.valueProperty().addListener((o, a, b) -> renderStepper());
            inventario.setConverter(new StringConverter<>() {
                public String toString(Equipamiento item) { return item == null ? "" : item.getNombre() + " · disponible: " + item.getCantidadDisponible(); }
                public Equipamiento fromString(String s) { return null; }
            });
            listaAsignaciones.setCellFactory(view -> new ListCell<>() {
                @Override protected void updateItem(AsignacionEquipamiento item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setGraphic(null); setText(null); return; }
                    Label nombreElemento = new Label(item.equipamiento().getNombre());
                    nombreElemento.getStyleClass().add("wizard-equipment-name");
                    javafx.scene.layout.Region espacio = new javafx.scene.layout.Region();
                    Label disponible = new Label(item.nuevo() ? "Nuevo" : String.valueOf(item.equipamiento().getCantidadDisponible()));
                    disponible.getStyleClass().add("wizard-row-meta");
                    disponible.setPrefWidth(120);
                    Label cantidad = new Label(String.valueOf(item.cantidad()));
                    cantidad.getStyleClass().add("wizard-row-quantity");
                    cantidad.setPrefWidth(70);
                    cantidad.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                    javafx.scene.layout.HBox fila = new javafx.scene.layout.HBox(8, nombreElemento, espacio, disponible, cantidad);
                    fila.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    javafx.scene.layout.HBox.setHgrow(nombreElemento, javafx.scene.layout.Priority.ALWAYS);
                    setText(null);
                    setGraphic(fila);
                }
            });
            clientes.setConverter(new StringConverter<>() {
                public String toString(Cliente c) { return c == null ? "" : c.apellido() + ", " + c.nombre() + " · DNI " + c.dni(); }
                public Cliente fromString(String s) { return null; }
            });
            listaParticipantes.setCellFactory(view -> new ListCell<>() {
                @Override protected void updateItem(AsignacionParticipante item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.cliente().apellido() + ", " + item.cliente().nombre()
                            + " · DNI " + item.cliente().dni() + (item.nuevo() ? " · nuevo" : ""));
                }
            });
            buscarCliente.textProperty().addListener((o, a, b) -> actualizarClientesFiltrados());
            modoRuta.selectedToggleProperty().addListener((o, a, b) -> render());
            rutas.valueProperty().addListener((o,a,b) -> actualizarDetalle());
            anterior.setOnAction(e -> { if (paso > 0) { paso--; render(); } });
            siguiente.setOnAction(e -> avanzar()); cancelar.setOnAction(e -> cancelarWizard());
            content.getStyleClass().add("wizard-content");
        }
        private ComboBox<String> opciones(String... valores) { return new ComboBox<>(FXCollections.observableArrayList(valores)); }
        void mostrar() {
            rutasActivas = rutaDAO.obtenerTodas(); rutas.setItems(FXCollections.observableArrayList(rutasActivas));
            inventario.setItems(FXCollections.observableArrayList(EquipamientoDAO.obtenerTodos()));
            todosLosClientes = com.example.desktop.Servicios.clientes().listar();
            render();
            javafx.scene.layout.HBox pasoEncabezado = new javafx.scene.layout.HBox(12, progress);
            pasoEncabezado.getStyleClass().add("wizard-step-caption");
            stepper.getStyleClass().add("wizard-stepper");
            VBox encabezado = new VBox(14, new Label("Nueva Salida"), stepper, pasoEncabezado);
            encabezado.getStyleClass().add("wizard-header");
            ScrollPane scroll = new ScrollPane(content);
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(true);
            scroll.getStyleClass().add("wizard-scroll");
            javafx.scene.layout.BorderPane root = new javafx.scene.layout.BorderPane();
            root.setTop(encabezado);
            root.setCenter(scroll);
            javafx.scene.layout.Region espacio = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox accionesIzquierda = new javafx.scene.layout.HBox(cancelar);
            javafx.scene.layout.HBox accionesDerecha = new javafx.scene.layout.HBox(10, anterior, siguiente);
            javafx.scene.layout.HBox barra = new javafx.scene.layout.HBox(10, accionesIzquierda, espacio, accionesDerecha);
            javafx.scene.layout.HBox.setHgrow(espacio, javafx.scene.layout.Priority.ALWAYS);
            root.setBottom(barra);
            root.getBottom().getStyleClass().add("wizard-actions");
            NavigationShell.setNavigationGuard(escenaAplicacion, this::confirmarCambioDeSeccion);
            NavigationShell.setRoot(escenaAplicacion, root, "salida-alta");
            javafx.application.Platform.runLater(() -> {
                aplicarIconoCalendario();
                fecha.requestFocus();
            });
        }
        private void aplicarIconoCalendario() {
            fecha.applyCss();
            javafx.scene.Node botonCalendario = fecha.lookup(".arrow-button");
            if (botonCalendario instanceof javafx.scene.layout.StackPane contenedor) {
                javafx.scene.layout.StackPane icono = com.example.desktop.util.SidebarIcon.load("SALIDAS.svg");
                icono.getStyleClass().add("wizard-calendar-icon");
                icono.setMouseTransparent(true);
                contenedor.getChildren().setAll(icono);
            }
        }
        private static final String[] TITULOS_PASOS = {"Datos", "Ruta", "Equipamiento", "Participantes", "Revisión"};
        private final java.util.Set<Integer> visitados = new java.util.HashSet<>();
        private boolean datosCompletos() { return fecha.getValue() != null; }
        private boolean rutaCompleta() {
            if (!isCreandoRuta()) return rutas.getValue() != null;
            if (nombre.getText().isBlank() || terreno.getValue() == null || tecnica.getValue() == null || fisica.getValue() == null) return false;
            try { return Double.parseDouble(altitud.getText().trim()) >= 0; }
            catch (NumberFormatException ex) { return false; }
        }
        private boolean pasoCompleto(int i) {
            return switch (i) { case 0 -> datosCompletos(); case 1 -> rutaCompleta(); case 2, 3 -> visitados.contains(i); default -> false; };
        }
        private boolean pasoAlcanzable(int i) {
            if (i <= paso) return true;
            return (i < 1 || datosCompletos()) && (i < 2 || rutaCompleta());
        }
        private void renderStepper() {
            stepper.getChildren().clear();
            int ultimo = TITULOS_PASOS.length - 1;
            for (int i = 0; i < TITULOS_PASOS.length; i++) {
                final int destino = i;
                boolean completo = i != paso && pasoCompleto(i), alcanzable = pasoAlcanzable(i);
                VBox item = new VBox(5);
                item.setAlignment(i == 0 ? javafx.geometry.Pos.TOP_LEFT : i == ultimo ? javafx.geometry.Pos.TOP_RIGHT : javafx.geometry.Pos.TOP_CENTER);
                item.getStyleClass().add("wizard-step");
                Label circulo = new Label(completo ? "✓" : String.valueOf(i + 1));
                circulo.getStyleClass().add("wizard-step-marker");
                Label nombrePaso = new Label(TITULOS_PASOS[i]);
                nombrePaso.getStyleClass().add("wizard-step-name");
                if (i == paso) item.getStyleClass().add("wizard-step-current");
                else if (completo) item.getStyleClass().add("wizard-step-complete");
                if (!alcanzable) item.getStyleClass().add("wizard-step-blocked");
                else if (i != paso) {
                    item.getStyleClass().add("wizard-step-reachable");
                    item.setOnMouseClicked(e -> { paso = destino; render(); });
                }
                item.getChildren().addAll(circulo, nombrePaso);
                stepper.getChildren().add(item);
                if (i < ultimo) {
                    javafx.scene.layout.Region conector = new javafx.scene.layout.Region();
                    conector.getStyleClass().add(completo ? "wizard-step-connector-complete" : "wizard-step-connector");
                    conector.setMinHeight(2);
                    conector.setPrefHeight(2);
                    conector.setMaxHeight(2);
                    conector.setMaxWidth(Double.MAX_VALUE);
                    javafx.scene.layout.HBox.setHgrow(conector, javafx.scene.layout.Priority.ALWAYS);
                    stepper.getChildren().add(conector);
                }
            }
        }
        private void render() {
            String[] titulos = TITULOS_PASOS;
            String[] descripciones = {"Defina cuándo se realizará la actividad.", "Seleccione una Ruta existente o configure una nueva.",
                    "Asigne los elementos que se llevarán.", "Seleccione los Clientes que participarán.", "Revise la información antes de confirmar."};
            heading.setText(titulos[paso]);
            progress.setText("Paso " + (paso + 1) + " de " + titulos.length);
            visitados.add(paso);
            renderStepper();
            content.getChildren().clear();
            Label descripcion = new Label(descripciones[paso]);
            descripcion.getStyleClass().add("wizard-description");
            content.getChildren().addAll(heading, descripcion);
            if (paso == 0) {
                VBox tarjeta = tarjetaPaso("Datos de la salida", "Defina cuándo se realizará la actividad.");
                tarjeta.getChildren().add(campo("Fecha de la salida *", fecha));
                fecha.setPromptText("Seleccionar fecha");
                content.getChildren().add(tarjeta);
            }
            if (paso == 1) {
                VBox tarjeta = tarjetaPaso("Seleccionar Ruta", "Seleccione una Ruta existente o cree una nueva.");
                tarjeta.getChildren().addAll(usarRuta, crearRuta);
                if (isCreandoRuta()) {
                    tarjeta.getChildren().addAll(campo("Nombre *", nombre), campo("Altitud máxima (m) *", altitud),
                            new Label("Tipo de terreno"), terreno, new Label("Dificultad técnica"), tecnica,
                            new Label("Dificultad física"), fisica,
                            new Label("La Ruta nueva se guardará solo al confirmar la Salida. Se crea como borrador, sin Checkpoints."));
                } else {
                    tarjeta.getChildren().addAll(new Label("Ruta activa"), rutas, detalle);
                    rutas.setMaxWidth(Double.MAX_VALUE);
                    actualizarDetalle();
                }
                content.getChildren().add(tarjeta);
            }
            if (paso == 2) renderEquipamiento();
            if (paso == 3) renderParticipantes();
            if (paso == 4) {
                content.getChildren().addAll(
                        tarjetaRevision("Datos generales", "Fecha: " + FechaFormato.formatear(fecha.getValue()) + "\nEstado inicial: Planificada", 0),
                        tarjetaRevision("Ruta", resumenRuta(), 1),
                        tarjetaRevision("Equipamiento", resumenEquipamientoWizard(), 2),
                        tarjetaRevision("Participantes", resumenParticipantesWizard(), 3),
                        new Label("La confirmación guardará la Salida y sus asociaciones en una única operación."));
            }
            anterior.setDisable(paso == 0); siguiente.setText(paso == 4 ? "Confirmar Salida" : "Siguiente");
            if (!siguiente.getStyleClass().contains("action-add")) siguiente.getStyleClass().add("action-add");
            if (!cancelar.getStyleClass().contains("secondary-button")) cancelar.getStyleClass().add("secondary-button");
            if (!anterior.getStyleClass().contains("secondary-button")) anterior.getStyleClass().add("secondary-button");
        }
        private boolean confirmarCambioDeSeccion() {
            if (!hayCambios()) return true;
            Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION,
                    "Hay datos ingresados en la Salida. ¿Quieres descartar el borrador?",
                    ButtonType.CANCEL, ButtonType.OK);
            confirmar.setTitle("Descartar Nueva Salida");
            confirmar.setHeaderText("Cambiar de sección");
            boolean descartar = AlertUtils.showAndWait(confirmar, tablaSalidas).orElse(ButtonType.CANCEL) == ButtonType.OK;
            if (descartar) NavigationShell.setNavigationGuard(escenaAplicacion, null);
            return descartar;
        }
        private boolean hayCambios() {
            return fecha.getValue() != null || rutas.getValue() != null || isCreandoRuta()
                    || inventario.getValue() != null || !"1".equals(cantidadAsignada.getText().trim())
                    || clientes.getValue() != null || !asignaciones.isEmpty()
                    || !participantes.isEmpty() || !nombre.getText().isBlank()
                    || !altitud.getText().isBlank() || terreno.getValue() != null
                    || tecnica.getValue() != null || fisica.getValue() != null
                    || !buscarCliente.getText().isBlank();
        }
        private void cancelarWizard() {
            if (hayCambios() && !confirmarCambioDeSeccion()) return;
            cerrarWizard();
        }
        private void cerrarWizard() {
            NavigationShell.setNavigationGuard(escenaAplicacion, null);
            try {
                javafx.scene.Parent pagina = new javafx.fxml.FXMLLoader(
                        getClass().getResource("/com/example/fxml/salidas.fxml")).load();
                NavigationShell.setRoot(escenaAplicacion, pagina);
            } catch (IOException ex) {
                aviso(Alert.AlertType.ERROR, "No se pudo volver a Salidas", ex.getMessage());
            }
        }
        private VBox campo(String titulo, javafx.scene.control.Control field) { return new VBox(4, new Label(titulo), field); }
        private VBox tarjetaPaso(String titulo, String subtitulo) {
            VBox tarjeta = new VBox(10, new Label(titulo), new Label(subtitulo));
            tarjeta.getStyleClass().add("wizard-card");
            tarjeta.getChildren().get(0).getStyleClass().add("wizard-card-title");
            tarjeta.getChildren().get(1).getStyleClass().add("wizard-card-subtitle");
            return tarjeta;
        }
        private VBox tarjetaRevision(String titulo, String detalleRevision, int pasoDestino) {
            Label texto = new Label(detalleRevision);
            texto.setWrapText(true);
            texto.getStyleClass().add("wizard-review-detail");
            Button editar = new Button("Editar");
            editar.getStyleClass().add("wizard-edit-button");
            editar.setOnAction(e -> { paso = pasoDestino; render(); });
            javafx.scene.layout.HBox encabezado = new javafx.scene.layout.HBox(8, new Label(titulo), new javafx.scene.layout.Region(), editar);
            javafx.scene.layout.HBox.setHgrow(encabezado.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);
            encabezado.getChildren().get(0).getStyleClass().add("wizard-card-title");
            VBox tarjeta = new VBox(8, encabezado, texto);
            tarjeta.getStyleClass().add("wizard-card");
            return tarjeta;
        }
        private String resumenRuta() {
            if (isCreandoRuta()) return nombre.getText().trim() + "\nAltitud máxima: " + altitud.getText().trim()
                    + " m · " + terreno.getValue() + " · Dificultad técnica " + tecnica.getValue()
                    + " · Dificultad física " + fisica.getValue() + "\nSin Checkpoints";
            actualizarDetalle();
            return rutas.getValue() == null ? "Sin Ruta seleccionada" : rutas.getValue().getNombre() + "\n" + detalle.getText();
        }
        private String resumenEquipamientoWizard() {
            if (asignaciones.isEmpty()) return "Sin Equipamiento asignado";
            return asignaciones.stream().map(a -> a.equipamiento().getNombre() + " × " + a.cantidad())
                    .collect(java.util.stream.Collectors.joining("\n"));
        }
        private String resumenParticipantesWizard() {
            if (participantes.isEmpty()) return "Sin Participantes asignados";
            return participantes.stream().map(p -> p.cliente().apellido() + ", " + p.cliente().nombre()
                    + " · DNI " + p.cliente().dni()).collect(java.util.stream.Collectors.joining("\n"));
        }
        private void renderEquipamiento() {
            listaAsignaciones.setMaxHeight(220);
            listaAsignaciones.setItems(FXCollections.observableArrayList(asignaciones));
            listaAsignaciones.setPlaceholder(new Label("Todavía no hay Equipamiento asignado"));
            listaAsignaciones.setPrefHeight(Math.min(Math.max(asignaciones.size(), 1) * 42, 190));
            listaAsignaciones.setFixedCellSize(42);
            Button agregar = new Button("Agregar elemento"); agregar.getStyleClass().add("wizard-secondary-button"); agregar.setOnAction(e -> agregarExistente());
            Button quitar = new Button("Quitar seleccionado"); quitar.getStyleClass().add("wizard-secondary-button");
            quitar.setDisable(asignaciones.isEmpty());
            quitar.setOnAction(e -> {
                AsignacionEquipamiento seleccionado = listaAsignaciones.getSelectionModel().getSelectedItem();
                if (seleccionado != null) { asignaciones.remove(seleccionado); render(); }
            });
            Button crear = new Button("Crear y agregar"); crear.getStyleClass().add("wizard-secondary-button"); crear.setOnAction(e -> crearYAgregar());
            inventario.setPrefWidth(340);
            inventario.setMaxWidth(Double.MAX_VALUE);
            cantidadAsignada.setPrefWidth(110);
            VBox tarjeta = tarjetaPaso("Equipamiento de la Salida", "Disponibilidad calculada según inventario y reservas.");
            javafx.scene.layout.HBox controles = new javafx.scene.layout.HBox(8, inventario, cantidadAsignada, agregar, crear);
            javafx.scene.layout.HBox.setHgrow(inventario, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.HBox encabezados = new javafx.scene.layout.HBox(8, new Label("Elemento"),
                    new javafx.scene.layout.Region(), new Label("Disponible"), new Label("Asignar"));
            encabezados.getStyleClass().add("wizard-equipment-header");
            javafx.scene.layout.HBox.setHgrow(encabezados.getChildren().get(0), javafx.scene.layout.Priority.ALWAYS);
            ((Label) encabezados.getChildren().get(2)).setPrefWidth(120);
            ((Label) encabezados.getChildren().get(3)).setPrefWidth(70);
            ((Label) encabezados.getChildren().get(3)).setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            tarjeta.getChildren().addAll(new Label("Elementos disponibles"), controles, encabezados, listaAsignaciones, quitar);
            content.getChildren().add(tarjeta);
        }
        private void renderParticipantes() {
            listaParticipantes.setPrefHeight(150);
            listaParticipantes.setMaxHeight(220);
            buscarCliente.setPromptText("Buscar por DNI, nombre o apellido");
            buscarCliente.getStyleClass().add("list-search-field");
            actualizarClientesFiltrados();
            listaParticipantes.setItems(FXCollections.observableArrayList(participantes));
            listaParticipantes.setPlaceholder(new Label("Todavía no hay Participantes asignados"));
            listaParticipantes.setPrefHeight(Math.min(Math.max(participantes.size(), 1) * 42, 190));
            listaParticipantes.setFixedCellSize(42);
            Button agregar = new Button("Agregar cliente seleccionado"); agregar.getStyleClass().add("wizard-secondary-button");
            agregar.setOnAction(e -> agregarClienteExistente());
            Button crear = new Button("+ Nuevo cliente"); crear.getStyleClass().add("wizard-secondary-button");
            crear.setOnAction(e -> crearClienteParticipante());
            Button quitar = new Button("Quitar seleccionado"); quitar.getStyleClass().add("wizard-secondary-button");
            quitar.setDisable(participantes.isEmpty());
            quitar.setOnAction(e -> {
                AsignacionParticipante seleccionado = listaParticipantes.getSelectionModel().getSelectedItem();
                if (seleccionado != null) { participantes.remove(seleccionado); render(); }
            });
            VBox tarjeta = tarjetaPaso("Participantes", "Seleccione los Clientes que participarán en esta Salida.");
            javafx.scene.layout.HBox busqueda = new javafx.scene.layout.HBox(8, SearchFieldIcon.wrap(buscarCliente), crear);
            javafx.scene.layout.HBox selector = new javafx.scene.layout.HBox(8, clientes, agregar);
            javafx.scene.layout.HBox.setHgrow(busqueda.getChildren().get(0), javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.HBox.setHgrow(clientes, javafx.scene.layout.Priority.ALWAYS);
            clientes.setMaxWidth(Double.MAX_VALUE);
            Label asignados = new Label("Participantes asignados · " + participantes.size());
            asignados.getStyleClass().add("wizard-card-title");
            tarjeta.getChildren().addAll(busqueda, selector, asignados, listaParticipantes, quitar);
            content.getChildren().add(tarjeta);
        }
        private void actualizarClientesFiltrados() {
            String consulta = buscarCliente.getText() == null ? "" : buscarCliente.getText().trim().toLowerCase();
            var filtrados = todosLosClientes.stream().filter(Cliente::activo)
                    .filter(c -> consulta.isEmpty() || c.dni().toLowerCase().contains(consulta)
                            || c.nombre().toLowerCase().contains(consulta) || c.apellido().toLowerCase().contains(consulta))
                    .toList();
            clientes.setItems(FXCollections.observableArrayList(filtrados));
            if (clientes.getValue() != null && !filtrados.contains(clientes.getValue())) clientes.setValue(null);
        }
        private void agregarClienteExistente() {
            Cliente cliente = clientes.getValue();
            if (cliente == null) { aviso(Alert.AlertType.WARNING, "Selecciona un Cliente", "Busca y selecciona un Cliente activo."); return; }
            if (participantes.stream().anyMatch(p -> p.cliente().id() == cliente.id())) {
                aviso(Alert.AlertType.INFORMATION, "Cliente ya asociado", "Ese Cliente ya está asociado a esta Salida."); return;
            }
            participantes.add(new AsignacionParticipante(cliente, false)); render();
        }
private void crearClienteParticipante() {
            Dialog<Cliente> dialog = new Dialog<>();
            dialog.initOwner(escenaAplicacion.getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle("Nuevo Cliente para la Salida");

            ButtonType guardar = new ButtonType(
                    "Agregar a la Salida", ButtonBar.ButtonData.OK_DONE);

            dialog.getDialogPane().getButtonTypes()
                    .addAll(guardar, ButtonType.CANCEL);

            TextField dni = new TextField();
            TextField nombre = new TextField();
            TextField apellido = new TextField();
            TextField email = new TextField();
            TextField telefono = new TextField();

            DatePicker nacimiento = new DatePicker();

            ComboBox<String> sexo = new ComboBox<>(
                    FXCollections.observableArrayList(
                            "Masculino", "Femenino", "No binario",
                            "Otro", "Prefiero no decir"));

            TextField emergenciaNombre = new TextField();
            TextField emergenciaTelefono = new TextField();
            TextField emergenciaRelacion = new TextField();

            CheckBox autorizacion = new CheckBox(
                    "Autorización de menores confirmada");

            TextField tutorNombre = new TextField();
            TextField tutorApellido = new TextField();
            TextField tutorDni = new TextField();
            TextField tutorTelefono = new TextField();

            Label fichaMedica = new Label("Sin archivo seleccionado");
            File[] archivoFicha = {null};

            Button seleccionarFicha = new Button(
                    "Seleccionar ficha médica PDF");

            seleccionarFicha.setOnAction(e -> {
                FileChooser selector = new FileChooser();
                selector.setTitle("Seleccionar ficha médica");
                selector.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter(
                                "Archivos PDF", "*.pdf", "*.PDF"));

                File archivo = selector.showOpenDialog(
                        escenaAplicacion.getWindow());

                if (archivo != null) {
                    archivoFicha[0] = archivo;
                    fichaMedica.setText(archivo.getName());
                }
            });

            GridPane formulario = new GridPane();
            formulario.setHgap(10);
            formulario.setVgap(7);
            formulario.setPadding(new javafx.geometry.Insets(12));

            Object[][] campos = {
                {"DNI *", dni},
                {"Nombre *", nombre},
                {"Apellido *", apellido},
                {"Nacimiento *", nacimiento},
                {"Email", email},
                {"Teléfono *", telefono},
                {"Sexo *", sexo},
                {"Contacto de emergencia", emergenciaNombre},
                {"Teléfono de emergencia", emergenciaTelefono},
                {"Relación", emergenciaRelacion},
                {"Tutor", tutorNombre},
                {"Apellido del tutor", tutorApellido},
                {"DNI del tutor", tutorDni},
                {"Teléfono del tutor", tutorTelefono},
                {"Ficha médica (opcional)",
                    new javafx.scene.layout.HBox(
                            8, seleccionarFicha, fichaMedica)}
            };

            for (int i = 0; i < campos.length; i++) {
                formulario.add(
                        new Label((String) campos[i][0]), 0, i);
                formulario.add(
                        (javafx.scene.Node) campos[i][1], 1, i);

                if (campos[i][1] instanceof Control control) {
                    control.setMaxWidth(Double.MAX_VALUE);
                }
            }

            formulario.add(autorizacion, 1, campos.length);
            dialog.getDialogPane().setContent(new ScrollPane(formulario));

            // NUEVO: validar antes de que se cierre el diálogo.
            Button botonGuardar = (Button) dialog.getDialogPane()
                    .lookupButton(guardar);

            botonGuardar.addEventFilter(
                    javafx.event.ActionEvent.ACTION, event -> {

                Cliente candidato = new Cliente(
                        0,
                        dni.getText().trim(),
                        nombre.getText().trim(),
                        apellido.getText().trim(),
                        nacimiento.getValue(),
                        email.getText().trim(),
                        telefono.getText().trim(),
                        sexo.getValue(),
                        emergenciaNombre.getText().trim(),
                        emergenciaTelefono.getText().trim(),
                        emergenciaRelacion.getText().trim(),
                        autorizacion.isSelected(),
                        tutorNombre.getText().trim(),
                        tutorApellido.getText().trim(),
                        tutorDni.getText().trim(),
                        tutorTelefono.getText().trim(),
                        true, LocalDate.now(), LocalDate.now());

                String error = com.example.desktop.Servicios.clientes().validar(new com.example.core.cliente.DatosCliente(
                        candidato.dni(), candidato.nombre(), candidato.apellido(), candidato.fechaNacimiento(),
                        candidato.email(), candidato.telefono(), candidato.sexo(),
                        candidato.contactoEmergenciaNombre(), candidato.contactoEmergenciaTelefono(),
                        candidato.contactoEmergenciaRelacion(), candidato.autorizacionMenores(),
                        candidato.tutorNombre(), candidato.tutorApellido(), candidato.tutorDni(),
                        candidato.tutorTelefono())).stream()
                        .map(com.example.core.excepcion.ErrorCampo::mensaje).findFirst().orElse(null);

                if (error != null) {
                    // NUEVO: conservar el formulario y los valores.
                    event.consume();
                    aviso(Alert.AlertType.ERROR,
                            "Revisa los datos del Cliente", error);
                    return;
                }

                // NUEVO: validar la ficha médica antes del cierre.
                if (archivoFicha[0] != null) {
                    File archivo = archivoFicha[0];

                    if (archivo.length() > 2L * 1024 * 1024
                            || !archivo.getName().toLowerCase()
                                    .endsWith(".pdf")) {
                        event.consume();
                        aviso(Alert.AlertType.ERROR,
                                "Ficha médica no válida",
                                "Elige un PDF de hasta 2 MB.");
                        return;
                    }

                    try {
                        byte[] contenido = Files.readAllBytes(
                                archivo.toPath());

                        if (contenido.length < 5
                                || contenido[0] != '%'
                                || contenido[1] != 'P'
                                || contenido[2] != 'D'
                                || contenido[3] != 'F'
                                || contenido[4] != '-') {

                            event.consume();
                            aviso(Alert.AlertType.ERROR,
                                    "Ficha médica no válida",
                                    "El archivo seleccionado no tiene "
                                    + "formato PDF válido.");
                        }
                    } catch (IOException ex) {
                        event.consume();
                        aviso(Alert.AlertType.ERROR,
                                "No se pudo leer la ficha médica",
                                ex.getMessage());
                    }
                }
            });

            // SIN CAMBIOS: construir el Cliente al confirmar.
            dialog.setResultConverter(button -> button == guardar
                    ? new Cliente(
                            0,
                            dni.getText().trim(),
                            nombre.getText().trim(),
                            apellido.getText().trim(),
                            nacimiento.getValue(),
                            email.getText().trim(),
                            telefono.getText().trim(),
                            sexo.getValue(),
                            emergenciaNombre.getText().trim(),
                            emergenciaTelefono.getText().trim(),
                            emergenciaRelacion.getText().trim(),
                            autorizacion.isSelected(),
                            tutorNombre.getText().trim(),
                            tutorApellido.getText().trim(),
                            tutorDni.getText().trim(),
                            tutorTelefono.getText().trim(),
                            true, LocalDate.now(), LocalDate.now())
                    : null);

            Cliente nuevo = dialog.showAndWait().orElse(null);
            if (nuevo == null) return;

            // La validación del Cliente ya se hizo antes del cierre.
            // Se conserva el procesamiento de la ficha para obtener
            // los bytes que se guardarán al confirmar la Salida.
            byte[] contenidoFicha = null;
            String nombreFicha = null;

            if (archivoFicha[0] != null) {
                try {
                    contenidoFicha = Files.readAllBytes(
                            archivoFicha[0].toPath());
                    nombreFicha = archivoFicha[0].getName();
                } catch (IOException ex) {
                    aviso(Alert.AlertType.ERROR,
                            "No se pudo leer la ficha médica",
                            ex.getMessage());
                    return;
                }
            }

            // SIN CAMBIOS: comprobar si el DNI ya existe.
            Cliente existente = todosLosClientes.stream()
                    .filter(c -> c.dni().trim()
                            .equals(nuevo.dni()))
                    .findFirst()
                    .orElse(null);

            if (existente != null) {
                if (!existente.activo()) {
                    aviso(Alert.AlertType.WARNING,
                            "DNI de Cliente inactivo",
                            "Ese DNI pertenece a un Cliente inactivo. "
                            + "Debe reactivarse desde Clientes antes "
                            + "de asociarlo.");
                } else {
                    Alert confirmar = new Alert(
                            Alert.AlertType.CONFIRMATION);
                    confirmar.setTitle("DNI ya registrado");
                    confirmar.setHeaderText("Usar el Cliente existente");
                    confirmar.setContentText(
                            "El DNI pertenece a "
                            + existente.nombre() + " "
                            + existente.apellido()
                            + ". ¿Quieres asociar ese registro?");

                    if (AlertUtils.showAndWait(confirmar, tablaSalidas)
                            .orElse(ButtonType.CANCEL) == ButtonType.OK) {

                        if (participantes.stream().noneMatch(
                                p -> p.cliente().id()
                                        == existente.id())) {
                            participantes.add(
                                    new AsignacionParticipante(
                                            existente, false));
                        }

                        render();
                    }
                }
                return;
            }

            if (participantes.stream().anyMatch(
                    p -> p.cliente().dni().equals(nuevo.dni()))) {
                aviso(Alert.AlertType.INFORMATION,
                        "Cliente ya asociado",
                        "Ese DNI ya está asociado a esta Salida.");
                return;
            }

            participantes.add(new AsignacionParticipante(
                    nuevo, true, contenidoFicha, nombreFicha));

            render();
        }
        private void agregarExistente() {
            Equipamiento seleccionado = inventario.getValue();
            int cantidad;
            try { cantidad = Integer.parseInt(cantidadAsignada.getText().trim()); }
            catch (RuntimeException e) { cantidad = 0; }
            if (seleccionado == null || cantidad <= 0) { aviso(Alert.AlertType.WARNING, "Cantidad inválida", "Selecciona un Elemento y una cantidad entera positiva."); return; }
            int acumulada = asignaciones.stream().filter(a -> !a.nuevo() && a.equipamiento().getId() == seleccionado.getId()).mapToInt(AsignacionEquipamiento::cantidad).sum();
            if (acumulada + cantidad > seleccionado.getCantidadDisponible()) {
                aviso(Alert.AlertType.WARNING, "Stock insuficiente", "Disponible para " + seleccionado.getNombre() + ": " + seleccionado.getCantidadDisponible() + "."); return;
            }
            int indice = -1;
            for (int i = 0; i < asignaciones.size(); i++) if (!asignaciones.get(i).nuevo() && asignaciones.get(i).equipamiento().getId() == seleccionado.getId()) indice = i;
            if (indice >= 0) { AsignacionEquipamiento anterior = asignaciones.get(indice); asignaciones.set(indice, new AsignacionEquipamiento(seleccionado, anterior.cantidad() + cantidad, false)); }
            else asignaciones.add(new AsignacionEquipamiento(seleccionado, cantidad, false));
            cantidadAsignada.setText("1"); render();
        }
        private void crearYAgregar() {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.initOwner(escenaAplicacion.getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle("Nuevo Elemento de inventario");

            ButtonType agregar = new ButtonType(
                    "Agregar a la Salida", ButtonBar.ButtonData.OK_DONE);

            dialog.getDialogPane().getButtonTypes()
                    .addAll(agregar, ButtonType.CANCEL);

            TextField nombreCampo = new TextField();
            ComboBox<String> categoria = new ComboBox<>(
                    FXCollections.observableArrayList(Equipamiento.CATEGORIAS));
            ComboBox<String> estado = new ComboBox<>(
                    FXCollections.observableArrayList(Equipamiento.ESTADOS));

            TextField totalCampo = new TextField("0");
            TextField cantidadCampo = new TextField("1");

            categoria.setValue("Campamento");
            estado.setValue("Disponible");

            GridPane formulario = new GridPane();
            formulario.setHgap(10);
            formulario.setVgap(8);
            formulario.addRow(0, new Label("Nombre"), nombreCampo);
            formulario.addRow(1, new Label("Categoría"), categoria);
            formulario.addRow(2, new Label("Estado"), estado);
            formulario.addRow(3, new Label("Cantidad total"), totalCampo);
            formulario.addRow(4, new Label("Cantidad a asignar"), cantidadCampo);

            dialog.getDialogPane().setContent(formulario);

            // NUEVO: interceptar el botón antes de cerrar el diálogo.
            Button botonAgregar = (Button) dialog.getDialogPane()
                    .lookupButton(agregar);

            botonAgregar.addEventFilter(
                    javafx.event.ActionEvent.ACTION, event -> {

                String nombreValidar = nombreCampo.getText() == null
                        ? "" : nombreCampo.getText().trim();

                int totalValidar;
                int cantidadValidar;

                try {
                    totalValidar = Integer.parseInt(
                            totalCampo.getText().trim());
                    cantidadValidar = Integer.parseInt(
                            cantidadCampo.getText().trim());
                } catch (RuntimeException ex) {
                    totalValidar = -1;
                    cantidadValidar = 0;
                }

                if (nombreValidar.isBlank()
                        || totalValidar < 0
                        || cantidadValidar <= 0
                        || cantidadValidar > totalValidar
                        || categoria.getValue() == null
                        || estado.getValue() == null) {

                    // NUEVO: impedir el cierre si hay errores.
                    event.consume();

                    aviso(Alert.AlertType.WARNING,
                            "Datos inválidos",
                            "Completa el nombre, categoría y estado; "
                            + "el total debe ser no negativo y la "
                            + "asignación positiva, sin superar el total.");
                }
            });

            // MODIFICADO: la validación ocurre antes de cerrar.
            if (dialog.showAndWait().orElse(ButtonType.CANCEL) != agregar) {
                return;
            }

            // MODIFICADO: los datos ya fueron validados.
            String nombre = nombreCampo.getText().trim();
            int total = Integer.parseInt(totalCampo.getText().trim());
            int cantidad = Integer.parseInt(cantidadCampo.getText().trim());

            Equipamiento nuevo = new Equipamiento(
                    0, nombre, categoria.getValue(), total, estado.getValue());

            asignaciones.add(
                    new AsignacionEquipamiento(nuevo, cantidad, true));

            render();
        }
        private void actualizarDetalle() {
            Ruta r = rutas.getValue();
            if (r == null) { detalle.setText("Selecciona una Ruta para ver sus datos."); return; }
            String puntos = Servicios.itinerarios().obtener(r.getId()).checkpoints().stream()
                    .map(cp -> cp.hora() + " · " + cp.nombre() + " · " + cp.altitud() + " m")
                    .collect(java.util.stream.Collectors.joining("\n"));
            detalle.setWrapText(true);
            detalle.setText(r.getNombre() + " · " + r.getAltitudMaxima() + " m · " + r.getTipoTerreno() + " · " + r.getDificultadTecnica() + "/" + r.getDificultadFisica()
                    + "\nCheckpoints que se copiarán:\n" + (puntos.isEmpty() ? "Sin Checkpoints" : puntos));
        }
        private String resumen() {
            Ruta rutaSeleccionada = rutas.getValue();
            String ruta;
            String puntos;
            if (isCreandoRuta()) {
                ruta = nombre.getText().trim();
                puntos = "Sin Checkpoints";
            } else if (rutaSeleccionada == null) {
                ruta = "(sin Ruta)";
                puntos = "Sin Checkpoints";
            } else {
                ruta = rutaSeleccionada.getNombre();
                puntos = Servicios.itinerarios().obtener(rutaSeleccionada.getId()).checkpoints().stream()
                        .map(cp -> cp.hora() + " · " + cp.nombre() + " · " + cp.latitud() + ", "
                                + cp.longitud() + " · " + cp.altitud() + " m"
                                + (cp.descripcion() == null || cp.descripcion().isBlank() ? "" : " · " + cp.descripcion()))
                        .collect(java.util.stream.Collectors.joining("\n"));
                if (puntos.isBlank()) puntos = "Sin Checkpoints";
            }
            String datosRuta = isCreandoRuta()
                    ? "Altitud máxima: " + altitud.getText().trim() + " m · Terreno: " + terreno.getValue()
                            + " · Dificultad técnica: " + tecnica.getValue() + " · Dificultad física: " + fisica.getValue()
                    : rutaSeleccionada == null ? "" : "Altitud máxima: " + rutaSeleccionada.getAltitudMaxima()
                            + " m · Terreno: " + rutaSeleccionada.getTipoTerreno() + " · Dificultad técnica: "
                            + rutaSeleccionada.getDificultadTecnica() + " · Dificultad física: "
                            + rutaSeleccionada.getDificultadFisica();
            String equipo = asignaciones.isEmpty() ? "ninguno" : asignaciones.stream()
                    .map(a -> "Ref #" + a.equipamiento().getId() + " · " + a.equipamiento().getNombre()
                            + " · " + a.equipamiento().getCategoria() + " × " + a.cantidad())
                    .collect(java.util.stream.Collectors.joining("\n"));
            String detalleParticipantes = participantes.isEmpty() ? "ninguno" : participantes.stream()
                    .map(p -> "Ref #" + p.cliente().id() + " · DNI " + p.cliente().dni() + " · "
                            + p.cliente().apellido() + ", " + p.cliente().nombre()
                            + " · Teléfono: " + textoResumen(p.cliente().telefono())
                            + " · Emergencia: " + textoResumen(p.cliente().contactoEmergenciaNombre())
                            + " / " + textoResumen(p.cliente().contactoEmergenciaTelefono())
                            + " / " + textoResumen(p.cliente().contactoEmergenciaRelacion()))
                    .collect(java.util.stream.Collectors.joining("\n"));
            return "Revisa los datos antes de confirmar:\n\nFecha: " + FechaFormato.formatear(fecha.getValue()) + "\nRuta: " + ruta
                    + (datosRuta.isBlank() ? "" : "\n" + datosRuta) + "\nCheckpoints:\n" + puntos
                    + "\nEstado inicial: planificada\nEquipamiento: " + equipo + "\nParticipantes: " + detalleParticipantes
                    + "\n\nAl confirmar se guardarán la Ruta nueva (si corresponde), los Elementos nuevos asociados, los Clientes nuevos seleccionados, las reservas, las copias operativas y la Salida en una sola operación.";
        }
        private void avanzar() {
            if (paso == 0 && fecha.getValue() == null) { aviso(Alert.AlertType.WARNING, "Fecha obligatoria", "Selecciona la fecha de la Salida."); return; }
            if (paso == 1) {
                if (isCreandoRuta()) {
                    if (nombre.getText().isBlank() || altitud.getText().isBlank() || terreno.getValue() == null || tecnica.getValue() == null || fisica.getValue() == null) { aviso(Alert.AlertType.WARNING,"Datos incompletos","Completa los datos obligatorios de la Ruta."); return; }
                    try { if (Double.parseDouble(altitud.getText().trim()) < 0) throw new NumberFormatException(); }
                    catch (NumberFormatException ex) { aviso(Alert.AlertType.WARNING,"Altitud inválida","La altitud debe ser un número mayor o igual a cero."); return; }
                } else if (rutas.getValue() == null) { aviso(Alert.AlertType.WARNING,"Ruta obligatoria","Selecciona una Ruta activa."); return; }
            }
            if (paso < 4) { paso++; render(); return; }
            try {
                Ruta nueva = null; Integer rutaId = null;
                if (isCreandoRuta()) nueva = new Ruta(Ruta.normalizarNombre(nombre.getText()), Double.parseDouble(altitud.getText().trim()), terreno.getValue(), tecnica.getValue(), fisica.getValue());
                else rutaId = rutas.getValue().getId();
                salidaDAO.confirmar(fecha.getValue(), rutaId, nueva, List.copyOf(asignaciones), List.copyOf(participantes));
                actualizarIndicadores(); actualizarTabla(); cerrarWizard();
                ToastNotifications.show(escenaAplicacion.getWindow(), "Salida guardada", ToastNotifications.Kind.SUCCESS);
            } catch (SQLException | RuntimeException ex) { aviso(Alert.AlertType.ERROR,"No se pudo confirmar la Salida",ex.getMessage()); }
        }
        private boolean isCreandoRuta() { return crearRuta.isSelected(); }
    }
}
