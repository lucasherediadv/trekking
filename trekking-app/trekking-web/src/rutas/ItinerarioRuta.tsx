import { useCallback, useState, type FormEvent } from "react";
import { api } from "../api/cliente";
import { llamar, ErrorApi } from "../api/errores";
import type { components } from "../api/schema";
import { AvisoError } from "../ui/AvisoError";
import { Cargando } from "../ui/Cargando";
import { ConfirmarSalida } from "../ui/ConfirmarSalida";
import { useToast } from "../ui/Toasts";
import { useCarga } from "../ui/useCarga";
import { useParams } from "react-router-dom";

type Ruta = components["schemas"]["RutaDto"];
type CheckpointDto = components["schemas"]["CheckpointDto"];

// Checkpoint pendiente de guardar: `clave` identifica la fila en pantalla; los nuevos no tienen id.
interface Punto {
  clave: number;
  id?: number;
  nombre: string;
  hora: string;
  latitud: number;
  longitud: number;
  altitud: number;
  descripcion: string;
}

type Campo = "nombre" | "hora" | "latitud" | "longitud" | "altitud";
type Borrador = Record<Campo | "descripcion", string>;
type Errores = Partial<Record<Campo, string>>;

let siguienteClave = 0;
const aPuntos = (cps: CheckpointDto[]): Punto[] =>
  cps.map((c) => ({
    clave: ++siguienteClave,
    id: c.id,
    nombre: c.nombre ?? "",
    hora: c.hora ?? "",
    latitud: c.latitud ?? 0,
    longitud: c.longitud ?? 0,
    altitud: c.altitud ?? 0,
    descripcion: c.descripcion ?? "",
  }));

const decimal = (t: string) => Number(t.trim().replace(",", "."));

function rango(texto: string, min: number, max: number, mensaje: string): string | undefined {
  if (!texto.trim()) return "Este campo es obligatorio.";
  const n = decimal(texto);
  if (Number.isNaN(n)) return "Ingresa un número decimal válido.";
  return n >= min && n <= max ? undefined : mensaje;
}

// Retroalimentación inmediata de formato; las reglas definitivas las valida el núcleo al guardar.
function validar(b: Borrador, altitudMaxima: number): Errores {
  const e: Errores = {};
  if (!b.nombre.trim()) e.nombre = "Ingresa el nombre del Checkpoint.";
  if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(b.hora.trim())) e.hora = "Usa el formato de 24 horas HH:mm, por ejemplo 14:30.";
  e.latitud = rango(b.latitud, -90, 90, "La latitud debe estar entre -90 y 90.");
  e.longitud = rango(b.longitud, -180, 180, "La longitud debe estar entre -180 y 180.");
  e.altitud = rango(b.altitud, 0, altitudMaxima, `La altitud debe estar entre 0 y ${altitudMaxima} m.`);
  return Object.fromEntries(Object.entries(e).filter(([, v]) => v)) as Errores;
}

const aBorrador = (p?: Punto): Borrador => ({
  nombre: p?.nombre ?? "",
  hora: p?.hora ?? "",
  latitud: p ? String(p.latitud) : "",
  longitud: p ? String(p.longitud) : "",
  altitud: p ? String(p.altitud) : "0",
  descripcion: p?.descripcion ?? "",
});

const CAMPOS: [Campo, string][] = [
  ["nombre", "Nombre"],
  ["hora", "Hora"],
  ["latitud", "Latitud"],
  ["longitud", "Longitud"],
  ["altitud", "Altitud"],
];

