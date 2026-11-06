import { useCallback, useState } from "react";
import { api } from "../api/cliente";
import { llamar } from "../api/errores";
import type { components } from "../api/schema";
import { AvisoError } from "../ui/AvisoError";
import { Cargando } from "../ui/Cargando";
import { useToast } from "../ui/Toasts";
import { useCarga } from "../ui/useCarga";

type Elemento = components["schemas"]["ElementoDto"];
type Estado = "activas" | "bajas" | "todas";

// Mismos tonos que en EquipamientoController de escritorio.
const TONO_ESTADO: Record<string, string> = {
  Disponible: "estado-exito",
  "En Inspección": "estado-advertencia",
  "En Reparación": "estado-advertencia",
  "Fuera de Servicio": "estado-peligro",
  Extraviado: "estado-peligro",
};

const VACIO: Record<Estado, string> = {
  activas: "Todavía no hay elementos activos en el Inventario.",
  bajas: "No hay elementos dados de baja en el Inventario.",
  todas: "Todavía no hay elementos en el Inventario.",
};

export function ListadoInventario() {
  const toast = useToast();
  const [estado, setEstado] = useState<Estado>("activas");

  const cargar = useCallback(
    async () =>
      (await llamar(api.GET("/api/inventario", { params: { query: estado === "activas" ? {} : { estado } } }))) ?? [],
    [estado],
  );
  const { estado: carga, recargar } = useCarga<Elemento[]>(cargar);

  async function cambiarVigencia(elemento: Elemento) {
    const baja = elemento.activo;
    const id = elemento.id!;
    try {
      await llamar(
        baja
          ? api.POST("/api/inventario/{id}/baja", { params: { path: { id } } })
          : api.POST("/api/inventario/{id}/reactivar", { params: { path: { id } } }),
      );
      toast.exito(baja ? "Elemento dado de baja" : "Elemento reactivado");
    } catch (error) {
      toast.error(error);
    }
    recargar();
  }

  return (
    <section>
      <h1>Inventario</h1>
      <div className="filtros">
        <label className="filtro">
          Estado
          <select value={estado} onChange={(e) => setEstado(e.target.value as Estado)}>
            <option value="activas">Activas</option>
            <option value="bajas">Bajas</option>
            <option value="todas">Todas</option>
          </select>
        </label>
      </div>

      {carga.fase === "cargando" && <Cargando />}
      {carga.fase === "error" && <AvisoError error={carga.error} alReintentar={recargar} />}
      {carga.fase === "listo" && carga.datos.length === 0 && <p className="vacio">{VACIO[estado]}</p>}
      {carga.fase === "listo" && carga.datos.length > 0 && (
        <table className="tabla">
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Categoría</th>
              <th>Cantidad total</th>
              <th>Estado del elemento</th>
              <th>Vigencia</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {carga.datos.map((e) => (
              <tr key={e.id} className={e.activo ? undefined : "fila-baja"}>
                <td>{e.nombre}</td>
                <td>{e.categoria}</td>
                <td>{e.cantidad}</td>
                <td>
                  <span className={TONO_ESTADO[e.estadoEtiqueta ?? ""]}>{e.estadoEtiqueta}</span>
                </td>
                <td>{e.activo ? "Vigente" : "De baja"}</td>
                <td>
                  <button
                    type="button"
                    className={e.activo ? "accion-peligro" : undefined}
                    aria-label={`${e.activo ? "Dar de baja" : "Reactivar"} ${e.nombre}`}
                    onClick={() => cambiarVigencia(e)}
                  >
                    {e.activo ? "Dar de baja" : "Reactivar"}
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
