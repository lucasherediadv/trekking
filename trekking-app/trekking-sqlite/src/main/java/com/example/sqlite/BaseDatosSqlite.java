package com.example.sqlite;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.example.core.ruta.NombreRuta;

/** Base SQLite de la aplicación: abre conexiones y es dueña del esquema. */
public class BaseDatosSqlite {

    private final String url;

    /** @param archivo ubicación del archivo de base; se crea al conectar si no existe. */
    public BaseDatosSqlite(Path archivo) {
        this.url = "jdbc:sqlite:" + Objects.requireNonNull(archivo, "archivo");
    }

    public Connection conectar() throws SQLException {
        Connection conexion = DriverManager.getConnection(url);
        try (Statement statement = conexion.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException e) {
            conexion.close();
            throw e;
        }
        return conexion;
    }

    /**
     * Crea las tablas e índices que falten y aplica las migraciones de datos.
     * Es idempotente y no borra tablas existentes.
     */
    public void inicializarEsquema() {
        String sql = """
                            CREATE TABLE IF NOT EXISTS rutas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL COLLATE NOCASE UNIQUE,
                    nombre_clave TEXT NOT NULL UNIQUE,
                    altitud_maxima REAL NOT NULL,
                    tipo_terreno TEXT NOT NULL,
                    dificultad_tecnica TEXT NOT NULL,
                    dificultad_fisica TEXT NOT NULL,
                    activo INTEGER NOT NULL DEFAULT 1
                )
                            """;
        String sqlCheckpoints = """
                CREATE TABLE IF NOT EXISTS checkpoints (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    hora TEXT NOT NULL,
                    latitud REAL NOT NULL CHECK (latitud BETWEEN -90 AND 90),
                    longitud REAL NOT NULL CHECK (longitud BETWEEN -180 AND 180),
                    altitud REAL NOT NULL DEFAULT 0 CHECK (altitud >= 0),
                    descripcion TEXT
                )
                """;
        String sqlRutaCheckpoints = """
                CREATE TABLE IF NOT EXISTS ruta_checkpoints (
                    ruta_id INTEGER NOT NULL,
                    checkpoint_id INTEGER NOT NULL,
                    orden INTEGER NOT NULL,
                    PRIMARY KEY (ruta_id, checkpoint_id),
                    UNIQUE (ruta_id, orden),
                    UNIQUE (checkpoint_id),
                    FOREIGN KEY (ruta_id) REFERENCES rutas(id),
                    FOREIGN KEY (checkpoint_id) REFERENCES checkpoints(id)
                )
                """;
        String sqlEquipamiento = """
                CREATE TABLE IF NOT EXISTS equipamiento (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    categoria TEXT NOT NULL,
                    cantidad INTEGER NOT NULL,
                    estado TEXT NOT NULL,
                    activo INTEGER NOT NULL DEFAULT 1
                )
                """;
        String sqlClientes = """  
            CREATE TABLE IF NOT EXISTS clientes ( id INTEGER PRIMARY KEY AUTOINCREMENT, dni TEXT UNIQUE, nombre TEXT NOT NULL, apellido TEXT NOT NULL, fecha_nacimiento TEXT, email TEXT, telefono TEXT, sexo_id INTEGER, sexo TEXT NOT NULL, contacto_emergencia_nombre TEXT, contacto_emergencia_telefono TEXT, contacto_emergencia_relacion TEXT, autorizacion_menores INTEGER NOT NULL DEFAULT 0, tutor_nombre TEXT, tutor_apellido TEXT, tutor_dni TEXT, tutor_telefono TEXT, activo INTEGER NOT NULL DEFAULT 1, ficha_medica BLOB, nombre_ficha_medica TEXT, fecha_alta TEXT NOT NULL DEFAULT CURRENT_DATE, fecha_modificacion TEXT NOT NULL DEFAULT CURRENT_DATE )
                """;
        String sqlSexos = """ 
            CREATE TABLE IF NOT EXISTS sexos ( id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL UNIQUE ) 
                """;
        String sqlContactosEmergencia = """ 
            CREATE TABLE IF NOT EXISTS contactos_emergencia ( id INTEGER PRIMARY KEY AUTOINCREMENT, cliente_id INTEGER NOT NULL UNIQUE, nombre TEXT NOT NULL, telefono TEXT NOT NULL, relacion TEXT NOT NULL, FOREIGN KEY (cliente_id) REFERENCES clientes(id) ) 
                """;
        String sqlSalidas = """
                CREATE TABLE IF NOT EXISTS salidas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    ruta_id INTEGER NOT NULL,
                    fecha TEXT NOT NULL,
                    estado TEXT NOT NULL DEFAULT 'planificada'
                        CHECK (estado IN ('planificada', 'realizada', 'cancelada')),
                    ruta_nombre TEXT NOT NULL,
                    ruta_altitud_maxima REAL NOT NULL CHECK (ruta_altitud_maxima >= 0),
                    ruta_tipo_terreno TEXT NOT NULL,
                    ruta_dificultad_tecnica TEXT NOT NULL,
                    ruta_dificultad_fisica TEXT NOT NULL,
                    FOREIGN KEY (ruta_id) REFERENCES rutas(id)
                )
                """;
        String sqlSalidaCheckpoints = """
                CREATE TABLE IF NOT EXISTS salida_checkpoints (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    salida_id INTEGER NOT NULL,
                    nombre TEXT NOT NULL,
                    hora TEXT NOT NULL,
                    latitud REAL NOT NULL CHECK (latitud BETWEEN -90 AND 90),
                    longitud REAL NOT NULL CHECK (longitud BETWEEN -180 AND 180),
                    altitud REAL NOT NULL CHECK (altitud >= 0),
                    descripcion TEXT,
                    orden INTEGER NOT NULL,
                    FOREIGN KEY (salida_id) REFERENCES salidas(id) ON DELETE CASCADE,
                    UNIQUE (salida_id, orden)
                )
                """;
        String sqlSalidaEquipamiento = """
                CREATE TABLE IF NOT EXISTS salida_equipamiento (
                    salida_id INTEGER NOT NULL,
                    equipamiento_id INTEGER NOT NULL,
                    nombre_copia TEXT NOT NULL,
                    categoria_copia TEXT NOT NULL,
                    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
                    PRIMARY KEY (salida_id, equipamiento_id),
                    FOREIGN KEY (salida_id) REFERENCES salidas(id) ON DELETE CASCADE,
                    FOREIGN KEY (equipamiento_id) REFERENCES equipamiento(id)
                )
                """;
        String sqlSalidaParticipantes = """
                CREATE TABLE IF NOT EXISTS salida_participantes (
                    salida_id INTEGER NOT NULL,
                    cliente_id INTEGER NOT NULL,
                    dni_copia TEXT NOT NULL,
                    nombre_copia TEXT NOT NULL,
                    apellido_copia TEXT NOT NULL,
                    telefono_copia TEXT,
                    emergencia_nombre_copia TEXT,
                    emergencia_telefono_copia TEXT,
                    emergencia_relacion_copia TEXT,
                    PRIMARY KEY (salida_id, cliente_id),
                    FOREIGN KEY (salida_id) REFERENCES salidas(id) ON DELETE CASCADE,
                    FOREIGN KEY (cliente_id) REFERENCES clientes(id)
                )
                """;
        try (
                Connection conexion = conectar(); Statement statement = conexion.createStatement()) {
            statement.execute(sql);
            migrarNombreClaveRuta(conexion);
            // Tabla de checkpoints
            statement.execute(sqlCheckpoints);
            statement.execute(sqlRutaCheckpoints);
            statement.execute(sqlEquipamiento);
            statement.execute(sqlClientes);
            statement.execute(sqlSexos);
            statement.execute(sqlContactosEmergencia);
            statement.execute(sqlSalidas);
            statement.execute(sqlSalidaCheckpoints);
            statement.execute(sqlSalidaEquipamiento);
            statement.execute(sqlSalidaParticipantes);
            // "Asignado" dejó de ser un estado: la asignación se deduce de la Reserva de stock.
            statement.executeUpdate("UPDATE equipamiento SET estado = 'Disponible' WHERE estado = 'Asignado'");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_salidas_fecha_estado ON salidas(fecha, estado)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_salida_equipamiento_elemento ON salida_equipamiento(equipamiento_id)");
            statement.executeUpdate(""" 
                INSERT OR IGNORE INTO sexos (nombre) VALUES ('Masculino'), ('Femenino'), ('No binario'), ('Otro'), ('Prefiero no decir') 
                """);

        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo inicializar la base de datos: " + e.getMessage(), e);
        }
    }

