import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from "react";
import type { components } from "../../api/schema";

// Estado del Asistente de Salida: vive en memoria en el layout del asistente (nunca
// en el almacenamiento del navegador) y se comparte entre las URL de los pasos.
// Los tickets siguientes agregan sus campos aquí y su regla en `pasos.ts`.

type Esquemas = components["schemas"];

export type ClavePaso = "datos" | "ruta" | "equipamiento" | "participantes" | "revision";

export interface DatosAsistente {
  // Datos (obligatorio)
  fecha: string;
  // Ruta (obligatorio); el nombre se guarda para mostrarlo en Revisión
  rutaId?: number;
  rutaNombre?: string;
  // Modo del paso Ruta: una Ruta nueva queda borrador y no se puede planificar (el paso queda incompleto)
  rutaNueva?: boolean;
  // Equipamiento (opcional)
  equipamiento: Esquemas["LineaEquipamientoDto"][];
  elementosNuevos: Esquemas["ElementoNuevoSolicitud"][];
  // Participantes (opcional)
  clientesExistentes: number[];
  clientesNuevos: Esquemas["ClienteNuevoSolicitud"][];
  // Pasos opcionales ya visitados
  visitados: ClavePaso[];
}

export const ESTADO_INICIAL: DatosAsistente = {
  fecha: "",
  equipamiento: [],
  elementosNuevos: [],
  clientesExistentes: [],
  clientesNuevos: [],
  visitados: [],
};

interface ContextoAsistente {
  datos: DatosAsistente;
  actualizar: (cambios: Partial<DatosAsistente>) => void;
  visitar: (paso: ClavePaso) => void;
}

const Contexto = createContext<ContextoAsistente | null>(null);

export function useAsistente(): ContextoAsistente {
  const c = useContext(Contexto);
  if (!c) throw new Error("useAsistente requiere ProveedorAsistente");
  return c;
}

export function ProveedorAsistente({ children }: { children: ReactNode }) {
  const [datos, setDatos] = useState<DatosAsistente>(ESTADO_INICIAL);
  const actualizar = useCallback((cambios: Partial<DatosAsistente>) => setDatos((d) => ({ ...d, ...cambios })), []);
  const visitar = useCallback(
    (paso: ClavePaso) => setDatos((d) => (d.visitados.includes(paso) ? d : { ...d, visitados: [...d.visitados, paso] })),
    [],
  );
  const valor = useMemo(() => ({ datos, actualizar, visitar }), [datos, actualizar, visitar]);
  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>;
}
