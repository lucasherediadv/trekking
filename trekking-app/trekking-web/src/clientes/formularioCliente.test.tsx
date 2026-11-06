import { screen, waitFor, within } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { server } from "../test/servidor";
import { renderApp } from "../test/renderApp";

const ANA = {
  id: 1,
  dni: "30111222",
  nombre: "Ana",
  apellido: "Pérez",
  fechaNacimiento: "1990-01-01",
  email: "ana@mail.com",
  telefono: "1155550000",
  sexo: "Femenino",
  contactoEmergenciaNombre: "",
  contactoEmergenciaTelefono: "",
  contactoEmergenciaRelacion: "",
  autorizacionMenores: false,
  tutorNombre: "",
  tutorApellido: "",
  tutorDni: "",
  tutorTelefono: "",
  activo: true,
  menor: false,
};

/** Listado que refleja los Clientes creados durante el test. */
function listado(inicial: object[] = [ANA]) {
  const clientes = [...inicial];
  server.use(
    http.get("*/api/clientes", () => HttpResponse.json(clientes)),
    http.head("*/api/clientes/:id/ficha-medica", () => new HttpResponse(null, { status: 404 })),
  );
  return clientes;
}

interface Datos {
  dni?: string;
  fechaNacimiento?: string;
  autorizacionMenores?: boolean;
  tutorNombre?: string;
}

/** Imita la validación sin guardar de la API: reglas mínimas, la web no las conoce. */
function validacion() {
  const cuerpos: Datos[] = [];
  server.use(
    http.post("*/api/clientes/validar", async ({ request }) => {
      const d = (await request.json()) as Datos;
      cuerpos.push(d);
      const errores = [];
      if (!/^\d{7,8}$/.test(d.dni ?? "")) errores.push({ campo: "dni", mensaje: "El DNI debe contener entre 7 y 8 numeros." });
      if ((d.fechaNacimiento ?? "") >= "2010-01-01") {
        if (!d.autorizacionMenores)
          errores.push({ campo: "autorizacionMenores", mensaje: "Para menores de 18 anos debes marcar la autorizacion de menores." });
        if (!d.tutorNombre)
          errores.push({ campo: "tutorNombre", mensaje: "Completa los datos del tutor para un cliente menor de edad." });
      }
      return HttpResponse.json(errores);
    }),
  );
  return cuerpos;
}

async function completarNuevo(usuario: ReturnType<typeof renderApp>["usuario"]) {
  await usuario.type(screen.getByLabelText(/DNI \*/), "28999888");
  await usuario.type(screen.getByLabelText(/^Nombre/), "Luis");
  await usuario.type(screen.getByLabelText(/^Apellido/), "Gómez");
  await usuario.type(screen.getByLabelText(/Fecha de nacimiento/), "1985-03-04");
  await usuario.type(screen.getByLabelText(/Email/), "luis@mail.com");
  await usuario.type(screen.getByLabelText(/^Teléfono/), "3511234567");
  await usuario.selectOptions(screen.getByLabelText(/Sexo/), "Masculino");
}

const confirmacion = () => screen.queryByRole("dialog", { name: "Cambios sin guardar" });
const noEncontrado = (id: number) =>
  HttpResponse.json({ codigo: "NO_ENCONTRADO", mensaje: `Cliente ${id} no existe` }, { status: 404 });

