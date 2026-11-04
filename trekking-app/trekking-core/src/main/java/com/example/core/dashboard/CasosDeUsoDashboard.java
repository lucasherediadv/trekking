package com.example.core.dashboard;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.example.core.cliente.Cliente;
import com.example.core.cliente.RepositorioClientes;
import com.example.core.inventario.ElementoInventario;
import com.example.core.inventario.EstadoElemento;
import com.example.core.inventario.RepositorioInventario;
import com.example.core.itinerario.Checkpoint;
import com.example.core.ruta.RepositorioRutas;
import com.example.core.salida.CopiaOperativa;
import com.example.core.salida.EstadoSalida;
import com.example.core.salida.RepositorioSalidas;
import com.example.core.salida.Salida;

/** Casos de uso del Dashboard: indicadores y Salidas próximas, de solo lectura. */
public class CasosDeUsoDashboard {

    /** Stock bajo: cantidad menor o igual a este valor (equipos activos). */
    public static final int UMBRAL_STOCK_BAJO = 2;

    /** Ventana de Salidas próximas en días (hoy + 7, inclusive). */
    public static final int VENTANA_PROXIMOS_DIAS = 7;

    /** Límite de filas de Salidas próximas en el Dashboard. */
    public static final int LIMITE_PROXIMOS = 5;

    private static final Comparator<ProximaSalida> POR_FECHA_Y_HORA = Comparator
            .comparing(ProximaSalida::fecha)
            .thenComparing(ProximaSalida::horaInicio, Comparator.nullsFirst(Comparator.<String>naturalOrder()));

    private final RepositorioRutas rutas;
    private final RepositorioClientes clientes;
    private final RepositorioInventario inventario;
    private final RepositorioSalidas salidas;
    private final Clock reloj;

    public CasosDeUsoDashboard(RepositorioRutas rutas, RepositorioClientes clientes,
            RepositorioInventario inventario, RepositorioSalidas salidas, Clock reloj) {
        this.rutas = Objects.requireNonNull(rutas, "rutas");
        this.clientes = Objects.requireNonNull(clientes, "clientes");
        this.inventario = Objects.requireNonNull(inventario, "inventario");
        this.salidas = Objects.requireNonNull(salidas, "salidas");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    public record Indicadores(int rutasActivas, int clientesActivos, int salidasProximas, int equipoNoDisponible) {
    }

    /** Horarios en formato HH:mm tal como se guardan en los Checkpoints; {@code null} si no hay Checkpoints. */
    public record ProximaSalida(int id, String nombreRuta, LocalDate fecha, String horaInicio, String horaFin,
            int participantes) {
    }

    public Indicadores indicadores() {
        int clientesActivos = (int) clientes.listarTodos().stream().filter(Cliente::activo).count();
        return new Indicadores(rutas.listarActivas().size(), clientesActivos,
                salidasProximas().size(), contarEquipoNoDisponible());
    }

    /**
     * Indicadores del tablero de Salidas: igual que {@link #indicadores()}, pero el equipo crítico se mide
     * por cantidad disponible (total menos Reserva de stock) en lugar de la total.
     */
    public Indicadores indicadoresDeSalidas() {
        Set<EstadoElemento> noAsignables = EstadoElemento.noAsignables();
        int criticos = (int) inventario.listarActivos().stream()
                .filter(e -> e.cantidadDisponible() <= UMBRAL_STOCK_BAJO || noAsignables.contains(e.estado()))
                .count();
        Indicadores base = indicadores();
        return new Indicadores(base.rutasActivas(), base.clientesActivos(), base.salidasProximas(), criticos);
    }

    /** Primeras Salidas próximas por fecha y hora de inicio, hasta {@link #LIMITE_PROXIMOS}. */
    public List<ProximaSalida> proximasSalidas() {
        return salidasProximas().stream()
                .map(this::aProxima)
                .sorted(POR_FECHA_Y_HORA)
                .limit(LIMITE_PROXIMOS)
                .toList();
    }

    /** Equipo activo con stock bajo o en un estado que impide asignarlo a una Salida. */
    private int contarEquipoNoDisponible() {
        Set<EstadoElemento> noAsignables = EstadoElemento.noAsignables();
        return (int) inventario.listarActivos().stream()
                .filter(e -> e.cantidad() <= UMBRAL_STOCK_BAJO || noAsignables.contains(e.estado()))
                .count();
    }

    /** Salida próxima: planificada desde hoy hasta hoy más la ventana de días, inclusive. */
    private List<Salida> salidasProximas() {
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate hasta = hoy.plusDays(VENTANA_PROXIMOS_DIAS);
        return salidas.listar().stream()
                .filter(s -> s.estado() == EstadoSalida.PLANIFICADA)
                .filter(s -> !s.fecha().isBefore(hoy) && !s.fecha().isAfter(hasta))
                .toList();
    }

    /** Horario = hora del primer y del último Checkpoint de la Copia operativa. */
    private ProximaSalida aProxima(Salida salida) {
        CopiaOperativa copia = salidas.buscarCopia(salida.id()).orElse(null);
        List<Checkpoint> puntos = copia == null ? List.of() : copia.checkpoints();
        String inicio = puntos.isEmpty() ? null : puntos.get(0).hora();
        String fin = puntos.isEmpty() ? null : puntos.get(puntos.size() - 1).hora();
        int participantes = copia == null ? 0 : copia.participantes().size();
        return new ProximaSalida(salida.id(), salida.rutaNombre(), salida.fecha(), inicio, fin, participantes);
    }
}
