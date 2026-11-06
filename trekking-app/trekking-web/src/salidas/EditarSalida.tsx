import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api } from "../api/cliente";
import { ErrorApi, llamar } from "../api/errores";
import type { components } from "../api/schema";
import { AvisoError } from "../ui/AvisoError";
import { Cargando } from "../ui/Cargando";
import { CampoTexto } from "../ui/Campos";
import { Formulario } from "../ui/Formulario";
import { useToast } from "../ui/Toasts";
import { useCarga } from "../ui/useCarga";
import { useFormulario } from "../ui/useFormulario";
import { EditorEquipamiento, type Elemento, type Linea } from "./asistente/EditorEquipamiento";
import { EditorParticipantes, type ClienteListado } from "./asistente/EditorParticipantes";
import { ProveedorAsistente, useAsistente } from "./asistente/estado";
import { PasoRuta } from "./asistente/PasoRuta";

type Esquemas = components["schemas"];

interface Datos {
  salida: Esquemas["SalidaDto"];
  copia: Esquemas["CopiaOperativaDto"];
  elementos: Elemento[];
  clientes: ClienteListado[];
}

// Los campos de lista viajan como JSON dentro de los valores del formulario para que
// useFormulario los trate como el resto (cambios sin guardar y errores por campo).
interface Valores extends Record<string, string> {
  fecha: string;
  ruta: string;
  equipamiento: string;
  participantes: string;
}

async function cargar(id: number): Promise<Datos> {
  const params = { params: { path: { id } } };
  const [salida, copia, elementos, activos] = await Promise.all([
    llamar(api.GET("/api/salidas/{id}", params)),
    llamar(api.GET("/api/salidas/{id}/copia-operativa", params)),
    llamar(api.GET("/api/salidas/{id}/elementos-para-editar", params)),
    llamar(api.GET("/api/clientes", { params: { query: {} } })),
  ]);
  // Un Participante dado de baja que ya estaba en la Copia operativa sigue visible para conservarlo.
  const clientes: ClienteListado[] = [...(activos ?? [])];
  for (const p of copia?.participantes ?? []) {
    if (!clientes.some((c) => c.id === p.id)) clientes.push({ id: p.id, dni: p.dni, nombre: p.nombre, apellido: p.apellido });
  }
  return { salida: salida ?? {}, copia: copia ?? {}, elementos: elementos ?? [], clientes };
}

const esConflictoDeEstado = (e: unknown) => e instanceof ErrorApi && e.estado === 409 && e.codigo !== "STOCK_INSUFICIENTE";

function leer<T>(texto: string): T[] {
  return JSON.parse(texto) as T[];
}

/** Lleva la Ruta elegida en PasoRuta (estado del asistente) al valor `ruta` del formulario. */
function PuenteRuta({ alCambiar }: { alCambiar: (ruta: string) => void }) {
  const { datos } = useAsistente();
  useEffect(() => alCambiar(datos.rutaId === undefined ? "" : String(datos.rutaId)), [datos.rutaId]); // eslint-disable-line react-hooks/exhaustive-deps
  return null;
}

function MensajeCampo({ texto }: { texto?: string }) {
  return texto ? <small className="error-campo">{texto}</small> : null;
}

