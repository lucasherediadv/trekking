package com.example.desktop.dao;

import com.example.desktop.database.ConexionDB;
import com.example.desktop.model.Ruta;
import com.example.core.itinerario.Checkpoint;
import com.example.desktop.model.Equipamiento;
import com.example.desktop.model.AsignacionEquipamiento;
import com.example.desktop.model.AsignacionParticipante;
import com.example.core.cliente.Cliente;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.HashMap;

public class SalidaDAO {
    /** Datos de la Salida que la edición necesita leer dentro de su transacción. */
    private record SalidaActual(int rutaId, boolean planificada, String rutaNombre, double rutaAltitudMaxima,
            String rutaTipoTerreno, String rutaDificultadTecnica, String rutaDificultadFisica) {}

    private final RutaDAO rutaDAO = new RutaDAO();

    public List<AsignacionEquipamiento> obtenerEquipamiento(int salidaId) throws SQLException {
        List<AsignacionEquipamiento> result = new ArrayList<>();
        try (Connection c=ConexionDB.conectar(); PreparedStatement ps=c.prepareStatement("SELECT e.id, e.estado, e.cantidad AS cantidad_total, se.nombre_copia, se.categoria_copia, se.cantidad AS cantidad_asignada FROM salida_equipamiento se JOIN equipamiento e ON e.id=se.equipamiento_id WHERE se.salida_id=? ORDER BY se.nombre_copia COLLATE NOCASE")) {
            ps.setInt(1,salidaId); try(ResultSet rs=ps.executeQuery()){ while(rs.next()) result.add(new AsignacionEquipamiento(new Equipamiento(rs.getInt("id"),rs.getString("nombre_copia"),rs.getString("categoria_copia"),rs.getInt("cantidad_total"),rs.getString("estado")),rs.getInt("cantidad_asignada"),false)); }
        }
        return result;
    }

    public List<AsignacionParticipante> obtenerParticipantes(int salidaId) throws SQLException {
        List<AsignacionParticipante> result=new ArrayList<>();
        try(Connection c=ConexionDB.conectar(); PreparedStatement ps=c.prepareStatement("SELECT c.*,sp.dni_copia,sp.nombre_copia,sp.apellido_copia,sp.telefono_copia,sp.emergencia_nombre_copia,sp.emergencia_telefono_copia,sp.emergencia_relacion_copia FROM salida_participantes sp JOIN clientes c ON c.id=sp.cliente_id WHERE sp.salida_id=? ORDER BY sp.apellido_copia COLLATE NOCASE, sp.nombre_copia COLLATE NOCASE")) {
            ps.setInt(1,salidaId); try(ResultSet rs=ps.executeQuery()){ while(rs.next()) result.add(new AsignacionParticipante(new Cliente(rs.getInt("id"),rs.getString("dni_copia"),rs.getString("nombre_copia"),rs.getString("apellido_copia"),LocalDate.parse(rs.getString("fecha_nacimiento")),rs.getString("email"),rs.getString("telefono_copia"),rs.getString("sexo"),rs.getString("emergencia_nombre_copia"),rs.getString("emergencia_telefono_copia"),rs.getString("emergencia_relacion_copia"),rs.getBoolean("autorizacion_menores"),rs.getString("tutor_nombre"),rs.getString("tutor_apellido"),rs.getString("tutor_dni"),rs.getString("tutor_telefono"),rs.getInt("activo")==1,null,null),false)); }
        }
        return result;
    }

