import { Link, Navigate, type RouteObject } from "react-router-dom";
import { ListadoClientes } from "./clientes/ListadoClientes";
import { ListadoRutas } from "./rutas/ListadoRutas";
import { Shell } from "./shell/Shell";
import { ToastProvider } from "./ui/Toasts";

function Seccion({ titulo }: { titulo: string }) {
  return (
    <section>
      <h1>{titulo}</h1>
    </section>
  );
}

function NoEncontrada() {
  return (
    <section className="aviso">
      <h1>Página no encontrada</h1>
      <p>La dirección que buscas no existe.</p>
      <Link to="/">Volver al inicio</Link>
    </section>
  );
}

// Router de datos (useMatches alimenta los breadcrumbs): main.tsx usa
// createBrowserRouter(rutas) y los tests createMemoryRouter(rutas).
export const rutas: RouteObject[] = [
  {
    element: (
      <ToastProvider>
        <Shell />
      </ToastProvider>
    ),
    children: [
      // Hasta que exista Salidas, la raíz va a Rutas.
      { index: true, element: <Navigate to="/rutas" replace /> },
      { path: "salidas", element: <Seccion titulo="Salidas" /> },
      {
        path: "rutas",
        handle: { miga: "Rutas" },
        children: [
          { index: true, element: <ListadoRutas /> },
          { path: "nueva", handle: { miga: "Nueva ruta" }, element: <Seccion titulo="Nueva ruta" /> },
        ],
      },
      { path: "inventario", element: <Seccion titulo="Inventario" /> },
      { path: "clientes", element: <ListadoClientes /> },
      { path: "*", element: <NoEncontrada /> },
    ],
  },
];
