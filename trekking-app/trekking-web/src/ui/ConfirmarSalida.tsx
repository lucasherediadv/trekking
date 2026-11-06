import { useEffect } from "react";
import { useBlocker } from "react-router-dom";

/**
 * Confirmación al salir de una pantalla con cambios sin guardar (navegación interna y cierre de pestaña).
 * `guardar` devuelve true si guardó bien; entonces se sale. Reutilizable en cualquier formulario.
 */
export function ConfirmarSalida(props: { hayCambios: boolean; guardar: () => Promise<boolean> }) {
  const { hayCambios, guardar } = props;
  const bloqueo = useBlocker(({ currentLocation, nextLocation }) => hayCambios && currentLocation.pathname !== nextLocation.pathname);

  useEffect(() => {
    if (!hayCambios) return;
    const alCerrar = (e: BeforeUnloadEvent) => e.preventDefault();
    window.addEventListener("beforeunload", alCerrar);
    return () => window.removeEventListener("beforeunload", alCerrar);
  }, [hayCambios]);

  if (bloqueo.state !== "blocked") return null;
  return (
    <div className="dialogo-fondo">
      <div role="dialog" aria-modal="true" aria-labelledby="titulo-salida" className="dialogo">
        <h2 id="titulo-salida">Cambios sin guardar</h2>
        <p>Hay cambios pendientes. ¿Qué quieres hacer?</p>
        <div className="dialogo-acciones">
          <button type="button" onClick={async () => ((await guardar()) ? bloqueo.proceed() : bloqueo.reset())}>
            Guardar
          </button>
          <button type="button" className="accion-peligro" onClick={() => bloqueo.proceed()}>
            Descartar
          </button>
          <button type="button" onClick={() => bloqueo.reset()}>
            Seguir editando
          </button>
        </div>
      </div>
    </div>
  );
}
