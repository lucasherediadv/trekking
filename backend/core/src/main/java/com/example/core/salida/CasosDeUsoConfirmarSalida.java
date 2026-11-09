package com.example.core.salida;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.core.cliente.CasosDeUsoClientes;
import com.example.core.cliente.CasosDeUsoFichasMedicas;
import com.example.core.cliente.Cliente;
import com.example.core.excepcion.ErrorCampo;
import com.example.core.excepcion.ValidacionException;
import com.example.core.inventario.CasosDeUsoInventario;
import com.example.core.inventario.DatosElemento;
import com.example.core.inventario.ElementoInventario;
import com.example.core.inventario.StockInsuficienteException;
import com.example.core.inventario.ValidadorElemento;
import com.example.core.itinerario.CasosDeUsoItinerario;
import com.example.core.itinerario.Checkpoint;
import com.example.core.ruta.CasosDeUsoRutas;
import com.example.core.ruta.Ruta;
import com.example.core.salida.DatosSalida.ClienteNuevo;
import com.example.core.salida.DatosSalida.ElementoNuevo;
import com.example.core.salida.DatosSalida.LineaEquipamiento;
import com.example.core.transaccion.Transaccion;

/**
 * Confirmación de una Salida desde el Asistente: valida todos los datos, la Reserva de stock incluida,
 * y guarda la Copia operativa en una única transacción (ADR 0001, ADR 0003).
 */
public class CasosDeUsoConfirmarSalida {

    private final Transaccion transaccion;
    private final RepositorioSalidas salidas;
    private final CasosDeUsoRutas rutas;
    private final CasosDeUsoItinerario itinerarios;
    private final CasosDeUsoInventario inventario;
    private final CasosDeUsoClientes clientes;
    private final CasosDeUsoFichasMedicas fichas;

    public CasosDeUsoConfirmarSalida(Transaccion transaccion, RepositorioSalidas salidas, CasosDeUsoRutas rutas,
            CasosDeUsoItinerario itinerarios, CasosDeUsoInventario inventario, CasosDeUsoClientes clientes,
            CasosDeUsoFichasMedicas fichas) {
        this.transaccion = Objects.requireNonNull(transaccion, "transaccion");
        this.salidas = Objects.requireNonNull(salidas, "salidas");
        this.rutas = Objects.requireNonNull(rutas, "rutas");
        this.itinerarios = Objects.requireNonNull(itinerarios, "itinerarios");
        this.inventario = Objects.requireNonNull(inventario, "inventario");
        this.clientes = Objects.requireNonNull(clientes, "clientes");
        this.fichas = Objects.requireNonNull(fichas, "fichas");
    }

    /**
     * @return la Salida planificada creada
     * @throws ValidacionException       fecha ausente, Ruta borrador o dada de baja, elemento no asignable
     *                                   por estado o por baja, o Cliente inválido, inactivo o repetido
     * @throws StockInsuficienteException la cantidad pedida supera el stock disponible de un elemento
     */
    public Salida confirmar(DatosSalida datos) {
        return transaccion.ejecutar(() -> {
            if (datos.fecha() == null) {
                throw error("fecha", "La fecha de la Salida es obligatoria.");
            }
            Ruta ruta = rutaUtilizable(datos.rutaId());
            List<Checkpoint> checkpoints = itinerarios.obtener(ruta.id()).checkpoints();
            for (Checkpoint cp : checkpoints) {
                if (cp.altitud() > ruta.altitudMaxima()) {
                    throw error("checkpoints", "El Checkpoint '" + cp.nombre()
                            + "' supera la altitud máxima de la Ruta.");
                }
            }
            List<CopiaOperativa.LineaEquipamiento> existentes = validarEquipamientoExistente(datos.equipamiento());
            validarElementosNuevos(datos.elementosNuevos());
            List<Cliente> participantes = new ArrayList<>(validarClientesExistentes(datos.clientesExistentes()));

            // Todo lo anterior solo lee: recién ahora se escribe.
            List<CopiaOperativa.LineaEquipamiento> equipamiento = new ArrayList<>(existentes);
            for (ElementoNuevo nuevo : datos.elementosNuevos()) {
                equipamiento.add(new CopiaOperativa.LineaEquipamiento(inventario.alta(nuevo.elemento()), nuevo.cantidad()));
            }
            for (ClienteNuevo nuevo : datos.clientesNuevos()) {
                Cliente cliente = clientes.alta(nuevo.cliente());
                if (nuevo.ficha() != null) {
                    fichas.adjuntar(cliente.id(), nuevo.ficha().nombreArchivo(), nuevo.ficha().contenido());
                }
                participantes.add(cliente);
            }
            int id = salidas.guardar(new CopiaOperativa(datos.fecha(), ruta, checkpoints, equipamiento, participantes));
            return salidas.buscarPorId(id).orElseThrow();
        });
    }

