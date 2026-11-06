import { screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { server } from "../test/servidor";
import { renderApp } from "../test/renderApp";

const cliente = (id: number, nombre: string, extra: Record<string, unknown> = {}) => ({
  id,
  dni: `3000000${id}`,
  nombre,
  apellido: "Pérez",
  fechaNacimiento: "1990-01-01",
  email: `${nombre.toLowerCase()}@mail.com`,
  telefono: "1155550000",
  activo: true,
  menor: false,
  ...extra,
});

const ACTIVOS = [cliente(1, "Ana"), cliente(2, "Beto", { menor: true, fechaNacimiento: "2015-05-05" })];
const BAJAS = [cliente(9, "Zoe", { activo: false })];
// Solo Ana (1) y Zoe (9) tienen ficha médica.
const CON_FICHA = [1, 9];

function listadoPorEstado() {
  const pedidos: (string | null)[] = [];
  server.use(
    http.get("*/api/clientes", ({ request }) => {
      const estado = new URL(request.url).searchParams.get("estado");
      pedidos.push(estado);
      if (estado === "bajas") return HttpResponse.json(BAJAS);
      if (estado === "todas") return HttpResponse.json([...ACTIVOS, ...BAJAS]);
      return HttpResponse.json(ACTIVOS);
    }),
    // La web solo consulta si existe la ficha (HEAD); nunca descarga su contenido.
    http.head("*/api/clientes/:id/ficha-medica", ({ params }) =>
      new HttpResponse(null, { status: CON_FICHA.includes(Number(params.id)) ? 200 : 404 }),
    ),
  );
  return pedidos;
}

const filas = () => screen.getAllByRole("row").slice(1);
const errorApi = (codigo: string, mensaje: string) => ({ codigo, mensaje });

describe("listado de Clientes", () => {
  it("muestra carga y luego los Clientes activos por defecto", async () => {
    const pedidos = listadoPorEstado();
    renderApp("/clientes");
    expect(screen.getByRole("status", { name: "Cargando" })).toBeInTheDocument();
    expect(await screen.findByText("Ana Pérez")).toBeInTheDocument();
    expect(filas()).toHaveLength(2);
    expect(screen.getByRole("combobox", { name: "Estado" })).toHaveValue("activas");
    expect(pedidos).toEqual([null]);
  });

  it("indica por fila si el Cliente es menor y si tiene ficha médica", async () => {
    listadoPorEstado();
    renderApp("/clientes");
    const ana = (await screen.findByText("Ana Pérez")).closest("tr")!;
    const beto = screen.getByText("Beto Pérez").closest("tr")!;
    expect(within(ana).queryByText("Menor")).not.toBeInTheDocument();
    expect(within(beto).getByText("Menor")).toBeInTheDocument();
    expect(await within(ana).findByText("Con ficha médica")).toBeInTheDocument();
    expect(await within(beto).findByText("Sin ficha médica")).toBeInTheDocument();
  });

  it("no descarga ni muestra el contenido de la ficha médica", async () => {
    listadoPorEstado();
    let descargas = 0;
    server.use(
      http.get("*/api/clientes/:id/ficha-medica", () => {
        descargas++;
        return HttpResponse.text("CONTENIDO SECRETO");
      }),
    );
    renderApp("/clientes");
    await screen.findByText("Con ficha médica");
    expect(descargas).toBe(0);
    expect(screen.queryByText(/CONTENIDO SECRETO/)).not.toBeInTheDocument();
  });

  it("el selector de estado recarga desde la API", async () => {
    const pedidos = listadoPorEstado();
    const { usuario } = renderApp("/clientes");
    await screen.findByText("Ana Pérez");
    await usuario.selectOptions(screen.getByRole("combobox", { name: "Estado" }), "bajas");
    expect(await screen.findByText("Zoe Pérez")).toBeInTheDocument();
    expect(screen.queryByText("Ana Pérez")).not.toBeInTheDocument();
    await usuario.selectOptions(screen.getByRole("combobox", { name: "Estado" }), "todas");
    expect(await screen.findByText("Ana Pérez")).toBeInTheDocument();
    expect(screen.getByText("Zoe Pérez")).toBeInTheDocument();
    expect(pedidos).toEqual([null, "bajas", "todas"]);
  });

  it("muestra estado vacío cuando no hay Clientes", async () => {
    server.use(http.get("*/api/clientes", () => HttpResponse.json([])));
    renderApp("/clientes");
    expect(await screen.findByText("Todavía no hay Clientes activos.")).toBeInTheDocument();
  });

  it("da de baja un Cliente activo y confirma con un aviso", async () => {
    listadoPorEstado();
    let baja = 0;
    server.use(
      http.post("*/api/clientes/1/baja", () => {
        baja++;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const { usuario } = renderApp("/clientes");
    const fila = (await screen.findByText("Ana Pérez")).closest("tr")!;
    expect(within(fila).queryByRole("button", { name: /Reactivar/ })).not.toBeInTheDocument();
    await usuario.click(within(fila).getByRole("button", { name: "Dar de baja Ana Pérez" }));
    expect(await screen.findByRole("status", { name: "Notificación" })).toHaveTextContent("Cliente dado de baja");
    expect(baja).toBe(1);
  });

  it("reactiva un Cliente de baja y solo ofrece esa acción", async () => {
    listadoPorEstado();
    let reactivado = 0;
    server.use(
      http.post("*/api/clientes/9/reactivar", () => {
        reactivado++;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const { usuario } = renderApp("/clientes");
    await screen.findByText("Ana Pérez");
    await usuario.selectOptions(screen.getByRole("combobox", { name: "Estado" }), "bajas");
    const fila = (await screen.findByText("Zoe Pérez")).closest("tr")!;
    expect(within(fila).queryByRole("button", { name: /Dar de baja/ })).not.toBeInTheDocument();
    await usuario.click(within(fila).getByRole("button", { name: "Reactivar Zoe Pérez" }));
    expect(await screen.findByRole("status", { name: "Notificación" })).toHaveTextContent("Cliente reactivado");
    expect(reactivado).toBe(1);
  });

  it("no guarda datos de Clientes en el almacenamiento del navegador", async () => {
    listadoPorEstado();
    const escritura = vi.spyOn(Storage.prototype, "setItem");
    localStorage.clear();
    sessionStorage.clear();
    renderApp("/clientes");
    await screen.findByText("Con ficha médica");
    expect(escritura).not.toHaveBeenCalled();
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
    escritura.mockRestore();
  });

  describe("errores", () => {
    it("una falla de red al cargar se distingue y permite reintentar", async () => {
      let servidorCaido = true; // no depende del orden de llegada: un pedido rezagado de otro test no lo altera
      server.use(
        http.get("*/api/clientes", () => {
          return servidorCaido ? HttpResponse.error() : HttpResponse.json(ACTIVOS);
        }),
        http.head("*/api/clientes/:id/ficha-medica", () => new HttpResponse(null, { status: 404 })),
      );
      const { usuario } = renderApp("/clientes");
      const aviso = await screen.findByRole("alert");
      expect(aviso).toHaveTextContent("No se pudo conectar con el servidor");
      servidorCaido = false;
      await usuario.click(within(aviso).getByRole("button", { name: "Reintentar" }));
      expect(await screen.findByText("Ana Pérez")).toBeInTheDocument();
      expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    });

    it("un 500 al cargar muestra mensaje genérico sin detalles internos", async () => {
      server.use(
        http.get("*/api/clientes", () =>
          HttpResponse.json(errorApi("ERROR_INTERNO", "NullPointerException en ClientesController"), { status: 500 }),
        ),
      );
      renderApp("/clientes");
      const aviso = await screen.findByRole("alert");
      expect(aviso).toHaveTextContent("Ocurrió un error inesperado");
      expect(aviso).not.toHaveTextContent("NullPointerException");
    });

    it("un 404 al dar de baja avisa que el Cliente ya no existe y recarga el listado", async () => {
      let cargas = 0;
      server.use(
        http.get("*/api/clientes", () => {
          cargas++;
          return HttpResponse.json(cargas === 1 ? ACTIVOS : ACTIVOS.slice(1));
        }),
        http.head("*/api/clientes/:id/ficha-medica", () => new HttpResponse(null, { status: 404 })),
        http.post("*/api/clientes/1/baja", () =>
          HttpResponse.json(errorApi("NO_ENCONTRADO", "Cliente 1 no existe"), { status: 404 }),
        ),
      );
      const { usuario } = renderApp("/clientes");
      await usuario.click(await screen.findByRole("button", { name: "Dar de baja Ana Pérez" }));
      expect(await screen.findByRole("status", { name: "Notificación" })).toHaveTextContent("El recurso ya no existe");
      await waitFor(() => expect(screen.queryByText("Ana Pérez")).not.toBeInTheDocument());
    });
  });
});
