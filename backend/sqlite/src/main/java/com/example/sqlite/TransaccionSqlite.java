package com.example.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.function.Supplier;

import com.example.core.transaccion.Transaccion;

/**
 * Transacción SQLite que toma el bloqueo de escritura al comenzar ({@code BEGIN IMMEDIATE}): dos
 * instancias de la aplicación no pueden validar y reservar stock a la vez (ADR 0003). Los repositorios
 * que usan la misma {@link BaseDatosSqlite} participan de ella sin cambios.
 */
public class TransaccionSqlite implements Transaccion {

    private final BaseDatosSqlite base;

    public TransaccionSqlite(BaseDatosSqlite base) {
        this.base = Objects.requireNonNull(base, "base");
    }

    @Override
    public <T> T ejecutar(Supplier<T> trabajo) {
        if (base.hayTransaccion()) return trabajo.get(); // ya serializada por la transacción externa
        try (Connection conexion = base.abrir()) {
            ejecutar(conexion, "BEGIN IMMEDIATE");
            base.iniciarTransaccion(conexion);
            try {
                T resultado = trabajo.get();
                ejecutar(conexion, "COMMIT");
                return resultado;
            } catch (RuntimeException | Error e) {
                try {
                    ejecutar(conexion, "ROLLBACK");
                } catch (SQLException | RuntimeException suprimida) {
                    e.addSuppressed(suprimida);
                }
                throw e;
            } finally {
                base.terminarTransaccion();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo completar la transacción.", e);
        }
    }

    private static void ejecutar(Connection conexion, String sql) throws SQLException {
        try (Statement statement = conexion.createStatement()) {
            statement.execute(sql);
        }
    }
}
