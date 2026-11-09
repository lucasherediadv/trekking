import { screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { server } from "../test/servidor";
import { renderApp } from "../test/renderApp";

const CARPA = {
  id: 1,
  nombre: "Carpa Uno",
  categoria: "Campamento",
  cantidad: 10,
  cantidadReservada: 2,
  cantidadDisponible: 8,
  estado: "DISPONIBLE",
  estadoEtiqueta: "Disponible",
  activo: true,
  asignable: true,
};

/** Listado que refleja los elementos creados durante el test. */
function listado(inicial: object[] = [CARPA]) {
  const elementos = [...inicial];
  server.use(http.get("*/api/inventario", () => HttpResponse.json(elementos)));
  return elementos;
}

const confirmacion = () => screen.queryByRole("dialog", { name: "Cambios sin guardar" });

describe("alta de elemento", () => {
  it("crea el elemento desde el listado y vuelve a él actualizado", async () => {
    const elementos = listado();
    let cuerpo: unknown;
    server.use(
      http.post("*/api/inventario", async ({ request }) => {
        cuerpo = await request.json();
        const creado = { ...CARPA, id: 2, nombre: "Soga Nueva", cantidad: 5, estado: "EN_INSPECCION", estadoEtiqueta: "En Inspección" };
        elementos.push(creado);
        return HttpResponse.json(creado, { status: 201 });
      }),
    );
    const { usuario, router } = renderApp("/inventario");
    await usuario.click(await screen.findByRole("link", { name: "Nuevo elemento" }));

    expect(router.state.location.pathname).toBe("/inventario/nuevo");
    expect(screen.getByRole("navigation", { name: "Breadcrumbs" })).toHaveTextContent("Inventario›Nuevo elemento");
    await usuario.type(screen.getByLabelText(/Nombre/), "Soga Nueva");
    await usuario.selectOptions(screen.getByLabelText(/Categoría/), "Escalada");
    await usuario.type(screen.getByLabelText(/Cantidad total/), "5");
    await usuario.selectOptions(screen.getByLabelText(/Estado del elemento/), "En Inspección");
    await usuario.click(screen.getByRole("button", { name: "Guardar" }));

    expect(await screen.findByText("Soga Nueva")).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/inventario");
    expect(cuerpo).toEqual({ nombre: "Soga Nueva", categoria: "Escalada", cantidad: 5, estado: "EN_INSPECCION" });
    expect(screen.getByRole("status", { name: "Notificación" })).toHaveTextContent("Elemento creado");
    expect(confirmacion()).not.toBeInTheDocument();
  });

  it("asigna el 422 a cada campo y muestra los errores sin campo como aviso general", async () => {
    server.use(
      http.post("*/api/inventario", () =>
        HttpResponse.json(
          {
            codigo: "VALIDACION",
            mensaje: "Datos inválidos",
            errores: [
              { campo: "nombre", mensaje: "El nombre es obligatorio." },
              { campo: "cantidad", mensaje: "La cantidad es obligatoria." },
              { campo: "", mensaje: "Ya existe un elemento así." },
            ],
          },
          { status: 422 },
        ),
      ),
    );
    const { usuario, router } = renderApp("/inventario/nuevo");
    await usuario.click(screen.getByRole("button", { name: "Guardar" }));

    const nombre = screen.getByLabelText(/Nombre/);
    expect(await screen.findByText("El nombre es obligatorio.")).toBeInTheDocument();
    expect(nombre).toHaveAccessibleDescription(/El nombre es obligatorio/);
    expect(nombre).toBeInvalid();
    expect(screen.getByLabelText(/Cantidad total/)).toHaveAccessibleDescription(/La cantidad es obligatoria/);
    expect(screen.getByLabelText(/Categoría/)).not.toBeInvalid();
    expect(screen.getByRole("alert")).toHaveTextContent("Ya existe un elemento así.");
    expect(router.state.location.pathname).toBe("/inventario/nuevo");
    await usuario.type(nombre, "x");
    expect(nombre).not.toBeInvalid();
  });

  it("una cantidad no entera se rechaza en el campo sin llamar a la API", async () => {
    let llamadas = 0;
    server.use(
      http.post("*/api/inventario", () => {
        llamadas++;
        return HttpResponse.json({}, { status: 201 });
      }),
    );
    const { usuario } = renderApp("/inventario/nuevo");
    await usuario.type(screen.getByLabelText(/Cantidad total/), "2,5");
    await usuario.click(screen.getByRole("button", { name: "Guardar" }));
    expect(await screen.findByText("Ingresa un número entero mayor o igual a cero.")).toBeInTheDocument();
    expect(llamadas).toBe(0);
  });
});

describe("edición de elemento", () => {
  it("precarga el elemento, muestra su vigencia, guarda y vuelve al listado actualizado", async () => {
    const elementos = listado();
    let cuerpo: unknown;
    server.use(
      http.get("*/api/inventario/1", () => HttpResponse.json(CARPA)),
      http.put("*/api/inventario/1", async ({ request }) => {
        cuerpo = await request.json();
        elementos[0] = { ...CARPA, nombre: "Carpa Renombrada" };
        return HttpResponse.json(elementos[0]);
      }),
    );
    const { usuario, router } = renderApp("/inventario");
    await usuario.click(await screen.findByRole("link", { name: "Editar Carpa Uno" }));

    expect(router.state.location.pathname).toBe("/inventario/1/editar");
    expect(screen.getByRole("navigation", { name: "Breadcrumbs" })).toHaveTextContent("Inventario›Editar elemento");
    expect(await screen.findByLabelText(/Nombre/)).toHaveValue("Carpa Uno");
    expect(screen.getByLabelText(/Categoría/)).toHaveValue("Campamento");
    expect(screen.getByLabelText(/Cantidad total/)).toHaveValue("10");
    expect(screen.getByLabelText(/Estado del elemento/)).toHaveValue("DISPONIBLE");
    expect(screen.getByText("Vigente")).toBeInTheDocument();

    await usuario.clear(screen.getByLabelText(/Nombre/));
    await usuario.type(screen.getByLabelText(/Nombre/), "Carpa Renombrada");
    await usuario.click(screen.getByRole("button", { name: "Guardar" }));

    expect(await screen.findByText("Carpa Renombrada")).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/inventario");
    expect(cuerpo).toEqual({ nombre: "Carpa Renombrada", categoria: "Campamento", cantidad: 10, estado: "DISPONIBLE" });
    expect(screen.getByRole("status", { name: "Notificación" })).toHaveTextContent("Elemento actualizado");
  });

  it("un 404 al abrir avisa que el elemento ya no existe y lleva al listado", async () => {
    listado();
    server.use(
      http.get("*/api/inventario/99", () =>
        HttpResponse.json({ codigo: "NO_ENCONTRADO", mensaje: "Elemento 99 no existe" }, { status: 404 }),
      ),
    );
    const { router } = renderApp("/inventario/99/editar");
    expect(await screen.findByRole("status", { name: "Notificación" })).toHaveTextContent("El recurso ya no existe");
    await waitFor(() => expect(router.state.location.pathname).toBe("/inventario"));
    expect(await screen.findByText("Carpa Uno")).toBeInTheDocument();
  });

  it("un 404 al guardar avisa y lleva al listado", async () => {
    listado();
    server.use(
      http.get("*/api/inventario/1", () => HttpResponse.json(CARPA)),
      http.put("*/api/inventario/1", () =>
        HttpResponse.json({ codigo: "NO_ENCONTRADO", mensaje: "Elemento 1 no existe" }, { status: 404 }),
      ),
    );
    const { usuario, router } = renderApp("/inventario/1/editar");
    await usuario.type(await screen.findByLabelText(/Nombre/), " Dos");
    await usuario.click(screen.getByRole("button", { name: "Guardar" }));
    expect(await screen.findByRole("status", { name: "Notificación" })).toHaveTextContent("El recurso ya no existe");
    await waitFor(() => expect(router.state.location.pathname).toBe("/inventario"));
    expect(confirmacion()).not.toBeInTheDocument();
  });
});

describe("confirmación al salir con cambios sin guardar", () => {
  async function conCambios() {
    listado();
    const app = renderApp("/inventario/nuevo");
    await app.usuario.type(await screen.findByLabelText(/Nombre/), "Algo");
    return app;
  }

  it("pide confirmación al salir; seguir editando conserva lo escrito y descartar completa la salida", async () => {
    const { usuario, router } = await conCambios();
    await usuario.click(screen.getByRole("link", { name: "Rutas" }));
    expect(confirmacion()).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/inventario/nuevo");
    await usuario.click(screen.getByRole("button", { name: "Seguir editando" }));
    expect(screen.getByLabelText(/Nombre/)).toHaveValue("Algo");

    await usuario.click(within(screen.getByRole("navigation", { name: "Breadcrumbs" })).getByRole("link", { name: "Inventario" }));
    expect(confirmacion()).toBeInTheDocument();
    await usuario.click(screen.getByRole("button", { name: "Descartar y salir" }));
    expect(router.state.location.pathname).toBe("/inventario");
  });

  it("Cancelar sin cambios vuelve sin confirmar y con cambios la pide", async () => {
    listado();
    const sin = renderApp("/inventario/nuevo");
    await sin.usuario.click(screen.getByRole("button", { name: "Cancelar" }));
    expect(sin.router.state.location.pathname).toBe("/inventario");
    expect(confirmacion()).not.toBeInTheDocument();
  });

  it("Cancelar con cambios pide confirmación", async () => {
    const { usuario } = await conCambios();
    await usuario.click(screen.getByRole("button", { name: "Cancelar" }));
    expect(confirmacion()).toBeInTheDocument();
  });
});