describe("alta de Cliente", () => {
  it("crea el Cliente desde el listado, vuelve a él actualizado y avisa", async () => {
    const clientes = listado();
    validacion();
    let cuerpo: unknown;
    server.use(
      http.post("*/api/clientes", async ({ request }) => {
        cuerpo = await request.json();
        const creado = { ...ANA, id: 2, nombre: "Luis", apellido: "Gómez", dni: "28999888" };
        clientes.push(creado);
        return HttpResponse.json(creado, { status: 201 });
      }),
    );
    const { usuario, router } = renderApp("/clientes");
    await usuario.click(await screen.findByRole("link", { name: "Nuevo cliente" }));

    expect(router.state.location.pathname).toBe("/clientes/nuevo");
    expect(screen.getByRole("navigation", { name: "Breadcrumbs" })).toHaveTextContent("Clientes›Nuevo cliente");
    await completarNuevo(usuario);
    await usuario.click(screen.getByRole("button", { name: "Guardar" }));

    expect(await screen.findByText("Luis Gómez")).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/clientes");
    expect(cuerpo).toMatchObject({
      dni: "28999888",
      nombre: "Luis",
      apellido: "Gómez",
      fechaNacimiento: "1985-03-04",
      email: "luis@mail.com",
      telefono: "3511234567",
      sexo: "Masculino",
      autorizacionMenores: false,
    });
    expect(screen.getByRole("status", { name: "Notificación" })).toHaveTextContent("Cliente creado");
    expect(confirmacion()).not.toBeInTheDocument();
  });

  it("valida sin guardar mientras se escribe y marca solo el campo tocado", async () => {
    listado();
    const cuerpos = validacion();
    let guardados = 0;
    server.use(
      http.post("*/api/clientes", () => {
        guardados++;
        return HttpResponse.json({}, { status: 201 });
      }),
    );
    const { usuario } = renderApp("/clientes/nuevo");
    const dni = screen.getByLabelText(/DNI \*/);
    await usuario.type(dni, "12");

    expect(await screen.findByText("El DNI debe contener entre 7 y 8 numeros.")).toBeInTheDocument();
    expect(dni).toBeInvalid();
    expect(dni).toHaveAccessibleDescription(/El DNI debe contener/);
    expect(screen.getByLabelText(/^Nombre/)).not.toBeInvalid();
    expect(cuerpos.at(-1)).toMatchObject({ dni: "12" });

    // Al completar el DNI el error desaparece sin guardar.
    await usuario.type(dni, "345678");
    await waitFor(() => expect(dni).not.toBeInvalid());
    expect(guardados).toBe(0);
  });

  it("si es menor lo indica, pide autorización y tutor según la API y lo quita al ser mayor", async () => {
    listado();
    validacion();
    const { usuario } = renderApp("/clientes/nuevo");
    expect(screen.queryByText(/menor de edad/i)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/Nombre del tutor/)).not.toBeInTheDocument();

    const fecha = screen.getByLabelText(/Fecha de nacimiento/);
    await usuario.type(fecha, "2015-05-05");
    expect(await screen.findByRole("note")).toHaveTextContent("Cliente menor de edad");
    const tutor = await screen.findByLabelText(/Nombre del tutor/);
    expect(await screen.findByText("Completa los datos del tutor para un cliente menor de edad.")).toBeInTheDocument();
    expect(tutor).toBeInvalid();
    expect(screen.getByLabelText(/Autorización de menores/)).toBeInvalid();

    await usuario.click(screen.getByLabelText(/Autorización de menores/));
    await usuario.type(tutor, "Marta");
    await waitFor(() => expect(tutor).not.toBeInvalid());
    expect(screen.getByLabelText(/Autorización de menores/)).not.toBeInvalid();

    await usuario.clear(fecha);
    await usuario.type(fecha, "1990-05-05");
    await waitFor(() => expect(screen.queryByRole("note")).not.toBeInTheDocument());
  });

  it("asigna el 422 al guardar a cada campo y el resto como aviso general", async () => {
    listado();
    validacion();
    server.use(
      http.post("*/api/clientes", () =>
        HttpResponse.json(
          {
            codigo: "VALIDACION",
            mensaje: "Datos inválidos",
            errores: [
              { campo: "dni", mensaje: "Ya existe un Cliente con ese DNI." },
              { campo: "", mensaje: "No se pudo guardar el Cliente." },
            ],
          },
          { status: 422 },
        ),
      ),
    );
    const { usuario, router } = renderApp("/clientes/nuevo");
    await completarNuevo(usuario);
    await usuario.click(screen.getByRole("button", { name: "Guardar" }));

    expect(await screen.findByText("Ya existe un Cliente con ese DNI.")).toBeInTheDocument();
    expect(screen.getByLabelText(/DNI \*/)).toBeInvalid();
    expect(screen.getByRole("alert")).toHaveTextContent("No se pudo guardar el Cliente.");
    expect(router.state.location.pathname).toBe("/clientes/nuevo");
  });
});

