import { Navigate, type RouteObject } from "react-router-dom";
import { Asistente } from "./Asistente";
import { PasoDatos } from "./PasoDatos";
import { PasoEquipamiento } from "./PasoEquipamiento";
import { PasoParticipantes } from "./PasoParticipantes";
import { PasoRevision } from "./PasoRevision";
import { PasoRuta } from "./PasoRuta";

// Un paso = un hijo con su `path` igual a la clave de `pasos.ts`.
export const rutaAsistente: RouteObject = {
  path: "nueva",
  handle: { miga: "Nueva Salida" },
  element: <Asistente />,
  children: [
    { index: true, element: <Navigate to="datos" replace /> },
    { path: "datos", element: <PasoDatos /> },
    { path: "ruta", element: <PasoRuta /> },
    { path: "equipamiento", element: <PasoEquipamiento /> },
    { path: "participantes", element: <PasoParticipantes /> },
    { path: "revision", element: <PasoRevision /> },
  ],
};
