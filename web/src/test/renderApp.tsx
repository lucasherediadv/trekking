import { render } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
import { createMemoryRouter, RouterProvider } from "react-router-dom";
import { rutas } from "../App";

// Seam único: la app completa con su enrutador, en la URL indicada.
export function renderApp(ruta = "/") {
  const usuario = userEvent.setup();
  const router = createMemoryRouter(rutas, { initialEntries: [ruta] });
  render(<RouterProvider router={router} />);
  return { usuario, router };
}
