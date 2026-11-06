import { useCallback, useEffect, useRef } from "react";
import { Navigate, useParams } from "react-router-dom";
import { api } from "../api/cliente";
import { ErrorApi, llamar } from "../api/errores";
import type { components } from "../api/schema";
import { AvisoError } from "../ui/AvisoError";
import { Cargando } from "../ui/Cargando";
import { guardarArchivo, nombreDeDescarga } from "../ui/descarga";
import { useToast } from "../ui/Toasts";
import { useCarga } from "../ui/useCarga";
import { ESTADOS, formatearFecha } from "./ListadoSalidas";

type Salida = components["schemas"]["SalidaDto"];
type Copia = components["schemas"]["CopiaOperativaDto"];

interface Datos {
  salida: Salida;
  copia: Copia;
  nombresElementos: Map<number, string>;
}

const valor = (texto?: string | number) => (texto === undefined || texto === "" ? "—" : texto);

async function cargar(id: number): Promise<Datos> {
  const [salida, copia, inventario] = await Promise.all([
    llamar(api.GET("/api/salidas/{id}", { params: { path: { id } } })),
    llamar(api.GET("/api/salidas/{id}/copia-operativa", { params: { path: { id } } })),
    // La Copia operativa solo trae ids de elementos: los nombres salen del Inventario (también de baja).
    llamar(api.GET("/api/inventario", { params: { query: { estado: "todas" } } })),
  ]);
  const nombresElementos = new Map<number, string>();
  for (const e of inventario ?? []) if (e.id !== undefined) nombresElementos.set(e.id, e.nombre ?? "");
  return { salida: salida ?? {}, copia: copia ?? {}, nombresElementos };
}

function Detalle({ id, datos }: { id: number; datos: Datos }) {
  const toast = useToast();
  const { salida, copia, nombresElementos } = datos;
  const estado = ESTADOS[salida.estado ?? ""];
  const ruta = copia.ruta ?? {};

  async function descargarFicha() {
    try {
      let respuesta: Response | undefined;
      const pdf = await llamar(
        api.GET("/api/salidas/{id}/ficha-operativa", { params: { path: { id } }, parseAs: "blob" }).then((r) => {
          respuesta = r.response;
          return r as { data?: Blob; error?: unknown; response: Response };
        }),
      );
      if (pdf) {
        const nombre = nombreDeDescarga(
          respuesta?.headers.get("Content-Disposition") ?? null,
          `ficha-operativa-salida-${id}.pdf`,
        );
        guardarArchivo(pdf, nombre);
      }
    } catch (e) {
      toast.error(e);
    }
  }

  return (
    <>
      <p>
        <strong>Fecha:</strong> {formatearFecha(copia.fecha ?? salida.fecha)} · <strong>Estado:</strong>{" "}
        <span className={estado?.tono}>{estado?.texto ?? salida.estado}</span>
      </p>
      <p>
        <button type="button" onClick={descargarFicha}>
          Descargar Ficha operativa
        </button>
      </p>

      <section aria-label="Ruta">
        <h2>Ruta</h2>
        <p>
          <strong>{valor(ruta.nombre ?? salida.rutaNombre)}</strong> · Altitud máxima: {valor(ruta.altitudMaxima)} m ·
          Terreno: {valor(ruta.tipoTerreno)} · Dificultad técnica: {valor(ruta.dificultadTecnica)} · Dificultad física:{" "}
          {valor(ruta.dificultadFisica)}
        </p>
      </section>

      <section aria-label="Checkpoints">
        <h2>Checkpoints</h2>
        {(copia.checkpoints ?? []).length === 0 ? (
          <p className="vacio">La Salida no tiene Checkpoints.</p>
        ) : (
          <table className="tabla">
            <thead>
              <tr>
                <th>Hora</th>
                <th>Nombre</th>
                <th>Altitud</th>
                <th>Descripción</th>
              </tr>
            </thead>
            <tbody>
              {copia.checkpoints!.map((c, i) => (
                <tr key={c.id ?? i}>
                  <td>{valor(c.hora)}</td>
                  <td>{valor(c.nombre)}</td>
                  <td>{valor(c.altitud)}</td>
                  <td>{valor(c.descripcion)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section aria-label="Equipamiento">
        <h2>Equipamiento</h2>
        {(copia.equipamiento ?? []).length === 0 ? (
          <p className="vacio">La Salida no tiene equipamiento asignado.</p>
        ) : (
          <table className="tabla">
            <thead>
              <tr>
                <th>Elemento</th>
                <th>Cantidad</th>
              </tr>
            </thead>
            <tbody>
              {copia.equipamiento!.map((l, i) => (
                <tr key={l.elementoId ?? i}>
                  <td>{nombresElementos.get(l.elementoId ?? -1) ?? `Elemento ${l.elementoId}`}</td>
                  <td>{valor(l.cantidad)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section aria-label="Participantes">
        <h2>Participantes</h2>
        {(copia.participantes ?? []).length === 0 ? (
          <p className="vacio">La Salida no tiene Participantes.</p>
        ) : (
          <table className="tabla">
            <thead>
              <tr>
                <th>DNI</th>
                <th>Nombre</th>
                <th>Apellido</th>
                <th>Fecha de nacimiento</th>
                <th>Teléfono</th>
              </tr>
            </thead>
            <tbody>
              {copia.participantes!.map((p, i) => (
                <tr key={p.id ?? i}>
                  <td>{valor(p.dni)}</td>
                  <td>{valor(p.nombre)}</td>
                  <td>{valor(p.apellido)}</td>
                  <td>{formatearFecha(p.fechaNacimiento) || "—"}</td>
                  <td>{valor(p.telefono)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </>
  );
}

/** Un 404 avisa con un toast y vuelve al listado. */
function NoEncontrada({ error }: { error: unknown }) {
  const toast = useToast();
  const avisado = useRef(false);
  useEffect(() => {
    if (avisado.current) return;
    avisado.current = true;
    toast.error(error);
  }, [toast, error]);
  return <Navigate to="/salidas" replace />;
}

export function DetalleSalida() {
  const { id: idTexto } = useParams();
  const id = Number(idTexto);
  const cargarDetalle = useCallback(() => cargar(id), [id]);
  const { estado, recargar } = useCarga<Datos>(cargarDetalle);

  if (estado.fase === "error" && estado.error instanceof ErrorApi && estado.error.tipo === "no-encontrado") {
    return <NoEncontrada error={estado.error} />;
  }
  return (
    <section>
      <h1>{estado.fase === "listo" ? `Salida de ${estado.datos.salida.rutaNombre}` : "Salida"}</h1>
      {estado.fase === "cargando" && <Cargando />}
      {estado.fase === "error" && <AvisoError error={estado.error} alReintentar={recargar} />}
      {estado.fase === "listo" && <Detalle id={id} datos={estado.datos} />}
    </section>
  );
}
