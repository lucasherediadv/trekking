import { api } from "../../api/cliente";
import { llamar } from "../../api/errores";
import type { components } from "../../api/schema";
import { AvisoError } from "../../ui/AvisoError";
import { Cargando } from "../../ui/Cargando";
import { useCarga } from "../../ui/useCarga";
import { useAsistente } from "./estado";

type Ruta = components["schemas"]["RutaDto"];

// Solo Rutas existentes y no borrador (regla del dominio: no se planifica con una Ruta borrador).
async function cargar(): Promise<Ruta[]> {
  const rutas = (await llamar(api.GET("/api/rutas"))) ?? [];
  return rutas.filter((r) => !r.borrador);
}

export function PasoRuta() {
  const { datos, actualizar } = useAsistente();
  const { estado, recargar } = useCarga<Ruta[]>(cargar);

  function elegir(valor: string, rutas: Ruta[]) {
    const ruta = rutas.find((r) => String(r.id) === valor);
    actualizar({ rutaId: ruta?.id, rutaNombre: ruta?.nombre });
  }

  return (
    <section className="paso-contenido">
      <h2>Ruta</h2>
      {estado.fase === "cargando" && <Cargando />}
      {estado.fase === "error" && <AvisoError error={estado.error} alReintentar={recargar} />}
      {estado.fase === "listo" && (
        <>
          {estado.datos.length === 0 && <p className="vacio">No hay Rutas disponibles para planificar.</p>}
          <label className="campo">
            Ruta
            <select value={datos.rutaId ?? ""} onChange={(e) => elegir(e.target.value, estado.datos)}>
              <option value="">Selecciona una Ruta</option>
              {estado.datos.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.nombre}
                </option>
              ))}
            </select>
          </label>
          <DetalleRuta ruta={estado.datos.find((r) => r.id === datos.rutaId)} />
        </>
      )}
    </section>
  );
}

function DetalleRuta({ ruta }: { ruta?: Ruta }) {
  if (!ruta) return null;
  return (
    <dl className="detalle">
      <dt>Altitud máxima</dt>
      <dd>{ruta.altitudMaxima} m</dd>
      <dt>Terreno</dt>
      <dd>{ruta.tipoTerreno}</dd>
      <dt>Dificultad técnica</dt>
      <dd>{ruta.dificultadTecnica}</dd>
      <dt>Dificultad física</dt>
      <dd>{ruta.dificultadFisica}</dd>
      <dt>Itinerario</dt>
      <dd>{ruta.cantidadCheckpoints} Checkpoints</dd>
    </dl>
  );
}
