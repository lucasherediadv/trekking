package com.example.api.salida;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.example.api.PruebaApi;
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

/** Confirmación de una Salida desde el Asistente: {@code POST /api/salidas}. */
class ConfirmarSalidaApiTest extends PruebaApi {

    private static final AtomicInteger DNI = new AtomicInteger(60_000_000 + ThreadLocalRandom.current().nextInt(9_000_000));
    private static final AtomicInteger NOMBRE = new AtomicInteger();

    @Autowired
    CasosDeUsoRutas rutas;
    @Autowired
    CasosDeUsoItinerario itinerarios;
    @Autowired
    CasosDeUsoInventario inventario;
    @Autowired
    CasosDeUsoClientes clientes;

    private static String unico(String base) {
        return base + " " + NOMBRE.incrementAndGet() + " " + System.nanoTime();
    }

    /** Nombres de Cliente: solo letras, así que los dígitos del sufijo único se pasan a letras. */
    private static String unicoLetras(String base) {
        StringBuilder letras = new StringBuilder();
        for (char c : unico("").trim().toCharArray()) letras.append(c == ' ' ? 'x' : (char) ('a' + (c - '0')));
        return base + " " + letras;
    }

    private int rutaConItinerario(String nombre) {
        int id = rutas.alta(new DatosRuta(nombre, 4200, "Rocoso", "Media", "Alta")).id();
        itinerarios.guardar(id, Itinerario.de(List.of(
                new Checkpoint(0, "Base " + nombre, "06:00", -33.0, -69.0, 3000, "Inicio"),
                new Checkpoint(0, "Cumbre " + nombre, "11:00", -33.1, -69.1, 4000, "Fin"))));
        return id;
    }

    private int elemento(String nombre, int cantidad) {
        return inventario.alta(new DatosElemento(nombre, "Campamento", cantidad, EstadoElemento.DISPONIBLE)).id();
    }

    private int clienteExistente(String nombre) {
        return clientes.alta(datosCliente(nombre)).id();
    }

    private static DatosCliente datosCliente(String nombre) {
        return new DatosCliente(String.valueOf(DNI.incrementAndGet()), nombre, "Gomez", LocalDate.of(1990, 1, 1), null,
                "3511234567", "Femenino", null, null, null, false, null, null, null, null);
    }

    private static String jsonClienteNuevo(String nombre, String ficha) {
        return """
                {"cliente":{"dni":"%s","nombre":"%s","apellido":"Nuevo","fechaNacimiento":"1991-02-03",
                "telefono":"3511234567","sexo":"Masculino","autorizacionMenores":false}%s}
                """.formatted(DNI.incrementAndGet(), nombre, ficha == null ? "" : ",\"ficha\":" + ficha);
    }

