package com.example.api.inventario;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.example.api.PruebaApi;
import com.example.core.inventario.CasosDeUsoInventario;
import com.example.core.inventario.DatosElemento;
import com.example.core.inventario.EstadoElemento;

class InventarioApiTest extends PruebaApi {

    @Autowired
    CasosDeUsoInventario inventario;

    private int crear(String nombre, EstadoElemento estado, boolean activo) {
        int id = inventario.alta(new DatosElemento(nombre, "Campamento", 5, estado)).id();
        if (!activo) inventario.darDeBaja(id);
        return id;
    }

    private static String json(String nombre, String categoria, Object cantidad, String estado) {
        return "{\"nombre\":\"" + nombre + "\",\"categoria\":\"" + categoria + "\",\"cantidad\":" + cantidad
                + ",\"estado\":" + (estado == null ? "null" : "\"" + estado + "\"") + "}";
    }

    @Test
    void listaSoloLosElementosActivosPorDefecto() throws Exception {
        crear("Inv lista activo", EstadoElemento.DISPONIBLE, true);
        crear("Inv lista baja", EstadoElemento.DISPONIBLE, false);

        mvc.perform(get("/api/inventario"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", hasItem("Inv lista activo")))
                .andExpect(jsonPath("$[*].nombre", not(hasItem("Inv lista baja"))));
    }

    @Test
    void filtraPorEstado() throws Exception {
        crear("Inv filtro activo", EstadoElemento.DISPONIBLE, true);
        crear("Inv filtro baja", EstadoElemento.DISPONIBLE, false);

        mvc.perform(get("/api/inventario").param("estado", "bajas"))
                .andExpect(jsonPath("$[*].nombre", hasItem("Inv filtro baja")))
                .andExpect(jsonPath("$[*].nombre", not(hasItem("Inv filtro activo"))));
        mvc.perform(get("/api/inventario").param("estado", "todas"))
                .andExpect(jsonPath("$[*].nombre", hasItem("Inv filtro baja")))
                .andExpect(jsonPath("$[*].nombre", hasItem("Inv filtro activo")));
    }

    @Test
    void asignablesExcluyeBajasYEstadosQueImpidenAsignar() throws Exception {
        crear("Inv asig disponible", EstadoElemento.DISPONIBLE, true);
        crear("Inv asig inspeccion", EstadoElemento.EN_INSPECCION, true);
        crear("Inv asig reparacion", EstadoElemento.EN_REPARACION, true);
        crear("Inv asig fuera", EstadoElemento.FUERA_DE_SERVICIO, true);
        crear("Inv asig extraviado", EstadoElemento.EXTRAVIADO, true);
        crear("Inv asig baja", EstadoElemento.DISPONIBLE, false);

        mvc.perform(get("/api/inventario/asignables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", hasItem("Inv asig disponible")))
                .andExpect(jsonPath("$[*].nombre", hasItem("Inv asig inspeccion")))
                .andExpect(jsonPath("$[*].nombre", not(hasItem("Inv asig reparacion"))))
                .andExpect(jsonPath("$[*].nombre", not(hasItem("Inv asig fuera"))))
                .andExpect(jsonPath("$[*].nombre", not(hasItem("Inv asig extraviado"))))
                .andExpect(jsonPath("$[*].nombre", not(hasItem("Inv asig baja"))));
    }

    @Test
    void devuelveElDetalleDeUnElemento() throws Exception {
        int id = crear("Inv detalle", EstadoElemento.EN_REPARACION, true);

        mvc.perform(get("/api/inventario/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nombre").value("Inv detalle"))
                .andExpect(jsonPath("$.categoria").value("Campamento"))
                .andExpect(jsonPath("$.cantidad").value(5))
                .andExpect(jsonPath("$.cantidadReservada").value(0))
                .andExpect(jsonPath("$.cantidadDisponible").value(5))
                .andExpect(jsonPath("$.estado").value("EN_REPARACION"))
                .andExpect(jsonPath("$.estadoEtiqueta").value("En Reparación"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.asignable").value(false));
    }

    @Test
    void elDetalleIndicaLaVigenciaDeUnElementoDadoDeBaja() throws Exception {
        int id = crear("Inv detalle baja", EstadoElemento.DISPONIBLE, false);

        mvc.perform(get("/api/inventario/{id}", id))
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.asignable").value(false));
    }

    @Test
    void crearDevuelve201ConElElemento() throws Exception {
        mvc.perform(post("/api/inventario").contentType(MediaType.APPLICATION_JSON)
                .content(json("Inv creado", "Seguridad", 3, "DISPONIBLE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Inv creado"))
                .andExpect(jsonPath("$.categoria").value("Seguridad"))
                .andExpect(jsonPath("$.cantidad").value(3))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void crearConDatosInvalidosResponde422ConErroresPorCampo() throws Exception {
        mvc.perform(post("/api/inventario").contentType(MediaType.APPLICATION_JSON)
                .content(json(" ", "Inexistente", -1, null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.errores[*].campo", hasItem("nombre")))
                .andExpect(jsonPath("$.errores[*].campo", hasItem("categoria")))
                .andExpect(jsonPath("$.errores[*].campo", hasItem("cantidad")))
                .andExpect(jsonPath("$.errores[*].campo", hasItem("estado")));
    }

    @Test
    void editarActualizaElElemento() throws Exception {
        int id = crear("Inv editar", EstadoElemento.DISPONIBLE, true);

        mvc.perform(put("/api/inventario/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(json("Inv editado", "Escalada", 9, "EXTRAVIADO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Inv editado"))
                .andExpect(jsonPath("$.categoria").value("Escalada"))
                .andExpect(jsonPath("$.cantidad").value(9))
                .andExpect(jsonPath("$.estado").value("EXTRAVIADO"));
    }

    @Test
    void editarConDatosInvalidosResponde422() throws Exception {
        int id = crear("Inv editar invalido", EstadoElemento.DISPONIBLE, true);

        mvc.perform(put("/api/inventario/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(json("", "Escalada", 9, "DISPONIBLE")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("nombre")));
    }

    @Test
    void darDeBajaYReactivarSonAccionesPost() throws Exception {
        int id = crear("Inv baja y alta", EstadoElemento.DISPONIBLE, true);

        mvc.perform(post("/api/inventario/{id}/baja", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        mvc.perform(get("/api/inventario").param("estado", "bajas"))
                .andExpect(jsonPath("$[*].nombre", hasItem("Inv baja y alta")));

        mvc.perform(post("/api/inventario/{id}/reactivar", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
        mvc.perform(get("/api/inventario"))
                .andExpect(jsonPath("$[*].nombre", hasItem("Inv baja y alta")));
    }

    @Test
    void noExisteDelete() throws Exception {
        int id = crear("Inv sin delete", EstadoElemento.DISPONIBLE, true);

        mvc.perform(delete("/api/inventario/{id}", id)).andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/api/inventario/{id}", id)).andExpect(status().isOk());
    }

    @Test
    void unElementoInexistenteResponde404() throws Exception {
        mvc.perform(get("/api/inventario/{id}", 999999)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
        mvc.perform(put("/api/inventario/{id}", 999999).contentType(MediaType.APPLICATION_JSON)
                .content(json("X", "Seguridad", 1, "DISPONIBLE"))).andExpect(status().isNotFound());
        mvc.perform(post("/api/inventario/{id}/baja", 999999)).andExpect(status().isNotFound());
        mvc.perform(post("/api/inventario/{id}/reactivar", 999999)).andExpect(status().isNotFound());
    }
}
