package com.example.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.example.core.excepcion.EntidadNoEncontradaException;
import com.example.core.itinerario.Checkpoint;
import com.example.core.itinerario.RepositorioItinerarios;

/** Implementación SQLite de los Itinerarios: el orden se persiste en {@code ruta_checkpoints.orden}. */
public class RepositorioItinerariosSqlite implements RepositorioItinerarios {

    private final BaseDatosSqlite base;

    public RepositorioItinerariosSqlite(BaseDatosSqlite base) {
        this.base = Objects.requireNonNull(base, "base");
    }

    @Override
    public List<Checkpoint> listar(int rutaId) {
        String sql = """
                SELECT c.id, c.nombre, c.hora, c.latitud, c.longitud, c.altitud, c.descripcion
                FROM checkpoints c
                INNER JOIN ruta_checkpoints rc ON rc.checkpoint_id = c.id
                WHERE rc.ruta_id = ?
                ORDER BY rc.orden
                """;
        try (Connection c = base.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, rutaId);
            try (ResultSet r = ps.executeQuery()) {
                List<Checkpoint> resultado = new ArrayList<>();
                while (r.next()) {
                    resultado.add(new Checkpoint(r.getLong("id"), r.getString("nombre"), r.getString("hora"),
                            r.getDouble("latitud"), r.getDouble("longitud"), r.getDouble("altitud"),
                            r.getString("descripcion")));
                }
                return resultado;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar el Itinerario.", e);
        }
    }

    @Override
    public void reemplazar(int rutaId, List<Checkpoint> ordenados) {
        try (Connection c = base.conectar()) {
            c.setAutoCommit(false);
            try {
                Set<Long> anteriores = idsActuales(c, rutaId);
                for (Checkpoint cp : ordenados) {
                    if (!cp.esNuevo() && !anteriores.contains(cp.id())) {
                        throw new EntidadNoEncontradaException("Checkpoint", (int) cp.id());
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM ruta_checkpoints WHERE ruta_id = ?")) {
                    ps.setInt(1, rutaId);
                    ps.executeUpdate();
                }
                int orden = 1;
                for (Checkpoint cp : ordenados) {
                    long id = cp.esNuevo() ? insertar(c, cp) : actualizar(c, cp);
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO ruta_checkpoints (ruta_id, checkpoint_id, orden) VALUES (?, ?, ?)")) {
                        ps.setInt(1, rutaId);
                        ps.setLong(2, id);
                        ps.setInt(3, orden++);
                        ps.executeUpdate();
                    }
                    anteriores.remove(id);
                }
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM checkpoints WHERE id = ?")) {
                    for (Long eliminado : anteriores) {
                        ps.setLong(1, eliminado);
                        ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar el Itinerario.", e);
        }
    }

    private static Set<Long> idsActuales(Connection c, int rutaId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT checkpoint_id FROM ruta_checkpoints WHERE ruta_id = ?")) {
            ps.setInt(1, rutaId);
            try (ResultSet r = ps.executeQuery()) {
                Set<Long> ids = new HashSet<>();
                while (r.next()) ids.add(r.getLong(1));
                return ids;
            }
        }
    }

    private static long insertar(Connection c, Checkpoint cp) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO checkpoints (nombre, hora, latitud, longitud, altitud, descripcion) VALUES (?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            asignar(ps, cp);
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (!claves.next()) throw new SQLException("No se obtuvo el id del Checkpoint.");
                return claves.getLong(1);
            }
        }
    }

    private static long actualizar(Connection c, Checkpoint cp) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE checkpoints SET nombre = ?, hora = ?, latitud = ?, longitud = ?, altitud = ?, descripcion = ? WHERE id = ?")) {
            asignar(ps, cp);
            ps.setLong(7, cp.id());
            ps.executeUpdate();
            return cp.id();
        }
    }

    private static void asignar(PreparedStatement ps, Checkpoint cp) throws SQLException {
        ps.setString(1, cp.nombre());
        ps.setString(2, cp.hora());
        ps.setDouble(3, cp.latitud());
        ps.setDouble(4, cp.longitud());
        ps.setDouble(5, cp.altitud());
        ps.setString(6, cp.descripcion());
    }
}
