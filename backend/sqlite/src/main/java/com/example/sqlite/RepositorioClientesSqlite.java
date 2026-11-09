package com.example.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.example.core.cliente.Cliente;
import com.example.core.cliente.DatosCliente;
import com.example.core.cliente.RepositorioClientes;
import com.example.core.excepcion.EntidadNoEncontradaException;

/**
 * Implementación SQLite de Clientes. Mantiene sincronizadas las tablas {@code sexos} y
 * {@code contactos_emergencia}. No toca las columnas de la ficha médica.
 */
public class RepositorioClientesSqlite implements RepositorioClientes {

    private static final String SELECT_CLIENTES = """
            SELECT c.id, c.dni, c.nombre, c.apellido, c.fecha_nacimiento, c.email, c.telefono,
                   COALESCE(s.nombre, c.sexo) AS sexo,
                   COALESCE(ce.nombre, c.contacto_emergencia_nombre) AS contacto_nombre,
                   COALESCE(ce.telefono, c.contacto_emergencia_telefono) AS contacto_telefono,
                   COALESCE(ce.relacion, c.contacto_emergencia_relacion) AS contacto_relacion,
                   c.autorizacion_menores, c.tutor_nombre, c.tutor_apellido, c.tutor_dni, c.tutor_telefono,
                   c.activo, c.fecha_alta, c.fecha_modificacion
            FROM clientes c
            LEFT JOIN sexos s ON s.id = c.sexo_id
            LEFT JOIN contactos_emergencia ce ON ce.cliente_id = c.id
            """;

    private final BaseDatosSqlite base;

    public RepositorioClientesSqlite(BaseDatosSqlite base) {
        this.base = Objects.requireNonNull(base, "base");
    }

    @Override
    public int insertar(DatosCliente d, LocalDate hoy) {
        String sql = """
                INSERT INTO clientes (dni, nombre, apellido, fecha_nacimiento, email, telefono, sexo,
                    contacto_emergencia_nombre, contacto_emergencia_telefono, contacto_emergencia_relacion,
                    autorizacion_menores, tutor_nombre, tutor_apellido, tutor_dni, tutor_telefono,
                    activo, fecha_alta, fecha_modificacion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
                """;
        try (Connection c = base.conectar()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                int i = asignarDatos(ps, d);
                ps.setString(i++, hoy.toString());
                ps.setString(i, hoy.toString());
                ps.executeUpdate();
                int id;
                try (ResultSet claves = ps.getGeneratedKeys()) {
                    if (!claves.next()) throw new SQLException("No se generó el id del Cliente.");
                    id = claves.getInt(1);
                }
                sincronizarSexoYContacto(c, id, d);
                c.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar el Cliente.", e);
        }
    }

    @Override
    public void actualizar(int id, DatosCliente d, LocalDate hoy) {
        String sql = """
                UPDATE clientes SET dni = ?, nombre = ?, apellido = ?, fecha_nacimiento = ?, email = ?,
                    telefono = ?, sexo = ?, contacto_emergencia_nombre = ?, contacto_emergencia_telefono = ?,
                    contacto_emergencia_relacion = ?, autorizacion_menores = ?, tutor_nombre = ?,
                    tutor_apellido = ?, tutor_dni = ?, tutor_telefono = ?, fecha_modificacion = ?
                WHERE id = ?
                """;
        try (Connection c = base.conectar()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                int i = asignarDatos(ps, d);
                ps.setString(i++, hoy.toString());
                ps.setInt(i, id);
                if (ps.executeUpdate() == 0) throw new EntidadNoEncontradaException("Cliente", id);
                sincronizarSexoYContacto(c, id, d);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo actualizar el Cliente.", e);
        }
    }

    @Override
    public void cambiarActivo(int id, boolean activo, LocalDate hoy) {
        try (Connection c = base.conectar(); PreparedStatement ps = c.prepareStatement(
                "UPDATE clientes SET activo = ?, fecha_modificacion = ? WHERE id = ?")) {
            ps.setInt(1, activo ? 1 : 0);
            ps.setString(2, hoy.toString());
            ps.setInt(3, id);
            if (ps.executeUpdate() == 0) throw new EntidadNoEncontradaException("Cliente", id);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo cambiar el estado del Cliente.", e);
        }
    }

