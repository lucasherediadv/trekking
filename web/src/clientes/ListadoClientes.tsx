import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/cliente";
import { llamar } from "../api/errores";
import { consultarFicha } from "../api/fichaMedica";
import type { components } from "../api/schema";
import { AvisoError } from "../ui/AvisoError";
import { Cargando } from "../ui/Cargando";
import { consultaPorEstado, type FiltroEstado } from "../ui/filtroEstado";
import { useToast } from "../ui/Toasts";
import { useCarga } from "../ui/useCarga";

type Cliente = components["schemas"]["ClienteDto"];
type Estado = FiltroEstado;

function useTieneFicha(id: number): boolean | undefined {
  const [tiene, setTiene] = useState<boolean>();
  useEffect(() => {
    let vigente = true;
    consultarFicha(id).then((t) => vigente && setTiene(t));
    return () => {
      vigente = false;
    };
  }, [id]);
  return tiene;
}

function IndicadorFicha({ id }: { id: number }) {
  const tiene = useTieneFicha(id);
  if (tiene === undefined) return <span className="estado-neutro">…</span>;
  return tiene ? (
    <span className="estado-exito">Con ficha médica</span>
  ) : (
    <span className="estado-neutro">Sin ficha médica</span>
  );
}

export function ListadoClientes() {
  const toast = useToast();
  const [estado, setEstado] = useState<Estado>("activas");

  const cargar = useCallback(
    async () =>
      (await llamar(api.GET("/api/clientes", { params: { query: consultaPorEstado(estado) } }))) ?? [],
    [estado],
  );
  const { estado: carga, recargar } = useCarga<Cliente[]>(cargar);

  async function cambiarEstado(cliente: Cliente) {
    const baja = cliente.activo;
    const id = cliente.id!;
    try {
      await llamar(
        baja
          ? api.POST("/api/clientes/{id}/baja", { params: { path: { id } } })
          : api.POST("/api/clientes/{id}/reactivar", { params: { path: { id } } }),
      );
      toast.exito(baja ? "Cliente dado de baja" : "Cliente reactivado");
    } catch (error) {
      toast.error(error);
    }
    recargar();
  }

  function mensajeVacio() {
    if (estado === "bajas") return "No hay Clientes dados de baja.";
    if (estado === "todas") return "Todavía no hay Clientes.";
    return "Todavía no hay Clientes activos.";
  }

  return (
    <section>
      <h1>Clientes</h1>
      <Link to="/clientes/nuevo" className="boton-enlace">
        Nuevo cliente
      </Link>
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
      {carga.fase === "listo" && carga.datos.length === 0 && <p className="vacio">{mensajeVacio()}</p>}
      {carga.fase === "listo" && carga.datos.length > 0 && (
        <table className="tabla">
          <thead>
            <tr>
              <th>Nombre</th>
              <th>DNI</th>
              <th>Email</th>
              <th>Teléfono</th>
              <th>Menor</th>
              <th>Ficha médica</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {carga.datos.map((c) => {
              const nombre = `${c.nombre} ${c.apellido}`;
              return (
                <tr key={c.id} className={c.activo ? undefined : "fila-baja"}>
                  <td>{nombre}</td>
                  <td>{c.dni}</td>
                  <td>{c.email}</td>
                  <td>{c.telefono}</td>
                  <td>{c.menor && <span className="insignia-borrador">Menor</span>}</td>
                  <td>
                    <IndicadorFicha id={c.id!} />
                  </td>
                  <td>
                    <Link to={`/clientes/${c.id}/editar`} aria-label={`Editar ${nombre}`}>
                      Editar
                    </Link>{" "}
                    <button
                      type="button"
                      className={c.activo ? "accion-peligro" : undefined}
                      aria-label={`${c.activo ? "Dar de baja" : "Reactivar"} ${nombre}`}
                      onClick={() => cambiarEstado(c)}
                    >
                      {c.activo ? "Dar de baja" : "Reactivar"}
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
    </section>
  );
}
