package com.example.core.ruta;

import java.util.List;
import java.util.Optional;

/** Contrato de persistencia de las Rutas. Lo implementa cada motor de base. */
public interface RepositorioRutas {

    /** Inserta una Ruta activa y devuelve su id. */
    int insertar(DatosRuta datos);

    void actualizar(int id, DatosRuta datos);

    void cambiarActiva(int id, boolean activa);

    Optional<Ruta> buscarPorId(int id);

    /** Indica si otra Ruta (distinta de {@code idExcluido}) ya usa ese nombre, según {@link NombreRuta#clave}. */
    boolean existeNombre(String nombre, int idExcluido);

    List<Ruta> listarActivas();

    List<Ruta> listarDadasDeBaja();

    List<Ruta> listarTodas();
}
