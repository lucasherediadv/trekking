import { useEffect, useId, useState } from "react";
import { api } from "../api/cliente";
import { llamar } from "../api/errores";
import type { components } from "../api/schema";
import { CampoSeleccion, CampoTexto } from "../ui/Campos";
import type { EstadoFormulario } from "../ui/useFormulario";

// Campos, conversiones y validación sin guardar del Cliente, reutilizables fuera de la
// página de alta/edición (tickets 09 y 15). Los valores viajan como texto para encajar
// con `useFormulario`; la casilla de autorización usa "true" / "".

type Datos = components["schemas"]["DatosCliente"];
type Cliente = components["schemas"]["ClienteDto"];

export const VACIO_CLIENTE = {
  dni: "",
  nombre: "",
  apellido: "",
  fechaNacimiento: "",
  email: "",
  telefono: "",
  sexo: "",
  contactoEmergenciaNombre: "",
  contactoEmergenciaTelefono: "",
  contactoEmergenciaRelacion: "",
  autorizacionMenores: "",
  tutorNombre: "",
  tutorApellido: "",
  tutorDni: "",
  tutorTelefono: "",
};
export type ValoresCliente = typeof VACIO_CLIENTE;

export function desdeCliente(c: Cliente): ValoresCliente {
  return {
    dni: c.dni ?? "",
    nombre: c.nombre ?? "",
    apellido: c.apellido ?? "",
    fechaNacimiento: c.fechaNacimiento ?? "",
    email: c.email ?? "",
    telefono: c.telefono ?? "",
    sexo: c.sexo ?? "",
    contactoEmergenciaNombre: c.contactoEmergenciaNombre ?? "",
    contactoEmergenciaTelefono: c.contactoEmergenciaTelefono ?? "",
    contactoEmergenciaRelacion: c.contactoEmergenciaRelacion ?? "",
    autorizacionMenores: c.autorizacionMenores ? "true" : "",
    tutorNombre: c.tutorNombre ?? "",
    tutorApellido: c.tutorApellido ?? "",
    tutorDni: c.tutorDni ?? "",
    tutorTelefono: c.tutorTelefono ?? "",
  };
}

export function aDatosCliente(v: ValoresCliente): Datos {
  const t = (s: string) => s.trim();
  return {
    dni: t(v.dni),
    nombre: t(v.nombre),
    apellido: t(v.apellido),
    fechaNacimiento: v.fechaNacimiento || undefined,
    email: t(v.email),
    telefono: t(v.telefono),
    sexo: v.sexo,
    contactoEmergenciaNombre: t(v.contactoEmergenciaNombre),
    contactoEmergenciaTelefono: t(v.contactoEmergenciaTelefono),
    contactoEmergenciaRelacion: t(v.contactoEmergenciaRelacion),
    autorizacionMenores: v.autorizacionMenores === "true",
    tutorNombre: t(v.tutorNombre),
    tutorApellido: t(v.tutorApellido),
    tutorDni: t(v.tutorDni),
    tutorTelefono: t(v.tutorTelefono),
  };
}

const ESPERA_MS = 250;

/**
 * Valida sin guardar (`POST /api/clientes/validar`) mientras se escribe y averigua si el
 * Cliente es menor. La regla de menores es de la API: es menor si, sin autorización ni
 * tutor, la API reclama la autorización de menores.
 */
