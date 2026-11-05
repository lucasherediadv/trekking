package com.example.api.dashboard;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.core.dashboard.CasosDeUsoDashboard;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final CasosDeUsoDashboard dashboard;

    public DashboardController(CasosDeUsoDashboard dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/indicadores")
    public IndicadoresDto indicadores() {
        return IndicadoresDto.de(dashboard.indicadores());
    }

    @GetMapping("/indicadores-salidas")
    public IndicadoresDto indicadoresDeSalidas() {
        return IndicadoresDto.de(dashboard.indicadoresDeSalidas());
    }

    @GetMapping("/proximas-salidas")
    public List<ProximaSalidaDto> proximasSalidas() {
        return dashboard.proximasSalidas().stream().map(ProximaSalidaDto::de).toList();
    }
}
