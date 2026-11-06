import type { ClavePaso, DatosAsistente } from "./estado";

// Reglas de los pasos (glosario: Asistente de Salida). Sin React: se prueban a
// través de la app. Para agregar una regla a un paso, cambiar su `completo`.

export interface Paso {
  clave: ClavePaso;
  titulo: string;
  obligatorio: boolean;
  /** Un paso obligatorio está completo si sus datos son válidos; uno opcional, al visitarlo. */
  completo: (d: DatosAsistente) => boolean;
}

export const PASOS: Paso[] = [
  { clave: "datos", titulo: "Datos", obligatorio: true, completo: (d) => d.fecha !== "" },
  { clave: "ruta", titulo: "Ruta", obligatorio: true, completo: (d) => d.rutaId !== undefined && !d.rutaNueva },
  { clave: "equipamiento", titulo: "Equipamiento", obligatorio: false, completo: (d) => d.visitados.includes("equipamiento") },
  { clave: "participantes", titulo: "Participantes", obligatorio: false, completo: (d) => d.visitados.includes("participantes") },
  { clave: "revision", titulo: "Revisión", obligatorio: false, completo: () => false },
];

export const indiceDe = (clave: ClavePaso) => PASOS.findIndex((p) => p.clave === clave);

/** Avanzar a un paso exige que todos los obligatorios anteriores estén completos. */
export function alcanzable(destino: ClavePaso, actual: ClavePaso, d: DatosAsistente): boolean {
  const i = indiceDe(destino);
  if (i <= indiceDe(actual)) return true; // hacia atrás (o el mismo): siempre, sin validar
  return PASOS.slice(0, i).every((p) => !p.obligatorio || p.completo(d));
}

export function tieneDatos(d: DatosAsistente): boolean {
  return (
    d.fecha !== "" ||
    d.rutaId !== undefined ||
    d.equipamiento.length > 0 ||
    d.elementosNuevos.length > 0 ||
    d.clientesExistentes.length > 0 ||
    d.clientesNuevos.length > 0
  );
}
