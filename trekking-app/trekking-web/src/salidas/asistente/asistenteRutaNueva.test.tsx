import { screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { server } from "../../test/servidor";
import { renderApp } from "../../test/renderApp";

const CERRO = {
  id: 1,
  nombre: "Cerro Uno",
  altitudMaxima: 3200,
  tipoTerreno: "Rocoso",
  dificultadTecnica: "Media",
  dificultadFisica: "Alta",
  activa: true,
  cantidadCheckpoints: 4,
  borrador: false,
};

type Usuario = ReturnType<typeof renderApp>["usuario"];

async function irAPasoRuta() {
  server.use(
    http.get("*/api/rutas", () => HttpResponse.json([CERRO])),
    http.get("*/api/salidas", () => HttpResponse.json([])),
  );
  const { usuario } = renderApp("/salidas/nueva");
  await usuario.type(await screen.findByLabelText("Fecha"), "2026-12-05");
  await usuario.click(screen.getByRole("button", { name: "Siguiente" }));
  await screen.findByLabelText("Ruta");
  return usuario;
}

async function completarNueva(usuario: Usuario, omitir: string[] = []) {
  await usuario.click(screen.getByRole("radio", { name: "Ruta nueva" }));
  const textos: [string, string][] = [
    ["Nombre", "Laguna Nueva"],
    ["Altitud máxima", "1200"],
  ];
  for (const [etiqueta, valor] of textos) {
    if (!omitir.includes(etiqueta)) await usuario.type(screen.getByLabelText(new RegExp(etiqueta)), valor);
  }
  const listas: [string, string][] = [
    ["Tipo de terreno", "Boscoso"],
    ["Dificultad técnica", "Baja"],
    ["Dificultad física", "Media"],
  ];
  for (const [etiqueta, valor] of listas) {
    if (!omitir.includes(etiqueta)) await usuario.selectOptions(screen.getByLabelText(new RegExp(etiqueta)), valor);
  }
}

const siguiente = () => screen.getByRole("button", { name: "Siguiente" });

describe("Asistente de Salida: Ruta nueva", () => {
  it("crea la Ruta nueva como borrador, lo explica y no permite avanzar", async () => {
    const usuario = await irAPasoRuta();
    let cuerpo: unknown;
    server.use(
      http.post("*/api/rutas", async ({ request }) => {
        cuerpo = await request.json();
        return HttpResponse.json({ ...CERRO, id: 7, nombre: "Laguna Nueva", borrador: true, cantidadCheckpoints: 0 }, { status: 201 });
      }),
    );
    await completarNueva(usuario);
    expect(screen.getByText(/queda como Ruta borrador/)).toBeInTheDocument();
    await usuario.click(screen.getByRole("button", { name: "Crear Ruta" }));

    const aviso = await screen.findByRole("status");
    expect(aviso).toHaveTextContent("Laguna Nueva");
    expect(aviso).toHaveTextContent(/Checkpoints/);
    expect(cuerpo).toEqual({
      nombre: "Laguna Nueva",
      altitudMaxima: 1200,
      tipoTerreno: "Boscoso",
      dificultadTecnica: "Baja",
      dificultadFisica: "Media",
    });
    expect(siguiente()).toBeDisabled();
  });

  it("con datos incompletos no crea la Ruta y bloquea los pasos posteriores", async () => {
    const usuario = await irAPasoRuta();
    const creadas: unknown[] = [];
    server.use(
      http.post("*/api/rutas", async ({ request }) => {
        creadas.push(await request.json());
        return HttpResponse.json({}, { status: 201 });
      }),
    );
    await completarNueva(usuario, ["Tipo de terreno"]);

    expect(screen.getByRole("button", { name: "Crear Ruta" })).toBeDisabled();
    expect(siguiente()).toBeDisabled();
    expect(within(screen.getByRole("navigation", { name: "Pasos del asistente" })).getByRole("button", { name: /Equipamiento/ })).toBeDisabled();
    expect(creadas).toEqual([]);
  });

  it("no permite crear la Ruta con una altitud negativa", async () => {
    const usuario = await irAPasoRuta();
    await completarNueva(usuario);
    await usuario.clear(screen.getByLabelText(/Altitud máxima/));
    await usuario.type(screen.getByLabelText(/Altitud máxima/), "-5");

    expect(screen.getByRole("button", { name: "Crear Ruta" })).toBeDisabled();
  });

  it("muestra por campo los errores 422 de la API", async () => {
    const usuario = await irAPasoRuta();
    server.use(
      http.post("*/api/rutas", () =>
        HttpResponse.json(
          { codigo: "VALIDACION", mensaje: "Datos inválidos", errores: [{ campo: "nombre", mensaje: "El nombre ya existe." }] },
          { status: 422 },
        ),
      ),
    );
    await completarNueva(usuario);
    await usuario.click(screen.getByRole("button", { name: "Crear Ruta" }));

    expect(await screen.findByText("El nombre ya existe.")).toBeInTheDocument();
    expect(screen.getByLabelText(/Nombre/)).toBeInvalid();
  });

  it("alterna entre Ruta existente y Ruta nueva conservando la elegida", async () => {
    const usuario = await irAPasoRuta();
    await usuario.selectOptions(screen.getByLabelText("Ruta"), "Cerro Uno");
    await usuario.click(screen.getByRole("radio", { name: "Ruta nueva" }));
    expect(screen.queryByLabelText("Ruta")).not.toBeInTheDocument();
    expect(screen.getByLabelText(/Nombre/)).toBeInTheDocument();

    await usuario.click(screen.getByRole("radio", { name: "Ruta existente" }));
    expect(screen.getByLabelText("Ruta")).toHaveValue("1");
    expect(siguiente()).toBeEnabled();
  });
});
