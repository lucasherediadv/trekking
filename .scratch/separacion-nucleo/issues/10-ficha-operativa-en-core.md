# 10: Ficha operativa en core

**What to build:** Core genera la Ficha operativa de una Salida como bytes, a partir de su Copia operativa vigente, con la fecha de emisión tomada del reloj inyectado. Desktop elige el destino con su diálogo y escribe el archivo de forma atómica, como hoy. El contenido, el formato y el nombre de archivo propuesto no cambian.

**Blocked by:** 07

**Status:** closed

- [x] Core no conoce el destino ni el sistema de archivos al generar la Ficha operativa.
- [x] Desktop conserva el nombre de archivo propuesto y el guardado atómico.
- [x] Tests de casos de uso con reloj fijo: se genera un PDF no vacío para una Salida con datos y para una Salida sin Checkpoints, equipamiento ni participantes, y una Salida inexistente produce un error de entidad no encontrada.
- [x] Validación manual: exportar fichas de Salidas con pocos y muchos datos y compararlas con las generadas antes del cambio.

## Comments

- Implementado en bb22888: `CasosDeUsoFichaOperativa` y `GeneradorFichaOperativa` en core (PDFBox y fuentes pasaron a core); desktop conserva `nombreArchivo` y el guardado atómico en `FichaOperativaPDFService`.
- Validación manual completada por el usuario: fichas de Salidas con pocos y muchos datos comparadas con las anteriores.
- `AGENTS.md` aún cita `service/FichaOperativaPDFService.java` como la salida PDF y la carpeta de fuentes en desktop: actualizar en el ticket 12.
