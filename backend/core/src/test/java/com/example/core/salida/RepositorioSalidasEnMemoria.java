package com.example.core.salida;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Repositorio falso para tests de casos de uso. */
public class RepositorioSalidasEnMemoria implements RepositorioSalidas {

    private final Map<Integer, Salida> salidas = new LinkedHashMap<>();
    private final Map<Integer, ResumenOperativo> resumenes = new LinkedHashMap<>();

    public void agregar(Salida salida) {
        salidas.put(salida.id(), salida);
    }

    public void agregar(Salida salida, ResumenOperativo resumen) {
        agregar(salida);
        resumenes.put(salida.id(), resumen);
    }

    @Override
    public List<Salida> listar() {
        return List.copyOf(salidas.values());
    }

    @Override
    public Map<Integer, ResumenOperativo> resumenes() {
        return resumenes;
    }

    @Override
    public Optional<Salida> buscarPorId(int id) {
        return Optional.ofNullable(salidas.get(id));
    }

    @Override
    public boolean cerrarSiPlanificada(int id, EstadoSalida nuevoEstado) {
        Salida s = salidas.get(id);
        if (s == null || s.estado() != EstadoSalida.PLANIFICADA) return false;
        salidas.put(id, new Salida(s.id(), s.rutaId(), s.fecha(), nuevoEstado, s.rutaNombre(),
                s.rutaAltitudMaxima(), s.rutaTipoTerreno(), s.rutaDificultadTecnica(), s.rutaDificultadFisica()));
        return true;
    }

    private final Map<Integer, CopiaOperativa> copias = new LinkedHashMap<>();

    public CopiaOperativa copia(int id) {
        return copias.get(id);
    }

    @Override
    public int guardar(CopiaOperativa copia) {
        int id = salidas.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
        copias.put(id, copia);
        agregar(new Salida(id, copia.ruta().id(), copia.fecha(), EstadoSalida.PLANIFICADA, copia.ruta().nombre(),
                copia.ruta().altitudMaxima(), copia.ruta().tipoTerreno(), copia.ruta().dificultadTecnica(),
                copia.ruta().dificultadFisica()));
        return id;
    }

    @Override
    public Optional<CopiaOperativa> buscarCopia(int id) {
        return Optional.ofNullable(copias.get(id));
    }

    @Override
    public void reemplazarCopia(int id, CopiaOperativa copia) {
        Salida s = salidas.get(id);
        copias.put(id, copia);
        salidas.put(id, new Salida(id, copia.ruta().id(), copia.fecha(), s.estado(), copia.ruta().nombre(),
                copia.ruta().altitudMaxima(), copia.ruta().tipoTerreno(), copia.ruta().dificultadTecnica(),
                copia.ruta().dificultadFisica()));
    }
}
