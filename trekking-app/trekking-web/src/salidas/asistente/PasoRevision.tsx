import { useState } from "react";
import { useNavigate, useOutletContext } from "react-router-dom";
import { api } from "../../api/cliente";
import { llamar, mensajeDe } from "../../api/errores";
import { useToast } from "../../ui/Toasts";
import type { ContextoPaso } from "./Asistente";
import { useAsistente, type ClavePaso } from "./estado";

/** De ISO (aaaa-mm-dd) a dd/mm/aaaa. */
function formatearFecha(fecha: string) {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(fecha);
  return m ? `${m[3]}/${m[2]}/${m[1]}` : fecha;
}

function Seccion(props: { titulo: string; paso: ClavePaso; children: React.ReactNode }) {
  const navegar = useNavigate();
  return (
    <section className="resumen-seccion" aria-label={props.titulo}>
      <h3>{props.titulo}</h3>
      {props.children}
      <button
        type="button"
        aria-label={`Editar ${props.titulo}`}
        onClick={() => navegar(`/salidas/nueva/${props.paso}`)}
      >
        Editar
      </button>
    </section>
  );
}

export function PasoRevision() {
  const { datos } = useAsistente();
  const { alConfirmar } = useOutletContext<ContextoPaso>();
  const navegar = useNavigate();
  const toast = useToast();
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<unknown>();

  // Una sola petición atómica; los Checkpoints no se envían (la Salida copia el Itinerario).
  async function confirmar() {
    setEnviando(true);
    setError(undefined);
    try {
      await llamar(
        api.POST("/api/salidas", {
          body: {
            fecha: datos.fecha,
            rutaId: datos.rutaId,
            equipamiento: datos.equipamiento,
            elementosNuevos: datos.elementosNuevos,
            clientesExistentes: datos.clientesExistentes,
            clientesNuevos: datos.clientesNuevos,
          },
        }),
      );
    } catch (e) {
      setError(e);
      setEnviando(false);
      return;
    }
    alConfirmar();
    toast.exito("Salida creada");
    navegar("/salidas");
  }

  return (
    <section className="paso-contenido">
      <h2>Revisión</h2>
      <Seccion titulo="Datos" paso="datos">
        <p>Fecha: {formatearFecha(datos.fecha)}</p>
      </Seccion>
      <Seccion titulo="Ruta" paso="ruta">
        <p>{datos.rutaNombre}</p>
      </Seccion>
      <Seccion titulo="Equipamiento" paso="equipamiento">
        <p>
          {datos.equipamiento.length + datos.elementosNuevos.length === 0
            ? "Sin equipamiento asignado."
            : `${datos.equipamiento.length + datos.elementosNuevos.length} elementos`}
        </p>
      </Seccion>
      <Seccion titulo="Participantes" paso="participantes">
        <p>
          {datos.clientesExistentes.length + datos.clientesNuevos.length === 0
            ? "Sin Participantes."
            : `${datos.clientesExistentes.length + datos.clientesNuevos.length} Participantes`}
        </p>
      </Seccion>
      {error !== undefined && (
        <div role="alert" className="aviso aviso-error">
          <p>{mensajeDe(error)}</p>
        </div>
      )}
      <button type="button" className="accion-primaria" disabled={enviando} onClick={confirmar}>
        Confirmar Salida
      </button>
    </section>
  );
}