    /** Actualiza bases existentes que todavía no tienen la clave normalizada de las Rutas. */
    private static void migrarNombreClaveRuta(Connection conexion) throws SQLException {
        boolean existeNombreClave = false;
        try (Statement statement = conexion.createStatement();
             ResultSet columnas = statement.executeQuery("PRAGMA table_info(rutas)")) {
            while (columnas.next()) {
                if ("nombre_clave".equalsIgnoreCase(columnas.getString("name"))) {
                    existeNombreClave = true;
                    break;
                }
            }
        }
        if (!existeNombreClave) {
            try (Statement statement = conexion.createStatement()) {
                statement.execute("ALTER TABLE rutas ADD COLUMN nombre_clave TEXT");
            }
        }

        List<Object[]> pendientes = new ArrayList<>();
        try (Statement statement = conexion.createStatement();
             ResultSet rutas = statement.executeQuery("SELECT id, nombre FROM rutas WHERE nombre_clave IS NULL OR nombre_clave = ''")) {
            while (rutas.next()) {
                pendientes.add(new Object[]{rutas.getInt("id"), NombreRuta.clave(rutas.getString("nombre"))});
            }
        }
        try (PreparedStatement actualizar = conexion.prepareStatement("UPDATE rutas SET nombre_clave = ? WHERE id = ?")) {
            for (Object[] fila : pendientes) {
                actualizar.setString(1, (String) fila[1]);
                actualizar.setInt(2, (Integer) fila[0]);
                actualizar.addBatch();
            }
            actualizar.executeBatch();
        }
        try (Statement statement = conexion.createStatement()) {
            statement.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx_rutas_nombre_clave ON rutas(nombre_clave)");
        }
    }

}
