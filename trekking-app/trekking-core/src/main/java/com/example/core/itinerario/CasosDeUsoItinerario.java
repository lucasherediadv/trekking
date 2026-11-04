package com.example.core.itinerario;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.example.core.excepcion.EntidadNoEncontradaException;
import com.example.core.excepcion.ErrorCampo;
import com.example.core.excepcion.ValidacionException;
import com.example.core.ruta.RepositorioRutas;
import com.example.core.ruta.Ruta;

/** Casos de uso del Itinerario de una Ruta: única puerta de entrada para cualquier presentación. */
public class CasosDeUsoItinerario {

    private final RepositorioRutas rutas;
    private final RepositorioItinerarios itinerarios;

    public CasosDeUsoItinerario(RepositorioRutas rutas, RepositorioItinerarios itinerarios) {
        this.rutas = Objects.requireNonNull(rutas, "rutas");
        this.itinerarios = Objects.requireNonNull(itinerarios, "itinerarios");
    }

    public Itinerario obtener(int rutaId) {
        buscarRuta(rutaId);
        return Itinerario.de(itinerarios.listar(rutaId));
    }

    /**
     * Guarda el Itinerario completo de la Ruta, ordenado por hora. Se admiten cero o al menos dos
     * Checkpoints (ADR 0001) y ninguno puede superar la altitud máxima de la Ruta.
     *
     * @return la Ruta actualizada: con dos o más Checkpoints deja de ser borrador
     */
    public Ruta guardar(int rutaId, Itinerario itinerario) {
        Ruta ruta = buscarRuta(rutaId);
        List<ErrorCampo> errores = new ArrayList<>();
        if (itinerario.cantidad() == 1) {
            errores.add(new ErrorCampo("checkpoints",
                    "El Itinerario necesita inicio y fin. Agrega otro Checkpoint o quita el único punto."));
        }
        for (Checkpoint cp : itinerario.checkpoints()) {
            for (ErrorCampo e : ValidadorCheckpoint.validar(cp, ruta.altitudMaxima())) {
                errores.add(new ErrorCampo("checkpoints", "Checkpoint '" + cp.nombre() + "': " + e.mensaje()));
            }
        }
        if (!errores.isEmpty()) throw new ValidacionException(errores);

        List<Checkpoint> normalizados = itinerario.checkpoints().stream()
                .map(cp -> new Checkpoint(cp.id(), cp.nombre().trim(), cp.hora().trim(), cp.latitud(),
                        cp.longitud(), cp.altitud(), cp.descripcion() == null ? "" : cp.descripcion().trim()))
                .toList();
        itinerarios.reemplazar(rutaId, normalizados);
        return buscarRuta(rutaId);
    }

    private Ruta buscarRuta(int rutaId) {
        return rutas.buscarPorId(rutaId).orElseThrow(() -> new EntidadNoEncontradaException("Ruta", rutaId));
    }
}
