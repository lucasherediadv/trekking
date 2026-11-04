package com.example.desktop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.example.core.cliente.Cliente;

public class ClienteDAO {

    /** Transitorio hasta el ticket 08: inserta un Cliente dentro de la transacción de confirmación de una Salida. */
    public int insertarEnTransaccion(Connection conexion, Cliente cliente, byte[] fichaMedica,
            String nombreFichaMedica) throws SQLException {
        String sql = """
                INSERT INTO clientes (dni, nombre, apellido, fecha_nacimiento, email, telefono, sexo,
                    contacto_emergencia_nombre, contacto_emergencia_telefono,
                    contacto_emergencia_relacion, autorizacion_menores, tutor_nombre,
                    tutor_apellido, tutor_dni, tutor_telefono, activo, ficha_medica,
                    nombre_ficha_medica, fecha_alta, fecha_modificacion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, CURRENT_DATE, CURRENT_DATE)
                """;
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cliente.dni());
            ps.setString(2, cliente.nombre());
            ps.setString(3, cliente.apellido());
            ps.setString(4, cliente.fechaNacimiento().toString());
            ps.setString(5, textoONull(cliente.email()));
            ps.setString(6, cliente.telefono());
            ps.setString(7, cliente.sexo());
            ps.setString(8, textoONull(cliente.contactoEmergenciaNombre()));
            ps.setString(9, textoONull(cliente.contactoEmergenciaTelefono()));
            ps.setString(10, textoONull(cliente.contactoEmergenciaRelacion()));
            ps.setBoolean(11, cliente.autorizacionMenores());
            ps.setString(12, textoONull(cliente.tutorNombre()));
            ps.setString(13, textoONull(cliente.tutorApellido()));
            ps.setString(14, textoONull(cliente.tutorDni()));
            ps.setString(15, textoONull(cliente.tutorTelefono()));
            ps.setBytes(16, fichaMedica);
            ps.setString(17, textoONull(nombreFichaMedica));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No se pudo guardar el Cliente nuevo.");
                int id = keys.getInt(1);
                sincronizarSexoYContacto(conexion, id, cliente.sexo(),
                        cliente.contactoEmergenciaNombre(), cliente.contactoEmergenciaTelefono(),
                        cliente.contactoEmergenciaRelacion());
                return id;
            }
        }
    }

    private static void sincronizarSexoYContacto(
            Connection conexion,
            int clienteId,
            String sexo,
            String contactoNombre,
            String contactoTelefono,
            String contactoRelacion)
            throws SQLException {

        // -----------------------------------------------------
        // Sincronizar sexo
        // -----------------------------------------------------

        try (
                PreparedStatement statement =
                        conexion.prepareStatement("""
                                UPDATE clientes
                                SET sexo_id = (
                                    SELECT id
                                    FROM sexos
                                    WHERE nombre = ?
                                )
                                WHERE id = ?
                                """)) {

            statement.setString(1, sexo);
            statement.setInt(2, clienteId);
            statement.executeUpdate();
        }

        // -----------------------------------------------------
        // Sincronizar contacto de emergencia
        // -----------------------------------------------------

        boolean tieneContacto =
                tieneTexto(contactoNombre)
                        && tieneTexto(contactoTelefono)
                        && tieneTexto(contactoRelacion);

        if (tieneContacto) {

            try (
                    PreparedStatement statement =
                            conexion.prepareStatement("""
                                    INSERT INTO contactos_emergencia (
                                        cliente_id,
                                        nombre,
                                        telefono,
                                        relacion
                                    )
                                    VALUES (?, ?, ?, ?)
                                    ON CONFLICT(cliente_id)
                                    DO UPDATE SET
                                        nombre = excluded.nombre,
                                        telefono = excluded.telefono,
                                        relacion = excluded.relacion
                                    """)) {

                statement.setInt(1, clienteId);
                statement.setString(2, contactoNombre);
                statement.setString(3, contactoTelefono);
                statement.setString(4, contactoRelacion);

                statement.executeUpdate();
            }

        } else {

            try (
                    PreparedStatement statement =
                            conexion.prepareStatement(
                                    "DELETE FROM contactos_emergencia "
                                            + "WHERE cliente_id = ?")) {

                statement.setInt(1, clienteId);
                statement.executeUpdate();
            }
        }
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }

    private static String textoONull(String valor) {
        return tieneTexto(valor) ? valor.trim() : null;
    }
}
