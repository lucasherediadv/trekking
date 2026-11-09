import { screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { server } from "../test/servidor";
import { renderApp } from "../test/renderApp";

const CERRO = {
  id: 1,
  nombre: "Cerro Uno",
  altitudMaxima: 3000,
  tipoTerreno: "Rocoso",
  dificultadTecnica: "Media",
  dificultadFisica: "Alta",
  activa: true,
  cantidadCheckpoints: 4,
  borrador: false,
};

/** Listado que refleja las Rutas creadas durante el test. */
function listado(inicial: object[] = [CERRO]) {
  const rutas = [...inicial];
  server.use(http.get("*/api/rutas", () => HttpResponse.json(rutas)));
  return rutas;
}

async function completarNueva(usuario: ReturnType<typeof renderApp>["usuario"]) {
  await usuario.type(screen.getByLabelText(/Nombre/), "Laguna Nueva");
  await usuario.type(screen.getByLabelText(/Altitud máxima/), "1200,5");
  await usuario.selectOptions(screen.getByLabelText(/Tipo de terreno/), "Boscoso");
  await usuario.selectOptions(screen.getByLabelText(/Dificultad técnica/), "Baja");
  await usuario.selectOptions(screen.getByLabelText(/Dificultad física/), "Media");
}

const confirmacion = () => screen.queryByRole("dialog", { name: "Cambios sin guardar" });

describe("alta de Ruta", () => {
  it("crea la Ruta desde el listado, vuelve a él actualizado y queda como borrador", async () => {
    const rutas = listado();
    let cuerpo: unknown;
    server.use(
      http.post("*/api/rutas", async ({ request }) => {
        cuerpo = await request.json();
        const creada = { ...CERRO, id: 2, nombre: "Laguna Nueva", cantidadCheckpoints: 0, borrador: true };
        rutas.push(creada);
        return HttpResponse.json(creada, { status: 201 });
      }),
    );
    const { usuario, router } = renderApp("/rutas");
    await usuario.click(await screen.findByRole("link", { name: "Nueva ruta" }));

    expect(router.state.location.pathname).toBe("/rutas/nueva");
    expect(screen.getByRole("navigation", { name: "Breadcrumbs" })).toHaveTextContent("Rutas›Nueva ruta");
    await completarNueva(usuario);
    await usuario.click(screen.getByRole("button", { name: "Guardar" }));

    expect(await screen.findByText("Laguna Nueva")).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/rutas");
    expect(cuerpo).toEqual({
      nombre: "Laguna Nueva",
      altitudMaxima: 1200.5,
      tipoTerreno: "Boscoso",
      dificultadTecnica: "Baja",
      dificultadFisica: "Media",
    });
    expect(screen.getByRole("status", { name: "Notificación" })).toHaveTextContent("Ruta creada");
    expect(screen.getByText("Borrador")).toBeInTheDocument();
    expect(confirmacion()).not.toBeInTheDocument();
  });

  it("Cancelar sin cambios vuelve al listado sin pedir confirmación", async () => {
    listado();
    const { usuario, router } = renderApp("/rutas/nueva");
    await usuario.click(screen.getByRole("button", { name: "Cancelar" }));
    expect(router.state.location.pathname).toBe("/rutas");
    expect(confirmacion()).not.toBeInTheDocument();
  });

  it("asigna el 422 a cada campo y muestra los errores sin campo como aviso general", async () => {
    server.use(
      http.post("*/api/rutas", () =>
        HttpResponse.json(
          {
            codigo: "VALIDACION",
            mensaje: "Datos inválidos",
            errores: [
              { campo: "nombre", mensaje: "El nombre de la ruta es obligatorio." },
              { campo: "altitudMaxima", mensaje: "La altitud máxima es obligatoria." },
              { campo: "", mensaje: "Ya existe una Ruta con ese nombre." },
            ],
          },
          { status: 422 },
        ),
      ),
    );
    const { usuario, router } = renderApp("/rutas/nueva");
    await usuario.click(screen.getByRole("button", { name: "Guardar" }));

    const nombre = screen.getByLabelText(/Nombre/);
    expect(await screen.findByText("El nombre de la ruta es obligatorio.")).toBeInTheDocument();
    expect(nombre).toHaveAccessibleDescription(/El nombre de la ruta es obligatorio/);
    expect(nombre).toBeInvalid();
    expect(screen.getByLabelText(/Altitud máxima/)).toHaveAccessibleDescription(/La altitud máxima es obligatoria/);
    expect(screen.getByLabelText(/Tipo de terreno/)).not.toBeInvalid();
    expect(screen.getByRole("alert")).toHaveTextContent("Ya existe una Ruta con ese nombre.");
    expect(router.state.location.pathname).toBe("/rutas/nueva");

    // Al corregir un campo, su error desaparece.
    await usuario.type(nombre, "x");
    expect(nombre).not.toBeInvalid();
  });

  it("un texto de altitud no numérico se rechaza en el campo sin llamar a la API", async () => {
    let llamadas = 0;
    server.use(
      http.post("*/api/rutas", () => {
        llamadas++;
        return HttpResponse.json({}, { status: 201 });
      }),
    );
    const { usuario } = renderApp("/rutas/nueva");
    await usuario.type(screen.getByLabelText(/Altitud máxima/), "abc");
    await usuario.click(screen.getByRole("button", { name: "Guardar" }));
    expect(await screen.findByText("Ingresa un valor numérico válido.")).toBeInTheDocument();
    expect(llamadas).toBe(0);
  });

  it("una falla inesperada al guardar muestra un aviso y conserva lo escrito", async () => {
    server.use(http.post("*/api/rutas", () => HttpResponse.error()));
    const { usuario } = renderApp("/rutas/nueva");
    await usuario.type(screen.getByLabelText(/Nombre/), "Laguna");
    await usuario.click(screen.getByRole("button", { name: "Guardar" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("No se pudo conectar con el servidor");
    expect(screen.getByLabelText(/Nombre/)).toHaveValue("Laguna");
  });
});

describe("edición de Ruta", () => {
  it("precarga la Ruta, guarda los cambios y vuelve al listado actualizado", async () => {
    const rutas = listado();
    let cuerpo: unknown;
    server.use(
      http.get("*/api/rutas/1", () => HttpResponse.json(CERRO)),
      http.put("*/api/rutas/1", async ({ request }) => {
        cuerpo = await request.json();
        rutas[0] = { ...CERRO, nombre: "Cerro Renombrado" };
        return HttpResponse.json(rutas[0]);
      }),
    );
    const { usuario, router } = renderApp("/rutas");
    await usuario.click(await screen.findByRole("link", { name: "Editar Cerro Uno" }));

    expect(router.state.location.pathname).toBe("/rutas/1/editar");
    expect(screen.getByRole("navigation", { name: "Breadcrumbs" })).toHaveTextContent("Rutas›Editar ruta");
    expect(await screen.findByLabelText(/Nombre/)).toHaveValue("Cerro Uno");
    expect(screen.getByLabelText(/Altitud máxima/)).toHaveValue("3000");
    expect(screen.getByLabelText(/Tipo de terreno/)).toHaveValue("Rocoso");
    expect(screen.getByLabelText(/Dificultad técnica/)).toHaveValue("Media");
    expect(screen.getByLabelText(/Dificultad física/)).toHaveValue("Alta");

    await usuario.clear(screen.getByLabelText(/Nombre/));
    await usuario.type(screen.getByLabelText(/Nombre/), "Cerro Renombrado");
    await usuario.click(screen.getByRole("button", { name: "Guardar" }));

    expect(await screen.findByText("Cerro Renombrado")).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/rutas");
    expect(cuerpo).toEqual({
      nombre: "Cerro Renombrado",
      altitudMaxima: 3000,
      tipoTerreno: "Rocoso",
      dificultadTecnica: "Media",
      dificultadFisica: "Alta",
    });
    expect(screen.getByRole("status", { name: "Notificación" })).toHaveTextContent("Ruta actualizada");
  });

  it("un 404 al abrir avisa que la Ruta ya no existe y lleva al listado", async () => {
    listado();
    server.use(
      http.get("*/api/rutas/99", () =>
        HttpResponse.json({ codigo: "NO_ENCONTRADO", mensaje: "Ruta 99 no existe" }, { status: 404 }),
      ),
    );
    const { router } = renderApp("/rutas/99/editar");
    expect(await screen.findByRole("status", { name: "Notificación" })).toHaveTextContent("El recurso ya no existe");
    await waitFor(() => expect(router.state.location.pathname).toBe("/rutas"));
    expect(await screen.findByText("Cerro Uno")).toBeInTheDocument();
  });

  it("un 404 al guardar avisa y lleva al listado", async () => {
    listado();
    server.use(
      http.get("*/api/rutas/1", () => HttpResponse.json(CERRO)),
      http.put("*/api/rutas/1", () =>
        HttpResponse.json({ codigo: "NO_ENCONTRADO", mensaje: "Ruta 1 no existe" }, { status: 404 }),
      ),
    );
    const { usuario, router } = renderApp("/rutas/1/editar");
    await usuario.type(await screen.findByLabelText(/Nombre/), " Dos");
    await usuario.click(screen.getByRole("button", { name: "Guardar" }));
    expect(await screen.findByRole("status", { name: "Notificación" })).toHaveTextContent("El recurso ya no existe");
    await waitFor(() => expect(router.state.location.pathname).toBe("/rutas"));
    expect(confirmacion()).not.toBeInTheDocument();
  });

  it("una falla al cargar permite reintentar", async () => {
    listado();
    let intentos = 0;
    server.use(
      http.get("*/api/rutas/1", () => {
        intentos++;
        return intentos === 1 ? HttpResponse.error() : HttpResponse.json(CERRO);
      }),
    );
    const { usuario } = renderApp("/rutas/1/editar");
    await usuario.click(await screen.findByRole("button", { name: "Reintentar" }));
    expect(await screen.findByLabelText(/Nombre/)).toHaveValue("Cerro Uno");
  });
});

describe("confirmación al salir con cambios sin guardar", () => {
  async function conCambios(ruta = "/rutas/nueva") {
    listado();
    const app = renderApp(ruta);
    await app.usuario.type(await screen.findByLabelText(/Nombre/), "Algo");
    return app;
  }

  it("pide confirmación al salir por el menú; seguir editando conserva lo escrito", async () => {
    const { usuario, router } = await conCambios();
    await usuario.click(screen.getByRole("link", { name: "Inventario" }));
    expect(confirmacion()).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/rutas/nueva");
    await usuario.click(screen.getByRole("button", { name: "Seguir editando" }));
    expect(confirmacion()).not.toBeInTheDocument();
    expect(screen.getByLabelText(/Nombre/)).toHaveValue("Algo");
  });

  it("descartar los cambios completa la salida", async () => {
    const { usuario, router } = await conCambios();
    await usuario.click(screen.getByRole("link", { name: "Inventario" }));
    await usuario.click(screen.getByRole("button", { name: "Descartar y salir" }));
    expect(router.state.location.pathname).toBe("/inventario");
  });

  it("también la pide al salir por breadcrumb, Cancelar o historial del navegador", async () => {
    const { usuario, router } = await conCambios();
    await usuario.click(within(screen.getByRole("navigation", { name: "Breadcrumbs" })).getByRole("link", { name: "Rutas" }));
    expect(confirmacion()).toBeInTheDocument();
    await usuario.click(screen.getByRole("button", { name: "Seguir editando" }));

    await usuario.click(screen.getByRole("button", { name: "Cancelar" }));
    expect(confirmacion()).toBeInTheDocument();
    await usuario.click(screen.getByRole("button", { name: "Seguir editando" }));

    await router.navigate(-1);
    await waitFor(() => expect(router.state.location.pathname).toBe("/rutas/nueva"));
  });

  it("revertir el campo a su valor original deja de contar como cambio", async () => {
    listado();
    server.use(http.get("*/api/rutas/1", () => HttpResponse.json(CERRO)));
    const { usuario, router } = renderApp("/rutas/1/editar");
    const nombre = await screen.findByLabelText(/Nombre/);
    await usuario.type(nombre, "x");
    await usuario.type(nombre, "{Backspace}");
    await usuario.click(screen.getByRole("link", { name: "Inventario" }));
    expect(confirmacion()).not.toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/inventario");
  });
});
