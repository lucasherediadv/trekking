package com.example.core.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.core.cliente.Cliente;
import com.example.core.cliente.DatosCliente;
import com.example.core.cliente.RepositorioClientesEnMemoria;
import com.example.core.dashboard.CasosDeUsoDashboard.ProximaSalida;
import com.example.core.inventario.DatosElemento;
import com.example.core.inventario.EstadoElemento;
import com.example.core.inventario.RepositorioInventarioEnMemoria;
import com.example.core.itinerario.Checkpoint;
import com.example.core.ruta.DatosRuta;
import com.example.core.ruta.Ruta;
import com.example.core.ruta.RepositorioRutasEnMemoria;
import com.example.core.salida.CopiaOperativa;
import com.example.core.salida.EstadoSalida;
import com.example.core.salida.RepositorioSalidasEnMemoria;

class CasosDeUsoDashboardTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 4);
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-04T15:00:00Z"), ZoneOffset.UTC);

    private RepositorioRutasEnMemoria rutas;
    private RepositorioClientesEnMemoria clientes;
    private RepositorioInventarioEnMemoria inventario;
    private RepositorioSalidasEnMemoria salidas;
    private CasosDeUsoDashboard dashboard;

    @BeforeEach
    void preparar() {
        rutas = new RepositorioRutasEnMemoria();
        clientes = new RepositorioClientesEnMemoria();
        inventario = new RepositorioInventarioEnMemoria();
        salidas = new RepositorioSalidasEnMemoria();
        dashboard = new CasosDeUsoDashboard(rutas, clientes, inventario, salidas, RELOJ);
    }

    private static Ruta ruta() {
        return new Ruta(1, "Uritorco", 1000, "Rocoso", "Media", "Alta", true, 2);
    }

    private static Checkpoint checkpoint(String hora) {
        return new Checkpoint(0, "P", hora, 0, 0, 0, null);
    }

    private static Cliente cliente(int id) {
        return new Cliente(id, "30" + id, "Ana", "Paz", LocalDate.of(1990, 1, 1), null, "351", "F",
                "Luis", "351", "Padre", false, null, null, null, null, true, HOY, null);
    }

    /** Guarda una Salida planificada y devuelve su id. */
    private int salida(LocalDate fecha, List<Checkpoint> puntos, int participantes) {
        List<Cliente> personas = java.util.stream.IntStream.rangeClosed(1, participantes)
                .mapToObj(CasosDeUsoDashboardTest::cliente).toList();
        return salidas.guardar(new CopiaOperativa(fecha, ruta(), puntos, List.of(), personas));
    }

    private int salida(LocalDate fecha) {
        return salida(fecha, List.of(checkpoint("08:00"), checkpoint("16:00")), 2);
    }

    @Test
    void incluyeSalidasDentroDeLaVentanaInclusiveYExcluyeLasDeFuera() {
        salida(HOY.minusDays(1));
        salida(HOY);
        salida(HOY.plusDays(7));
        salida(HOY.plusDays(8));
        assertEquals(2, dashboard.indicadores().salidasProximas());
        assertEquals(List.of(HOY, HOY.plusDays(7)),
                dashboard.proximasSalidas().stream().map(ProximaSalida::fecha).toList());
    }

    @Test
    void excluyeSalidasNoPlanificadas() {
        int realizada = salida(HOY);
        int cancelada = salida(HOY.plusDays(1));
        salida(HOY.plusDays(2));
        salidas.cerrarSiPlanificada(realizada, EstadoSalida.REALIZADA);
        salidas.cerrarSiPlanificada(cancelada, EstadoSalida.CANCELADA);
        assertEquals(1, dashboard.indicadores().salidasProximas());
        assertEquals(1, dashboard.proximasSalidas().size());
    }

    @Test
    void proximaSalidaTraeHorarioDelPrimeroYUltimoCheckpointYParticipantes() {
        int id = salida(HOY, List.of(checkpoint("07:30"), checkpoint("10:00"), checkpoint("15:45")), 3);
        ProximaSalida p = dashboard.proximasSalidas().get(0);
        assertEquals(id, p.id());
        assertEquals("Uritorco", p.nombreRuta());
        assertEquals("07:30", p.horaInicio());
        assertEquals("15:45", p.horaFin());
        assertEquals(3, p.participantes());
    }

    @Test
    void salidaSinCheckpointsNoTieneHorario() {
        salida(HOY, List.of(), 0);
        ProximaSalida p = dashboard.proximasSalidas().get(0);
        assertNull(p.horaInicio());
        assertNull(p.horaFin());
    }

    @Test
    void ordenaPorFechaYHoraYLimitaACinco() {
        salida(HOY.plusDays(2), List.of(checkpoint("09:00")), 0);
        int temprana = salida(HOY.plusDays(1), List.of(checkpoint("06:00")), 0);
        salida(HOY.plusDays(1), List.of(checkpoint("07:00")), 0);
        for (int i = 0; i < 4; i++) salida(HOY.plusDays(3));
        List<ProximaSalida> proximas = dashboard.proximasSalidas();
        assertEquals(5, proximas.size());
        assertEquals(temprana, proximas.get(0).id());
        assertEquals(7, dashboard.indicadores().salidasProximas());
    }

    @Test
    void cuentaRutasYClientesActivos() {
        rutas.insertar(new DatosRuta("A", 1, "t", "d", "f"));
        int baja = rutas.insertar(new DatosRuta("B", 1, "t", "d", "f"));
        rutas.cambiarActiva(baja, false);
        clientes.insertar(datosCliente("1"), HOY);
        int inactivo = clientes.insertar(datosCliente("2"), HOY);
        clientes.cambiarActivo(inactivo, false, HOY);
        CasosDeUsoDashboard.Indicadores i = dashboard.indicadores();
        assertEquals(1, i.rutasActivas());
        assertEquals(1, i.clientesActivos());
    }

    private static DatosCliente datosCliente(String dni) {
        return new DatosCliente(dni, "Ana", "Paz", LocalDate.of(1990, 1, 1), null, "351", "F",
                "Luis", "351", "Padre", false, null, null, null, null);
    }

    @Test
    void equipoNoDisponibleCuentaStockBajoYEstadosNoAsignablesSoloActivos() {
        inventario.insertar(new DatosElemento("Carpa", "Refugio", 10, EstadoElemento.DISPONIBLE)); // no
        inventario.insertar(new DatosElemento("Soga", "Seguridad", 2, EstadoElemento.DISPONIBLE)); // stock bajo
        inventario.insertar(new DatosElemento("Casco", "Seguridad", 10, EstadoElemento.EN_REPARACION)); // no asignable
        inventario.insertar(new DatosElemento("Mapa", "Navegación", 10, EstadoElemento.EN_INSPECCION)); // asignable
        inventario.insertar(new DatosElemento("Pico", "Herramientas", 10, EstadoElemento.EXTRAVIADO)); // no asignable
        int baja = inventario.insertar(new DatosElemento("Hacha", "Herramientas", 1, EstadoElemento.FUERA_DE_SERVICIO));
        inventario.cambiarActivo(baja, false);
        assertEquals(3, dashboard.indicadores().equipoNoDisponible());
    }

    @Test
    void indicadoresDeSalidasMideElEquipoCriticoPorCantidadDisponible() {
        int carpa = inventario.insertar(new DatosElemento("Carpa", "Refugio", 10, EstadoElemento.DISPONIBLE));
        inventario.insertar(new DatosElemento("Casco", "Seguridad", 10, EstadoElemento.EN_REPARACION));
        inventario.reservar(carpa, 8); // disponible 2
        assertEquals(1, dashboard.indicadores().equipoNoDisponible()); // solo el casco: la carpa tiene 10 en total
        assertEquals(2, dashboard.indicadoresDeSalidas().equipoNoDisponible());
    }
}
