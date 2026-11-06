// Descarga explícita de archivos (sin previsualización): usar desde un botón.

/** Nombre indicado por la API en Content-Disposition (filename* o filename); `respaldo` si no viene. */
export function nombreDeDescarga(contentDisposition: string | null, respaldo: string): string {
  if (!contentDisposition) return respaldo;
  const extendido = /filename\*\s*=\s*[^']*'[^']*'([^;]+)/i.exec(contentDisposition);
  if (extendido) {
    try {
      return decodeURIComponent(extendido[1].trim());
    } catch {
      /* sigue con filename simple */
    }
  }
  const simple = /filename\s*=\s*"?([^";]+)"?/i.exec(contentDisposition);
  return simple ? simple[1].trim() : respaldo;
}

/** Dispara la descarga de `contenido` en el navegador con el nombre dado. */
export function guardarArchivo(contenido: Blob, nombre: string) {
  const url = URL.createObjectURL(contenido);
  const enlace = document.createElement("a");
  enlace.href = url;
  enlace.download = nombre;
  document.body.appendChild(enlace);
  enlace.click();
  enlace.remove();
  URL.revokeObjectURL(url);
}
