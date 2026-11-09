package com.example.api.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.core.cliente.CasosDeUsoClientes;
import com.example.core.cliente.CasosDeUsoFichasMedicas;
import com.example.core.dashboard.CasosDeUsoDashboard;
import com.example.core.inventario.CasosDeUsoInventario;
import com.example.core.itinerario.CasosDeUsoItinerario;
import com.example.core.ruta.CasosDeUsoRutas;
import com.example.core.salida.CasosDeUsoConfirmarSalida;
import com.example.core.salida.CasosDeUsoEditarSalida;
import com.example.core.salida.CasosDeUsoFichaOperativa;
import com.example.core.salida.CasosDeUsoSalidas;
import com.example.sqlite.AlmacenFichasMedicasSqlite;
import com.example.sqlite.BaseDatosSqlite;
import com.example.sqlite.RepositorioClientesSqlite;
import com.example.sqlite.RepositorioInventarioSqlite;
import com.example.sqlite.RepositorioItinerariosSqlite;
import com.example.sqlite.RepositorioRutasSqlite;
import com.example.sqlite.RepositorioSalidasSqlite;
import com.example.sqlite.TransaccionSqlite;

/**
 * Ensamblado de casos de uso y repositorios (ADR 0006).
 * Los controladores reciben los casos de uso por inyección.
 */
@Configuration
public class ConfiguracionCasosDeUso {

    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public RepositorioRutasSqlite repositorioRutas(BaseDatosSqlite base) {
        return new RepositorioRutasSqlite(base);
    }

    @Bean
    public RepositorioSalidasSqlite repositorioSalidas(BaseDatosSqlite base) {
        return new RepositorioSalidasSqlite(base);
    }

    @Bean
    public RepositorioClientesSqlite repositorioClientes(BaseDatosSqlite base) {
        return new RepositorioClientesSqlite(base);
    }

    @Bean
    public RepositorioInventarioSqlite repositorioInventario(BaseDatosSqlite base) {
        return new RepositorioInventarioSqlite(base);
    }

    @Bean
    public CasosDeUsoInventario casosDeUsoInventario(RepositorioInventarioSqlite repositorio) {
        return new CasosDeUsoInventario(repositorio);
    }

    @Bean
    public CasosDeUsoClientes casosDeUsoClientes(RepositorioClientesSqlite repositorio, Clock reloj) {
        return new CasosDeUsoClientes(repositorio, reloj);
    }

    @Bean
    public CasosDeUsoFichasMedicas casosDeUsoFichasMedicas(RepositorioClientesSqlite repositorio, BaseDatosSqlite base) {
        return new CasosDeUsoFichasMedicas(repositorio, new AlmacenFichasMedicasSqlite(base));
    }

    @Bean
    public CasosDeUsoRutas casosDeUsoRutas(RepositorioRutasSqlite repositorio) {
        return new CasosDeUsoRutas(repositorio);
    }

    @Bean
    public CasosDeUsoItinerario casosDeUsoItinerario(RepositorioRutasSqlite repositorio, BaseDatosSqlite base) {
        return new CasosDeUsoItinerario(repositorio, new RepositorioItinerariosSqlite(base));
    }

    @Bean
    public CasosDeUsoSalidas casosDeUsoSalidas(RepositorioSalidasSqlite repositorio, Clock reloj) {
        return new CasosDeUsoSalidas(repositorio, reloj);
    }

    @Bean
    public CasosDeUsoConfirmarSalida casosDeUsoConfirmarSalida(BaseDatosSqlite base, RepositorioSalidasSqlite repositorio,
            CasosDeUsoRutas rutas, CasosDeUsoItinerario itinerarios, CasosDeUsoInventario inventario,
            CasosDeUsoClientes clientes, CasosDeUsoFichasMedicas fichasMedicas) {
        return new CasosDeUsoConfirmarSalida(new TransaccionSqlite(base), repositorio,
                rutas, itinerarios, inventario, clientes, fichasMedicas);
    }

    @Bean
    public CasosDeUsoEditarSalida casosDeUsoEditarSalida(BaseDatosSqlite base, RepositorioSalidasSqlite repositorio,
            CasosDeUsoRutas rutas, CasosDeUsoItinerario itinerarios, CasosDeUsoInventario inventario,
            CasosDeUsoClientes clientes) {
        return new CasosDeUsoEditarSalida(new TransaccionSqlite(base), repositorio, rutas, itinerarios, inventario, clientes);
    }

    @Bean
    public CasosDeUsoFichaOperativa casosDeUsoFichaOperativa(RepositorioSalidasSqlite repositorio, Clock reloj) {
        return new CasosDeUsoFichaOperativa(repositorio, reloj);
    }

    @Bean
    public CasosDeUsoDashboard casosDeUsoDashboard(RepositorioRutasSqlite rutas, RepositorioClientesSqlite clientes,
            RepositorioInventarioSqlite inventario, RepositorioSalidasSqlite salidas, Clock reloj) {
        return new CasosDeUsoDashboard(rutas, clientes, inventario, salidas, reloj);
    }
}
