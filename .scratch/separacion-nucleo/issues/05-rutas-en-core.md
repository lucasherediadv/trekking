# 05: Rutas y Ruta borrador en core

**What to build:** Alta, edición, baja, reactivación y listado de Rutas pasan a hacerse mediante casos de uso de core. La regla de Ruta borrador vive en core: una Ruta cuyo Itinerario no tiene Checkpoints es borrador y no puede usarse para planificar una Salida, y deja de serlo cuando tiene dos o más Checkpoints guardados.

**Blocked by:** 02

**Status:** ready-for-agent

- [ ] Las pantallas de Rutas no acceden a la persistencia de Rutas: solo usan casos de uso de core.
- [ ] La condición de Ruta borrador la calcula core.
- [ ] El formulario de Ruta valida en vivo con el validador de core.
- [ ] Tests de casos de uso: alta válida e inválida, edición, baja y reactivación, Ruta borrador sin Checkpoints y Ruta no borrador con dos o más.
- [ ] Test de integración en sqlite: repositorio de rutas.
- [ ] Validación manual de Rutas: alta, edición, baja, reactivación, listado y filtros.
