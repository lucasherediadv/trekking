import { mensajeDe } from "../api/errores";

/** Aviso de falla de carga con reintento. Reutilizable en cualquier pantalla. */
export function AvisoError({ error, alReintentar }: { error: unknown; alReintentar: () => void }) {
  return (
    <div role="alert" className="aviso aviso-error">
      <p>{mensajeDe(error)}</p>
      <button type="button" onClick={alReintentar}>
        Reintentar
      </button>
    </div>
  );
}
