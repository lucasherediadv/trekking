import { useCallback, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api } from "../api/cliente";
import { ErrorApi, llamar } from "../api/errores";
import type { components } from "../api/schema";
import { AvisoError } from "../ui/AvisoError";
import { CampoSeleccion, CampoTexto } from "../ui/Campos";
import { Cargando } from "../ui/Cargando";
import { Formulario } from "../ui/Formulario";
import { useToast } from "../ui/Toasts";
import { useCarga } from "../ui/useCarga";
import { useFormulario } from "../ui/useFormulario";

type Ruta = components["schemas"]["RutaDto"];

const TERRENOS = ["Rocoso", "Boscoso", "Sendero", "Mixto"];
const DIFICULTADES = ["Baja", "Media", "Alta"];

const VACIA = { nombre: "", altitudMaxima: "", tipoTerreno: "", dificultadTecnica: "", dificultadFisica: "" };
type Valores = typeof VACIA;

// Misma conversión que ValidadorRuta.parsearAltitud del núcleo (admite coma decimal).
const aAltitud = (texto: string) => Number(texto.trim().replace(",", "."));

function validarLocal(v: Valores): Record<string, string> {
  const t = v.altitudMaxima.trim();
  return t !== "" && !Number.isFinite(aAltitud(t)) ? { altitudMaxima: "Ingresa un valor numérico válido." } : {};
}

function Contenido({ ruta, id }: { ruta: Valores; id?: number }) {
  const navegar = useNavigate();
  const toast = useToast();
  const estado = useFormulario<Valores>({
    inicial: ruta,
    validar: validarLocal,
    enviar: (v) => {
      const cuerpo = {
        nombre: v.nombre,
        altitudMaxima: v.altitudMaxima.trim() === "" ? undefined : aAltitud(v.altitudMaxima),
        tipoTerreno: v.tipoTerreno || undefined,
        dificultadTecnica: v.dificultadTecnica || undefined,
        dificultadFisica: v.dificultadFisica || undefined,
      };
      return llamar(
        id === undefined
          ? api.POST("/api/rutas", { body: cuerpo })
          : api.PUT("/api/rutas/{id}", { params: { path: { id } }, body: cuerpo }),
      );
    },
    alGuardar: () => {
      toast.exito(id === undefined ? "Ruta creada" : "Ruta actualizada");
      navegar("/rutas");
    },
    alNoEncontrado: (error) => {
      toast.error(error);
      navegar("/rutas");
    },
  });

  return (
    <Formulario
      titulo={id === undefined ? "Nueva ruta" : "Editar ruta"}
      descripcion={
        id === undefined
          ? "Completa la información principal de la Ruta. Quedará como borrador hasta tener al menos dos Checkpoints en su Itinerario."
          : "Modifica la información principal de la Ruta."
      }
      estado={estado}
      volverA="/rutas"
    >
      <CampoTexto f={estado} nombre="nombre" etiqueta="Nombre" requerido ayuda="Letras, números y espacios." placeholder="Ej: Cerro Champaquí" />
      <CampoTexto f={estado} nombre="altitudMaxima" etiqueta="Altitud máxima (m)" requerido modoEntrada="decimal" ayuda="Metros. Debe ser mayor o igual que 0." placeholder="Ej: 1200" />
      <CampoSeleccion f={estado} nombre="tipoTerreno" etiqueta="Tipo de terreno" requerido opciones={TERRENOS} />
      <CampoSeleccion f={estado} nombre="dificultadTecnica" etiqueta="Dificultad técnica" requerido opciones={DIFICULTADES} />
      <CampoSeleccion f={estado} nombre="dificultadFisica" etiqueta="Dificultad física" requerido opciones={DIFICULTADES} />
    </Formulario>
  );
}

export function RutaNueva() {
  return <Contenido ruta={VACIA} />;
}

export function RutaEditar() {
  const id = Number(useParams().id);
  const navegar = useNavigate();
  const toast = useToast();
  const cargar = useCallback(async () => (await llamar(api.GET("/api/rutas/{id}", { params: { path: { id } } }))) as Ruta, [id]);
  const { estado, recargar } = useCarga<Ruta>(cargar);

  const noExiste = estado.fase === "error" && estado.error instanceof ErrorApi && estado.error.tipo === "no-encontrado";
  useEffect(() => {
    if (!noExiste) return;
    toast.error(estado.fase === "error" ? estado.error : undefined);
    navegar("/rutas", { replace: true });
  }, [noExiste]); // eslint-disable-line react-hooks/exhaustive-deps

  if (estado.fase === "error" && !noExiste) return <AvisoError error={estado.error} alReintentar={recargar} />;
  if (estado.fase !== "listo") return <Cargando />;
  const r = estado.datos;
  return (
    <Contenido
      id={id}
      ruta={{
        nombre: r.nombre ?? "",
        altitudMaxima: String(r.altitudMaxima ?? ""),
        tipoTerreno: r.tipoTerreno ?? "",
        dificultadTecnica: r.dificultadTecnica ?? "",
        dificultadFisica: r.dificultadFisica ?? "",
      }}
    />
  );
}
