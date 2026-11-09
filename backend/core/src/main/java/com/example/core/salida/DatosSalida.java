package com.example.core.salida;

import java.time.LocalDate;
import java.util.List;

import com.example.core.cliente.DatosCliente;
import com.example.core.cliente.FichaMedica;
import com.example.core.inventario.DatosElemento;

/**
 * Todo lo que el Asistente de Salida reúne para confirmar una Salida. Los Checkpoints no se envían:
 * la Salida copia el Itinerario vigente de la Ruta.
 *
 * @param equipamiento      elementos existentes y cantidad a reservar de cada uno
 * @param elementosNuevos   elementos que se dan de alta junto con la Salida
 * @param clientesExistentes ids de Clientes ya registrados
 * @param clientesNuevos    Clientes que se dan de alta junto con la Salida
 */
public record DatosSalida(LocalDate fecha, int rutaId, List<LineaEquipamiento> equipamiento,
        List<ElementoNuevo> elementosNuevos, List<Integer> clientesExistentes, List<ClienteNuevo> clientesNuevos) {

    public record LineaEquipamiento(int elementoId, int cantidad) {
    }

    public record ElementoNuevo(DatosElemento elemento, int cantidad) {
    }

    /** Cliente a registrar; la ficha médica es opcional. */
    public record ClienteNuevo(DatosCliente cliente, FichaMedica ficha) {
    }
}
