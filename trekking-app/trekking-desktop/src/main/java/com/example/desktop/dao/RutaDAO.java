package com.example.desktop.dao;

import com.example.desktop.database.ConexionDB;
import com.example.desktop.model.Ruta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RutaDAO {

    public boolean existeNombre(String nombre, int idExcluido) {
        String sql = "SELECT id, nombre FROM rutas WHERE id <> ?";

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)) {

            statement.setInt(1, idExcluido);
            try (ResultSet resultSet = statement.executeQuery()) {
                String claveBuscada = Ruta.claveNombre(nombre);
                while (resultSet.next()) {
                    if (Ruta.claveNombre(resultSet.getString("nombre")).equals(claveBuscada)) {
                        return true;
                    }
                }
                return false;
            }

        } catch (SQLException e) {

            System.out.println(
                    "Error al comprobar el nombre: "
                            + e.getMessage());

            return true;
        }
    }

    public int insertar(Ruta ruta) {

        String sql = """
                INSERT INTO rutas
                (
                    nombre,
                    nombre_clave,
                    altitud_maxima,
                    tipo_terreno,
                    dificultad_tecnica,
                    dificultad_fisica
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(
                        sql,
                        Statement.RETURN_GENERATED_KEYS)) {

            String nombre = Ruta.normalizarNombre(ruta.getNombre());
            statement.setString(1, nombre);
            statement.setString(2, Ruta.claveNombre(nombre));
            statement.setDouble(3, ruta.getAltitudMaxima());
            statement.setString(4, ruta.getTipoTerreno());
            statement.setString(5, ruta.getDificultadTecnica());
            statement.setString(6, ruta.getDificultadFisica());

            statement.executeUpdate();

            try (ResultSet resultSet = statement.getGeneratedKeys()) {

                if (resultSet.next()) {

                    int idGenerado = resultSet.getInt(1);

                    System.out.println(
                            "Ruta guardada correctamente. ID: "
                                    + idGenerado);

                    return idGenerado;
                }
            }

            System.out.println(
                    "La ruta se guardó, pero no se pudo obtener su ID.");

            return -1;

        } catch (SQLException e) {

            System.out.println(
                    "Error al guardar la ruta: "
                            + e.getMessage());

            return -1;
        }
    }

    public boolean actualizar(Ruta ruta) {

        String sql = """
                UPDATE rutas
                SET
                    nombre = ?,
                    nombre_clave = ?,
                    altitud_maxima = ?,
                    tipo_terreno = ?,
                    dificultad_tecnica = ?,
                    dificultad_fisica = ?
                WHERE id = ?
                """;

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)) {

            String nombre = Ruta.normalizarNombre(ruta.getNombre());
            statement.setString(1, nombre);
            statement.setString(2, Ruta.claveNombre(nombre));
            statement.setDouble(3, ruta.getAltitudMaxima());
            statement.setString(4, ruta.getTipoTerreno());
            statement.setString(5, ruta.getDificultadTecnica());
            statement.setString(6, ruta.getDificultadFisica());
            statement.setInt(7, ruta.getId());

            int filasAfectadas = statement.executeUpdate();

            if (filasAfectadas > 0) {

                System.out.println(
                        "Ruta actualizada correctamente.");

                return true;
            }

            System.out.println("No se encontró la ruta.");

            return false;

        } catch (SQLException e) {

            System.out.println(
                    "Error al actualizar la ruta: "
                            + e.getMessage());

            return false;
        }
    }

    public boolean eliminar(int id) {

        String sql = "UPDATE rutas SET activo = 0 WHERE id = ?";

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)) {

            statement.setInt(1, id);

            int filasAfectadas = statement.executeUpdate();

            if (filasAfectadas > 0) {

                System.out.println(
                        "Ruta eliminada correctamente.");

                return true;
            }

            System.out.println("No se encontró la ruta.");

            return false;

        } catch (SQLException e) {

            System.out.println(
                    "Error al eliminar la ruta: "
                            + e.getMessage());

            return false;
        }
    }

    public boolean reactivar(int id) {
        String sql = "UPDATE rutas SET activo = 1 WHERE id = ?";
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al reactivar la ruta: " + e.getMessage());
            return false;
        }
    }

    public List<Ruta> obtenerTodas() {
        return obtenerPorEstado(true);
    }

    public List<Ruta> obtenerTodasInactivas() {
        return obtenerPorEstado(false);
    }

    private List<Ruta> obtenerPorEstado(boolean activa) {
        List<Ruta> rutas = new ArrayList<>();

        String sql = """
                SELECT
                    id,
                    nombre,
                    altitud_maxima,
                    tipo_terreno,
                    dificultad_tecnica,
                    dificultad_fisica
                FROM rutas
                WHERE activo = ?
                """;

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)) {

            statement.setInt(1, activa ? 1 : 0);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rutas.add(mapRow(resultSet));
                }
            }

        } catch (SQLException e) {

            System.out.println(
                    "Error al obtener las rutas por estado: "
                            + e.getMessage());
        }

        return rutas;
    }

    public List<Ruta> obtenerTodasIncluyendoInactivas() {

        String sql = """
                SELECT
                    id,
                    nombre,
                    altitud_maxima,
                    tipo_terreno,
                    dificultad_tecnica,
                    dificultad_fisica
                FROM rutas
                """;

        List<Ruta> rutas = new ArrayList<>();

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement =
                        conexion.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                rutas.add(mapRow(resultSet));
            }

        } catch (SQLException e) {

            System.out.println(
                    "Error al obtener todas las rutas: "
                            + e.getMessage());
        }

        return rutas;
    }

    public Ruta obtenerPorId(Connection conexion, int rutaId)
            throws SQLException {
        String sql = """
                SELECT
                    id,
                    nombre,
                    altitud_maxima,
                    tipo_terreno,
                    dificultad_tecnica,
                    dificultad_fisica
                FROM rutas
                WHERE id = ?
                """;

        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, rutaId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapRow(resultSet) : null;
            }
        }
    }

    public boolean estaActiva(int id) {

        String sql = "SELECT activo FROM rutas WHERE id = ?";

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement =
                        conexion.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                return resultSet.next()
                        && resultSet.getInt("activo") == 1;
            }

        } catch (SQLException e) {

            System.out.println(
                    "Error al consultar estado de la ruta: "
                            + e.getMessage());

            return false;
        }
    }

    private Ruta mapRow(ResultSet rs) throws SQLException {

        return new Ruta(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getDouble("altitud_maxima"),
                rs.getString("tipo_terreno"),
                rs.getString("dificultad_tecnica"),
                rs.getString("dificultad_fisica"));
    }
}
