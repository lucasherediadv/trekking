/** De ISO (aaaa-mm-dd) a dd/mm/aaaa; cualquier otro valor se muestra tal cual. */
export function formatearFecha(fecha?: string) {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(fecha ?? "");
  return m ? `${m[3]}/${m[2]}/${m[1]}` : (fecha ?? "");
}

/** El valor, o "—" si falta o está vacío. */
export const valorOGuion = (texto?: string | number) => (texto === undefined || texto === "" ? "—" : texto);
