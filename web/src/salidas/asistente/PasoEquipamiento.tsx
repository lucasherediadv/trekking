import { api } from "../../api/cliente";
import { llamar } from "../../api/errores";
import { AvisoError } from "../../ui/AvisoError";
import { Cargando } from "../../ui/Cargando";
import { useCarga } from "../../ui/useCarga";
import { EditorEquipamiento, type Elemento, type Equipamiento } from "./EditorEquipamiento";
import { useAsistente } from "./estado";

// Solo los elementos asignables (no en reparación, fuera de servicio ni extraviados) y vigentes.
async function cargar(): Promise<Elemento[]> {
  return (await llamar(api.GET("/api/inventario/asignables"))) ?? [];
}

export function PasoEquipamiento() {
  const { datos, actualizar } = useAsistente();
  const { estado, recargar } = useCarga<Elemento[]>(cargar);

  function cambiar(e: Equipamiento, elementos: Elemento[]) {
    const nombresElementos: Record<number, string> = {};
    for (const l of e.lineas) {
      const nombre = elementos.find((x) => x.id === l.elementoId)?.nombre ?? datos.nombresElementos[l.elementoId!];
      if (nombre !== undefined) nombresElementos[l.elementoId!] = nombre;
    }
    actualizar({ equipamiento: e.lineas, elementosNuevos: e.nuevos, nombresElementos });
  }

  return (
    <section className="paso-contenido">
      <h2>Equipamiento</h2>
      {estado.fase === "cargando" && <Cargando />}
      {estado.fase === "error" && <AvisoError error={estado.error} alReintentar={recargar} />}
      {estado.fase === "listo" && (
        <EditorEquipamiento
          elementos={estado.datos}
          lineas={datos.equipamiento}
          nuevos={datos.elementosNuevos}
          alCambiar={(e) => cambiar(e, estado.datos)}
        />
      )}
    </section>
  );
}
