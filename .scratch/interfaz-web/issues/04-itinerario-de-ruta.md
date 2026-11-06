# 04: Itinerario de una Ruta

**What to build:** El guía abre el Itinerario de una Ruta, ve sus Checkpoints en orden y los agrega, edita, quita y reordena, guardando el Itinerario completo de una vez. Con dos o más Checkpoints guardados, la Ruta deja de ser borrador.

**Blocked by:** 02

**Status:** ready-for-agent

- [ ] Se accede desde la fila de la Ruta; la pantalla tiene URL propia y breadcrumbs.
- [ ] Los Checkpoints se muestran en orden explícito (ADR 0002) y se pueden agregar, editar, quitar y reordenar antes de guardar.
- [ ] Guardar envía el Itinerario completo en una sola petición y refleja la Ruta resultante.
- [ ] Los errores de un Checkpoint inválido se muestran por campo.
- [ ] Tras guardar dos o más Checkpoints, la Ruta aparece en el listado sin marca de borrador.
- [ ] Con cambios sin guardar se pide confirmación al salir.
- [ ] Tests del seam único: camino feliz con reordenamiento, 422 por campo, 404 y el paso de borrador a Ruta utilizable.