export function useValidacionCliente(valores: ValoresCliente) {
  const [resultado, setResultado] = useState<{ errores: Record<string, string>; menor: boolean }>({
    errores: {},
    menor: false,
  });
  const clave = JSON.stringify(valores);

  useEffect(() => {
    let vigente = true;
    const espera = setTimeout(async () => {
      const datos = aDatosCliente(valores);
      try {
        const errores = (await llamar(api.POST("/api/clientes/validar", { body: datos }))) ?? [];
        let menor = false;
        if (datos.fechaNacimiento) {
          const sinReglas = { ...datos, autorizacionMenores: false, tutorNombre: "", tutorApellido: "", tutorDni: "", tutorTelefono: "" };
          const prueba = (await llamar(api.POST("/api/clientes/validar", { body: sinReglas }))) ?? [];
          menor = prueba.some((e) => e.campo === "autorizacionMenores");
        }
        if (!vigente) return;
        const porCampo: Record<string, string> = {};
        for (const e of errores) if (e.campo && e.mensaje && !(e.campo in porCampo)) porCampo[e.campo] = e.mensaje;
        setResultado({ errores: porCampo, menor });
      } catch {
        // La validación en vivo es ayuda; al guardar la API vuelve a validar y avisa.
      }
    }, ESPERA_MS);
    return () => {
      vigente = false;
      clearTimeout(espera);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clave]);

  return resultado;
}

const CAMPOS_MENOR = ["autorizacionMenores", "tutorNombre", "tutorApellido", "tutorDni", "tutorTelefono"];
const SEXOS = ["Masculino", "Femenino", "No binario", "Otro", "Prefiero no decir"];

type Estado = Pick<EstadoFormulario<ValoresCliente>, "valores" | "errores" | "cambiar">;

/**
 * Todos los campos del Cliente con validación sin guardar. Un error en vivo solo se marca
 * en los campos que la persona ya tocó (y en los de menores, apenas aparecen); los del 422 (`f.errores`) siempre se muestran.
 */
export function CamposCliente({ f }: { f: Estado }) {
  const { errores: vivos, menor } = useValidacionCliente(f.valores);
  const [tocados, setTocados] = useState<ReadonlySet<string>>(new Set());
  const idAutorizacion = useId();

  const errores: Record<string, string> = {};
  for (const [campo, mensaje] of Object.entries(vivos)) if (tocados.has(campo) || (menor && CAMPOS_MENOR.includes(campo))) errores[campo] = mensaje;
  Object.assign(errores, f.errores);

  const g: Estado = {
    valores: f.valores,
    errores,
    cambiar: (campo, valor) => {
      setTocados((t) => new Set(t).add(campo));
      f.cambiar(campo, valor);
    },
  };
  const errorAutorizacion = errores.autorizacionMenores;

  return (
    <>
      <CampoTexto f={g} nombre="dni" etiqueta="DNI" requerido modoEntrada="numeric" ayuda="7 u 8 números." />
      <CampoTexto f={g} nombre="nombre" etiqueta="Nombre" requerido />
      <CampoTexto f={g} nombre="apellido" etiqueta="Apellido" requerido />
      <CampoTexto f={g} nombre="fechaNacimiento" etiqueta="Fecha de nacimiento" requerido tipo="date" />
      {menor && (
        <div role="note" className="aviso">
          Cliente menor de edad: requiere autorización de menores y los datos de su tutor.
        </div>
      )}
      <CampoTexto f={g} nombre="email" etiqueta="Email" tipo="email" />
      <CampoTexto f={g} nombre="telefono" etiqueta="Teléfono" requerido tipo="tel" />
      <CampoSeleccion f={g} nombre="sexo" etiqueta="Sexo" requerido opciones={SEXOS} />
      <CampoTexto f={g} nombre="contactoEmergenciaNombre" etiqueta="Contacto de emergencia (nombre)" ayuda="Completa los tres datos del contacto o ninguno." />
      <CampoTexto f={g} nombre="contactoEmergenciaTelefono" etiqueta="Contacto de emergencia (teléfono)" tipo="tel" />
      <CampoTexto f={g} nombre="contactoEmergenciaRelacion" etiqueta="Contacto de emergencia (relación)" />
      {menor && (
        <>
          <div className="campo">
            <label htmlFor={idAutorizacion}>
              <input
                id={idAutorizacion}
                type="checkbox"
                name="autorizacionMenores"
                checked={f.valores.autorizacionMenores === "true"}
                aria-invalid={errorAutorizacion ? true : undefined}
                aria-describedby={errorAutorizacion ? `${idAutorizacion}-error` : undefined}
                onChange={(e) => g.cambiar("autorizacionMenores", e.target.checked ? "true" : "")}
              />{" "}
              Autorización de menores
            </label>
            {errorAutorizacion && (
              <small id={`${idAutorizacion}-error`} className="error-campo">
                {errorAutorizacion}
              </small>
            )}
          </div>
          <CampoTexto f={g} nombre="tutorNombre" etiqueta="Nombre del tutor" requerido />
          <CampoTexto f={g} nombre="tutorApellido" etiqueta="Apellido del tutor" requerido />
          <CampoTexto f={g} nombre="tutorDni" etiqueta="DNI del tutor" requerido modoEntrada="numeric" />
          <CampoTexto f={g} nombre="tutorTelefono" etiqueta="Teléfono del tutor" requerido tipo="tel" />
        </>
      )}
    </>
  );
}
