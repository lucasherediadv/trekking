package com.example.core.cliente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.core.excepcion.EntidadNoEncontradaException;
import com.example.core.excepcion.ErrorCampo;
import com.example.core.excepcion.ValidacionException;

class CasosDeUsoClientesTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 4);

    private RepositorioClientesEnMemoria repositorio;
    private CasosDeUsoClientes clientes;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioClientesEnMemoria();
        Clock reloj = Clock.fixed(HOY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        clientes = new CasosDeUsoClientes(repositorio, reloj);
    }

    private static DatosCliente adulto() {
        return new DatosCliente("30123456", "Ana", "Pérez", LocalDate.of(1990, 5, 20), "ana@mail.com",
                "3511234567", "Femenino", "Luis", "3517654321", "Hermano", false, "", "", "", "");
    }

    private static DatosCliente conNacimiento(LocalDate nacimiento, boolean autorizacion, String tutorNombre,
            String tutorApellido, String tutorDni, String tutorTelefono) {
        DatosCliente a = adulto();
        return new DatosCliente("40123456", a.nombre(), a.apellido(), nacimiento, a.email(), a.telefono(),
                a.sexo(), a.contactoEmergenciaNombre(), a.contactoEmergenciaTelefono(),
                a.contactoEmergenciaRelacion(), autorizacion, tutorNombre, tutorApellido, tutorDni,
                tutorTelefono);
    }

    private static List<String> campos(ValidacionException e) {
        return e.errores().stream().map(ErrorCampo::campo).toList();
    }

    @Test
    void altaDeClienteValidoLoDevuelveActivoConFechas() {
        Cliente c = clientes.alta(adulto());
        assertTrue(c.id() > 0);
        assertTrue(c.activo());
        assertEquals("Pérez", c.apellido());
        assertEquals(HOY, c.fechaAlta());
        assertEquals(HOY, c.fechaModificacion());
        assertEquals("Luis", c.contactoEmergenciaNombre());
    }

    @Test
    void altaNormalizaEspaciosYVaciosAOpcionalesNulos() {
        DatosCliente a = adulto();
        Cliente c = clientes.alta(new DatosCliente(" 30123456 ", " Ana ", "Pérez", a.fechaNacimiento(), "",
                "3511234567", "Femenino", "", "", "", false, "", "", "", ""));
        assertEquals("30123456", c.dni());
        assertEquals("Ana", c.nombre());
        assertNull(c.email());
        assertNull(c.contactoEmergenciaNombre());
    }

    @Test
    void dniDuplicadoEsRechazadoEnElCampoDni() {
        clientes.alta(adulto());
        ValidacionException e = assertThrows(ValidacionException.class, () -> clientes.alta(adulto()));
        assertEquals(List.of("dni"), campos(e));
    }

    @Test
    void dniInvalidoEsRechazado() {
        DatosCliente a = adulto();
        DatosCliente corto = new DatosCliente("123", a.nombre(), a.apellido(), a.fechaNacimiento(), a.email(),
                a.telefono(), a.sexo(), "", "", "", false, "", "", "", "");
        ValidacionException e = assertThrows(ValidacionException.class, () -> clientes.alta(corto));
        assertEquals(List.of("dni"), campos(e));
    }

    @Test
    void fechaDeNacimientoFuturaEsRechazada() {
        DatosCliente futuro = conNacimiento(HOY.plusDays(1), true, "Mario", "Pérez", "20123456", "3510000000");
        ValidacionException e = assertThrows(ValidacionException.class, () -> clientes.alta(futuro));
        assertEquals(List.of("fechaNacimiento"), campos(e));
    }

    @Test
    void menorSinTutorEsRechazado() {
        DatosCliente menor = conNacimiento(HOY.minusYears(10), true, "", "", "", "");
        ValidacionException e = assertThrows(ValidacionException.class, () -> clientes.alta(menor));
        assertEquals(List.of("tutorNombre", "tutorApellido", "tutorDni", "tutorTelefono"), campos(e));
    }

    @Test
    void menorSinAutorizacionEsRechazado() {
        DatosCliente menor = conNacimiento(HOY.minusYears(10), false, "Mario", "Pérez", "20123456", "3510000000");
        ValidacionException e = assertThrows(ValidacionException.class, () -> clientes.alta(menor));
        assertEquals(List.of("autorizacionMenores"), campos(e));
    }

    @Test
    void menorConTutorYAutorizacionEsAceptado() {
        DatosCliente menor = conNacimiento(HOY.minusYears(10), true, "Mario", "Pérez", "20123456", "3510000000");
        Cliente c = clientes.alta(menor);
        assertEquals("Mario", c.tutorNombre());
        assertTrue(c.autorizacionMenores());
    }

    @Test
    void cumpleanosDe18EnLaFechaDelRelojYaNoEsMenor() {
        DatosCliente justoMayor = conNacimiento(HOY.minusYears(18), false, "", "", "", "");
        assertFalse(clientes.esMenor(justoMayor.fechaNacimiento()));
        clientes.alta(justoMayor);
    }

    @Test
    void undiaAntesDeCumplir18SigueSiendoMenor() {
        assertTrue(clientes.esMenor(HOY.minusYears(18).plusDays(1)));
    }

    @Test
    void contactoDeEmergenciaIncompletoEsRechazado() {
        DatosCliente a = adulto();
        DatosCliente incompleto = new DatosCliente("30999999", a.nombre(), a.apellido(), a.fechaNacimiento(),
                a.email(), a.telefono(), a.sexo(), "Luis", "", "", false, "", "", "", "");
        ValidacionException e = assertThrows(ValidacionException.class, () -> clientes.alta(incompleto));
        assertEquals(List.of("contactoEmergenciaTelefono", "contactoEmergenciaRelacion"), campos(e));
    }

    @Test
    void validarDevuelveErroresPorCampoSinGuardar() {
        DatosCliente vacio = new DatosCliente("", "", "", null, "", "", null, "", "", "", false, "", "", "", "");
        List<String> campos = clientes.validar(vacio).stream().map(ErrorCampo::campo).toList();
        assertEquals(List.of("dni", "nombre", "apellido", "fechaNacimiento", "telefono", "sexo"), campos);
        assertTrue(clientes.listar().isEmpty());
    }

    @Test
    void editarActualizaYPermiteConservarElPropioDni() {
        Cliente c = clientes.alta(adulto());
        DatosCliente a = adulto();
        Cliente editado = clientes.editar(c.id(), new DatosCliente(a.dni(), "Anita", a.apellido(),
                a.fechaNacimiento(), a.email(), a.telefono(), a.sexo(), "", "", "", false, "", "", "", ""));
        assertEquals("Anita", editado.nombre());
        assertNull(editado.contactoEmergenciaNombre());
    }

    @Test
    void editarNoPuedeUsarElDniDeOtroCliente() {
        clientes.alta(adulto());
        DatosCliente a = adulto();
        Cliente otro = clientes.alta(new DatosCliente("31000000", "Beto", "Gómez", a.fechaNacimiento(), "",
                a.telefono(), "Masculino", "", "", "", false, "", "", "", ""));
        ValidacionException e = assertThrows(ValidacionException.class, () -> clientes.editar(otro.id(), adulto()));
        assertEquals(List.of("dni"), campos(e));
    }

    @Test
    void editarClienteInexistenteFalla() {
        assertThrows(EntidadNoEncontradaException.class, () -> clientes.editar(99, adulto()));
    }

    @Test
    void bajaYReactivacion() {
        Cliente c = clientes.alta(adulto());
        clientes.darDeBaja(c.id());
        assertFalse(clientes.obtener(c.id()).activo());
        clientes.reactivar(c.id());
        assertTrue(clientes.obtener(c.id()).activo());
    }

    @Test
    void bajaDeClienteInexistenteFalla() {
        assertThrows(EntidadNoEncontradaException.class, () -> clientes.darDeBaja(99));
        assertThrows(EntidadNoEncontradaException.class, () -> clientes.reactivar(99));
    }

    @Test
    void listarDevuelveActivosEInactivos() {
        Cliente c = clientes.alta(adulto());
        clientes.darDeBaja(c.id());
        assertEquals(1, clientes.listar().size());
    }
}
