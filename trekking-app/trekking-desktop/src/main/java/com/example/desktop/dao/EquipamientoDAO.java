package com.example.desktop.dao;

import com.example.desktop.database.ConexionDB;
import com.example.desktop.model.Equipamiento;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

import com.example.core.inventario.EstadoElemento;

/** Lecturas de inventario que todavía usa el área de Salidas; Inventario ya usa los casos de uso de core. */
public class EquipamientoDAO {

    /** Estados que impiden asignar, listados para SQL; la regla vive en core. */
    public static String estadosNoAsignablesSql() {
        return EstadoElemento.noAsignables().stream()
                .map(e -> "'" + e.etiqueta() + "'")
                .collect(Collectors.joining(", "));
    }

    public static ObservableList<Equipamiento> obtenerTodos() {

        ObservableList<Equipamiento> equipamientos = FXCollections.observableArrayList();

        String sql = """
                SELECT e.*, COALESCE(SUM(CASE WHEN s.estado = 'planificada'
                    THEN se.cantidad ELSE 0 END), 0) AS cantidad_reservada
                FROM equipamiento e
                LEFT JOIN salida_equipamiento se ON se.equipamiento_id = e.id
                LEFT JOIN salidas s ON s.id = se.salida_id
                WHERE activo = 1
                GROUP BY e.id
                ORDER BY e.nombre COLLATE NOCASE
                """;

        try (
                Connection conexion = ConexionDB.conectar();
                Statement statement = conexion.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                equipamientos.add(mapRow(resultSet));
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error al obtener equipamiento: "
                            + e.getMessage());
        }

        return equipamientos;
    }

    /** Returns active equipment availability excluding the reservation of the edited Salida. */
    public static ObservableList<Equipamiento> obtenerTodosParaEdicion(int salidaId) {
        ObservableList<Equipamiento> equipamientos = FXCollections.observableArrayList();
        String sql = """
                SELECT e.*, COALESCE(SUM(CASE WHEN s.estado = 'planificada'
                    THEN se.cantidad ELSE 0 END), 0) AS cantidad_reservada
                FROM equipamiento e
                LEFT JOIN salida_equipamiento se ON se.equipamiento_id = e.id
                LEFT JOIN salidas s ON s.id = se.salida_id AND s.id <> ?
                WHERE e.activo = 1
                GROUP BY e.id
                ORDER BY e.nombre COLLATE NOCASE
                """;
        try (Connection conexion = ConexionDB.conectar();
             PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, salidaId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) equipamientos.add(mapRow(resultSet));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar la disponibilidad para editar la Salida.", e);
        }
        return equipamientos;
    }

    public static ObservableList<Equipamiento> obtenerDadosDeBaja() {

        ObservableList<Equipamiento> equipamientos = FXCollections.observableArrayList();

        String sql = """
                SELECT e.*, COALESCE(SUM(CASE WHEN s.estado = 'planificada'
                    THEN se.cantidad ELSE 0 END), 0) AS cantidad_reservada
                FROM equipamiento e
                LEFT JOIN salida_equipamiento se ON se.equipamiento_id = e.id
                LEFT JOIN salidas s ON s.id = se.salida_id
                WHERE e.activo = 0
                GROUP BY e.id
                ORDER BY e.nombre COLLATE NOCASE
                """;

        try (
                Connection conexion = ConexionDB.conectar();
                Statement statement = conexion.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                equipamientos.add(mapRow(resultSet));
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error al obtener equipamiento dado de baja: "
                            + e.getMessage());
        }

        return equipamientos;
    }

    private static Equipamiento mapRow(ResultSet resultSet)
            throws SQLException {

        return new Equipamiento(
                resultSet.getInt("id"),
                resultSet.getString("nombre"),
                resultSet.getString("categoria"),
                resultSet.getInt("cantidad"),
                resultSet.getString("estado"),
                resultSet.getInt("cantidad_reservada"));
    }

    public static Equipamiento obtenerPorId(int id) {
        try (Connection conexion = ConexionDB.conectar()) {
            return obtenerPorId(conexion, id);
        } catch (SQLException e) {
            System.out.println(
                    "Error al obtener equipamiento por ID: "
                            + e.getMessage());
            return null;
        }
    }

    public static Equipamiento obtenerPorId(Connection conexion, int id)
            throws SQLException {
        String sql = """
                SELECT e.*, COALESCE((SELECT SUM(se.cantidad)
                    FROM salida_equipamiento se JOIN salidas s ON s.id = se.salida_id
                    WHERE se.equipamiento_id = e.id AND s.estado = 'planificada'), 0)
                    AS cantidad_reservada
                FROM equipamiento e
                WHERE e.id = ?
                """;

        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }
            }
        }

        return null;
    }
}
