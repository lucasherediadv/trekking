import { useCallback, useRef, useState, type FormEvent } from "react";
import { ErrorApi, mensajeDe } from "../api/errores";

export type Errores = Record<string, string>;

interface Opciones<V extends Record<string, string>> {
  /** Valores con los que arranca el formulario (vacíos al crear, los de la entidad al editar). */
  inicial: V;
  /** Validación local mínima previa al envío (formato); las reglas de negocio las valida la API. */
  validar?: (valores: V) => Errores;
  /** Guarda en la API. Debe lanzar el ErrorApi de `llamar` si falla. */
  enviar: (valores: V) => Promise<unknown>;
  /** Se invoca tras guardar; la salida ya no pide confirmación. */
  alGuardar: () => void;
  /** Se invoca si la API responde 404 (la entidad ya no existe); la salida ya no pide confirmación. */
  alNoEncontrado: (error: ErrorApi) => void;
}

/**
 * Estado común de los formularios: valores, errores por campo (422), aviso general,
 * cambios sin guardar y envío. Se renderiza con `Formulario` y los `Campo*`.
 */
export function useFormulario<V extends Record<string, string>>(opciones: Opciones<V>) {
  const { inicial } = opciones;
  const [valores, setValores] = useState<V>(inicial);
  const [errores, setErrores] = useState<Errores>({});
  const [aviso, setAviso] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);
  const salidaLibre = useRef(false);

  const sucio = (Object.keys(inicial) as (keyof V)[]).some((c) => valores[c] !== inicial[c]);
  const sucioRef = useRef(sucio);
  sucioRef.current = sucio;
  /** Para `ConfirmarSalida`: hay cambios sin guardar y no se está saliendo por haber guardado. */
  const bloquear = useCallback(() => sucioRef.current && !salidaLibre.current, []);

  function cambiar(campo: keyof V & string, valor: string) {
    setValores((v) => ({ ...v, [campo]: valor }));
    setErrores((e) => {
      if (!(campo in e)) return e;
      const { [campo]: _quitado, ...resto } = e;
      return resto;
    });
  }

  async function guardar(evento: FormEvent) {
    evento.preventDefault();
    if (enviando) return;
    const locales = opciones.validar?.(valores) ?? {};
    if (Object.keys(locales).length > 0) {
      setErrores(locales);
      setAviso(null);
      return;
    }
    setEnviando(true);
    setErrores({});
    setAviso(null);
    try {
      await opciones.enviar(valores);
      salidaLibre.current = true;
      opciones.alGuardar();
    } catch (causa) {
      const error = causa instanceof ErrorApi ? causa : null;
      if (error?.tipo === "no-encontrado") {
        salidaLibre.current = true;
        opciones.alNoEncontrado(error);
        return;
      }
      if (error && error.errores.length > 0) {
        const porCampo: Errores = {};
        const generales: string[] = [];
        for (const { campo, mensaje } of error.errores) {
          if (campo in inicial && !(campo in porCampo)) porCampo[campo] = mensaje;
          else generales.push(mensaje);
        }
        setErrores(porCampo);
        setAviso(generales.length > 0 ? generales.join(" ") : null);
      } else {
        setAviso(mensajeDe(causa));
      }
    } finally {
      setEnviando(false);
    }
  }

  return { valores, errores, aviso, enviando, sucio, bloquear, cambiar, guardar };
}

export type EstadoFormulario<V extends Record<string, string>> = ReturnType<typeof useFormulario<V>>;
