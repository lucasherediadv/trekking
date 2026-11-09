import { screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { server } from "../test/servidor";
import { renderApp } from "../test/renderApp";

const SALIDA = {
  id: 1,
  rutaId: 1,
  fecha: "2026-11-10",
  estado: "PLANIFICADA",
  rutaNombre: "Cerro Uno",
};

const COPIA = {
  fecha: "2026-11-10",
  ruta: {
    id: 1,
    nombre: "Cerro Uno",
    altitudMaxima: 3200,
    tipoTerreno: "Montaña",
    dificultadTecnica: "Media",
    dificultadFisica: "Alta",
  },
  checkpoints: [
    { id: 1, nombre: "Refugio", hora: "09:30", altitud: 2100, descripcion: "Descanso" },
    { id: 2, nombre: "Cima", hora: "13:00", altitud: 3200, descripcion: "" },
  ],
  equipamiento: [{ elementoId: 7, cantidad: 2 }],
  participantes: [{ id: 3, dni: "30111222", nombre: "Ana", apellido: "Pérez", fechaNacimiento: "1990-05-01", telefono: "555-1234" }],
};

const INVENTARIO = [{ id: 7, nombre: "Carpa", categoria: "Refugio", cantidad: 5 }];

function apiFeliz(salida: Record<string, unknown> = SALIDA) {
  server.use(
    http.get("*/api/salidas/1", () => HttpResponse.json(salida)),
    http.get("*/api/salidas/1/copia-operativa", () => HttpResponse.json(COPIA)),
    http.get("*/api/inventario", () => HttpResponse.json(INVENTARIO)),
    http.get("*/api/salidas", () => HttpResponse.json([{ ...SALIDA, participantes: "", equipamiento: "", vencida: false }])),
    http.get("*/api/dashboard/indicadores-salidas", () => HttpResponse.json({})),
    http.get("*/api/dashboard/proximas-salidas", () => HttpResponse.json([])),
  );
}

describe("detalle de Salida", () => {
  it("se llega desde el listado y muestra datos, estado y Copia operativa con breadcrumbs", async () => {
    apiFeliz();
    const { usuario, router } = renderApp("/salidas");
    await usuario.click(await screen.findByRole("link", { name: "Ver Salida de Cerro Uno" }));
    expect(router.state.location.pathname).toBe("/salidas/1");

    expect(await screen.findByRole("heading", { name: "Salida de Cerro Uno" })).toBeInTheDocument();
    const migas = screen.getByRole("navigation", { name: "Breadcrumbs" });
    expect(within(migas).getByRole("link", { name: "Salidas" })).toHaveAttribute("href", "/salidas");

    expect(screen.getByText(/10\/11\/2026/)).toBeInTheDocument();
    expect(screen.getByText("Planificada")).toHaveClass("estado-informacion");

    const ruta = screen.getByRole("region", { name: "Ruta" });
    expect(ruta).toHaveTextContent("3200");
    expect(ruta).toHaveTextContent("Montaña");
    expect(ruta).toHaveTextContent("Media");
    expect(ruta).toHaveTextContent("Alta");

    const checkpoints = screen.getByRole("region", { name: "Checkpoints" });
    expect(checkpoints).toHaveTextContent("Refugio");
    expect(checkpoints).toHaveTextContent("09:30");
    expect(checkpoints).toHaveTextContent("Cima");

    const equipo = screen.getByRole("region", { name: "Equipamiento" });
    expect(equipo).toHaveTextContent("Carpa");
    expect(equipo).toHaveTextContent("2");

    const participantes = screen.getByRole("region", { name: "Participantes" });
    expect(participantes).toHaveTextContent("Ana");
    expect(participantes).toHaveTextContent("Pérez");
    expect(participantes).toHaveTextContent("30111222");
  });

  it("descarga la Ficha operativa solo al pedirlo, con el nombre que indica la API", async () => {
    apiFeliz();
    let pedidas = 0;
    server.use(
      http.get("*/api/salidas/1/ficha-operativa", () => {
        pedidas++;
        return new HttpResponse(new Uint8Array([37, 80, 68, 70]), {
          headers: {
            "Content-Type": "application/pdf",
            "Content-Disposition": "attachment; filename*=UTF-8''Cerro-Uno-2026-11-10-salida-1.pdf",
          },
        });
      }),
    );
    const crear = vi.fn(() => "blob:ficha");
    const revocar = vi.fn();
    Object.assign(URL, { createObjectURL: crear, revokeObjectURL: revocar });
    const clics: string[] = [];
    vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(function (this: HTMLAnchorElement) {
      clics.push(this.download);
    });

    const { usuario } = renderApp("/salidas/1");
    const boton = await screen.findByRole("button", { name: "Descargar Ficha operativa" });
    expect(pedidas).toBe(0);
    await usuario.click(boton);
    await waitFor(() => expect(clics).toEqual(["Cerro-Uno-2026-11-10-salida-1.pdf"]));
    expect(pedidas).toBe(1);
    expect(crear).toHaveBeenCalledTimes(1);
    expect(revocar).toHaveBeenCalledWith("blob:ficha");
    expect(screen.queryByRole("link", { name: /Ficha operativa/ })).not.toBeInTheDocument();
    vi.restoreAllMocks();
  });

  it("un error al descargar la Ficha se informa con un aviso", async () => {
    apiFeliz();
    server.use(http.get("*/api/salidas/1/ficha-operativa", () => HttpResponse.error()));
    const { usuario } = renderApp("/salidas/1");
    await usuario.click(await screen.findByRole("button", { name: "Descargar Ficha operativa" }));
    expect(await screen.findByRole("status", { name: "Notificación" })).toHaveTextContent("No se pudo conectar");
  });

  it("no ofrece ver ni descargar adjuntos médicos de los Participantes", async () => {
    apiFeliz();
    renderApp("/salidas/1");
    await screen.findByRole("region", { name: "Participantes" });
    expect(screen.queryByText(/ficha médica|adjunto/i)).not.toBeInTheDocument();
    expect(document.querySelector('a[href*="ficha-medica"]')).toBeNull();
  });

  it("un 404 avisa y regresa al listado", async () => {
    apiFeliz();
    server.use(
      http.get("*/api/salidas/99", () => HttpResponse.json({ codigo: "NO_ENCONTRADO", mensaje: "no" }, { status: 404 })),
      http.get("*/api/salidas/99/copia-operativa", () =>
        HttpResponse.json({ codigo: "NO_ENCONTRADO", mensaje: "no" }, { status: 404 }),
      ),
    );
    const { router } = renderApp("/salidas/99");
    expect(await screen.findByRole("status", { name: "Notificación" })).toHaveTextContent("El recurso ya no existe.");
    await waitFor(() => expect(router.state.location.pathname).toBe("/salidas"));
    expect(await screen.findByRole("heading", { name: "Salidas" })).toBeInTheDocument();
  });
});
