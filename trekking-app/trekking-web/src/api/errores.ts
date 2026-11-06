// Único punto que interpreta los errores de la API (cuerpo uniforme CuerpoError).
// Las pantallas llaman a `llamar(...)` y muestran `mensajeDe(error)`; nunca leen
// respuestas HTTP ni el cuerpo de error directamente.

export type TipoError = "red" | "no-encontrado" | "solicitud-invalida" | "interno" | "negocio";

export interface ErrorCampo {
  campo: string;
  mensaje: string;
}

export class ErrorApi extends Error {
  constructor(
    readonly tipo: TipoError,
    mensaje: string,
    readonly estado?: number,
    readonly codigo?: string,
    readonly errores: ErrorCampo[] = [],
  ) {
    super(mensaje);
  }
}

export const MENSAJES = {
  red: "No se pudo conectar con el servidor. Revisa tu conexión e intenta de nuevo.",
  noEncontrado: "El recurso ya no existe.",
  solicitudInvalida: "La solicitud no es válida.",
  interno: "Ocurrió un error inesperado. Intenta de nuevo más tarde.",
} as const;

interface CuerpoError {
  codigo?: string;
  mensaje?: string;
  errores?: ErrorCampo[];
}

export function interpretarRespuesta(estado: number, cuerpo: unknown): ErrorApi {
  const c: CuerpoError = cuerpo && typeof cuerpo === "object" ? (cuerpo as CuerpoError) : {};
  if (estado === 404) return new ErrorApi("no-encontrado", MENSAJES.noEncontrado, estado, c.codigo);
  if (estado === 400) return new ErrorApi("solicitud-invalida", MENSAJES.solicitudInvalida, estado, c.codigo);
  if (estado >= 500 || !c.mensaje) return new ErrorApi("interno", MENSAJES.interno, estado, c.codigo);
  // Errores de negocio (409, 422...): el mensaje del núcleo está pensado para el usuario.
  return new ErrorApi("negocio", c.mensaje, estado, c.codigo, c.errores ?? []);
}

export function interpretarFalla(causa: unknown): ErrorApi {
  if (causa instanceof ErrorApi) return causa;
  return new ErrorApi("red", MENSAJES.red);
}

interface Resultado<T> {
  data?: T;
  error?: unknown;
  response: Response;
}

/** Envuelve una llamada de openapi-fetch: devuelve los datos o lanza un ErrorApi. */
export async function llamar<T>(peticion: Promise<Resultado<T>>): Promise<T | undefined> {
  let resultado: Resultado<T>;
  try {
    resultado = await peticion;
  } catch (causa) {
    throw interpretarFalla(causa);
  }
  if (!resultado.response.ok) throw interpretarRespuesta(resultado.response.status, resultado.error);
  return resultado.data;
}

export function mensajeDe(causa: unknown): string {
  return interpretarFalla(causa).message;
}
