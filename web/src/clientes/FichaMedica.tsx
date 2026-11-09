import { useEffect, useState } from "react";
import { api } from "../api/cliente";
import { ErrorApi, llamar } from "../api/errores";
import { consultarFicha } from "../api/fichaMedica";
import { CampoArchivo } from "../ui/CampoArchivo";
import { guardarArchivo, nombreDeDescarga } from "../ui/descarga";
import { useToast } from "../ui/Toasts";

/**
 * Ficha médica de un Cliente: adjuntar/reemplazar, descargar y quitar, siempre con acciones explícitas.
 * Nunca se previsualiza ni se guarda en el navegador.
 */
export function FichaMedica({ id }: { id: number }) {
  const toast = useToast();
  const [tiene, setTiene] = useState<boolean>();
  const [ocupado, setOcupado] = useState(false);
  const [confirmando, setConfirmando] = useState(false);

  useEffect(() => {
    let vigente = true;
    consultarFicha(id).then((t) => vigente && setTiene(t));
    return () => {
      vigente = false;
    };
  }, [id]);

  async function accion(tarea: () => Promise<void>) {
    setOcupado(true);
    try {
      await tarea();
    } catch (e) {
      if (e instanceof ErrorApi && e.tipo === "no-encontrado" && tiene) setTiene(false);
      else toast.error(e);
    } finally {
      setOcupado(false);
    }
  }

  const adjuntar = (archivo: File) =>
    accion(async () => {
      await llamar(
        api.PUT("/api/clientes/{id}/ficha-medica", {
          params: { path: { id } },
          body: { archivo: archivo as unknown as string },
          bodySerializer: () => {
            const datos = new FormData();
            datos.append("archivo", archivo, archivo.name);
            return datos;
          },
        }),
      );
      setTiene(true);
      toast.exito("Ficha médica adjuntada");
    });

  const descargar = () =>
    accion(async () => {
      let respuesta: Response | undefined;
      const contenido = await llamar(
        api.GET("/api/clientes/{id}/ficha-medica", { params: { path: { id } }, parseAs: "blob" }).then((r) => {
          respuesta = r.response;
          return r as { data?: Blob; error?: unknown; response: Response };
        }),
      );
      if (contenido)
        guardarArchivo(contenido, nombreDeDescarga(respuesta?.headers.get("Content-Disposition") ?? null, `ficha-medica-cliente-${id}`));
    });

  const quitar = () => {
    setConfirmando(false);
    return accion(async () => {
      await llamar(api.DELETE("/api/clientes/{id}/ficha-medica", { params: { path: { id } } }));
      setTiene(false);
      toast.exito("Ficha médica quitada");
    });
  };

  return (
    <section aria-labelledby="titulo-ficha-medica">
      <h2 id="titulo-ficha-medica">Ficha médica</h2>
      {tiene === false && <p className="ayuda">Este cliente no tiene ficha médica.</p>}
      {tiene && (
        <p className="acciones-formulario">
          <button type="button" disabled={ocupado} onClick={descargar}>
            Descargar ficha médica
          </button>
          <button type="button" className="accion-peligro" disabled={ocupado} onClick={() => setConfirmando(true)}>
            Quitar ficha médica
          </button>
        </p>
      )}
      {tiene !== undefined && (
        <CampoArchivo
          etiqueta={tiene ? "Reemplazar ficha médica" : "Adjuntar ficha médica"}
          alElegir={adjuntar}
          disabled={ocupado}
        />
      )}
      {confirmando && (
        <div className="modal-fondo">
          <div role="dialog" aria-modal="true" aria-label="Quitar ficha médica" className="modal">
            <p>¿Quitar la ficha médica de este cliente? Esta acción no se puede deshacer.</p>
            <div className="acciones-formulario">
              <button type="button" onClick={() => setConfirmando(false)}>
                Cancelar
              </button>
              <button type="button" className="accion-peligro" onClick={quitar}>
                Quitar
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}
