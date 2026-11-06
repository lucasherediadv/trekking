# 05: Listado de Inventario

**What to build:** El encargado de equipo ve los elementos del Inventario en una tabla con su Estado del elemento, filtra por estado y da de baja o reactiva desde cada fila.

**Blocked by:** 02

**Status:** ready-for-agent

- [ ] La tabla lista los elementos activos por defecto; el selector `estado` ofrece activas, bajas y todas.
- [ ] Cada fila muestra cantidad total, Estado del elemento con su color y vigencia.
- [ ] Baja y reactivación son acciones por fila, con solo la acción válida para el estado.
- [ ] Hay carga, estado vacío y reintento, con el manejo común de errores del ticket 02.
- [ ] El vocabulario es "Inventario" y "Estado del elemento", según el glosario.
- [ ] Tests del seam único: camino feliz, filtro `estado`, baja y reactivación, y un error de carga.
