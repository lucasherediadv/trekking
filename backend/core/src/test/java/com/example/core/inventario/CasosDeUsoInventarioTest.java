package com.example.core.inventario;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import com.example.core.excepcion.EntidadNoEncontradaException;
import com.example.core.excepcion.ErrorCampo;
import com.example.core.excepcion.ValidacionException;

import org.junit.jupiter.api.Test;

class CasosDeUsoInventarioTest {

    private final RepositorioInventarioEnMemoria repositorio = new RepositorioInventarioEnMemoria();
    private final CasosDeUsoInventario inventario = new CasosDeUsoInventario(repositorio);

    @Test
    void altaValidaDevuelveElElementoActivoYSinReserva() {
        ElementoInventario creado = inventario.alta(
                new DatosElemento("Carpa", "Campamento", 5, EstadoElemento.DISPONIBLE));

        assertEquals("Carpa", creado.nombre());
        assertEquals(5, creado.cantidadDisponible());
        assertEquals(List.of(creado), inventario.listarActivos());
    }

    @Test
    void altaInvalidaInformaElErrorDeCadaCampoYNoGuarda() {
        ValidacionException e = assertThrows(ValidacionException.class,
                () -> inventario.alta(new DatosElemento("  ", "Inexistente", -1, null)));

        assertEquals(List.of("nombre", "categoria", "cantidad", "estado"),
                e.errores().stream().map(ErrorCampo::campo).toList());
        assertEquals(List.of(), inventario.listarActivos());
    }

    private ElementoInventario carpa() {
        return inventario.alta(new DatosElemento("Carpa", "Campamento", 5, EstadoElemento.DISPONIBLE));
    }

    @Test
    void edicionActualizaLosDatos() {
        ElementoInventario carpa = carpa();

        ElementoInventario editada = inventario.editar(carpa.id(),
                new DatosElemento("Carpa 4p", "Campamento", 8, EstadoElemento.EN_INSPECCION));

        assertEquals("Carpa 4p", editada.nombre());
        assertEquals(8, editada.cantidad());
        assertEquals(EstadoElemento.EN_INSPECCION, inventario.obtener(carpa.id()).estado());
    }

    @Test
    void edicionRechazaUnaCantidadMenorQueLaReservada() {
        ElementoInventario carpa = carpa();
        repositorio.reservar(carpa.id(), 4);

        ValidacionException e = assertThrows(ValidacionException.class, () -> inventario.editar(carpa.id(),
                new DatosElemento("Carpa", "Campamento", 3, EstadoElemento.DISPONIBLE)));

        assertEquals("cantidad", e.errores().get(0).campo());
        assertEquals(5, inventario.obtener(carpa.id()).cantidad());
        assertEquals(4, inventario.obtener(carpa.id()).cantidadReservada());
    }

    @Test
    void edicionInvalidaInformaErroresPorCampo() {
        ElementoInventario carpa = carpa();

        ValidacionException e = assertThrows(ValidacionException.class, () -> inventario.editar(carpa.id(),
                new DatosElemento("", "Campamento", 5, EstadoElemento.DISPONIBLE)));

        assertEquals(List.of("nombre"), e.errores().stream().map(ErrorCampo::campo).toList());
    }

    @Test
    void editarUnElementoInexistenteLanzaEntidadNoEncontrada() {
        assertThrows(EntidadNoEncontradaException.class, () -> inventario.editar(99,
                new DatosElemento("Carpa", "Campamento", 5, EstadoElemento.DISPONIBLE)));
    }

    @Test
    void bajaLoSacaDelListadoActivoYReactivarLoDevuelve() {
        ElementoInventario carpa = carpa();

        inventario.darDeBaja(carpa.id());
        assertEquals(List.of(), inventario.listarActivos());
        assertEquals(List.of(carpa.id()), inventario.listarDadosDeBaja().stream().map(ElementoInventario::id).toList());

        inventario.reactivar(carpa.id());
        assertEquals(List.of(carpa.id()), inventario.listarActivos().stream().map(ElementoInventario::id).toList());
        assertEquals(List.of(), inventario.listarDadosDeBaja());
    }

    @Test
    void bajaYReactivacionDeUnElementoInexistenteLanzanEntidadNoEncontrada() {
        assertThrows(EntidadNoEncontradaException.class, () -> inventario.darDeBaja(99));
        assertThrows(EntidadNoEncontradaException.class, () -> inventario.reactivar(99));
    }

    @Test
    void soloLosEstadosQueImpidenAsignarQuedanFueraDeLosAsignables() {
        for (EstadoElemento estado : EstadoElemento.values()) {
            inventario.alta(new DatosElemento(estado.etiqueta(), "Seguridad", 1, estado));
        }

        List<String> asignables = inventario.listarAsignables().stream().map(ElementoInventario::nombre).toList();

        assertEquals(List.of("Disponible", "En Inspección"), asignables);
    }

    @Test
    void unElementoDadoDeBajaNoEsAsignable() {
        ElementoInventario carpa = carpa();
        inventario.darDeBaja(carpa.id());

        assertEquals(List.of(), inventario.listarAsignables());
    }
}
