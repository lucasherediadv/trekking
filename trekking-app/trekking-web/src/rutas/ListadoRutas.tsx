import { useCallback, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { consultaPorEstado, type FiltroEstado } from "../ui/filtroEstado";
import { api } from "../api/cliente";
import { llamar } from "../api/errores";
import type { components } from "../api/schema";
import { AvisoError } from "../ui/AvisoError";
import { Cargando } from "../ui/Cargando";
import { useToast } from "../ui/Toasts";
import { useCarga } from "../ui/useCarga";

type Ruta = components["schemas"]["RutaDto"];
type Estado = FiltroEstado;
type Opcion = [valor: string, texto: string];

const TERRENOS = ["Rocoso", "Boscoso", "Sendero", "Mixto"];
const DIFICULTADES = ["Baja", "Media", "Alta"];
const iguales = (v: string): Opcion => [v, v];

// Mismos tonos que EstadoCell en escritorio.
const TONO_DIFICULTAD: Record<string, string> = {
  Baja: "estado-exito",
  Media: "estado-advertencia",
  Alta: "estado-peligro",
};

function Filtro(props: { etiqueta: string; valor: string; opciones: Opcion[]; alCambiar: (v: string) => void }) {
  return (
    <label className="filtro">
      {props.etiqueta}
      <select value={props.valor} onChange={(e) => props.alCambiar(e.target.value)}>
        {props.opciones.map(([v, t]) => (
          <option key={v} value={v}>
            {t}
          </option>
        ))}
      </select>
    </label>
  );
}

export function ListadoRutas() {
  const toast = useToast();
  const [estado, setEstado] = useState<Estado>("activas");
  const [terreno, setTerreno] = useState("");
  const [tecnica, setTecnica] = useState("");
  const [fisica, setFisica] = useState("");

  const cargar = useCallback(
    async () =>
      (await llamar(api.GET("/api/rutas", { params: { query: consultaPorEstado(estado) } }))) ?? [],
    [estado],
  );
  const { estado: carga, recargar } = useCarga<Ruta[]>(cargar);

  const visibles = useMemo(
    () =>
      carga.fase !== "listo"
        ? []
        : carga.datos.filter(
            (r) =>
              (!terreno || r.tipoTerreno === terreno) &&
              (!tecnica || r.dificultadTecnica === tecnica) &&
              (!fisica || r.dificultadFisica === fisica),
          ),
    [carga, terreno, tecnica, fisica],
  );

  async function cambiarEstado(ruta: Ruta) {
    const baja = ruta.activa;
    const id = ruta.id!;
    try {
      await llamar(
        baja
          ? api.POST("/api/rutas/{id}/baja", { params: { path: { id } } })
          : api.POST("/api/rutas/{id}/reactivar", { params: { path: { id } } }),
      );
      toast.exito(baja ? "Ruta dada de baja" : "Ruta reactivada");
    } catch (error) {
      toast.error(error);
    }
    recargar();
  }

  function mensajeVacio() {
    if (carga.fase === "listo" && carga.datos.length > 0) return "No hay Rutas que coincidan con los filtros.";
    if (estado === "bajas") return "No hay Rutas dadas de baja.";
    if (estado === "todas") return "Todavía no hay Rutas.";
    return "Todavía no hay Rutas activas.";
  }

  return (
    <section>
      <h1>Rutas</h1>
      <Link to="/rutas/nueva" className="boton-enlace">
        Nueva ruta
      </Link>
      <div className="filtros">
        <Filtro
          etiqueta="Estado"
          valor={estado}
          alCambiar={(v) => setEstado(v as Estado)}
          opciones={[["activas", "Activas"], ["bajas", "Bajas"], ["todas", "Todas"]]}
        />
        <Filtro etiqueta="Terreno" valor={terreno} alCambiar={setTerreno} opciones={[["", "Todos"], ...TERRENOS.map(iguales)]} />
        <Filtro
          etiqueta="Dificultad técnica"
          valor={tecnica}
          alCambiar={setTecnica}
          opciones={[["", "Todas"], ...DIFICULTADES.map(iguales)]}
        />
        <Filtro
          etiqueta="Dificultad física"
          valor={fisica}
          alCambiar={setFisica}
          opciones={[["", "Todas"], ...DIFICULTADES.map(iguales)]}
        />
      </div>

      {carga.fase === "cargando" && <Cargando />}
      {carga.fase === "error" && <AvisoError error={carga.error} alReintentar={recargar} />}
      {carga.fase === "listo" && visibles.length === 0 && <p className="vacio">{mensajeVacio()}</p>}
      {carga.fase === "listo" && visibles.length > 0 && (
        <table className="tabla">
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Altitud máxima</th>
              <th>Terreno</th>
              <th>Dificultad técnica</th>
              <th>Dificultad física</th>
              <th>Itinerario</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {visibles.map((r) => (
              <tr key={r.id} className={r.activa ? undefined : "fila-baja"}>
                <td>{r.nombre}</td>
                <td>{r.altitudMaxima} m</td>
                <td>{r.tipoTerreno}</td>
                <td>
                  <span className={TONO_DIFICULTAD[r.dificultadTecnica ?? ""]}>{r.dificultadTecnica}</span>
                </td>
                <td>
                  <span className={TONO_DIFICULTAD[r.dificultadFisica ?? ""]}>{r.dificultadFisica}</span>
                </td>
                <td>
                  {r.borrador ? (
                    <span className="insignia-borrador">Borrador</span>
                  ) : (
                    `${r.cantidadCheckpoints} Checkpoints · Completa`
                  )}
                </td>
                <td>
                  <Link to={`/rutas/${r.id}/itinerario`} aria-label={`Itinerario de ${r.nombre}`}>
                    Itinerario
                  </Link>{" "}
                  <Link to={`/rutas/${r.id}/editar`} aria-label={`Editar ${r.nombre}`}>
                    Editar
                  </Link>{" "}
                  <button
                    type="button"
                    className={r.activa ? "accion-peligro" : undefined}
                    aria-label={`${r.activa ? "Dar de baja" : "Reactivar"} ${r.nombre}`}
                    onClick={() => cambiarEstado(r)}
                  >
                    {r.activa ? "Dar de baja" : "Reactivar"}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
