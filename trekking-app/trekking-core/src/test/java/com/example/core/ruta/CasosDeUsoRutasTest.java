package com.example.core.ruta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.example.core.excepcion.EntidadNoEncontradaException;
import com.example.core.excepcion.ErrorCampo;
import com.example.core.excepcion.ValidacionException;

import org.junit.jupiter.api.Test;

class CasosDeUsoRutasTest {

    private final RepositorioRutasEnMemoria repositorio = new RepositorioRutasEnMemoria();
    private final CasosDeUsoRutas rutas = new CasosDeUsoRutas(repositorio);

    private static DatosRuta datos(String nombre) {
        return new DatosRuta(nombre, 1250, "Rocoso", "Media", "Alta");
    }

    @Test
    void altaValidaCreaUnaRutaActivaYBorrador() {
        Ruta creada = rutas.alta(datos("  Cerro   Uritorco "));

        assertEquals("Cerro Uritorco", creada.nombre());
        assertTrue(creada.activa());
        assertTrue(creada.esBorrador());
        assertEquals(List.of(creada), rutas.listarActivas());
    }

    @Test
    void altaInvalidaInformaElErrorDeCadaCampoYNoGuarda() {
        ValidacionException e = assertThrows(ValidacionException.class,
                () -> rutas.alta(new DatosRuta("  ", -5, null, "Extrema", null)));

        assertEquals(List.of("nombre", "altitudMaxima", "tipoTerreno", "dificultadTecnica", "dificultadFisica"),
                e.errores().stream().map(ErrorCampo::campo).toList());
        assertEquals(List.of(), rutas.listarTodas());
    }

    @Test
    void altaRechazaNombreConCaracteresInvalidosONombreLargo() {
        assertThrows(ValidacionException.class, () -> rutas.alta(datos("Cerro-Uritorco!")));
        assertThrows(ValidacionException.class, () -> rutas.alta(datos("a".repeat(101))));
    }

    @Test
    void altaRechazaNombreDuplicadoSinImportarMayusculasNiEspacios() {
        rutas.alta(datos("Cerro Uritorco"));

        ValidacionException e = assertThrows(ValidacionException.class, () -> rutas.alta(datos(" cerro  URITORCO")));

        assertEquals("nombre", e.errores().get(0).campo());
        assertEquals(1, rutas.listarTodas().size());
    }

    @Test
    void edicionActualizaLosDatosYPermiteConservarElPropioNombre() {
        Ruta ruta = rutas.alta(datos("Cerro Uritorco"));

        Ruta editada = rutas.editar(ruta.id(), new DatosRuta("Cerro Uritorco", 1900, "Mixto", "Baja", "Baja"));

        assertEquals(1900, editada.altitudMaxima());
        assertEquals("Mixto", rutas.obtener(ruta.id()).tipoTerreno());
    }

    @Test
    void edicionRechazaElNombreDeOtraRuta() {
        rutas.alta(datos("Cerro Uritorco"));
        Ruta otra = rutas.alta(datos("Cerro Champaquí"));

        assertThrows(ValidacionException.class, () -> rutas.editar(otra.id(), datos("cerro uritorco")));
    }

    @Test
    void edicionDeRutaInexistenteFalla() {
        assertThrows(EntidadNoEncontradaException.class, () -> rutas.editar(99, datos("Nueva")));
    }

    @Test
    void bajaYReactivacionCambianLaVigenciaYElListado() {
        Ruta ruta = rutas.alta(datos("Cerro Uritorco"));

        rutas.darDeBaja(ruta.id());
        assertEquals(List.of(), rutas.listarActivas());
        assertEquals(1, rutas.listarDadasDeBaja().size());
        assertFalse(rutas.obtener(ruta.id()).activa());

        rutas.reactivar(ruta.id());
        assertEquals(1, rutas.listarActivas().size());
        assertEquals(List.of(), rutas.listarDadasDeBaja());
        assertEquals(1, rutas.listarTodas().size());
    }

    @Test
    void bajaDeRutaInexistenteFalla() {
        assertThrows(EntidadNoEncontradaException.class, () -> rutas.darDeBaja(99));
        assertThrows(EntidadNoEncontradaException.class, () -> rutas.reactivar(99));
    }

    @Test
    void rutaSinCheckpointsEsBorradorYNoPuedeUsarseEnUnaSalida() {
        Ruta ruta = rutas.alta(datos("Cerro Uritorco"));

        assertTrue(ruta.esBorrador());
        assertFalse(ruta.puedeUsarseEnSalida());
    }

    @Test
    void rutaConDosOMasCheckpointsNoEsBorradorYPuedeUsarseEnUnaSalida() {
        Ruta ruta = rutas.alta(datos("Cerro Uritorco"));
        repositorio.fijarCheckpoints(ruta.id(), 2);

        Ruta guardada = rutas.obtener(ruta.id());

        assertFalse(guardada.esBorrador());
        assertTrue(guardada.puedeUsarseEnSalida());
    }

    @Test
    void validadorAceptaLaAltitudComoTextoConComa() {
        assertEquals(List.of(), ValidadorRuta.validar("Cerro", "1250,5", "Rocoso", "Baja", "Baja"));
        assertEquals("altitudMaxima", ValidadorRuta.validar("Cerro", "abc", "Rocoso", "Baja", "Baja").get(0).campo());
        assertEquals("altitudMaxima", ValidadorRuta.validar("Cerro", "", "Rocoso", "Baja", "Baja").get(0).campo());
    }
}