function Contenido(props: { id: number; datos: Datos; alConflicto: (mensaje: string) => void }) {
  const { id, datos, alConflicto } = props;
  const navegar = useNavigate();
  const toast = useToast();
  const rutaOriginal = datos.copia.ruta?.id ?? datos.salida.rutaId;

  const f = useFormulario<Valores>({
    inicial: {
      fecha: datos.copia.fecha ?? datos.salida.fecha ?? "",
      ruta: rutaOriginal === undefined ? "" : String(rutaOriginal),
      equipamiento: JSON.stringify(datos.copia.equipamiento ?? []),
      participantes: JSON.stringify((datos.copia.participantes ?? []).map((p) => p.id)),
    },
    validar: (v) => ({
      ...(v.fecha.trim() === "" ? { fecha: "La fecha es obligatoria." } : {}),
      ...(v.ruta === "" ? { ruta: "La Ruta es obligatoria." } : {}),
    }),
    // Una sola petición que reemplaza la Copia operativa. Si cambia la Ruta, la copia toma su Itinerario.
    enviar: async (v) => {
      const rutaId = Number(v.ruta);
      const checkpoints =
        rutaId === rutaOriginal
          ? (datos.copia.checkpoints ?? [])
          : ((await llamar(api.GET("/api/rutas/{rutaId}/itinerario", { params: { path: { rutaId } } }))) ?? []);
      try {
        return await llamar(
          api.PUT("/api/salidas/{id}", {
            params: { path: { id } },
            body: {
              fecha: v.fecha,
              rutaId,
              actualizarDesdeRuta: false,
              checkpoints,
              equipamiento: leer<Linea>(v.equipamiento),
              participantes: leer<number>(v.participantes),
            },
          }),
        );
      } catch (e) {
        if (esConflictoDeEstado(e)) alConflicto((e as ErrorApi).message);
        throw e;
      }
    },
    alGuardar: () => {
      toast.exito("Salida actualizada");
      navegar("/salidas");
    },
    alNoEncontrado: (error) => {
      toast.error(error);
      navegar("/salidas");
    },
  });

  return (
    <Formulario
      titulo="Editar salida"
      descripcion="Modifica la fecha, la Ruta, el equipamiento y los Participantes de la Salida."
      estado={f}
      volverA="/salidas"
    >
      <ProveedorAsistente inicial={{ rutaId: rutaOriginal, rutaNombre: datos.copia.ruta?.nombre, rutaNueva: false }}>
        <PuenteRuta alCambiar={(ruta) => f.cambiar("ruta", ruta)} />
        <CampoTexto f={f} nombre="fecha" etiqueta="Fecha" requerido tipo="date" />
        <PasoRuta />
        <MensajeCampo texto={f.errores.ruta} />
      </ProveedorAsistente>
      <section className="paso-contenido">
        <h2>Equipamiento</h2>
        <EditorEquipamiento
          elementos={datos.elementos}
          lineas={leer<Linea>(f.valores.equipamiento)}
          nuevos={[]}
          permitirNuevos={false}
          alCambiar={(e) => f.cambiar("equipamiento", JSON.stringify(e.lineas))}
        />
        <MensajeCampo texto={f.errores.equipamiento} />
      </section>
      <section className="paso-contenido">
        <h2>Participantes</h2>
        <EditorParticipantes
          clientes={datos.clientes}
          existentes={leer<number>(f.valores.participantes)}
          nuevos={[]}
          permitirNuevos={false}
          alCambiar={(p) => f.cambiar("participantes", JSON.stringify(p.existentes))}
        />
        <MensajeCampo texto={f.errores.participantes} />
      </section>
    </Formulario>
  );
}

export function EditarSalida() {
  const id = Number(useParams().id);
  const navegar = useNavigate();
  const toast = useToast();
  const [conflicto, setConflicto] = useState<string>();
  const cargarSalida = useCallback(() => cargar(id), [id]);
  const { estado, recargar } = useCarga<Datos>(cargarSalida);

  const noExiste = estado.fase === "error" && estado.error instanceof ErrorApi && estado.error.tipo === "no-encontrado";
  useEffect(() => {
    if (!noExiste) return;
    toast.error(estado.fase === "error" ? estado.error : undefined);
    navegar("/salidas", { replace: true });
  }, [noExiste]); // eslint-disable-line react-hooks/exhaustive-deps

  if (estado.fase === "error" && !noExiste) return <AvisoError error={estado.error} alReintentar={recargar} />;
  if (estado.fase !== "listo") {
    return (
      <>
        {conflicto && (
          <div role="alert" className="aviso aviso-error">
            {conflicto}
          </div>
        )}
        <Cargando />
      </>
    );
  }
  if (estado.datos.salida.estado !== "PLANIFICADA") {
    return (
      <section>
        <h1>Editar salida</h1>
        {conflicto && (
          <div role="alert" className="aviso aviso-error">
            {conflicto}
          </div>
        )}
        <p className="aviso">Esta Salida ya no se puede editar.</p>
        <Link to="/salidas">Volver a Salidas</Link>
      </section>
    );
  }
  return <Contenido id={id} datos={estado.datos} alConflicto={(m) => { setConflicto(m); recargar(); }} />;
}
