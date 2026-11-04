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
import com.example.core.inventario.ElementoInventario;
import com.example.core.inventario.EstadoElemento;
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

    @Override
    public Optional<CopiaOperativa> buscarCopia(int id) {
        try (Connection c = base.conectar()) {
            Ruta ruta;
            LocalDate fecha;
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT s.*, COALESCE(r.activo, 0) AS ruta_activa FROM salidas s
                    LEFT JOIN rutas r ON r.id = s.ruta_id WHERE s.id = ?
                    """)) {
                ps.setInt(1, id);
                try (ResultSet r = ps.executeQuery()) {
                    if (!r.next()) return Optional.empty();
                    fecha = LocalDate.parse(r.getString("fecha"));
                    ruta = new Ruta(r.getInt("ruta_id"), r.getString("ruta_nombre"), r.getDouble("ruta_altitud_maxima"),
                            r.getString("ruta_tipo_terreno"), r.getString("ruta_dificultad_tecnica"),
                            r.getString("ruta_dificultad_fisica"), r.getInt("ruta_activa") == 1, 0);
                }
            }
            List<Checkpoint> checkpoints = leerCheckpoints(c, id);
            ruta = new Ruta(ruta.id(), ruta.nombre(), ruta.altitudMaxima(), ruta.tipoTerreno(),
                    ruta.dificultadTecnica(), ruta.dificultadFisica(), ruta.activa(), checkpoints.size());
            return Optional.of(new CopiaOperativa(fecha, ruta, checkpoints, leerEquipamiento(c, id), leerParticipantes(c, id)));
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar la Copia operativa de la Salida.", e);
        }
    }

    @Override
    public void reemplazarCopia(int id, CopiaOperativa copia) {
        Ruta ruta = copia.ruta();
        try (Connection c = base.conectar()) {
            try (PreparedStatement ps = c.prepareStatement("""
                    UPDATE salidas SET ruta_id = ?, fecha = ?, ruta_nombre = ?, ruta_altitud_maxima = ?,
                        ruta_tipo_terreno = ?, ruta_dificultad_tecnica = ?, ruta_dificultad_fisica = ?
                    WHERE id = ?
                    """)) {
                ps.setInt(1, ruta.id());
                ps.setString(2, copia.fecha().toString());
                ps.setString(3, ruta.nombre());
                ps.setDouble(4, ruta.altitudMaxima());
                ps.setString(5, ruta.tipoTerreno());
                ps.setString(6, ruta.dificultadTecnica());
                ps.setString(7, ruta.dificultadFisica());
                ps.setInt(8, id);
                ps.executeUpdate();
            }
            for (String tabla : List.of("salida_checkpoints", "salida_equipamiento", "salida_participantes")) {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM " + tabla + " WHERE salida_id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
            }
            guardarCheckpoints(c, id, copia.checkpoints());
            guardarEquipamiento(c, id, copia.equipamiento());
            guardarParticipantes(c, id, copia.participantes());
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo reemplazar la Copia operativa de la Salida.", e);
        }
    }

    private static List<Checkpoint> leerCheckpoints(Connection c, int salidaId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT nombre, hora, latitud, longitud, altitud, descripcion
                FROM salida_checkpoints WHERE salida_id = ? ORDER BY orden
                """)) {
            ps.setInt(1, salidaId);
            try (ResultSet r = ps.executeQuery()) {
                List<Checkpoint> checkpoints = new ArrayList<>();
                while (r.next()) {
                    checkpoints.add(new Checkpoint(0, r.getString("nombre"), r.getString("hora"), r.getDouble("latitud"),
                            r.getDouble("longitud"), r.getDouble("altitud"), r.getString("descripcion")));
                }
                return checkpoints;
            }
        }
    }

    private static List<CopiaOperativa.LineaEquipamiento> leerEquipamiento(Connection c, int salidaId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT e.id, e.estado, e.cantidad AS cantidad_total, se.nombre_copia, se.categoria_copia,
                       se.cantidad AS cantidad_asignada,
                       COALESCE((SELECT SUM(x.cantidad) FROM salida_equipamiento x
                                 JOIN salidas s ON s.id = x.salida_id
                                 WHERE x.equipamiento_id = e.id AND s.estado = 'planificada'), 0) AS cantidad_reservada
                FROM salida_equipamiento se JOIN equipamiento e ON e.id = se.equipamiento_id
                WHERE se.salida_id = ? ORDER BY se.nombre_copia COLLATE NOCASE
                """)) {
            ps.setInt(1, salidaId);
            try (ResultSet r = ps.executeQuery()) {
                List<CopiaOperativa.LineaEquipamiento> lineas = new ArrayList<>();
                while (r.next()) {
                    EstadoElemento estado = EstadoElemento.desdeEtiqueta(r.getString("estado"))
                            .orElseThrow(() -> new IllegalStateException("Estado de elemento desconocido en la base."));
                    lineas.add(new CopiaOperativa.LineaEquipamiento(new ElementoInventario(r.getInt("id"),
                            r.getString("nombre_copia"), r.getString("categoria_copia"), r.getInt("cantidad_total"),
                            estado, r.getInt("cantidad_reservada")), r.getInt("cantidad_asignada")));
                }
                return lineas;
            }
        }
    }

    private static List<Cliente> leerParticipantes(Connection c, int salidaId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT c.id, c.fecha_nacimiento, c.email, c.sexo, c.autorizacion_menores, c.tutor_nombre,
                       c.tutor_apellido, c.tutor_dni, c.tutor_telefono, c.activo, c.fecha_alta, c.fecha_modificacion,
                       sp.dni_copia, sp.nombre_copia, sp.apellido_copia, sp.telefono_copia,
                       sp.emergencia_nombre_copia, sp.emergencia_telefono_copia, sp.emergencia_relacion_copia
                FROM salida_participantes sp JOIN clientes c ON c.id = sp.cliente_id
                WHERE sp.salida_id = ? ORDER BY sp.apellido_copia COLLATE NOCASE, sp.nombre_copia COLLATE NOCASE
                """)) {
            ps.setInt(1, salidaId);
            try (ResultSet r = ps.executeQuery()) {
                List<Cliente> clientes = new ArrayList<>();
                while (r.next()) {
                    clientes.add(new Cliente(r.getInt("id"), r.getString("dni_copia"), r.getString("nombre_copia"),
                            r.getString("apellido_copia"), fecha(r.getString("fecha_nacimiento")), r.getString("email"),
                            r.getString("telefono_copia"), r.getString("sexo"), r.getString("emergencia_nombre_copia"),
                            r.getString("emergencia_telefono_copia"), r.getString("emergencia_relacion_copia"),
                            r.getBoolean("autorizacion_menores"), r.getString("tutor_nombre"), r.getString("tutor_apellido"),
                            r.getString("tutor_dni"), r.getString("tutor_telefono"), r.getBoolean("activo"),
                            fecha(r.getString("fecha_alta")), fecha(r.getString("fecha_modificacion"))));
                }
                return clientes;
            }
        }
    }

    private static LocalDate fecha(String texto) {
        return texto == null || texto.isEmpty() ? null : LocalDate.parse(texto);
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
