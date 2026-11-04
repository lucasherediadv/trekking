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
import com.example.core.cliente.Cliente;
import com.example.core.excepcion.EntidadNoEncontradaException;
import com.example.core.excepcion.ErrorCampo;
import com.example.core.excepcion.ValidacionException;
import com.example.core.inventario.CasosDeUsoInventario;
import com.example.core.inventario.ElementoInventario;
import com.example.core.inventario.StockInsuficienteException;
import com.example.core.itinerario.CasosDeUsoItinerario;
import com.example.core.itinerario.Checkpoint;
import com.example.core.itinerario.Itinerario;
import com.example.core.itinerario.ValidadorCheckpoint;
import com.example.core.ruta.CasosDeUsoRutas;
import com.example.core.ruta.Ruta;
import com.example.core.salida.DatosSalida.LineaEquipamiento;
import com.example.core.transaccion.Transaccion;

/**
 * Edición de una Salida planificada: valida todo, la Reserva de stock incluida, y reemplaza la Copia
 * operativa vigente en una única transacción (ADR 0001, ADR 0003). Solo se puede guardar en la Ruta
 * un Checkpoint nuevo; los demás cambios de Checkpoints quedan en la Salida (ADR 0002).
 */
public class CasosDeUsoEditarSalida {

    private final Transaccion transaccion;
    private final RepositorioSalidas salidas;
    private final CasosDeUsoRutas rutas;
    private final CasosDeUsoItinerario itinerarios;
    private final CasosDeUsoInventario inventario;
    private final CasosDeUsoClientes clientes;

    public CasosDeUsoEditarSalida(Transaccion transaccion, RepositorioSalidas salidas, CasosDeUsoRutas rutas,
            CasosDeUsoItinerario itinerarios, CasosDeUsoInventario inventario, CasosDeUsoClientes clientes) {
        this.transaccion = Objects.requireNonNull(transaccion, "transaccion");
        this.salidas = Objects.requireNonNull(salidas, "salidas");
        this.rutas = Objects.requireNonNull(rutas, "rutas");
        this.itinerarios = Objects.requireNonNull(itinerarios, "itinerarios");
        this.inventario = Objects.requireNonNull(inventario, "inventario");
        this.clientes = Objects.requireNonNull(clientes, "clientes");
    }

    /** Copia operativa vigente, para abrir el formulario de edición. */
    public CopiaOperativa copia(int salidaId) {
        return salidas.buscarCopia(salidaId).orElseThrow(() -> new EntidadNoEncontradaException("Salida", salidaId));
    }

