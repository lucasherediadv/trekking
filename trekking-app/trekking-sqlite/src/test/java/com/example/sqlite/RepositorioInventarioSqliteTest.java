package com.example.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.core.inventario.DatosElemento;
import com.example.core.inventario.ElementoInventario;
import com.example.core.inventario.EstadoElemento;

class RepositorioInventarioSqliteTest {

    @TempDir
    Path directorio;

    private BaseDatosSqlite base;
    private RepositorioInventarioSqlite repositorio;

    @BeforeEach
    void preparar() {
        base = new BaseDatosSqlite(directorio.resolve("inventario.db"));
        base.inicializarEsquema();
        repositorio = new RepositorioInventarioSqlite(base);
    }

    private int insertar(String nombre, int cantidad, EstadoElemento estado) {
        return repositorio.insertar(new DatosElemento(nombre, "Campamento", cantidad, estado));
    }

    @Test
    void guardaYRecuperaUnElementoConSuEstado() {
        int id = insertar("Carpa", 5, EstadoElemento.EN_REPARACION);

        ElementoInventario e = repositorio.buscarPorId(id).orElseThrow();

        assertEquals(new ElementoInventario(id, "Carpa", "Campamento", 5, EstadoElemento.EN_REPARACION, 0), e);
        assertTrue(repositorio.buscarPorId(id + 1).isEmpty());
    }

    @Test
    void guardaLosMismosTextosDeEstadoQueAntes() throws SQLException {
        insertar("Carpa", 5, EstadoElemento.FUERA_DE_SERVICIO);

        assertEquals("Fuera de Servicio", consultarTexto("SELECT estado FROM equipamiento"));
    }

    @Test
    void actualizaYSeparaActivosDeDadosDeBaja() {
        int id = insertar("Carpa", 5, EstadoElemento.DISPONIBLE);
        int otro = insertar("Soga", 2, EstadoElemento.DISPONIBLE);

        repositorio.actualizar(id, new DatosElemento("Carpa 4p", "Seguridad", 7, EstadoElemento.EN_INSPECCION));
        repositorio.cambiarActivo(otro, false);

        assertEquals(List.of(new ElementoInventario(id, "Carpa 4p", "Seguridad", 7, EstadoElemento.EN_INSPECCION, 0)),
                repositorio.listarActivos());
        assertEquals(List.of(otro), repositorio.listarDadosDeBaja().stream().map(ElementoInventario::id).toList());
    }

    @Test
    void laCantidadReservadaSumaSoloSalidasPlanificadas() throws SQLException {
        int carpa = insertar("Carpa", 10, EstadoElemento.DISPONIBLE);
        crearRuta();
        crearSalida(1, "planificada");
        crearSalida(2, "planificada");
        crearSalida(3, "realizada");
        reservar(1, carpa, 3);
        reservar(2, carpa, 2);
        reservar(3, carpa, 4);

        assertEquals(5, repositorio.buscarPorId(carpa).orElseThrow().cantidadReservada());
        assertEquals(5, repositorio.listarActivos().get(0).cantidadReservada());
        assertEquals(5, repositorio.buscarPorId(carpa).orElseThrow().cantidadDisponible());

        repositorio.cambiarActivo(carpa, false);
        assertEquals(5, repositorio.listarDadosDeBaja().get(0).cantidadReservada());
    }

    @Test
    void listarActivosExcluyendoRecibeLosEstadosPorParametro() {
        insertar("Disponible", 1, EstadoElemento.DISPONIBLE);
        insertar("Inspección", 1, EstadoElemento.EN_INSPECCION);
        insertar("Reparación", 1, EstadoElemento.EN_REPARACION);
        int baja = insertar("Baja", 1, EstadoElemento.DISPONIBLE);
        repositorio.cambiarActivo(baja, false);

        List<String> nombres = repositorio.listarActivosExcluyendo(EstadoElemento.noAsignables()).stream()
                .map(ElementoInventario::nombre).toList();

        assertEquals(List.of("Disponible", "Inspección"), nombres);
        assertEquals(3, repositorio.listarActivosExcluyendo(Set.of()).size());
    }

    @Test
    void elListadoSeOrdenaPorNombreSinDistinguirMayusculas() {
        insertar("b", 1, EstadoElemento.DISPONIBLE);
        insertar("A", 1, EstadoElemento.DISPONIBLE);

        assertEquals(List.of("A", "b"), repositorio.listarActivos().stream().map(ElementoInventario::nombre).toList());
    }

    @Test
    void laMigracionConvierteAsignadoEnDisponibleYEsIdempotente() throws SQLException {
        try (Connection c = base.conectar(); Statement s = c.createStatement()) {
            s.execute("INSERT INTO equipamiento (nombre, categoria, cantidad, estado) VALUES ('Vieja', 'Campamento', 3, 'Asignado')");
            s.execute("INSERT INTO equipamiento (nombre, categoria, cantidad, estado) VALUES ('Rota', 'Campamento', 3, 'En Reparación')");
        }

        base.inicializarEsquema();
        base.inicializarEsquema();

        assertEquals(List.of(EstadoElemento.DISPONIBLE, EstadoElemento.EN_REPARACION),
                repositorio.listarActivos().stream().map(ElementoInventario::estado).toList().stream()
                        .sorted().toList());
    }

    private void crearRuta() throws SQLException {
        ejecutar("INSERT INTO rutas (id, nombre, nombre_clave, altitud_maxima, tipo_terreno, dificultad_tecnica, dificultad_fisica) "
                + "VALUES (1, 'Ruta', 'ruta', 1000, 'Mixto', 'Baja', 'Baja')");
    }

    private void crearSalida(int id, String estado) throws SQLException {
        ejecutar("INSERT INTO salidas (id, ruta_id, fecha, estado, ruta_nombre, ruta_altitud_maxima, ruta_tipo_terreno, "
                + "ruta_dificultad_tecnica, ruta_dificultad_fisica) VALUES (" + id + ", 1, '2026-12-01', '" + estado
                + "', 'Ruta', 1000, 'Mixto', 'Baja', 'Baja')");
    }

    private void reservar(int salida, int elemento, int cantidad) throws SQLException {
        ejecutar("INSERT INTO salida_equipamiento (salida_id, equipamiento_id, nombre_copia, categoria_copia, cantidad) "
                + "VALUES (" + salida + ", " + elemento + ", 'c', 'c', " + cantidad + ")");
    }

    private void ejecutar(String sql) throws SQLException {
        try (Connection c = base.conectar(); Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }

    private String consultarTexto(String sql) throws SQLException {
        try (Connection c = base.conectar(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
            return r.next() ? r.getString(1) : null;
        }
    }
}
