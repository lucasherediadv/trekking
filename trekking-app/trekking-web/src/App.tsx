import { Link, Navigate, type RouteObject } from "react-router-dom";
import { ListadoClientes } from "./clientes/ListadoClientes";
import { ClienteEditar, ClienteNuevo } from "./clientes/FormularioCliente";
import { RutaEditar, RutaNueva } from "./rutas/FormularioRuta";
import { ElementoEditar, ElementoNuevo } from "./inventario/FormularioElemento";
import { ListadoInventario } from "./inventario/ListadoInventario";
import { ItinerarioRuta } from "./rutas/ItinerarioRuta";
import { ListadoRutas } from "./rutas/ListadoRutas";
import { DetalleSalida } from "./salidas/DetalleSalida";
import { ListadoSalidas } from "./salidas/ListadoSalidas";
import { rutaAsistente } from "./salidas/asistente/rutas";
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
      { index: true, element: <Navigate to="/salidas" replace /> },
      {
        path: "salidas",
        handle: { miga: "Salidas" },
        children: [
          { index: true, element: <ListadoSalidas /> },
          rutaAsistente,
          { path: ":id", handle: { miga: "Detalle de Salida" }, element: <DetalleSalida /> },
        ],
      },
      {
        path: "rutas",
        handle: { miga: "Rutas" },
        children: [
          { index: true, element: <ListadoRutas /> },
          { path: "nueva", handle: { miga: "Nueva ruta" }, element: <RutaNueva /> },
          { path: ":id/editar", handle: { miga: "Editar ruta" }, element: <RutaEditar /> },
          { path: ":id/itinerario", handle: { miga: "Itinerario" }, element: <ItinerarioRuta /> },
        ],
      },
      {
        path: "inventario",
        handle: { miga: "Inventario" },
        children: [
          { index: true, element: <ListadoInventario /> },
          { path: "nuevo", handle: { miga: "Nuevo elemento" }, element: <ElementoNuevo /> },
          { path: ":id/editar", handle: { miga: "Editar elemento" }, element: <ElementoEditar /> },
        ],
      },
      {
        path: "clientes",
        handle: { miga: "Clientes" },
        children: [
          { index: true, element: <ListadoClientes /> },
          { path: "nuevo", handle: { miga: "Nuevo cliente" }, element: <ClienteNuevo /> },
          { path: ":id/editar", handle: { miga: "Editar cliente" }, element: <ClienteEditar /> },
        ],
      },
      { path: "*", element: <NoEncontrada /> },
    ],
  },
];