    /**
     * Elementos que el formulario puede ofrecer: los asignables y los ya asignados a la Salida. Su
     * Reserva no incluye la de esta Salida, que la edición reemplaza.
     */
    public List<ElementoInventario> elementosParaEditar(int salidaId) {
        Map<Integer, Integer> propias = cantidadesPorElemento(copia(salidaId));
        Map<Integer, ElementoInventario> elementos = new LinkedHashMap<>();
        inventario.listarAsignables().forEach(e -> elementos.put(e.id(), e));
        propias.keySet().forEach(id -> elementos.computeIfAbsent(id, inventario::obtener));
        return elementos.values().stream()
                .map(e -> new ElementoInventario(e.id(), e.nombre(), e.categoria(), e.cantidad(), e.estado(),
                        e.cantidadReservada() - propias.getOrDefault(e.id(), 0)))
                .sorted(java.util.Comparator.comparing(ElementoInventario::nombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * @return la Salida con su Copia operativa reemplazada
     * @throws SalidaNoEditableException  la Salida no está planificada
     * @throws ValidacionException        fecha ausente, Ruta no utilizable, Checkpoint inválido, elemento no
     *                                    asignable o Cliente inactivo o repetido
     * @throws StockInsuficienteException la cantidad pedida supera el stock disponible de un elemento
     */
    public Salida editar(int salidaId, DatosEdicionSalida datos) {
        return transaccion.ejecutar(() -> {
            Salida salida = salidas.buscarPorId(salidaId)
                    .orElseThrow(() -> new EntidadNoEncontradaException("Salida", salidaId));
            if (salida.estado() != EstadoSalida.PLANIFICADA) throw new SalidaNoEditableException(salida.estado());
            CopiaOperativa actual = copia(salidaId);
            if (datos.fecha() == null) throw error("fecha", "La fecha de la Salida es obligatoria.");

            Ruta catalogo = rutas.obtener(datos.rutaId());
            boolean mismaRuta = datos.rutaId() == salida.rutaId();
            if (!mismaRuta && !catalogo.puedeUsarseEnSalida()) {
                throw error("ruta", "La Ruta no puede usarse en una Salida: debe estar activa y tener al menos "
                        + Ruta.CHECKPOINTS_MINIMOS + " Checkpoints.");
            }
            Ruta rutaCopia = mismaRuta && !datos.actualizarDesdeRuta() ? actual.ruta() : catalogo;

            List<Checkpoint> checkpoints = Itinerario.de(datos.checkpoints()).checkpoints();
            validarCheckpoints(checkpoints, rutaCopia.altitudMaxima());
            Itinerario nuevoItinerario = itinerarioConNuevos(catalogo, datos.checkpointsNuevosParaRuta(), rutaCopia);

            List<CopiaOperativa.LineaEquipamiento> equipamiento = validarEquipamiento(actual, datos);
            List<Cliente> participantes = validarParticipantes(actual, datos);

            // Todo lo anterior solo lee: recién ahora se escribe.
            if (nuevoItinerario != null) itinerarios.guardar(catalogo.id(), nuevoItinerario);
            salidas.reemplazarCopia(salidaId,
                    new CopiaOperativa(datos.fecha(), rutaCopia, checkpoints, equipamiento, participantes));
            return salidas.buscarPorId(salidaId).orElseThrow();
        });
    }

    private static void validarCheckpoints(List<Checkpoint> checkpoints, double altitudMaxima) {
        List<ErrorCampo> errores = new ArrayList<>();
        for (Checkpoint cp : checkpoints) {
            for (ErrorCampo e : ValidadorCheckpoint.validar(cp, altitudMaxima)) {
                errores.add(new ErrorCampo("checkpoints", "Checkpoint '" + cp.nombre() + "': " + e.mensaje()));
            }
        }
        if (!errores.isEmpty()) throw new ValidacionException(errores);
    }

    /** Itinerario de la Ruta del catálogo con los Checkpoints nuevos agregados, o {@code null} si no hay. */
    private Itinerario itinerarioConNuevos(Ruta catalogo, List<Checkpoint> nuevos, Ruta rutaCopia) {
        if (nuevos.isEmpty()) return null;
        if (!catalogo.activa()) throw error("checkpoints", "La Ruta está dada de baja y no admite nuevos Checkpoints.");
        validarCheckpoints(nuevos, Math.min(rutaCopia.altitudMaxima(), catalogo.altitudMaxima()));
        Itinerario itinerario = itinerarios.obtener(catalogo.id());
        for (Checkpoint nuevo : nuevos) itinerario = itinerario.agregar(nuevo);
        return itinerario;
    }

    private List<CopiaOperativa.LineaEquipamiento> validarEquipamiento(CopiaOperativa actual, DatosEdicionSalida datos) {
        Map<Integer, CopiaOperativa.LineaEquipamiento> propias = actual.equipamiento().stream()
                .collect(Collectors.toMap(l -> l.elemento().id(), l -> l));
        Map<Integer, Integer> pedido = new LinkedHashMap<>();
        for (LineaEquipamiento linea : datos.equipamiento()) {
            if (linea.cantidad() <= 0) throw error("equipamiento", "La cantidad asignada debe ser un entero positivo.");
            pedido.merge(linea.elementoId(), linea.cantidad(), Integer::sum);
        }
        Set<Integer> asignables = inventario.listarAsignables().stream()
                .map(ElementoInventario::id).collect(Collectors.toSet());
        List<CopiaOperativa.LineaEquipamiento> resultado = new ArrayList<>();
        pedido.forEach((id, cantidad) -> {
            ElementoInventario elemento = inventario.obtener(id);
            CopiaOperativa.LineaEquipamiento propia = propias.get(id);
            boolean conservada = propia != null && !datos.refrescarEquipamiento().contains(id);
            if (!conservada) {
                if (elemento.estado().impideAsignar()) {
                    throw error("equipamiento", elemento.nombre() + " no puede asignarse: está "
                            + elemento.estado().etiqueta() + ".");
                }
                if (!asignables.contains(id)) {
                    throw error("equipamiento", elemento.nombre() + " está dado de baja y no puede asignarse.");
                }
            }
            // La Reserva de esta Salida se reemplaza: no cuenta contra la cantidad pedida.
            int reservadaPorOtras = elemento.cantidadReservada() - (propia == null ? 0 : propia.cantidad());
            int disponible = Math.max(0, elemento.cantidad() - reservadaPorOtras);
            if (cantidad > disponible) throw new StockInsuficienteException(elemento.nombre(), disponible);
            resultado.add(new CopiaOperativa.LineaEquipamiento(conservada ? propia.elemento() : elemento, cantidad));
        });
        return resultado;
    }

    private List<Cliente> validarParticipantes(CopiaOperativa actual, DatosEdicionSalida datos) {
        Map<Integer, Cliente> propios = actual.participantes().stream()
                .collect(Collectors.toMap(Cliente::id, c -> c));
        Set<Integer> vistos = new HashSet<>();
        List<Cliente> resultado = new ArrayList<>();
        for (int id : datos.participantes()) {
            if (!vistos.add(id)) throw error("participantes", "No se puede asociar dos veces el mismo Cliente.");
            Cliente propio = propios.get(id);
            if (propio != null && !datos.refrescarParticipantes().contains(id)) {
                resultado.add(propio); // conserva la copia y puede seguir aunque el Cliente se haya dado de baja
                continue;
            }
            Cliente cliente = clientes.obtener(id);
            if (!cliente.activo()) {
                throw error("participantes", "El Cliente " + cliente.apellido() + ", " + cliente.nombre()
                        + " está dado de baja. Reactívalo desde Clientes antes de asociarlo.");
            }
            resultado.add(cliente);
        }
        return resultado;
    }

    private static Map<Integer, Integer> cantidadesPorElemento(CopiaOperativa copia) {
        Map<Integer, Integer> cantidades = new LinkedHashMap<>();
        copia.equipamiento().forEach(l -> cantidades.merge(l.elemento().id(), l.cantidad(), Integer::sum));
        return cantidades;
    }

    private static ValidacionException error(String campo, String mensaje) {
        return new ValidacionException(List.of(new ErrorCampo(campo, mensaje)));
    }
}
