import { useCallback, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api } from "../api/cliente";
import { ErrorApi, llamar } from "../api/errores";
import type { components } from "../api/schema";
import { AvisoError } from "../ui/AvisoError";
import { Cargando } from "../ui/Cargando";
import { Formulario } from "../ui/Formulario";
import { useToast } from "../ui/Toasts";
import { useCarga } from "../ui/useCarga";
import { useFormulario } from "../ui/useFormulario";
import {
  CamposElemento,
  ELEMENTO_VACIO,
  cuerpoElemento,
  validarElemento,
  valoresDeElemento,
  type ValoresElemento,
} from "./CamposElemento";

type Elemento = components["schemas"]["ElementoDto"];

function Contenido({ elemento, id, vigente }: { elemento: ValoresElemento; id?: number; vigente?: boolean }) {
  const navegar = useNavigate();
  const toast = useToast();
  const estado = useFormulario<ValoresElemento>({
    inicial: elemento,
    validar: validarElemento,
    enviar: (v) =>
      llamar(
        id === undefined
          ? api.POST("/api/inventario", { body: cuerpoElemento(v) })
          : api.PUT("/api/inventario/{id}", { params: { path: { id } }, body: cuerpoElemento(v) }),
      ),
    alGuardar: () => {
      toast.exito(id === undefined ? "Elemento creado" : "Elemento actualizado");
      navegar("/inventario");
    },
    alNoEncontrado: (error) => {
      toast.error(error);
      navegar("/inventario");
    },
  });

  return (
    <Formulario
      titulo={id === undefined ? "Nuevo elemento" : "Editar elemento"}
      descripcion="Completa los datos del elemento de Inventario."
      estado={estado}
      volverA="/inventario"
    >
      <CamposElemento f={estado} />
      {vigente !== undefined && (
        <p className="ayuda">
          Vigencia: <strong>{vigente ? "Vigente" : "De baja"}</strong>. Se cambia con Dar de baja o Reactivar en el listado.
        </p>
      )}
    </Formulario>
  );
}

export function ElementoNuevo() {
  return <Contenido elemento={ELEMENTO_VACIO} />;
}

export function ElementoEditar() {
  const id = Number(useParams().id);
  const navegar = useNavigate();
  const toast = useToast();
  const cargar = useCallback(
    async () => (await llamar(api.GET("/api/inventario/{id}", { params: { path: { id } } }))) as Elemento,
    [id],
  );
  const { estado, recargar } = useCarga<Elemento>(cargar);

  const noExiste = estado.fase === "error" && estado.error instanceof ErrorApi && estado.error.tipo === "no-encontrado";
  useEffect(() => {
    if (!noExiste) return;
    toast.error(estado.fase === "error" ? estado.error : undefined);
    navegar("/inventario", { replace: true });
  }, [noExiste]); // eslint-disable-line react-hooks/exhaustive-deps

  if (estado.fase === "error" && !noExiste) return <AvisoError error={estado.error} alReintentar={recargar} />;
  if (estado.fase !== "listo") return <Cargando />;
  return <Contenido id={id} elemento={valoresDeElemento(estado.datos)} vigente={estado.datos.activo ?? true} />;
}