    private ResultActions confirmar(String cuerpo) throws Exception {
        return mvc.perform(post("/api/salidas").contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private static String cuerpo(int rutaId, String equipamiento, String elementosNuevos, String existentes,
            String nuevos) {
        return """
                {"fecha":"2031-03-01","rutaId":%d,"equipamiento":[%s],"elementosNuevos":[%s],
                "clientesExistentes":[%s],"clientesNuevos":[%s]}
                """.formatted(rutaId, equipamiento, elementosNuevos, existentes, nuevos);
    }

    @Test
    void confirmaLaSalidaConTodosSusDatosYQuedaConsultable() throws Exception {
        int ruta = rutaConItinerario(unico("Ruta confirmar"));
        int carpa = elemento(unico("Carpa"), 5);
        int existente = clienteExistente(unicoLetras("Existente"));
        String nombreNuevoElemento = unico("Soga");
        String nombreClienteNuevo = unicoLetras("Nuevo");

        String respuesta = confirmar(cuerpo(ruta,
                "{\"elementoId\":%d,\"cantidad\":2}".formatted(carpa),
                "{\"elemento\":{\"nombre\":\"%s\",\"categoria\":\"Seguridad\",\"cantidad\":4,\"estado\":\"DISPONIBLE\"},\"cantidad\":3}"
                        .formatted(nombreNuevoElemento),
                String.valueOf(existente),
                jsonClienteNuevo(nombreClienteNuevo, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.rutaId").value(ruta))
                .andExpect(jsonPath("$.fecha").value("2031-03-01"))
                .andExpect(jsonPath("$.estado").value("PLANIFICADA"))
                .andReturn().getResponse().getContentAsString();
        int id = Integer.parseInt(respuesta.replaceAll(".*\"id\":(\\d+).*", "$1"));

        mvc.perform(get("/api/salidas/" + id + "/copia-operativa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkpoints.length()").value(2))
                .andExpect(jsonPath("$.equipamiento[*].cantidad", hasItem(2)))
                .andExpect(jsonPath("$.equipamiento[*].nombre", hasItem(nombreNuevoElemento)))
                .andExpect(jsonPath("$.equipamiento[*].cantidad", hasItem(3)))
                .andExpect(jsonPath("$.participantes.length()").value(2))
                .andExpect(jsonPath("$.participantes[*].nombre", hasItem(nombreClienteNuevo)));

        mvc.perform(get("/api/inventario/" + carpa)).andExpect(jsonPath("$.cantidadReservada").value(2));
    }

    @Test
    void guardaLaFichaDelClienteNuevoRecibidaEnBase64() throws Exception {
        int ruta = rutaConItinerario(unico("Ruta ficha"));
        String nombre = unicoLetras("ConFicha");
        String base64 = Base64.getEncoder().encodeToString("contenido ficha".getBytes(StandardCharsets.UTF_8));

        confirmar(cuerpo(ruta, "", "", "",
                jsonClienteNuevo(nombre, "{\"nombreArchivo\":\"ficha.pdf\",\"contenidoBase64\":\"" + base64 + "\"}")))
                .andExpect(status().isCreated());

        int clienteId = clientes.listar().stream().filter(c -> c.nombre().equals(nombre)).findFirst().orElseThrow().id();
        mvc.perform(get("/api/clientes/" + clienteId + "/ficha-medica"))
                .andExpect(status().isOk())
                .andExpect(content().bytes("contenido ficha".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void base64InvalidoResponde422ConElCampo() throws Exception {
        int ruta = rutaConItinerario(unico("Ruta base64"));
        String nombre = unicoLetras("Base64Malo");

        confirmar(cuerpo(ruta, "", "", "",
                jsonClienteNuevo(nombre, "{\"nombreArchivo\":\"f.pdf\",\"contenidoBase64\":\"%%%no%%%\"}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errores[0].campo").value("clientesNuevos[0].ficha"));
        org.junit.jupiter.api.Assertions.assertTrue(clientes.listar().stream().noneMatch(c -> c.nombre().equals(nombre)));
    }

    @Test
    void faltaDeStockResponde409YNoDejaNingunDatoAMedias() throws Exception {
        int ruta = rutaConItinerario(unico("Ruta stock"));
        int escaso = elemento(unico("Escaso"), 2);
        String nombreElemento = unico("ElementoHuerfano");
        String nombreCliente = unicoLetras("ClienteHuerfano");

        confirmar(cuerpo(ruta,
                "{\"elementoId\":%d,\"cantidad\":3}".formatted(escaso),
                "{\"elemento\":{\"nombre\":\"%s\",\"categoria\":\"X\",\"cantidad\":1,\"estado\":\"DISPONIBLE\"},\"cantidad\":1}"
                        .formatted(nombreElemento),
                "", jsonClienteNuevo(nombreCliente, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"))
                .andExpect(jsonPath("$.disponible").value(2));

        mvc.perform(get("/api/salidas")).andExpect(jsonPath("$[?(@.rutaId==%d)]".formatted(ruta)).isEmpty());
        mvc.perform(get("/api/inventario")).andExpect(jsonPath("$[*].nombre", not(hasItem(nombreElemento))));
        mvc.perform(get("/api/clientes")).andExpect(jsonPath("$[*].nombre", not(hasItem(nombreCliente))));
        mvc.perform(get("/api/inventario/" + escaso)).andExpect(jsonPath("$.cantidadReservada").value(0));
    }

    @Test
    void rutaBorradorSeRechazaCon422() throws Exception {
        int borrador = rutas.alta(new DatosRuta(unico("Ruta borrador"), 4200, "Rocoso", "Media", "Alta")).id();

        confirmar(cuerpo(borrador, "", "", "", ""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errores[0].campo").value("ruta"));
    }

    @Test
    void rutaDadaDeBajaSeRechazaCon422() throws Exception {
        int ruta = rutaConItinerario(unico("Ruta baja"));
        rutas.darDeBaja(ruta);

        confirmar(cuerpo(ruta, "", "", "", ""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errores[0].campo").value("ruta"));
    }

    @Test
    void rutaOClienteInexistenteResponde404() throws Exception {
        confirmar(cuerpo(999_999, "", "", "", "")).andExpect(status().isNotFound());

        int ruta = rutaConItinerario(unico("Ruta cliente inexistente"));
        confirmar(cuerpo(ruta, "", "", "999999", "")).andExpect(status().isNotFound());
    }

    @Test
    void datosInvalidosResponden422ConErroresPorCampo() throws Exception {
        int ruta = rutaConItinerario(unico("Ruta invalida"));

        confirmar("{\"rutaId\":%d}".formatted(ruta))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("fecha")));
        confirmar("{\"fecha\":\"2031-03-01\"}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("rutaId")));
        confirmar(cuerpo(ruta, "", "", "", jsonClienteNuevo("", null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("nombre")));
    }

    @Test
    void cuerpoMalFormadoResponde400() throws Exception {
        confirmar("{no es json").andExpect(status().isBadRequest());
    }

    @Test
    void cambiosPosterioresEnOrigenNoAlteranLaCopiaOperativa() throws Exception {
        String nombreRuta = unico("Ruta aislada");
        int ruta = rutaConItinerario(nombreRuta);
        String nombreCliente = unicoLetras("Aislado");
        int existente = clienteExistente(nombreCliente);

        String respuesta = confirmar(cuerpo(ruta, "", "", String.valueOf(existente), ""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int id = Integer.parseInt(respuesta.replaceAll(".*\"id\":(\\d+).*", "$1"));

        rutas.editar(ruta, new DatosRuta(nombreRuta + " editada", 4300, "Rocoso", "Media", "Media"));
        itinerarios.guardar(ruta, Itinerario.de(List.of(
                new Checkpoint(0, "Otro 1", "07:00", -33.0, -69.0, 3000, "x"),
                new Checkpoint(0, "Otro 2", "08:00", -33.0, -69.0, 3100, "x"),
                new Checkpoint(0, "Otro 3", "09:00", -33.0, -69.0, 3200, "x"))));
        clientes.editar(existente, new DatosCliente(clientes.obtener(existente).dni(), "Renombrado", "Gomez",
                LocalDate.of(1990, 1, 1), null, "3511234567", "Femenino", null, null, null, false, null, null, null, null));

        mvc.perform(get("/api/salidas/" + id + "/copia-operativa"))
                .andExpect(jsonPath("$.ruta.nombre").value(nombreRuta))
                .andExpect(jsonPath("$.checkpoints.length()").value(2))
                .andExpect(jsonPath("$.participantes[0].nombre").value(nombreCliente));
    }
}
