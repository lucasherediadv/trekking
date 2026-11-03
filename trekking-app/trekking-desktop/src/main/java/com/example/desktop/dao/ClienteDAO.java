package com.example.desktop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

import com.example.desktop.database.ConexionDB;
import com.example.desktop.model.Cliente;
import com.example.desktop.model.FichaMedica;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ClienteDAO {

    /** Inserta un Cliente dentro de la transacción de confirmación de una Salida. */
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
            ps.setString(1, cliente.getDni());
            ps.setString(2, cliente.getNombre());
            ps.setString(3, cliente.getApellido());
            ps.setString(4, cliente.getFechaNacimiento().toString());
            ps.setString(5, textoONull(cliente.getEmail()));
            ps.setString(6, cliente.getTelefono());
            ps.setString(7, cliente.getSexo());
            ps.setString(8, textoONull(cliente.getContactoEmergenciaNombre()));
            ps.setString(9, textoONull(cliente.getContactoEmergenciaTelefono()));
            ps.setString(10, textoONull(cliente.getContactoEmergenciaRelacion()));
            ps.setBoolean(11, cliente.isAutorizacionMenores());
            ps.setString(12, textoONull(cliente.getTutorNombre()));
            ps.setString(13, textoONull(cliente.getTutorApellido()));
            ps.setString(14, textoONull(cliente.getTutorDni()));
            ps.setString(15, textoONull(cliente.getTutorTelefono()));
            ps.setBytes(16, fichaMedica);
            ps.setString(17, textoONull(nombreFichaMedica));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No se pudo guardar el Cliente nuevo.");
                int id = keys.getInt(1);
                sincronizarSexoYContacto(conexion, id, cliente.getSexo(),
                        cliente.getContactoEmergenciaNombre(), cliente.getContactoEmergenciaTelefono(),
                        cliente.getContactoEmergenciaRelacion());
                return id;
            }
        }
    }

    // =========================================================
    // INSERTAR CLIENTE
    // =========================================================

    public boolean insertarCliente(
            String dni,
            String nombre,
            String apellido,
            LocalDate fechaNacimiento,
            String email,
            String telefono,
            String sexo,
            String contactoNombre,
            String contactoTelefono,
            String contactoRelacion,
            boolean activo,
            boolean autorizacionMenores,
            String tutorNombre,
            String tutorApellido,
            String tutorDni,
            String tutorTelefono,
            byte[] fichaMedica,
            String nombreFichaMedica) {

        String sql = """
                INSERT INTO clientes (
                    dni,
                    nombre,
                    apellido,
                    fecha_nacimiento,
                    email,
                    telefono,
                    sexo,
                    contacto_emergencia_nombre,
                    contacto_emergencia_telefono,
                    contacto_emergencia_relacion,
                    autorizacion_menores,
                    tutor_nombre,
                    tutor_apellido,
                    tutor_dni,
                    tutor_telefono,
                    activo,
                    ficha_medica,
                    nombre_ficha_medica,
                    fecha_alta,
                    fecha_modificacion
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE, CURRENT_DATE)
                """;

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)) {

            conexion.setAutoCommit(false);

            try {
                statement.setString(1, dni);
                statement.setString(2, nombre);
                statement.setString(3, apellido);
                statement.setString(4,
                        fechaNacimiento == null ? null : fechaNacimiento.toString());
                statement.setString(5, textoONull(email));
                statement.setString(6, textoONull(telefono));
                statement.setString(7, sexo);
                statement.setString(8, textoONull(contactoNombre));
                statement.setString(9, textoONull(contactoTelefono));
                statement.setString(10, textoONull(contactoRelacion));
                statement.setBoolean(11, autorizacionMenores);
                statement.setString(12, textoONull(tutorNombre));
                statement.setString(13, textoONull(tutorApellido));
                statement.setString(14, textoONull(tutorDni));
                statement.setString(15, textoONull(tutorTelefono));
                statement.setBoolean(16, activo);
                statement.setBytes(17, fichaMedica);
                statement.setString(18, textoONull(nombreFichaMedica));

                statement.executeUpdate();

                int clienteId = obtenerUltimoId(conexion);

                sincronizarSexoYContacto(
                        conexion,
                        clienteId,
                        sexo,
                        contactoNombre,
                        contactoTelefono,
                        contactoRelacion);

                conexion.commit();

                return true;

            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            }

        } catch (SQLException e) {
            System.out.println("Error al guardar cliente: " + e.getMessage());
            return false;
        }
    }

    // =========================================================
    // ACTUALIZAR CLIENTE
    // =========================================================

    public boolean actualizarCliente(
            int id,
            String dni,
            String nombre,
            String apellido,
            LocalDate fechaNacimiento,
            String email,
            String telefono,
            String sexo,
            String contactoNombre,
            String contactoTelefono,
            String contactoRelacion,
            boolean activo,
            boolean autorizacionMenores,
            String tutorNombre,
            String tutorApellido,
            String tutorDni,
            String tutorTelefono,
            byte[] fichaMedica,
            String nombreFichaMedica,
            boolean actualizarFichaMedica) {

        String sql = """
                UPDATE clientes SET
                    dni = ?,
                    nombre = ?,
                    apellido = ?,
                    fecha_nacimiento = ?,
                    email = ?,
                    telefono = ?,
                    sexo = ?,
                    contacto_emergencia_nombre = ?,
                    contacto_emergencia_telefono = ?,
                    contacto_emergencia_relacion = ?,
                    autorizacion_menores = ?,
                    tutor_nombre = ?,
                    tutor_apellido = ?,
                    tutor_dni = ?,
                    tutor_telefono = ?,
                    activo = ?
                """;

        if (actualizarFichaMedica) {
            sql += """
                    , ficha_medica = ?,
                      nombre_ficha_medica = ?
                    """;
        }

        sql += """
                , fecha_modificacion = CURRENT_DATE
                WHERE id = ?
                """;

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)) {

            conexion.setAutoCommit(false);

            try {
                statement.setString(1, dni);
                statement.setString(2, nombre);
                statement.setString(3, apellido);
                statement.setString(4,
                        fechaNacimiento == null ? null : fechaNacimiento.toString());
                statement.setString(5, textoONull(email));
                statement.setString(6, textoONull(telefono));
                statement.setString(7, sexo);
                statement.setString(8, textoONull(contactoNombre));
                statement.setString(9, textoONull(contactoTelefono));
                statement.setString(10, textoONull(contactoRelacion));
                statement.setBoolean(11, autorizacionMenores);
                statement.setString(12, textoONull(tutorNombre));
                statement.setString(13, textoONull(tutorApellido));
                statement.setString(14, textoONull(tutorDni));
                statement.setString(15, textoONull(tutorTelefono));
                statement.setBoolean(16, activo);

                int indiceId = 17;

                if (actualizarFichaMedica) {
                    statement.setBytes(17, fichaMedica);
                    statement.setString(18, textoONull(nombreFichaMedica));
                    indiceId = 19;
                }

                statement.setInt(indiceId, id);

                int filas = statement.executeUpdate();

                if (filas == 0) {
                    conexion.rollback();
                    return false;
                }

                sincronizarSexoYContacto(
                        conexion,
                        id,
                        sexo,
                        contactoNombre,
                        contactoTelefono,
                        contactoRelacion);

                conexion.commit();

                return true;

            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            }

        } catch (SQLException e) {
            System.out.println("Error al actualizar cliente: " + e.getMessage());
            return false;
        }
    }

    // =========================================================
    // BAJA LÓGICA
    // =========================================================

    public void eliminarCliente(int id) {

        String sql = """
                UPDATE clientes
                SET activo = 0,
                    fecha_modificacion = CURRENT_DATE
                WHERE id = ?
                """;

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)) {

            statement.setInt(1, id);

            int filas = statement.executeUpdate();

            if (filas == 0) {
                System.out.println("No se encontró ningún cliente con ese ID.");
            }

        } catch (SQLException e) {
            System.out.println("Error al eliminar cliente: " + e.getMessage());
        }
    }

    public void reactivarCliente(int id) {
        String sql = "UPDATE clientes SET activo = 1, fecha_modificacion = CURRENT_DATE WHERE id = ?";
        try (Connection conexion = ConexionDB.conectar(); PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo reactivar el Cliente.", e);
        }
    }

    // =========================================================
    // OBTENER FICHA MÉDICA
    // =========================================================

    public FichaMedica obtenerFichaMedica(int clienteId) {

        String sql = """
                SELECT ficha_medica, nombre_ficha_medica
                FROM clientes
                WHERE id = ?
                """;

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)) {

            statement.setInt(1, clienteId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()
                        && resultSet.getBytes("ficha_medica") != null) {

                    return new FichaMedica(
                            resultSet.getBytes("ficha_medica"),
                            resultSet.getString("nombre_ficha_medica"));
                }
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error al obtener la ficha médica: "
                            + e.getMessage());
        }

        return null;
    }

    // =========================================================
    // OBTENER TODOS LOS CLIENTES
    // =========================================================

    public ObservableList<Cliente> obtenerTodosLosClientes() {

        ObservableList<Cliente> clientes =
                FXCollections.observableArrayList();

        String sql = """
                SELECT
                    c.id,
                    c.dni,
                    c.nombre,
                    c.apellido,
                    c.fecha_nacimiento,
                    c.email,
                    c.telefono,
                    COALESCE(s.nombre, c.sexo) AS sexo,
                    COALESCE(
                        ce.nombre,
                        c.contacto_emergencia_nombre
                    ) AS contacto_nombre,
                    COALESCE(
                        ce.telefono,
                        c.contacto_emergencia_telefono
                    ) AS contacto_telefono,
                    COALESCE(
                        ce.relacion,
                        c.contacto_emergencia_relacion
                    ) AS contacto_relacion,
                    c.activo,
                    c.autorizacion_menores,
                    c.tutor_nombre,
                    c.tutor_apellido,
                    c.tutor_dni,
                    c.tutor_telefono,
                    c.fecha_alta,
                    c.fecha_modificacion
                FROM clientes c
                LEFT JOIN sexos s
                    ON s.id = c.sexo_id
                LEFT JOIN contactos_emergencia ce
                    ON ce.cliente_id = c.id
                ORDER BY c.apellido, c.nombre
                """;

        try (
                Connection conexion = ConexionDB.conectar();
                Statement statement = conexion.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {

                clientes.add(new Cliente(
                        resultSet.getInt("id"),
                        resultSet.getString("dni"),
                        resultSet.getString("nombre"),
                        resultSet.getString("apellido"),
                        parsearFecha(
                                resultSet.getString("fecha_nacimiento")),
                        resultSet.getString("email"),
                        resultSet.getString("telefono"),
                        resultSet.getString("sexo"),
                        resultSet.getString("contacto_nombre"),
                        resultSet.getString("contacto_telefono"),
                        resultSet.getString("contacto_relacion"),
                        resultSet.getBoolean("autorizacion_menores"),
                        resultSet.getString("tutor_nombre"),
                        resultSet.getString("tutor_apellido"),
                        resultSet.getString("tutor_dni"),
                        resultSet.getString("tutor_telefono"),
                        resultSet.getBoolean("activo"),
                        parsearFecha(
                                resultSet.getString("fecha_alta")),
                        parsearFecha(
                                resultSet.getString("fecha_modificacion"))
                ));
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error al obtener clientes: "
                            + e.getMessage());
        }

        return clientes;
    }

    // =========================================================
    // AUXILIARES
    // =========================================================

    private int obtenerUltimoId(Connection conexion)
            throws SQLException {

        try (
                Statement statement = conexion.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT last_insert_rowid()")) {

            return resultSet.next()
                    ? resultSet.getInt(1)
                    : 0;
        }
    }

    private void sincronizarSexoYContacto(
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

    private static LocalDate parsearFecha(String fecha) {

        return fecha == null || fecha.isEmpty()
                ? null
                : LocalDate.parse(fecha);
    }

    private static boolean tieneTexto(String valor) {

        return valor != null
                && !valor.trim().isEmpty();
    }

    private static String textoONull(String valor) {

        return tieneTexto(valor)
                ? valor.trim()
                : null;
    }
}
