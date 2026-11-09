import { useEffect, useRef } from "react";

/** Diálogo accesible de confirmación de una acción; Escape o "Volver" la descartan. */
export function ConfirmarAccion(props: { pregunta: string; alConfirmar: () => void; alCancelar: () => void }) {
  const confirmar = useRef<HTMLButtonElement>(null);
  const { alCancelar } = props;

  useEffect(() => {
    confirmar.current?.focus();
    const alTeclear = (e: KeyboardEvent) => e.key === "Escape" && alCancelar();
    document.addEventListener("keydown", alTeclear);
    return () => document.removeEventListener("keydown", alTeclear);
  }, [alCancelar]);

  return (
    <div className="modal-fondo">
      <div role="alertdialog" aria-modal="true" aria-label={props.pregunta} className="modal">
        <p>{props.pregunta}</p>
        <div className="acciones-formulario">
          <button type="button" ref={confirmar} className="accion-peligro" onClick={props.alConfirmar}>
            Confirmar
          </button>
          <button type="button" onClick={props.alCancelar}>
            Volver
          </button>
        </div>
      </div>
    </div>
  );
}
