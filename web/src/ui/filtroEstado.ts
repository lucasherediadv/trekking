/** Filtro de vigencia de los listados; "activas" es lo que la API devuelve sin parámetro. */
export type FiltroEstado = "activas" | "bajas" | "todas";

export const consultaPorEstado = (estado: FiltroEstado) => (estado === "activas" ? {} : { estado });
