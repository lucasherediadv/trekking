import { Fragment } from "react";
import { Link, useMatches } from "react-router-dom";

// Cada ruta secundaria declara en `handle.miga` su texto; las migas se arman con
// las rutas que coinciden, de la raíz de la sección a la pantalla actual.
export type ManejadorRuta = { miga?: string };

export function Breadcrumbs() {
  const migas = useMatches().flatMap((m) => {
    const miga = (m.handle as ManejadorRuta | undefined)?.miga;
    return miga ? [{ texto: miga, ruta: m.pathname }] : [];
  });
  if (migas.length < 2) return null;
  return (
    <nav aria-label="Breadcrumbs" className="breadcrumbs">
      {migas.map((m, i) => (
        <Fragment key={m.ruta}>
          {i > 0 && <span aria-hidden="true">›</span>}
          {i < migas.length - 1 ? (
            <Link to={m.ruta}>{m.texto}</Link>
          ) : (
            <span aria-current="page">{m.texto}</span>
          )}
        </Fragment>
      ))}
    </nav>
  );
}
