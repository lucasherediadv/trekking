package com.example.core.salida;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

import com.example.core.excepcion.EntidadNoEncontradaException;

/**
 * Genera la Ficha operativa de una Salida como PDF, a partir de su Copia operativa vigente. Core no
 * conoce el destino: cada presentación decide dónde guardar los bytes.
 */
public class CasosDeUsoFichaOperativa {

    private final RepositorioSalidas salidas;
    private final Clock reloj;
    private final GeneradorFichaOperativa generador = new GeneradorFichaOperativa();

    public CasosDeUsoFichaOperativa(RepositorioSalidas salidas, Clock reloj) {
        this.salidas = Objects.requireNonNull(salidas, "salidas");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    /**
     * @return los bytes del PDF, con la fecha de emisión tomada del reloj
     * @throws EntidadNoEncontradaException la Salida no existe
     */
    public byte[] generar(int salidaId) {
        Salida salida = salidas.buscarPorId(salidaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Salida", salidaId));
        CopiaOperativa copia = salidas.buscarCopia(salidaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Salida", salidaId));
        return generador.generar(salida, copia, LocalDateTime.now(reloj));
    }
}
