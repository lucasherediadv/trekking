import { useEffect } from "react";
import { useBlocker } from "react-router-dom";

/**
 * Pide confirmación antes de abandonar la pantalla (menú, breadcrumb, Cancelar o historial del
 * navegador) mientras `bloquear()` sea verdadero. También avisa al cerrar o recargar la pestaña.
 */
export function ConfirmarSalida({ bloquear }: { bloquear: () => boolean }) {
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
