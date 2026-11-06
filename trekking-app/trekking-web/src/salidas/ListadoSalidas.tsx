import { Link, useNavigate } from "react-router-dom";
import { api } from "../api/cliente";
import { llamar } from "../api/errores";
import type { components } from "../api/schema";
import { useToast } from "../ui/Toasts";
import { AvisoError } from "../ui/AvisoError";
import { Cargando } from "../ui/Cargando";
import { useCarga } from "../ui/useCarga";

type SalidaListada = components["schemas"]["SalidaListadaDto"];
type Indicadores = components["schemas"]["IndicadoresDto"];
type ProximaSalida = components["schemas"]["ProximaSalidaDto"];

interface Datos {
  salidas: SalidaListada[];
  indicadores: Indicadores;
  proximas: ProximaSalida[];
}

// Mismos tonos que EstadoCell en escritorio.
export const ESTADOS: Record<string, { texto: string; tono: string }> = {
  PLANIFICADA: { texto: "Planificada", tono: "estado-informacion" },
  REALIZADA: { texto: "Realizada", tono: "estado-exito" },
  CANCELADA: { texto: "Cancelada", tono: "estado-peligro" },
};

type Accion = "realizar" | "cancelar";
const ACCIONES: Record<Accion, { verbo: string; aviso: string }> = {
  realizar: { verbo: "Marcar como realizada", aviso: "Salida marcada como realizada" },
  cancelar: { verbo: "Cancelar", aviso: "Salida cancelada" },
};

async function cargar(): Promise<Datos> {
  const [salidas, indicadores, proximas] = await Promise.all([
    llamar(api.GET("/api/salidas")),
    llamar(api.GET("/api/dashboard/indicadores-salidas")),
    llamar(api.GET("/api/dashboard/proximas-salidas")),
  ]);
  return { salidas: salidas ?? [], indicadores: indicadores ?? {}, proximas: proximas ?? [] };
}

/** De ISO (aaaa-mm-dd) a dd/mm/aaaa; cualquier otro valor se muestra tal cual. */
export function formatearFecha(fecha?: string) {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(fecha ?? "");
  return m ? `${m[3]}/${m[2]}/${m[1]}` : (fecha ?? "");
}

const resumen = (texto?: string) => (texto ? texto : "—");

function Tarjeta(props: { titulo: string; cifra?: number; detalle: string; destino: string }) {
  return (
    <Link to={props.destino} className="tarjeta-indicador">
      <span className="tarjeta-titulo">{props.titulo}</span>
      <span className="tarjeta-cifra">{props.cifra ?? 0}</span>
      <span className="tarjeta-detalle">{props.detalle}</span>
    </Link>
  );
}

function Indicadores({ datos }: { datos: Indicadores }) {
  return (
    <section aria-label="Indicadores" className="indicadores">
      <Tarjeta
        titulo="Salidas próximas"
        cifra={datos.salidasProximas}
        detalle="Planificadas; hoy y próximos 7 días (inclusive)"
        destino="/salidas"
      />
    </section>
  );
}

function Proximas({ proximas }: { proximas: ProximaSalida[] }) {
  return (
    <section aria-label="Próximas Salidas">
      <h2>Próximas Salidas</h2>
      {proximas.length === 0 ? (
        <p className="vacio">No hay Salidas próximas.</p>
      ) : (
        <ul className="proximas">
          {proximas.map((p) => (
            <li key={p.id}>
              <strong>{p.nombreRuta}</strong> · {formatearFecha(p.fecha)} · {p.horaInicio} - {p.horaFin}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function Acciones({ salida, alCambiar }: { salida: SalidaListada; alCambiar: (s: SalidaListada, a: Accion) => void }) {
  // Solo las Salidas planificadas admiten cambios; las finales no ofrecen acciones.
  const navegar = useNavigate();
  if (salida.estado !== "PLANIFICADA") return null;
  return (
    <>
      <button type="button" aria-label={`Editar Salida de ${salida.rutaNombre}`} onClick={() => navegar(`/salidas/${salida.id}/editar`)}>
        Editar
      </button>{" "}
      <button type="button" aria-label={`Realizar Salida de ${salida.rutaNombre}`}
        onClick={() => alCambiar(salida, "realizar")}
      >
        Realizar
      </button>{" "}
      <button type="button" className="accion-peligro" aria-label={`Cancelar Salida de ${salida.rutaNombre}`}
        onClick={() => alCambiar(salida, "cancelar")}
      >
        Cancelar
      </button>
    </>
  );
}

function Tabla({ salidas, alCambiar }: { salidas: SalidaListada[]; alCambiar: (s: SalidaListada, a: Accion) => void }) {
  if (salidas.length === 0) return <p className="vacio">Todavía no hay Salidas.</p>;
  return (
    <table className="tabla">
      <thead>
        <tr>
          <th>Fecha</th>
          <th>Ruta</th>
          <th>Participantes</th>
          <th>Equipamiento asignado</th>
          <th>Estado</th>
          <th>Acciones</th>
        </tr>
      </thead>
      <tbody>
        {salidas.map((s) => {
          const estado = ESTADOS[s.estado ?? ""];
          return (
            <tr key={s.id} className={s.vencida ? "fila-vencida" : undefined}>
              <td>{formatearFecha(s.fecha)}</td>
              <td>{s.rutaNombre}</td>
              <td>{resumen(s.participantes)}</td>
              <td>{resumen(s.equipamiento)}</td>
              <td>
                <span className={estado?.tono}>{estado?.texto ?? s.estado}</span>
                {s.vencida && <span className="insignia-vencida">Vencida</span>}
              </td>
              <td>
                <Link to={`/salidas/${s.id}`} aria-label={`Ver Salida de ${s.rutaNombre}`}>
                  Ver
                </Link>{" "}
                <Acciones salida={s} alCambiar={alCambiar} />
              </td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}

export function ListadoSalidas() {
  const toast = useToast();
  const { estado, recargar } = useCarga<Datos>(cargar);

  // Cualquier resultado (éxito, 404, 409) recarga el listado; nunca se reintenta solo.
  async function cambiarEstado(salida: SalidaListada, accion: Accion) {
    const { verbo, aviso } = ACCIONES[accion];
    if (!window.confirm(`¿${verbo} la Salida de ${salida.rutaNombre}?`)) return;
    const params = { params: { path: { id: salida.id! } } };
    try {
      await llamar(
        accion === "realizar"
          ? api.POST("/api/salidas/{id}/realizada", params)
          : api.POST("/api/salidas/{id}/cancelar", params),
      );
      toast.exito(aviso);
    } catch (error) {
      toast.error(error);
    }
    recargar();
  }
  return (
    <section>
      <h1>Salidas</h1>
      <Link to="/salidas/nueva" className="accion-primaria">
        Nueva Salida
      </Link>
      {estado.fase === "cargando" && <Cargando />}
      {estado.fase === "error" && <AvisoError error={estado.error} alReintentar={recargar} />}
      {estado.fase === "listo" && (
        <>
          <Indicadores datos={estado.datos.indicadores} />
          <Proximas proximas={estado.datos.proximas} />
          <Tabla salidas={estado.datos.salidas} alCambiar={cambiarEstado} />
        </>
      )}
    </section>
  );
}
