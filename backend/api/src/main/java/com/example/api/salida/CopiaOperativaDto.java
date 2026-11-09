package com.example.api.salida;

import java.time.LocalDate;
import java.util.List;

import com.example.core.cliente.Cliente;
import com.example.core.itinerario.Checkpoint;
import com.example.core.ruta.Ruta;
import com.example.core.salida.CopiaOperativa;

/** Representación JSON de la Copia operativa de una Salida. Reutilizable por los endpoints de escritura. */
public record CopiaOperativaDto(LocalDate fecha, RutaCopiaDto ruta, List<CheckpointDto> checkpoints,
        List<LineaEquipamientoDto> equipamiento, List<ParticipanteDto> participantes) {

    public static CopiaOperativaDto de(CopiaOperativa c) {
        return new CopiaOperativaDto(c.fecha(), RutaCopiaDto.de(c.ruta()),
                c.checkpoints().stream().map(CheckpointDto::de).toList(),
                c.equipamiento().stream().map(LineaEquipamientoDto::de).toList(),
                c.participantes().stream().map(ParticipanteDto::de).toList());
    }

    public record RutaCopiaDto(int id, String nombre, double altitudMaxima, String tipoTerreno,
            String dificultadTecnica, String dificultadFisica) {

        static RutaCopiaDto de(Ruta r) {
            return new RutaCopiaDto(r.id(), r.nombre(), r.altitudMaxima(), r.tipoTerreno(), r.dificultadTecnica(),
                    r.dificultadFisica());
        }
    }

    public record CheckpointDto(long id, String nombre, String hora, double latitud, double longitud,
            double altitud, String descripcion) {

        static CheckpointDto de(Checkpoint c) {
            return new CheckpointDto(c.id(), c.nombre(), c.hora(), c.latitud(), c.longitud(), c.altitud(),
                    c.descripcion());
        }
    }

    public record LineaEquipamientoDto(int elementoId, String nombre, String categoria, int cantidad) {

        static LineaEquipamientoDto de(CopiaOperativa.LineaEquipamiento l) {
            return new LineaEquipamientoDto(l.elemento().id(), l.elemento().nombre(), l.elemento().categoria(),
                    l.cantidad());
        }
    }

    public record ParticipanteDto(int id, String dni, String nombre, String apellido, LocalDate fechaNacimiento,
            String telefono) {

        static ParticipanteDto de(Cliente c) {
            return new ParticipanteDto(c.id(), c.dni(), c.nombre(), c.apellido(), c.fechaNacimiento(), c.telefono());
        }
    }
}
