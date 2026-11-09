package com.example.core.cliente;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.core.excepcion.EntidadNoEncontradaException;

class CasosDeUsoFichasMedicasTest {

    private AlmacenFichasMedicasEnMemoria almacen;
    private CasosDeUsoFichasMedicas fichas;
    private int clienteId;

    @BeforeEach
    void preparar() {
        RepositorioClientesEnMemoria repositorio = new RepositorioClientesEnMemoria();
        almacen = new AlmacenFichasMedicasEnMemoria();
        fichas = new CasosDeUsoFichasMedicas(repositorio, almacen);
        clienteId = repositorio.insertar(new DatosCliente("30123456", "Ana", "Pérez", LocalDate.of(1990, 5, 20),
                null, "3511234567", "Femenino", null, null, null, false, null, null, null, null),
                LocalDate.of(2026, 11, 4));
    }

    @Test
    void adjuntaYConsultaLaFicha() {
        fichas.adjuntar(clienteId, "ana.pdf", new byte[] { 1, 2, 3 });

        FichaMedica ficha = fichas.consultar(clienteId).orElseThrow();
        assertEquals("ana.pdf", ficha.nombreArchivo());
        assertArrayEquals(new byte[] { 1, 2, 3 }, ficha.contenido());
    }

    @Test
    void reemplazaLaFichaAnterior() {
        fichas.adjuntar(clienteId, "vieja.pdf", new byte[] { 1 });
        fichas.adjuntar(clienteId, "nueva.pdf", new byte[] { 9, 9 });

        FichaMedica ficha = fichas.consultar(clienteId).orElseThrow();
        assertEquals("nueva.pdf", ficha.nombreArchivo());
        assertArrayEquals(new byte[] { 9, 9 }, ficha.contenido());
    }

    @Test
    void sinFichaConsultaVacio() {
        assertTrue(fichas.consultar(clienteId).isEmpty());
    }

    @Test
    void quitaLaFicha() {
        fichas.adjuntar(clienteId, "ana.pdf", new byte[] { 1 });
        fichas.quitar(clienteId);

        assertTrue(fichas.consultar(clienteId).isEmpty());
    }

    @Test
    void clienteInexistenteNoAdmiteFicha() {
        assertThrows(EntidadNoEncontradaException.class, () -> fichas.adjuntar(99, "x.pdf", new byte[] { 1 }));
        assertThrows(EntidadNoEncontradaException.class, () -> fichas.consultar(99));
        assertThrows(EntidadNoEncontradaException.class, () -> fichas.quitar(99));
        assertTrue(almacen.buscar(99).isEmpty());
    }
}
