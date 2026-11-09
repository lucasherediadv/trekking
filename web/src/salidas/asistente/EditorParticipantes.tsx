import { useState } from "react";
import { api } from "../../api/cliente";
import { ErrorApi, llamar } from "../../api/errores";
import type { components } from "../../api/schema";
import { aDatosCliente, CamposCliente, VACIO_CLIENTE, type ValoresCliente } from "../../clientes/CamposCliente";
import { leerBase64 } from "../../ui/archivo";
import { CampoArchivo } from "../../ui/CampoArchivo";
import { useFormulario } from "../../ui/useFormulario";

type Esquemas = components["schemas"];
export type ClienteListado = Esquemas["ClienteDto"];
export type ClienteNuevo = Esquemas["ClienteNuevoSolicitud"];

export interface Participantes {
  /** Ids de Clientes existentes elegidos. */
  existentes: number[];
  nuevos: ClienteNuevo[];
}

interface Props extends Participantes {
  /** Clientes que se ofrecen (ya filtrados a los activos por quien carga). */
  clientes: ClienteListado[];
  alCambiar: (participantes: Participantes) => void;
  /** Ofrece crear Clientes nuevos (por omisión sí; la edición de una Salida no los admite). */
  permitirNuevos?: boolean;
}

function FormularioNuevo({ alAgregar }: { alAgregar: (c: ClienteNuevo) => void }) {
  const [ficha, setFicha] = useState<File>();
  const f = useFormulario<ValoresCliente>({
    inicial: VACIO_CLIENTE,
    // Valida sin guardar: los errores de la API se marcan por campo.
    enviar: async (v) => {
      const datos = aDatosCliente(v);
      const errores = (await llamar(api.POST("/api/clientes/validar", { body: datos }))) ?? [];
      if (errores.length > 0) {
        throw new ErrorApi("negocio", "Hay datos inválidos.", 422, undefined, errores.map((e) => ({ campo: e.campo ?? "", mensaje: e.mensaje ?? "" })));
      }
      // La ficha solo se lee para enviarla: queda en memoria, nunca en el almacenamiento del navegador.
      const adjunta = ficha ? { nombreArchivo: ficha.name, contenidoBase64: await leerBase64(ficha) } : undefined;
      alAgregar({ cliente: datos, ...(adjunta ? { ficha: adjunta } : {}) });
    },
    alGuardar: () => {},
    alNoEncontrado: () => {},
  });
  return (
    <fieldset className="cliente-nuevo">
      <legend>Crear Cliente nuevo</legend>
      <CamposCliente f={f} />
      <CampoArchivo etiqueta="Ficha médica (opcional)" alElegir={setFicha} />
      {ficha && <p>Ficha elegida: {ficha.name}</p>}
      {f.aviso && <p role="alert" className="error-campo">{f.aviso}</p>}
      <button type="button" disabled={f.enviando} onClick={(e) => f.guardar(e)}>
        Agregar Cliente nuevo
      </button>
    </fieldset>
  );
}

/**
 * Elección de Clientes existentes y alta de Clientes nuevos. Presentacional:
 * recibe los Clientes ofrecidos y los Participantes actuales, y avisa los cambios.
 */
export function EditorParticipantes({ clientes, existentes, nuevos, alCambiar, permitirNuevos = true }: Props) {
  const [alta, setAlta] = useState(0); // cambia la key para vaciar el formulario tras agregar
  function alternar(id: number) {
    alCambiar({ existentes: existentes.includes(id) ? existentes.filter((x) => x !== id) : [...existentes, id], nuevos });
  }

  return (
    <>
      {clientes.length === 0 && <p className="vacio">No hay Clientes activos.</p>}
      <ul className="lista-participantes">
        {clientes.map((c) => (
          <li key={c.id}>
            <label>
              <input type="checkbox" checked={existentes.includes(c.id!)} onChange={() => alternar(c.id!)} />{" "}
              <span>
                {c.nombre} {c.apellido}
              </span>{" "}
              <small>DNI {c.dni}</small>
            </label>
          </li>
        ))}
      </ul>
      {nuevos.length > 0 && (
        <ul className="lista-participantes" aria-label="Clientes nuevos">
          {nuevos.map((n, i) => (
            <li key={i}>
              <span>
                {n.cliente?.nombre} {n.cliente?.apellido} (nuevo)
              </span>
              {n.ficha && <small> con ficha médica</small>}
              <button
                type="button"
                aria-label={`Quitar ${n.cliente?.nombre} ${n.cliente?.apellido}`}
                onClick={() => alCambiar({ existentes, nuevos: nuevos.filter((_, j) => j !== i) })}
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
            alCambiar({ existentes, nuevos: [...nuevos, n] });
            setAlta((a) => a + 1);
          }}
        />
      )}
    </>
  );
}
