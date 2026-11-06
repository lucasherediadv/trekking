import { screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { server } from "../test/servidor";
import { renderApp } from "../test/renderApp";

const salida = (id: number, extra: Record<string, unknown> = {}) => ({
  id,
  rutaId: 1,
  fecha: "2026-11-10",
  estado: "PLANIFICADA",
  rutaNombre: "Cerro Uno",
  participantes: "Ana Pérez, Luis Gómez",
  equipamiento: "2 Carpas",
  vencida: false,
  ...extra,
});

const SALIDAS = [
  salida(1),
  salida(2, { rutaNombre: "Laguna Dos", fecha: "2026-10-01", vencida: true, participantes: "", equipamiento: "" }),
  salida(3, { rutaNombre: "Sendero Tres", estado: "REALIZADA", fecha: "2026-09-01" }),
  salida(4, { rutaNombre: "Mirador Cuatro", estado: "CANCELADA", fecha: "2026-09-05" }),
];

const INDICADORES = { salidasProximas: 3, rutasActivas: 7, clientesActivos: 12, equipoNoDisponible: 2 };
const PROXIMAS = [
  { id: 1, nombreRuta: "Cerro Uno", fecha: "2026-11-10", horaInicio: "08:00", horaFin: "16:30", participantes: 2 },
];

function apiFeliz() {
  server.use(
    http.get("*/api/salidas", () => HttpResponse.json(SALIDAS)),
    http.get("*/api/dashboard/indicadores-salidas", () => HttpResponse.json(INDICADORES)),
    http.get("*/api/dashboard/proximas-salidas", () => HttpResponse.json(PROXIMAS)),
  );
}

const fila = (texto: string) => within(screen.getByRole("table")).getByText(texto).closest("tr")!;

describe("listado de Salidas e inicio", () => {
  it("la raíz redirige a Salidas y marca la sección activa", async () => {
    apiFeliz();
    renderApp("/");
    expect(screen.getByRole("heading", { name: "Salidas" })).toBeInTheDocument();
    const menu = screen.getByRole("navigation", { name: "Menú principal" });
    expect(within(menu).getByRole("link", { name: "Salidas" })).toHaveAttribute("aria-current", "page");
    expect(await screen.findByText("Mirador Cuatro")).toBeInTheDocument();
  });

  it("muestra carga y luego indicadores, próximas Salidas y la tabla con su resumen", async () => {
    apiFeliz();
    renderApp("/salidas");
    expect(screen.getByRole("status", { name: "Cargando" })).toBeInTheDocument();
    await screen.findByText("Mirador Cuatro");

    const indicadores = screen.getByRole("region", { name: "Indicadores" });
    for (const [titulo, cifra] of [
      ["Salidas próximas", "3"],
      ["Rutas activas", "7"],
      ["Clientes activos", "12"],
      ["Inventario crítico", "2"],
    ]) {
      const tarjeta = within(indicadores).getByRole("link", { name: new RegExp(titulo) });
      expect(tarjeta).toHaveTextContent(cifra);
    }
    expect(within(indicadores).getByRole("link", { name: /Rutas activas/ })).toHaveAttribute("href", "/rutas");
    expect(within(indicadores).getByRole("link", { name: /Inventario crítico/ })).toHaveAttribute("href", "/inventario");

    const proximas = screen.getByRole("region", { name: "Próximas Salidas" });
    expect(within(proximas).getByText("Cerro Uno")).toBeInTheDocument();
    expect(proximas).toHaveTextContent("10/11/2026");
    expect(proximas).toHaveTextContent("08:00 - 16:30");

    const cabeceras = within(screen.getByRole("table")).getAllByRole("columnheader").map((c) => c.textContent);
    expect(cabeceras).toEqual(["Fecha", "Ruta", "Participantes", "Equipamiento asignado", "Estado", "Acciones"]);
    const primera = fila("Cerro Uno").cells;
    expect(primera[0]).toHaveTextContent("10/11/2026");
    expect(primera[2]).toHaveTextContent("Ana Pérez, Luis Gómez");
    expect(primera[3]).toHaveTextContent("2 Carpas");
    expect(fila("Laguna Dos").cells[2]).toHaveTextContent("—");
    expect(fila("Laguna Dos").cells[3]).toHaveTextContent("—");
  });

  it("marca la Salida vencida como condición, sin cambiar su estado", async () => {
    apiFeliz();
    renderApp("/salidas");
    await screen.findByText("Laguna Dos");
    const vencida = fila("Laguna Dos");
    expect(within(vencida).getByText("Planificada")).toBeInTheDocument();
    expect(within(vencida).getByText("Vencida")).toBeInTheDocument();
    expect(within(fila("Cerro Uno")).queryByText("Vencida")).not.toBeInTheDocument();
  });

  it("ofrece solo las acciones válidas para el estado de cada fila", async () => {
    apiFeliz();
    renderApp("/salidas");
    await screen.findByText("Laguna Dos");
    const planificada = fila("Cerro Uno");
    for (const accion of ["Editar", "Realizar", "Cancelar"]) {
      expect(within(planificada).getByRole("button", { name: `${accion} Salida de Cerro Uno` })).toBeInTheDocument();
    }
    for (const ruta of ["Sendero Tres", "Mirador Cuatro"]) {
      expect(within(fila(ruta)).queryByRole("button")).not.toBeInTheDocument();
    }
  });

  it("los colores de estado siguen los del escritorio", async () => {
    apiFeliz();
    renderApp("/salidas");
    await screen.findByText("Laguna Dos");
    expect(within(fila("Cerro Uno")).getByText("Planificada")).toHaveClass("estado-informacion");
    expect(within(fila("Sendero Tres")).getByText("Realizada")).toHaveClass("estado-exito");
    expect(within(fila("Mirador Cuatro")).getByText("Cancelada")).toHaveClass("estado-peligro");
  });

  it("muestra estado vacío cuando no hay Salidas ni próximas", async () => {
    server.use(
      http.get("*/api/salidas", () => HttpResponse.json([])),
      http.get("*/api/dashboard/indicadores-salidas", () =>
        HttpResponse.json({ salidasProximas: 0, rutasActivas: 0, clientesActivos: 0, equipoNoDisponible: 0 }),
      ),
      http.get("*/api/dashboard/proximas-salidas", () => HttpResponse.json([])),
    );
    renderApp("/salidas");
    expect(await screen.findByText("Todavía no hay Salidas.")).toBeInTheDocument();
    expect(screen.getByText("No hay Salidas próximas.")).toBeInTheDocument();
  });

  describe("realizar y cancelar", () => {
    afterEach(() => vi.restoreAllMocks());
    const errorApi = (codigo: string, mensaje: string) => ({ codigo, mensaje });
    // Tras la acción el listado recargado trae la Salida 1 con el estado nuevo.
    function listadoTrasAccion(estadoNuevo: string) {
      let cargas = 0;
      server.use(
        http.get("*/api/salidas", () => {
          cargas++;
          return HttpResponse.json(cargas === 1 ? SALIDAS : [salida(1, { estado: estadoNuevo }), ...SALIDAS.slice(1)]);
        }),
        http.get("*/api/dashboard/indicadores-salidas", () => HttpResponse.json(INDICADORES)),
        http.get("*/api/dashboard/proximas-salidas", () => HttpResponse.json(PROXIMAS)),
      );
      return () => cargas;
    }

    it.each([
      ["Realizar", "realizada", "REALIZADA", "Realizada", "Salida marcada como realizada"],
      ["Cancelar", "cancelar", "CANCELADA", "Cancelada", "Salida cancelada"],
    ])("%s pide confirmación, llama a la API, avisa y recarga sin acciones", async (accion, ruta, nuevo, texto, aviso) => {
      const cargas = listadoTrasAccion(nuevo);
      let llamadas = 0;
      server.use(
        http.post(`*/api/salidas/1/${ruta}`, () => {
          llamadas++;
          return new HttpResponse(null, { status: 204 });
        }),
      );
      const confirmar = vi.spyOn(window, "confirm").mockReturnValue(true);
      const { usuario } = renderApp("/salidas");
      await usuario.click(await screen.findByRole("button", { name: `${accion} Salida de Cerro Uno` }));
      expect(confirmar).toHaveBeenCalledOnce();
      expect(await screen.findByRole("status", { name: "Notificación" })).toHaveTextContent(aviso);
      await waitFor(() => expect(within(fila("Cerro Uno")).getByText(texto)).toBeInTheDocument());
      expect(llamadas).toBe(1);
      expect(cargas()).toBe(2);
      expect(within(fila("Cerro Uno")).queryByRole("button")).not.toBeInTheDocument();
    });

    it("si no se confirma no se llama a la API", async () => {
      apiFeliz();
      let llamadas = 0;
      server.use(
        http.post("*/api/salidas/1/cancelar", () => {
          llamadas++;
          return new HttpResponse(null, { status: 204 });
        }),
      );
      vi.spyOn(window, "confirm").mockReturnValue(false);
      const { usuario } = renderApp("/salidas");
      await usuario.click(await screen.findByRole("button", { name: "Cancelar Salida de Cerro Uno" }));
      expect(llamadas).toBe(0);
    });

    it("un 409 muestra el mensaje de la API, recarga y no reintenta", async () => {
      const cargas = listadoTrasAccion("REALIZADA");
      let llamadas = 0;
      server.use(
        http.post("*/api/salidas/1/cancelar", () => {
          llamadas++;
          return HttpResponse.json(errorApi("CONFLICTO", "La Salida ya fue realizada"), { status: 409 });
        }),
      );
      vi.spyOn(window, "confirm").mockReturnValue(true);
      const { usuario } = renderApp("/salidas");
      await usuario.click(await screen.findByRole("button", { name: "Cancelar Salida de Cerro Uno" }));
      expect(await screen.findByRole("status", { name: "Notificación" })).toHaveTextContent(
        "La Salida ya fue realizada",
      );
      await waitFor(() => expect(within(fila("Cerro Uno")).getByText("Realizada")).toBeInTheDocument());
      expect(llamadas).toBe(1);
      expect(cargas()).toBe(2);
    });

    it("un 404 avisa que la Salida ya no existe y recarga el listado", async () => {
      let cargas = 0;
      server.use(
        http.get("*/api/salidas", () => {
          cargas++;
          return HttpResponse.json(cargas === 1 ? SALIDAS : SALIDAS.slice(1));
        }),
        http.get("*/api/dashboard/indicadores-salidas", () => HttpResponse.json(INDICADORES)),
        http.get("*/api/dashboard/proximas-salidas", () => HttpResponse.json(PROXIMAS)),
        http.post("*/api/salidas/1/realizada", () =>
          HttpResponse.json(errorApi("NO_ENCONTRADO", "Salida 1 no existe"), { status: 404 }),
        ),
      );
      vi.spyOn(window, "confirm").mockReturnValue(true);
      const { usuario } = renderApp("/salidas");
      await usuario.click(await screen.findByRole("button", { name: "Realizar Salida de Cerro Uno" }));
      expect(await screen.findByRole("status", { name: "Notificación" })).toHaveTextContent("El recurso ya no existe");
      await waitFor(() => expect(within(screen.getByRole("table")).queryByText("Cerro Uno")).not.toBeInTheDocument());
    });
  });

  it("un error de carga se informa y permite reintentar", async () => {
    apiFeliz();
    let intentos = 0;
    server.use(
      http.get("*/api/salidas", () => {
        intentos++;
        return intentos === 1 ? HttpResponse.error() : HttpResponse.json(SALIDAS);
      }),
    );
    const { usuario } = renderApp("/salidas");
    const aviso = await screen.findByRole("alert");
    expect(aviso).toHaveTextContent("No se pudo conectar con el servidor");
    await usuario.click(within(aviso).getByRole("button", { name: "Reintentar" }));
    expect(await screen.findByText("Mirador Cuatro")).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });
});
