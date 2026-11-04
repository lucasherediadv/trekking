package com.example.desktop.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.example.desktop.database.ConexionDB;
import com.example.desktop.model.FichaMedica;

/** Acceso transitorio a la ficha médica del Cliente; el ticket 04 lo reemplaza por el almacén de core. */
public class FichaMedicaDAO {

    public FichaMedica obtener(int clienteId) {
        String sql = "SELECT ficha_medica, nombre_ficha_medica FROM clientes WHERE id = ?";
        try (Connection conexion = ConexionDB.conectar(); PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getBytes("ficha_medica") != null) {
                    return new FichaMedica(rs.getBytes("ficha_medica"), rs.getString("nombre_ficha_medica"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener la ficha médica: " + e.getMessage());
        }
        return null;
    }

    public void guardar(int clienteId, byte[] datos, String nombreArchivo) {
        actualizar(clienteId, datos, nombreArchivo);
    }

    public void borrar(int clienteId) {
        actualizar(clienteId, null, null);
    }

    private void actualizar(int clienteId, byte[] datos, String nombreArchivo) {
        String sql = "UPDATE clientes SET ficha_medica = ?, nombre_ficha_medica = ? WHERE id = ?";
        try (Connection conexion = ConexionDB.conectar(); PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setBytes(1, datos);
            ps.setString(2, nombreArchivo);
            ps.setInt(3, clienteId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar la ficha médica.", e);
        }
    }
}
