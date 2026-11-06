import { useEffect } from "react";
import { useBlocker } from "react-router-dom";

/**
 * Pide confirmación antes de abandonar la pantalla (menú, breadcrumb, Cancelar o historial del
 * navegador) mientras `bloquear()` sea verdadero. También avisa al cerrar o recargar la pestaña.
 * Si se pasa `guardar` (devuelve true si guardó bien), el diálogo ofrece además "Guardar" y sale al guardar.
 */
export function ConfirmarSalida({ bloquear, guardar }: { bloquear: () => boolean; guardar?: () => Promise<boolean> }) {
  const bloqueo = useBlocker(() => bloquear());

  useEffect(() => {
    const alDescargar = (e: BeforeUnloadEvent) => {
      if (bloquear()) e.preventDefault();
    };
    window.addEventListener("beforeunload", alDescargar);
    return () => window.removeEventListener("beforeunload", alDescargar);
  }, [bloquear]);

  if (bloqueo.state !== "blocked") return null;
  return (
    <div className="modal-fondo">
      <div role="dialog" aria-modal="true" aria-label="Cambios sin guardar" className="modal">
        <p>Tienes cambios sin guardar. Si sales ahora se perderán.</p>
        <div className="acciones-formulario">
          {guardar && (
            <button type="button" onClick={async () => ((await guardar()) ? bloqueo.proceed() : bloqueo.reset())}>
              Guardar
            </button>
          )}
          <button type="button" onClick={() => bloqueo.reset()}>
            Seguir editando
          </button>
          <button type="button" className="accion-peligro" onClick={() => bloqueo.proceed()}>
            Descartar y salir
          </button>
        </div>
      </div>
    </div>
  );
}
