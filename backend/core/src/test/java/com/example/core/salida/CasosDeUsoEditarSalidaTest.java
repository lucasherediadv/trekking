package com.example.core.salida;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.core.cliente.AlmacenFichasMedicasEnMemoria;
import com.example.core.cliente.CasosDeUsoClientes;
import com.example.core.cliente.CasosDeUsoFichasMedicas;
import com.example.core.cliente.DatosCliente;
import com.example.core.cliente.RepositorioClientesEnMemoria;
import com.example.core.excepcion.ValidacionException;
import com.example.core.inventario.CasosDeUsoInventario;
import com.example.core.inventario.DatosElemento;
import com.example.core.inventario.EstadoElemento;
import com.example.core.inventario.RepositorioInventarioEnMemoria;
import com.example.core.inventario.StockInsuficienteException;
import com.example.core.itinerario.CasosDeUsoItinerario;
import com.example.core.itinerario.Checkpoint;
import com.example.core.itinerario.Itinerario;
import com.example.core.itinerario.RepositorioItinerariosEnMemoria;
import com.example.core.ruta.CasosDeUsoRutas;
import com.example.core.ruta.DatosRuta;
import com.example.core.ruta.RepositorioRutasEnMemoria;
import com.example.core.salida.DatosSalida.LineaEquipamiento;
import com.example.core.transaccion.TransaccionDirecta;

class CasosDeUsoEditarSalidaTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 12, 1);

    private RepositorioSalidasEnMemoria salidas;
    private CasosDeUsoRutas rutas;
    private CasosDeUsoItinerario itinerarios;
    private CasosDeUsoInventario inventario;
    private CasosDeUsoClientes clientes;
    private RepositorioInventarioEnMemoria repositorioInventario;
    private CasosDeUsoEditarSalida editar;
    private int rutaId;
    private int sogaId;
    private int clienteId;
    private int salidaId;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-04T15:00:00Z"), ZoneOffset.UTC);
        salidas = new RepositorioSalidasEnMemoria();
        RepositorioRutasEnMemoria repositorioRutas = new RepositorioRutasEnMemoria();
        rutas = new CasosDeUsoRutas(repositorioRutas);
        itinerarios = new CasosDeUsoItinerario(repositorioRutas, new RepositorioItinerariosEnMemoria(repositorioRutas));
        repositorioInventario = new RepositorioInventarioEnMemoria();
        inventario = new CasosDeUsoInventario(repositorioInventario);
        RepositorioClientesEnMemoria repositorioClientes = new RepositorioClientesEnMemoria();
        clientes = new CasosDeUsoClientes(repositorioClientes, reloj);
        CasosDeUsoFichasMedicas fichas = new CasosDeUsoFichasMedicas(repositorioClientes, new AlmacenFichasMedicasEnMemoria());
        TransaccionDirecta transaccion = new TransaccionDirecta();
        editar = new CasosDeUsoEditarSalida(transaccion, salidas, rutas, itinerarios, inventario, clientes);

        rutaId = rutas.alta(new DatosRuta("Uritorco", 1000, "Rocoso", "Media", "Alta")).id();
        itinerarios.guardar(rutaId, Itinerario.de(List.of(checkpoint("Inicio", "08:00", 500), checkpoint("Cima", "11:00", 900))));
        sogaId = inventario.alta(new DatosElemento("Soga", "Seguridad", 5, EstadoElemento.DISPONIBLE)).id();
        clienteId = clientes.alta(datosCliente("30111222", "Ana")).id();

        salidaId = new CasosDeUsoConfirmarSalida(transaccion, salidas, rutas, itinerarios, inventario, clientes, fichas)
                .confirmar(new DatosSalida(FECHA, rutaId, List.of(new LineaEquipamiento(sogaId, 3)),
                        List.of(), List.of(clienteId), List.of())).id();
        repositorioInventario.reservar(sogaId, 3); // la Reserva que produce la Salida planificada
    }

    private static Checkpoint checkpoint(String nombre, String hora, double altitud) {
        return new Checkpoint(0, nombre, hora, -31.4, -64.2, altitud, "");
    }

    private static DatosCliente datosCliente(String dni, String nombre) {
        return new DatosCliente(dni, nombre, "Paz", LocalDate.of(1990, 5, 20), null, "3511234567", "Femenino",
                null, null, null, false, null, null, null, null);
    }

    /** Pedido que deja la Salida igual que está, para modificar solo lo que cada test necesita. */
    private DatosEdicionSalida sinCambios() {
        CopiaOperativa copia = editar.copia(salidaId);
        return new DatosEdicionSalida(copia.fecha(), rutaId, false, copia.checkpoints(), List.of(),
                List.of(new LineaEquipamiento(sogaId, 3)), List.of(clienteId), Set.of(), Set.of());
    }

    private DatosEdicionSalida conEquipamiento(List<LineaEquipamiento> equipamiento) {
        DatosEdicionSalida d = sinCambios();
        return new DatosEdicionSalida(d.fecha(), d.rutaId(), d.actualizarDesdeRuta(), d.checkpoints(),
                d.checkpointsNuevosParaRuta(), equipamiento, d.participantes(), d.refrescarEquipamiento(), d.refrescarParticipantes());
    }

    private DatosEdicionSalida conCheckpoints(List<Checkpoint> checkpoints, List<Checkpoint> nuevosParaRuta) {
        DatosEdicionSalida d = sinCambios();
        return new DatosEdicionSalida(d.fecha(), d.rutaId(), d.actualizarDesdeRuta(), checkpoints, nuevosParaRuta,
                d.equipamiento(), d.participantes(), d.refrescarEquipamiento(), d.refrescarParticipantes());
    }

    @Test
    void elReemplazoDeLaCopiaOperativaNoDejaVersionesAnteriores() {
        int sogaExtra = inventario.alta(new DatosElemento("Casco", "Seguridad", 4, EstadoElemento.DISPONIBLE)).id();
        int otroCliente = clientes.alta(datosCliente("30999888", "Eva")).id();
        LocalDate nueva = FECHA.plusDays(7);

        editar.editar(salidaId, new DatosEdicionSalida(nueva, rutaId, false, List.of(checkpoint("Solo", "09:00", 100)),
                List.of(), List.of(new LineaEquipamiento(sogaExtra, 2)), List.of(otroCliente), Set.of(), Set.of()));

        CopiaOperativa copia = editar.copia(salidaId);
        assertEquals(nueva, copia.fecha());
        assertEquals(List.of("Solo"), copia.checkpoints().stream().map(Checkpoint::nombre).toList());
        assertEquals(List.of("Casco"), copia.equipamiento().stream().map(l -> l.elemento().nombre()).toList());
        assertEquals(List.of("Eva"), copia.participantes().stream().map(c -> c.nombre()).toList());
        assertEquals(1, salidas.listar().size());
    }

    @Test
    void aumentarUnaCantidadPorEncimaDelStockEsRechazado() {
        StockInsuficienteException e = assertThrows(StockInsuficienteException.class,
                () -> editar.editar(salidaId, conEquipamiento(List.of(new LineaEquipamiento(sogaId, 6)))));
        assertEquals("Soga", e.elemento());
        assertEquals(5, e.disponible());
        assertEquals(3, editar.copia(salidaId).equipamiento().get(0).cantidad());
    }

    @Test
    void laReservaDeLaPropiaSalidaNoCuentaContraLaCantidadPedida() {
        editar.editar(salidaId, conEquipamiento(List.of(new LineaEquipamiento(sogaId, 5))));
        assertEquals(5, editar.copia(salidaId).equipamiento().get(0).cantidad());
    }

    @Test
    void laReservaDeOtrasSalidasSiCuenta() {
        repositorioInventario.reservar(sogaId, 4); // 3 de esta Salida + 1 de otra
        assertThrows(StockInsuficienteException.class,
                () -> editar.editar(salidaId, conEquipamiento(List.of(new LineaEquipamiento(sogaId, 5)))));
        editar.editar(salidaId, conEquipamiento(List.of(new LineaEquipamiento(sogaId, 4))));
    }

    @Test
    void unCheckpointNuevoPuedeGuardarseEnLaRutaActiva() {
        Checkpoint nuevo = checkpoint("Mirador", "10:00", 700);
        List<Checkpoint> copia = new java.util.ArrayList<>(editar.copia(salidaId).checkpoints());
        copia.add(nuevo);

        editar.editar(salidaId, conCheckpoints(copia, List.of(nuevo)));

        assertEquals(List.of("Inicio", "Mirador", "Cima"),
                itinerarios.obtener(rutaId).checkpoints().stream().map(Checkpoint::nombre).toList());
        assertEquals(List.of("Inicio", "Mirador", "Cima"),
                editar.copia(salidaId).checkpoints().stream().map(Checkpoint::nombre).toList());
    }

    @Test
    void unCambioLocalNoModificaLaRuta() {
        List<Checkpoint> local = List.of(checkpoint("Solo mío", "09:00", 100));

        editar.editar(salidaId, conCheckpoints(local, List.of()));

        assertEquals(List.of("Solo mío"), editar.copia(salidaId).checkpoints().stream().map(Checkpoint::nombre).toList());
        assertEquals(List.of("Inicio", "Cima"),
                itinerarios.obtener(rutaId).checkpoints().stream().map(Checkpoint::nombre).toList());
    }

    @Test
    void noGuardaEnLaRutaDadaDeBaja() {
        rutas.darDeBaja(rutaId);
        Checkpoint nuevo = checkpoint("Mirador", "10:00", 700);
        assertThrows(ValidacionException.class,
                () -> editar.editar(salidaId, conCheckpoints(List.of(nuevo), List.of(nuevo))));
        assertEquals(2, itinerarios.obtener(rutaId).cantidad());
    }

    @Test
    void losCheckpointsDeLaCopiaRespetanLaAltitudMaximaCopiada() {
        // La Ruta del catálogo sube su máxima, pero la Salida conserva la copiada (1000 m).
        rutas.editar(rutaId, new DatosRuta("Uritorco", 3000, "Rocoso", "Media", "Alta"));
        assertThrows(ValidacionException.class,
                () -> editar.editar(salidaId, conCheckpoints(List.of(checkpoint("Alto", "09:00", 1500)), List.of())));

        // Al actualizar desde la Ruta se copia la nueva máxima.
        DatosEdicionSalida d = conCheckpoints(List.of(checkpoint("Alto", "09:00", 1500)), List.of());
        editar.editar(salidaId, new DatosEdicionSalida(d.fecha(), rutaId, true, d.checkpoints(), List.of(),
                d.equipamiento(), d.participantes(), Set.of(), Set.of()));
        assertEquals(3000, editar.copia(salidaId).ruta().altitudMaxima());
    }

    @Test
    void unaSalidaCerradaNoEsEditablePorElFlujoNormal() {
        salidas.cerrarSiPlanificada(salidaId, EstadoSalida.REALIZADA);
        assertThrows(SalidaNoEditableException.class, () -> editar.editar(salidaId, sinCambios()));
    }

    @Test
    void conservaLaCopiaDeUnParticipanteDadoDeBajaPeroNoAceptaAgregarlo() {
        clientes.darDeBaja(clienteId);
        editar.editar(salidaId, sinCambios()); // sigue asociado
        int otro = clientes.alta(datosCliente("30333444", "Eva")).id();
        clientes.darDeBaja(otro);
        DatosEdicionSalida d = sinCambios();
        assertThrows(ValidacionException.class, () -> editar.editar(salidaId, new DatosEdicionSalida(d.fecha(), rutaId,
                false, d.checkpoints(), List.of(), d.equipamiento(), List.of(clienteId, otro), Set.of(), Set.of())));
    }

    @Test
    void rechazaFechaAusenteYClienteRepetido() {
        DatosEdicionSalida d = sinCambios();
        assertThrows(ValidacionException.class, () -> editar.editar(salidaId, new DatosEdicionSalida(null, rutaId,
                false, d.checkpoints(), List.of(), d.equipamiento(), d.participantes(), Set.of(), Set.of())));
        assertThrows(ValidacionException.class, () -> editar.editar(salidaId, new DatosEdicionSalida(d.fecha(), rutaId,
                false, d.checkpoints(), List.of(), d.equipamiento(), List.of(clienteId, clienteId), Set.of(), Set.of())));
        assertEquals(1, editar.copia(salidaId).participantes().size());
    }
}
