package com.example.desktop;

import java.time.Clock;

import com.example.core.cliente.CasosDeUsoClientes;
import com.example.core.inventario.CasosDeUsoInventario;
import com.example.desktop.database.ConexionDB;
import com.example.sqlite.RepositorioClientesSqlite;
import com.example.sqlite.RepositorioInventarioSqlite;

/** Ensambla los casos de uso de core con su persistencia sqlite. Único lugar donde desktop conoce ambas. */
public final class Servicios {

    private static final CasosDeUsoInventario INVENTARIO =
            new CasosDeUsoInventario(new RepositorioInventarioSqlite(ConexionDB.base()));

    private static final CasosDeUsoClientes CLIENTES =
            new CasosDeUsoClientes(new RepositorioClientesSqlite(ConexionDB.base()), Clock.systemDefaultZone());

    private Servicios() {
    }

    public static CasosDeUsoInventario inventario() {
        return INVENTARIO;
    }

    public static CasosDeUsoClientes clientes() {
        return CLIENTES;
    }
}
