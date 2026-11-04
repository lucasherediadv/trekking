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

import com.example.core.excepcion.EntidadNoEncontradaException;
import com.example.core.ruta.DatosRuta;
import com.example.core.ruta.NombreRuta;
import com.example.core.ruta.RepositorioRutas;
import com.example.core.ruta.Ruta;

/** Implementación SQLite de las Rutas. La cantidad de Checkpoints sale del Itinerario guardado. */
public class RepositorioRutasSqlite implements RepositorioRutas {

    private static final String SELECT_RUTAS = """
            SELECT r.id, r.nombre, r.altitud_maxima, r.tipo_terreno, r.dificultad_tecnica,
                   r.dificultad_fisica, r.activo,
                   (SELECT COUNT(*) FROM ruta_checkpoints rc WHERE rc.ruta_id = r.id) AS cantidad_checkpoints
            FROM rutas r
            """;

    private final BaseDatosSqlite base;

    public RepositorioRutasSqlite(BaseDatosSqlite base) {
        this.base = Objects.requireNonNull(base, "base");
    }

    @Override
    public int insertar(DatosRuta datos) {
        String sql = """
                INSERT INTO rutas (nombre, nombre_clave, altitud_maxima, tipo_terreno,
                                   dificultad_tecnica, dificultad_fisica, activo)
                VALUES (?, ?, ?, ?, ?, ?, 1)
                """;
        try (Connection c = base.conectar(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            asignar(ps, datos);
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (!claves.next()) throw new IllegalStateException("No se pudo guardar la ruta.");
                return claves.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar la ruta.", e);
        }
    }

    @Override
    public void actualizar(int id, DatosRuta datos) {
        String sql = """
                UPDATE rutas SET nombre = ?, nombre_clave = ?, altitud_maxima = ?, tipo_terreno = ?,
                                 dificultad_tecnica = ?, dificultad_fisica = ?
                WHERE id = ?
                """;
        try (Connection c = base.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            asignar(ps, datos);
            ps.setInt(7, id);
            if (ps.executeUpdate() == 0) throw new EntidadNoEncontradaException("Ruta", id);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo actualizar la ruta.", e);
        }
    }

    private static void asignar(PreparedStatement ps, DatosRuta datos) throws SQLException {
        String nombre = NombreRuta.normalizar(datos.nombre());
        ps.setString(1, nombre);
        ps.setString(2, NombreRuta.clave(nombre));
        ps.setDouble(3, datos.altitudMaxima());
        ps.setString(4, datos.tipoTerreno());
        ps.setString(5, datos.dificultadTecnica());
        ps.setString(6, datos.dificultadFisica());
    }

    @Override
    public void cambiarActiva(int id, boolean activa) {
        try (Connection c = base.conectar(); PreparedStatement ps = c.prepareStatement(
                "UPDATE rutas SET activo = ? WHERE id = ?")) {
            ps.setInt(1, activa ? 1 : 0);
            ps.setInt(2, id);
            if (ps.executeUpdate() == 0) throw new EntidadNoEncontradaException("Ruta", id);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo cambiar la vigencia de la ruta.", e);
        }
    }

    @Override
    public Optional<Ruta> buscarPorId(int id) {
        try (Connection c = base.conectar(); PreparedStatement ps = c.prepareStatement(SELECT_RUTAS + " WHERE r.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet r = ps.executeQuery()) {
                return r.next() ? Optional.of(mapear(r)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar la ruta.", e);
        }
    }

    @Override
    public boolean existeNombre(String nombre, int idExcluido) {
        try (Connection c = base.conectar(); PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM rutas WHERE nombre_clave = ? AND id <> ?")) {
            ps.setString(1, NombreRuta.clave(nombre));
            ps.setInt(2, idExcluido);
            try (ResultSet r = ps.executeQuery()) {
                return r.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo comprobar el nombre de la ruta.", e);
        }
    }

    @Override
    public List<Ruta> listarActivas() {
        return listar(" WHERE r.activo = 1");
    }

    @Override
    public List<Ruta> listarDadasDeBaja() {
        return listar(" WHERE r.activo = 0");
    }

    @Override
    public List<Ruta> listarTodas() {
        return listar("");
    }

    private List<Ruta> listar(String filtro) {
        try (Connection c = base.conectar(); PreparedStatement ps = c.prepareStatement(
                SELECT_RUTAS + filtro + " ORDER BY r.nombre COLLATE NOCASE, r.id");
                ResultSet r = ps.executeQuery()) {
            List<Ruta> resultado = new ArrayList<>();
            while (r.next()) resultado.add(mapear(r));
            return resultado;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo listar las rutas.", e);
        }
    }

    private static Ruta mapear(ResultSet r) throws SQLException {
        return new Ruta(r.getInt("id"), r.getString("nombre"), r.getDouble("altitud_maxima"),
                r.getString("tipo_terreno"), r.getString("dificultad_tecnica"), r.getString("dificultad_fisica"),
                r.getInt("activo") == 1, r.getInt("cantidad_checkpoints"));
    }
}