describe("edición de Cliente", () => {
  it("precarga el Cliente, guarda los cambios y vuelve al listado actualizado", async () => {
    const clientes = listado();
    validacion();
    let cuerpo: unknown;
    server.use(
      http.get("*/api/clientes/1", () => HttpResponse.json(ANA)),
      http.put("*/api/clientes/1", async ({ request }) => {
        cuerpo = await request.json();
        clientes[0] = { ...ANA, nombre: "Anita" };
        return HttpResponse.json(clientes[0]);
      }),
    );
    const { usuario, router } = renderApp("/clientes");
    await usuario.click(await screen.findByRole("link", { name: "Editar Ana Pérez" }));

    expect(router.state.location.pathname).toBe("/clientes/1/editar");
    expect(screen.getByRole("navigation", { name: "Breadcrumbs" })).toHaveTextContent("Clientes›Editar cliente");
    const nombre = await screen.findByLabelText(/^Nombre/);
    expect(nombre).toHaveValue("Ana");
    expect(screen.getByLabelText(/DNI \*/)).toHaveValue("30111222");
    expect(screen.getByLabelText(/Fecha de nacimiento/)).toHaveValue("1990-01-01");
    expect(screen.getByLabelText(/Sexo/)).toHaveValue("Femenino");

    await usuario.clear(nombre);
    await usuario.type(nombre, "Anita");
    await usuario.click(screen.getByRole("button", { name: "Guardar" }));

    expect(await screen.findByText("Anita Pérez")).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/clientes");
    expect(cuerpo).toMatchObject({ dni: "30111222", nombre: "Anita", apellido: "Pérez", sexo: "Femenino" });
    expect(screen.getByRole("status", { name: "Notificación" })).toHaveTextContent("Cliente actualizado");
  });

  it("un 404 al abrir avisa que el Cliente ya no existe y lleva al listado", async () => {
    listado();
    server.use(http.get("*/api/clientes/99", () => noEncontrado(99)));
    const { router } = renderApp("/clientes/99/editar");
    expect(await screen.findByRole("status", { name: "Notificación" })).toHaveTextContent("El recurso ya no existe");
    await waitFor(() => expect(router.state.location.pathname).toBe("/clientes"));
    expect(await screen.findByText("Ana Pérez")).toBeInTheDocument();
  });

  it("un 404 al guardar avisa y lleva al listado", async () => {
    listado();
    validacion();
    server.use(
      http.get("*/api/clientes/1", () => HttpResponse.json(ANA)),
      http.put("*/api/clientes/1", () => noEncontrado(1)),
    );
    const { usuario, router } = renderApp("/clientes/1/editar");
    await usuario.type(await screen.findByLabelText(/^Nombre/), "x");
    await usuario.click(screen.getByRole("button", { name: "Guardar" }));
    expect(await screen.findByRole("status", { name: "Notificación" })).toHaveTextContent("El recurso ya no existe");
    await waitFor(() => expect(router.state.location.pathname).toBe("/clientes"));
    expect(confirmacion()).not.toBeInTheDocument();
  });
});

describe("confirmación al salir con cambios sin guardar", () => {
  async function conCambios() {
    listado();
    validacion();
    const app = renderApp("/clientes/nuevo");
    await app.usuario.type(await screen.findByLabelText(/^Nombre/), "Algo");
    return app;
  }

  it("Cancelar sin cambios vuelve sin confirmar", async () => {
    listado();
    validacion();
    const { usuario, router } = renderApp("/clientes/nuevo");
    await usuario.click(screen.getByRole("button", { name: "Cancelar" }));
    expect(router.state.location.pathname).toBe("/clientes");
    expect(confirmacion()).not.toBeInTheDocument();
  });

  it("pide confirmación al salir; seguir conserva lo escrito y descartar completa la salida", async () => {
    const { usuario, router } = await conCambios();
    await usuario.click(within(screen.getByRole("navigation", { name: "Breadcrumbs" })).getByRole("link", { name: "Clientes" }));
    expect(confirmacion()).toBeInTheDocument();
    await usuario.click(screen.getByRole("button", { name: "Seguir editando" }));
    expect(screen.getByLabelText(/^Nombre/)).toHaveValue("Algo");

    await usuario.click(screen.getByRole("button", { name: "Cancelar" }));
    await usuario.click(screen.getByRole("button", { name: "Descartar y salir" }));
    expect(router.state.location.pathname).toBe("/clientes");
  });
});
