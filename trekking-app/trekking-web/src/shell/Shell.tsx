import { useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { Breadcrumbs } from "./Breadcrumbs";
import { Icono, LogoMontana, type NombreIcono } from "./iconos";

const secciones: { ruta: string; texto: string; icono: NombreIcono }[] = [
  { ruta: "/salidas", texto: "Salidas", icono: "salidas" },
  { ruta: "/rutas", texto: "Rutas", icono: "rutas" },
  { ruta: "/inventario", texto: "Inventario", icono: "inventario" },
  { ruta: "/clientes", texto: "Clientes", icono: "clientes" },
];

export function Shell() {
  const [contraido, setContraido] = useState(false);
  return (
    <div className="shell">
      <aside className={contraido ? "menu menu-contraido" : "menu"}>
        <div className="menu-marca">
          <LogoMontana />
          {!contraido && <span>TrekRisk</span>}
        </div>
        <nav aria-label="Menú principal" className="menu-navegacion">
          {secciones.map((s) => (
            <NavLink key={s.ruta} to={s.ruta} title={s.texto} className="menu-enlace">
              <Icono nombre={s.icono} />
              {!contraido && <span>{s.texto}</span>}
            </NavLink>
          ))}
        </nav>
        <button
          type="button"
          className="menu-alternar"
          aria-label={contraido ? "Expandir menú" : "Contraer menú"}
          title={contraido ? "Expandir menú" : "Contraer menú"}
          onClick={() => setContraido(!contraido)}
        >
          <Icono nombre={contraido ? "expandir" : "contraer"} />
        </button>
      </aside>
      <main className="contenido">
        <Breadcrumbs />
        <Outlet />
      </main>
    </div>
  );
}
