import { screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { server } from "../test/servidor";
import { renderApp } from "../test/renderApp";

const elemento = (id: number, nombre: string, extra: Record<string, unknown> = {}) => ({
  id,
  nombre,
  categoria: "Camping",
  cantidad: 10,
  cantidadReservada: 2,
  cantidadDisponible: 8,
  estado: "DISPONIBLE",
  estadoEtiqueta: "Disponible",
  activo: true,
  asignable: true,
  ...extra,
});

const ACTIVOS = [
  elemento(1, "Carpa Uno"),
  elemento(2, "Soga Dos", { estado: "EN_REPARACION", estadoEtiqueta: "En Reparación", asignable: false }),
  elemento(3, "Piqueta Tres", { estado: "EXTRAVIADO", estadoEtiqueta: "Extraviado", asignable: false }),
];
const BAJAS = [elemento(9, "Vieja Nueve", { activo: false })];

function listadoPorEstado() {
  const pedidos: (string | null)[] = [];
  server.use(
    http.get("*/api/inventario", ({ request }) => {
      const estado = new URL(request.url).searchParams.get("estado");
      pedidos.push(estado);
      if (estado === "bajas") return HttpResponse.json(BAJAS);
      if (estado === "todas") return HttpResponse.json([...ACTIVOS, ...BAJAS]);
      return HttpResponse.json(ACTIVOS);
    }),
  );
  return pedidos;
}

const filas = () => screen.getAllByRole("row").slice(1);

describe("listado de Inventario", () => {
  it("muestra carga y luego los elementos activos con cantidad, Estado del elemento y vigencia", async () => {
    const pedidos = listadoPorEstado();
    renderApp("/inventario");
    expect(screen.getByRole("status", { name: "Cargando" })).toBeInTheDocument();
    expect(await screen.findByText("Carpa Uno")).toBeInTheDocument();
    expect(filas()).toHaveLength(3);
    expect(screen.getByRole("combobox", { name: "Estado" })).toHaveValue("activas");
    expect(pedidos).toEqual([null]);
    const fila = screen.getByText("Carpa Uno").closest("tr")!;
    expect(within(fila).getByText("10")).toBeInTheDocument();
    expect(within(fila).getByText("Disponible")).toHaveClass("estado-exito");
    expect(within(fila).getByText("Vigente")).toBeInTheDocument();
    expect(screen.getByRole("columnheader", { name: "Estado del elemento" })).toBeInTheDocument();
  });

  it("colorea el Estado del elemento según su tono", async () => {
    listadoPorEstado();
    renderApp("/inventario");
    await screen.findByText("Carpa Uno");
    expect(screen.getByText("En Reparación")).toHaveClass("estado-advertencia");
    expect(screen.getByText("Extraviado")).toHaveClass("estado-peligro");
  });

  it("el selector estado ofrece activas, bajas y todas y recarga desde la API", async () => {
    const pedidos = listadoPorEstado();
    const { usuario } = renderApp("/inventario");
    await screen.findByText("Carpa Uno");
    const selector = screen.getByRole("combobox", { name: "Estado" });
    expect(within(selector).getAllByRole("option").map((o) => o.textContent)).toEqual(["Activas", "Bajas", "Todas"]);
    await usuario.selectOptions(selector, "bajas");
    const fila = (await screen.findByText("Vieja Nueve")).closest("tr")!;
    expect(screen.queryByText("Carpa Uno")).not.toBeInTheDocument();
    expect(within(fila).getByText("De baja")).toBeInTheDocument();
    await usuario.selectOptions(selector, "todas");
    expect(await screen.findByText("Carpa Uno")).toBeInTheDocument();
    expect(screen.getByText("Vieja Nueve")).toBeInTheDocument();
    expect(pedidos).toEqual([null, "bajas", "todas"]);
  });

  it("muestra estado vacío cuando no hay elementos", async () => {
    server.use(http.get("*/api/inventario", () => HttpResponse.json([])));
    renderApp("/inventario");
    expect(await screen.findByText("Todavía no hay elementos activos en el Inventario.")).toBeInTheDocument();
  });

  it("da de baja un elemento activo y confirma con un aviso", async () => {
    listadoPorEstado();
    let baja = 0;
    server.use(
      http.post("*/api/inventario/1/baja", () => {
        baja++;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const { usuario } = renderApp("/inventario");
    const fila = (await screen.findByText("Carpa Uno")).closest("tr")!;
    expect(within(fila).queryByRole("button", { name: /Reactivar/ })).not.toBeInTheDocument();
    await usuario.click(within(fila).getByRole("button", { name: "Dar de baja Carpa Uno" }));
    expect(await screen.findByRole("status", { name: "Notificación" })).toHaveTextContent("Elemento dado de baja");
    expect(baja).toBe(1);
  });

  it("reactiva un elemento de baja y solo ofrece esa acción", async () => {
    listadoPorEstado();
    let reactivado = 0;
    server.use(
      http.post("*/api/inventario/9/reactivar", () => {
        reactivado++;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const { usuario } = renderApp("/inventario");
    await screen.findByText("Carpa Uno");
    await usuario.selectOptions(screen.getByRole("combobox", { name: "Estado" }), "bajas");
    const fila = (await screen.findByText("Vieja Nueve")).closest("tr")!;
    expect(within(fila).queryByRole("button", { name: /Dar de baja/ })).not.toBeInTheDocument();
    await usuario.click(within(fila).getByRole("button", { name: "Reactivar Vieja Nueve" }));
    expect(await screen.findByRole("status", { name: "Notificación" })).toHaveTextContent("Elemento reactivado");
    expect(reactivado).toBe(1);
  });

  it("una falla al cargar se avisa y permite reintentar", async () => {
    let servidorCaido = true; // no depende del orden de llegada: un pedido rezagado de otro test no lo altera
    server.use(
      http.get("*/api/inventario", () => {
        return servidorCaido ? HttpResponse.error() : HttpResponse.json(ACTIVOS);
      }),
    );
    const { usuario } = renderApp("/inventario");
    const aviso = await screen.findByRole("alert");
    expect(aviso).toHaveTextContent("No se pudo conectar con el servidor");
    servidorCaido = false;
    await usuario.click(within(aviso).getByRole("button", { name: "Reintentar" }));
    expect(await screen.findByText("Carpa Uno")).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByRole("alert")).not.toBeInTheDocument());
  });
});
