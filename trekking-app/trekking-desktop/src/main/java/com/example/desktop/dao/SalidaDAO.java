package com.example.desktop.dao;

import com.example.desktop.database.ConexionDB;
import com.example.desktop.model.Ruta;
import com.example.desktop.model.Salida;
import com.example.desktop.model.Checkpoint;
import com.example.desktop.model.Equipamiento;
import com.example.desktop.model.AsignacionEquipamiento;
import com.example.desktop.model.AsignacionParticipante;
import com.example.desktop.model.Cliente;
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
    public record ResumenOperativo(String participantes, String equipamiento) {}

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
                Salida actual;
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
                    int total = new RutaCheckpointDAO().obtenerPorRuta(c, rutaId).size();
                    if (total + guardarEnRuta.size() == 1) throw new SQLException("La Ruta no puede quedar con exactamente un Checkpoint.");
                    for (Checkpoint cp : guardarEnRuta) {
                        validarCheckpoint(cp, ruta.getAltitudMaxima());
                        if (cp.getAltitud() > rutaCatalogo.getAltitudMaxima())
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
                        ins.setInt(1, salidaId); ins.setString(2, cp.getNombre().trim()); ins.setString(3, cp.getHora());
                        ins.setDouble(4, cp.getLatitud()); ins.setDouble(5, cp.getLongitud()); ins.setDouble(6, cp.getAltitud());
                        ins.setString(7, cp.getDescripcion()); ins.setInt(8, orden++); ins.addBatch();
                    }
                    ins.executeBatch();
                }
                for (Checkpoint cp : guardarEnRuta) agregarCheckpointRuta(c, rutaId, cp);
                if (equipamiento != null && participantes != null) {
                    reemplazarAsignaciones(c, salidaId, equipamiento, participantes,
                            "planificada".equals(actual.estado()), refrescarEquipamiento, refrescarParticipantes);
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
            int id = a.cliente().getId();
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
            Cliente cl=a.cliente(); String[] cp=clienteCopias.get(cl.getId());
            if (refrescarParticipantes.contains(cl.getId())) cp=null;
            try(PreparedStatement ins=c.prepareStatement("INSERT INTO salida_participantes(salida_id,cliente_id,dni_copia,nombre_copia,apellido_copia,telefono_copia,emergencia_nombre_copia,emergencia_telefono_copia,emergencia_relacion_copia) VALUES(?,?,?,?,?,?,?,?,?)")) {
                ins.setInt(1,salidaId); ins.setInt(2,cl.getId()); ins.setString(3,cp==null?cl.getDni():cp[0]); ins.setString(4,cp==null?cl.getNombre():cp[1]); ins.setString(5,cp==null?cl.getApellido():cp[2]); ins.setString(6,cp==null?cl.getTelefono():cp[3]); ins.setString(7,cp==null?cl.getContactoEmergenciaNombre():cp[4]); ins.setString(8,cp==null?cl.getContactoEmergenciaTelefono():cp[5]); ins.setString(9,cp==null?cl.getContactoEmergenciaRelacion():cp[6]); ins.executeUpdate();
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

    private void validarCheckpoint(Checkpoint cp, double maxima) throws SQLException {
        if (cp == null || cp.getNombre() == null || cp.getNombre().isBlank() || cp.getHora() == null
                || !cp.getHora().matches("(?:[01]\\d|2[0-3]):[0-5]\\d")
                || !Double.isFinite(cp.getLatitud()) || cp.getLatitud() < -90 || cp.getLatitud() > 90
                || !Double.isFinite(cp.getLongitud()) || cp.getLongitud() < -180 || cp.getLongitud() > 180
                || !Double.isFinite(cp.getAltitud()) || cp.getAltitud() < 0 || cp.getAltitud() > maxima)
            throw new SQLException("Revisa nombre, hora, coordenadas y altitud de cada Checkpoint. Ninguno puede superar la altitud máxima copiada de la Ruta (" + maxima + " m).");
    }

    private void agregarCheckpointRuta(Connection c, int rutaId, Checkpoint cp) throws SQLException {
        long id;
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO checkpoints (nombre,hora,latitud,longitud,altitud,descripcion) VALUES (?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cp.getNombre().trim()); ps.setString(2, cp.getHora()); ps.setDouble(3, cp.getLatitud());
            ps.setDouble(4, cp.getLongitud()); ps.setDouble(5, cp.getAltitud()); ps.setString(6, cp.getDescripcion()); ps.executeUpdate();
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

    public List<Salida> obtenerTodas() {
        String sql = "SELECT * FROM salidas ORDER BY fecha DESC, id DESC";
        List<Salida> salidas = new ArrayList<>();
        try (Connection c = ConexionDB.conectar();
             Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) salidas.add(mapear(rs));
        } catch (SQLException | RuntimeException e) {
            throw new IllegalStateException("No se pudieron cargar las salidas.", e);
        }
        return salidas;
    }

    /** Returns searchable participant copies and assigned equipment summaries for the Salidas list. */
    public Map<Integer, ResumenOperativo> obtenerResumenesOperativos() {
        Map<Integer, ResumenOperativo> resumenes = new HashMap<>();
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
        try (Connection c = ConexionDB.conectar(); Statement statement = c.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) resumenes.put(rs.getInt("id"), new ResumenOperativo(
                    rs.getString("participantes"), rs.getString("equipamiento")));
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron cargar los resúmenes de las Salidas.", e);
        }
        return resumenes;
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

    /** Confirma una Salida y, opcionalmente, su Ruta nueva en una única transacción. */
    public int confirmar(LocalDate fecha, Integer rutaId, Ruta rutaNueva) throws SQLException {
        return confirmar(fecha, rutaId, rutaNueva, List.of());
    }

    /** Confirma la Salida y sus reservas de Inventario de forma atómica. */
    public int confirmar(LocalDate fecha, Integer rutaId, Ruta rutaNueva,
            List<AsignacionEquipamiento> asignaciones) throws SQLException {
        return confirmar(fecha, rutaId, rutaNueva, asignaciones, List.of());
    }

    /** Confirma la Salida, sus reservas y sus Participantes en una sola transacción. */
    public int confirmar(LocalDate fecha, Integer rutaId, Ruta rutaNueva,
            List<AsignacionEquipamiento> asignaciones, List<AsignacionParticipante> participantes) throws SQLException {
        if (fecha == null) throw new SQLException("La fecha de la Salida es obligatoria.");
        if ((rutaId == null) == (rutaNueva == null))
            throw new SQLException("Selecciona una Ruta o completa una Ruta nueva.");
        try (Connection c = ConexionDB.conectar()) {
            try (Statement timeout = c.createStatement()) { timeout.execute("PRAGMA busy_timeout = 5000"); }
            c.setAutoCommit(false);
            try {
                bloquearInventario(c, asignaciones);
                int idRuta = rutaId == null ? insertarRuta(c, rutaNueva) : rutaId;
                Ruta ruta = rutaDAO.obtenerPorId(c, idRuta);
                if (ruta == null) throw new SQLException("La Ruta seleccionada no existe.");
                if (rutaId != null) {
                    try (PreparedStatement ps = c.prepareStatement("SELECT activo FROM rutas WHERE id = ?")) {
                        ps.setInt(1, idRuta);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next() || rs.getInt(1) != 1)
                                throw new SQLException("Selecciona una Ruta activa.");
                        }
                    }
                }
                String sql = """
                        INSERT INTO salidas (ruta_id, fecha, estado, ruta_nombre, ruta_altitud_maxima,
                            ruta_tipo_terreno, ruta_dificultad_tecnica, ruta_dificultad_fisica)
                        VALUES (?, ?, 'planificada', ?, ?, ?, ?, ?)
                        """;
                int salidaId;
                try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, idRuta); ps.setString(2, fecha.toString());
                    ps.setString(3, ruta.getNombre()); ps.setDouble(4, ruta.getAltitudMaxima());
                    ps.setString(5, ruta.getTipoTerreno()); ps.setString(6, ruta.getDificultadTecnica());
                    ps.setString(7, ruta.getDificultadFisica()); ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("No se obtuvo el ID de la Salida.");
                        salidaId = keys.getInt(1);
                    }
                }
                List<Checkpoint> puntos = new RutaCheckpointDAO().obtenerPorRuta(c, idRuta);
                try (PreparedStatement ps = c.prepareStatement("""
                        INSERT INTO salida_checkpoints
                            (salida_id, nombre, hora, latitud, longitud, altitud, descripcion, orden)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """)) {
                    int orden = 0;
                    for (Checkpoint cp : puntos) {
                        if (cp.getAltitud() > ruta.getAltitudMaxima())
                            throw new SQLException("Un Checkpoint supera la altitud máxima de la Ruta.");
                        ps.setInt(1, salidaId); ps.setString(2, cp.getNombre()); ps.setString(3, cp.getHora());
                        ps.setDouble(4, cp.getLatitud()); ps.setDouble(5, cp.getLongitud());
                        ps.setDouble(6, cp.getAltitud()); ps.setString(7, cp.getDescripcion());
                        ps.setInt(8, orden++); ps.addBatch();
                    }
                    ps.executeBatch();
                }
                guardarAsignaciones(c, salidaId, asignaciones);
                guardarParticipantes(c, salidaId, participantes);
                c.commit();
                return salidaId;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                if (e instanceof SQLException sqlException) throw sqlException;
                throw new SQLException("No se pudo confirmar la Salida.", e);
            }
        }
    }

    private void guardarParticipantes(Connection c, int salidaId,
            List<AsignacionParticipante> participantes) throws SQLException {
        if (participantes == null) throw new SQLException("La lista de Participantes es inválida.");
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        java.util.Set<String> dnis = new java.util.HashSet<>();
        ClienteDAO clienteDAO = new ClienteDAO();
        for (AsignacionParticipante asignacion : participantes) {
            if (asignacion == null || asignacion.cliente() == null)
                throw new SQLException("Hay un Participante inválido.");
            Cliente cliente = asignacion.cliente();
            String dni = cliente.getDni() == null ? "" : cliente.getDni().trim();
            if (dni.isEmpty() || !dnis.add(dni))
                throw new SQLException("No se puede asociar dos veces el mismo Cliente.");
            int clienteId = cliente.getId();
            if (asignacion.nuevo()) {
                try (PreparedStatement ps = c.prepareStatement("SELECT activo FROM clientes WHERE dni = ?")) {
                    ps.setString(1, dni);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            if (rs.getInt("activo") != 1)
                                throw new SQLException("El DNI ya pertenece a un Cliente inactivo. Reactívalo desde Clientes antes de asociarlo.");
                            throw new SQLException("El DNI ya pertenece a un Cliente activo. Busca y selecciona el registro existente.");
                        }
                    }
                }
                clienteId = clienteDAO.insertarEnTransaccion(c, cliente,
                        asignacion.fichaMedica(), asignacion.nombreFichaMedica());
            } else {
                try (PreparedStatement ps = c.prepareStatement("SELECT dni, activo FROM clientes WHERE id = ?")) {
                    ps.setInt(1, clienteId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("El Cliente seleccionado ya no existe.");
                        if (rs.getInt("activo") != 1)
                            throw new SQLException("El Cliente está inactivo. Reactívalo desde Clientes antes de asociarlo.");
                        if (!dni.equals(rs.getString("dni")))
                            throw new SQLException("El DNI del Cliente seleccionado cambió. Actualiza la lista y vuelve a seleccionarlo.");
                    }
                }
            }
            if (!ids.add(clienteId)) throw new SQLException("No se puede asociar dos veces el mismo Cliente.");
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO salida_participantes
                        (salida_id, cliente_id, dni_copia, nombre_copia, apellido_copia, telefono_copia,
                         emergencia_nombre_copia, emergencia_telefono_copia, emergencia_relacion_copia)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                ps.setInt(1, salidaId); ps.setInt(2, clienteId); ps.setString(3, dni);
                ps.setString(4, cliente.getNombre()); ps.setString(5, cliente.getApellido());
                ps.setString(6, cliente.getTelefono());
                ps.setString(7, cliente.getContactoEmergenciaNombre());
                ps.setString(8, cliente.getContactoEmergenciaTelefono());
                ps.setString(9, cliente.getContactoEmergenciaRelacion());
                ps.executeUpdate();
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

    private void guardarAsignaciones(Connection c, int salidaId,
            List<AsignacionEquipamiento> asignaciones) throws SQLException {
        if (asignaciones == null) throw new SQLException("La lista de Equipamiento es inválida.");
        String insertItem = "INSERT INTO equipamiento (nombre, categoria, cantidad, estado, activo) VALUES (?, ?, ?, ?, 1)";
        String insertAsignacion = """
                INSERT INTO salida_equipamiento (salida_id, equipamiento_id, nombre_copia, categoria_copia, cantidad)
                VALUES (?, ?, ?, ?, ?)
                """;
        for (AsignacionEquipamiento asignacion : asignaciones) {
            if (asignacion == null || asignacion.equipamiento() == null || asignacion.cantidad() <= 0)
                throw new SQLException("La cantidad asignada debe ser un entero positivo.");
            Equipamiento item = asignacion.equipamiento();
            int id = item.getId();
            int total = item.getCantidad();
            if (asignacion.nuevo()) {
                if (item.getNombre() == null || item.getNombre().isBlank())
                    throw new SQLException("El nombre del Elemento de inventario es obligatorio.");
                if (total < 0) throw new SQLException("La cantidad total debe ser mayor o igual a cero.");
                if (!Equipamiento.categoriaValida(item.getCategoria()))
                    throw new SQLException("Selecciona una categoría válida para el Elemento de inventario.");
                if (!Equipamiento.estadoValido(item.getEstado()))
                    throw new SQLException("Selecciona un estado válido para el Elemento de inventario.");
                try (PreparedStatement ps = c.prepareStatement(insertItem, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, item.getNombre()); ps.setString(2, item.getCategoria());
                    ps.setInt(3, total); ps.setString(4, item.getEstado()); ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("No se pudo guardar el Elemento de inventario nuevo.");
                        id = keys.getInt(1);
                    }
                }
            } else {
                Equipamiento actual = EquipamientoDAO.obtenerPorId(c, id);
                if (actual == null || actual.getId() != id) throw new SQLException("El Elemento de inventario seleccionado ya no existe.");
                if (!esActivo(c, id)) throw new SQLException("El Elemento de inventario está dado de baja y no puede asignarse.");
                total = actual.getCantidad();
                int disponible = total - actual.getCantidadReservada();
                if (asignacion.cantidad() > disponible)
                    throw new SQLException("No hay cantidad suficiente de " + actual.getNombre() + ". Disponible: " + disponible + ".");
                item = actual;
            }
            if (asignacion.cantidad() > total)
                throw new SQLException("No hay cantidad suficiente de " + item.getNombre() + ". Disponible: " + total + ".");
            try (PreparedStatement ps = c.prepareStatement(insertAsignacion)) {
                ps.setInt(1, salidaId); ps.setInt(2, id); ps.setString(3, item.getNombre());
                ps.setString(4, item.getCategoria()); ps.setInt(5, asignacion.cantidad()); ps.executeUpdate();
            }
        }
    }

    private boolean esActivo(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT activo FROM equipamiento WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getInt(1) == 1; }
        }
    }

    private int insertarRuta(Connection c, Ruta ruta) throws SQLException {
        String nombre = Ruta.normalizarNombre(ruta.getNombre());
        if (nombre.isBlank() || nombre.length() > 100 || !Ruta.nombreValido(nombre))
            throw new SQLException("El nombre de la Ruta es obligatorio y debe tener hasta 100 caracteres válidos.");
        if (ruta.getAltitudMaxima() < 0 || !Double.isFinite(ruta.getAltitudMaxima()))
            throw new SQLException("La altitud máxima debe ser un número mayor o igual a cero.");
        try (PreparedStatement check = c.prepareStatement("SELECT 1 FROM rutas WHERE nombre_clave = ?")) {
            check.setString(1, Ruta.claveNombre(nombre));
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) throw new SQLException("Ya existe una Ruta con ese nombre.");
            }
        }
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO rutas (nombre, nombre_clave, altitud_maxima, tipo_terreno, dificultad_tecnica, dificultad_fisica)
                VALUES (?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre); ps.setString(2, Ruta.claveNombre(nombre)); ps.setDouble(3, ruta.getAltitudMaxima());
            ps.setString(4, ruta.getTipoTerreno()); ps.setString(5, ruta.getDificultadTecnica());
            ps.setString(6, ruta.getDificultadFisica()); ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No se pudo guardar la Ruta nueva.");
                return keys.getInt(1);
            }
        }
    }

    public void cambiarEstado(int id, String nuevoEstado) throws SQLException {
        if (!List.of("realizada", "cancelada").contains(nuevoEstado))
            throw new SQLException("Estado de cierre inválido.");
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement(
                "UPDATE salidas SET estado = ? WHERE id = ? AND estado = 'planificada'")) {
            ps.setString(1, nuevoEstado); ps.setInt(2, id);
            if (ps.executeUpdate() != 1)
                throw new SQLException("Solo se puede cerrar una salida planificada.");
        }
    }

    private Salida mapear(ResultSet rs) throws SQLException {
        return new Salida(rs.getInt("id"), rs.getInt("ruta_id"),
                LocalDate.parse(rs.getString("fecha")), rs.getString("estado"),
                rs.getString("ruta_nombre"), rs.getDouble("ruta_altitud_maxima"),
                rs.getString("ruta_tipo_terreno"), rs.getString("ruta_dificultad_tecnica"),
                rs.getString("ruta_dificultad_fisica"));
    }
}
