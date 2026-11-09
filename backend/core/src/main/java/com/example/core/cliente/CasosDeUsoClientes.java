package com.example.core.cliente;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.example.core.excepcion.EntidadNoEncontradaException;
import com.example.core.excepcion.ErrorCampo;
import com.example.core.excepcion.ValidacionException;

/** Casos de uso de Clientes: única puerta de entrada para cualquier presentación. */
public class CasosDeUsoClientes {

    private final RepositorioClientes repositorio;
    private final Clock reloj;

    public CasosDeUsoClientes(RepositorioClientes repositorio, Clock reloj) {
        this.repositorio = Objects.requireNonNull(repositorio, "repositorio");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    public Cliente alta(DatosCliente datos) {
        DatosCliente normalizados = normalizar(datos);
        verificar(normalizados, null);
        int id = repositorio.insertar(normalizados, hoy());
        return obtener(id);
    }

    public Cliente editar(int id, DatosCliente datos) {
        obtener(id);
        DatosCliente normalizados = normalizar(datos);
        verificar(normalizados, id);
        repositorio.actualizar(id, normalizados, hoy());
        return obtener(id);
    }

    public void darDeBaja(int id) {
        obtener(id);
        repositorio.cambiarActivo(id, false, hoy());
    }

    public void reactivar(int id) {
        obtener(id);
        repositorio.cambiarActivo(id, true, hoy());
    }

    public Cliente obtener(int id) {
        return repositorio.buscarPorId(id).orElseThrow(() -> new EntidadNoEncontradaException("Cliente", id));
    }

    /** Todos los Clientes, activos e inactivos; la presentación filtra y busca. */
    public List<Cliente> listar() {
        return repositorio.listarTodos();
    }

    /** Reglas de formato y de menor de edad, sin efectos y sin consultar duplicados (para validar en vivo). */
    public List<ErrorCampo> validar(DatosCliente datos) {
        return ValidadorCliente.validar(normalizar(datos), hoy());
    }

    public boolean esMenor(LocalDate fechaNacimiento) {
        return ValidadorCliente.esMenor(fechaNacimiento, hoy());
    }

    private void verificar(DatosCliente datos, Integer idPropio) {
        List<ErrorCampo> errores = new ArrayList<>(ValidadorCliente.validar(datos, hoy()));
        boolean dniInvalido = errores.stream().anyMatch(e -> e.campo().equals("dni"));
        if (!dniInvalido && repositorio.buscarPorDni(datos.dni())
                .filter(existente -> idPropio == null || existente.id() != idPropio).isPresent()) {
            errores.add(0, new ErrorCampo("dni", "El DNI ya esta registrado."));
        }
        if (!errores.isEmpty()) throw new ValidacionException(errores);
    }

    private LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    /** Recorta espacios y deja en {@code null} los textos opcionales vacíos. */
    private static DatosCliente normalizar(DatosCliente d) {
        return new DatosCliente(requerido(d.dni()), requerido(d.nombre()), requerido(d.apellido()),
                d.fechaNacimiento(), opcional(d.email()), requerido(d.telefono()), opcional(d.sexo()),
                opcional(d.contactoEmergenciaNombre()), opcional(d.contactoEmergenciaTelefono()),
                opcional(d.contactoEmergenciaRelacion()), d.autorizacionMenores(), opcional(d.tutorNombre()),
                opcional(d.tutorApellido()), opcional(d.tutorDni()), opcional(d.tutorTelefono()));
    }

    private static String requerido(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static String opcional(String valor) {
        String t = requerido(valor);
        return t.isEmpty() ? null : t;
    }
}