function FormularioCheckpoint(props: {
  inicial?: Punto;
  altitudMaxima: number;
  alAceptar: (b: Borrador) => void;
  alCancelar: () => void;
}) {
  const [valores, setValores] = useState<Borrador>(() => aBorrador(props.inicial));
  const [errores, setErrores] = useState<Errores>({});

  function enviar(e: FormEvent) {
    e.preventDefault();
    const encontrados = validar(valores, props.altitudMaxima);
    setErrores(encontrados);
    if (Object.keys(encontrados).length === 0) props.alAceptar(valores);
  }

  return (
    <form className="formulario" onSubmit={enviar} noValidate>
      <h2>{props.inicial ? "Editar Checkpoint" : "Nuevo Checkpoint"}</h2>
      {CAMPOS.map(([campo, etiqueta]) => (
        <div key={campo} className="campo">
          <label htmlFor={`cp-${campo}`}>{etiqueta}</label>
          <input
            id={`cp-${campo}`}
            value={valores[campo]}
            aria-invalid={errores[campo] ? true : undefined}
            aria-describedby={errores[campo] ? `error-${campo}` : undefined}
            onChange={(e) => setValores({ ...valores, [campo]: e.target.value })}
          />
          {errores[campo] && (
            <span id={`error-${campo}`} className="error-campo">
              {errores[campo]}
            </span>
          )}
        </div>
      ))}
      <div className="campo">
        <label htmlFor="cp-descripcion">Descripción</label>
        <textarea
          id="cp-descripcion"
          value={valores.descripcion}
          onChange={(e) => setValores({ ...valores, descripcion: e.target.value })}
        />
      </div>
      <p className="ayuda">Altitud entre 0 y {props.altitudMaxima} m, según la altitud máxima de la Ruta.</p>
      <div className="acciones-formulario">
        <button type="submit">{props.inicial ? "Guardar cambios" : "Agregar"}</button>
        <button type="button" onClick={props.alCancelar}>
          Cancelar
        </button>
      </div>
    </form>
  );
}

