import { useCallback, useEffect, useRef } from "react";
import { Navigate, Outlet, useLocation, useNavigate } from "react-router-dom";
import { ConfirmarSalida } from "../../ui/ConfirmarSalida";
import { ProveedorAsistente, useAsistente, type ClavePaso } from "./estado";
import { alcanzable, indiceDe, PASOS, tieneDatos } from "./pasos";

export const RAIZ_ASISTENTE = "/salidas/nueva";

function pasoActual(pathname: string): ClavePaso | undefined {
  const ultimo = pathname.split("/").filter(Boolean).pop();
  return PASOS.find((p) => p.clave === ultimo)?.clave;
}

function Indicador({ actual }: { actual: ClavePaso }) {
  const { datos } = useAsistente();
  const navegar = useNavigate();
  return (
    <nav aria-label="Pasos del asistente">
      <ol className="pasos">
        {PASOS.map((p, i) => {
          const habilitado = alcanzable(p.clave, actual, datos);
          return (
            <li key={p.clave}>
              <button
                type="button"
                className={p.clave === actual ? "paso paso-actual" : "paso"}
                aria-current={p.clave === actual ? "step" : undefined}
                disabled={!habilitado}
                onClick={() => navegar(`${RAIZ_ASISTENTE}/${p.clave}`)}
              >
                {i + 1}. {p.titulo}
                {p.completo(datos) && <span className="paso-completado"> Completado</span>}
              </button>
            </li>
          );
        })}
      </ol>
    </nav>
  );
}

function Contenido({ actual }: { actual: ClavePaso }) {
  const { datos, visitar } = useAsistente();
  const navegar = useNavigate();
  const confirmada = useRef(false);

  // Un paso opcional cuenta como completado al visitarlo, esté vacío o no.
  useEffect(() => visitar(actual), [actual, visitar]);

  // Navegar entre los pasos del asistente es libre; salir de él con datos pide confirmación.
  const bloquear = useCallback(
    (destino?: string) => !confirmada.current && tieneDatos(datos) && !destino?.startsWith(RAIZ_ASISTENTE),
    [datos],
  );

  const i = indiceDe(actual);
  const siguiente = PASOS[i + 1];
  const anterior = PASOS[i - 1];

  return (
    <>
      <h1 className="asistente-titulo">Nueva Salida</h1>
      <Indicador actual={actual} />
      <Outlet
        context={{
          alConfirmar: () => {
            confirmada.current = true; // éxito: salir sin pedir confirmación
          },
        }}
      />
      <div className="asistente-acciones">
        <button type="button" onClick={() => navegar("/salidas")}>
          Cancelar
        </button>
        {anterior && (
          <button type="button" onClick={() => navegar(`${RAIZ_ASISTENTE}/${anterior.clave}`)}>
            Anterior
          </button>
        )}
        {siguiente && (
          <button
            type="button"
            className="accion-primaria"
            disabled={!alcanzable(siguiente.clave, actual, datos)}
            onClick={() => navegar(`${RAIZ_ASISTENTE}/${siguiente.clave}`)}
          >
            Siguiente
          </button>
        )}
      </div>
      <ConfirmarSalida bloquear={bloquear} />
    </>
  );
}

/** Contexto del Outlet de los pasos. */
export interface ContextoPaso {
  alConfirmar: () => void;
}

function Guardia() {
  const { pathname } = useLocation();
  const { datos } = useAsistente();
  const actual = pasoActual(pathname);
  if (!actual) return <Navigate to={`${RAIZ_ASISTENTE}/datos`} replace />;
  // Una URL de paso no alcanzable (p. ej. tras recargar) vuelve a Datos.
  if (!alcanzable(actual, "datos", datos)) return <Navigate to={`${RAIZ_ASISTENTE}/datos`} replace />;
  return <Contenido actual={actual} />;
}

/** Layout de /salidas/nueva: aloja el estado en memoria compartido por los pasos. */
export function Asistente() {
  return (
    <ProveedorAsistente>
      <section>
        <Guardia />
      </section>
    </ProveedorAsistente>
  );
}
