package com.example.desktop.dao;

import com.example.desktop.database.ConexionDB;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Map;

/** Consultas de indicadores del tablero de Salidas; la edición y el listado pasan por core. */
public class SalidaDAO {

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
}
