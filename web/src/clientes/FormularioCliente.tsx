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
import { FichaMedica } from "./FichaMedica";
import { aDatosCliente, CamposCliente, desdeCliente, VACIO_CLIENTE, type ValoresCliente } from "./CamposCliente";

type Cliente = components["schemas"]["ClienteDto"];

function Contenido({ cliente, id }: { cliente: ValoresCliente; id?: number }) {
  const navegar = useNavigate();
  const toast = useToast();
  const estado = useFormulario<ValoresCliente>({
    inicial: cliente,
    enviar: (v) => {
      const body = aDatosCliente(v);
      return llamar(
        id === undefined
          ? api.POST("/api/clientes", { body })
          : api.PUT("/api/clientes/{id}", { params: { path: { id } }, body }),
      );
    },
    alGuardar: () => {
      toast.exito(id === undefined ? "Cliente creado" : "Cliente actualizado");
      navegar("/clientes");
    },
    alNoEncontrado: (error) => {
      toast.error(error);
      navegar("/clientes");
    },
  });

  return (
    <Formulario
      titulo={id === undefined ? "Nuevo cliente" : "Editar cliente"}
      descripcion="Los datos se validan mientras escribes; al guardar se vuelven a validar."
      estado={estado}
      volverA="/clientes"
    >
      <CamposCliente f={estado} />
    </Formulario>
  );
}

export function ClienteNuevo() {
  return <Contenido cliente={VACIO_CLIENTE} />;
}

export function ClienteEditar() {
  const id = Number(useParams().id);
  const navegar = useNavigate();
  const toast = useToast();
  const cargar = useCallback(async () => (await llamar(api.GET("/api/clientes/{id}", { params: { path: { id } } }))) as Cliente, [id]);
  const { estado, recargar } = useCarga<Cliente>(cargar);

  const noExiste = estado.fase === "error" && estado.error instanceof ErrorApi && estado.error.tipo === "no-encontrado";
  useEffect(() => {
    if (!noExiste) return;
    toast.error(estado.fase === "error" ? estado.error : undefined);
    navegar("/clientes", { replace: true });
  }, [noExiste]); // eslint-disable-line react-hooks/exhaustive-deps

  if (estado.fase === "error" && !noExiste) return <AvisoError error={estado.error} alReintentar={recargar} />;
  if (estado.fase !== "listo") return <Cargando />;
  return (
    <>
      <Contenido id={id} cliente={desdeCliente(estado.datos)} />
      <FichaMedica id={id} />
    </>
  );
}
