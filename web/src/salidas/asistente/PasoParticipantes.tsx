import { api } from "../../api/cliente";
import { llamar } from "../../api/errores";
import { AvisoError } from "../../ui/AvisoError";
import { Cargando } from "../../ui/Cargando";
import { useCarga } from "../../ui/useCarga";
import { EditorParticipantes, type ClienteListado, type Participantes } from "./EditorParticipantes";
import { useAsistente } from "./estado";

// Solo los Clientes activos (el listado de la API los devuelve por defecto).
async function cargar(): Promise<ClienteListado[]> {
  return (await llamar(api.GET("/api/clientes", { params: { query: {} } }))) ?? [];
}

export function PasoParticipantes() {
  const { datos, actualizar } = useAsistente();
  const { estado, recargar } = useCarga<ClienteListado[]>(cargar);

  function cambiar(p: Participantes, clientes: ClienteListado[]) {
    const nombresClientes: Record<number, string> = {};
    for (const id of p.existentes) {
      const c = clientes.find((x) => x.id === id);
      const nombre = c ? `${c.nombre} ${c.apellido}` : datos.nombresClientes[id];
      if (nombre !== undefined) nombresClientes[id] = nombre;
    }
    actualizar({ clientesExistentes: p.existentes, clientesNuevos: p.nuevos, nombresClientes });
  }

  return (
    <section className="paso-contenido">
      <h2>Participantes</h2>
      {estado.fase === "cargando" && <Cargando />}
      {estado.fase === "error" && <AvisoError error={estado.error} alReintentar={recargar} />}
      {estado.fase === "listo" && (
        <EditorParticipantes
          clientes={estado.datos}
          existentes={datos.clientesExistentes}
          nuevos={datos.clientesNuevos}
          alCambiar={(p) => cambiar(p, estado.datos)}
        />
      )}
    </section>
  );
}
