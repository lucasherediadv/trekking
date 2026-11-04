package com.example.desktop.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.example.desktop.database.ConexionDB;

/**
 * Datos y consultas del panel de Inicio.
 *
 * <p>Vocabulario fijado en diseño:
 * <ul>
 *   <li>Inicio = sección lateral {@code dashboard}.</li>
 *   <li>Dashboard = panel informativo de solo lectura.</li>
 *   <li>Salida próxima = Salida planificada desde hoy hasta hoy más la ventana de días.</li>
 * </ul>
 */
public class DashboardService {

    /** Stock bajo: cantidad menor o igual a este valor (equipos activos). */
    public static final int UMBRAL_STOCK_BAJO = 2;

    /** Ventana de Salidas próximas en días (hoy + 7, inclusive). */
    public static final int VENTANA_PROXIMOS_DIAS = 7;

    /** Límite de filas en el dashboard. */
    public static final int LIMITE_PROXIMOS = 5;

    public record Kpis(
            int rutasActivas,
            int clientesActivos,
            int salidasProximas,
            int equipamientoCritico) {
    }

    public record ProximaSalida(
            int id,
            String nombreRuta,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            int participantes) {
    }

    public Kpis obtenerKpis() {
        return new Kpis(
                contarRutasActivas(),
                contarClientesActivos(),
                contarSalidasProximas(),
                contarEquipamientoCritico());
    }

    public int contarRutasActivas() {
        return contar("SELECT COUNT(*) FROM rutas WHERE activo = 1");
    }

    public int contarClientesActivos() {
        return contar("SELECT COUNT(*) FROM clientes WHERE activo = 1");
    }

    public int contarSalidasProximas() {
        String sql = """
                SELECT COUNT(*)
                FROM salidas
                WHERE estado = 'planificada' AND fecha >= ? AND fecha <= ?
                """;
        try (Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, LocalDate.now().toString());
            statement.setString(
                    2, LocalDate.now().plusDays(VENTANA_PROXIMOS_DIAS).toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        } catch (SQLException e) {
            System.out.println(
                    "Error al contar Salidas próximas: " + e.getMessage());
            return 0;
        }
    }

    public int contarEquipamientoCritico() {
        String sql = """
                SELECT COUNT(*)
                FROM equipamiento
                WHERE activo = 1
                  AND (cantidad <= ?
                       OR estado IN (%s))
                """.formatted(com.example.desktop.dao.EquipamientoDAO.estadosNoAsignablesSql());
        try (Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setInt(1, UMBRAL_STOCK_BAJO);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        } catch (SQLException e) {
            System.out.println(
                    "Error al contar equipamiento crítico: " + e.getMessage());
            return 0;
        }
    }

    public List<ProximaSalida> obtenerProximasSalidas() {
        return obtenerProximasSalidas(LIMITE_PROXIMOS);
    }

    /** Horario = hora del primer y del último Checkpoint de la Copia operativa. */
    public List<ProximaSalida> obtenerProximasSalidas(int limite) {
        String sql = """
                SELECT
                    s.id,
                    s.ruta_nombre,
                    s.fecha,
                    (SELECT sc.hora FROM salida_checkpoints sc
                     WHERE sc.salida_id = s.id ORDER BY sc.orden ASC LIMIT 1) AS hora_inicio,
                    (SELECT sc.hora FROM salida_checkpoints sc
                     WHERE sc.salida_id = s.id ORDER BY sc.orden DESC LIMIT 1) AS hora_fin,
                    (SELECT COUNT(*) FROM salida_participantes sp
                     WHERE sp.salida_id = s.id) AS participantes
                FROM salidas s
                WHERE s.estado = 'planificada' AND s.fecha >= ? AND s.fecha <= ?
                ORDER BY s.fecha ASC, hora_inicio ASC
                LIMIT ?
                """;
        List<ProximaSalida> proximas = new ArrayList<>();
        try (Connection conexion = ConexionDB.conectar();
                PreparedStatement statement = conexion.prepareStatement(sql)) {
            statement.setString(1, LocalDate.now().toString());
            statement.setString(
                    2, LocalDate.now().plusDays(VENTANA_PROXIMOS_DIAS).toString());
            statement.setInt(3, limite);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    proximas.add(new ProximaSalida(
                            resultSet.getInt("id"),
                            resultSet.getString("ruta_nombre"),
                            LocalDate.parse(resultSet.getString("fecha")),
                            parsearHora(resultSet.getString("hora_inicio")),
                            parsearHora(resultSet.getString("hora_fin")),
                            resultSet.getInt("participantes")));
                }
            }
        } catch (SQLException | RuntimeException e) {
            System.out.println(
                    "Error al obtener Salidas próximas: " + e.getMessage());
        }
        return proximas;
    }

    private int contar(String sql) {
        try (Connection conexion = ConexionDB.conectar();
                Statement statement = conexion.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException e) {
            System.out.println("Error al contar: " + e.getMessage());
            return 0;
        }
    }

    private static LocalTime parsearHora(String hora) {
        return hora == null || hora.isBlank() ? null : LocalTime.parse(hora);
    }
}