    public List<Checkpoint> obtenerCheckpoints(int salidaId) throws SQLException {
        List<Checkpoint> puntos = new ArrayList<>();
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement(
                "SELECT nombre, hora, latitud, longitud, altitud, descripcion FROM salida_checkpoints WHERE salida_id = ? ORDER BY orden")) {
            ps.setInt(1, salidaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) puntos.add(new Checkpoint(0L, rs.getString("nombre"), rs.getString("hora"),
                        rs.getDouble("latitud"), rs.getDouble("longitud"), rs.getDouble("altitud"), rs.getString("descripcion")));
            }
        }
        return puntos;
    }

    public void actualizar(int salidaId, LocalDate fecha, int rutaId, List<Checkpoint> puntos,
            List<Checkpoint> guardarEnRuta, boolean actualizarDesdeRuta) throws SQLException {
        actualizar(salidaId, fecha, rutaId, puntos, guardarEnRuta, actualizarDesdeRuta, null, null);
    }

    /** Updates the operational copy and its assignments atomically when assignment lists are supplied. */
    public void actualizar(int salidaId, LocalDate fecha, int rutaId, List<Checkpoint> puntos,
            List<Checkpoint> guardarEnRuta, boolean actualizarDesdeRuta,
            List<AsignacionEquipamiento> equipamiento, List<AsignacionParticipante> participantes) throws SQLException {
        actualizar(salidaId, fecha, rutaId, puntos, guardarEnRuta, actualizarDesdeRuta,
                equipamiento, participantes, java.util.Set.of(), java.util.Set.of());
    }

    public void actualizar(int salidaId, LocalDate fecha, int rutaId, List<Checkpoint> puntos,
            List<Checkpoint> guardarEnRuta, boolean actualizarDesdeRuta,
            List<AsignacionEquipamiento> equipamiento, List<AsignacionParticipante> participantes,
            java.util.Set<Integer> refrescarEquipamiento, java.util.Set<Integer> refrescarParticipantes) throws SQLException {
        if (fecha == null || puntos == null) throw new SQLException("La fecha y los Checkpoints son obligatorios.");
        try (Connection c = ConexionDB.conectar()) {
            if (equipamiento != null) {
                try (Statement timeout = c.createStatement()) { timeout.execute("PRAGMA busy_timeout = 5000"); }
            }
            c.setAutoCommit(false);
            try {
                if (equipamiento != null) bloquearInventario(c, equipamiento);
                SalidaActual actual;
                try (PreparedStatement ps = c.prepareStatement("SELECT * FROM salidas WHERE id = ?")) {
                    ps.setInt(1, salidaId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("La Salida ya no existe.");
                        actual = mapear(rs);
                    }
                }
                Ruta rutaCatalogo = rutaDAO.obtenerPorId(c, rutaId);
                if (rutaCatalogo == null) throw new SQLException("La Ruta seleccionada ya no existe.");
                boolean mismaRuta = rutaId == actual.rutaId();
                Ruta ruta = mismaRuta && !actualizarDesdeRuta
                        ? new Ruta(rutaId, actual.rutaNombre(), actual.rutaAltitudMaxima(), actual.rutaTipoTerreno(),
                                actual.rutaDificultadTecnica(), actual.rutaDificultadFisica())
                        : rutaCatalogo;
                try (PreparedStatement ps = c.prepareStatement("SELECT activo FROM rutas WHERE id = ?")) {
                    ps.setInt(1, rutaId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || (rs.getInt(1) != 1 && !mismaRuta))
                            throw new SQLException("No se puede cambiar a una Ruta inactiva.");
                    }
                }
                for (Checkpoint cp : puntos) validarCheckpoint(cp, ruta.getAltitudMaxima());
                if (!guardarEnRuta.isEmpty()) {
                    if (mismaRuta && !rutaActiva(c, rutaId)) throw new SQLException("La Ruta inactiva no admite nuevos Checkpoints.");
                    int total = checkpointsDeRuta(c, rutaId).size();
                    if (total + guardarEnRuta.size() == 1) throw new SQLException("La Ruta no puede quedar con exactamente un Checkpoint.");
                    for (Checkpoint cp : guardarEnRuta) {
                        validarCheckpoint(cp, ruta.getAltitudMaxima());
                        if (cp.altitud() > rutaCatalogo.getAltitudMaxima())
                            throw new SQLException("El Checkpoint supera la altitud máxima actual de la Ruta.");
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE salidas SET ruta_id = ?, fecha = ?, ruta_nombre = ?, ruta_altitud_maxima = ?, ruta_tipo_terreno = ?, ruta_dificultad_tecnica = ?, ruta_dificultad_fisica = ? WHERE id = ?")) {
                    ps.setInt(1, rutaId); ps.setString(2, fecha.toString()); ps.setString(3, ruta.getNombre());
                    ps.setDouble(4, ruta.getAltitudMaxima()); ps.setString(5, ruta.getTipoTerreno());
                    ps.setString(6, ruta.getDificultadTecnica()); ps.setString(7, ruta.getDificultadFisica()); ps.setInt(8, salidaId);
                    ps.executeUpdate();
                }
                try (PreparedStatement del = c.prepareStatement("DELETE FROM salida_checkpoints WHERE salida_id = ?")) {
                    del.setInt(1, salidaId); del.executeUpdate();
                }
                try (PreparedStatement ins = c.prepareStatement("INSERT INTO salida_checkpoints (salida_id,nombre,hora,latitud,longitud,altitud,descripcion,orden) VALUES (?,?,?,?,?,?,?,?)")) {
                    int orden = 0;
                    for (Checkpoint cp : puntos) {
                        ins.setInt(1, salidaId); ins.setString(2, cp.nombre().trim()); ins.setString(3, cp.hora());
                        ins.setDouble(4, cp.latitud()); ins.setDouble(5, cp.longitud()); ins.setDouble(6, cp.altitud());
                        ins.setString(7, cp.descripcion()); ins.setInt(8, orden++); ins.addBatch();
                    }
                    ins.executeBatch();
                }
                for (Checkpoint cp : guardarEnRuta) agregarCheckpointRuta(c, rutaId, cp);
                if (equipamiento != null && participantes != null) {
                    reemplazarAsignaciones(c, salidaId, equipamiento, participantes,
                            actual.planificada(), refrescarEquipamiento, refrescarParticipantes);
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                if (e instanceof SQLException sql) throw sql;
                throw new SQLException("No se pudo actualizar la Salida.", e);
            }
        }
    }

    private void reemplazarAsignaciones(Connection c, int salidaId,
            List<AsignacionEquipamiento> equipamiento, List<AsignacionParticipante> participantes,
            boolean reservaActiva, java.util.Set<Integer> refrescarEquipamiento,
            java.util.Set<Integer> refrescarParticipantes) throws SQLException {
        // Keep the old snapshots available while validating and before replacing either relation.
        Map<Integer, String[]> equipoCopias = new java.util.HashMap<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT equipamiento_id,nombre_copia,categoria_copia FROM salida_equipamiento WHERE salida_id=?")) {
            ps.setInt(1, salidaId); try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) equipoCopias.put(rs.getInt(1), new String[]{rs.getString(2), rs.getString(3)});
            }
        }
        Map<Integer, String[]> clienteCopias = new java.util.HashMap<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT cliente_id,dni_copia,nombre_copia,apellido_copia,telefono_copia,emergencia_nombre_copia,emergencia_telefono_copia,emergencia_relacion_copia FROM salida_participantes WHERE salida_id=?")) {
            ps.setInt(1, salidaId); try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) clienteCopias.put(rs.getInt(1), new String[]{rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8)});
            }
        }
        // Exclude this Salida's former reservation while checking its proposed quantities.
        java.util.Set<Integer> vistos = new java.util.HashSet<>();
        for (AsignacionEquipamiento a : equipamiento) {
            if (a == null || a.equipamiento() == null || a.cantidad() <= 0) throw new SQLException("La cantidad asignada debe ser un entero positivo.");
            if (a.nuevo()) continue;
            int id = a.equipamiento().getId();
            if (!vistos.add(id)) throw new SQLException("No se puede asignar dos veces el mismo Elemento de inventario.");
            Equipamiento actual = EquipamientoDAO.obtenerPorId(c, id);
            if (actual == null) throw new SQLException("El Elemento de inventario ya no existe.");
            if (!esActivo(c, id) && (!equipoCopias.containsKey(id) || refrescarEquipamiento.contains(id)))
                throw new SQLException("El Elemento de inventario está dado de baja y no puede agregarse nuevamente.");
            int reservada = actual.getCantidadReservada();
            if (reservaActiva && equipoCopias.containsKey(id)) reservada -= cantidadAsignadaActual(c, salidaId, id);
            int disponible = Math.max(0, actual.getCantidad() - reservada);
            if (a.cantidad() > disponible) throw new SQLException("No hay cantidad suficiente de " + actual.getNombre() + ". Disponible: " + disponible + ".");
        }
        java.util.Set<Integer> clientesVistos = new java.util.HashSet<>();
        for (AsignacionParticipante a : participantes) {
            if (a == null || a.cliente() == null || a.nuevo()) throw new SQLException("En edición, selecciona Clientes existentes.");
            int id = a.cliente().id();
            if (!clientesVistos.add(id)) throw new SQLException("No se puede asociar dos veces el mismo Cliente.");
            if (!clienteCopias.containsKey(id) || refrescarParticipantes.contains(id)) {
                try (PreparedStatement ps = c.prepareStatement("SELECT activo FROM clientes WHERE id=?")) {
                    ps.setInt(1,id); try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || rs.getInt(1) != 1) throw new SQLException("Los Clientes inactivos solo pueden conservarse si ya estaban asociados.");
                    }
                }
            }
        }
        try (PreparedStatement ps=c.prepareStatement("DELETE FROM salida_equipamiento WHERE salida_id=?")) { ps.setInt(1,salidaId); ps.executeUpdate(); }
        for (AsignacionEquipamiento a : equipamiento) {
            Equipamiento item = a.nuevo() ? a.equipamiento() : EquipamientoDAO.obtenerPorId(c,a.equipamiento().getId());
            int id=item.getId(); String[] copia=equipoCopias.get(id);
            boolean refrescar = refrescarEquipamiento.contains(id);
            String nombre=copia == null || refrescar ? item.getNombre() : copia[0], categoria=copia == null || refrescar ? item.getCategoria() : copia[1];
            if (a.nuevo()) {
                try (PreparedStatement ins=c.prepareStatement("INSERT INTO equipamiento(nombre,categoria,cantidad,estado,activo) VALUES(?,?,?,?,1)",Statement.RETURN_GENERATED_KEYS)) {
                    ins.setString(1,item.getNombre()); ins.setString(2,item.getCategoria()); ins.setInt(3,item.getCantidad()); ins.setString(4,item.getEstado()); ins.executeUpdate();
                    try(ResultSet keys=ins.getGeneratedKeys()){ if(!keys.next()) throw new SQLException("No se pudo crear el Elemento de inventario."); id=keys.getInt(1); }
                }
                nombre=item.getNombre(); categoria=item.getCategoria();
            }
            try(PreparedStatement ins=c.prepareStatement("INSERT INTO salida_equipamiento(salida_id,equipamiento_id,nombre_copia,categoria_copia,cantidad) VALUES(?,?,?,?,?)")) {
                ins.setInt(1,salidaId); ins.setInt(2,id); ins.setString(3,nombre); ins.setString(4,categoria); ins.setInt(5,a.cantidad()); ins.executeUpdate();
            }
        }
        try (PreparedStatement ps=c.prepareStatement("DELETE FROM salida_participantes WHERE salida_id=?")) { ps.setInt(1,salidaId); ps.executeUpdate(); }
        for (AsignacionParticipante a : participantes) {
            Cliente cl=a.cliente(); String[] cp=clienteCopias.get(cl.id());
            if (refrescarParticipantes.contains(cl.id())) cp=null;
            try(PreparedStatement ins=c.prepareStatement("INSERT INTO salida_participantes(salida_id,cliente_id,dni_copia,nombre_copia,apellido_copia,telefono_copia,emergencia_nombre_copia,emergencia_telefono_copia,emergencia_relacion_copia) VALUES(?,?,?,?,?,?,?,?,?)")) {
                ins.setInt(1,salidaId); ins.setInt(2,cl.id()); ins.setString(3,cp==null?cl.dni():cp[0]); ins.setString(4,cp==null?cl.nombre():cp[1]); ins.setString(5,cp==null?cl.apellido():cp[2]); ins.setString(6,cp==null?cl.telefono():cp[3]); ins.setString(7,cp==null?cl.contactoEmergenciaNombre():cp[4]); ins.setString(8,cp==null?cl.contactoEmergenciaTelefono():cp[5]); ins.setString(9,cp==null?cl.contactoEmergenciaRelacion():cp[6]); ins.executeUpdate();
            }
        }
    }

    private int cantidadAsignadaActual(Connection c, int salidaId, int equipoId) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement("SELECT cantidad FROM salida_equipamiento WHERE salida_id=? AND equipamiento_id=?")) { ps.setInt(1,salidaId); ps.setInt(2,equipoId); try(ResultSet rs=ps.executeQuery()){return rs.next()?rs.getInt(1):0;} }
    }

    private boolean rutaActiva(Connection c, int rutaId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT activo FROM rutas WHERE id = ?")) {
            ps.setInt(1, rutaId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getInt(1) == 1; }
        }
    }

    private List<Checkpoint> checkpointsDeRuta(Connection c, int rutaId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT cp.id, cp.nombre, cp.hora, cp.latitud, cp.longitud, cp.altitud, cp.descripcion
                FROM checkpoints cp INNER JOIN ruta_checkpoints rc ON rc.checkpoint_id = cp.id
                WHERE rc.ruta_id = ? ORDER BY rc.orden
                """)) {
            ps.setInt(1, rutaId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Checkpoint> puntos = new ArrayList<>();
                while (rs.next()) puntos.add(new Checkpoint(rs.getLong("id"), rs.getString("nombre"), rs.getString("hora"),
                        rs.getDouble("latitud"), rs.getDouble("longitud"), rs.getDouble("altitud"), rs.getString("descripcion")));
                return puntos;
            }
        }
    }

    private void validarCheckpoint(Checkpoint cp, double maxima) throws SQLException {
        if (cp == null || cp.nombre() == null || cp.nombre().isBlank() || cp.hora() == null
                || !cp.hora().matches("(?:[01]\\d|2[0-3]):[0-5]\\d")
                || !Double.isFinite(cp.latitud()) || cp.latitud() < -90 || cp.latitud() > 90
                || !Double.isFinite(cp.longitud()) || cp.longitud() < -180 || cp.longitud() > 180
                || !Double.isFinite(cp.altitud()) || cp.altitud() < 0 || cp.altitud() > maxima)
            throw new SQLException("Revisa nombre, hora, coordenadas y altitud de cada Checkpoint. Ninguno puede superar la altitud máxima copiada de la Ruta (" + maxima + " m).");
    }

    private void agregarCheckpointRuta(Connection c, int rutaId, Checkpoint cp) throws SQLException {
        long id;
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO checkpoints (nombre,hora,latitud,longitud,altitud,descripcion) VALUES (?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cp.nombre().trim()); ps.setString(2, cp.hora()); ps.setDouble(3, cp.latitud());
            ps.setDouble(4, cp.longitud()); ps.setDouble(5, cp.altitud()); ps.setString(6, cp.descripcion()); ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { if (!rs.next()) throw new SQLException("No se pudo crear el Checkpoint de Ruta."); id = rs.getLong(1); }
        }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO ruta_checkpoints (ruta_id,checkpoint_id,orden) VALUES (?,?,?)")) {
            ps.setInt(1, rutaId); ps.setLong(2, id);
            try (PreparedStatement max = c.prepareStatement("SELECT COALESCE(MAX(orden),0)+1 FROM ruta_checkpoints WHERE ruta_id = ?")) {
                max.setInt(1, rutaId); try (ResultSet rs = max.executeQuery()) { ps.setInt(3, rs.next() ? rs.getInt(1) : 1); }
            }
            ps.executeUpdate();
        }
    }

    public Map<String, Integer> obtenerIndicadores() {
        LocalDate hoy = LocalDate.now();
        try (Connection c = ConexionDB.conectar(); Statement s = c.createStatement()) {
            int proximas = contar(s, "SELECT COUNT(*) FROM salidas WHERE estado = 'planificada' AND fecha BETWEEN '" + hoy + "' AND '" + hoy.plusDays(7) + "'");
            int rutas = contar(s, "SELECT COUNT(*) FROM rutas WHERE activo = 1");
            int clientes = contar(s, "SELECT COUNT(*) FROM clientes WHERE activo = 1");
            int criticos = contar(s, """
                    SELECT COUNT(*) FROM equipamiento e
                    WHERE e.activo = 1 AND (
                        e.cantidad - COALESCE((SELECT SUM(se.cantidad) FROM salida_equipamiento se
                            JOIN salidas sa ON sa.id = se.salida_id
                            WHERE se.equipamiento_id = e.id AND sa.estado = 'planificada'), 0) <= 2
                        OR e.estado IN (%s))
                    """.formatted(EquipamientoDAO.estadosNoAsignablesSql()));
            return Map.of("proximas", proximas, "rutas", rutas, "clientes", clientes, "criticos", criticos);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron calcular los indicadores.", e);
        }
    }

    private int contar(Statement s, String sql) throws SQLException {
        try (ResultSet rs = s.executeQuery(sql)) { return rs.next() ? rs.getInt(1) : 0; }
    }

    public int crear(LocalDate fecha, int rutaId) throws SQLException {
        try (Connection c = ConexionDB.conectar()) {
            c.setAutoCommit(false);
            try {
                Ruta ruta = rutaDAO.obtenerPorId(c, rutaId);
                if (ruta == null) throw new SQLException("La ruta seleccionada no existe.");
                try (PreparedStatement activa = c.prepareStatement(
                        "SELECT activo FROM rutas WHERE id = ?")) {
                    activa.setInt(1, rutaId);
                    try (ResultSet rs = activa.executeQuery()) {
                        if (!rs.next() || rs.getInt(1) != 1)
                            throw new SQLException("Selecciona una ruta activa para crear la salida.");
                    }
                }
                String sql = """
                        INSERT INTO salidas (ruta_id, fecha, estado, ruta_nombre,
                            ruta_altitud_maxima, ruta_tipo_terreno,
                            ruta_dificultad_tecnica, ruta_dificultad_fisica)
                        VALUES (?, ?, 'planificada', ?, ?, ?, ?, ?)
                        """;
                int id;
                try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, rutaId); ps.setString(2, fecha.toString());
                    ps.setString(3, ruta.getNombre()); ps.setDouble(4, ruta.getAltitudMaxima());
                    ps.setString(5, ruta.getTipoTerreno()); ps.setString(6, ruta.getDificultadTecnica());
                    ps.setString(7, ruta.getDificultadFisica()); ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("No se obtuvo el ID de la salida.");
                        id = keys.getInt(1);
                    }
                }
                c.commit();
                return id;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private void bloquearInventario(Connection c, List<AsignacionEquipamiento> asignaciones)
            throws SQLException {
        // SQLite obtains its database write lock on this statement, before reading availability.
        try (PreparedStatement ps = c.prepareStatement("UPDATE equipamiento SET cantidad = cantidad WHERE id = ?")) {
            for (AsignacionEquipamiento item : asignaciones.stream()
                    .filter(a -> !a.nuevo()).sorted(Comparator.comparingInt(a -> a.equipamiento().getId())).toList()) {
                ps.setInt(1, item.equipamiento().getId());
                if (ps.executeUpdate() != 1) throw new SQLException("El Elemento de inventario ya no existe.");
            }
        }
    }

    private boolean esActivo(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT activo FROM equipamiento WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getInt(1) == 1; }
        }
    }

    private SalidaActual mapear(ResultSet rs) throws SQLException {
        return new SalidaActual(rs.getInt("ruta_id"), "planificada".equals(rs.getString("estado")),
                rs.getString("ruta_nombre"), rs.getDouble("ruta_altitud_maxima"),
                rs.getString("ruta_tipo_terreno"), rs.getString("ruta_dificultad_tecnica"),
                rs.getString("ruta_dificultad_fisica"));
    }
}