    private Ruta rutaUtilizable(int rutaId) {
        Ruta ruta = rutas.obtener(rutaId);
        if (!ruta.activa()) throw error("ruta", "La Ruta está dada de baja y no puede usarse en una Salida.");
        if (!ruta.puedeUsarseEnSalida()) {
            throw error("ruta", "La Ruta es borrador: necesita al menos " + Ruta.CHECKPOINTS_MINIMOS
                    + " Checkpoints para planificar una Salida.");
        }
        return ruta;
    }

    /** Valida estado, baja y stock de los elementos del catálogo; suma las líneas repetidas del mismo elemento. */
    private List<CopiaOperativa.LineaEquipamiento> validarEquipamientoExistente(List<LineaEquipamiento> lineas) {
        Map<Integer, Integer> pedido = new LinkedHashMap<>();
        for (LineaEquipamiento linea : lineas) {
            if (linea.cantidad() <= 0) throw error("equipamiento", "La cantidad asignada debe ser un entero positivo.");
            pedido.merge(linea.elementoId(), linea.cantidad(), Integer::sum);
        }
        Set<Integer> asignables = inventario.listarAsignables().stream()
                .map(ElementoInventario::id).collect(Collectors.toSet());
        List<CopiaOperativa.LineaEquipamiento> resultado = new ArrayList<>();
        pedido.forEach((id, cantidad) -> {
            ElementoInventario elemento = inventario.obtener(id);
            if (elemento.estado().impideAsignar()) {
                throw error("equipamiento", elemento.nombre() + " no puede asignarse: está "
                        + elemento.estado().etiqueta() + ".");
            }
            if (!asignables.contains(id)) {
                throw error("equipamiento", elemento.nombre() + " está dado de baja y no puede asignarse.");
            }
            if (cantidad > elemento.cantidadDisponible()) {
                throw new StockInsuficienteException(elemento.nombre(), elemento.cantidadDisponible());
            }
            resultado.add(new CopiaOperativa.LineaEquipamiento(elemento, cantidad));
        });
        return resultado;
    }

    private void validarElementosNuevos(List<ElementoNuevo> nuevos) {
        for (ElementoNuevo nuevo : nuevos) {
            DatosElemento datos = nuevo.elemento();
            List<ErrorCampo> errores = ValidadorElemento.validar(datos);
            if (!errores.isEmpty()) throw new ValidacionException(errores);
            if (nuevo.cantidad() <= 0) throw error("equipamiento", "La cantidad asignada debe ser un entero positivo.");
            if (datos.estado().impideAsignar()) {
                throw error("equipamiento", datos.nombre() + " no puede asignarse: está " + datos.estado().etiqueta() + ".");
            }
            if (nuevo.cantidad() > datos.cantidad()) throw new StockInsuficienteException(datos.nombre(), datos.cantidad());
        }
    }

    private List<Cliente> validarClientesExistentes(List<Integer> ids) {
        Set<Integer> vistos = new HashSet<>();
        List<Cliente> resultado = new ArrayList<>();
        for (int id : ids) {
            if (!vistos.add(id)) throw error("participantes", "No se puede asociar dos veces el mismo Cliente.");
            Cliente cliente = clientes.obtener(id);
            if (!cliente.activo()) {
                throw error("participantes", "El Cliente " + cliente.apellido() + ", " + cliente.nombre()
                        + " está dado de baja. Reactívalo desde Clientes antes de asociarlo.");
            }
            resultado.add(cliente);
        }
        return resultado;
    }

    private static ValidacionException error(String campo, String mensaje) {
        return new ValidacionException(List.of(new ErrorCampo(campo, mensaje)));
    }
}
