import createClient from "openapi-fetch";
import type { paths } from "./schema";

// Vacío: mismo origen (proxy de Vite en desarrollo); Request exige URL absoluta. Configurable con VITE_API_URL.
export const urlBaseApi: string = import.meta.env.VITE_API_URL ?? "";

// Los paths del contrato ya incluyen el prefijo /api.
export const api = createClient<paths>({
  baseUrl: urlBaseApi || window.location.origin,
  // Resuelve fetch en cada llamada (y no al crear el cliente) para que MSW lo intercepte en los tests.
  fetch: (peticion) => globalThis.fetch(peticion),
});
