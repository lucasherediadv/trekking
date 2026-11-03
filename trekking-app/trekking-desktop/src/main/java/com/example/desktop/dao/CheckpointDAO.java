package com.example.desktop.dao;

import com.example.desktop.database.ConexionDB;
import com.example.desktop.model.Checkpoint;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CheckpointDAO {

    public long insertar(Checkpoint checkpoint) throws SQLException {

        String sql = """
                INSERT INTO checkpoints
                (nombre, hora, latitud, longitud, altitud, descripcion)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        long generatedId = -1;

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(
                        sql,
                        Statement.RETURN_GENERATED_KEYS)
        ) {

            bindCheckpointValues(statement, checkpoint);

            statement.executeUpdate();

            try (ResultSet resultSet = statement.getGeneratedKeys()) {

                if (resultSet.next()) {
                    generatedId = resultSet.getLong(1);
                }
            }
        }

        return generatedId;
    }

    public List<Checkpoint> obtenerTodos() throws SQLException {

        String sql = """
                SELECT *
                FROM checkpoints
                ORDER BY hora, nombre COLLATE NOCASE
                """;

        List<Checkpoint> checkpoints = new ArrayList<>();

        try (
                Connection conexion = ConexionDB.conectar();
                Statement statement = conexion.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)
        ) {

            while (resultSet.next()) {
                checkpoints.add(mapRow(resultSet));
            }
        }

        return checkpoints;
    }

    public List<Checkpoint> obtenerDisponiblesParaRuta() throws SQLException {
        String sql = """
                SELECT c.* FROM checkpoints c
                WHERE NOT EXISTS (
                    SELECT 1 FROM ruta_checkpoints rc WHERE rc.checkpoint_id = c.id
                )
                ORDER BY c.hora, c.nombre COLLATE NOCASE
                """;
        List<Checkpoint> checkpoints = new ArrayList<>();
        try (Connection conexion = ConexionDB.conectar();
             Statement statement = conexion.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) checkpoints.add(mapRow(resultSet));
        }
        return checkpoints;
    }

    public int actualizar(Checkpoint checkpoint) throws SQLException {

        String sql = """
                UPDATE checkpoints
                SET nombre = ?,
                    hora = ?,
                    latitud = ?,
                    longitud = ?,
                    altitud = ?,
                    descripcion = ?
                WHERE id = ?
                """;

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)
        ) {

            bindCheckpointValues(statement, checkpoint);
            statement.setLong(7, checkpoint.getId());

            return statement.executeUpdate();
        }
    }

    public boolean eliminar(long id) throws SQLException {

        String sql = """
                DELETE FROM checkpoints
                WHERE id = ?
                """;

        try (
                Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            return statement.executeUpdate() > 0;
        }
    }

    private static void bindCheckpointValues(
            PreparedStatement statement,
            Checkpoint checkpoint) throws SQLException {

        statement.setString(1, checkpoint.getNombre());
        statement.setString(2, checkpoint.getHora());
        statement.setDouble(3, checkpoint.getLatitud());
        statement.setDouble(4, checkpoint.getLongitud());
        statement.setDouble(5, checkpoint.getAltitud());
        statement.setString(6, checkpoint.getDescripcion());
    }

    private static Checkpoint mapRow(ResultSet resultSet)
            throws SQLException {

        return new Checkpoint(
                resultSet.getLong("id"),
                resultSet.getString("nombre"),
                resultSet.getString("hora"),
                resultSet.getDouble("latitud"),
                resultSet.getDouble("longitud"),
                resultSet.getDouble("altitud"),
                resultSet.getString("descripcion")
        );
    }
}
