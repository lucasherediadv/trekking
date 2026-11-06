import { screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { server } from "../../test/servidor";
import { renderApp } from "../../test/renderApp";

const ruta = (id: number, nombre: string, extra: Record<string, unknown> = {}) => ({
  id,
  nombre,
  altitudMaxima: 3200,
  tipoTerreno: "Rocoso",
  dificultadTecnica: "Media",
  dificultadFisica: "Alta",
  activa: true,
  cantidadCheckpoints: 4,
  borrador: false,
  ...extra,
});

const RUTAS = [ruta(1, "Cerro Uno"), ruta(2, "Laguna Borrador", { borrador: true, cantidadCheckpoints: 0 })];

function api() {
  const peticiones: unknown[] = [];
  server.use(
    http.get("*/api/rutas", () => HttpResponse.json(RUTAS)),
    http.get("*/api/salidas", () => HttpResponse.json([])),
    http.get("*/api/dashboard/indicadores-salidas", () => HttpResponse.json({})),
    http.get("*/api/dashboard/proximas-salidas", () => HttpResponse.json([])),
    http.post("*/api/salidas", async ({ request }) => {
      peticiones.push(await request.json());
      return HttpResponse.json({ id: 9 }, { status: 201 });
    }),
  );
  return peticiones;
}

const indicador = () => screen.getByRole("navigation", { name: "Pasos del asistente" });
const paso = (nombre: string) => within(indicador()).getByRole("button", { name: new RegExp(`^\\d\\. ${nombre}`) });

type Usuario = ReturnType<typeof renderApp>["usuario"];

async function completarDatos(usuario: Usuario) {
  await usuario.type(screen.getByLabelText("Fecha"), "2026-12-05");
}

async function elegirRuta(usuario: Usuario) {
  await usuario.selectOptions(await screen.findByLabelText("Ruta"), "Cerro Uno");
}

describe("Asistente de Salida", () => {
  it("crea una Salida básica en una única petición y vuelve al listado con un toast", async () => {
    const peticiones = api();
    const { usuario } = renderApp("/salidas/nueva");
    expect(await screen.findByRole("heading", { name: "Datos de la salida" })).toBeInTheDocument();
    await completarDatos(usuario);
    await usuario.click(screen.getByRole("button", { name: "Siguiente" }));
    await elegirRuta(usuario);
    await usuario.click(screen.getByRole("button", { name: "Siguiente" }));
    await usuario.click(screen.getByRole("button", { name: "Siguiente" }));
    await usuario.click(screen.getByRole("button", { name: "Siguiente" }));

    expect(screen.getByRole("heading", { name: "Revisión" })).toBeInTheDocument();
    const revision = screen.getByRole("main");
    expect(revision).toHaveTextContent("05/12/2026");
    expect(revision).toHaveTextContent("Cerro Uno");
    await usuario.click(screen.getByRole("button", { name: "Confirmar Salida" }));

    expect(await screen.findByRole("status", { name: "Notificación" })).toHaveTextContent("Salida creada");
    expect(screen.getByRole("heading", { name: "Salidas" })).toBeInTheDocument();
    expect(peticiones).toEqual([
      {
        fecha: "2026-12-05",
        rutaId: 1,
        equipamiento: [],
        elementosNuevos: [],
        clientesExistentes: [],
        clientesNuevos: [],
      },
    ]);
  });

  it("el indicador habilita los pasos según las reglas de navegación y bloquea sin diálogos", async () => {
    api();
    const { usuario } = renderApp("/salidas/nueva");
    await screen.findByRole("heading", { name: "Datos de la salida" });
    for (const p of ["Ruta", "Equipamiento", "Participantes", "Revisión"]) expect(paso(p)).toBeDisabled();
    expect(screen.getByRole("button", { name: "Siguiente" })).toBeDisabled();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();

    await completarDatos(usuario);
    expect(paso("Ruta")).toBeEnabled();
    for (const p of ["Equipamiento", "Participantes", "Revisión"]) expect(paso(p)).toBeDisabled();

    await usuario.click(paso("Ruta"));
    await elegirRuta(usuario);
    for (const p of ["Equipamiento", "Participantes", "Revisión"]) expect(paso(p)).toBeEnabled();

    // Opcionales: completados solo una vez visitados, aunque estén vacíos.
    expect(paso("Equipamiento")).not.toHaveTextContent("Completado");
    await usuario.click(paso("Equipamiento"));
    expect(screen.getByRole("heading", { name: "Equipamiento" })).toBeInTheDocument();
    expect(paso("Equipamiento")).toHaveTextContent("Completado");
    expect(paso("Participantes")).not.toHaveTextContent("Completado");

    // Saltar a Revisión sin recorrer todo y volver atrás conservando lo ingresado.
    await usuario.click(paso("Revisión"));
    expect(screen.getByRole("heading", { name: "Revisión" })).toBeInTheDocument();
    await usuario.click(paso("Datos"));
    expect(screen.getByLabelText("Fecha")).toHaveValue("2026-12-05");
    await usuario.click(paso("Ruta"));
    expect(screen.getByLabelText("Ruta")).toHaveValue("1");
  });

  it("invalidar un dato obligatorio completado vuelve a bloquear los pasos posteriores", async () => {
    api();
    const { usuario } = renderApp("/salidas/nueva");
    await completarDatos(usuario);
    await usuario.click(paso("Ruta"));
    await elegirRuta(usuario);
    expect(paso("Revisión")).toBeEnabled();

    await usuario.click(paso("Datos"));
    await usuario.clear(screen.getByLabelText("Fecha"));
    for (const p of ["Ruta", "Equipamiento", "Participantes", "Revisión"]) expect(paso(p)).toBeDisabled();
  });

  it("el paso Ruta ofrece solo Rutas existentes que no son borrador", async () => {
    api();
    const { usuario } = renderApp("/salidas/nueva");
    await completarDatos(usuario);
    await usuario.click(paso("Ruta"));
    const selector = await screen.findByLabelText("Ruta");
    const opciones = within(selector).getAllByRole("option").map((o) => o.textContent);
    expect(opciones).toContain("Cerro Uno");
    expect(opciones).not.toContain("Laguna Borrador");
  });

  it("una recarga reinicia el asistente: una URL de paso posterior vuelve a Datos", async () => {
    api();
    renderApp("/salidas/nueva/revision");
    expect(await screen.findByRole("heading", { name: "Datos de la salida" })).toBeInTheDocument();
    expect(screen.getByLabelText("Fecha")).toHaveValue("");
  });

  it("al confirmar, un 422 o un 409 se muestra sin perder lo ingresado", async () => {
    api();
    const { usuario } = renderApp("/salidas/nueva");
    await completarDatos(usuario);
    await usuario.click(paso("Ruta"));
    await elegirRuta(usuario);
    await usuario.click(paso("Revisión"));

    for (const [estado, mensaje] of [
      [422, "La fecha no puede ser pasada"],
      [409, "Stock insuficiente para Carpa: disponible 1"],
    ] as const) {
      server.use(
        http.post("*/api/salidas", () =>
          HttpResponse.json({ codigo: "NEGOCIO", mensaje, errores: [] }, { status: estado }),
        ),
      );
      await usuario.click(screen.getByRole("button", { name: "Confirmar Salida" }));
      expect(await screen.findByRole("alert")).toHaveTextContent(mensaje);
      expect(screen.getByRole("heading", { name: "Revisión" })).toBeInTheDocument();
      expect(screen.getByRole("main")).toHaveTextContent("Cerro Uno");
    }
    await usuario.click(paso("Datos"));
    expect(screen.getByLabelText("Fecha")).toHaveValue("2026-12-05");
  });

  it("con datos ingresados pide confirmación al abandonar; sin datos no", async () => {
    api();
    const vacio = renderApp("/salidas/nueva");
    await screen.findByRole("heading", { name: "Datos de la salida" });
    const menu = () => screen.getByRole("navigation", { name: "Menú principal" });
    await vacio.usuario.click(within(menu()).getByRole("link", { name: "Rutas" }));
    expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();
    expect(await screen.findByRole("heading", { name: "Rutas" })).toBeInTheDocument();
  });

  it("con datos ingresados, abandonar pide confirmar y permite seguir o salir", async () => {
    api();
    const { usuario } = renderApp("/salidas/nueva");
    await completarDatos(usuario);
    const enlace = () =>
      within(screen.getByRole("navigation", { name: "Menú principal" })).getByRole("link", { name: "Rutas" });

    await usuario.click(enlace());
    const dialogo = await screen.findByRole("alertdialog");
    await usuario.click(within(dialogo).getByRole("button", { name: "Seguir en el asistente" }));
    expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();
    expect(screen.getByLabelText("Fecha")).toHaveValue("2026-12-05");

    await usuario.click(enlace());
    await usuario.click(within(await screen.findByRole("alertdialog")).getByRole("button", { name: "Abandonar" }));
    expect(await screen.findByRole("heading", { name: "Rutas" })).toBeInTheDocument();
  });
});
