import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { rutas } from "./App";
import "./estilos.css";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <RouterProvider router={createBrowserRouter(rutas)} />
  </StrictMode>,
);
