import { useState } from "react";
import { api } from "../../api/cliente";
import { llamar } from "../../api/errores";
import type { components } from "../../api/schema";
import { AvisoError } from "../../ui/AvisoError";
import { Cargando } from "../../ui/Cargando";
import { useCarga } from "../../ui/useCarga";
import { CamposRuta, cuerpoRuta, RUTA_VACIA, rutaCompleta, validarLocal, type ValoresRuta } from "../../rutas/FormularioRuta";
import { useFormulario } from "../../ui/useFormulario";
import { useAsistente } from "./estado";

type Ruta = components["schemas"]["RutaDto"];

// Solo Rutas existentes y no borrador (regla del dominio: no se planifica con una Ruta borrador).
async function cargar(): Promise<Ruta[]> {
  const rutas = (await llamar(api.GET("/api/rutas"))) ?? [];
  return rutas.filter((r) => !r.borrador);
}

export function PasoRuta() {
  const { datos, actualizar } = useAsistente();
  const nueva = datos.rutaNueva === true;

  return (
    <section className="paso-contenido">
      <h2>Ruta</h2>
      <div role="radiogroup" aria-label="Origen de la Ruta" className="campo">
        <label>
          <input type="radio" name="origen-ruta" checked={!nueva} onChange={() => actualizar({ rutaNueva: false })} /> Ruta existente
        </label>{" "}
        <label>
          <input type="radio" name="origen-ruta" checked={nueva} onChange={() => actualizar({ rutaNueva: true })} /> Ruta nueva
        </label>
      </div>
      {nueva ? <RutaNueva /> : <RutaExistente />}
    </section>
  );
}

/** Crea la Ruta en la API: queda borrador, así que no completa el paso. */
function RutaNueva() {
  const [creada, setCreada] = useState<string>();
  const formulario = useFormulario<ValoresRuta>({
    inicial: RUTA_VACIA,
    validar: validarLocal,
    enviar: async (v) => {
      const ruta = await llamar(api.POST("/api/rutas", { body: cuerpoRuta(v) }));
      setCreada(ruta?.nombre ?? v.nombre);
    },
    alGuardar: () => undefined,
    alNoEncontrado: () => undefined,
  });

  return (
    <form onSubmit={formulario.guardar} noValidate>
      <p className="ayuda">
        Una Ruta nueva queda como Ruta borrador hasta tener al menos dos Checkpoints en su Itinerario: no se puede usar para
        confirmar una Salida.
      </p>
      <CamposRuta estado={formulario} />
      {formulario.aviso && (
        <div role="alert" className="aviso aviso-error">
          <p>{formulario.aviso}</p>
        </div>
      )}
      {creada !== undefined && (
        <div role="status" className="aviso">
          <p>
            La Ruta {creada} se creó como borrador. Agrega sus Checkpoints desde Rutas y luego elígela como Ruta existente para
            continuar.
          </p>
        </div>
      )}
      <button type="submit" disabled={formulario.enviando || !rutaCompleta(formulario.valores)}>
        Crear Ruta
      </button>
    </form>
  );
}

function RutaExistente() {
  const { datos, actualizar } = useAsistente();
  const { estado, recargar } = useCarga<Ruta[]>(cargar);

  function elegir(valor: string, rutas: Ruta[]) {
    const ruta = rutas.find((r) => String(r.id) === valor);
    actualizar({ rutaId: ruta?.id, rutaNombre: ruta?.nombre });
  }

  return (
    <>
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
    </>
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
