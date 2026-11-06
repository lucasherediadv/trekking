import { screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { server } from "../test/servidor";
import { renderApp } from "../test/renderApp";

const RUTA = {
  id: 7,
  nombre: "Cerro Siete",
  altitudMaxima: 3000,
  tipoTerreno: "Rocoso",
  dificultadTecnica: "Media",
  dificultadFisica: "Alta",
  activa: true,
  cantidadCheckpoints: 2,
  borrador: false,
};
const cp = (id: number, nombre: string, hora: string) => ({
  id,
  nombre,
  hora,
  latitud: -32.5,
  longitud: -69.9,
  altitud: 1500,
  descripcion: "",
});
const BASE = cp(1, "Base", "06:00");
const CUMBRE = cp(2, "Cumbre", "11:00");

function api(checkpoints = [BASE, CUMBRE], ruta: Record<string, unknown> = RUTA) {
  const guardados: unknown[] = [];
  server.use(
    http.get("*/api/rutas/7", () => HttpResponse.json(ruta)),
    http.get("*/api/rutas/7/itinerario", () => HttpResponse.json(checkpoints)),
    http.put("*/api/rutas/7/itinerario", async ({ request }) => {
      const cuerpo = await request.json();
      guardados.push(cuerpo);
      return HttpResponse.json({ ...RUTA, cantidadCheckpoints: (cuerpo as unknown[]).length, borrador: false });
    }),
  );
  return guardados;
}

const nombres = () =>
  screen.getAllByRole("listitem").map((li) => within(li).getByTestId("nombre-checkpoint").textContent);

const migaRutas = () =>
  within(screen.getByRole("navigation", { name: "Breadcrumbs" })).getByRole("link", { name: "Rutas" });

async function completarFormulario(usuario: ReturnType<typeof renderApp>["usuario"], v: Record<string, string>) {
  for (const [campo, valor] of Object.entries(v)) {
    const input = screen.getByLabelText(campo);
    await usuario.clear(input);
    await usuario.type(input, valor);
  }
}

describe("Itinerario de una Ruta", () => {
  it("muestra los Checkpoints en orden con breadcrumbs y URL propia", async () => {
    api();
    renderApp("/rutas/7/itinerario");
    expect(await screen.findByRole("heading", { name: "Itinerario · Cerro Siete" })).toBeInTheDocument();
    expect(nombres()).toEqual(["Base", "Cumbre"]);
    const migas = screen.getByRole("navigation", { name: "Breadcrumbs" });
    expect(within(migas).getByRole("link", { name: "Rutas" })).toBeInTheDocument();
    expect(within(migas).getByText("Itinerario")).toHaveAttribute("aria-current", "page");
  });

  it("se accede desde la fila de la Ruta en el listado", async () => {
    api();
    server.use(http.get("*/api/rutas", () => HttpResponse.json([RUTA])));
    const { usuario, router } = renderApp("/rutas");
    await usuario.click(await screen.findByRole("link", { name: "Itinerario de Cerro Siete" }));
    expect(router.state.location.pathname).toBe("/rutas/7/itinerario");
    expect(await screen.findByRole("heading", { name: /Itinerario · Cerro Siete/ })).toBeInTheDocument();
  });

  it("reordena, agrega, edita y quita; guarda todo en una sola petición", async () => {
    const guardados = api();
    const { usuario } = renderApp("/rutas/7/itinerario");
    await screen.findByText("Base");

    await usuario.click(screen.getByRole("button", { name: "Bajar Base" }));
    expect(nombres()).toEqual(["Cumbre", "Base"]);
    await usuario.click(screen.getByRole("button", { name: "Subir Base" }));
    expect(nombres()).toEqual(["Base", "Cumbre"]);

    await usuario.click(screen.getByRole("button", { name: "Agregar Checkpoint" }));
    await completarFormulario(usuario, {
      Nombre: "Refugio",
      Hora: "09:00",
      Latitud: "-32,6",
      Longitud: "-69.8",
      Altitud: "2000",
    });
    await usuario.click(screen.getByRole("button", { name: "Agregar" }));
    expect(nombres()).toEqual(["Base", "Cumbre", "Refugio"]);

    await usuario.click(screen.getByRole("button", { name: "Editar Base" }));
    await completarFormulario(usuario, { Nombre: "Base norte" });
    await usuario.click(screen.getByRole("button", { name: "Guardar cambios" }));
    await usuario.click(screen.getByRole("button", { name: "Quitar Cumbre" }));
    expect(nombres()).toEqual(["Base norte", "Refugio"]);

    await usuario.click(screen.getByRole("button", { name: "Guardar Itinerario" }));
    expect(await screen.findByRole("status", { name: "Notificación" })).toHaveTextContent("Itinerario guardado");
    expect(guardados).toHaveLength(1);
    expect(guardados[0]).toEqual([
      { id: 1, nombre: "Base norte", hora: "06:00", latitud: -32.5, longitud: -69.9, altitud: 1500, descripcion: "" },
      { nombre: "Refugio", hora: "09:00", latitud: -32.6, longitud: -69.8, altitud: 2000, descripcion: "" },
    ]);
  });

  it("valida cada campo del Checkpoint antes de agregarlo", async () => {
    api();
    const { usuario } = renderApp("/rutas/7/itinerario");
    await screen.findByText("Base");
    await usuario.click(screen.getByRole("button", { name: "Agregar Checkpoint" }));
    await usuario.click(screen.getByRole("button", { name: "Agregar" }));
    expect(screen.getByText("Ingresa el nombre del Checkpoint.")).toBeInTheDocument();
    expect(screen.getByText("Usa el formato de 24 horas HH:mm, por ejemplo 14:30.")).toBeInTheDocument();
    expect(nombres()).toEqual(["Base", "Cumbre"]);

    await completarFormulario(usuario, { Nombre: "X", Hora: "10:00", Latitud: "0", Longitud: "0", Altitud: "3500" });
    await usuario.click(screen.getByRole("button", { name: "Agregar" }));
    expect(screen.getByText("La altitud debe estar entre 0 y 3000 m.")).toBeInTheDocument();
    expect(nombres()).toEqual(["Base", "Cumbre"]);
  });

  it("muestra los errores del servidor (422) y conserva los cambios pendientes", async () => {
    api();
    server.use(
      http.put("*/api/rutas/7/itinerario", () =>
        HttpResponse.json(
          {
            codigo: "VALIDACION",
            mensaje: "Hay datos inválidos",
            errores: [{ campo: "checkpoints", mensaje: "Checkpoint 'Cumbre': La altitud debe estar entre 0 y 3000 m." }],
          },
          { status: 422 },
        ),
      ),
    );
    const { usuario } = renderApp("/rutas/7/itinerario");
    await screen.findByText("Base");
    await usuario.click(screen.getByRole("button", { name: "Quitar Base" }));
    await usuario.click(screen.getByRole("button", { name: "Guardar Itinerario" }));
    const aviso = await screen.findByRole("alert");
    expect(aviso).toHaveTextContent("Checkpoint 'Cumbre': La altitud debe estar entre 0 y 3000 m.");
    expect(nombres()).toEqual(["Cumbre"]);
  });

  it("una Ruta inexistente muestra que ya no existe", async () => {
    server.use(
      http.get("*/api/rutas/7", () => HttpResponse.json({ codigo: "NO_ENCONTRADO", mensaje: "x" }, { status: 404 })),
      http.get("*/api/rutas/7/itinerario", () =>
        HttpResponse.json({ codigo: "NO_ENCONTRADO", mensaje: "x" }, { status: 404 }),
      ),
    );
    renderApp("/rutas/7/itinerario");
    expect(await screen.findByRole("alert")).toHaveTextContent("El recurso ya no existe");
  });

  it("al guardar dos o más Checkpoints la Ruta deja de ser borrador en el listado", async () => {
    let borrador = true;
    const draft = { ...RUTA, borrador: true, cantidadCheckpoints: 0 };
    server.use(
      http.get("*/api/rutas", () => HttpResponse.json([borrador ? draft : { ...RUTA, cantidadCheckpoints: 2 }])),
      http.get("*/api/rutas/7", () => HttpResponse.json(draft)),
      http.get("*/api/rutas/7/itinerario", () => HttpResponse.json([])),
      http.put("*/api/rutas/7/itinerario", () => {
        borrador = false;
        return HttpResponse.json({ ...RUTA, borrador: false });
      }),
    );
    const { usuario } = renderApp("/rutas");
    expect(await screen.findByText("Borrador")).toBeInTheDocument();
    await usuario.click(screen.getByRole("link", { name: "Itinerario de Cerro Siete" }));
    expect(await screen.findByText(/no tiene Checkpoints/)).toBeInTheDocument();
    for (const [nombre, hora] of [
      ["Uno", "06:00"],
      ["Dos", "07:00"],
    ]) {
      await usuario.click(screen.getByRole("button", { name: "Agregar Checkpoint" }));
      await completarFormulario(usuario, { Nombre: nombre, Hora: hora, Latitud: "1", Longitud: "1", Altitud: "10" });
      await usuario.click(screen.getByRole("button", { name: "Agregar" }));
    }
    await usuario.click(screen.getByRole("button", { name: "Guardar Itinerario" }));
    await screen.findByRole("status", { name: "Notificación" });
    await usuario.click(migaRutas());
    expect(await screen.findByText(/2 Checkpoints/)).toBeInTheDocument();
    expect(screen.queryByText("Borrador")).not.toBeInTheDocument();
  });

  describe("cambios sin guardar", () => {
    async function conCambios() {
      const guardados = api();
      server.use(http.get("*/api/rutas", () => HttpResponse.json([RUTA])));
      const r = renderApp("/rutas/7/itinerario");
      await screen.findByText("Base");
      await r.usuario.click(screen.getByRole("button", { name: "Quitar Cumbre" }));
      return { ...r, guardados };
    }

    it("pide confirmación al salir y permite seguir editando", async () => {
      const { usuario, router } = await conCambios();
      await usuario.click(migaRutas());
      const dialogo = await screen.findByRole("dialog", { name: "Cambios sin guardar" });
      await usuario.click(within(dialogo).getByRole("button", { name: "Seguir editando" }));
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
      expect(router.state.location.pathname).toBe("/rutas/7/itinerario");
      expect(nombres()).toEqual(["Base"]);
    });

    it("descartar abandona la pantalla sin guardar", async () => {
      const { usuario, router, guardados } = await conCambios();
      await usuario.click(migaRutas());
      await usuario.click(within(await screen.findByRole("dialog")).getByRole("button", { name: "Descartar" }));
      await waitFor(() => expect(router.state.location.pathname).toBe("/rutas"));
      expect(guardados).toHaveLength(0);
    });

    it("guardar desde el aviso guarda y luego sale", async () => {
      const { usuario, router, guardados } = await conCambios();
      await usuario.click(migaRutas());
      await usuario.click(within(await screen.findByRole("dialog")).getByRole("button", { name: "Guardar" }));
      await waitFor(() => expect(router.state.location.pathname).toBe("/rutas"));
      expect(guardados).toHaveLength(1);
    });

    it("sin cambios sale sin preguntar", async () => {
      api();
      server.use(http.get("*/api/rutas", () => HttpResponse.json([RUTA])));
      const { usuario, router } = renderApp("/rutas/7/itinerario");
      await screen.findByText("Base");
      await usuario.click(migaRutas());
      await waitFor(() => expect(router.state.location.pathname).toBe("/rutas"));
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    });
  });
});
