# 14: Asistente de Salida: Equipamiento

**What to build:** En el paso Equipamiento, el guía elige elementos del Inventario con su cantidad y puede crear elementos nuevos sin salir del asistente. Si falta stock, ve el elemento y la cantidad disponible.

**Blocked by:** 06, 13

**Status:** ready-for-agent

- [ ] Se ofrecen solo los elementos asignables; los que están en reparación, fuera de servicio o extraviados no aparecen.
- [ ] Se elige la cantidad por elemento y se pueden crear elementos nuevos dentro del asistente.
- [ ] Revisión incluye el equipamiento elegido y la confirmación lo envía en la misma petición.
- [ ] Un 409 de stock insuficiente indica el elemento y la cantidad disponible, sin perder lo ingresado, y no deja datos parciales.
- [ ] Tests del seam único: elección feliz, elemento nuevo, elementos no asignables ocultos y 409 de stock con elemento y disponible.
