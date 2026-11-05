# 12: Ficha operativa en PDF

**What to build:** La interfaz web puede descargar la Ficha operativa de una Salida como PDF imprimible, sin adjuntos médicos.

**Blocked by:** 09

**Status:** ready-for-agent

- [x] Un endpoint de la Salida devuelve el PDF con tipo de contenido PDF y un nombre de archivo reconocible en la descarga.
- [x] El contenido es el generado por core a partir de la Copia operativa vigente; la API solo lo entrega.
- [x] El PDF no incorpora ninguna ficha médica de los participantes.
- [x] Una Salida inexistente responde 404.
- [x] Tests `MockMvc` con SQLite en `@TempDir`: respuesta PDF válida, cabeceras de descarga, 404, y un Cliente con ficha médica cuyo contenido no aparece en el PDF.
