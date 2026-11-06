import { screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { server } from "../test/servidor";
import { renderApp } from "../test/renderApp";

const ruta = (id: number, nombre: string, extra: Record<string, unknown> = {}) => ({
  id,
  nombre,
  altitudMaxima: 3000,
  tipoTerreno: "Rocoso",
  dificultadTecnica: "Media",
  dificultadFisica: "Alta",
  activa: true,
  cantidadCheckpoints: 4,
  borrador: false,
  ...extra,
});

const ACTIVAS = [
  ruta(1, "Cerro Uno"),
  ruta(2, "Laguna Dos", { tipoTerreno: "Boscoso", dificultadTecnica: "Baja", dificultadFisica: "Baja" }),
  ruta(3, "Sendero Tres", { borrador: true, cantidadCheckpoints: 0, tipoTerreno: "Sendero" }),
];
const BAJAS = [ruta(9, "Vieja Nueve", { activa: false })];

function listadoPorEstado() {
  const pedidos: (string | null)[] = [];
  server.use(
    http.get("*/api/rutas", ({ request }) => {
      const estado = new URL(request.url).searchParams.get("estado");
      pedidos.push(estado);
      if (estado === "bajas") return HttpResponse.json(BAJAS);
      if (estado === "todas") return HttpResponse.json([...ACTIVAS, ...BAJAS]);
      return HttpResponse.json(ACTIVAS);
    }),
  );
  return pedidos;
}

const filas = () => screen.getAllByRole("row").slice(1);
const errorApi = (codigo: string, mensaje: string) => ({ codigo, mensaje });

describe("listado de Rutas", () => {
  it("muestra carga y luego las Rutas activas por defecto", async () => {
    const pedidos = listadoPorEstado();
    renderApp("/rutas");
    expect(screen.getByRole("status", { name: "Cargando" })).toBeInTheDocument();
    expect(await screen.findByText("Cerro Uno")).toBeInTheDocument();
    expect(filas()).toHaveLength(3);
    expect(screen.getByRole("combobox", { name: "Estado" })).toHaveValue("activas");
    expect(pedidos).toEqual([null]);
  });

  it("distingue visiblemente una Ruta borrador", async () => {
    listadoPorEstado();
    renderApp("/rutas");
    const fila = (await screen.findByText("Sendero Tres")).closest("tr")!;
    expect(within(fila).getByText("Borrador")).toBeInTheDocument();
    const otra = screen.getByText("Cerro Uno").closest("tr")!;
    expect(within(otra).queryByText("Borrador")).not.toBeInTheDocument();
  });

  it("el selector de estado recarga desde la API", async () => {
    const pedidos = listadoPorEstado();
    const { usuario } = renderApp("/rutas");
    await screen.findByText("Cerro Uno");
    await usuario.selectOptions(screen.getByRole("combobox", { name: "Estado" }), "bajas");
    expect(await screen.findByText("Vieja Nueve")).toBeInTheDocument();
    expect(screen.queryByText("Cerro Uno")).not.toBeInTheDocument();
    await usuario.selectOptions(screen.getByRole("combobox", { name: "Estado" }), "todas");
    expect(await screen.findByText("Cerro Uno")).toBeInTheDocument();
    expect(screen.getByText("Vieja Nueve")).toBeInTheDocument();
    expect(pedidos).toEqual([null, "bajas", "todas"]);
  });

  it("filtra en el cliente por terreno y dificultades sin nuevas peticiones", async () => {
    const pedidos = listadoPorEstado();
    const { usuario } = renderApp("/rutas");
    await screen.findByText("Cerro Uno");

    await usuario.selectOptions(screen.getByRole("combobox", { name: "Terreno" }), "Boscoso");
    expect(filas()).toHaveLength(1);
    expect(screen.getByText("Laguna Dos")).toBeInTheDocument();

    await usuario.selectOptions(screen.getByRole("combobox", { name: "Terreno" }), "Todos");
    await usuario.selectOptions(screen.getByRole("combobox", { name: "Dificultad técnica" }), "Media");
    expect(screen.queryByText("Laguna Dos")).not.toBeInTheDocument();
    await usuario.selectOptions(screen.getByRole("combobox", { name: "Dificultad física" }), "Baja");
    expect(screen.getByText("No hay Rutas que coincidan con los filtros.")).toBeInTheDocument();
    expect(pedidos).toEqual([null]);
  });

  it("muestra estado vacío cuando no hay Rutas", async () => {
    server.use(http.get("*/api/rutas", () => HttpResponse.json([])));
    renderApp("/rutas");
    expect(await screen.findByText("Todavía no hay Rutas activas.")).toBeInTheDocument();
  });

  it("da de baja una Ruta activa y confirma con un aviso", async () => {
    listadoPorEstado();
    let baja = 0;
    server.use(
      http.post("*/api/rutas/1/baja", () => {
        baja++;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const { usuario } = renderApp("/rutas");
    const fila = (await screen.findByText("Cerro Uno")).closest("tr")!;
    expect(within(fila).queryByRole("button", { name: /Reactivar/ })).not.toBeInTheDocument();
    await usuario.click(within(fila).getByRole("button", { name: "Dar de baja Cerro Uno" }));
    expect(await screen.findByRole("status", { name: "Notificación" })).toHaveTextContent("Ruta dada de baja");
    expect(baja).toBe(1);
  });

  it("reactiva una Ruta de baja y solo ofrece esa acción", async () => {
    listadoPorEstado();
    let reactivada = 0;
    server.use(
      http.post("*/api/rutas/9/reactivar", () => {
        reactivada++;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const { usuario } = renderApp("/rutas");
    await screen.findByText("Cerro Uno");
    await usuario.selectOptions(screen.getByRole("combobox", { name: "Estado" }), "bajas");
    const fila = (await screen.findByText("Vieja Nueve")).closest("tr")!;
    expect(within(fila).queryByRole("button", { name: /Dar de baja/ })).not.toBeInTheDocument();
    await usuario.click(within(fila).getByRole("button", { name: "Reactivar Vieja Nueve" }));
    expect(await screen.findByRole("status", { name: "Notificación" })).toHaveTextContent("Ruta reactivada");
    expect(reactivada).toBe(1);
  });

  it("los colores de estado siguen los del escritorio", async () => {
    listadoPorEstado();
    renderApp("/rutas");
    const laguna = (await screen.findByText("Laguna Dos")).closest("tr")!;
    const [tecnica] = within(laguna).getAllByText("Baja");
    expect(tecnica).toHaveClass("estado-exito");
    const cerro = screen.getByText("Cerro Uno").closest("tr")!;
    expect(within(cerro).getByText("Media")).toHaveClass("estado-advertencia");
    expect(within(cerro).getByText("Alta")).toHaveClass("estado-peligro");
  });

  describe("errores", () => {
    it("una falla de red al cargar se distingue y permite reintentar", async () => {
      let intentos = 0;
      server.use(
        http.get("*/api/rutas", () => {
          intentos++;
          return intentos === 1 ? HttpResponse.error() : HttpResponse.json(ACTIVAS);
        }),
      );
      const { usuario } = renderApp("/rutas");
      const aviso = await screen.findByRole("alert");
      expect(aviso).toHaveTextContent("No se pudo conectar con el servidor");
      await usuario.click(within(aviso).getByRole("button", { name: "Reintentar" }));
      expect(await screen.findByText("Cerro Uno")).toBeInTheDocument();
      expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    });

    it("un 500 al cargar muestra mensaje genérico sin detalles internos", async () => {
      server.use(
        http.get("*/api/rutas", () =>
          HttpResponse.json(errorApi("ERROR_INTERNO", "NullPointerException en RutasController"), { status: 500 }),
        ),
      );
      renderApp("/rutas");
      const aviso = await screen.findByRole("alert");
      expect(aviso).toHaveTextContent("Ocurrió un error inesperado");
      expect(aviso).not.toHaveTextContent("NullPointerException");
      expect(aviso).not.toHaveTextContent("No se pudo conectar");
      expect(within(aviso).getByRole("button", { name: "Reintentar" })).toBeInTheDocument();
    });

    it("un 400 muestra mensaje genérico", async () => {
      server.use(
        http.get("*/api/rutas", () =>
          HttpResponse.json(errorApi("PETICION_INVALIDA", "detalle interno"), { status: 400 }),
        ),
      );
      renderApp("/rutas");
      const aviso = await screen.findByRole("alert");
      expect(aviso).toHaveTextContent("La solicitud no es válida");
      expect(aviso).not.toHaveTextContent("detalle interno");
    });

    it("un 404 al dar de baja avisa que la Ruta ya no existe y recarga el listado", async () => {
      let cargas = 0;
      server.use(
        http.get("*/api/rutas", () => {
          cargas++;
          return HttpResponse.json(cargas === 1 ? ACTIVAS : ACTIVAS.slice(1));
        }),
        http.post("*/api/rutas/1/baja", () =>
          HttpResponse.json(errorApi("NO_ENCONTRADO", "Ruta 1 no existe"), { status: 404 }),
        ),
      );
      const { usuario } = renderApp("/rutas");
      await usuario.click(await screen.findByRole("button", { name: "Dar de baja Cerro Uno" }));
      expect(await screen.findByRole("status", { name: "Notificación" })).toHaveTextContent(
        "El recurso ya no existe",
      );
      await waitFor(() => expect(screen.queryByText("Cerro Uno")).not.toBeInTheDocument());
    });

    it("un 500 y una falla de red al dar de baja muestran avisos distintos", async () => {
      listadoPorEstado();
      let intento = 0;
      server.use(
        http.post("*/api/rutas/1/baja", () => {
          intento++;
          return intento === 1
            ? HttpResponse.json(errorApi("ERROR_INTERNO", "stacktrace"), { status: 500 })
            : HttpResponse.error();
        }),
      );
      const { usuario } = renderApp("/rutas");
      await usuario.click(await screen.findByRole("button", { name: "Dar de baja Cerro Uno" }));
      const toast = await screen.findByRole("status", { name: "Notificación" });
      expect(toast).toHaveTextContent("Ocurrió un error inesperado");
      expect(toast).not.toHaveTextContent("stacktrace");
      await usuario.click(screen.getByRole("button", { name: "Dar de baja Cerro Uno" }));
      await waitFor(() =>
        expect(screen.getByRole("status", { name: "Notificación" })).toHaveTextContent("No se pudo conectar"),
      );
    });
  });
});
