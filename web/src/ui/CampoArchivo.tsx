import { useId, useState, type ChangeEvent } from "react";
import { errorDeArchivo } from "./archivo";

interface Props {
  etiqueta: string;
  /** Se llama solo con archivos que pasan la validación de tamaño. */
  alElegir: (archivo: File) => void;
  disabled?: boolean;
}

/** Selector de un archivo que rechaza en el navegador los que superan el límite, sin llamar a la API. */
export function CampoArchivo({ etiqueta, alElegir, disabled }: Props) {
  const id = useId();
  const [error, setError] = useState<string>();

  function alCambiar(e: ChangeEvent<HTMLInputElement>) {
    const archivo = e.target.files?.[0];
    e.target.value = "";
    if (!archivo) return;
    const mensaje = errorDeArchivo(archivo);
    setError(mensaje);
    if (!mensaje) alElegir(archivo);
  }

  return (
    <div className="campo">
      <label htmlFor={id}>{etiqueta}</label>
      <input id={id} type="file" disabled={disabled} onChange={alCambiar} aria-invalid={error ? true : undefined} />
      {error && (
        <div role="alert" className="aviso aviso-error">
          {error}
        </div>
      )}
    </div>
  );
}
