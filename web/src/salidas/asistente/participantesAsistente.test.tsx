import { screen, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { server } from "../../test/servidor";
import { renderApp } from "../../test/renderApp";

const cliente = (id: number, nombre: string, apellido: string) => ({
  id,
  dni: `3011122${id}`,
  nombre,
  apellido,
  activo: true,
  menor: false,
});

function api(opciones: { creacion?: () => Response } = {}) {
  const peticiones: any[] = [];
  server.use(
    http.get("*/api/rutas", () =>
      HttpResponse.json([{ id: 1, nombre: "Cerro Uno", activa: true, borrador: false, cantidadCheckpoints: 4 }]),
    ),
    http.get("*/api/inventario/asignables", () => HttpResponse.json([])),
    http.get("*/api/clientes", () => HttpResponse.json([cliente(1, "Ana", "Pérez"), cliente(2, "Beto", "Díaz")])),
    http.post("*/api/clientes/validar", () => HttpResponse.json([])),
    http.get("*/api/salidas", () => HttpResponse.json([])),
    http.get("*/api/dashboard/indicadores-salidas", () => HttpResponse.json({})),
    http.get("*/api/dashboard/proximas-salidas", () => HttpResponse.json([])),
    http.post("*/api/salidas", async ({ request }) => {
      peticiones.push(await request.json());
      return opciones.creacion ? opciones.creacion() : HttpResponse.json({ id: 9 }, { status: 201 });
    }),
  );
  return peticiones;
}

const paso = (nombre: string) =>
  within(screen.getByRole("navigation", { name: "Pasos del asistente" })).getByRole("button", {
    name: new RegExp(`^\\d\\. ${nombre}`),
  });

type Usuario = ReturnType<typeof renderApp>["usuario"];

async function llegarAParticipantes() {
  const { usuario } = renderApp("/salidas/nueva");
  await usuario.type(await screen.findByLabelText("Fecha"), "2026-12-05");
  await usuario.click(paso("Ruta"));
  await usuario.selectOptions(await screen.findByLabelText("Ruta"), "Cerro Uno");
  await usuario.click(paso("Participantes"));
  await screen.findByText("Ana Pérez");
  return usuario;
}

async function completarNuevo(usuario: Usuario) {
  await usuario.type(screen.getByLabelText(/DNI \*/), "28999888");
  await usuario.type(screen.getByLabelText(/^Nombre/), "Luis");
  await usuario.type(screen.getByLabelText(/^Apellido/), "Gómez");
  await usuario.type(screen.getByLabelText(/Fecha de nacimiento/), "1985-03-04");
  await usuario.type(screen.getByLabelText(/^Teléfono/), "3511234567");
  await usuario.selectOptions(screen.getByLabelText(/Sexo/), "Masculino");
}

const agregar = (usuario: Usuario) => usuario.click(screen.getByRole("button", { name: "Agregar Cliente nuevo" }));

async function confirmar(usuario: Usuario) {
  await usuario.click(paso("Revisión"));
  await usuario.click(screen.getByRole("button", { name: "Confirmar Salida" }));
}

describe("Asistente de Salida: Participantes", () => {
  it("elige un Cliente existente y lo envía en la confirmación", async () => {
    const peticiones = api();
    const usuario = await llegarAParticipantes();
    await usuario.click(screen.getByRole("checkbox", { name: /Ana Pérez/ }));
    await usuario.click(paso("Revisión"));
    expect(screen.getByRole("main")).toHaveTextContent("Ana Pérez");
    await usuario.click(screen.getByRole("button", { name: "Confirmar Salida" }));
    await screen.findByRole("heading", { name: "Salidas" });
    expect(peticiones[0]).toMatchObject({ clientesExistentes: [1], clientesNuevos: [] });
  });

  it("crea un Cliente nuevo sin ficha", async () => {
    const peticiones = api();
    const usuario = await llegarAParticipantes();
    await completarNuevo(usuario);
    await agregar(usuario);
    expect(screen.getByText("Luis Gómez (nuevo)")).toBeInTheDocument();
    expect(screen.getByLabelText(/^Nombre/)).toHaveValue("");
    await confirmar(usuario);
    await screen.findByRole("heading", { name: "Salidas" });
    expect(peticiones[0].clientesNuevos).toHaveLength(1);
    expect(peticiones[0].clientesNuevos[0]).toMatchObject({ cliente: { dni: "28999888", nombre: "Luis", sexo: "Masculino" } });
    expect(peticiones[0].clientesNuevos[0].ficha).toBeUndefined();
  });

  it("crea un Cliente nuevo con ficha médica en base64", async () => {
    const peticiones = api();
    const usuario = await llegarAParticipantes();
    await completarNuevo(usuario);
    await usuario.upload(screen.getByLabelText("Ficha médica (opcional)"), new File(["hola"], "ficha.pdf", { type: "application/pdf" }));
    await agregar(usuario);
    await confirmar(usuario);
    await screen.findByRole("heading", { name: "Salidas" });
    expect(peticiones[0].clientesNuevos[0].ficha).toEqual({ nombreArchivo: "ficha.pdf", contenidoBase64: "aG9sYQ==" });
  });

  it("rechaza una ficha de más de 10 MB antes de enviar", async () => {
    const peticiones = api();
    const usuario = await llegarAParticipantes();
    await completarNuevo(usuario);
    const grande = new File(["x"], "grande.pdf");
    Object.defineProperty(grande, "size", { value: 10 * 1024 * 1024 + 1 });
    await usuario.upload(screen.getByLabelText("Ficha médica (opcional)"), grande);
    expect(await screen.findByText("El archivo supera el máximo de 10 MB.")).toBeInTheDocument();
    await agregar(usuario);
    await confirmar(usuario);
    await screen.findByRole("heading", { name: "Salidas" });
    expect(peticiones).toHaveLength(1);
    expect(peticiones[0].clientesNuevos[0].ficha).toBeUndefined();
  });

  it("marca por campo los errores de validación del Cliente nuevo", async () => {
    api();
    server.use(
      http.post("*/api/clientes/validar", () => HttpResponse.json([{ campo: "dni", mensaje: "El DNI debe contener entre 7 y 8 numeros." }])),
    );
    const usuario = await llegarAParticipantes();
    await completarNuevo(usuario);
    await agregar(usuario);
    expect((await screen.findAllByText("El DNI debe contener entre 7 y 8 numeros.")).length).toBeGreaterThan(0);
    expect(screen.queryByText(/\(nuevo\)/)).not.toBeInTheDocument();
  });

  it("muestra los errores 422 de la confirmación sin perder lo ingresado", async () => {
    api({
      creacion: () =>
        HttpResponse.json(
          { codigo: "VALIDACION", mensaje: "Hay datos inválidos.", errores: [{ campo: "dni", mensaje: "Ya existe un Cliente con ese DNI." }] },
          { status: 422 },
        ),
    });
    const usuario = await llegarAParticipantes();
    await completarNuevo(usuario);
    await agregar(usuario);
    await confirmar(usuario);
    expect(await screen.findByRole("alert")).toHaveTextContent("Ya existe un Cliente con ese DNI.");
    await usuario.click(paso("Participantes"));
    expect(await screen.findByText("Luis Gómez (nuevo)")).toBeInTheDocument();
  });

  it("no escribe en localStorage, sessionStorage ni IndexedDB", async () => {
    api();
    const escrituras = vi.spyOn(Storage.prototype, "setItem");
    const idb = vi.fn();
    vi.stubGlobal("indexedDB", { open: idb });
    const usuario = await llegarAParticipantes();
    await completarNuevo(usuario);
    await usuario.upload(screen.getByLabelText("Ficha médica (opcional)"), new File(["hola"], "ficha.pdf"));
    await agregar(usuario);
    await screen.findByText("Luis Gómez (nuevo)");
    expect(escrituras).not.toHaveBeenCalled();
    expect(idb).not.toHaveBeenCalled();
    vi.unstubAllGlobals();
    escrituras.mockRestore();
  });
});
