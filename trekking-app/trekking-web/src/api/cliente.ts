import createClient from "openapi-fetch";
import type { paths } from "./schema";

// Vacío: rutas relativas a /api (proxy de Vite). Configurable con VITE_API_URL.
export const urlBaseApi: string = import.meta.env.VITE_API_URL ?? "";

// Los paths del contrato ya incluyen el prefijo /api.
export const api = createClient<paths>({ baseUrl: urlBaseApi });
