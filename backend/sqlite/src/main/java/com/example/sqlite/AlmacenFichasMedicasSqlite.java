package com.example.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

import com.example.core.cliente.AlmacenFichasMedicas;
import com.example.core.cliente.FichaMedica;
import com.example.core.excepcion.EntidadNoEncontradaException;

/** Guarda la ficha médica en las columnas {@code ficha_medica} y {@code nombre_ficha_medica} del Cliente. */
public class AlmacenFichasMedicasSqlite implements AlmacenFichasMedicas {

    private final BaseDatosSqlite base;

    public AlmacenFichasMedicasSqlite(BaseDatosSqlite base) {
        this.base = Objects.requireNonNull(base, "base");
    }

    @Override
    public void guardar(int clienteId, FichaMedica ficha) {
        actualizar(clienteId, ficha.contenido(), ficha.nombreArchivo());
    }

    @Override
    public Optional<FichaMedica> buscar(int clienteId) {
        String sql = "SELECT ficha_medica, nombre_ficha_medica FROM clientes WHERE id = ?";
        try (Connection c = base.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getBytes("ficha_medica") != null) {
                    return Optional.of(new FichaMedica(rs.getString("nombre_ficha_medica"), rs.getBytes("ficha_medica")));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo leer la ficha médica.", e);
        }
    }

    @Override
    public void quitar(int clienteId) {
        actualizar(clienteId, null, null);
    }

    private void actualizar(int clienteId, byte[] contenido, String nombreArchivo) {
        String sql = "UPDATE clientes SET ficha_medica = ?, nombre_ficha_medica = ? WHERE id = ?";
        try (Connection c = base.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBytes(1, contenido);
            ps.setString(2, nombreArchivo);
            ps.setInt(3, clienteId);
            if (ps.executeUpdate() == 0) throw new EntidadNoEncontradaException("Cliente", clienteId);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar la ficha médica.", e);
        }
    }
}
