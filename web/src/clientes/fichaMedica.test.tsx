import { beforeEach } from "vitest";
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
  activo: true,
  menor: false,
};

/** API con un Cliente y una ficha médica que se puede tener o no; refleja adjuntar y quitar. */
function api(conFicha: boolean) {
  const estado = { tiene: conFicha, subidas: [] as { nombre: string }[], descargas: 0, quitadas: 0 };
  server.use(
    http.get("*/api/clientes", () => HttpResponse.json([ANA])),
    http.get("*/api/clientes/1", () => HttpResponse.json(ANA)),
    http.post("*/api/clientes/validar", () => HttpResponse.json([])),
    http.head("*/api/clientes/1/ficha-medica", () => new HttpResponse(null, { status: estado.tiene ? 200 : 404 })),
    http.put("*/api/clientes/1/ficha-medica", async ({ request }) => {
      // Debe ser multipart; el archivo se registra en `adjuntados` (ver abajo).
      if (!request.headers.get("content-type")?.startsWith("multipart/form-data")) return new HttpResponse(null, { status: 415 });
      estado.subidas.push({ nombre: adjuntados.at(-1)?.nombre ?? "" });
      estado.tiene = true;
      return new HttpResponse(null, { status: 204 });
    }),
    http.get("*/api/clientes/1/ficha-medica", () => {
      estado.descargas++;
      if (!estado.tiene) return HttpResponse.json({ codigo: "NO_ENCONTRADO", mensaje: "no" }, { status: 404 });
      return new HttpResponse(new Uint8Array([1, 2, 3]), {
        headers: {
          "Content-Type": "application/octet-stream",
          "Content-Disposition": "attachment; filename*=UTF-8''ficha%20m%C3%A9dica%20Ana.pdf",
        },
      });
    }),
    http.delete("*/api/clientes/1/ficha-medica", () => {
      estado.quitadas++;
      estado.tiene = false;
      return new HttpResponse(null, { status: 204 });
    }),
  );
  return estado;
}

const seccion = () => screen.findByRole("region", { name: "Ficha médica" });
// El File de jsdom no se puede serializar con el fetch de Node: se registra qué archivo se anexa al
// FormData (campo y nombre) y se anexa texto en su lugar, para que MSW reciba un multipart válido.
const adjuntados: { campo: string; nombre: string }[] = [];
const anexarReal = FormData.prototype.append as (this: FormData, campo: string, valor: string | Blob, nombre?: string) => void;
beforeEach(() => {
  adjuntados.length = 0;
  vi.spyOn(FormData.prototype, "append").mockImplementation(function (this: FormData, campo: string, valor: string | Blob, nombre?: string) {
    if (typeof valor === "string") return anexarReal.call(this, campo, valor);
    adjuntados.push({ campo, nombre: nombre ?? (valor as File).name });
    return anexarReal.call(this, campo, "contenido");
  } as typeof FormData.prototype.append);
});
afterEach(() => vi.restoreAllMocks());

// `tamano` solo simula un archivo grande (que nunca se envía).
const archivo = (nombre = "ficha.pdf", tamano?: number) => {
  const f = new File(["x"], nombre, { type: "application/pdf" });
  if (tamano !== undefined) Object.defineProperty(f, "size", { value: tamano });
  return f;
};

