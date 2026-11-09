// Elección de archivos adjuntos: límite de tamaño validado en el navegador y lectura a base64.
// Los datos médicos nunca se guardan en el navegador: solo se leen para enviarlos a la API.

/** Máximo de una ficha médica (igual al de la API, `trekking.api.max-ficha-medica`). */
export const LIMITE_ARCHIVO_BYTES = 10 * 1024 * 1024;

export const MENSAJE_ARCHIVO_GRANDE = "El archivo supera el máximo de 10 MB.";

/** Mensaje si el archivo no se puede enviar (supera el límite); `undefined` si es válido. */
export function errorDeArchivo(archivo: File): string | undefined {
  return archivo.size > LIMITE_ARCHIVO_BYTES ? MENSAJE_ARCHIVO_GRANDE : undefined;
}

/** Contenido del archivo en base64 (sin prefijo `data:`), como lo piden los Clientes nuevos de una Salida. */
export function leerBase64(archivo: Blob): Promise<string> {
  return new Promise((resolver, rechazar) => {
    const lector = new FileReader();
    lector.onload = () => resolver(String(lector.result).replace(/^data:[^,]*,/, ""));
    lector.onerror = () => rechazar(lector.error ?? new Error("No se pudo leer el archivo."));
    lector.readAsDataURL(archivo);
  });
}
