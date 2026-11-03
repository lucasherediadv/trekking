package com.example.desktop.dao;

import com.example.desktop.database.ConexionDB;
import com.example.desktop.model.Checkpoint;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class RutaCheckpointDAO {

    public List<Checkpoint> obtenerPorRuta(int rutaId) {
        try (Connection conexion = ConexionDB.conectar()) {
            return obtenerPorRuta(conexion, rutaId);
        } catch (SQLException e) {
            System.out.println(
                    "Error al obtener los checkpoints de la ruta: "
                            + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Checkpoint> obtenerPorRuta(
            Connection conexion,
            int rutaId) throws SQLException {
        String sql = """
                SELECT
                    c.id,
                    c.nombre,
                    c.hora,
                    c.latitud,
                    c.longitud,
                    c.altitud,
                    c.descripcion
                FROM checkpoints c
                INNER JOIN ruta_checkpoints rc
                    ON rc.checkpoint_id = c.id
                WHERE rc.ruta_id = ?
                ORDER BY rc.orden
                """;

        List<Checkpoint> checkpoints = new ArrayList<>();

        try (PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, rutaId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    checkpoints.add(new Checkpoint(
                            resultSet.getLong("id"),
                            resultSet.getString("nombre"),
                            resultSet.getString("hora"),
                            resultSet.getDouble("latitud"),
                            resultSet.getDouble("longitud"),
                            resultSet.getDouble("altitud"),
                            resultSet.getString("descripcion")
                    ));
                }
            }
        }

        return checkpoints;
    }

    public boolean reemplazarPorRuta(
            int rutaId,
            List<Checkpoint> checkpoints) {

        if (checkpoints.size() == 1) {
            return false;
        }

        String sqlEliminar = """
                DELETE FROM ruta_checkpoints
                WHERE ruta_id = ?
                """;

        String sqlInsertar = """
                INSERT INTO ruta_checkpoints
                (ruta_id, checkpoint_id, orden)
                VALUES (?, ?, ?)
                """;
        String sqlCrearCheckpoint = """
                INSERT INTO checkpoints (nombre, hora, latitud, longitud, altitud, descripcion)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        String sqlActualizarCheckpoint = """
                UPDATE checkpoints SET nombre = ?, hora = ?, latitud = ?, longitud = ?,
                    altitud = ?, descripcion = ? WHERE id = ?
                """;
        List<Checkpoint> puntosNuevos = checkpoints.stream()
                .filter(checkpoint -> checkpoint.getId() == 0).toList();

        try (Connection conexion = ConexionDB.conectar()) {

            conexion.setAutoCommit(false);

            Set<Long> checkpointsAnteriores = new HashSet<>();
            try (PreparedStatement consulta = conexion.prepareStatement(
                    "SELECT checkpoint_id FROM ruta_checkpoints WHERE ruta_id = ?")) {
                consulta.setInt(1, rutaId);
                try (ResultSet rs = consulta.executeQuery()) {
                    while (rs.next()) checkpointsAnteriores.add(rs.getLong(1));
                }
            }

            try (PreparedStatement consulta = conexion.prepareStatement(
                    "SELECT altitud_maxima FROM rutas WHERE id = ?")) {
                consulta.setInt(1, rutaId);
                try (ResultSet rs = consulta.executeQuery()) {
                    if (!rs.next()) {
                        conexion.rollback();
                        return false;
                    }
                    double maximoRuta = rs.getDouble(1);
                    if (checkpoints.stream().anyMatch(cp -> cp.getAltitud() > maximoRuta)) {
                        conexion.rollback();
                        return false;
                    }
                }
            }

            try (
                    PreparedStatement eliminar =
                            conexion.prepareStatement(sqlEliminar);
                    PreparedStatement insertar =
                            conexion.prepareStatement(sqlInsertar)
            ) {
                eliminar.setInt(1, rutaId);
                eliminar.executeUpdate();

                try (PreparedStatement crear = conexion.prepareStatement(
                        sqlCrearCheckpoint, java.sql.Statement.RETURN_GENERATED_KEYS);
                     PreparedStatement actualizar = conexion.prepareStatement(sqlActualizarCheckpoint)) {

                    for (Checkpoint checkpoint : checkpoints) {
                        PreparedStatement target = checkpoint.getId() == 0 ? crear : actualizar;
                        target.setString(1, checkpoint.getNombre());
                        target.setString(2, checkpoint.getHora());
                        target.setDouble(3, checkpoint.getLatitud());
                        target.setDouble(4, checkpoint.getLongitud());
                        target.setDouble(5, checkpoint.getAltitud());
                        target.setString(6, checkpoint.getDescripcion());
                        if (checkpoint.getId() != 0) target.setLong(7, checkpoint.getId());
                        target.executeUpdate();
                        if (checkpoint.getId() == 0) {
                            try (ResultSet keys = crear.getGeneratedKeys()) {
                                if (!keys.next()) throw new SQLException("No se obtuvo el ID del Checkpoint.");
                                checkpoint.setId(keys.getLong(1));
                            }
                        }
                    }
                }

                int orden = 1;

                for (Checkpoint checkpoint : checkpoints.stream()
                        .sorted(Comparator.comparing(Checkpoint::getHora)).toList()) {
                    insertar.setInt(1, rutaId);
                    insertar.setLong(2, checkpoint.getId());
                    insertar.setInt(3, orden);
                    insertar.addBatch();

                    orden++;
                }

                insertar.executeBatch();

                Set<Long> conservados = checkpoints.stream()
                        .filter(checkpoint -> checkpoint.getId() > 0)
                        .map(Checkpoint::getId).collect(java.util.stream.Collectors.toSet());
                try (PreparedStatement borrar = conexion.prepareStatement(
                        "DELETE FROM checkpoints WHERE id = ?")) {
                    for (Long idAnterior : checkpointsAnteriores) {
                        if (!conservados.contains(idAnterior)) {
                            borrar.setLong(1, idAnterior);
                            borrar.addBatch();
                        }
                    }
                    borrar.executeBatch();
                }

                conexion.commit();
                return true;

            } catch (SQLException e) {
                conexion.rollback();
                puntosNuevos.forEach(checkpoint -> checkpoint.setId(0));
                System.out.println(
                        "Error al guardar los checkpoints de la ruta: "
                                + e.getMessage());
                return false;

            } finally {
                conexion.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.out.println(
                    "Error al conectar con la base de datos: "
                            + e.getMessage());
            return false;
        }
    }
}
