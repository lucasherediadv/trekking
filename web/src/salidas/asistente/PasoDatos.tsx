import { useAsistente } from "./estado";

export function PasoDatos() {
  const { datos, actualizar } = useAsistente();
  return (
    <section className="paso-contenido">
      <h2>Datos de la salida</h2>
      <p className="ayuda">Indica la fecha en que se realizará la Salida.</p>
      <label className="campo">
        Fecha
        <input type="date" value={datos.fecha} onChange={(e) => actualizar({ fecha: e.target.value })} />
      </label>
    </section>
  );
}
