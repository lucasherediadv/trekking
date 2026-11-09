import type { components } from "../api/schema";
import { CampoSeleccion, CampoTexto } from "../ui/Campos";
import type { EstadoFormulario, Errores } from "../ui/useFormulario";

type Elemento = components["schemas"]["ElementoDto"];
export type DatosElemento = components["schemas"]["DatosElemento"];

// Mismas categorías que ValidadorElemento.CATEGORIAS del núcleo.
export const CATEGORIAS = [
  "Campamento",
  "Navegación",
  "Comunicación",
  "Seguridad",
  "Escalada",
  "Vestimenta",
  "Hidratación",
  "Primeros Auxilios",
  "Herramientas",
  "Transporte",
];

// Valor de la API y etiqueta de EstadoElemento del núcleo.
export const ESTADOS_ELEMENTO = [
  { valor: "DISPONIBLE", etiqueta: "Disponible" },
  { valor: "EN_REPARACION", etiqueta: "En Reparación" },
  { valor: "EN_INSPECCION", etiqueta: "En Inspección" },
  { valor: "FUERA_DE_SERVICIO", etiqueta: "Fuera de Servicio" },
  { valor: "EXTRAVIADO", etiqueta: "Extraviado" },
];

export const ELEMENTO_VACIO = { nombre: "", categoria: "", cantidad: "", estado: "" };
export type ValoresElemento = typeof ELEMENTO_VACIO;

export function valoresDeElemento(e: Elemento): ValoresElemento {
  return { nombre: e.nombre ?? "", categoria: e.categoria ?? "", cantidad: String(e.cantidad ?? ""), estado: e.estado ?? "" };
}

/** Validación de formato previa al envío; las reglas de negocio las valida la API. */
export function validarElemento(v: ValoresElemento): Errores {
  const t = v.cantidad.trim();
  return t !== "" && !/^\d+$/.test(t) ? { cantidad: "Ingresa un número entero mayor o igual a cero." } : {};
}

/** Cuerpo de la API; los campos vacíos se omiten para que la API responda 422 por campo. */
export function cuerpoElemento(v: ValoresElemento): DatosElemento {
  return {
    nombre: v.nombre,
    categoria: v.categoria || undefined,
    cantidad: v.cantidad.trim() === "" ? undefined : Number(v.cantidad.trim()),
    estado: (v.estado || undefined) as DatosElemento["estado"],
  };
}

/** Campos del elemento, reutilizables dentro de cualquier formulario (p. ej. alta de elementos en el asistente de Salida). */
export function CamposElemento({ f }: { f: Pick<EstadoFormulario<ValoresElemento>, "valores" | "errores" | "cambiar"> }) {
  return (
    <>
      <CampoTexto f={f} nombre="nombre" etiqueta="Nombre" requerido placeholder="Ej: Carpa 4 personas" />
      <CampoSeleccion f={f} nombre="categoria" etiqueta="Categoría" requerido opciones={CATEGORIAS} />
      <CampoTexto f={f} nombre="cantidad" etiqueta="Cantidad total" requerido modoEntrada="numeric" ayuda="Número entero mayor o igual a 0." placeholder="Ej: 10" />
      <CampoSeleccion f={f} nombre="estado" etiqueta="Estado del elemento" requerido opciones={ESTADOS_ELEMENTO} />
    </>
  );
}
