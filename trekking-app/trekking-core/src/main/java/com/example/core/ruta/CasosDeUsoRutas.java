package com.example.core.ruta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.example.core.excepcion.EntidadNoEncontradaException;
import com.example.core.excepcion.ErrorCampo;
import com.example.core.excepcion.ValidacionException;

/** Casos de uso de las Rutas: única puerta de entrada para cualquier presentación. */
public class CasosDeUsoRutas {

    private final RepositorioRutas repositorio;

    public CasosDeUsoRutas(RepositorioRutas repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
    }

    public Ruta alta(DatosRuta datos) {
        DatosRuta normalizados = validar(datos, -1);
        int id = repositorio.insertar(normalizados);
        return repositorio.buscarPorId(id).orElseThrow();
    }

    public Ruta editar(int id, DatosRuta datos) {
        obtener(id);
        repositorio.actualizar(id, validar(datos, id));
        return obtener(id);
    }

    public void darDeBaja(int id) {
        obtener(id);
        repositorio.cambiarActiva(id, false);
    }

    public void reactivar(int id) {
        obtener(id);
        repositorio.cambiarActiva(id, true);
    }

    public Ruta obtener(int id) {
        return repositorio.buscarPorId(id).orElseThrow(() -> new EntidadNoEncontradaException("Ruta", id));
    }

    public List<Ruta> listarActivas() {
        return repositorio.listarActivas();
    }

    public List<Ruta> listarDadasDeBaja() {
        return repositorio.listarDadasDeBaja();
    }

    public List<Ruta> listarTodas() {
        return repositorio.listarTodas();
    }

    /** Valida los datos y la unicidad del nombre; devuelve los datos con el nombre normalizado. */
    private DatosRuta validar(DatosRuta datos, int idExcluido) {
        List<ErrorCampo> errores = new ArrayList<>(ValidadorRuta.validar(datos));
        String nombre = NombreRuta.normalizar(datos.nombre());
        if (errores.stream().noneMatch(e -> e.campo().equals("nombre"))
                && repositorio.existeNombre(nombre, idExcluido)) {
            errores.add(0, new ErrorCampo("nombre", "Ya existe una ruta con ese nombre."));
        }
        if (!errores.isEmpty()) throw new ValidacionException(errores);
        return new DatosRuta(nombre, datos.altitudMaxima(), datos.tipoTerreno(), datos.dificultadTecnica(),
                datos.dificultadFisica());
    }
}
