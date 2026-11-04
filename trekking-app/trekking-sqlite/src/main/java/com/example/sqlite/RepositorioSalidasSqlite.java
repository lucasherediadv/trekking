package com.example.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.example.core.salida.EstadoSalida;
import com.example.core.salida.RepositorioSalidas;
import com.example.core.salida.ResumenOperativo;
import com.example.core.salida.Salida;

/** Implementación SQLite de las Salidas. Es dueña de la conversión entre el enum y los textos guardados. */
public class RepositorioSalidasSqlite implements RepositorioSalidas {

    private final BaseDatosSqlite base;

    public RepositorioSalidasSqlite(BaseDatosSqlite base) {
        this.base = Objects.requireNonNull(base, "base");
    }

    /** Texto con el que se guarda cada estado; no cambia respecto de las bases existentes. */
    static String texto(EstadoSalida estado) {
        return switch (estado) {
            case PLANIFICADA -> "planificada";
            case REALIZADA -> "realizada";
            case CANCELADA -> "cancelada";
        };
    }

    private static EstadoSalida estado(String texto) {
        for (EstadoSalida estado : EstadoSalida.values()) {
            if (texto(estado).equals(texto)) return estado;
        }
        throw new IllegalStateException("Estado de Salida desconocido en la base: " + texto);
    }

    @Override
    public List<Salida> listar() {
        try (Connection c = base.conectar(); Statement s = c.createStatement();
                ResultSet r = s.executeQuery("SELECT * FROM salidas ORDER BY fecha DESC, id DESC")) {
            List<Salida> salidas = new ArrayList<>();
            while (r.next()) salidas.add(mapear(r));
            return salidas;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron cargar las salidas.", e);
        }
    }

    @Override
    public Map<Integer, ResumenOperativo> resumenes() {
        String sql = """
                SELECT s.id,
                    COALESCE((SELECT GROUP_CONCAT(participante.nombre, ', ')
                        FROM (SELECT sp.nombre_copia || ' ' || sp.apellido_copia AS nombre
                            FROM salida_participantes sp
                            WHERE sp.salida_id = s.id
                            ORDER BY sp.apellido_copia COLLATE NOCASE, sp.nombre_copia COLLATE NOCASE) participante), '') AS participantes,
                    COALESCE((SELECT GROUP_CONCAT(se.nombre_copia || ' × ' || se.cantidad, ', ')
                        FROM salida_equipamiento se WHERE se.salida_id = s.id), '') AS equipamiento
                FROM salidas s
                """;
        try (Connection c = base.conectar(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
            Map<Integer, ResumenOperativo> resumenes = new HashMap<>();
            while (r.next()) {
                resumenes.put(r.getInt("id"), new ResumenOperativo(r.getString("participantes"), r.getString("equipamiento")));
            }
            return resumenes;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron cargar los resúmenes de las Salidas.", e);
        }
    }

    @Override
    public Optional<Salida> buscarPorId(int id) {
        try (Connection c = base.conectar(); PreparedStatement ps = c.prepareStatement("SELECT * FROM salidas WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet r = ps.executeQuery()) {
                return r.next() ? Optional.of(mapear(r)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar la Salida.", e);
        }
    }

    @Override
    public boolean cerrarSiPlanificada(int id, EstadoSalida nuevoEstado) {
        // La Reserva de stock se deduce de las Salidas planificadas: al cambiar el estado queda liberada.
        String sql = "UPDATE salidas SET estado = ? WHERE id = ? AND estado = 'planificada'";
        try (Connection c = base.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, texto(nuevoEstado));
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo actualizar la Salida.", e);
        }
    }

    private static Salida mapear(ResultSet r) throws SQLException {
        return new Salida(r.getInt("id"), r.getInt("ruta_id"), LocalDate.parse(r.getString("fecha")),
                estado(r.getString("estado")), r.getString("ruta_nombre"), r.getDouble("ruta_altitud_maxima"),
                r.getString("ruta_tipo_terreno"), r.getString("ruta_dificultad_tecnica"),
                r.getString("ruta_dificultad_fisica"));
    }
}
