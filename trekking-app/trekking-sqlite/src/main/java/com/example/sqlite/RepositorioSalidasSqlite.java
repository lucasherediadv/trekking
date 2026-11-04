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

import com.example.core.cliente.Cliente;
import com.example.core.itinerario.Checkpoint;
import com.example.core.ruta.Ruta;
import com.example.core.salida.CopiaOperativa;
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

    @Override
    public int guardar(CopiaOperativa copia) {
        Ruta ruta = copia.ruta();
        try (Connection c = base.conectar()) {
            int id;
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO salidas (ruta_id, fecha, estado, ruta_nombre, ruta_altitud_maxima,
                        ruta_tipo_terreno, ruta_dificultad_tecnica, ruta_dificultad_fisica)
                    VALUES (?, ?, 'planificada', ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, ruta.id());
                ps.setString(2, copia.fecha().toString());
                ps.setString(3, ruta.nombre());
                ps.setDouble(4, ruta.altitudMaxima());
                ps.setString(5, ruta.tipoTerreno());
                ps.setString(6, ruta.dificultadTecnica());
                ps.setString(7, ruta.dificultadFisica());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new IllegalStateException("No se obtuvo el id de la Salida.");
                    id = keys.getInt(1);
                }
            }
            guardarCheckpoints(c, id, copia.checkpoints());
            guardarEquipamiento(c, id, copia.equipamiento());
            guardarParticipantes(c, id, copia.participantes());
            return id;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar la Salida.", e);
        }
    }

    private static void guardarCheckpoints(Connection c, int salidaId, List<Checkpoint> checkpoints) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO salida_checkpoints (salida_id, nombre, hora, latitud, longitud, altitud, descripcion, orden)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            int orden = 0;
            for (Checkpoint cp : checkpoints) {
                ps.setInt(1, salidaId);
                ps.setString(2, cp.nombre());
                ps.setString(3, cp.hora());
                ps.setDouble(4, cp.latitud());
                ps.setDouble(5, cp.longitud());
                ps.setDouble(6, cp.altitud());
                ps.setString(7, cp.descripcion());
                ps.setInt(8, orden++);
                ps.executeUpdate();
            }
        }
    }

    private static void guardarEquipamiento(Connection c, int salidaId, List<CopiaOperativa.LineaEquipamiento> lineas)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO salida_equipamiento (salida_id, equipamiento_id, nombre_copia, categoria_copia, cantidad)
                VALUES (?, ?, ?, ?, ?)
                """)) {
            for (CopiaOperativa.LineaEquipamiento linea : lineas) {
                ps.setInt(1, salidaId);
                ps.setInt(2, linea.elemento().id());
                ps.setString(3, linea.elemento().nombre());
                ps.setString(4, linea.elemento().categoria());
                ps.setInt(5, linea.cantidad());
                ps.executeUpdate();
            }
        }
    }

    private static void guardarParticipantes(Connection c, int salidaId, List<Cliente> participantes) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO salida_participantes
                    (salida_id, cliente_id, dni_copia, nombre_copia, apellido_copia, telefono_copia,
                     emergencia_nombre_copia, emergencia_telefono_copia, emergencia_relacion_copia)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (Cliente cliente : participantes) {
                ps.setInt(1, salidaId);
                ps.setInt(2, cliente.id());
                ps.setString(3, cliente.dni());
                ps.setString(4, cliente.nombre());
                ps.setString(5, cliente.apellido());
                ps.setString(6, cliente.telefono());
                ps.setString(7, cliente.contactoEmergenciaNombre());
                ps.setString(8, cliente.contactoEmergenciaTelefono());
                ps.setString(9, cliente.contactoEmergenciaRelacion());
                ps.executeUpdate();
            }
        }
    }

    private static Salida mapear(ResultSet r) throws SQLException {
        return new Salida(r.getInt("id"), r.getInt("ruta_id"), LocalDate.parse(r.getString("fecha")),
                estado(r.getString("estado")), r.getString("ruta_nombre"), r.getDouble("ruta_altitud_maxima"),
                r.getString("ruta_tipo_terreno"), r.getString("ruta_dificultad_tecnica"),
                r.getString("ruta_dificultad_fisica"));
    }
}
