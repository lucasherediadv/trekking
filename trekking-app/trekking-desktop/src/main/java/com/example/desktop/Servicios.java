package com.example.desktop;

import java.time.Clock;

import com.example.core.cliente.CasosDeUsoClientes;
import com.example.core.cliente.CasosDeUsoFichasMedicas;
import com.example.core.inventario.CasosDeUsoInventario;
import com.example.core.itinerario.CasosDeUsoItinerario;
import com.example.core.ruta.CasosDeUsoRutas;
import com.example.desktop.database.ConexionDB;
import com.example.sqlite.AlmacenFichasMedicasSqlite;
import com.example.sqlite.RepositorioClientesSqlite;
import com.example.sqlite.RepositorioInventarioSqlite;
import com.example.sqlite.RepositorioItinerariosSqlite;
import com.example.sqlite.RepositorioRutasSqlite;

/** Ensambla los casos de uso de core con su persistencia sqlite. Único lugar donde desktop conoce ambas. */
public final class Servicios {

    private static final RepositorioRutasSqlite REPOSITORIO_RUTAS = new RepositorioRutasSqlite(ConexionDB.base());

    private static final CasosDeUsoInventario INVENTARIO =
            new CasosDeUsoInventario(new RepositorioInventarioSqlite(ConexionDB.base()));

    private static final CasosDeUsoClientes CLIENTES =
            new CasosDeUsoClientes(new RepositorioClientesSqlite(ConexionDB.base()), Clock.systemDefaultZone());

    private static final CasosDeUsoFichasMedicas FICHAS_MEDICAS = new CasosDeUsoFichasMedicas(
            new RepositorioClientesSqlite(ConexionDB.base()), new AlmacenFichasMedicasSqlite(ConexionDB.base()));

    private static final CasosDeUsoRutas RUTAS = new CasosDeUsoRutas(REPOSITORIO_RUTAS);

    private static final CasosDeUsoItinerario ITINERARIOS = new CasosDeUsoItinerario(
            REPOSITORIO_RUTAS, new RepositorioItinerariosSqlite(ConexionDB.base()));

    private Servicios() {
    }

    public static CasosDeUsoInventario inventario() {
        return INVENTARIO;
    }

    public static CasosDeUsoClientes clientes() {
        return CLIENTES;
    }

    public static CasosDeUsoFichasMedicas fichasMedicas() {
        return FICHAS_MEDICAS;
    }

    public static CasosDeUsoRutas rutas() {
        return RUTAS;
    }

    public static CasosDeUsoItinerario itinerarios() {
        return ITINERARIOS;
    }
}
