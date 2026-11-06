import { useCallback, useEffect, useState } from "react";

export type EstadoCarga<T> =
  | { fase: "cargando" }
  | { fase: "error"; error: unknown }
  | { fase: "listo"; datos: T };

/** Carga asíncrona con fases cargando/error/listo. Recarga con `recargar()` o si cambia `cargar` (memoizar con useCallback). */
export function useCarga<T>(cargar: () => Promise<T>) {
  const [estado, setEstado] = useState<EstadoCarga<T>>({ fase: "cargando" });
  const [version, setVersion] = useState(0);

  useEffect(() => {
    let vigente = true;
    setEstado({ fase: "cargando" });
    cargar().then(
      (datos) => vigente && setEstado({ fase: "listo", datos }),
      (error) => vigente && setEstado({ fase: "error", error }),
    );
    return () => {
      vigente = false;
    };
  }, [cargar, version]);

  const recargar = useCallback(() => setVersion((v) => v + 1), []);
  return { estado, recargar };
}
