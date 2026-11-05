package com.example.api.salida;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.example.core.cliente.DatosCliente;
import com.example.core.cliente.FichaMedica;
import com.example.core.excepcion.ErrorCampo;
import com.example.core.excepcion.ValidacionException;
import com.example.core.inventario.DatosElemento;
import com.example.core.salida.DatosSalida;

/**
 * Cuerpo de {@code POST /api/salidas}: lo que reúne el Asistente de Salida. No lleva Checkpoints (la Salida
 * copia el Itinerario vigente). Las listas ausentes se toman como vacías; las reglas de negocio las valida core.
 */
public record SolicitudSalidaDto(LocalDate fecha, Integer rutaId, List<LineaEquipamientoSolicitud> equipamiento,
        List<ElementoNuevoSolicitud> elementosNuevos, List<Integer> clientesExistentes,
        List<ClienteNuevoSolicitud> clientesNuevos) {

    public record LineaEquipamientoSolicitud(int elementoId, int cantidad) {
    }

    public record ElementoNuevoSolicitud(DatosElemento elemento, int cantidad) {
    }

    public record ClienteNuevoSolicitud(DatosCliente cliente, FichaSolicitud ficha) {
    }

    /** Ficha médica como nombre de archivo y contenido en base64. */
    public record FichaSolicitud(String nombreArchivo, String contenidoBase64) {
    }

    /**
     * Traduce a los datos de core.
     *
     * @param maxFicha tamaño máximo en bytes de cada ficha decodificada
     * @throws ValidacionException si faltan datos obligatorios del formato o una ficha no es base64 válido
     */
    public DatosSalida aDatosSalida(long maxFicha) {
        List<ErrorCampo> errores = new ArrayList<>();
        if (fecha == null) errores.add(new ErrorCampo("fecha", "La fecha de la Salida es obligatoria."));
        if (rutaId == null) errores.add(new ErrorCampo("rutaId", "La Ruta es obligatoria."));

        var lineas = new ArrayList<DatosSalida.LineaEquipamiento>();
        for (var l : orVacia(equipamiento)) lineas.add(new DatosSalida.LineaEquipamiento(l.elementoId(), l.cantidad()));

        var nuevosElementos = new ArrayList<DatosSalida.ElementoNuevo>();
        var elementos = orVacia(elementosNuevos);
        for (int i = 0; i < elementos.size(); i++) {
            var e = elementos.get(i);
            if (e == null || e.elemento() == null) {
                errores.add(new ErrorCampo("elementosNuevos[" + i + "].elemento", "El elemento es obligatorio."));
            } else {
                nuevosElementos.add(new DatosSalida.ElementoNuevo(e.elemento(), e.cantidad()));
            }
        }

        var existentes = new ArrayList<Integer>();
        for (Integer id : orVacia(clientesExistentes)) {
            if (id == null) errores.add(new ErrorCampo("clientesExistentes", "El id de Cliente es obligatorio."));
            else existentes.add(id);
        }

        var nuevosClientes = new ArrayList<DatosSalida.ClienteNuevo>();
        var clientes = orVacia(clientesNuevos);
        for (int i = 0; i < clientes.size(); i++) {
            var c = clientes.get(i);
            String campo = "clientesNuevos[" + i + "]";
            if (c == null || c.cliente() == null) {
                errores.add(new ErrorCampo(campo + ".cliente", "El Cliente es obligatorio."));
                continue;
            }
            nuevosClientes.add(new DatosSalida.ClienteNuevo(c.cliente(),
                    decodificar(c.ficha(), campo + ".ficha", maxFicha, errores)));
        }

        if (!errores.isEmpty()) throw new ValidacionException(errores);
        return new DatosSalida(fecha, rutaId, lineas, nuevosElementos, existentes, nuevosClientes);
    }

    private static FichaMedica decodificar(FichaSolicitud ficha, String campo, long maxFicha, List<ErrorCampo> errores) {
        if (ficha == null) return null;
        if (ficha.nombreArchivo() == null || ficha.nombreArchivo().isBlank() || ficha.contenidoBase64() == null) {
            errores.add(new ErrorCampo(campo, "La ficha médica necesita nombre de archivo y contenido."));
            return null;
        }
        try {
            byte[] contenido = Base64.getDecoder().decode(ficha.contenidoBase64());
            if (contenido.length > maxFicha) throw new MaxUploadSizeExceededException(maxFicha);
            return new FichaMedica(ficha.nombreArchivo(), contenido);
        } catch (IllegalArgumentException e) {
            errores.add(new ErrorCampo(campo, "El contenido de la ficha médica no es base64 válido."));
            return null;
        }
    }

    private static <T> List<T> orVacia(List<T> lista) {
        return lista == null ? List.of() : lista;
    }
}