function EditorItinerario(props: { rutaInicial: Ruta; checkpoints: CheckpointDto[] }) {
  const toast = useToast();
  const [ruta, setRuta] = useState(props.rutaInicial);
  const [puntos, setPuntos] = useState(() => aPuntos(props.checkpoints));
  const [cambios, setCambios] = useState(false);
  const [editando, setEditando] = useState<number | "nuevo" | null>(null);
  const [erroresServidor, setErroresServidor] = useState<string[]>([]);

  const modificar = (nuevos: Punto[]) => {
    setPuntos(nuevos);
    setCambios(true);
    setErroresServidor([]);
  };

  function mover(i: number, delta: number) {
    const copia = [...puntos];
    [copia[i], copia[i + delta]] = [copia[i + delta], copia[i]];
    modificar(copia);
  }

  function aceptar(b: Borrador) {
    const datos = {
      nombre: b.nombre.trim(),
      hora: b.hora.trim(),
      latitud: decimal(b.latitud),
      longitud: decimal(b.longitud),
      altitud: decimal(b.altitud),
      descripcion: b.descripcion.trim(),
    };
    if (editando === "nuevo") modificar([...puntos, { clave: ++siguienteClave, ...datos }]);
    else modificar(puntos.map((p, i) => (i === editando ? { ...p, ...datos } : p)));
    setEditando(null);
  }

  const guardar = useCallback(async (): Promise<boolean> => {
    const cuerpo: CheckpointDto[] = puntos.map(({ clave: _clave, ...cp }) => cp);
    try {
      const actualizada = await llamar(
        api.PUT("/api/rutas/{rutaId}/itinerario", { params: { path: { rutaId: ruta.id! } }, body: cuerpo }),
      );
      if (actualizada) setRuta(actualizada);
      setCambios(false);
      setErroresServidor([]);
      toast.exito("Itinerario guardado");
      // El servidor ordena por hora: se relee para mostrar el orden y los ids definitivos.
      try {
        const guardados = await llamar(
          api.GET("/api/rutas/{rutaId}/itinerario", { params: { path: { rutaId: ruta.id! } } }),
        );
        if (guardados) setPuntos(aPuntos(guardados));
      } catch {
        // Se conserva lo mostrado; ya quedó guardado.
      }
      return true;
    } catch (error) {
      if (error instanceof ErrorApi && error.errores.length > 0) setErroresServidor(error.errores.map((e) => e.mensaje));
      else toast.error(error);
      return false;
    }
  }, [puntos, ruta.id, toast]);

  const resumen = `Altitud máxima ${ruta.altitudMaxima} m · ${ruta.tipoTerreno} · Dificultad ${ruta.dificultadTecnica} / ${ruta.dificultadFisica}`;

  return (
    <section>
      <h1>Itinerario · {ruta.nombre}</h1>
      <p className="ayuda">{resumen}</p>
      <p className={ruta.borrador ? "insignia-borrador" : "ayuda"}>
        {ruta.borrador
          ? "Borrador · agrega dos o más Checkpoints para usar esta Ruta en una Salida."
          : "La Ruta puede guardarse sin Checkpoints o con al menos dos."}
      </p>

      <div className="barra-itinerario">
        <h2>Checkpoints ({puntos.length})</h2>
        <button type="button" onClick={() => setEditando("nuevo")}>
          Agregar Checkpoint
        </button>
      </div>

      {puntos.length === 0 ? (
        <p className="vacio">Esta Ruta todavía no tiene Checkpoints.</p>
      ) : (
        <ol className="lista-checkpoints">
          {puntos.map((p, i) => (
            <li key={p.clave}>
              <span className="hora-checkpoint">{p.hora}</span>
              <div className="datos-checkpoint">
                <strong data-testid="nombre-checkpoint">{p.nombre}</strong>
                <span className="ayuda">
                  {p.altitud} m · {p.latitud}, {p.longitud}
                </span>
              </div>
              <div className="acciones-fila">
                <button type="button" aria-label={`Subir ${p.nombre}`} disabled={i === 0} onClick={() => mover(i, -1)}>
                  ↑
                </button>
                <button
                  type="button"
                  aria-label={`Bajar ${p.nombre}`}
                  disabled={i === puntos.length - 1}
                  onClick={() => mover(i, 1)}
                >
                  ↓
                </button>
                <button type="button" aria-label={`Editar ${p.nombre}`} onClick={() => setEditando(i)}>
                  Editar
                </button>
                <button
                  type="button"
                  className="accion-peligro"
                  aria-label={`Quitar ${p.nombre}`}
                  onClick={() => modificar(puntos.filter((_, j) => j !== i))}
                >
                  Quitar
                </button>
              </div>
            </li>
          ))}
        </ol>
      )}

      {editando !== null && (
        <FormularioCheckpoint
          key={editando === "nuevo" ? "nuevo" : puntos[editando].clave}
          inicial={editando === "nuevo" ? undefined : puntos[editando]}
          altitudMaxima={ruta.altitudMaxima ?? 0}
          alAceptar={aceptar}
          alCancelar={() => setEditando(null)}
        />
      )}

      {erroresServidor.length > 0 && (
        <div role="alert" className="aviso aviso-error">
          {erroresServidor.map((m) => (
            <p key={m}>{m}</p>
          ))}
        </div>
      )}

      <div className="acciones-formulario">
        <button type="button" disabled={!cambios} onClick={guardar}>
          Guardar Itinerario
        </button>
      </div>
      <ConfirmarSalida hayCambios={cambios} guardar={guardar} />
    </section>
  );
}

export function ItinerarioRuta() {
  const { id } = useParams();
  const rutaId = Number(id);
  const cargar = useCallback(async () => {
    const [ruta, checkpoints] = await Promise.all([
      llamar(api.GET("/api/rutas/{id}", { params: { path: { id: rutaId } } })),
      llamar(api.GET("/api/rutas/{rutaId}/itinerario", { params: { path: { rutaId } } })),
    ]);
    return { ruta: ruta!, checkpoints: checkpoints ?? [] };
  }, [rutaId]);
  const { estado, recargar } = useCarga(cargar);

  if (estado.fase === "cargando") return <Cargando />;
  if (estado.fase === "error") return <AvisoError error={estado.error} alReintentar={recargar} />;
  return <EditorItinerario rutaInicial={estado.datos.ruta} checkpoints={estado.datos.checkpoints} />;
}
