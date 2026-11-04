package com.example.desktop.dao;

import com.example.desktop.database.ConexionDB;
import com.example.desktop.model.Ruta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RutaDAO {

    public List<Ruta> obtenerTodas() {
        return obtenerPorEstado(true);
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
