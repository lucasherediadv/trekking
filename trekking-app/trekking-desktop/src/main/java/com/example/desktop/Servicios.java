package com.example.desktop;

import com.example.core.inventario.CasosDeUsoInventario;
import com.example.desktop.database.ConexionDB;
import com.example.sqlite.RepositorioInventarioSqlite;

/** Ensambla los casos de uso de core con su persistencia sqlite. Único lugar donde desktop conoce ambas. */
public final class Servicios {

    private static final CasosDeUsoInventario INVENTARIO =
            new CasosDeUsoInventario(new RepositorioInventarioSqlite(ConexionDB.base()));

    private Servicios() {
    }

    public static CasosDeUsoInventario inventario() {
        return INVENTARIO;
    }
}
