package com.example.api.dashboard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.example.core.cliente.CasosDeUsoClientes;
import com.example.core.cliente.DatosCliente;
import com.example.core.inventario.CasosDeUsoInventario;
import com.example.core.inventario.DatosElemento;
import com.example.core.inventario.EstadoElemento;
import com.example.core.itinerario.CasosDeUsoItinerario;
import com.example.core.itinerario.Checkpoint;
import com.example.core.itinerario.Itinerario;
import com.example.core.ruta.CasosDeUsoRutas;
import com.example.core.ruta.DatosRuta;
import com.example.core.salida.CasosDeUsoConfirmarSalida;
import com.example.core.salida.DatosSalida;

class DashboardConDatosApiTest extends BaseDashboardAislada {

    @Autowired
    MockMvc mvc;
    @Autowired
    CasosDeUsoRutas rutas;
    @Autowired
    CasosDeUsoItinerario itinerarios;
    @Autowired
    CasosDeUsoClientes clientes;
    @Autowired
    CasosDeUsoInventario inventario;
    @Autowired
    CasosDeUsoConfirmarSalida confirmar;

    @DynamicPropertySource
    static void base(DynamicPropertyRegistry registro) {
        String ruta = nuevaBase();
        registro.add("trekking.db", () -> ruta);
    }

    @Test
    void reflejaLosDatosSembradosPorLosCasosDeUso() throws Exception {
        int rutaId = rutas.alta(new DatosRuta("Dashboard Cerro", 4200, "Rocoso", "Media", "Alta")).id();
        itinerarios.guardar(rutaId, Itinerario.de(List.of(
                new Checkpoint(0, "Inicio", "08:00", 0, 0, 1000, null),
                new Checkpoint(0, "Cumbre", "16:30", 0, 0, 2000, null))));
        int clienteId = clientes.alta(new DatosCliente("30111222", "Ana", "Paz", LocalDate.of(1990, 1, 1), null,
                "3511234567", "Femenino", "Luis", "3517654321", "Padre", false, null, null, null, null)).id();
        inventario.alta(new DatosElemento("Dashboard Soga", "Campamento", 1, EstadoElemento.DISPONIBLE));
        LocalDate fecha = LocalDate.now().plusDays(1);
        confirmar.confirmar(new DatosSalida(fecha, rutaId, List.of(), List.of(), List.of(clienteId), List.of()));

        for (String url : new String[] { "/api/dashboard/indicadores", "/api/dashboard/indicadores-salidas" }) {
            mvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rutasActivas").value(1))
                    .andExpect(jsonPath("$.clientesActivos").value(1))
                    .andExpect(jsonPath("$.salidasProximas").value(1))
                    .andExpect(jsonPath("$.equipoNoDisponible").value(1));
        }

        mvc.perform(get("/api/dashboard/proximas-salidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombreRuta").value("Dashboard Cerro"))
                .andExpect(jsonPath("$[0].fecha").value(fecha.toString()))
                .andExpect(jsonPath("$[0].horaInicio").value("08:00"))
                .andExpect(jsonPath("$[0].horaFin").value("16:30"))
                .andExpect(jsonPath("$[0].participantes").value(1));
    }
}
