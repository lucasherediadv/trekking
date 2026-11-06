import type { ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { ConfirmarSalida } from "./ConfirmarSalida";
import type { EstadoFormulario } from "./useFormulario";

interface Props<V extends Record<string, string>> {
  titulo: string;
  descripcion?: string;
  estado: EstadoFormulario<V>;
  /** Destino de Cancelar. */
  volverA: string;
  children: ReactNode;
}

/** Marco común de los formularios: título, aviso general, Guardar/Cancelar y confirmación de salida. */
export function Formulario<V extends Record<string, string>>(props: Props<V>) {
  const navegar = useNavigate();
  const { estado } = props;
  return (
    <section>
      <h1>{props.titulo}</h1>
      {props.descripcion && <p className="ayuda">{props.descripcion}</p>}
      <form className="formulario" noValidate onSubmit={estado.guardar}>
        {estado.aviso && (
          <div role="alert" className="aviso aviso-error">
            {estado.aviso}
          </div>
        )}
        <div className="campos">{props.children}</div>
        <div className="acciones-formulario">
          <button type="submit" className="primario" disabled={estado.enviando}>
            Guardar
          </button>
          <button type="button" onClick={() => navegar(props.volverA)}>
            Cancelar
          </button>
        </div>
      </form>
      <ConfirmarSalida bloquear={estado.bloquear} />
    </section>
  );
}
