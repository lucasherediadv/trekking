package com.example.core.salida;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.core.cliente.AlmacenFichasMedicasEnMemoria;
import com.example.core.cliente.CasosDeUsoClientes;
import com.example.core.cliente.CasosDeUsoFichasMedicas;
import com.example.core.cliente.DatosCliente;
import com.example.core.cliente.FichaMedica;
import com.example.core.cliente.RepositorioClientesEnMemoria;
import com.example.core.excepcion.ValidacionException;
import com.example.core.inventario.CasosDeUsoInventario;
import com.example.core.inventario.DatosElemento;
import com.example.core.inventario.EstadoElemento;
import com.example.core.inventario.RepositorioInventarioEnMemoria;
import com.example.core.inventario.StockInsuficienteException;
import com.example.core.itinerario.Checkpoint;
import com.example.core.itinerario.CasosDeUsoItinerario;
import com.example.core.itinerario.Itinerario;
import com.example.core.itinerario.RepositorioItinerariosEnMemoria;
import com.example.core.ruta.CasosDeUsoRutas;
import com.example.core.ruta.DatosRuta;
import com.example.core.ruta.RepositorioRutasEnMemoria;
import com.example.core.transaccion.TransaccionDirecta;
import com.example.core.salida.DatosSalida.ClienteNuevo;
import com.example.core.salida.DatosSalida.ElementoNuevo;
import com.example.core.salida.DatosSalida.LineaEquipamiento;

class CasosDeUsoConfirmarSalidaTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 12, 1);

    private RepositorioSalidasEnMemoria salidas;
    private CasosDeUsoRutas rutas;
    private CasosDeUsoItinerario itinerarios;
    private CasosDeUsoInventario inventario;
    private CasosDeUsoClientes clientes;
    private CasosDeUsoFichasMedicas fichas;
    private AlmacenFichasMedicasEnMemoria almacen;
    private RepositorioInventarioEnMemoria repositorioInventario;
    private CasosDeUsoConfirmarSalida confirmar;
    private int rutaId;
    private int sogaId;

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
        almacen = new AlmacenFichasMedicasEnMemoria();
        fichas = new CasosDeUsoFichasMedicas(repositorioClientes, almacen);
        confirmar = new CasosDeUsoConfirmarSalida(new TransaccionDirecta(),
                salidas, rutas, itinerarios, inventario, clientes, fichas);

        rutaId = rutas.alta(new DatosRuta("Uritorco", 1000, "Rocoso", "Media", "Alta")).id();
        itinerarios.guardar(rutaId, Itinerario.de(List.of(checkpoint("Inicio", "08:00"), checkpoint("Cima", "11:00"))));
        sogaId = inventario.alta(new DatosElemento("Soga", "Seguridad", 5, EstadoElemento.DISPONIBLE)).id();
    }

    private static Checkpoint checkpoint(String nombre, String hora) {
        return new Checkpoint(0, nombre, hora, -31.4, -64.2, 500, "");
    }

    private static DatosCliente datosCliente(String dni) {
        return new DatosCliente(dni, "Ana", "Paz", LocalDate.of(1990, 5, 20), null, "3511234567", "Femenino",
                null, null, null, false, null, null, null, null);
    }

    private DatosSalida datos(List<LineaEquipamiento> equipamiento) {
        return new DatosSalida(FECHA, rutaId, equipamiento, List.of(), List.of(), List.of());
    }

    @Test
    void confirmaUnaSalidaValidaConSuCopiaOperativa() {
        int clienteId = clientes.alta(datosCliente("30111222")).id();
        Salida salida = confirmar.confirmar(new DatosSalida(FECHA, rutaId,
                List.of(new LineaEquipamiento(sogaId, 3)), List.of(), List.of(clienteId), List.of()));

        assertEquals(EstadoSalida.PLANIFICADA, salida.estado());
        assertEquals(FECHA, salida.fecha());
        CopiaOperativa copia = salidas.copia(salida.id());
        assertEquals("Uritorco", copia.ruta().nombre());
        assertEquals(List.of("Inicio", "Cima"), copia.checkpoints().stream().map(Checkpoint::nombre).toList());
        assertEquals(3, copia.equipamiento().get(0).cantidad());
        assertEquals("Soga", copia.equipamiento().get(0).elemento().nombre());
        assertEquals("30111222", copia.participantes().get(0).dni());
    }

    @Test
    void rechazaStockInsuficienteIdentificandoElElemento() {
        StockInsuficienteException e = assertThrows(StockInsuficienteException.class,
                () -> confirmar.confirmar(datos(List.of(new LineaEquipamiento(sogaId, 6)))));
        assertEquals("Soga", e.elemento());
        assertEquals(5, e.disponible());
        assertTrue(salidas.listar().isEmpty());
    }

    @Test
    void descuentaLaReservaDeOtrasSalidasYSumaLineasRepetidas() {
        repositorioInventario.reservar(sogaId, 3);
        assertThrows(StockInsuficienteException.class,
                () -> confirmar.confirmar(datos(List.of(new LineaEquipamiento(sogaId, 2), new LineaEquipamiento(sogaId, 1)))));
        confirmar.confirmar(datos(List.of(new LineaEquipamiento(sogaId, 2))));
    }

    @Test
    void rechazaElementosNoAsignablesPorEstadoOBaja() {
        int roto = inventario.alta(new DatosElemento("Casco", "Seguridad", 5, EstadoElemento.EN_REPARACION)).id();
        int baja = inventario.alta(new DatosElemento("Mochila", "Campamento", 5, EstadoElemento.DISPONIBLE)).id();
        inventario.darDeBaja(baja);
        assertThrows(ValidacionException.class, () -> confirmar.confirmar(datos(List.of(new LineaEquipamiento(roto, 1)))));
        assertThrows(ValidacionException.class, () -> confirmar.confirmar(datos(List.of(new LineaEquipamiento(baja, 1)))));
        assertTrue(salidas.listar().isEmpty());
    }

    @Test
    void rechazaRutaBorradorYRutaDadaDeBaja() {
        int borrador = rutas.alta(new DatosRuta("Borrador", 900, "Rocoso", "Baja", "Baja")).id();
        rutas.darDeBaja(rutaId);
        assertThrows(ValidacionException.class,
                () -> confirmar.confirmar(new DatosSalida(FECHA, borrador, List.of(), List.of(), List.of(), List.of())));
        assertThrows(ValidacionException.class, () -> confirmar.confirmar(datos(List.of())));
        assertTrue(salidas.listar().isEmpty());
    }

    @Test
    void rechazaSalidaSinFecha() {
        assertThrows(ValidacionException.class, () -> confirmar.confirmar(
                new DatosSalida(null, rutaId, List.of(), List.of(), List.of(), List.of())));
    }

    @Test
    void creaElementosYClientesNuevosJuntoConLaSalida() {
        Salida salida = confirmar.confirmar(new DatosSalida(FECHA, rutaId, List.of(),
                List.of(new ElementoNuevo(new DatosElemento("Linterna", "Seguridad", 4, EstadoElemento.DISPONIBLE), 2)),
                List.of(), List.of(new ClienteNuevo(datosCliente("30999888"), new FichaMedica("ficha.pdf", new byte[] {1})))));

        CopiaOperativa copia = salidas.copia(salida.id());
        assertEquals("Linterna", copia.equipamiento().get(0).elemento().nombre());
        assertEquals(2, copia.equipamiento().get(0).cantidad());
        int clienteId = copia.participantes().get(0).id();
        assertTrue(almacen.buscar(clienteId).isPresent());
        assertEquals(4, inventario.obtener(copia.equipamiento().get(0).elemento().id()).cantidad());
    }

    @Test
    void rechazaElementoNuevoAsignadoEnMasDeLoQueExiste() {
        assertThrows(StockInsuficienteException.class, () -> confirmar.confirmar(new DatosSalida(FECHA, rutaId, List.of(),
                List.of(new ElementoNuevo(new DatosElemento("Linterna", "Seguridad", 1, EstadoElemento.DISPONIBLE), 2)),
                List.of(), List.of())));
        assertTrue(inventario.listarActivos().stream().noneMatch(e -> e.nombre().equals("Linterna")));
    }

    @Test
    void rechazaClientesInactivosRepetidosOConDniYaRegistrado() {
        int inactivo = clientes.alta(datosCliente("30111222")).id();
        clientes.darDeBaja(inactivo);
        int activo = clientes.alta(datosCliente("30333444")).id();
        assertThrows(ValidacionException.class, () -> confirmar.confirmar(new DatosSalida(FECHA, rutaId,
                List.of(), List.of(), List.of(inactivo), List.of())));
        assertThrows(ValidacionException.class, () -> confirmar.confirmar(new DatosSalida(FECHA, rutaId,
                List.of(), List.of(), List.of(activo, activo), List.of())));
        assertThrows(ValidacionException.class, () -> confirmar.confirmar(new DatosSalida(FECHA, rutaId,
                List.of(), List.of(), List.of(), List.of(new ClienteNuevo(datosCliente("30333444"), null)))));
    }

    @Test
    void laCopiaOperativaNoCambiaAlModificarLosCatalogosDespues() {
        int clienteId = clientes.alta(datosCliente("30111222")).id();
        Salida salida = confirmar.confirmar(new DatosSalida(FECHA, rutaId,
                List.of(new LineaEquipamiento(sogaId, 2)), List.of(), List.of(clienteId), List.of()));

        rutas.editar(rutaId, new DatosRuta("Otro nombre", 2000, "Mixto", "Alta", "Alta"));
        inventario.editar(sogaId, new DatosElemento("Cuerda", "Escalada", 9, EstadoElemento.DISPONIBLE));
        clientes.editar(clienteId, new DatosCliente("30111222", "Beatriz", "Paz", LocalDate.of(1990, 5, 20), null, "3511234567",
                "Femenino", null, null, null, false, null, null, null, null));

        CopiaOperativa copia = salidas.copia(salida.id());
        assertEquals("Uritorco", copia.ruta().nombre());
        assertEquals(1000, copia.ruta().altitudMaxima());
        assertEquals("Soga", copia.equipamiento().get(0).elemento().nombre());
        assertEquals("Ana", copia.participantes().get(0).nombre());
    }
}
