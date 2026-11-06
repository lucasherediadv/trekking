import { screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { server } from "../test/servidor";
import { renderApp } from "../test/renderApp";

const listado = {
  id: 7,
  rutaId: 1,
  fecha: "2026-11-10",
  estado: "PLANIFICADA",
  rutaNombre: "Cerro Uno",
  participantes: "Ana Pérez",
  equipamiento: "2 Carpa Uno",
  vencida: false,
};

const CHECKPOINTS_COPIA = [{ id: 1, nombre: "Base", hora: "08:00", altitud: 1000 }];
const CHECKPOINTS_RUTA_DOS = [
  { id: 5, nombre: "Laguna", hora: "09:00", altitud: 1500 },
  { id: 6, nombre: "Cima Dos", hora: "12:00", altitud: 2000 },
];

const elemento = (id: number, nombre: string, disponible: number) => ({
  id,
  nombre,
  categoria: "Campamento",
  cantidad: 10,
  cantidadReservada: 0,
  cantidadDisponible: disponible,
  estado: "DISPONIBLE",
  estadoEtiqueta: "Disponible",
  activo: true,
  asignable: true,
});

const cliente = (id: number, nombre: string, apellido: string) => ({ id, dni: `3000000${id}`, nombre, apellido, activo: true });

function api(opciones: { editar?: () => Response | Promise<Response>; estados?: string[] } = {}) {
  const puts: unknown[] = [];
  const estados = opciones.estados ?? ["PLANIFICADA"];
  let lecturas = 0;
  server.use(
    http.get("*/api/salidas", () => HttpResponse.json([listado])),
    http.get("*/api/dashboard/indicadores-salidas", () => HttpResponse.json({})),
    http.get("*/api/dashboard/proximas-salidas", () => HttpResponse.json([])),
    http.get("*/api/salidas/7", () => {
      const estado = estados[Math.min(lecturas++, estados.length - 1)];
      return HttpResponse.json({ id: 7, rutaId: 1, fecha: "2026-11-10", estado, rutaNombre: "Cerro Uno" });
    }),
    http.get("*/api/salidas/7/copia-operativa", () =>
      HttpResponse.json({
        fecha: "2026-11-10",
        ruta: { id: 1, nombre: "Cerro Uno" },
        checkpoints: CHECKPOINTS_COPIA,
        equipamiento: [{ elementoId: 1, cantidad: 2 }],
        participantes: [{ id: 1, dni: "30000001", nombre: "Ana", apellido: "Pérez" }],
      }),
    ),
    http.get("*/api/salidas/7/elementos-para-editar", () =>
      HttpResponse.json([elemento(1, "Carpa Uno", 5), elemento(2, "Soga Dos", 3)]),
    ),
    http.get("*/api/clientes", () => HttpResponse.json([cliente(1, "Ana", "Pérez"), cliente(2, "Luis", "Gómez")])),
    http.get("*/api/rutas", () =>
      HttpResponse.json([
        { id: 1, nombre: "Cerro Uno", activa: true, borrador: false, cantidadCheckpoints: 4 },
        { id: 2, nombre: "Laguna Dos", activa: true, borrador: false, cantidadCheckpoints: 2 },
      ]),
    ),
    http.get("*/api/rutas/2/itinerario", () => HttpResponse.json(CHECKPOINTS_RUTA_DOS)),
    http.put("*/api/salidas/7", async ({ request }) => {
      puts.push(await request.json());
      return opciones.editar ? opciones.editar() : HttpResponse.json({ id: 7 });
    }),
  );
  return puts;
}

async function abrirDesdeListado() {
  const app = renderApp("/salidas");
  await app.usuario.click(await screen.findByRole("button", { name: "Editar Salida de Cerro Uno" }));
  await screen.findByLabelText(/^Fecha/);
  return app;
}

const guardar = () => screen.getByRole("button", { name: "Guardar" });
const confirmacion = () => screen.queryByRole("dialog", { name: "Cambios sin guardar" });

describe("editar Salida planificada", () => {
  it("abre el formulario con la Copia operativa, guarda en una petición y vuelve al listado con aviso", async () => {
    const puts = api();
    const { usuario, router } = await abrirDesdeListado();
    expect(router.state.location.pathname).toBe("/salidas/7/editar");
    expect(screen.getByRole("heading", { name: "Editar salida" })).toBeInTheDocument();
    expect(within(screen.getByRole("navigation", { name: "Breadcrumbs" })).getByText("Editar salida")).toBeInTheDocument();
    expect(screen.getByLabelText(/^Fecha/)).toHaveValue("2026-11-10");
    expect(await screen.findByLabelText("Cantidad de Carpa Uno")).toHaveValue(2);
    expect(screen.getByLabelText("Cantidad de Soga Dos")).toHaveValue(null);
    expect(screen.getByRole("checkbox", { name: /Ana Pérez/ })).toBeChecked();

    await usuario.clear(screen.getByLabelText(/^Fecha/));
    await usuario.type(screen.getByLabelText(/^Fecha/), "2026-12-01");
    await usuario.type(screen.getByLabelText("Cantidad de Soga Dos"), "3");
    await usuario.click(screen.getByRole("checkbox", { name: /Ana Pérez/ }));
    await usuario.click(screen.getByRole("checkbox", { name: /Luis Gómez/ }));
    await usuario.selectOptions(await screen.findByLabelText("Ruta"), "Laguna Dos");
    await usuario.click(guardar());

    expect(await screen.findByRole("status", { name: "Notificación" })).toHaveTextContent("Salida actualizada");
    expect(await screen.findByRole("heading", { name: "Salidas" })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/salidas");
    expect(puts).toEqual([
      {
        fecha: "2026-12-01",
        rutaId: 2,
        actualizarDesdeRuta: false,
        checkpoints: CHECKPOINTS_RUTA_DOS,
        equipamiento: [
          { elementoId: 1, cantidad: 2 },
          { elementoId: 2, cantidad: 3 },
        ],
        participantes: [2],
      },
    ]);
  });

  it("conserva los Checkpoints de la Copia operativa si la Ruta no cambia", async () => {
    const puts = api();
    const { usuario } = await abrirDesdeListado();
    await usuario.click(guardar());
    await screen.findByRole("heading", { name: "Salidas" });
    expect(puts).toMatchObject([{ rutaId: 1, checkpoints: CHECKPOINTS_COPIA, participantes: [1] }]);
  });

  it("muestra cada 422 junto a su campo", async () => {
    api({
      editar: () =>
        HttpResponse.json(
          { codigo: "VALIDACION", mensaje: "Datos inválidos", errores: [{ campo: "fecha", mensaje: "La fecha de la Salida es obligatoria." }] },
          { status: 422 },
        ),
    });
    const { usuario, router } = await abrirDesdeListado();
    await usuario.click(guardar());
    expect(await screen.findByText("La fecha de la Salida es obligatoria.")).toBeInTheDocument();
    expect(screen.getByLabelText(/^Fecha/)).toBeInvalid();
    expect(router.state.location.pathname).toBe("/salidas/7/editar");
  });

  it("ante un 409 por estado final muestra el mensaje, recarga y no reintenta", async () => {
    const puts = api({
      estados: ["PLANIFICADA", "REALIZADA"],
      editar: () =>
        HttpResponse.json({ codigo: "CONFLICTO", mensaje: "La Salida ya no está planificada: está Realizada." }, { status: 409 }),
    });
    const { usuario } = await abrirDesdeListado();
    await usuario.click(guardar());
    expect(await screen.findByText("La Salida ya no está planificada: está Realizada.")).toBeInTheDocument();
    expect(await screen.findByText("Esta Salida ya no se puede editar.")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Guardar" })).not.toBeInTheDocument();
    expect(puts).toHaveLength(1);
  });

  it("ante un 409 de stock indica el elemento y la cantidad disponible y deja corregir", async () => {
    const puts = api({
      editar: () =>
        HttpResponse.json(
          { codigo: "STOCK_INSUFICIENTE", mensaje: "Stock insuficiente.", elemento: "Soga Dos", disponible: 3 },
          { status: 409 },
        ),
    });
    const { usuario } = await abrirDesdeListado();
    await usuario.type(await screen.findByLabelText("Cantidad de Soga Dos"), "9");
    await usuario.click(guardar());
    expect(await screen.findByRole("alert")).toHaveTextContent("Stock insuficiente. Soga Dos: disponible 3.");
    expect(screen.getByLabelText("Cantidad de Soga Dos")).toHaveValue(9);
    expect(puts).toHaveLength(1);
  });

  it("pide confirmación al salir con cambios sin guardar y no al salir sin cambios", async () => {
    api();
    const { usuario, router } = await abrirDesdeListado();
    await usuario.click(screen.getByRole("button", { name: "Cancelar" }));
    expect(router.state.location.pathname).toBe("/salidas");
    expect(confirmacion()).not.toBeInTheDocument();

    await usuario.click(await screen.findByRole("button", { name: "Editar Salida de Cerro Uno" }));
    await usuario.type(await screen.findByLabelText("Cantidad de Soga Dos"), "1");
    await usuario.click(screen.getByRole("button", { name: "Cancelar" }));
    expect(confirmacion()).toBeInTheDocument();
    await usuario.click(screen.getByRole("button", { name: "Seguir editando" }));
    expect(screen.getByLabelText("Cantidad de Soga Dos")).toHaveValue(1);
    await usuario.click(screen.getByRole("button", { name: "Cancelar" }));
    await usuario.click(screen.getByRole("button", { name: "Descartar y salir" }));
    expect(router.state.location.pathname).toBe("/salidas");
  });
});
