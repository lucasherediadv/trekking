package com.example.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.example.core.excepcion.EntidadNoEncontradaException;
import com.example.core.excepcion.ErrorCampo;
import com.example.core.excepcion.ValidacionException;
import com.example.core.inventario.DatosElemento;
import com.example.core.inventario.ElementoInventario;
import com.example.core.inventario.EstadoElemento;
import com.example.core.inventario.RepositorioInventario;

/** Implementación SQLite del Inventario. Es dueña de la conversión entre el enum y los textos guardados. */
public class RepositorioInventarioSqlite implements RepositorioInventario {

    private static final String SELECT_ELEMENTOS = """
            SELECT e.id, e.nombre, e.categoria, e.cantidad, e.estado,
                   COALESCE((SELECT SUM(se.cantidad)
                             FROM salida_equipamiento se
                             JOIN salidas s ON s.id = se.salida_id
                             WHERE se.equipamiento_id = e.id AND s.estado = 'planificada'), 0) AS cantidad_reservada
            FROM equipamiento e
            """;

    private final BaseDatosSqlite base;

    public RepositorioInventarioSqlite(BaseDatosSqlite base) {
        this.base = Objects.requireNonNull(base, "base");
    }

    /** Texto con el que se guarda cada estado; no cambia respecto de las bases existentes. */
    static String texto(EstadoElemento estado) {
        return switch (estado) {
            case DISPONIBLE -> "Disponible";
            case EN_REPARACION -> "En Reparación";
            case EN_INSPECCION -> "En Inspección";
            case FUERA_DE_SERVICIO -> "Fuera de Servicio";
            case EXTRAVIADO -> "Extraviado";
        };
    }

    private static EstadoElemento estado(String texto) {
        for (EstadoElemento estado : EstadoElemento.values()) {
            if (texto(estado).equals(texto)) return estado;
        }
        throw new IllegalStateException("Estado de elemento desconocido en la base: " + texto);
    }

    @Override
    public int insertar(DatosElemento datos) {
        String sql = "INSERT INTO equipamiento (nombre, categoria, cantidad, estado, activo) VALUES (?, ?, ?, ?, 1)";
        try (Connection c = base.conectar(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, datos.nombre().strip());
            ps.setString(2, datos.categoria());
            ps.setInt(3, datos.cantidad());
            ps.setString(4, texto(datos.estado()));
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (!claves.next()) throw new IllegalStateException("No se pudo guardar el elemento de inventario.");
                return claves.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar el elemento de inventario.", e);
        }
    }

    @Override
    public void actualizar(int id, DatosElemento datos) {
        // La condición sobre la reserva se evalúa en la misma sentencia para no perder una reserva concurrente.
        String sql = """
                UPDATE equipamiento
                SET nombre = ?, categoria = ?, cantidad = ?, estado = ?
                WHERE id = ? AND ? >= (
                    SELECT COALESCE(SUM(se.cantidad), 0)
                    FROM salida_equipamiento se
                    JOIN salidas s ON s.id = se.salida_id
                    WHERE se.equipamiento_id = equipamiento.id AND s.estado = 'planificada')
                """;
        try (Connection c = base.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, datos.nombre().strip());
            ps.setString(2, datos.categoria());
            ps.setInt(3, datos.cantidad());
            ps.setString(4, texto(datos.estado()));
            ps.setInt(5, id);
            ps.setInt(6, datos.cantidad());
            if (ps.executeUpdate() == 0) {
                if (buscarPorId(c, id).isEmpty()) throw new EntidadNoEncontradaException("Elemento de inventario", id);
                throw new ValidacionException(List.of(new ErrorCampo("cantidad",
                        "La cantidad total no puede ser menor que la cantidad reservada.")));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo actualizar el elemento de inventario.", e);
        }
    }

    @Override
    public void cambiarActivo(int id, boolean activo) {
        try (Connection c = base.conectar(); PreparedStatement ps = c.prepareStatement(
                "UPDATE equipamiento SET activo = ? WHERE id = ?")) {
            ps.setInt(1, activo ? 1 : 0);
            ps.setInt(2, id);
            if (ps.executeUpdate() == 0) throw new EntidadNoEncontradaException("Elemento de inventario", id);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo cambiar la vigencia del elemento de inventario.", e);
        }
    }

    @Override
    public Optional<ElementoInventario> buscarPorId(int id) {
        try (Connection c = base.conectar()) {
            return buscarPorId(c, id);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar el elemento de inventario.", e);
        }
    }

    private Optional<ElementoInventario> buscarPorId(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT_ELEMENTOS + " WHERE e.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet r = ps.executeQuery()) {
                return r.next() ? Optional.of(mapear(r)) : Optional.empty();
            }
        }
    }

    @Override
    public List<ElementoInventario> listarActivos() {
        return listarActivosExcluyendo(Set.of());
    }

    @Override
    public List<ElementoInventario> listarDadosDeBaja() {
        return listar(0, Set.of());
    }

    @Override
    public List<ElementoInventario> listarActivosExcluyendo(Set<EstadoElemento> estadosExcluidos) {
        return listar(1, estadosExcluidos);
    }

    private List<ElementoInventario> listar(int activo, Set<EstadoElemento> excluidos) {
        StringBuilder sql = new StringBuilder(SELECT_ELEMENTOS).append(" WHERE e.activo = ?");
        if (!excluidos.isEmpty()) {
            sql.append(" AND e.estado NOT IN (").append(String.join(",", java.util.Collections.nCopies(excluidos.size(), "?"))).append(")");
        }
        sql.append(" ORDER BY e.nombre COLLATE NOCASE, e.id");
        try (Connection c = base.conectar(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int indice = 1;
            ps.setInt(indice++, activo);
            for (EstadoElemento estado : excluidos) ps.setString(indice++, texto(estado));
            List<ElementoInventario> resultado = new ArrayList<>();
            try (ResultSet r = ps.executeQuery()) {
                while (r.next()) resultado.add(mapear(r));
            }
            return resultado;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo listar el inventario.", e);
        }
    }

    private static ElementoInventario mapear(ResultSet r) throws SQLException {
        return new ElementoInventario(r.getInt("id"), r.getString("nombre"), r.getString("categoria"),
                r.getInt("cantidad"), estado(r.getString("estado")), r.getInt("cantidad_reservada"));
    }
}
