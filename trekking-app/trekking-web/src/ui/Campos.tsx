import { useId, type HTMLInputTypeAttribute } from "react";
import type { EstadoFormulario } from "./useFormulario";

type Formulario<V extends Record<string, string>> = Pick<EstadoFormulario<V>, "valores" | "errores" | "cambiar">;

interface Comun<V extends Record<string, string>> {
  f: Formulario<V>;
  nombre: keyof V & string;
  etiqueta: string;
  requerido?: boolean;
  ayuda?: string;
}

interface PropsControl {
  id: string;
  name: string;
  "aria-invalid": true | undefined;
  "aria-describedby": string | undefined;
}

function Campo<V extends Record<string, string>>(props: Comun<V> & { control: (p: PropsControl) => React.ReactNode }) {
  const id = useId();
  const error = props.f.errores[props.nombre];
  const ids = [props.ayuda && `${id}-ayuda`, error && `${id}-error`].filter(Boolean).join(" ");
  return (
    <div className="campo">
      <label htmlFor={id}>
        {props.etiqueta}
        {props.requerido ? " *" : ""}
      </label>
      {props.control({
        id,
        name: props.nombre,
        "aria-invalid": error ? true : undefined,
        "aria-describedby": ids || undefined,
      })}
      {props.ayuda && (
        <small id={`${id}-ayuda`} className="ayuda">
          {props.ayuda}
        </small>
      )}
      {error && (
        <small id={`${id}-error`} className="error-campo">
          {error}
        </small>
      )}
    </div>
  );
}

export function CampoTexto<V extends Record<string, string>>(
  props: Comun<V> & { tipo?: HTMLInputTypeAttribute; modoEntrada?: "decimal" | "numeric" | "text"; placeholder?: string },
) {
  return (
    <Campo
      {...props}
      control={(p) => (
        <input
          {...p}
          type={props.tipo ?? "text"}
          inputMode={props.modoEntrada}
          placeholder={props.placeholder}
          value={props.f.valores[props.nombre]}
          onChange={(e) => props.f.cambiar(props.nombre, e.target.value)}
        />
      )}
    />
  );
}

export function CampoSeleccion<V extends Record<string, string>>(props: Comun<V> & { opciones: (string | { valor: string; etiqueta: string })[]; vacio?: string }) {
  return (
    <Campo
      {...props}
      control={(p) => (
        <select {...p} value={props.f.valores[props.nombre]} onChange={(e) => props.f.cambiar(props.nombre, e.target.value)}>
          <option value="">{props.vacio ?? "Selecciona una opción"}</option>
          {props.opciones.map((o) => {
            const { valor, etiqueta } = typeof o === "string" ? { valor: o, etiqueta: o } : o;
            return (
              <option key={valor} value={valor}>
                {etiqueta}
              </option>
            );
          })}
        </select>
      )}
    />
  );
}
