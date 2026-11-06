import { useState } from "react";
import type { components } from "../../api/schema";
import { CampoTexto } from "../../ui/Campos";
import { useFormulario } from "../../ui/useFormulario";
import { CamposElemento, ELEMENTO_VACIO, cuerpoElemento, validarElemento, type ValoresElemento } from "../../inventario/CamposElemento";

type Esquemas = components["schemas"];
export type Elemento = Esquemas["ElementoDto"];
export type Linea = Esquemas["LineaEquipamientoDto"];
export type ElementoNuevo = Esquemas["ElementoNuevoSolicitud"];

export interface Equipamiento {
  lineas: Linea[];
  nuevos: ElementoNuevo[];
}

interface Props extends Equipamiento {
  /** Elementos que se ofrecen (ya filtrados a los asignables por quien carga). */
  elementos: Elemento[];
  alCambiar: (equipamiento: Equipamiento) => void;
  /** Ofrece crear elementos nuevos (por omisión sí; la edición de una Salida no los admite). */
  permitirNuevos?: boolean;
}

const ASIGNAR_VACIO = { ...ELEMENTO_VACIO, asignar: "" };
type ValoresNuevo = typeof ASIGNAR_VACIO;

function validarNuevo(v: ValoresNuevo) {
  const errores = validarElemento(v);
  for (const c of ["nombre", "categoria", "cantidad", "estado"] as const) {
    if (!errores[c] && v[c].trim() === "") errores[c] = "Este campo es obligatorio.";
  }
  if (!/^\d+$/.test(v.asignar.trim()) || Number(v.asignar) < 1) errores.asignar = "Ingresa un número entero mayor a cero.";
  return errores;
}

function FormularioNuevo({ alAgregar }: { alAgregar: (e: ElementoNuevo) => void }) {
  const f = useFormulario<ValoresNuevo>({
    inicial: ASIGNAR_VACIO,
    validar: validarNuevo,
    enviar: async (v) => alAgregar({ elemento: cuerpoElemento(v), cantidad: Number(v.asignar.trim()) }),
    alGuardar: () => {},
    alNoEncontrado: () => {},
  });
  return (
    <fieldset className="elemento-nuevo">
      <legend>Crear elemento nuevo</legend>
      <CamposElemento f={f} />
      <CampoTexto f={f} nombre="asignar" etiqueta="Cantidad a asignar" requerido modoEntrada="numeric" placeholder="Ej: 2" />
      {f.aviso && <p role="alert" className="error-campo">{f.aviso}</p>}
      <button type="button" onClick={(e) => f.guardar(e)}>
        Agregar elemento nuevo
      </button>
    </fieldset>
  );
}

/**
 * Elección de equipamiento y alta de elementos nuevos. Presentacional:
 * recibe los elementos ofrecidos y el equipamiento actual, y avisa los cambios.
 */
export function EditorEquipamiento({ elementos, lineas, nuevos, alCambiar, permitirNuevos = true }: Props) {
  const [alta, setAlta] = useState(0); // cambia la key para vaciar el formulario tras agregar
  function cambiarCantidad(id: number, texto: string) {
    const n = Number(texto);
    const resto = lineas.filter((l) => l.elementoId !== id);
    alCambiar({ lineas: n > 0 ? [...resto, { elementoId: id, cantidad: n }] : resto, nuevos });
  }

  return (
    <>
      {elementos.length === 0 && <p className="vacio">No hay elementos asignables en el Inventario.</p>}
      <ul className="lista-equipamiento">
        {elementos.map((e) => (
          <li key={e.id}>
            <span>{e.nombre}</span> <small>Disponible: {e.cantidadDisponible}</small>
            <input
              type="number"
              min={0}
              aria-label={`Cantidad de ${e.nombre}`}
              value={lineas.find((l) => l.elementoId === e.id)?.cantidad ?? ""}
              onChange={(ev) => cambiarCantidad(e.id!, ev.target.value)}
            />
          </li>
        ))}
      </ul>
      {nuevos.length > 0 && (
        <ul className="lista-equipamiento" aria-label="Elementos nuevos">
          {nuevos.map((n, i) => (
            <li key={i}>
              <span>
                {n.elemento?.nombre} x {n.cantidad}
              </span>
              <button
                type="button"
                aria-label={`Quitar ${n.elemento?.nombre}`}
                onClick={() => alCambiar({ lineas, nuevos: nuevos.filter((_, j) => j !== i) })}
              >
                Quitar
              </button>
            </li>
          ))}
        </ul>
      )}
      {permitirNuevos && (
        <FormularioNuevo
          key={alta}
          alAgregar={(n) => {
            alCambiar({ lineas, nuevos: [...nuevos, n] });
            setAlta((a) => a + 1);
          }}
        />
      )}
    </>
  );
}
