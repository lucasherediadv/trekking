package com.example.sqlite;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.core.cliente.Cliente;
import com.example.core.cliente.DatosCliente;
import com.example.core.excepcion.EntidadNoEncontradaException;

class RepositorioClientesSqliteTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 4);

    @TempDir
    Path directorio;

    private BaseDatosSqlite base;
    private RepositorioClientesSqlite repositorio;

    @BeforeEach
    void preparar() {
        base = new BaseDatosSqlite(directorio.resolve("clientes.db"));
        base.inicializarEsquema();
        repositorio = new RepositorioClientesSqlite(base);
    }

    private static DatosCliente datos(String dni, String apellido, String contactoNombre,
            String contactoTelefono, String contactoRelacion) {
        return new DatosCliente(dni, "Ana", apellido, LocalDate.of(1990, 5, 20), "ana@mail.com", "3511234567",
                "Femenino", contactoNombre, contactoTelefono, contactoRelacion, false, null, null, null, null);
    }

    @Test
    void guardaYRecuperaUnClienteConSuContactoDeEmergencia() {
        int id = repositorio.insertar(datos("30123456", "Pérez", "Luis", "3517654321", "Hermano"), HOY);

        Cliente c = repositorio.buscarPorId(id).orElseThrow();

        assertEquals(new Cliente(id, "30123456", "Ana", "Pérez", LocalDate.of(1990, 5, 20), "ana@mail.com",
                "3511234567", "Femenino", "Luis", "3517654321", "Hermano", false, null, null, null, null, true,
                HOY, HOY), c);
        assertEquals(1, contar("SELECT COUNT(*) FROM contactos_emergencia WHERE cliente_id = " + id));
        assertEquals(c, repositorio.buscarPorDni("30123456").orElseThrow());
        assertTrue(repositorio.buscarPorId(id + 1).isEmpty());
        assertTrue(repositorio.buscarPorDni("99999999").isEmpty());
    }

    @Test
    void sinContactoNoCreaFilaYAlEditarLaActualizaOLaQuita() {
        int id = repositorio.insertar(datos("30123456", "Pérez", null, null, null), HOY);
        assertEquals(0, contar("SELECT COUNT(*) FROM contactos_emergencia"));
        assertNull(repositorio.buscarPorId(id).orElseThrow().contactoEmergenciaNombre());

        repositorio.actualizar(id, datos("30123456", "Pérez", "Luis", "3517654321", "Hermano"), HOY.plusDays(1));
        Cliente conContacto = repositorio.buscarPorId(id).orElseThrow();
        assertEquals("Luis", conContacto.contactoEmergenciaNombre());
        assertEquals(HOY.plusDays(1), conContacto.fechaModificacion());
        assertEquals(HOY, conContacto.fechaAlta());

        repositorio.actualizar(id, datos("30123456", "Pérez", "Marta", "3510000000", "Madre"), HOY);
        assertEquals("Marta", repositorio.buscarPorId(id).orElseThrow().contactoEmergenciaNombre());
        assertEquals(1, contar("SELECT COUNT(*) FROM contactos_emergencia"));

        repositorio.actualizar(id, datos("30123456", "Pérez", null, null, null), HOY);
        assertNull(repositorio.buscarPorId(id).orElseThrow().contactoEmergenciaNombre());
        assertEquals(0, contar("SELECT COUNT(*) FROM contactos_emergencia"));
    }

    @Test
    void bajaYReactivacionConservanElRegistro() {
        int id = repositorio.insertar(datos("30123456", "Pérez", null, null, null), HOY);

        repositorio.cambiarActivo(id, false, HOY);
        assertFalse(repositorio.buscarPorId(id).orElseThrow().activo());
        assertEquals(1, repositorio.listarTodos().size());

        repositorio.cambiarActivo(id, true, HOY);
        assertTrue(repositorio.buscarPorId(id).orElseThrow().activo());
    }

    @Test
    void listaTodosOrdenadosPorApellidoYNombre() {
        repositorio.insertar(datos("30000002", "Zárate", null, null, null), HOY);
        repositorio.insertar(datos("30000001", "Acosta", null, null, null), HOY);

        List<String> apellidos = repositorio.listarTodos().stream().map(Cliente::apellido).toList();

        assertEquals(List.of("Acosta", "Zárate"), apellidos);
    }

    @Test
    void operarSobreUnClienteInexistenteFalla() {
        assertThrows(EntidadNoEncontradaException.class,
                () -> repositorio.actualizar(99, datos("30123456", "Pérez", null, null, null), HOY));
        assertThrows(EntidadNoEncontradaException.class, () -> repositorio.cambiarActivo(99, false, HOY));
    }

    @Test
    void editarNoPierdeLaFichaMedicaGuardada() throws SQLException {
        int id = repositorio.insertar(datos("30123456", "Pérez", null, null, null), HOY);
        try (Connection c = base.conectar(); PreparedStatement ps = c.prepareStatement(
                "UPDATE clientes SET ficha_medica = ?, nombre_ficha_medica = 'ficha.pdf' WHERE id = ?")) {
            ps.setBytes(1, new byte[] {1, 2, 3});
            ps.setInt(2, id);
            ps.executeUpdate();
        }

        repositorio.actualizar(id, datos("30123456", "Gómez", null, null, null), HOY);

        try (Connection c = base.conectar(); Statement st = c.createStatement();
                ResultSet rs = st.executeQuery("SELECT ficha_medica, nombre_ficha_medica FROM clientes")) {
            assertTrue(rs.next());
            assertArrayEquals(new byte[] {1, 2, 3}, rs.getBytes(1));
            assertEquals("ficha.pdf", rs.getString(2));
        }
    }

    private int contar(String sql) {
        try (Connection c = base.conectar(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
