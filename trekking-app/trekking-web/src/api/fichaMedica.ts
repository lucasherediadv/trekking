import { urlBaseApi } from "./cliente";

/**
 * Indica si el Cliente tiene ficha médica sin descargarla: HEAD sobre el recurso
 * (200 existe, 404 no existe). `ClienteDto` no trae este dato. Ante un fallo de red devuelve false.
 */
export async function consultarFicha(id: number): Promise<boolean> {
  try {
    const r = await globalThis.fetch(`${urlBaseApi || window.location.origin}/api/clientes/${id}/ficha-medica`, {
      method: "HEAD",
    });
    return r.ok;
  } catch {
    return false;
  }
}
