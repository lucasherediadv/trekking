package com.example.core.ruta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** Repositorio falso para tests de casos de uso. */
public class RepositorioRutasEnMemoria implements RepositorioRutas {

    private record Fila(DatosRuta datos, boolean activa, int checkpoints) {
    }

    private final Map<Integer, Fila> filas = new TreeMap<>();
    private int siguienteId = 1;

    /** Simula los Checkpoints guardados en el Itinerario de la Ruta. */
    public void fijarCheckpoints(int id, int cantidad) {
        Fila f = filas.get(id);
        filas.put(id, new Fila(f.datos, f.activa, cantidad));
    }

    @Override
    public int insertar(DatosRuta datos) {
        int id = siguienteId++;
        filas.put(id, new Fila(datos, true, 0));
        return id;
    }

    @Override
    public void actualizar(int id, DatosRuta datos) {
        Fila f = filas.get(id);
        filas.put(id, new Fila(datos, f.activa, f.checkpoints));
    }

    @Override
    public void cambiarActiva(int id, boolean activa) {
        Fila f = filas.get(id);
        filas.put(id, new Fila(f.datos, activa, f.checkpoints));
    }

    @Override
    public Optional<Ruta> buscarPorId(int id) {
        return Optional.ofNullable(filas.get(id)).map(f -> aRuta(id, f));
    }

    @Override
    public boolean existeNombre(String nombre, int idExcluido) {
        return filas.entrySet().stream().anyMatch(e -> e.getKey() != idExcluido
                && NombreRuta.clave(e.getValue().datos.nombre()).equals(NombreRuta.clave(nombre)));
    }

    @Override
    public List<Ruta> listarActivas() {
        return listar(true);
    }

    @Override
    public List<Ruta> listarDadasDeBaja() {
        return listar(false);
    }

    @Override
    public List<Ruta> listarTodas() {
        List<Ruta> resultado = new ArrayList<>();
        filas.forEach((id, f) -> resultado.add(aRuta(id, f)));
        return resultado;
    }

    private List<Ruta> listar(boolean activa) {
        return listarTodas().stream().filter(r -> r.activa() == activa).toList();
    }

    private static Ruta aRuta(int id, Fila f) {
        DatosRuta d = f.datos;
        return new Ruta(id, d.nombre(), d.altitudMaxima(), d.tipoTerreno(), d.dificultadTecnica(),
                d.dificultadFisica(), f.activa, f.checkpoints);
    }
}
