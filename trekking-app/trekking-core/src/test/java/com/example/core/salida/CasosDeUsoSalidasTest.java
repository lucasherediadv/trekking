package com.example.core.salida;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.core.excepcion.EntidadNoEncontradaException;

class CasosDeUsoSalidasTest {

    private static final LocalDate HOY = LocalDate.of(2026, 5, 10);

    private RepositorioSalidasEnMemoria repositorio;
    private CasosDeUsoSalidas casosDeUso;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioSalidasEnMemoria();
        Clock reloj = Clock.fixed(Instant.parse("2026-05-10T15:00:00Z"), ZoneOffset.UTC);
        casosDeUso = new CasosDeUsoSalidas(repositorio, reloj);
    }

    private static Salida salida(int id, LocalDate fecha, EstadoSalida estado) {
        return new Salida(id, 1, fecha, estado, "Cerro Uritorco", 1000, "Rocoso", "Media", "Alta");
    }

    @Test
    void marcaRealizadaUnaSalidaPlanificada() {
        repositorio.agregar(salida(1, HOY, EstadoSalida.PLANIFICADA));
        casosDeUso.marcarRealizada(1);
        assertEquals(EstadoSalida.REALIZADA, casosDeUso.obtener(1).estado());
    }

    @Test
    void cancelaUnaSalidaPlanificada() {
        repositorio.agregar(salida(1, HOY, EstadoSalida.PLANIFICADA));
        casosDeUso.cancelar(1);
        assertEquals(EstadoSalida.CANCELADA, casosDeUso.obtener(1).estado());
    }

    @Test
    void rechazaCerrarUnaSalidaRealizadaOCancelada() {
        repositorio.agregar(salida(1, HOY, EstadoSalida.REALIZADA));
        repositorio.agregar(salida(2, HOY, EstadoSalida.CANCELADA));
        assertThrows(TransicionInvalidaException.class, () -> casosDeUso.cancelar(1));
        assertThrows(TransicionInvalidaException.class, () -> casosDeUso.marcarRealizada(2));
        assertThrows(TransicionInvalidaException.class, () -> casosDeUso.marcarRealizada(1));
        assertEquals(EstadoSalida.REALIZADA, casosDeUso.obtener(1).estado());
        assertEquals(EstadoSalida.CANCELADA, casosDeUso.obtener(2).estado());
    }

    @Test
    void rechazaCerrarUnaSalidaInexistente() {
        assertThrows(EntidadNoEncontradaException.class, () -> casosDeUso.cancelar(99));
    }

    @Test
    void salidaConFechaPasadaEstaVencida() {
        repositorio.agregar(salida(1, HOY.minusDays(1), EstadoSalida.PLANIFICADA));
        assertTrue(casosDeUso.listar().get(0).vencida());
    }

    @Test
    void salidaDeHoyNoEstaVencida() {
        repositorio.agregar(salida(1, HOY, EstadoSalida.PLANIFICADA));
        assertFalse(casosDeUso.listar().get(0).vencida());
    }

    @Test
    void salidaCerradaConFechaPasadaNoEstaVencida() {
        repositorio.agregar(salida(1, HOY.minusDays(3), EstadoSalida.REALIZADA));
        assertFalse(casosDeUso.listar().get(0).vencida());
    }

    @Test
    void listaConSuResumenOVacioSiNoTiene() {
        repositorio.agregar(salida(1, HOY, EstadoSalida.PLANIFICADA), new ResumenOperativo("Ana Paz", "Soga × 2"));
        repositorio.agregar(salida(2, HOY, EstadoSalida.PLANIFICADA));
        var filas = casosDeUso.listar();
        assertEquals("Ana Paz", filas.get(0).resumen().participantes());
        assertEquals(ResumenOperativo.VACIO, filas.get(1).resumen());
    }
}
