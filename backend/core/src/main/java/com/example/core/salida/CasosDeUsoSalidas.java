package com.example.core.salida;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.example.core.excepcion.EntidadNoEncontradaException;

/** Casos de uso de listado y cierre de Salidas: única puerta de entrada para cualquier presentación. */
public class CasosDeUsoSalidas {

    private final RepositorioSalidas repositorio;
    private final Clock reloj;

    public CasosDeUsoSalidas(RepositorioSalidas repositorio, Clock reloj) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    /** Salidas con su resumen operativo y la condición de vencida calculada con el reloj. */
    public List<SalidaListada> listar() {
        Map<Integer, ResumenOperativo> resumenes = repositorio.resumenes();
        LocalDate hoy = LocalDate.now(reloj);
        return repositorio.listar().stream()
                .map(s -> new SalidaListada(s, resumenes.getOrDefault(s.id(), ResumenOperativo.VACIO), vencida(s, hoy)))
                .toList();
    }

    public Salida obtener(int id) {
        return repositorio.buscarPorId(id).orElseThrow(() -> new EntidadNoEncontradaException("Salida", id));
    }

    public void marcarRealizada(int id) {
        cerrar(id, EstadoSalida.REALIZADA);
    }

    public void cancelar(int id) {
        cerrar(id, EstadoSalida.CANCELADA);
    }

    /** Salida vencida: planificada con fecha anterior a hoy. */
    private static boolean vencida(Salida salida, LocalDate hoy) {
        return salida.estado() == EstadoSalida.PLANIFICADA && salida.fecha().isBefore(hoy);
    }

    private void cerrar(int id, EstadoSalida nuevo) {
        Salida salida = obtener(id);
        if (salida.estado() != EstadoSalida.PLANIFICADA) throw new TransicionInvalidaException(salida.estado());
        if (!repositorio.cerrarSiPlanificada(id, nuevo)) {
            // Otra instancia la cerró entre la lectura y la escritura.
            throw new TransicionInvalidaException(obtener(id).estado());
        }
    }
}
