import { screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { server } from "../../test/servidor";
import { renderApp } from "../../test/renderApp";

const elemento = (id: number, nombre: string, disponible: number) => ({
  id,
  nombre,
  categoria: "Campamento",
  cantidad: disponible,
  cantidadReservada: 0,
  cantidadDisponible: disponible,
  estado: "DISPONIBLE",
  estadoEtiqueta: "Disponible",
  activo: true,
  asignable: true,
});

// La API solo devuelve los asignables: los que están en reparación, fuera de servicio o extraviados no vienen.
const ASIGNABLES = [elemento(1, "Carpa Uno", 5), elemento(2, "Soga Dos", 3)];

function api() {
  const peticiones: unknown[] = [];
  server.use(
    http.get("*/api/rutas", () =>
      HttpResponse.json([{ id: 1, nombre: "Cerro Uno", activa: true, borrador: false, cantidadCheckpoints: 4 }]),
    ),
    http.get("*/api/inventario/asignables", () => HttpResponse.json(ASIGNABLES)),
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

const paso = (nombre: string) =>
  within(screen.getByRole("navigation", { name: "Pasos del asistente" })).getByRole("button", {
    name: new RegExp(`^\\d\\. ${nombre}`),
  });

async function llegarAEquipamiento() {
  const { usuario } = renderApp("/salidas/nueva");
  await usuario.type(await screen.findByLabelText("Fecha"), "2026-12-05");
  await usuario.click(paso("Ruta"));
  await usuario.selectOptions(await screen.findByLabelText("Ruta"), "Cerro Uno");
  await usuario.click(paso("Equipamiento"));
  await screen.findByText("Carpa Uno");
  return usuario;
}

describe("Asistente de Salida: Equipamiento", () => {
  it("elige elementos con su cantidad y los envía en la misma petición de confirmación", async () => {
    const peticiones = api();
    const usuario = await llegarAEquipamiento();
    await usuario.type(screen.getByLabelText("Cantidad de Carpa Uno"), "2");
    await usuario.click(paso("Revisión"));

    expect(screen.getByRole("main")).toHaveTextContent("Carpa Uno x 2");
    await usuario.click(screen.getByRole("button", { name: "Confirmar Salida" }));
    expect(await screen.findByRole("heading", { name: "Salidas" })).toBeInTheDocument();
    expect(peticiones).toEqual([
      {
        fecha: "2026-12-05",
        rutaId: 1,
        equipamiento: [{ elementoId: 1, cantidad: 2 }],
        elementosNuevos: [],
        clientesExistentes: [],
        clientesNuevos: [],
      },
    ]);
  });

  it("crea un elemento nuevo dentro del asistente y lo envía como elemento nuevo", async () => {
    const peticiones = api();
    const usuario = await llegarAEquipamiento();
    await usuario.type(screen.getByLabelText("Nombre *"), "Linterna");
    await usuario.selectOptions(screen.getByLabelText("Categoría *"), "Seguridad");
    await usuario.type(screen.getByLabelText("Cantidad total *"), "4");
    await usuario.selectOptions(screen.getByLabelText("Estado del elemento *"), "Disponible");
    await usuario.type(screen.getByLabelText("Cantidad a asignar *"), "2");
    await usuario.click(screen.getByRole("button", { name: "Agregar elemento nuevo" }));

    expect(screen.getByText("Linterna x 2")).toBeInTheDocument();
    expect(screen.getByLabelText("Nombre *")).toHaveValue("");
    await usuario.click(paso("Revisión"));
    expect(screen.getByRole("main")).toHaveTextContent("Linterna x 2");
    await usuario.click(screen.getByRole("button", { name: "Confirmar Salida" }));
    await screen.findByRole("heading", { name: "Salidas" });
    expect(peticiones).toMatchObject([
      {
        equipamiento: [],
        elementosNuevos: [
          { elemento: { nombre: "Linterna", categoria: "Seguridad", cantidad: 4, estado: "DISPONIBLE" }, cantidad: 2 },
        ],
      },
    ]);
  });

  it("ofrece solo los elementos asignables", async () => {
    api();
    await llegarAEquipamiento();
    expect(screen.getByText("Soga Dos")).toBeInTheDocument();
    expect(screen.getAllByLabelText(/^Cantidad de /)).toHaveLength(2);
  });

  it("un 409 de stock indica elemento y disponible, sin perder lo ingresado", async () => {
    api();
    server.use(
      http.post("*/api/salidas", () =>
        HttpResponse.json(
          {
            codigo: "STOCK_INSUFICIENTE",
            mensaje: "No hay cantidad suficiente.",
            elemento: "Carpa Uno",
            disponible: 1,
          },
          { status: 409 },
        ),
      ),
    );
    const usuario = await llegarAEquipamiento();
    await usuario.type(screen.getByLabelText("Cantidad de Carpa Uno"), "4");
    await usuario.click(paso("Revisión"));
    await usuario.click(screen.getByRole("button", { name: "Confirmar Salida" }));

    const alerta = await screen.findByRole("alert");
    expect(alerta).toHaveTextContent("Carpa Uno");
    expect(alerta).toHaveTextContent("Carpa Uno: disponible 1");
    expect(screen.getByRole("main")).toHaveTextContent("Carpa Uno x 4");
    await usuario.click(paso("Equipamiento"));
    expect(await screen.findByLabelText("Cantidad de Carpa Uno")).toHaveValue(4);
  });
});