    @Override
    public Optional<Cliente> buscarPorId(int id) {
        return buscar("WHERE c.id = ?", ps -> ps.setInt(1, id));
    }

    @Override
    public Optional<Cliente> buscarPorDni(String dni) {
        return buscar("WHERE c.dni = ?", ps -> ps.setString(1, dni));
    }

    @Override
    public List<Cliente> listarTodos() {
        try (Connection c = base.conectar(); Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(SELECT_CLIENTES + " ORDER BY c.apellido, c.nombre")) {
            List<Cliente> clientes = new ArrayList<>();
            while (rs.next()) clientes.add(mapear(rs));
            return clientes;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron listar los Clientes.", e);
        }
    }

    private interface Parametros {
        void asignar(PreparedStatement ps) throws SQLException;
    }

    private Optional<Cliente> buscar(String where, Parametros parametros) {
        try (Connection c = base.conectar(); PreparedStatement ps = c.prepareStatement(SELECT_CLIENTES + where)) {
            parametros.asignar(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar el Cliente.", e);
        }
    }

    /** Asigna los 15 datos editables y devuelve el próximo índice libre. */
    private static int asignarDatos(PreparedStatement ps, DatosCliente d) throws SQLException {
        ps.setString(1, d.dni());
        ps.setString(2, d.nombre());
        ps.setString(3, d.apellido());
        ps.setString(4, d.fechaNacimiento().toString());
        ps.setString(5, d.email());
        ps.setString(6, d.telefono());
        ps.setString(7, d.sexo());
        ps.setString(8, d.contactoEmergenciaNombre());
        ps.setString(9, d.contactoEmergenciaTelefono());
        ps.setString(10, d.contactoEmergenciaRelacion());
        ps.setBoolean(11, d.autorizacionMenores());
        ps.setString(12, d.tutorNombre());
        ps.setString(13, d.tutorApellido());
        ps.setString(14, d.tutorDni());
        ps.setString(15, d.tutorTelefono());
        return 16;
    }

    private static void sincronizarSexoYContacto(Connection c, int id, DatosCliente d) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE clientes SET sexo_id = (SELECT id FROM sexos WHERE nombre = ?) WHERE id = ?")) {
            ps.setString(1, d.sexo());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
        boolean tieneContacto = d.contactoEmergenciaNombre() != null && d.contactoEmergenciaTelefono() != null
                && d.contactoEmergenciaRelacion() != null;
        if (tieneContacto) {
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO contactos_emergencia (cliente_id, nombre, telefono, relacion)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT(cliente_id) DO UPDATE SET nombre = excluded.nombre,
                        telefono = excluded.telefono, relacion = excluded.relacion
                    """)) {
                ps.setInt(1, id);
                ps.setString(2, d.contactoEmergenciaNombre());
                ps.setString(3, d.contactoEmergenciaTelefono());
                ps.setString(4, d.contactoEmergenciaRelacion());
                ps.executeUpdate();
            }
        } else {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM contactos_emergencia WHERE cliente_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        }
    }

    private static Cliente mapear(ResultSet rs) throws SQLException {
        return new Cliente(rs.getInt("id"), rs.getString("dni"), rs.getString("nombre"), rs.getString("apellido"),
                fecha(rs.getString("fecha_nacimiento")), rs.getString("email"), rs.getString("telefono"),
                rs.getString("sexo"), rs.getString("contacto_nombre"), rs.getString("contacto_telefono"),
                rs.getString("contacto_relacion"), rs.getBoolean("autorizacion_menores"),
                rs.getString("tutor_nombre"), rs.getString("tutor_apellido"), rs.getString("tutor_dni"),
                rs.getString("tutor_telefono"), rs.getBoolean("activo"), fecha(rs.getString("fecha_alta")),
                fecha(rs.getString("fecha_modificacion")));
    }

    private static LocalDate fecha(String texto) {
        return texto == null || texto.isEmpty() ? null : LocalDate.parse(texto);
    }
}
