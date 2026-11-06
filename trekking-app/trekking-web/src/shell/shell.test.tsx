import { screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { server } from "../test/servidor";
import { renderApp } from "../test/renderApp";

beforeEach(() =>
  server.use(
    http.get("*/api/rutas", () => HttpResponse.json([])),
    http.get("*/api/salidas", () => HttpResponse.json([])),
    http.get("*/api/dashboard/indicadores-salidas", () => HttpResponse.json({})),
    http.get("*/api/dashboard/proximas-salidas", () => HttpResponse.json([])),
  ),
);

describe("shell de la aplicación", () => {
  it("la raíz redirige a Salidas y marca la sección activa", () => {
    renderApp("/");
    const menu = screen.getByRole("navigation", { name: "Menú principal" });
    expect(within(menu).getByRole("link", { name: "Salidas" })).toHaveAttribute("aria-current", "page");
    expect(screen.getByRole("heading", { name: "Salidas" })).toBeInTheDocument();
  });

  it("el menú muestra la marca y las cuatro secciones", () => {
    renderApp("/rutas");
    const menu = screen.getByRole("navigation", { name: "Menú principal" });
    expect(screen.getByText("TrekRisk")).toBeInTheDocument();
    for (const seccion of ["Salidas", "Rutas", "Inventario", "Clientes"]) {
      expect(within(menu).getByRole("link", { name: seccion })).toBeInTheDocument();
    }
  });

  it("navega entre secciones desde el menú", async () => {
    const { usuario } = renderApp("/rutas");
    await usuario.click(screen.getByRole("link", { name: "Clientes" }));
    expect(screen.getByRole("heading", { name: "Clientes" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Clientes" })).toHaveAttribute("aria-current", "page");
  });

  it("se contrae a iconos con tooltips y vuelve a expandirse", async () => {
    const { usuario } = renderApp("/rutas");
    await usuario.click(screen.getByRole("button", { name: "Contraer menú" }));
    expect(screen.queryByText("TrekRisk")).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Inventario" })).toHaveAttribute("title", "Inventario");
    await usuario.click(screen.getByRole("button", { name: "Expandir menú" }));
    expect(screen.getByText("TrekRisk")).toBeInTheDocument();
  });

  it("las pantallas secundarias muestran breadcrumbs que permiten volver", async () => {
    const { usuario } = renderApp("/rutas/nueva");
    const migas = screen.getByRole("navigation", { name: "Breadcrumbs" });
    expect(within(migas).getByText("Nueva ruta")).toHaveAttribute("aria-current", "page");
    await usuario.click(within(migas).getByRole("link", { name: "Rutas" }));
    expect(screen.getByRole("heading", { name: "Rutas" })).toBeInTheDocument();
    expect(screen.queryByRole("navigation", { name: "Breadcrumbs" })).not.toBeInTheDocument();
  });

  it("una URL inexistente avisa y ofrece volver", async () => {
    const { usuario } = renderApp("/no-existe");
    expect(screen.getByRole("heading", { name: "Página no encontrada" })).toBeInTheDocument();
    await usuario.click(screen.getByRole("link", { name: "Volver al inicio" }));
    expect(screen.getByRole("heading", { name: "Salidas" })).toBeInTheDocument();
  });
});
