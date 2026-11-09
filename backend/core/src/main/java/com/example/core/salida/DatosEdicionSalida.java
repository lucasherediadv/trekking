package com.example.core.salida;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.example.core.itinerario.Checkpoint;
import com.example.core.salida.DatosSalida.LineaEquipamiento;

/**
 * Todo lo que el formulario de edición envía para reemplazar la Copia operativa de una Salida.
 *
 * @param actualizarDesdeRuta     recopia los datos de la Ruta del catálogo en lugar de conservar los copiados
 * @param checkpoints             Checkpoints de la Copia operativa; cualquier cantidad
 * @param checkpointsNuevosParaRuta Checkpoints nuevos que además se guardan en la Ruta activa (ADR 0002)
 * @param equipamiento            elementos del catálogo y cantidad a reservar de cada uno
 * @param participantes           ids de Clientes ya registrados
 * @param refrescarEquipamiento   elementos cuya copia se vuelve a tomar del catálogo (quitados y agregados otra vez)
 * @param refrescarParticipantes  Clientes cuya copia se vuelve a tomar del catálogo
 */
public record DatosEdicionSalida(LocalDate fecha, int rutaId, boolean actualizarDesdeRuta,
        List<Checkpoint> checkpoints, List<Checkpoint> checkpointsNuevosParaRuta,
        List<LineaEquipamiento> equipamiento, List<Integer> participantes,
        Set<Integer> refrescarEquipamiento, Set<Integer> refrescarParticipantes) {
}
