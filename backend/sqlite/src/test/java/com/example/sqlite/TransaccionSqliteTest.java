package com.example.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.core.cliente.CasosDeUsoClientes;
import com.example.core.cliente.CasosDeUsoFichasMedicas;
import com.example.core.cliente.DatosCliente;
import com.example.core.excepcion.ValidacionException;
import com.example.core.inventario.CasosDeUsoInventario;
import com.example.core.inventario.DatosElemento;
import com.example.core.inventario.EstadoElemento;
import com.example.core.inventario.StockInsuficienteException;
import com.example.core.itinerario.CasosDeUsoItinerario;
import com.example.core.itinerario.Checkpoint;
import com.example.core.itinerario.Itinerario;
import com.example.core.ruta.CasosDeUsoRutas;
import com.example.core.ruta.DatosRuta;
import com.example.core.salida.CasosDeUsoConfirmarSalida;
import com.example.core.salida.DatosSalida;
import com.example.core.salida.DatosSalida.ClienteNuevo;
import com.example.core.salida.DatosSalida.ElementoNuevo;
import com.example.core.salida.DatosSalida.LineaEquipamiento;
import com.example.core.salida.Salida;

class TransaccionSqliteTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 12, 1);

    @TempDir
    Path directorio;

    private BaseDatosSqlite base;
    private CasosDeUsoConfirmarSalida confirmar;
    private CasosDeUsoRutas rutas;
    private CasosDeUsoInventario inventario;
    private CasosDeUsoClientes clientes;
    private int rutaId;
    private int sogaId;

    @BeforeEach
    void preparar() {
        base = new BaseDatosSqlite(directorio.resolve("transaccion.db"));
        base.inicializarEsquema();
        Clock reloj = Clock.fixed(Instant.parse("2026-11-04T15:00:00Z"), ZoneOffset.UTC);
        RepositorioRutasSqlite repositorioRutas = new RepositorioRutasSqlite(base);
        RepositorioClientesSqlite repositorioClientes = new RepositorioClientesSqlite(base);
        rutas = new CasosDeUsoRutas(repositorioRutas);
        CasosDeUsoItinerario itinerarios = new CasosDeUsoItinerario(repositorioRutas, new RepositorioItinerariosSqlite(base));
        inventario = new CasosDeUsoInventario(new RepositorioInventarioSqlite(base));
        clientes = new CasosDeUsoClientes(repositorioClientes, reloj);
        confirmar = new CasosDeUsoConfirmarSalida(new TransaccionSqlite(base), new RepositorioSalidasSqlite(base),
                rutas, itinerarios, inventario, clientes,
                new CasosDeUsoFichasMedicas(repositorioClientes, new AlmacenFichasMedicasSqlite(base)));

        rutaId = rutas.alta(new DatosRuta("Uritorco", 1000, "Rocoso", "Media", "Alta")).id();
        itinerarios.guardar(rutaId, Itinerario.de(List.of(
                new Checkpoint(0, "Inicio", "08:00", -31.4, -64.2, 500, ""),
                new Checkpoint(0, "Cima", "11:00", -31.4, -64.2, 900, ""))));
        sogaId = inventario.alta(new DatosElemento("Soga", "Seguridad", 5, EstadoElemento.DISPONIBLE)).id();
    }

    private static DatosCliente datosCliente(String dni) {
        return new DatosCliente(dni, "Ana", "Paz", LocalDate.of(1990, 5, 20), null, "3511234567", "Femenino",
                null, null, null, false, null, null, null, null);
    }

    @Test
    void dosConfirmacionesConcurrentesNoReservanMasQueElStockTotal() throws Exception {
        int hilos = 2;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch largada = new CountDownLatch(1);
        try {
            List<Future<Salida>> resultados = new ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                Callable<Salida> confirmacion = () -> {
                    largada.await();
                    return confirmar.confirmar(new DatosSalida(FECHA, rutaId, List.of(new LineaEquipamiento(sogaId, 3)),
                            List.of(), List.of(), List.of()));
                };
                resultados.add(pool.submit(confirmacion));
            }
            largada.countDown();

            int confirmadas = 0;
            int rechazadas = 0;
            for (Future<Salida> resultado : resultados) {
                try {
                    resultado.get();
                    confirmadas++;
                } catch (java.util.concurrent.ExecutionException e) {
                    assertTrue(e.getCause() instanceof StockInsuficienteException, String.valueOf(e.getCause()));
                    rechazadas++;
                }
            }
            assertEquals(1, confirmadas);
            assertEquals(1, rechazadas);
            assertEquals(3, inventario.obtener(sogaId).cantidadReservada());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void siFallaLaConfirmacionNoQuedaNingunCambioGuardado() {
        clientes.alta(datosCliente("30111222"));
        // El elemento nuevo se crea antes de descubrir que el DNI del Cliente nuevo ya existe.
        assertThrows(ValidacionException.class, () -> confirmar.confirmar(new DatosSalida(FECHA, rutaId, List.of(),
                List.of(new ElementoNuevo(new DatosElemento("Linterna", "Seguridad", 4, EstadoElemento.DISPONIBLE), 1)),
                List.of(), List.of(new ClienteNuevo(datosCliente("30111222"), null)))));

        assertTrue(inventario.listarActivos().stream().noneMatch(e -> e.nombre().equals("Linterna")));
        assertTrue(new RepositorioSalidasSqlite(base).listar().isEmpty());
    }

    @Test
    void laCopiaOperativaGuardadaNoCambiaAlEditarLosCatalogos() {
        Salida salida = confirmar.confirmar(new DatosSalida(FECHA, rutaId, List.of(new LineaEquipamiento(sogaId, 2)),
                List.of(), List.of(), List.of(new ClienteNuevo(datosCliente("30111222"), null))));

        rutas.editar(rutaId, new DatosRuta("Otro nombre", 2000, "Mixto", "Alta", "Alta"));
        inventario.editar(sogaId, new DatosElemento("Cuerda", "Escalada", 9, EstadoElemento.DISPONIBLE));

        Salida guardada = new RepositorioSalidasSqlite(base).buscarPorId(salida.id()).orElseThrow();
        assertEquals("Uritorco", guardada.rutaNombre());
        assertEquals(1000, guardada.rutaAltitudMaxima());
        var resumen = new RepositorioSalidasSqlite(base).resumenes().get(salida.id());
        assertEquals("Soga × 2", resumen.equipamiento());
        assertEquals("Ana Paz", resumen.participantes());
        assertEquals(2, inventario.obtener(sogaId).cantidadReservada());
    }
}
