package com.example.sqlite;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.core.cliente.DatosCliente;
import com.example.core.cliente.FichaMedica;
import com.example.core.excepcion.EntidadNoEncontradaException;

class AlmacenFichasMedicasSqliteTest {

    @TempDir
    Path directorio;

    private AlmacenFichasMedicasSqlite almacen;
    private int clienteId;

    @BeforeEach
    void preparar() {
        BaseDatosSqlite base = new BaseDatosSqlite(directorio.resolve("fichas.db"));
        base.inicializarEsquema();
        almacen = new AlmacenFichasMedicasSqlite(base);
        clienteId = new RepositorioClientesSqlite(base).insertar(new DatosCliente("30123456", "Ana", "Pérez",
                LocalDate.of(1990, 5, 20), null, "3511234567", "Femenino", null, null, null, false, null, null,
                null, null), LocalDate.of(2026, 11, 4));
    }

    @Test
    void guardaYBuscaLaFicha() {
        almacen.guardar(clienteId, new FichaMedica("ana.pdf", new byte[] { 1, 2, 3 }));

        FichaMedica ficha = almacen.buscar(clienteId).orElseThrow();
        assertEquals("ana.pdf", ficha.nombreArchivo());
        assertArrayEquals(new byte[] { 1, 2, 3 }, ficha.contenido());
    }

    @Test
    void guardarReemplazaLaAnterior() {
        almacen.guardar(clienteId, new FichaMedica("vieja.pdf", new byte[] { 1 }));
        almacen.guardar(clienteId, new FichaMedica("nueva.pdf", new byte[] { 7, 8 }));

        FichaMedica ficha = almacen.buscar(clienteId).orElseThrow();
        assertEquals("nueva.pdf", ficha.nombreArchivo());
        assertArrayEquals(new byte[] { 7, 8 }, ficha.contenido());
    }

    @Test
    void quitarDejaAlClienteSinFicha() {
        almacen.guardar(clienteId, new FichaMedica("ana.pdf", new byte[] { 1 }));
        almacen.quitar(clienteId);

        assertTrue(almacen.buscar(clienteId).isEmpty());
    }

    @Test
    void sinFichaBuscaVacio() {
        assertTrue(almacen.buscar(clienteId).isEmpty());
    }

    @Test
    void clienteInexistenteFalla() {
        assertThrows(EntidadNoEncontradaException.class,
                () -> almacen.guardar(99, new FichaMedica("x.pdf", new byte[] { 1 })));
    }
}