describe("ficha médica del Cliente", () => {
  it("sin ficha avisa y permite adjuntar; no ofrece descargar ni quitar", async () => {
    const estado = api(false);
    const { usuario } = renderApp("/clientes/1/editar");
    const s = await seccion();
    expect(await within(s).findByText("Este cliente no tiene ficha médica.")).toBeInTheDocument();
    expect(within(s).queryByRole("button", { name: "Descargar ficha médica" })).not.toBeInTheDocument();
    expect(within(s).queryByRole("button", { name: "Quitar ficha médica" })).not.toBeInTheDocument();

    await usuario.upload(await within(s).findByLabelText("Adjuntar ficha médica"), archivo("nueva.pdf"));
    expect(await within(s).findByRole("button", { name: "Descargar ficha médica" })).toBeInTheDocument();
    expect(estado.subidas).toEqual([{ nombre: "nueva.pdf" }]);
    expect(adjuntados).toEqual([{ campo: "archivo", nombre: "nueva.pdf" }]);
    expect(screen.getByRole("status", { name: "Notificación" })).toHaveTextContent("Ficha médica adjuntada");
  });

  it("con ficha permite reemplazarla", async () => {
    const estado = api(true);
    const { usuario } = renderApp("/clientes/1/editar");
    const s = await seccion();
    await within(s).findByRole("button", { name: "Descargar ficha médica" });
    await usuario.upload(await within(s).findByLabelText("Reemplazar ficha médica"), archivo("otra.pdf"));
    await waitFor(() => expect(estado.subidas).toHaveLength(1));
    expect(estado.subidas[0].nombre).toBe("otra.pdf");
    expect(await screen.findByRole("status", { name: "Notificación" })).toHaveTextContent("Ficha médica adjuntada");
  });

  it("rechaza en el navegador un archivo de más de 10 MB sin llamar a la API", async () => {
    const estado = api(false);
    const { usuario } = renderApp("/clientes/1/editar");
    const s = await seccion();
    await usuario.upload(await within(s).findByLabelText("Adjuntar ficha médica"), archivo("enorme.pdf", 10 * 1024 * 1024 + 1));
    expect(await within(s).findByRole("alert")).toHaveTextContent("El archivo supera el máximo de 10 MB.");
    expect(estado.subidas).toHaveLength(0);
  });

  it("descarga solo con la acción explícita y conserva el nombre original, sin vista previa", async () => {
    const estado = api(true);
    const crear = vi.fn(() => "blob:ficha");
    const revocar = vi.fn();
    Object.assign(URL, { createObjectURL: crear, revokeObjectURL: revocar });
    const clics: string[] = [];
    vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(function (this: HTMLAnchorElement) {
      clics.push(this.download);
    });
    const { usuario } = renderApp("/clientes/1/editar");
    const s = await seccion();
    const boton = await within(s).findByRole("button", { name: "Descargar ficha médica" });
    expect(estado.descargas).toBe(0);
    expect(document.querySelector("iframe, embed, object, img")).toBeNull();
    await usuario.click(boton);
    await waitFor(() => expect(clics).toEqual(["ficha médica Ana.pdf"]));
    expect(estado.descargas).toBe(1);
    expect(revocar).toHaveBeenCalledWith("blob:ficha");
    vi.restoreAllMocks();
  });

  it("si la descarga responde 404 avisa que no tiene ficha en lugar de descargar", async () => {
    const estado = api(true);
    const { usuario } = renderApp("/clientes/1/editar");
    const s = await seccion();
    const boton = await within(s).findByRole("button", { name: "Descargar ficha médica" });
    estado.tiene = false;
    await usuario.click(boton);
    expect(await within(s).findByText("Este cliente no tiene ficha médica.")).toBeInTheDocument();
    expect(within(s).queryByRole("button", { name: "Descargar ficha médica" })).not.toBeInTheDocument();
  });

  it("quitar pide confirmación; cancelar no quita y confirmar sí", async () => {
    const estado = api(true);
    const { usuario } = renderApp("/clientes/1/editar");
    const s = await seccion();
    await usuario.click(await within(s).findByRole("button", { name: "Quitar ficha médica" }));
    const dialogo = screen.getByRole("dialog", { name: "Quitar ficha médica" });
    await usuario.click(within(dialogo).getByRole("button", { name: "Cancelar" }));
    expect(screen.queryByRole("dialog", { name: "Quitar ficha médica" })).not.toBeInTheDocument();
    expect(estado.quitadas).toBe(0);

    await usuario.click(within(s).getByRole("button", { name: "Quitar ficha médica" }));
    await usuario.click(within(screen.getByRole("dialog", { name: "Quitar ficha médica" })).getByRole("button", { name: "Quitar" }));
    expect(await within(s).findByText("Este cliente no tiene ficha médica.")).toBeInTheDocument();
    expect(estado.quitadas).toBe(1);
  });

  it.each([
    [413, "El archivo es demasiado grande."],
    [422, "Archivo inválido."],
  ])("muestra el error %i de la API al adjuntar", async (estadoHttp, mensaje) => {
    api(false);
    server.use(
      http.put("*/api/clientes/1/ficha-medica", () => HttpResponse.json({ codigo: "X", mensaje }, { status: estadoHttp })),
    );
    const { usuario } = renderApp("/clientes/1/editar");
    const s = await seccion();
    await usuario.upload(await within(s).findByLabelText("Adjuntar ficha médica"), archivo());
    expect(await screen.findByRole("status", { name: "Notificación" })).toHaveTextContent(mensaje);
  });

  it("un 404 al adjuntar se informa con mensaje claro", async () => {
    api(false);
    server.use(
      http.put("*/api/clientes/1/ficha-medica", () =>
        HttpResponse.json({ codigo: "NO_ENCONTRADO", mensaje: "no" }, { status: 404 }),
      ),
    );
    const { usuario } = renderApp("/clientes/1/editar");
    const s = await seccion();
    await usuario.upload(await within(s).findByLabelText("Adjuntar ficha médica"), archivo());
    expect(await screen.findByRole("status", { name: "Notificación" })).toHaveTextContent("El recurso ya no existe");
  });

  it("no escribe en localStorage, sessionStorage ni IndexedDB", async () => {
    api(true);
    const escrituras = vi.spyOn(Storage.prototype, "setItem");
    const idb = vi.fn();
    vi.stubGlobal("indexedDB", { open: idb });
    const { usuario } = renderApp("/clientes/1/editar");
    const s = await seccion();
    await usuario.upload(await within(s).findByLabelText("Reemplazar ficha médica"), archivo());
    await screen.findByRole("status", { name: "Notificación" });
    expect(escrituras).not.toHaveBeenCalled();
    expect(idb).not.toHaveBeenCalled();
    vi.unstubAllGlobals();
    escrituras.mockRestore();
  });
});
