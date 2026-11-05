package com.example.api.dashboard;

import com.example.core.dashboard.CasosDeUsoDashboard.Indicadores;

/** Representación JSON de los indicadores del Dashboard. */
public record IndicadoresDto(int rutasActivas, int clientesActivos, int salidasProximas, int equipoNoDisponible) {

    static IndicadoresDto de(Indicadores indicadores) {
        return new IndicadoresDto(indicadores.rutasActivas(), indicadores.clientesActivos(),
                indicadores.salidasProximas(), indicadores.equipoNoDisponible());
    }
}
